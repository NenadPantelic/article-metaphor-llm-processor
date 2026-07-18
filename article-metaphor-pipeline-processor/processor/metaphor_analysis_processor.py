import json
import math
import re
from typing import Any

from openai import OpenAI

from config.config_properties import AssistantConfig
from config.logconfig import get_logger
from data.processing_milestone import ProcessingMilestone
from db.repository.conversation_repository import ConversationRepository
from exception.invalid_data_exception import InvalidDataException
from helper.serialization import deserialize_body
from model.conversation import Conversation
from model.processing_data import ProcessingData, LemmasWithExplanations
from processor.step_processor import StepProcessor
from util.time_util import utc_now

_SYSTEM_ROLE = "system"
_USER_ROLE = "user"

_NUM_OF_CHARACTERS_PER_PROCESSABLE_TEXT = 1500

log = get_logger()


def _reconstruct_text(sentences: list[str] = "", original_text: str = "", offset: int = 0):
    offset_in_text = offset
    composed_sentences = []
    for sentence in sentences:
        # sentence
        offset_in_text += len(sentence)
        composed_sentences.append(sentence + original_text[offset_in_text])
        # punctuation for the sentence end
        offset_in_text += 1

    return "".join(composed_sentences)


def split_text_into_processable_parts(text: str, lemma_meanings: dict[str, list[str]]) -> list[dict[str, Any]]:
    """
    Split text into processable parts based on the num of characters.
    The idea is to divide text into multiple requests not to cross the token limit. Parts will not be 100% equally
    divided, but we will just get the equal number of sentences in them.

    :param text: text to be split
    :param lemma_meanings: lemma meanings dictionary
    :return: a list of payloads that will be used to process the text
    """
    sentences = re.findall(r'[^.!?]+[.!?]+', text)
    num_of_parts = math.ceil(len(text) / _NUM_OF_CHARACTERS_PER_PROCESSABLE_TEXT)

    # what if we have only 5 sentences, and we need 10 parts, not very likely to happen
    num_of_parts = min(num_of_parts, len(sentences))

    # 11 sentences, 4 parts
    sentence_count_per_request = math.ceil(len(sentences) / num_of_parts)
    offset = 0
    len_of_subtext = 0

    subtexts_data_for_analysis = []
    for i in range(num_of_parts):
        sentences_to_process = sentences[offset: offset + sentence_count_per_request]
        offset += sentence_count_per_request

        subtext = _reconstruct_text(sentences_to_process, text, len_of_subtext)
        subtext_explanations = {
            lemma: explanations for lemma, explanations in lemma_meanings.items() if lemma in subtext
        }
        payload_for_analysis = {
            "text": subtext,
            "lemmas_meanings": subtext_explanations
        }
        len_of_subtext += len(subtext)

        subtexts_data_for_analysis.append(payload_for_analysis)

    return subtexts_data_for_analysis


class MetaphorAnalysisProcessor(StepProcessor):
    def __init__(self, assistant_config: AssistantConfig, conversation_repository: ConversationRepository):
        super().__init__(ProcessingMilestone.METAPHOR_ANALYSIS)
        self._client = OpenAI(api_key=assistant_config.api_key)
        self._assistant_config = assistant_config
        self._conversation_repository = conversation_repository
        self._conversation_cache = self._load_conversations()

    ### Cache methods ###
    def _load_conversations(self):
        """
        Loads conversations into the cache for better performance.
        :return: None
        """
        conversations = self._conversation_repository.find_all_conversations()
        return {conversation.document_id: conversation.conversation_id for conversation in conversations}

    def _remove_from_cache(self, document_id: str):
        """
        Evicts the entry from the cache. This should be called when the last chunk of a document is processed.
        :param document_id: document id that is a key in the cache
        :return: None
        """
        if document_id in self._conversation_cache:
            log.info(f"Removing the cache key: {document_id}")
            self._conversation_cache.pop(document_id)

    def _get_or_create_conversation(self, document_id: str):
        """
        Gets or creates a conversation with the given document_id.
        Conversation is created per document, and multiple chunks of the same document are processed
        in the same conversation. So, if the conversation for a given document does not exist, it
        creates a new conversation. Otherwise, it returns an existing conversation.

        :param document_id: document id
        :return: the conversation id
        """
        conversation_id = self._get_conversation(document_id)
        if conversation_id:
            return conversation_id

        response = self._client.responses.create(
            model=self._assistant_config.model,
            input=[
                {
                    "role": _SYSTEM_ROLE,
                    "content": self._assistant_config.start_conversation_instruction
                }
            ]
        )
        conversation_id = response.conversation
        log.info(f"Conversation started: id={conversation_id}")

        return self._store_conversation(document_id, conversation_id)

    def _store_conversation(self, doc_id: str, conversation_id: str):
        now = utc_now()
        self._conversation_repository.save_conversation(Conversation(doc_id, conversation_id, now, now))
        self._conversation_cache[doc_id] = conversation_id
        return conversation_id

    def _get_conversation(self, doc_id: str) -> str:
        conversation_id = self._conversation_cache.get(doc_id)
        if conversation_id:
            log.debug("Using cached conversation id", conversation_id)
            return conversation_id

        conversation = self._conversation_repository.find_by_document_id(doc_id)
        if conversation:
            self._conversation_cache[doc_id] = conversation.conversation_id
            return conversation.conversation_id

        return None

    def _build_prompt(self, text: str, lemma_meanings: dict[str, list[str]]) -> str:
        prompt_template = self._assistant_config.assistant_prompt_template
        prompt = prompt_template.replace("{{text}}", text).replace("{{lemma_meanings}}", json.dumps(lemma_meanings))
        log.debug(f"Prompt: {prompt}")
        return prompt

    def analyze_sentence(self, document_id: str, text: str, lemma_meanings: dict[str, list[str]],
                         last_chunk: bool = False) -> dict:
        """
        Sends one sentence + LUs into the ongoing conversation.
        :param document_id: document id
        :param text: text to be analyzed
        :param lemma_meanings: lemma meanings (explanations) for lemmas present in text
        :param last_chunk: boolean indicating if the last chunk of the document is about to be processed
        :return:
        """
        # TODO: I need a transformer to combine text + lemma meanings; think about the prompt
        # TODO: validate sentence_payload
        conversation_id = self._get_or_create_conversation(document_id=document_id)

        response = self._client.responses.create(
            model=self._assistant_config.model,
            conversation=conversation_id,
            response_format={"type": "json_object"},
            input=[
                {
                    "role": _USER_ROLE,
                    "content": self._build_prompt(text, lemma_meanings)
                }
            ]
        )

        if last_chunk:
            self._remove_from_cache(document_id)

        return deserialize_body(response.output_text)

    def execute(self, message: LemmasWithExplanations, document_id: str, text: str) -> ProcessingData:
        if not document_id or not text:
            raise InvalidDataException("Document ID and text are required")

        # collect lemma meanings into a dictionary
        lemma_meanings = {}
        for le in message.lemmas_explanations:
            lemma_meanings[le.lemma] = {
                "ldoce": le.ldoce_explanations,
                "cambridge": le.cambridge_explanations,
            }

        subtexts_data_for_analysis = split_text_into_processable_parts(text, lemma_meanings)
        for subtext_data in subtexts_data_for_analysis:
            result = self.analyze_sentence(document_id, subtext_data.get("text"), subtext_data.get("lemma_meanings"),
                                           True)
            print("Result:", result)
