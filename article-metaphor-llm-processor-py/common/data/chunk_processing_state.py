from dataclasses import dataclass
from enum import Enum
from typing import Any


class ChunkProcessingState(Enum):
    LEXICAL_UNIT_PROCESSING__IN_PROGRESS = "LEXICAL_UNIT_PROCESSING__IN_PROGRESS"
    LEXICAL_UNIT_PROCESSING__COMPLETE = "LEXICAL_UNIT_PROCESSING__COMPLETE"
    LEXICAL_UNIT_PROCESSING__FAILED = "LEXICAL_UNIT_PROCESSING__FAILED"

    LEMMA_MEANING_LOOKUP__IN_PROGRESS = "LEMMA_MEANING_LOOKUP__IN_PROGRESS"
    LEMMA_MEANING_LOOKUP__COMPLETE = "LEMMA_MEANING_LOOKUP__COMPLETE"
    LEMMA_MEANING__FAILED = "LEMMA_MEANING__FAILED"

    METAPHOR_ANALYSIS__IN_PROGRESS = "METAPHOR_ANALYSIS__IN_PROGRESS"
    METAPHOR_ANALYSIS__COMPLETE = "METAPHOR_ANALYSIS__COMPLETE"
    METAPHOR_ANALYSIS__FAILED = "METAPHOR_ANALYSIS__FAILED"


@dataclass
class ChunkProcessingError:
    error: str
    reprocessable: bool

    def to_dict(self):
        return {
            "error": self.error,
            "reprocessable": self.reprocessable,
        }


@dataclass
class ChunkProcessingStateUpdate:
    chunk_id: str
    document_id: str
    state: ChunkProcessingState = None
    data: dict[str, Any] | None = None  # it must contain field 'type'
    error: ChunkProcessingError = None
    processing_time: int = 0

    def to_dict(self) -> dict[str, Any]:
        return {
            "chunk_id": self.chunk_id,
            "document_id": self.document_id,
            "state": self.state.value,
            "data": self.data,
            "error": None if self.error is None else self.error.to_dict(),
            "processing_time": self.processing_time,
        }
