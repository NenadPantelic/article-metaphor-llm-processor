import uuid
from config.config import get_config
from config.logconfig import get_logger
from config.config_properties import DatabaseConfig, ServiceConfig, LemmaMeaningsCacheConfig, AssistantConfig, \
    RabbitMQConfig, ProcessingConfig, DictionaryConfig
from data.processing_milestone import ProcessingMilestone
from processor.analysis_result_assembly_processor import AnalysisResultAssemblyProcessor
from processor.pipeline_processor import PipelineProcessor

config = get_config("config.ini")
service_config = ServiceConfig.from_config(config)
logger = get_logger(service_config.name)
assistant_config = AssistantConfig.from_config(config)
rmq_config = RabbitMQConfig.from_config(config)
processing_config = ProcessingConfig.from_config(config)
lemma_meanings_cache_config = LemmaMeaningsCacheConfig.from_config(config)
dictionary_config = DictionaryConfig.from_config(config)

from db.repository.chunk_processing_state_repository import ChunkProcessingStateRepository
from db.repository.conversation_repository import ConversationRepository
from db.client.mongodb_client import MongoDBClient

from model.chunk_processing_state import ChunkProcessingState
from model.processing_data import RawMessage, LemmasWithExplanations, LemmaExplanations

from cache.lemma_meaning_cache import LemmaMeaningsCache
from processor.lemma_meanings_lookup_processor import LemmaMeaningsLookupProcessor
from processor.lexical_unit_processor import LexicalUnitProcessor
from service.dictionary_access_service import DictionaryAccessService
from processor.metaphor_analysis_processor import MetaphorAnalysisProcessor
from util.time_util import utc_now

if __name__ == "__main__":
    db_config = DatabaseConfig.from_config(config)
    mongo_client = MongoDBClient(db_config)

    cpsr = ChunkProcessingStateRepository(mongo_client, "chunk_processing_states")
    pipeline_processor = PipelineProcessor(rmq_config, processing_config.queue, cpsr)

    lemma_meanings_cache = LemmaMeaningsCache(lemma_meanings_cache_config)
    dictionary_access_service = DictionaryAccessService(lemma_meanings_cache, dictionary_config)

    lexical_unit_processor = LexicalUnitProcessor()
    lemma_meaning_lookup_processor = LemmaMeaningsLookupProcessor(dictionary_access_service)
    conversation_repository = ConversationRepository(mongo_client, "conversations")
    metaphor_analysis_processor = MetaphorAnalysisProcessor(assistant_config=assistant_config,
                                                            conversation_repository=conversation_repository)
    analysis_result_assembly_processor = AnalysisResultAssemblyProcessor()

    milestones = [
        ProcessingMilestone.STARTED,
        ProcessingMilestone.LEXICAL_UNIT_PROCESSING,
        ProcessingMilestone.LEMMA_MEANING_LOOKUP,
        ProcessingMilestone.METAPHOR_ANALYSIS,
        ProcessingMilestone.RESULT_ASSEMBLY
    ]

    processors = [
        lexical_unit_processor, lemma_meaning_lookup_processor, metaphor_analysis_processor,
        analysis_result_assembly_processor
    ]

    processor_idx = 0
    for milestone in milestones:
        pipeline_processor.register_processors(milestone, processors[processor_idx:])
        processor_idx += 1

    ###
    document_id = str(uuid.uuid4())

    text_1 = (
        """
        The company had spent nearly a year trying to recover from a difficult period. At first, the problems seemed manageable. A delayed product launch was followed by several unexpected resignations, and the finance team had to revise its forecasts twice. None of these events looked disastrous on its own, but together they began to change the mood inside the organization. 
        By March, uncertainty had settled over the office. People became more careful in meetings, and decisions that would once have taken an afternoon were pushed into the following week. The leadership team kept asking for more information, while employees waited for someone to draw a clear line through the confusion. In the corridors, small conversations often stopped when a manager walked past.
        The new chief executive, Elena Marković, arrived at a difficult moment. She did not promise an immediate turnaround. Instead, she spent her first few weeks listening to employees, visiting regional offices, and examining projects that had received little attention from the previous management team. She quickly discovered that some of the company's problems were older than the latest financial figures suggested.
        We have been carrying more weight than we realized," she told the staff during a meeting in April. "Before we can move forward, we need to understand what is slowing us down."
        """
    )
    chunk_1_id = str(uuid.uuid4())

    text_2 = (
        """
        Her remarks were followed by several quiet changes. Two projects were closed, although neither had failed completely. Their teams were moved to other parts of the business, where their experience could be used more effectively. At the same time, the company began to simplify a reporting system that had grown over several years. Managers had become accustomed to producing increasingly detailed reports, even when few people read them.
        The process was uncomfortable. Some employees felt that the organization was cutting into its own foundations. Others believed that the changes were long overdue. For several weeks, the debate ran through the company like a low electrical current. It was rarely visible in official meetings, but it could be felt in conversations over coffee and in the questions people asked after presentations.
        By early summer, the first signs of improvement appeared. Customer complaints were falling, and the sales team had started to recover several accounts that had seemed permanently lost. The numbers were still modest, but they gave the company something it had been missing for months: a sense of direction.
        Elena remained cautious. She knew that one good quarter could not erase years of accumulated problems. In a message to managers, she wrote that the company was "turning a corner, not crossing the finish line." The distinction was deliberate. She wanted employees to recognize the progress without assuming that the difficult work was over.
        There were still obstacles ahead. A major supplier unexpectedly increased its prices, putting pressure on several products. Meanwhile, a competitor introduced a cheaper alternative that began to attract customers in one of the company's most important markets. The management team had to decide whether to defend its existing position or change its strategy before the new competitor gained ground.
        """
    )
    chunk_2_id = str(uuid.uuid4())

    text_3 = (
        """
        The discussion lasted for weeks. Some executives argued that the company should hold its course and wait for market conditions to improve. Others believed that the old strategy had reached the end of the road. Eventually, Elena approved a compromise: the company would protect its strongest products while investing more heavily in a smaller number of new ones.
        The decision did not produce an immediate breakthrough. Instead, progress came in small increments. A new product performed slightly better than expected. A regional sales office exceeded its target. An internal software project that had been delayed for months finally reached testing. Each result was small, but together they began to change how people talked about the future.
        By autumn, the atmosphere in the company was noticeably different. Employees were still under pressure, and several difficult decisions remained. But the sense that the organization was drifting had largely disappeared. People were beginning to believe that the problems could be addressed one at a time.
        Looking back, Elena said that the company had not needed a dramatic rescue. "We needed to stop treating every problem as an emergency," she said. "Once we could see the road clearly, we could decide where to go."
        The company was not out of danger. But for the first time in a long while, it was moving under its own power.
        """
    )
    chunk_3_id = str(uuid.uuid4())

    data_arr = [
        {
            "chunk_id": chunk_1_id,
            "document_id": document_id,
            "text": text_1,
            "order": 1,
        },
        {
            "chunk_id": chunk_2_id,
            "document_id": document_id,
            "text": text_2,
            "order": 2,
        },
        {
            "chunk_id": chunk_3_id,
            "document_id": document_id,
            "text": text_3,
            "order": 3,
        },
    ]

    ###
    for data in data_arr:
        pipeline_processor.process_pipeline(data)
    # pipeline_processor.process_pipeline()
    # example_text = (
    #     "The economy is on fire, investors are flooding the market, "
    #     "and small businesses are drowning in debt. "
    #     "He grasped the idea, but things spiraled out of control."
    # )
    # chunk_id = str(uuid.uuid4())
    #
    # cps = cpsr.find_processing_state(chunk_id)
    # # print(f"CPS: {cps}")
    # if not cps:
    #     cps = ChunkProcessingState(chunk_id=chunk_id, order=1)
    #
    # raw_message = RawMessage(text=example_text)
    #
    # lexical_unit_processing_result = lexical_unit_processor.execute(raw_message)
    # # print(f"Lexical unit processing data: {lexical_unit_processing_result}")
    #
    # cps.last_execution_timestamp = utc_now()
    # cps.data = {lexical_unit_processor.milestone.name: lexical_unit_processing_result.to_dict()}
    # cpsr.save_chunk_processing_state(chunk_processing_state=cps)
    # # print(f"Updated CPS: {cps}")
    #
    # print("---------")
    # cps = cpsr.find_processing_state(chunk_id)
    # # print(f"CPS after creation: {cps}")
    #
    # lemmas_with_meanings = lemma_meaning_lookup_processor.execute(lexical_unit_processing_result)
    # # print(lemmas_with_meanings)
    #
    # # import json
    # #
    # # lemmas_explanations = None
    # # with open("lemmas_with_explanations.json", "r") as file:
    # #     data = file.read()
    # #     lemmas_explanations_dicts = json.loads(data)
    # #     lemmas_explanations = [LemmaExplanations(**led) for led in lemmas_explanations_dicts]
    # # lemmas_with_meanings = LemmasWithExplanations(lemmas_explanations, utc_now())
    #
    # # conversation_repository = ConversationRepository(mongo_client, "conversations")
    # # metaphor_analysis_processor = MetaphorAnalysisProcessor(assistant_config=assistant_config,
    # #                                                         conversation_repository=conversation_repository)
    # # document_id = str(uuid.uuid4())
    # # metaphor_analysis_result = metaphor_analysis_processor.execute(lemmas_with_meanings, document_id=document_id,
    # #                                                                text=example_text,
    # #                                                                last_chunk=True)
    # # cps.last_execution_timestamp = utc_now()
    # # cps.data[metaphor_analysis_processor.milestone.name] = metaphor_analysis_result.to_dict()
    # # cpsr.save_chunk_processing_state(chunk_processing_state=cps)
    # # print(f"Updated CPS: {cps}")
    # #
    # # analysis_result_assembly_processor = AnalysisResultAssemblyProcessor()
    # # assembly_result = analysis_result_assembly_processor.execute(metaphor_analysis_result, document_id, example_text,
    # #                                                              True)
    # #
    # # segment_list = [r.to_dict() for r in assembly_result]
    # # import json
    # #
    # # output = json.dumps(segment_list)
    # # print(assembly_result)
    # #
    # # with open("output/assembly_result.json", "w") as fp:
    # #     fp.write(output)
