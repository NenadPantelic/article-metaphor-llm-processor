import redis

from config.config_data import CacheConfig
from config.logconfig import get_logger

log = get_logger()


class RedisCache:
    def __init__(self, cache_config: CacheConfig):
        self._cache = redis.Redis(host=cache_config.host,
                                  port=cache_config.port,
                                  username=cache_config.username,
                                  password=cache_config.password,
                                  decode_responses=True)
        self._cache.ping()

    def close(self):
        if self._cache:
            log.info("Closing redis cache")
            self._cache.close()
