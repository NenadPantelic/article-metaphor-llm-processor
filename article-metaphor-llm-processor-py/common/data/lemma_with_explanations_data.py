from dataclasses import dataclass
from typing import List, Any

from data.message import OutputMessage, InputMessage


@dataclass
class LemmaExplanations:
    lemma: str
    cambridge_explanation: List[str]
    ldoce_explanation: List[str]


@dataclass
class LemmaWithExplanationsData(InputMessage, OutputMessage):
    chunk_id: str
    document_id: str
    surface: str
    explanations: List[LemmaExplanations]

    def to_dict(self) -> dict[str, Any]:
        return {
            "chunk_id": self.chunk_id,
            "document_id": self.document_id,
            "surface": self.surface,
            "explanations": self.explanations,
        }
