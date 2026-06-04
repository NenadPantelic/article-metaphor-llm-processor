from pymongo import MongoClient

from exception.database_exception import DatabaseException
from helper.singleton import SingletonMeta


class MongoDBClient(metaclass=SingletonMeta):
    def __init__(self, host, port, db_name):
        self._client = MongoClient(f"mongodb://{host}:{port}")
        self._db = self._client[db_name]

    def get_collection_handler(self, collection_name):
        try:
            return self._db[collection_name]
        except KeyError:
            raise DatabaseException(f"Collection {collection_name} does not exist")

    def close(self):
        if self._client:
            self._client.close()
