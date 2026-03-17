from abc import ABC, abstractmethod
from json import dumps


class InputMessage(ABC):
    def __init__(self, chunk_id: str, document_id: str, **kwargs):
        self.chunk_id = chunk_id
        self.document_id = document_id


class OutputMessage(ABC):
    def __init__(self, chunk_id: str, document_id: str, **kwargs):
        self.chunk_id = chunk_id
        self.document_id = document_id

    def serialize(self) -> bytes:
        return dumps(self.to_dict())

    @abstractmethod
    def to_dict(self) -> dict:
        pass
