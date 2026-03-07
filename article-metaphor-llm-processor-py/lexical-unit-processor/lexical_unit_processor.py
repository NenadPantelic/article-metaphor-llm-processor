from typing import Any

from common.data.lexical_unit_data import LexicalUnitData
from common.config.logconfig import LogConfig
from config.config_data import RabbitMQConfig

from client.chunk_processing_state_api_client import ChunkProcessingStateApiClient
from data.chunk_processing_state import ChunkProcessingStateUpdate, ChunkProcessingState
from helper.exception_util import is_error_retryable
from helper.serialization import deserialize_body
from lexical_unit_text_processor import LexicalUnitTextPreprocessor
from rabbitmq.rmq_consumer import RabbitMQConsumer

from data import Chunk
from rabbitmq.rmq_producer import RabbitMQProducer

log = LogConfig.default(__name__, "lexical_unit_processor")


class LexicalUnitProcessor(RabbitMQConsumer):
    def __init__(self, rabbitmq_config: RabbitMQConfig, queue: str, producer: RabbitMQProducer,
                 text_processor: LexicalUnitTextPreprocessor,
                 chunk_processing_api_client: ChunkProcessingStateApiClient, ):
        super().__init__(rabbitmq_config, queue)
        self._text_processor = text_processor
        self._producer = producer
        self._chunk_processing_api_client = chunk_processing_api_client

    def process(self, chunk_data: Chunk) -> dict[str, Any]:
        log.info(f"Processing chunk: {chunk_data}")
        return self._text_processor.process_text(chunk_data.text)

    def callback(self, method, properties, body):
        data = deserialize_body(body)
        chunk = Chunk(**data)
        chunk_id = chunk.chunk_id
        document_id = chunk.document_id
        log.info(f"Received chunk: {chunk}.")

        chunk_processing_state_update = ChunkProcessingStateUpdate(chunk_id, document_id)
        try:
            log.info("Marking chunk[id={chunk_id}] as in processing.")
            chunk_processing_state_update.state = ChunkProcessingState.LEXICAL_UNIT_PROCESSING__IN_PROGRESS
            self._chunk_processing_api_client.update_chunk_processing_state(chunk_processing_state_update)

            processed_data = self.process(chunk)
            log.info(f"Processed chunk: {chunk}.Marking it as completed.")
            chunk_processing_state_update.state = ChunkProcessingState.LEXICAL_UNIT_PROCESSING__COMPLETE
            chunk_processing_state_update.should_be_reprocessed = False
            chunk_processing_state_update.payload = processed_data
            self._chunk_processing_api_client.update_chunk_processing_state(chunk_processing_state_update)

            lexical_unit_data = LexicalUnitData(chunk_id, document_id, **processed_data)
            self._producer.send(lexical_unit_data)
        except Exception as e:
            log.error(f"Error processing chunk: {chunk}. Reason: {e}")
            should_reprocess_chunk = is_error_retryable(e)
            chunk_processing_state_update.state = ChunkProcessingState.LEXICAL_UNIT_PROCESSING__FAILED
            chunk_processing_state_update.should_be_reprocessed = should_reprocess_chunk
            chunk_processing_state_update.payload = None
            self._chunk_processing_api_client.update_chunk_processing_state(chunk_processing_state_update)
