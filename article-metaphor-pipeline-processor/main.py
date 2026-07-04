import uuid

from cache.lemma_meaning_cache import LemmaMeaningsCache
from config.config import get_config
from config.config_properties import DatabaseConfig, ServiceConfig, LemmaMeaningsCacheConfig
from config.logconfig import get_logger

from db.repository.chunk_processing_state_repository import ChunkProcessingStateRepository
from db.client.mongodb_client import MongoDBClient

from model.chunk_processing_state import ChunkProcessingState
from model.processing_data import RawMessage
from processor.lemma_meanings_lookup_processor import LemmaMeaningsLookupProcessor
from processor.lexical_unit_processor import LexicalUnitProcessor
from service.dictionary_access_service import DictionaryAccessService
from util.time_util import utc_now

config = get_config("config.ini")
service_config = ServiceConfig.from_config(config)
# lemma_meanings_cache_config = LemmaMeaningsCacheConfig.from_config(config)

logger = get_logger(service_config.name)

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
    print(f"CPS: {cps}")
    if not cps:
        cps = ChunkProcessingState(chunk_id=chunk_id)

    raw_message = RawMessage(text=example_text)
    lexical_unit_processor = LexicalUnitProcessor()
    lexical_unit_processing_result = lexical_unit_processor.execute(raw_message)
    print(f"Lexical unit processing data: {lexical_unit_processing_result}")

    cps.last_execution_timestamp = utc_now()
    cps.data = {lexical_unit_processor.milestone.name: lexical_unit_processing_result.to_dict()}
    cpsr.save_chunk_processing_state(chunk_processing_state=cps)
    print(f"Updated CPS: {cps}")

    print("---------")
    cps = cpsr.find_processing_state(chunk_id)
    print(f"CPS after creation: {cps}")

    lemma_meaning_lookup_processor = LemmaMeaningsLookupProcessor()
    lemmas_with_meanings = lemma_meaning_lookup_processor.execute(lexical_unit_processing_result)
    print(lemmas_with_meanings)
