from typing import Any

from client.chunk_processing_state_api_client import ChunkProcessingStateApiClient
from common.data.lexical_unit_data import LexicalUnitData
from common.config.logconfig import LogConfig
from config.config_data import RabbitMQConfig
from data.chunk_processing_state import ChunkProcessingStateUpdate, ChunkProcessingState
from data.lemma_with_explanations_data import LemmaWithExplanationsData
from dictionary_access_service import DictionaryAccessService
from helper.exception_util import is_error_retryable
from helper.serialization import deserialize_body
from rabbitmq.rmq_consumer import RabbitMQConsumer
from rabbitmq.rmq_producer import RabbitMQProducer

log = LogConfig.default(__name__, "lemma_meaning_retrieval_handler")


class LemmaMeaningRetrievalHandler(RabbitMQConsumer):
    def __init__(self, rabbitmq_config: RabbitMQConfig, queue: str, producer: RabbitMQProducer,
                 dictionary_access_service: DictionaryAccessService,
                 chunk_processing_api_client: ChunkProcessingStateApiClient, ):
        super().__init__(rabbitmq_config, queue)
        self._dictionary_access_service = dictionary_access_service
        self._producer = producer
        self._chunk_processing_api_client = chunk_processing_api_client

    def process(self, lexical_unit_data: LexicalUnitData) -> dict[str, Any]:
        log.info(f"Processing the lexical unit data: {lexical_unit_data}")
        return self._dictionary_access_service.get_lu_meanings(lexical_unit_data.lexical_units,
                                                               lexical_unit_data.unique_lemmas)

    def callback(self, method, properties, body):
        data = deserialize_body(body)
        lexical_unit_data = LexicalUnitData(**data)
        chunk_id = lexical_unit_data.chunk_id
        document_id = lexical_unit_data.document_id
        log.info(f"Received a lexical unit: {lexical_unit_data}")

        chunk_processing_state_update = ChunkProcessingStateUpdate(chunk_id, document_id)
        try:
            log.info("Marking chunk[id={chunk_id}] as in processing.")
            chunk_processing_state_update.state = ChunkProcessingState.DICTIONARY_ACCESS__IN_PROGRESS
            self._chunk_processing_api_client.update_chunk_processing_state(chunk_processing_state_update)

            processed_data = self.process(lexical_unit_data)
            log.info(f"Processed lexical unit data: {lexical_unit_data}. Marking it as complete.")

            chunk_processing_state_update.should_be_reprocessed = False
            chunk_processing_state_update.payload = processed_data
            chunk_processing_state_update.state = ChunkProcessingState.DICTIONARY_ACCESS__COMPLETE
            self._chunk_processing_api_client.update_chunk_processing_state(chunk_processing_state_update)

            log.info(
                f"Sending the lemma with explanations to the next processor in the pipeline: chunk_id = {chunk_id}")
            # TODO: populate the lemma with explanations properly
            lemma_with_explanations = LemmaWithExplanationsData(chunk_id, document_id, **processed_data)
            self._producer.send(lemma_with_explanations)
        except Exception as e:
            log.error(f"Error processing the lexical unit: chunk_id={chunk_id}. Reason: {e}")

            should_be_reprocessed = is_error_retryable(e)
            chunk_processing_state_update.state = ChunkProcessingState.DICTIONARY_ACCESS__FAILED
            chunk_processing_state_update.payload = None
            chunk_processing_state_update.should_be_reprocessed = should_be_reprocessed
            self._chunk_processing_api_client.update_chunk_processing_state(chunk_processing_state_update)
