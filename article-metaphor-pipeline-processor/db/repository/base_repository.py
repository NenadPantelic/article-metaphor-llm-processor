from typing import Any

from pymongo import ReturnDocument

from config.logconfig import LogConfig
from db.client.mongodb_client import MongoDBClient
from exception.database_exception import DatabaseException

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
        return self._entity_type(**record)

    def save(self, query: dict, updated_record: dict,
                              raise_if_zero_matches: bool = True):
        log.debug(f"Upserting the {self._entity_name}. Query = {query}, new record = {updated_record}")

        update_date = {"$set": updated_record}
        updated_document = self._collection.find_one_and_update(query, update_date, upsert=True,
                                                                return_document=ReturnDocument.AFTER)

        if raise_if_zero_matches and updated_document.matched_count == 0:
            log.debug(f"No matching {self._entity_name} found for query: {query}")
            raise DatabaseException(f"No matching {self._entity_name} found for query: {query}")

        if raise_if_zero_matches and updated_document.modified_count == 0:
            log.debug(f"Record identified by query {query} has not been modified")
            raise DatabaseException(f"Record identified by query {query} has not been modified")

        return self._entity_type(**updated_document)

    def close(self):
        self._db_client.close()
