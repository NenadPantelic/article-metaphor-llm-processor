import enum


class ProcessingMilestone(enum.Enum):
    STARTED = 0
    LEXICAL_UNIT_PROCESSING = 1
    LEMMA_MEANING_LOOKUP = 2
    METAPHOR_ANALYSIS = 3
    RESULT_ASSEMBLY = 4
    COMPLETE = 5
