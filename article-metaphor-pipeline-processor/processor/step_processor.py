from abc import abstractmethod, ABC

from data.processing_milestone import ProcessingMilestone
from model.processing_data import ProcessingData


class StepProcessor(ABC):
    def __init__(self, milestone: ProcessingMilestone):
        self._milestone = milestone

    @property
    def milestone(self) -> ProcessingMilestone:
        return self._milestone

    @abstractmethod
    def execute(self, message: ProcessingData, **kwargs) -> ProcessingData:
        pass
