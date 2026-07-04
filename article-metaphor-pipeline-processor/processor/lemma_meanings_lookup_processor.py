from config.logconfig import get_logger
from data.processing_milestone import ProcessingMilestone
from model.processing_data import LexicalUnitProcessingData, LemmasWithExplanations, LemmaExplanations
from processor.step_processor import StepProcessor
from service.dictionary_access_service import DictionaryAccessService

log = get_logger()


class LemmaMeaningsLookupProcessor(StepProcessor):
    def __init__(self):
        super().__init__(ProcessingMilestone.LEMMA_MEANING_LOOKUP)
        self._dictionary_access_service = DictionaryAccessService()

    def execute(self, message: LexicalUnitProcessingData) -> LemmasWithExplanations:
        log.info(f"[lemma-meaning-lookup] Processing {message}")
        lemma_meanings = self._dictionary_access_service.lookup_basic_lemma_meanings(message.unique_lemmas)

        log.info(f"Basic lemma meanings: {lemma_meanings}")
        lemma_explanations_list = []

        for lemma, meanings in lemma_meanings.items():
            lemma_explanations = LemmaExplanations(lemma, cambridge_explanation=meanings.get("cambridge"),
                                                   ldoce_explanation=meanings.get("ldoce"))
            lemma_explanations_list.append(lemma_explanations)

        return LemmasWithExplanations(lemma_explanations_list)
