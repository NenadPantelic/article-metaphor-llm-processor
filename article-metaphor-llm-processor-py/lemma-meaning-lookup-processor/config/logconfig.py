from config.logconfig import LogConfig
from .config import service_config

_FILENAME = ""

def get_logger(file_name: str = None):
    name = service_config.name
    filename = file_name or f"{name}.log"
    return LogConfig.default(name, filename)
