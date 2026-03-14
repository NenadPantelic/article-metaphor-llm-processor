from typing import List, Dict, Any
import spacy
from flashtext import KeywordProcessor
import uuid

from common.data.chunk import Chunk
from processor.processor_service import ProcessorService

# POS tags considered lexical units under MIPVU
CONTENT_POS = {"NOUN", "VERB", "ADJ", "ADV"}

# ---------------------------
# NLP Model
# ---------------------------
# Check FlashText algorith
nlp = spacy.load("en_core_web_sm", disable=["ner"])  # NER not needed here
# it is a curated list of fixed or semi-fixed multi-word lexical units that:
# * behave as one conceptual unit
# * often trigger metaphorical readings
# * would be misclassified if treated token-by-token
# * have dictionary entries or idiomatic meanings
# Examples:
# 1. on fire → idiomatic state
# 2. out of control → abstract state
# 3. grasp the idea → metaphorical cognitive action
# start with empty MWE
DEFAULT_MWES = []


def preprocess_text(text: str, mwe_list: List[str] = None) -> Dict[str, Any]:
    """
    Perform Step 2 preprocessing.

    Returns:
    {
        "sentences": [...],
        "lexical_units": [...],
        "unique_lemmas": [...]
    }
    """

    if mwe_list is None:
        mwe_list = DEFAULT_MWES

    doc = nlp(text)

    # ---------------------------
    # 2.1 Sentence Segmentation
    # ---------------------------
    sentences = []
    for i, sent in enumerate(doc.sents):
        sentences.append(
            {
                "sentence_id": i,
                "text": sent.text,
                "offset_start": sent.start_char,
                "offset_end": sent.end_char,
            }
        )

    # ---------------------------
    # 2.5 Multi-Word Expression Detection
    # ---------------------------
    mwe_processor = KeywordProcessor(case_sensitive=False)
    for phrase in mwe_list:
        mwe_processor.add_keyword(phrase)

    mwe_matches = []
    for phrase, start, end in mwe_processor.extract_keywords(text, span_info=True):
        mwe_matches.append(
            {
                "surface": phrase,
                "lemma": phrase.split()[-1].lower(),
                "pos": "MWE",
                "offset_start": start,
                "offset_end": end,
            }
        )

    # ---------------------------
    # 2.2–2.7 Token-based LU Extraction
    # ---------------------------
    lexical_units = []

    for token in doc:
        if token.pos_ not in CONTENT_POS:
            continue
        if token.is_space or token.is_punct:
            continue

        lu = {
            "lu_id": f"lu_{uuid.uuid4().hex[:8]}",
            "surface": token.text,
            "lemma": token.lemma_.lower(),
            "pos": token.pos_,
            "offset_start": token.idx,
            "offset_end": token.idx + len(token.text),
            "sentence_id": next(
                s["sentence_id"]
                for s in sentences
                if token.idx >= s["offset_start"] and token.idx < s["offset_end"]
            ),
        }
        lexical_units.append(lu)

    # ---------------------------
    # Merge MWE LUs
    # ---------------------------
    for mwe in mwe_matches:
        mwe["lu_id"] = f"lu_{uuid.uuid4().hex[:8]}"
        mwe["sentence_id"] = next(
            s["sentence_id"]
            for s in sentences
            if mwe["offset_start"] >= s["offset_start"]
            and mwe["offset_end"] <= s["offset_end"]
        )
        lexical_units.append(mwe)

    # ---------------------------
    # Deduplicate overlapping LUs
    # ---------------------------
    seen_offsets = set()
    unique_lus = []
    for lu in sorted(lexical_units, key=lambda x: (x["offset_start"], x["offset_end"])):
        key = (lu["offset_start"], lu["offset_end"])
        if key not in seen_offsets:
            seen_offsets.add(key)
            unique_lus.append(lu)

    # ---------------------------
    # 2.8 Unique Lemma Extraction
    # ---------------------------
    unique_lemmas = sorted({lu["lemma"] for lu in unique_lus})

    return {
        "sentences": sentences,
        "lexical_units": unique_lus,
        "unique_lemmas": unique_lemmas,
    }


# ---------------------------
# Example usage
# ---------------------------


class LexicalUnitTextPreprocessor(ProcessorService):
    def __init__(self):
        # TODO: check NER
        self._nlp = spacy.load("en_core_web_sm", disable=["ner"])  # NER not needed here

    # ---------------------------
    # 1. Sentence Segmentation
    # ---------------------------
    def do_sentence_segmentation(self, doc) -> List[Dict[str, Any]]:
        """
        Do the sentence segmentation.
        @param: text: text to be segmented
        @return: list of sentence segmentation results
        """
        sentences = []
        for i, sent in enumerate(doc.sents):
            sentences.append(
                {
                    "sentence_id": i,
                    "text": sent.text,
                    "offset_start": sent.start_char,
                    "offset_end": sent.end_char,
                }
            )

        return sentences

    # ---------------------------
    # 2. Multi-Word Expression Detection
    # ---------------------------
    def do_mwe_detection(self, text: str, mwe_list: List[str]) -> List[Dict[str, Any]]:
        """
        Do the MWE detection.
        @param: text: text to be detected
        @return: list of MWE detection results
        """
        mwe_processor = KeywordProcessor(case_sensitive=False)
        for phrase in mwe_list:
            mwe_processor.add_keyword(phrase)

        mwe_matches = []
        for phrase, start, end in mwe_processor.extract_keywords(text, span_info=True):
            mwe_matches.append(
                {
                    "surface": phrase,
                    "lemma": phrase.split()[-1].lower(),
                    "pos": "MWE",
                    "offset_start": start,
                    "offset_end": end,
                }
            )

        return mwe_matches

    # ---------------------------
    # 3. Token-based LU Extraction
    # ---------------------------
    def do_token_based_lu_extraction(
            self, doc: spacy.Language, sentences: List[Dict[str, Any]]
    ) -> List[Dict[str, Any]]:
        """
        Do token-based LU extraction.
        @param: doc: spaCy document
        @param: sentences: list of sentences
        @return: list of token-based LU extraction results
        """
        lexical_units = []

        for token in doc:
            if token.pos_ not in CONTENT_POS:
                continue

            # skip spaces and punctuations
            if token.is_space or token.is_punct:
                continue

            lu = {
                "lu_id": f"lu_{uuid.uuid4().hex[:8]}",
                "surface": token.text,
                "lemma": token.lemma_.lower(),
                "pos": token.pos_,
                "offset_start": token.idx,
                "offset_end": token.idx + len(token.text),
                "sentence_id": next(
                    s["sentence_id"]
                    for s in sentences
                    if s["offset_start"] <= token.idx < s["offset_end"]
                ),
            }
            lexical_units.append(lu)

        return lexical_units

    # ---------------------------
    # Extract MWE LUs
    # ---------------------------
    def extract_mwe_lexical_units(
            self, mwe_matches: List[Dict[str, Any]], sentences: List[str]
    ) -> List[Dict[str, Any]]:
        """
        Extract MWE lexical units.
        :param mwe_matches:
        :param sentences:
        :return: list of MWE lexical units
        """
        lexical_units = []

        for mwe in mwe_matches:
            mwe["lu_id"] = f"lu_{uuid.uuid4().hex[:8]}"
            mwe["sentence_id"] = next(
                s["sentence_id"]
                for s in sentences
                if mwe["offset_start"] >= s["offset_start"]
                and mwe["offset_end"] <= s["offset_end"]
            )
            lexical_units.append(mwe)

        return lexical_units

    # ---------------------------
    # Deduplicate overlapping LUs
    # ---------------------------
    def deduplicate_lexical_units(
            self, lexical_units: List[Dict[str, Any]]
    ) -> List[Dict[str, Any]]:
        seen_offsets = set()
        unique_lus = []
        for lu in sorted(
                lexical_units, key=lambda x: (x["offset_start"], x["offset_end"])
        ):
            key = (lu["offset_start"], lu["offset_end"])
            if key not in seen_offsets:
                seen_offsets.add(key)
                unique_lus.append(lu)

        return unique_lus

    # ---------------------------
    # Unique Lemma Extraction
    # ---------------------------
    def get_unique_lemmas(
            self, unique_lexical_units: List[Dict[str, Any]]
    ) -> List[Dict[str, Any]]:
        """
        Get the unique lemmas from lexical units.
        :param unique_lexical_units:
        :return: list of unique lemmas
        """
        return sorted({lu["lemma"] for lu in unique_lexical_units})

    def process(self, chunk: Chunk) -> Dict[str, Any]:
        """
        Process the input text and extract:
        1. sentences
        2. lexical units
        3. unique lemmas

        :param chunk: chunk whose text is going to be processed
        :return: execute the lexical unit processing
        """
        text = chunk.text
        doc = self._nlp(text)
        sentences = self.do_sentence_segmentation(doc)
        mwe_matches = self.do_mwe_detection(text, DEFAULT_MWES)

        token_based_lus = self.do_token_based_lu_extraction(doc, sentences)
        mwe_lexical_units = self.extract_mwe_lexical_units(mwe_matches, sentences)

        lexical_units = token_based_lus + mwe_lexical_units
        unique_lexical_units = self.deduplicate_lexical_units(lexical_units)
        unique_lemmas = self.get_unique_lemmas(unique_lexical_units)

        return {
            "lexical_units": unique_lexical_units,
            "unique_lemmas": unique_lemmas,
        }


if __name__ == "__main__":
    example_text = (
        "The economy is on fire, investors are flooding the market, "
        "and small businesses are drowning in debt. "
        "He grasped the idea, but things spiraled out of control."
    )

    text_processor = LexicalUnitTextPreprocessor()
    output = text_processor.process_text(example_text)

    import json

    print(json.dumps(output, indent=2, ensure_ascii=False))
