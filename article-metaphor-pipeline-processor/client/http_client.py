from enum import Enum
from typing import Any, Union

from requests import Session

from exception.client_exception import ClientException
from helper.serialization import serialize_body, deserialize_body

X_SERVICE_KEY_HEADER = "X-service-key"


class HttpMethod(Enum):
    GET = "GET"
    POST = "POST"
    PUT = "PUT"
    DELETE = "DELETE"


class HttpClient:
    def __init__(self, scheme="http", host="127.0.0.1", port="", api_base_path="", headers=None):
        self._session = Session()

        if port:
            url = f"{scheme}://{host}:{port}"
        else:
            url = f"{scheme}://{host}"

        self._url = f"{url}/{api_base_path}" if api_base_path else url

        # self._session.headers.update({_CONTENT_TYPE_HEADER: _APPLICATION_JSON})
        if headers:
            self._session.headers.update(headers)

    def _call(self, method: HttpMethod, endpoint: str, body: dict[str, Any] = None, headers=None, to_json=True) -> \
            Union[dict[str, Any], str]:
        url = f"{self._url}/{endpoint}"
        payload = serialize_body(body)

        response = None
        match method:
            case HttpMethod.GET:
                # TODO: timeouts should be configurable
                response = self._session.get(url, headers=headers, timeout=10)

            case HttpMethod.POST:
                response = self._session.post(url, json=payload, headers=headers, timeout=10)

            case HttpMethod.PUT:
                response = self._session.put(url, json=payload, headers=headers, timeout=10)

            case HttpMethod.DELETE:
                response = self._session.delete(url, headers=headers, timeout=10)

            case _:
                raise ClientException(f"Unsupported HTTP method: {method}")

        if not response:
            raise ClientException("Unable to execute the request.")

        try:
            response.raise_for_status()
            if to_json:
                return deserialize_body(response.text)
            else:
                return response.text
        except Exception as e:
            raise ClientException(str(e))
