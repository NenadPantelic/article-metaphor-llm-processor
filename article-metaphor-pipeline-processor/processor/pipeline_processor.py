import json
from typing import List, Union, Any

from config.config_properties import RabbitMQConfig
from config.logconfig import get_logger
from data.pipeline_message import ProcessingMessage, ReprocessingMessage
from data.processing_milestone import ProcessingMilestone
from db.repository.chunk_processing_state_repository import ChunkProcessingStateRepository
from exception.processor_exception import ProcessorException
from model.chunk_processing_state import ChunkProcessingState, ChunkProcessingError
from model.processing_data import RawMessage, ProcessingData
from processor.step_processor import StepProcessor
from rabbitmq.rabbitmq_consumer import RabbitMQConsumer
from util.exception_util import is_error_retryable
from util.time_util import utc_now

log = get_logger()


def _try_parsing_pipeline_message(data: dict) -> Union[ProcessingMessage, ReprocessingMessage]:
    if data is None:
        raise ProcessorException("Missing data")

    chunk_id = data.get("chunk_id")
    if not chunk_id:
        raise ProcessorException("Chunk id is required.")

    document_id = data.get("document_id")
    if not document_id:
        raise ProcessorException("Document id is required.")

    text = data.get("text")
    order = data.get("order", 0)

    if text and order:
        return ProcessingMessage(chunk_id, order, document_id, text)

    type_of_reprocessing = data.get("type_of_reprocessing")
    if type_of_reprocessing:
        return ReprocessingMessage(chunk_id, order, document_id, type_of_reprocessing)

    raise ProcessorException(f"Unknown message type: {data}")


def execute_step(processor: StepProcessor, chunk_id: str, message: ProcessingData, additional_attrs: dict[str, Any]) -> \
        tuple[
            Union[ProcessingData, None], Union[ChunkProcessingError, None]]:
    try:
        log.info(f"Processing chunk: {chunk_id}")
        output = processor.execute(message, **additional_attrs)
        return output, None
    except Exception as e:
        log.error(f"Processing failed with error: {e}")
        processing_error = ChunkProcessingError(str(e), utc_now(), processor.milestone, is_error_retryable(e))
        return None, processing_error


class PipelineProcessor(RabbitMQConsumer):
    def __init__(self, rabbitmq_config: RabbitMQConfig, queue: str,
                 chunk_processing_state_repository: ChunkProcessingStateRepository):
        super().__init__(rabbitmq_config, queue)
        self._chunk_processing_state_repository = chunk_processing_state_repository
        self._milestone_processors = {}

    def register_processors(self, milestone: ProcessingMilestone, processors: List[StepProcessor]) -> None:
        self._milestone_processors[milestone] = processors

    def process_pipeline(self, data: dict):
        chunk_processing_state = self._get_or_create_chunk_processing_state(data)
        milestone = chunk_processing_state.last_executed_milestone
        processors = self._get_processors(milestone)
        key_milestone = (milestone or ProcessingMilestone.STARTED).name
        input_message = chunk_processing_state.data[key_milestone]

        additional_attrs = {
            "document_id": chunk_processing_state.document_id,
            "text": chunk_processing_state.text,
            "last_chunk": True
        }
        for processor in processors:
            output_message, error = execute_step(processor, chunk_processing_state.chunk_id, input_message,
                                                 additional_attrs)
            execution_time = output_message.execution_time if output_message else utc_now()
            chunk_processing_state.last_execution_timestamp = execution_time

            if error:
                log.error(f"Processing failed with error: {error}")
                chunk_processing_state.errors.append(error)
                chunk_processing_state.failed_on_last_execution = True
                self._chunk_processing_state_repository.save_chunk_processing_state(chunk_processing_state)
                break
            else:
                log.info(f"Successfully completed milestone {processor.milestone}")
                chunk_processing_state.last_executed_milestone = processor.milestone
                chunk_processing_state.data[processor.milestone.name] = output_message
                # to resolve the last failure on rerun
                chunk_processing_state.failed_on_last_execution = False
                self._chunk_processing_state_repository.save_chunk_processing_state(chunk_processing_state)

            input_message = output_message

    def _get_or_create_chunk_processing_state(self, data: dict) -> ChunkProcessingState:
        pipeline_message = _try_parsing_pipeline_message(data)
        chunk_id = pipeline_message.chunk_id
        order = pipeline_message.order
        document_id = pipeline_message.document_id

        chunk_processing_state = self._chunk_processing_state_repository.find_processing_state(chunk_id)
        if chunk_processing_state:
            log.info(f"Found chunk processing state: {chunk_processing_state}")
            return chunk_processing_state

        log.info(f"Received a new chunk to be processed: {chunk_id}")
        processing_data = {ProcessingMilestone.STARTED.name: RawMessage(pipeline_message.text)}
        chunk_processing_state = ChunkProcessingState(chunk_id=chunk_id, order=order, document_id=document_id,
                                                      text=pipeline_message.text,
                                                      data=processing_data, last_executed_milestone=None)
        return self._chunk_processing_state_repository.save_chunk_processing_state(chunk_processing_state)

    def callback(self, method, properties, body):
        data = json.loads(body)
        self.process_pipeline(data)
        self._channel.basic_ack(delivery_tag=method.delivery_tag)

    def _get_processors(self, milestone: ProcessingMilestone):
        if milestone is None:
            milestone = ProcessingMilestone.STARTED

        processors = self._milestone_processors.get(milestone)
        if processors is None:
            raise ProcessorException(f"Unknown milestone: {milestone}. Cannot resolve any processors for it.")

        return processors
