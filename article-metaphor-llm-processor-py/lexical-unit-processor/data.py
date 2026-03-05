from dataclasses import dataclass
from typing import Any
from json import dumps

from rabbitmq.serializable_message import SerializableMessage


@dataclass
class Chunk:
    chunk_id: str
    document_id: str
    text: str


@dataclass
class LexicalUnitData(SerializableMessage):
    chunk_id: str
    document_id: str
    sentences: list[dict[str, Any]]
    lexical_units: list[dict[str, Any]]
    unique_lemmas: list[dict[str, Any]]

    def serialize(self) -> bytes:
        data = {
            "chunk_id": self.chunk_id,
            "document_id": self.document_id,
            "sentences": self.sentences,
            "lexical_units": self.lexical_units,
            "unique_lemmas": self.unique_lemmas
        }
        return dumps(data)
