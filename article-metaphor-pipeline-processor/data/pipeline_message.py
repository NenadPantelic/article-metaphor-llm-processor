import enum
from abc import ABC


class ReprocessingType(enum.Enum):
    USER_REQUESTED = 0
    ATTEMPT_AFTER_FAILURE = 1


class PipelineMessage(ABC):
    def __init__(self, chunk_id: str):
        self._chunk_id = chunk_id

    @property
    def chunk_id(self) -> str:
        return self._chunk_id


class ProcessingMessage(PipelineMessage):
    def __init__(self, chunk_id: str, text: str):
        super().__init__(chunk_id)
        self._text = text

    @property
    def text(self) -> str:
        return self._text


class ReprocessingMessage(PipelineMessage):
    def __init__(self, chunk_id: str, type_of_reprocessing: ReprocessingType):
        super().__init__(chunk_id)
        self._type_of_reprocessing = type_of_reprocessing

    @property
    def type_of_reprocessing(self) -> ReprocessingType:
        return self._type_of_reprocessing
