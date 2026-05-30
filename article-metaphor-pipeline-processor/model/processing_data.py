import abc
from dataclasses import dataclass
from datetime import datetime

from util.time_util import utc_now


class ProcessingData(abc.ABC):
    execution_time: datetime

    def __init__(self, execution_time: datetime = None) -> None:
        self.execution_time = execution_time or utc_now()

    def to_dict(self) -> dict:
        return {
            "execution_time": self.execution_time,
        }


@dataclass
class RawMessage(ProcessingData):
    text: str

    def __init__(self, text: str) -> None:
        super().__init__()
        self._text = text

    def to_dict(self) -> dict:
        return {
            "text": self.text,
            "execution_time": self.execution_time,
        }
