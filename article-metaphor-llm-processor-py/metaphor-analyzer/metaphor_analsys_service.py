"""
Given a sentence, a lexical unit, and its basic meanings from dictionaries, is the lexical unit metaphorical in
context — and if so, how?

It compares the contextual meaning vs basic meaning:
- Applies MIPVU decision rules and assigns
    - DIRECT
    - INDIRECT
    - IMPLICIT
    - NONE
- Produces a short explanation
- Produces an output

Instructions for the assistant:
1. follow MIPVU strictly
2. prefer NON-metaphorical if unsure
3. treat dictionary meanings as authoritative
4. never invent meanings
5. never output prose outside JSON

This will use the response API
Before	        Now	                Why?
Assistants	   Prompts	        Prompts hold configuration (model, tools, instructions) and are easier to version and update
Threads	      Conversations     Streams of items instead of just messages
Runs	      Responses	        Responses send input items or use a conversation object and receive output items; tool call loops are explicitly managed
Run steps	  Items	            Generalized objects—can be messages, tool calls, outputs, and more

"""

import json

from openai import OpenAI

from config import db_config, assistant_config
from config.logconfig import LogConfig
from db.db_handler import ConversationDBHandler
from processor.processor_service import ProcessorService

_SYSTEM_ROLE = "system"
_USER_ROLE = "user"

log = LogConfig.default(__name__, "metaphor-analyzer")


class MetaphorAnalysisService(ProcessorService):
    def __init__(self):
        self._client = OpenAI(api_key=assistant_config.api_key)
        self._conversation_cache = self._load_conversations()
        self._db_handler = ConversationDBHandler(db_config)

    ### Cache methods ###
    def _load_conversations(self):
        """
        Loads conversations into the cache for better performance.
        :return: None
        """
        conversations = self._db_handler.get_all_conversations()
        return {conversation.doc_id: conversation.conversation_id for conversation in conversations}

    def _remove_from_cache(self, document_id: str):
        """
        Evicts the entry from the cache. This should be called when the last chunk of a document is processed.
        :param document_id: document id that is a key in the cache
        :return: None
        """
        if document_id in self._conversation_cache:
            print(f"Removing the cache key: {document_id}")
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
            model=assistant_config.model,
            input=[
                {
                    "role": _SYSTEM_ROLE,
                    "content": assistant_config.prompt
                }
            ]
        )
        conversation_id = response.conversation
        log.info(f"Conversation started: id={conversation_id}")
        return self._store_conversation(document_id, conversation_id)

    def _store_conversation(self, doc_id: str, conversation_id: str):
        self._db_handler.insert_conversation(doc_id, conversation_id)
        self._conversation_cache[doc_id] = conversation_id
        return conversation_id

    def _get_conversation(self, doc_id: str) -> str:
        conversation_id = self._conversation_cache.get(doc_id)
        if conversation_id:
            log.debug("Using cached conversation id", conversation_id)
            return conversation_id

        conversation = self._db_handler.get_conversation(doc_id)
        if conversation:
            self._conversation_cache[doc_id] = conversation.conversation_id
            return conversation.conversation_id

        return None

    def process(self):
        # TODO:
        pass

    def analyze_sentence(self, document_id: str, sentence_payload: dict, last_chunk: bool = False) -> dict:
        """
        Sends one sentence + LUs into the ongoing conversation.

        :param document_id: document id
        :param sentence_payload: dict containing the sentence, LUs and meanings
        :param last_chunk: boolean indicating if the last chunk of the document is about to be processed
        :return:
        """
        # TODO: validate sentence_payload
        conversation_id = self._get_or_create_conversation(document_id=document_id)
        response = self._client.responses.create(
            model=assistant_config.model,
            conversation=conversation_id,
            response_format={"type": "json_object"},
            input=[
                {
                    "role": _USER_ROLE,
                    "content": json.dumps(sentence_payload, ensure_ascii=False)
                }
            ]
        )

        if last_chunk:
            self._remove_from_cache(document_id)

        return json.loads(response.output_text)
