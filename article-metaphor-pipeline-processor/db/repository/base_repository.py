from typing import Any

from pymongo import ReturnDocument

from config.logconfig import LogConfig
from db.client.mongodb_client import MongoDBClient

log = LogConfig.default(__name__, __file__)


class BaseRepository:
    def __init__(self, db_client: MongoDBClient, collection_name: str, entity_type: type[Any]):
        self._db_client = db_client
        self._collection = self._db_client.get_collection_handler(collection_name)
        self._entity_type = entity_type
        self._entity_name = self._entity_type.__name__

    def find(self, query: dict):
        log.debug(f"Trying to find a {self._entity_name} by: {query}")
        record = self._collection.find_one(query)
        log.debug(f"Found: {record}")
        return self._entity_type(**record) if record else None

    def save(self, query: dict, updated_record: dict):
        log.debug(f"Upserting the {self._entity_name}. Query = {query}, new record = {updated_record}")

        update_date = {"$set": updated_record}
        updated_document = self._collection.find_one_and_update(query, update_date, upsert=True,
                                                                return_document=ReturnDocument.AFTER)

        return self._entity_type(**updated_document) if updated_document else None

    def close(self):
        self._db_client.close()
