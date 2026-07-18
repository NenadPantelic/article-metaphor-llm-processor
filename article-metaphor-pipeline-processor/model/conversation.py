from dataclasses import dataclass
from datetime import datetime


@dataclass
class Conversation:
    _id: str
    document_id: str
    conversation_id: str
    created_at: datetime
    updated_at: datetime

    def __init__(self, document_id: str, conversation_id: str, created_at: datetime, updated_at: datetime):
        self.document_id = document_id
        self.conversation_id = conversation_id
        self.created_at = created_at
        self.updated_at = updated_at

    def to_dict(self) -> dict:
        serialized = {
            "document_id": self.document_id,
            "conversation_id": self.conversation_id,
            "created_at": self.created_at.isoformat(),
            "updated_at": self.updated_at.isoformat(),
        }

        if self._id:
            serialized["_id"] = self._id

        return serialized
