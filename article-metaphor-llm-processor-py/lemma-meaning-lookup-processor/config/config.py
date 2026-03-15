from dataclasses import dataclass

from config.config_data import CacheConfig

# Cache section/keys and default values
_LDOCE_SECTION = "ldoce"
_CAMBRIDGE_SECTION = "cambridge"

_BUCKET_KEY = "cache-key"


@dataclass
class LemmaMeaningsCacheConfig(CacheConfig):
    host: str
    port: int
    username: str
    password: str
    ldoce_cache_key: str
    cambridge_cache_key: str

    @staticmethod
    def from_config(_config):
        cache_config = CacheConfig.from_config(_config)

        ldoce_section = _config[_LDOCE_SECTION]
        ldoce_cache_key = ldoce_section.get(_BUCKET_KEY)

        cambridge_section = _config[_CAMBRIDGE_SECTION]
        cambridge_cache_key = cambridge_section.get(_BUCKET_KEY)

        return LemmaMeaningsCacheConfig(host=cache_config.host, port=cache_config.port, username=cache_config.username,
                                        password=cache_config.password, ldoce_cache_key=ldoce_cache_key,
                                        cambridge_cache_key=cambridge_cache_key)
