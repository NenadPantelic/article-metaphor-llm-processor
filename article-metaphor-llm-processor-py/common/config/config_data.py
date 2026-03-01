import os
from dataclasses import dataclass
from config import config

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

# RabbitMQ section/key and default values
_RABBITMQ_SECTION = "rabbitmq"
_RABBITMQ_HOST_KEY = "host"
_RABBITMQ_PORT_KEY = "port"
_RABBITMQ_VHOST_KEY = "vhost"
_RABBITMQ_PASSWORD_KEY = "password"

_DEFAULT_HOST = "localhost"
_DEFAULT_RABBITMQ_PORT = 5672


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
class RabbitMQConfig:
    host: str
    port: int
    vhost: str
    username: str
    password: str

    @staticmethod
    def from_config(_config):
        rabbit_section = _config[_RABBITMQ_SECTION]

        host = rabbit_section.get(_RABBITMQ_HOST_KEY)
        port = rabbit_section.get(_RABBITMQ_PORT_KEY)
        vhost = rabbit_section.get(_RABBITMQ_VHOST_KEY)
        username = rabbit_section.get(_RABBITMQ_VHOST_KEY)
        password = rabbit_section.get(_RABBITMQ_PASSWORD_KEY)

        return RabbitMQConfig(host=host, port=port, vhost=vhost, username=username, password=password)


assistant_config = AssistantConfig.from_config(config)
rabbitmq_config = RabbitMQConfig.from_config(config)
