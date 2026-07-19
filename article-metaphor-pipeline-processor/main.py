import uuid

from config.config import get_config
from config.config_properties import DatabaseConfig, ServiceConfig, LemmaMeaningsCacheConfig, AssistantConfig
from config.logconfig import get_logger

# To initialize the logger first
config = get_config("config.ini")
service_config = ServiceConfig.from_config(config)
assistant_config = AssistantConfig.from_config(config)
# lemma_meanings_cache_config = LemmaMeaningsCacheConfig.from_config(config)

logger = get_logger(service_config.name)

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

# print(assistant_config.__dict__)

if __name__ == "__main__":
    example_text = (
        "The economy is on fire, investors are flooding the market, "
        "and small businesses are drowning in debt. "
        "He grasped the idea, but things spiraled out of control."
    )
    chunk_id = str(uuid.uuid4())

    db_config = DatabaseConfig.from_config(config)
    mongo_client = MongoDBClient(db_config)

    cpsr = ChunkProcessingStateRepository(mongo_client, "chunk_processing_states")
    cps = cpsr.find_processing_state(chunk_id)
    #print(f"CPS: {cps}")
    if not cps:
        cps = ChunkProcessingState(chunk_id=chunk_id)

    raw_message = RawMessage(text=example_text)
    lexical_unit_processor = LexicalUnitProcessor()
    lexical_unit_processing_result = lexical_unit_processor.execute(raw_message)
    #print(f"Lexical unit processing data: {lexical_unit_processing_result}")

    cps.last_execution_timestamp = utc_now()
    cps.data = {lexical_unit_processor.milestone.name: lexical_unit_processing_result.to_dict()}
    cpsr.save_chunk_processing_state(chunk_processing_state=cps)
    #print(f"Updated CPS: {cps}")

    print("---------")
    cps = cpsr.find_processing_state(chunk_id)
    #print(f"CPS after creation: {cps}")

    lemma_meaning_lookup_processor = LemmaMeaningsLookupProcessor()
    lemmas_with_meanings = lemma_meaning_lookup_processor.execute(lexical_unit_processing_result)
    #print(lemmas_with_meanings)

    # ### Test
    # lemmas_explanations = []
    # LemmaExplanations("business", cambridge_explanations=[
    #     '''the activity of buying and selling goods and services:', 'a particular company that buys and sells goods and "
    #     "services:', 'work that you do to earn money:', 'the amount of work done or the number of goods or services sold"
    #     " by a company or organization:', 'a situation or activity, often one that you are giving your opinion about:', "
    #     "'the things that you do or the matters that relate only to you:', 'the actions of performers in a play, "
    #     "television programme, film, etc., intended to create a particular situation and feeling:', 'the activity of "
    #     "buying and selling goods and services, or a particular company that does this, or work in general rather than "
    #     "pleasure:', 'Business is also the degree of success of a company or of your work:', 'a matter or a situation:',"
    #     " 'the things that you do or the matters that relate only to you:', 'the activity of buying and selling goods "
    #     "and services:', 'work that you do to earn money:', 'a company or organization that buys and sells goods or "
    #     "services:', 'the amount of work done or the number of goods or services sold by a company or organization:', "
    #     "'the fact that someone buys goods or services from a particular store, company, etc.:', 'used to say that a "
    #     "business is operating, people are working, etc. in the way that they normally do:', 'used to say that in a "
    #     "particular situation the most important thing is to do what is right for a business:', 'someone who you feel "
    #     "you can work well with, because you understand each other:'''], ldoce_explanations=[
    #     ''''
    #
    #     '''
    # ])
    # lemmas_with_explanations = LemmasWithExplanations()
    #
    # ###
    conversation_repository = ConversationRepository(mongo_client, "conversations")
    metaphor_analysis_processor = MetaphorAnalysisProcessor(assistant_config=assistant_config,
                                                            conversation_repository=conversation_repository)

    metaphor_analysis_processor.execute(lemmas_with_meanings, document_id=str(uuid.uuid4()), text=example_text)
