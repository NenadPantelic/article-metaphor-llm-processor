import redis

from common.cache.cache import RedisCache
from config.config import LemmaMeaningsCacheConfig
from config.logconfig import get_logger

log = get_logger()


class LemmaMeaningsCache(RedisCache):
    def __init__(self, cache_config: LemmaMeaningsCacheConfig):
        super().__init__(cache_config)

        self._ldoce_cache_key = cache_config.ldoce_cache_key
        self._cambridge_cache_key = cache_config.cambridge_cache_key

    def set_ldoce_lemma_meanings(self, lemma: str, meanings: list[str]):
        log.info(f"Setting LDOCE lemma meanings for {lemma}")
        key = f"{self._ldoce_cache_key}:{lemma}"
        self._cache.rpush(key, *meanings)

    def get_ldoce_lemma_meanings(self, lemma: str) -> list[str]:
        log.info(f"Getting LDOCE lemma meanings for {lemma}")
        key = f"{self._ldoce_cache_key}:{lemma}"
        return self._cache.lrange(key, 0, -1)

    def put_cambridge_lemma_meanings(self, lemma: str, meanings: list[str]):
        log.info(f"Setting Cambridge lemma meanings for {lemma}")
        key = f"{self._cambridge_cache_key}:{lemma}"
        self._cache.rpush(key, *meanings)

    def get_cambridge_lemma_meanings(self, lemma: str) -> list[str]:
        log.info(f"Getting Cambridge lemma meanings for {lemma}")
        key = f"{self._cambridge_cache_key}:{lemma}"
        return self._cache.lrange(key, 0, -1)

    def close(self):
        if self._cache:
            log.info("Closing redis cache")
            self._cache.close()
