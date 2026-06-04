from db.client.mongodb_client import MongoDBClient
from db.repository.base_repository import BaseRepository
from model.chunk_processing_state import ChunkProcessingState


class ChunkProcessingStateRepository(BaseRepository):
    def __init__(self, db_client: MongoDBClient, collection_name: str):
        super().__init__(db_client, collection_name, ChunkProcessingState)

    def find_processing_state(self, chunk_id: str) -> ChunkProcessingState:
        return self.find({"chunk_id": chunk_id})

    def save_chunk_processing_state(self, chunk_processing_state: ChunkProcessingState) -> ChunkProcessingState:
        return self.save(
            {"chunk_id": chunk_processing_state.chunk_id},
            chunk_processing_state.to_dict()
        )
