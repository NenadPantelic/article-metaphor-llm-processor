from typing import Any

from client.chunk_processing_state_api_client import ChunkProcessingStateApiClient
from config.config_data import RabbitMQConfig
from data.chunk_processing_state import ChunkProcessingStateUpdate, ChunkProcessingState
from helper.serialization import deserialize_body
from lexical_unit_text_processor import LexicalUnitTextPreprocessor
from rabbitmq.rmq_consumer import RabbitMQConsumer

from data import Chunk, LexicalUnitData
from rabbitmq.rmq_producer import RabbitMQProducer
from common.config.logconfig import LogConfig

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
        log.info(f"Received chunk: {chunk}. Marking chunk[id={chunk_id}] as in processing.")
        try:
            processed_data = self.process(chunk)
            log.info(f"Processed chunk: {chunk}")
            chunk_processing_state_update = ChunkProcessingStateUpdate(chunk_id,
                                                                       document_id,
                                                                       ChunkProcessingState.LEXICAL_UNIT_PROCESSING__COMPLETE,
                                                                       False,
                                                                       processed_data)
            self._chunk_processing_api_client.update_chunk_processing_state(chunk_processing_state_update)
            lexical_unit_data = LexicalUnitData(chunk_id, document_id, **processed_data)
            self._producer.send(lexical_unit_data)
        except Exception as e:
            log.error(f"Error processing chunk: {chunk}. Reason: {e}")
            is_retryable = False  # TODO
            chunk_processing_state_update = ChunkProcessingStateUpdate(chunk_id,
                                                                       document_id,
                                                                       ChunkProcessingState.LEXICAL_UNIT_PROCESSING__FAILED,
                                                                       is_retryable,
                                                                       None)
            self._chunk_processing_api_client.update_chunk_processing_state(chunk_processing_state_update)
