from typing import List

from config.logconfig import get_logger
from db.client.mongodb_client import MongoDBClient
from db.repository.base_repository import BaseRepository
from model.conversation import Conversation

log = get_logger()


class ConversationRepository(BaseRepository):
    def __init__(self, db_client: MongoDBClient, collection_name: str):
        super().__init__(db_client, collection_name, Conversation)

    def find_by_document_id(self, doc_id: str) -> Conversation:
        return self.find_one({"document_id": doc_id})

    def find_all_conversations(self) -> List[Conversation]:
        log.debug(f"Getting all conversations")
        return self.find()

    def save_conversation(self, conversation: Conversation) -> Conversation:
        return self.save(
            {"conversation_id": conversation.conversation_id},
            conversation.to_dict()
        )
