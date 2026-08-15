from dataclasses import dataclass
from datetime import datetime


@dataclass
class Conversation:
    _id: str
    document_id: str
    conversation_id: str
    created_at: datetime
    updated_at: datetime

    def __init__(self, _id: str, document_id: str, conversation_id: str, created_at: datetime, updated_at: datetime):
        self._id = _id
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


class ConversationBuilder:
    def __init__(self):
        self._id = None
        self.document_id = None
        self.conversation_id = None
        self.created_at = None
        self.updated_at = None

    @staticmethod
    def new_builder() -> "ConversationBuilder":
        return ConversationBuilder()

    def with_id(self, _id: str):
        self._id = _id
        return self

    def with_document_id(self, document_id: str):
        self.document_id = document_id
        return self

    def with_conversation_id(self, conversation_id: str):
        self.conversation_id = conversation_id
        return self

    def with_created_at(self, created_at: datetime):
        self.created_at = created_at
        return self

    def with_updated_at(self, updated_at: datetime):
        self.updated_at = updated_at
        return self

    def build(self) -> Conversation:
        return Conversation(self._id, self.document_id, self.conversation_id, self.created_at, self.updated_at)
