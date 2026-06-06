from pymongo import MongoClient

from config.config_properties import DatabaseConfig
from exception.database_exception import DatabaseException
from helper.singleton import SingletonMeta


class MongoDBClient(metaclass=SingletonMeta):
    def __init__(self, db_config: DatabaseConfig):
        self._client = MongoClient(
            host=db_config.host,
            port=db_config.port,
            username=db_config.username,
            password=db_config.password,
            authSource="admin",  # The database where the user is defined
            authMechanism="SCRAM-SHA-256"  # Default for MongoDB 4.0+
        )
        self._db = self._client[db_config.database]

    def get_collection_handler(self, collection_name):
        try:
            return self._db[collection_name]
        except KeyError:
            raise DatabaseException(f"Collection {collection_name} does not exist")

    def close(self):
        if self._client:
            self._client.close()
