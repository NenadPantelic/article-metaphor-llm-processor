from dataclasses import dataclass
from typing import List

from rabbitmq.serializable_message import SerializableMessage


@dataclass
class LemmaWithExplanationsData(SerializableMessage):
    chunk_id: str
    document_id: str
    surface: str
    lemma: str
    cambridge_explanation: List[str]
    ldoce_explanation: List[str]

    def serialize(self) -> bytes:
        return {
            "chunk_id": self.chunk_id,
            "document_id": self.document_id,
            "surface": self.surface,
            "lemma": self.lemma,
            "cambridge_explanation": self.cambridge_explanation,
            "ldoce_explanation": self.ldoce_explanation
        }
