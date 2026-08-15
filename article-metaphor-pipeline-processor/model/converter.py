from typing import Any

from data.processing_milestone import ProcessingMilestone
from exception.processor_exception import ProcessorException
from model.processing_data import RawMessage, ProcessingData, LexicalUnitProcessingData, LemmasWithExplanations, \
    ArticleMetaphorAnalysis, AnalyzedText

PROCESSING_DATA_CONVERSION_MAPPING = {
    ProcessingMilestone.STARTED.name: RawMessage,
    ProcessingMilestone.LEXICAL_UNIT_PROCESSING.name: LexicalUnitProcessingData,
    ProcessingMilestone.LEMMA_MEANING_LOOKUP.name: LemmasWithExplanations,
    ProcessingMilestone.METAPHOR_ANALYSIS.name: ArticleMetaphorAnalysis,
    ProcessingMilestone.RESULT_ASSEMBLY.name: AnalyzedText,
}


class DataConverter:
    @staticmethod
    def from_dict(milestone: str, serialized_data: dict[str, Any]) -> ProcessingData:
        target_type = PROCESSING_DATA_CONVERSION_MAPPING.get(milestone)
        if target_type is None:
            raise ProcessorException(f"Unable to map given data with milestone {milestone}")

        if isinstance(serialized_data, target_type):
            return serialized_data

        return target_type(**serialized_data)
