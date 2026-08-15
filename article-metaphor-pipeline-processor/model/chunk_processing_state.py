from dataclasses import dataclass, field
from datetime import datetime
from typing import Any

from bson import ObjectId

from data.processing_milestone import ProcessingMilestone
from model.converter import DataConverter
from model.processing_data import ProcessingData


@dataclass
class ChunkProcessingError:
    error: str
    executed_at: datetime
    failed_at_milestone: ProcessingMilestone = None
    reprocessable: bool = False

    def to_dict(self) -> dict[str, Any]:
        return {
            "error": self.error,
            "executed_at": self.executed_at.isoformat(),
            "failed_at_milestone": self.failed_at_milestone.name if self.failed_at_milestone else None,
            "reprocessable": self.reprocessable,
        }


@dataclass
class ChunkProcessingState:
    # _id: str
    chunk_id: str
    order: int
    document_id: str
    text: str
    last_executed_milestone: ProcessingMilestone
    last_executed_milestone: ProcessingMilestone = None
    last_execution_timestamp: datetime = None
    failed_on_last_execution: bool = False
    data: dict[str, ProcessingData] = field(default_factory=dict)  # milestone -> dict
    errors: list[ChunkProcessingError] = field(default_factory=list)

    def __init__(self, _id: ObjectId = None, chunk_id: str = "", order: int = 1, document_id: str = None,
                 text: str = None,
                 last_executed_milestone: ProcessingMilestone = None,
                 last_execution_timestamp: datetime = None, failed_on_last_execution: bool = False,
                 data: dict[str, dict[str, Any]] = None, errors: list[ChunkProcessingError] = None, ):
        # self._id = str(_id) if _id else None
        self.chunk_id = chunk_id
        self.order = order
        self.document_id = document_id
        self.text = text
        self.last_executed_milestone = last_executed_milestone
        self.last_execution_timestamp = last_execution_timestamp
        self.failed_on_last_execution = failed_on_last_execution
        self.data = {k: DataConverter.from_dict(k, v) for k, v in data.items()}
        self.errors = errors or []

    def to_dict(self) -> dict:
        serialized = {
            "chunk_id": self.chunk_id,
            "order": self.order,
            "document_id": self.document_id,
            "text": self.text,
            "last_execution_timestamp": self.last_execution_timestamp,
            "last_executed_milestone": self.last_executed_milestone.name if self.last_executed_milestone else None,
            "failed_on_last_execution": self.failed_on_last_execution,
            "data": {k: v.to_dict() for k, v in self.data.items() if v},
            "errors": [err.to_dict() for err in self.errors],
        }

        # if self._id:
        #     serialized["_id"] = self._id

        return serialized
