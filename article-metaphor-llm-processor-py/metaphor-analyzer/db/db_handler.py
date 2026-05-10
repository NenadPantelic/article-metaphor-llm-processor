import sqlite3
from typing import List

from config.logconfig import LogConfig
from ..data import Conversation
from ..config import DBConfig

log = LogConfig.default(__name__, "metaphor-analyzer")


class ConversationDBHandler:
    INSERT_STMTNT = "INSERT INTO conversations (doc_id, conversation_id) VALUES (?, ?)"
    QUERY_BY_DOC_ID = "SELECT * FROM conversation WHERE doc_id = ?"
    QUERY_ALL = "SELECT * FROM conversation"

    def __init__(self, db_config: DBConfig):
        self._connection = sqlite3.connect(db_config.database)
        self._cursor = self._connection.cursor()
        self._connection.row_factory = sqlite3.Row

    def close(self):
        if self._connection:
            log.info("Closing database connection")
            self._connection.close()

    def insert_conversation(self, doc_id: str, conversation_id: str):
        log.debug(f"Inserting conversation into database: doc_id = {doc_id}, conversation_id = {conversation_id}")
        try:
            self._cursor.execute(ConversationDBHandler.INSERT_STMTNT, (doc_id, conversation_id))
            self._connection.commit()
        except sqlite3.Error as e:
            log.error(f"An error occurred when inserting a record into the database: {e}")
            self._connection.rollback()

    def get_all_conversations(self) -> List[Conversation]:
        log.debug(f"Getting all conversations")
        self._cursor.execute(ConversationDBHandler.QUERY_ALL)

        all_conversations = self._cursor.fetchall()
        return [Conversation(**conv) for conv in all_conversations]

    def get_conversation(self, doc_id: str) -> Conversation:
        log.debug(f"Getting conversation from database: doc_id = {doc_id}")
        self._cursor.execute(ConversationDBHandler.QUERY_BY_DOC_ID, (doc_id,))
        row = self._cursor.fetchone()
        if not row:
            return None

        return Conversation(**row)
