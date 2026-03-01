from dataclasses import dataclass

@dataclass
class Chunk:
    chunk_id: str
    document_id: str
    text: str

    def serialize(self):
        return {
            "chunk_id": self.chunk_id,
            "document_id": self.document_id,
        }
