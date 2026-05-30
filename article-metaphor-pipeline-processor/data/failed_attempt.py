from dataclasses import dataclass

from data.processing_milestone import ProcessingMilestone


@dataclass
class FailedAttempt:
    time: int
    error: str
    failed_at_milestone: ProcessingMilestone
