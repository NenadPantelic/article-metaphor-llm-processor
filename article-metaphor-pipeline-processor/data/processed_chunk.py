from dataclasses import dataclass
from model.chunk_processing_state import ChunkProcessingError


@dataclass
class ProcessedChunk:
    chunk_id: str
    error: ChunkProcessingError
