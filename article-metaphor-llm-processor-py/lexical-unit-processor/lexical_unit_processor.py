from client.chunk_processing_state_api_client import ChunkProcessingStateApiClient
from common.config.logconfig import LogConfig
from common.data.chunk import Chunk
from common.data.lexical_unit_data import LexicalUnitData
from config.config_data import RabbitMQConfig
from exception.invalid_data_exception import InvalidDataException
from lexical_unit_processing_service import LexicalUnitTextPreprocessor
from processor.pipeline_step_processor import PipelineStepProcessor
from rabbitmq.rmq_producer import RabbitMQProducer
from data.message import OutputMessage

log = LogConfig.default(__name__, "lexical_unit_processor")


class LexicalUnitProcessor(PipelineStepProcessor):
    def __init__(self,
                 rabbitmq_config: RabbitMQConfig,
                 queue: str,
                 producer: RabbitMQProducer,
                 text_processor: LexicalUnitTextPreprocessor,
                 chunk_processing_api_client: ChunkProcessingStateApiClient,
                 ):
        super().__init__(rabbitmq_config,
                         queue,
                         producer,
                         text_processor,
                         Chunk,
                         LexicalUnitData,
                         chunk_processing_api_client)

    def _prepare_output_message(self, chunk_id: str, document_id: str, **kwargs) -> OutputMessage:
        lexical_units = kwargs.get("lexical_units")
        if not lexical_units:
            raise InvalidDataException("Lexical units attribute not present and it is required.")

        unique_lemmas = kwargs.get("unique_lemmas")
        if not unique_lemmas:
            raise InvalidDataException("Unique lemmas attribute not present and it is required.")

        return LexicalUnitData(
            chunk_id=chunk_id,
            document_id=document_id,
            lexical_units=lexical_units,
            unique_lemmas=unique_lemmas)
