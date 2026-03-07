from dataclasses import dataclass
from typing import Any
from json import dumps

from rabbitmq.serializable_message import SerializableMessage


@dataclass
class Chunk:
    chunk_id: str
    document_id: str
    text: str

