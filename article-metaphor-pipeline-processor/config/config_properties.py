import os
from configparser import RawConfigParser
from dataclasses import dataclass

# Service section/keys
_SERVICE_SECTION = "service"
_SERVICE_NAME_KEY = "name"

# MongoDB section/keys
_DB_SECTION = "db"
_DB_HOST_KEY = "host"
_DB_PORT_KEY = "port"
_DB_USERNAME_KEY = "username"
_DB_PASSWORD_KEY = "password"
_DB_DATABASE_KEY = "database"
_DB_CHUNKS_COLLECTION_KEY = "chunks_collection"
_DB_CHUNK_PROCESSING_STATE_COLLECTION_KEY = "chunk_processing_state_collection"

# RabbitMQ section/key and default values
_RABBITMQ_SECTION = "rabbitmq"
_RABBITMQ_HOST_KEY = "host"
_RABBITMQ_PORT_KEY = "port"
_RABBITMQ_VHOST_KEY = "vhost"
_RABBITMQ_USERNAME_KEY = "username"
_RABBITMQ_PASSWORD_KEY = "password"

_DEFAULT_HOST = "localhost"
_DEFAULT_RABBITMQ_PORT = 5672

# Cache section/keys and default values
_CACHE_SECTION = "cache"
_CACHE_HOST_KEY = "host"
_CACHE_PORT_KEY = "port"
_CACHE_USERNAME_KEY = "username"
_CACHE_PASSWORD_KEY = "password"
_DEFAULT_CACHE_HOST = "127.0.0.1"
_DEFAULT_CACHE_PORT = 6379

# Lemma meaning cache section/keys
_LDOCE_SECTION = "ldoce"
_CAMBRIDGE_SECTION = "cambridge"

_BUCKET_KEY = "cache-key"

# Assistant section/keys and default values
_ASSISTANT_SECTION = "assistant"
_ASSISTANT_NAME_KEY = "name"
_ASSISTANT_MODEL_KEY = "model"

_ASSISTANT_START_CONVERSATION_INSTRUCTION_KEY = "start_conversation_instruction"
_ASSISTANT_START_CONVERSATION_INSTRUCTION_FILE_PATH_KEY = "start_conversation_instruction_file_path"

_ASSISTANT_PROMPT_TEMPLATE_KEY = "assistant_prompt_template"
_ASSISTANT_PROMPT_TEMPLATE_FILE_KEY = "assistant_prompt_template_path"

_ASSISTANT_OPEN_API_KEY = "api_key"
_OPEN_API_KEY_ENV_VARIABLE = "OPENAI_API_KEY"

_DEFAULT_NAME = "MIPVU Metaphor Annotator"
_DEFAULT_MODEL = "gpt-4.1-mini"
_DEFAULT_INSTRUCTIONS = ""


# timeouts


@dataclass
class ServiceConfig:
    name: str

    @staticmethod
    def from_config(_config: RawConfigParser):
        service_section = _config[_SERVICE_SECTION]
        name = service_section.get(_SERVICE_NAME_KEY)

        return ServiceConfig(name=name)


@dataclass
class RabbitMQConfig:
    host: str
    port: int
    vhost: str
    username: str
    password: str

    @staticmethod
    def from_config(_config: RawConfigParser):
        rabbit_section = _config.get_section(_RABBITMQ_SECTION)

        host = rabbit_section.get(_RABBITMQ_HOST_KEY)
        port = rabbit_section.getint(_RABBITMQ_PORT_KEY)
        vhost = rabbit_section.get(_RABBITMQ_VHOST_KEY)
        username = rabbit_section.get(_RABBITMQ_USERNAME_KEY)
        password = rabbit_section.get(_RABBITMQ_PASSWORD_KEY)

        return RabbitMQConfig(host=host, port=port, vhost=vhost, username=username, password=password)


@dataclass
class DatabaseConfig:
    host: str
    port: int
    username: str
    password: str
    database: str
    chunks_collection: str
    chunk_processing_state_collection: str

    @staticmethod
    def from_config(_config: RawConfigParser):
        db_section = _config[_DB_SECTION]

        db_host = db_section.get(_DB_HOST_KEY)
        db_port = db_section.getint(_DB_PORT_KEY)
        db_username = db_section.get(_DB_USERNAME_KEY)
        db_password = db_section.get(_DB_PASSWORD_KEY)
        db_database = db_section.get(_DB_DATABASE_KEY)
        db_chunks_collection = db_section.get(_DB_CHUNKS_COLLECTION_KEY)
        db_chunk_processing_state_collection = db_section.get(_DB_CHUNK_PROCESSING_STATE_COLLECTION_KEY)

        return DatabaseConfig(db_host, db_port, db_username, db_password, db_database, db_chunks_collection,
                              db_chunk_processing_state_collection)


@dataclass
class CacheConfig:
    host: str
    port: int
    username: str
    password: str

    @staticmethod
    def from_config(_config: RawConfigParser):
        cache_section = _config[_CACHE_SECTION]

        host = cache_section.get(_CACHE_HOST_KEY)
        port = cache_section.getint(_CACHE_PORT_KEY, 0)
        username = cache_section.get(_CACHE_USERNAME_KEY, _DEFAULT_CACHE_HOST)
        password = cache_section.get(_CACHE_PASSWORD_KEY, _DEFAULT_CACHE_PORT)

        return CacheConfig(host=host, port=port, username=username, password=password)


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


def read_env_variable(env_variable: str) -> str:
    """
    Reads the environment variable and returns its value.

    :param env_variable: the environment variable to read
    :return: value of the environment variable
    """
    return os.environ.get(env_variable)


@dataclass
class AssistantConfig:
    name: str = _DEFAULT_NAME
    model: str = _DEFAULT_MODEL
    start_conversation_instruction: str = ""
    assistant_prompt_template: str = ""
    api_key: str = ""

    @staticmethod
    def from_config(_config):
        assistant_section = _config[_ASSISTANT_SECTION]
        name = assistant_section.get(_ASSISTANT_NAME_KEY)
        model = assistant_section.get(_ASSISTANT_MODEL_KEY)
        start_conversation_instruction = assistant_section.get(_ASSISTANT_START_CONVERSATION_INSTRUCTION_KEY)
        prompt = assistant_section.get(_ASSISTANT_PROMPT_TEMPLATE_KEY)
        api_key = assistant_section.get(_ASSISTANT_OPEN_API_KEY) or read_env_variable(_OPEN_API_KEY_ENV_VARIABLE)

        if not start_conversation_instruction:
            with open(assistant_section.get(_ASSISTANT_START_CONVERSATION_INSTRUCTION_FILE_PATH_KEY), "r") as f:
                start_conversation_instruction = f.read()

        if not prompt:
            with open(assistant_section.get(_ASSISTANT_PROMPT_TEMPLATE_FILE_KEY), "r") as f:
                prompt = f.read()

        return AssistantConfig(name=name, model=model, start_conversation_instruction=start_conversation_instruction,
                               assistant_prompt_template=prompt, api_key=api_key)
