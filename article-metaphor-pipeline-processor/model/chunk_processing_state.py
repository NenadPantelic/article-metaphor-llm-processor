from dataclasses import dataclass, field
from datetime import datetime

from bson import ObjectId

from data.processing_milestone import ProcessingMilestone


@dataclass
class ChunkProcessingError:
    error: str
    executed_at: datetime
    failed_at_milestone: ProcessingMilestone = None


@dataclass
class ChunkProcessingState:
    _id: str
    chunk_id: str
    last_executed_milestone: ProcessingMilestone = None
    last_execution_timestamp: datetime = None
    failed_on_last_execution: bool = False
    data: dict[str, dict] = field(default_factory=dict)  # milestone -> dict
    errors: list[ChunkProcessingError] = field(default_factory=list)

    def __init__(self, _id: ObjectId = None, chunk_id: str = "", last_executed_milestone: ProcessingMilestone = None,
                 last_execution_timestamp: datetime = None, failed_on_last_execution: bool = False,
                 data: dict[str, dict] = None, errors: list[ChunkProcessingError] = None):
        self._id = str(_id) if _id else None
        self.chunk_id = chunk_id
        self.last_executed_milestone = last_executed_milestone
        self.last_execution_timestamp = last_execution_timestamp
        self.failed_on_last_execution = failed_on_last_execution
        self.data = data
        self.errors = errors

    def to_dict(self) -> dict:
        serialized = {
            "chunk_id": self.chunk_id,
            "last_execution_timestamp": self.last_execution_timestamp,
            "last_executed_milestone": self.last_executed_milestone.name if self.last_executed_milestone else None,
            "failed_on_last_execution": self.failed_on_last_execution,
            "data": self.data,
            "errors": self.errors,
        }

        if self._id:
            serialized["_id"] = self._id

        return serialized
