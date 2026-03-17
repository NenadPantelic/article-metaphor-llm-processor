from typing import Any

from client.http_client import HttpClient, HttpMethod
from data.chunk_processing_state import ChunkProcessingStateUpdate

_X_SERVICE_KEY_HEADER = "X-service-key"
_CONTENT_TYPE_HEADER = "Content-Type"
_APPLICATION_JSON = "application/json"


class ChunkProcessingStateApiClient(HttpClient):
    def __init__(self, scheme="http", host="127.0.0.1", port="", api_base_path="", headers=None, **kwargs):
        req_headers = headers or {}
        req_headers[_X_SERVICE_KEY_HEADER] = kwargs.get("service_key", "")
        req_headers[_CONTENT_TYPE_HEADER] = _APPLICATION_JSON
        super().__init__(scheme, host, port, api_base_path, req_headers)

    def get_chunk_processing_state(self, chunk_id: str, stage: str) -> dict[str, Any]:
        endpoint = f"/api/v1/processing-chunks/{chunk_id}/{stage}"
        return self._call(HttpMethod.GET, endpoint)

    def update_chunk_processing_state(self, chunk_processing_state_update: ChunkProcessingStateUpdate):
        endpoint = f"/api/v1/processing-chunks/{chunk_processing_state_update.chunk_id}"
        return self._call(HttpMethod.PUT, endpoint, body=chunk_processing_state_update.to_dict())
