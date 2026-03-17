from abc import ABC, abstractmethod
from typing import Any

from data.message import InputMessage


class ProcessorService(ABC):
    def __init__(self):
        pass

    @abstractmethod
    def process(self, input_message: InputMessage) -> dict[str, Any]:
        pass
