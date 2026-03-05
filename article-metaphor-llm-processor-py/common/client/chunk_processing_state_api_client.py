from enum import Enum
from typing import Any

from requests import Session

from config.config_data import ChunkProcessingStateApiClientConfig
from data.chunk_processing_state import ChunkProcessingStateUpdate
from exception.client_exception import ClientException
from helper.serialization import serialize_body, deserialize_body

X_SERVICE_KEY_HEADER = "X-service-key"


class HttpMethod(Enum):
    GET = "GET"
    POST = "POST"
    PUT = "PUT"
    DELETE = "DELETE"


_CONTENT_TYPE_HEADER = "Content-Type"
_APPLICATION_JSON = "application/json"


class ChunkProcessingStateApiClient:
    def __init__(self, chunk_processing_api_client_config: ChunkProcessingStateApiClientConfig, api_base_path=""):
        self._session = Session()

        self._session.headers.update({
            X_SERVICE_KEY_HEADER: chunk_processing_api_client_config.service_key,
            _CONTENT_TYPE_HEADER: _APPLICATION_JSON
        })
        self._base_api_path = api_base_path

    def get_chunk_processing_state(self, chunk_id: str, stage: str) -> dict[str, Any]:
        endpoint = f"/api/v1/processing-chunks/{chunk_id}/{stage}"
        return self._call(HttpMethod.GET, endpoint)

    def update_chunk_processing_state(self, chunk_processing_state_update: ChunkProcessingStateUpdate):
        endpoint = f"/api/v1/processing-chunks/{chunk_processing_state_update.chunk_id}"
        return self._call(HttpMethod.PUT, endpoint, body=chunk_processing_state_update.to_dict())

    def _call(self, method: HttpMethod, endpoint: str, body: dict[str, Any] = None, headers=None) -> dict[str, Any]:
        url = f"{self._base_api_path}/{endpoint}" if self._base_api_path else endpoint
        payload = serialize_body(body)

        response = None
        match method:
            case HttpMethod.GET:
                response = self._session.get(url, headers=headers)

            case HttpMethod.POST:
                response = self._session.post(url, json=payload, headers=headers)

            case HttpMethod.PUT:
                response = self._session.put(url, json=payload, headers=headers)

            case HttpMethod.DELETE:
                response = self._session.delete(url, headers=headers)

            case _:
                raise ClientException(f"Unsupported HTTP method: {method}")

        if not response:
            raise ClientException("Unable to execute the request.")

        try:
            response.raise_for_status()
            return deserialize_body(response.text)
        except Exception as e:
            raise ClientException(str(e))
