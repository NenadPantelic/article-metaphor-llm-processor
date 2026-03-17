from dataclasses import dataclass
from json import dumps
from typing import Any

from data.message import OutputMessage, InputMessage


@dataclass
class LexicalUnitData(InputMessage, OutputMessage):
    chunk_id: str
    document_id: str
    # sentences: list[dict[str, Any]]
    lexical_units: list[dict[str, Any]]
    unique_lemmas: list[dict[str, Any]]

    def serialize(self) -> bytes:
        return dumps(self.to_dict())

    def to_dict(self) -> dict:
        return {
            "chunk_id": self.chunk_id,
            "document_id": self.document_id,
            # "sentences": self.sentences,
            "lexical_units": self.lexical_units,
            "unique_lemmas": self.unique_lemmas
        }
