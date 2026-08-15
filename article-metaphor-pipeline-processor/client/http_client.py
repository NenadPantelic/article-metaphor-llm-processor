from enum import Enum
from typing import Any, Union

import requests
from requests import Session

from config.logconfig import get_logger
from exception.client_exception import ClientException
from helper.serialization import serialize_body, deserialize_body
from util.retry_util import retry_http


class HttpMethod(Enum):
    GET = "GET"
    POST = "POST"
    PUT = "PUT"
    DELETE = "DELETE"


logger = get_logger()


class HttpClient:
    def __init__(self, url="", scheme="http", host="127.0.0.1", port="", api_base_path="", headers=None,
                 http_timeout: int = 30):
        self._session = Session()

        if url:
            self._url = url
        else:
            if port:
                url = f"{scheme}://{host}:{port}"
            else:
                url = f"{scheme}://{host}"

            self._url = f"{url}/{api_base_path}" if api_base_path else url

        # self._session.headers.update({_CONTENT_TYPE_HEADER: _APPLICATION_JSON})
        if headers:
            self._session.headers.update(headers)

        self._timeout = http_timeout

    # add configuration
    def _call(self, method: HttpMethod, endpoint: str, body: dict[str, Any] = None, headers=None, to_json=True) -> \
            Union[dict[str, Any], str]:
        url = f"{self._url}/{endpoint}"
        logger.debug(f"Calling {method} and URL {url}")
        payload = serialize_body(body) if body else None

        try:
            response = self._execute_http_request(method=method, url=url, payload=payload, headers=headers)
            if not response:
                raise ClientException("Unable to execute the request.")

            response.raise_for_status()

            if to_json:
                return deserialize_body(response.text)
            else:
                return response.text
        except Exception as e:
            raise ClientException(str(e), 500)

    @retry_http()
    def _execute_http_request(self, method: HttpMethod, url: str, payload: str,
                              headers: dict[str, Any] = None) -> requests.Response:
        response = None
        match method:
            case HttpMethod.GET:
                response = self._session.get(url, headers=headers, timeout=self._timeout)

            case HttpMethod.POST:
                response = self._session.post(url, json=payload, headers=headers, timeout=self._timeout)

            case HttpMethod.PUT:
                response = self._session.put(url, json=payload, headers=headers, timeout=self._timeout)

            case HttpMethod.DELETE:
                response = self._session.delete(url, headers=headers, timeout=self._timeout)

            case _:
                raise ClientException(f"Unsupported HTTP method: {method}")

        return response
