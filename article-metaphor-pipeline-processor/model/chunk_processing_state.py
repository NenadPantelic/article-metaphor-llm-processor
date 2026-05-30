from dataclasses import dataclass, field
from datetime import datetime

from data.processing_milestone import ProcessingMilestone
from model.processing_data import ProcessingData


@dataclass
class ChunkProcessingError:
    error: str
    executed_at: datetime
    failed_at_milestone: ProcessingMilestone = None


@dataclass
class ChunkProcessingState:
    chunk_id: str
    last_executed_milestone: ProcessingMilestone = None
    last_execution_timestamp: datetime = None
    failed_on_last_execution: bool = False
    data: dict[ProcessingMilestone, ProcessingData] = field(default_factory=dict)  # milestone -> dict
    errors: list[ChunkProcessingError] = field(default_factory=list)

    def to_dict(self) -> dict:
        return {
            "chunk_id": self.chunk_id,
            "last_execution_timestamp": self.last_execution_timestamp,
            "last_executed_milestone": self.last_executed_milestone,
            "failed_on_last_execution": self.failed_on_last_execution,
            "data": self.data,
            "errors": self.errors,
        }
