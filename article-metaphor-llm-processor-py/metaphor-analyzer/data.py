from dataclasses import dataclass
from enum import Enum

from data.message import OutputMessage


@dataclass
class Conversation:
    id: int
    doc_id: str
    conversation_id: str


class MetaphorType(Enum):
    DIRECT = "DIRECT"
    INDIRECT = "INDIRECT"
    IMPLICIT = "IMPLICIT"


@dataclass
class Metaphor(OutputMessage):
    chunk_id: str
    document_id: str
    type: str = None
    phrase: str = None
    offset: int = 0
    metaphor_type: MetaphorType = MetaphorType.DIRECT
    explanation: str = None

    def to_dict(self) -> dict:
        return {
            "chunk_id": self.chunk_id,
            "document_id": self.document_id,
            "type": self.type,
            "phrase": self.phrase,
            "offset": self.offset,
            "metaphor_type": self.metaphor_type.value,
            "explanation": self.explanation
        }
