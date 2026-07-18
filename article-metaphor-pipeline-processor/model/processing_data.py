import abc
from dataclasses import dataclass
from datetime import datetime
from typing import Any

from util.time_util import utc_now


class ProcessingData(abc.ABC):
    execution_time: datetime  # timestamp to int??

    def __init__(self, execution_time: datetime = None) -> None:
        self.execution_time = execution_time or utc_now()
        # int(timestamp.timestamp())

    def to_dict(self) -> dict:
        return {
            "execution_time": self.execution_time,
        }


@dataclass
class RawMessage(ProcessingData):
    text: str

    def __init__(self, text: str) -> None:
        super().__init__()
        self.text = text

    def to_dict(self) -> dict:
        return {
            "text": self.text,
            "execution_time": self.execution_time,
        }


@dataclass
class LexicalUnitProcessingData(ProcessingData):
    lexical_units: list
    unique_lemmas: list

    def __init__(self, lexical_units: list = None, unique_lemmas: list = None, execution_time: datetime = None):
        super().__init__(execution_time)
        self.lexical_units = lexical_units
        self.unique_lemmas = unique_lemmas

    def to_dict(self) -> dict:
        return {
            "lexical_units": self.lexical_units,
            "unique_lemmas": self.unique_lemmas,
            "execution_time": self.execution_time,
        }


@dataclass
class LemmaExplanations:
    lemma: str
    cambridge_explanations: list[str]
    ldoce_explanations: list[str]

    def to_dict(self) -> dict[str, Any]:
        return {
            "lemma": self.lemma,
            "cambridge_explanations": self.cambridge_explanations,
            "ldoce_explanations": self.ldoce_explanations,
        }


@dataclass
class LemmasWithExplanations(ProcessingData):
    lemmas_explanations: list[LemmaExplanations]

    def __init__(self, lemmas_explanations: list = None, execution_time: datetime = None):
        super().__init__(execution_time)
        self.lemmas_explanations = lemmas_explanations

    def to_dict(self) -> dict[str, Any]:
        return {
            "explanations": self.lemmas_explanations,
        }
