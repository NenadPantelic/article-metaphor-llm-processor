from abc import ABC, abstractmethod
from typing import Any

from client.chunk_processing_state_api_client import ChunkProcessingStateApiClient
from common.config.logconfig import LogConfig
from config.config_data import RabbitMQConfig
from data.chunk_processing_state import ChunkProcessingStateUpdate, ChunkProcessingState
from helper.exception_util import is_error_retryable
from helper.serialization import deserialize_body
from processor.processor_service import ProcessorService
from rabbitmq.rmq_consumer import RabbitMQConsumer
from rabbitmq.rmq_producer import RabbitMQProducer
from data.message import OutputMessage, InputMessage

log = LogConfig.default(__name__, __file__)


class PipelineStepProcessor(ABC, RabbitMQConsumer):
    def __init__(self, rabbitmq_config: RabbitMQConfig, queue: str, producer: RabbitMQProducer,
                 processor_service: ProcessorService,
                 input_message_type: type[InputMessage],
                 output_message_type: type[OutputMessage],
                 chunk_processing_api_client: ChunkProcessingStateApiClient):
        super().__init__(rabbitmq_config, queue)
        self._processor_service = processor_service
        self._producer = producer
        self._input_message_type = input_message_type
        self._output_message_type = output_message_type
        self._chunk_processing_api_client = chunk_processing_api_client

    def process(self, input_message: InputMessage) -> dict[str, Any]:
        log.info(f"Processing an input message: {input_message}")
        return self._processor_service.process(input_message)

    @abstractmethod
    def _prepare_output_message(self, chunk_id: str, document_id: str, **kwargs) -> OutputMessage:
        pass

    def callback(self, method, properties, body):
        data = deserialize_body(body)
        input_message = self._input_message_type(**data)
        chunk_id = input_message.chunk_id
        document_id = input_message.document_id
        log.info(f"Received an input message: {input_message}")

        chunk_processing_state_update = ChunkProcessingStateUpdate(chunk_id, document_id)
        try:
            log.info("Marking chunk[id={chunk_id}] as in processing.")
            chunk_processing_state_update.state = ChunkProcessingState.METAPHOR_ANALYSIS__IN_PROGRESS
            self._chunk_processing_api_client.update_chunk_processing_state(chunk_processing_state_update)

            processed_data = self.process(input_message)
            log.info(f"Processed the input message: {input_message}. Marking it as completed.")
            output_message = self._prepare_output_message(chunk_id, document_id, **processed_data)

            chunk_processing_state_update.state = ChunkProcessingState.METAPHOR_ANALYSIS__IN_COMPLETE
            chunk_processing_state_update.should_be_reprocessed = False
            chunk_processing_state_update.payload = output_message.to_dict()
            self._chunk_processing_api_client.update_chunk_processing_state(chunk_processing_state_update)

            self._producer.send(output_message)
        except Exception as e:
            log.error(f"An error occurred when processing the input message: {input_message}. Reason: {e}")
            should_reprocess_chunk = is_error_retryable(e)
            chunk_processing_state_update.state = ChunkProcessingState.METAPHOR_ANALYSIS__IN_FAILED
            chunk_processing_state_update.should_be_reprocessed = should_reprocess_chunk
            chunk_processing_state_update.payload = None
            self._chunk_processing_api_client.update_chunk_processing_state(chunk_processing_state_update)
