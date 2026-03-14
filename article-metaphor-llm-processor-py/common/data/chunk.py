from dataclasses import dataclass

from data.message import InputMessage


@dataclass
class Chunk(InputMessage):
    chunk_id: str
    document_id: str
    text: str

    def __init__(self, chunk_id: str, document_id: str, text: str) -> None:
        self.chunk_id = chunk_id
        self.document_id = document_id
        self.text = text
