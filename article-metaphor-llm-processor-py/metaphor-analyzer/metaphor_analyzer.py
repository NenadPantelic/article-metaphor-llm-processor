from client.chunk_processing_state_api_client import ChunkProcessingStateApiClient
from common.config.logconfig import LogConfig
from config.config_data import RabbitMQConfig
from data import Metaphor
from data.chunk_processing_state import ChunkProcessingState
from data.lemma_with_explanations_data import LemmaWithExplanationsData
from data.message import OutputMessage
from exception.invalid_data_exception import InvalidDataException
from metaphor_analsys_service import MetaphorAnalysisService
from processor.pipeline_step_processor import PipelineStepProcessor
from rabbitmq.rmq_producer import RabbitMQProducer

log = LogConfig.default(__name__, "metaphor-analyzer")


class MetaphorAnalyzer(PipelineStepProcessor):
    def __init__(self, rabbitmq_config: RabbitMQConfig, queue: str, producer: RabbitMQProducer,
                 metaphor_analysis_service: MetaphorAnalysisService,
                 chunk_processing_api_client: ChunkProcessingStateApiClient, ):
        super().__init__(rabbitmq_config,
                         queue,
                         producer,
                         metaphor_analysis_service,
                         LemmaWithExplanationsData,
                         Metaphor,
                         ChunkProcessingState.METAPHOR_ANALYSIS__IN_PROGRESS,
                         ChunkProcessingState.METAPHOR_ANALYSIS__COMPLETE,
                         ChunkProcessingState.METAPHOR_ANALYSIS__FAILED,
                         chunk_processing_api_client)

    def _prepare_output_message(self, chunk_id: str, document_id: str, **kwargs) -> OutputMessage:
        phrase = kwargs.get("phrase")
        offset = kwargs.get("offset")
        metaphor_type = kwargs.get("metaphor_type")
        explanation = kwargs.get("explanation")

        if not all([phrase, offset, metaphor_type]):
            raise InvalidDataException("Phrase, offset and metaphor type attributes not present and they are required.")

        return Metaphor(chunk_id=chunk_id, document_id=document_id, phrase=phrase, offset=offset,
                        metaphor_type=metaphor_type, explanation=explanation
                        )
