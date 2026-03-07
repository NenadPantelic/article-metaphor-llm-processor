from dataclasses import dataclass
from enum import Enum
from typing import Any


class ChunkProcessingState(Enum):
    LEXICAL_UNIT_PROCESSING__IN_PROGRESS = "LEXICAL_UNIT_PROCESSING__IN_PROGRESS"
    LEXICAL_UNIT_PROCESSING__COMPLETE = "LEXICAL_UNIT_PROCESSING__COMPLETE"
    LEXICAL_UNIT_PROCESSING__FAILED = "LEXICAL_UNIT_PROCESSING__FAILED"

    DICTIONARY_ACCESS__IN_PROGRESS = "DICTIONARY_ACCESS__IN_PROGRESS"
    DICTIONARY_ACCESS__COMPLETE = "DICTIONARY_ACCESS__COMPLETE"
    DICTIONARY_ACCESS__FAILED = "DICTIONARY_ACCESS__FAILED"


@dataclass
class ChunkProcessingStateUpdate:
    chunk_id: str
    document_id: str
    state: ChunkProcessingState = None
    should_be_reprocessed: bool = False
    payload: dict[str, Any] | None = None

    def to_dict(self) -> dict[str, Any]:
        return {
            "chunk_id": self.chunk_id,
            "document_id": self.document_id,
            "state": self.state.value,
            "should_be_reprocessed": self.should_be_reprocessed,
            "payload": self.payload,
        }
