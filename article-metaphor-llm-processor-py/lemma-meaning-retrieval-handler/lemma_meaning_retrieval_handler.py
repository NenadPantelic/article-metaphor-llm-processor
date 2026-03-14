from client.chunk_processing_state_api_client import ChunkProcessingStateApiClient
from common.config.logconfig import LogConfig
from common.data.lexical_unit_data import LexicalUnitData
from config.config_data import RabbitMQConfig
from data.lemma_with_explanations_data import LemmaWithExplanationsData
from data.message import OutputMessage
from dictionary_access_service import DictionaryAccessService
from exception.invalid_data_exception import InvalidDataException
from processor.pipeline_step_processor import PipelineStepProcessor
from rabbitmq.rmq_producer import RabbitMQProducer

log = LogConfig.default(__name__, "lemma_meaning_retrieval_handler")


class LemmaMeaningRetrievalHandler(PipelineStepProcessor):
    def __init__(self,
                 rabbitmq_config: RabbitMQConfig,
                 queue: str,
                 producer: RabbitMQProducer,
                 dictionary_access_service: DictionaryAccessService,
                 chunk_processing_api_client: ChunkProcessingStateApiClient,
                 ):
        super().__init__(rabbitmq_config,
                         queue,
                         producer,
                         dictionary_access_service,
                         LexicalUnitData,
                         LemmaWithExplanationsData,
                         chunk_processing_api_client)

    def _prepare_output_message(self, chunk_id: str, document_id: str, **kwargs) -> OutputMessage:
        meanings = kwargs.get("meanings")
        if not meanings:
            raise InvalidDataException("Meanings attribute not present and it is required.")

        return LemmaWithExplanationsData(

            chunk_id=chunk_id,
            document_id=document_id,
            surface="",
            explanations=meanings,
        )
