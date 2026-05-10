import os
from dataclasses import dataclass
from config.config import Config

# Assistant section/keys and default values
_ASSISTANT_SECTION = "assistant"
_ASSISTANT_NAME_KEY = "name"
_ASSISTANT_MODEL_KEY = "model"
_ASSISTANT_PROMPT_FILE_KEY = "prompt_file_path"
_ASSISTANT_OPEN_API_KEY = "api_key"
_OPEN_API_KEY_ENV_VARIABLE = "OPENAI_API_KEY"

_DEFAULT_NAME = "MIPVU Metaphor Annotator"
_DEFAULT_MODEL = "gpt-4.1-mini"
_DEFAULT_INSTRUCTIONS = ""

# Database section and default values
_DB_SECTION = "db"
_DB_DATABASE = "database"


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
    prompt: str = _DEFAULT_INSTRUCTIONS
    api_key: str = ""

    @staticmethod
    def from_config(_config):
        assistant_section = _config[_ASSISTANT_SECTION]
        name = assistant_section.get(_ASSISTANT_NAME_KEY)
        model = assistant_section.get(_ASSISTANT_MODEL_KEY)
        prompt = ""
        api_key = assistant_section.get(_ASSISTANT_OPEN_API_KEY) or read_env_variable(_OPEN_API_KEY_ENV_VARIABLE)

        with open(assistant_section.get(_ASSISTANT_PROMPT_FILE_KEY), "r") as f:
            prompt = f.read()

        return AssistantConfig(name=name, model=model, prompt=prompt, api_key=api_key)


@dataclass
class DBConfig:
    # host, port, credentials...
    database: str

    @staticmethod
    def from_config(_config):
        db_section = _config[_DB_SECTION]
        database = db_section.get(_DB_DATABASE)
        return DBConfig(database)


config = Config("./config.ini")
assistant_config = AssistantConfig.from_config(config)
db_config = DBConfig.from_config(config)

