import logging
import sys

DEFAULT_LOGGING_LEVEL = logging.INFO
DEFAULT_LOGGING_FORMAT = "[%(asctime)s] [%(levelname)s] %(message)s"


class LogConfig:
    def __init__(self):
        self._name = None
        self._level = logging.INFO

        self._console_logger_level = logging.INFO
        self._console_format = ""

        self._file_logger_level = logging.INFO
        self._file_format = ""
        self._file_filename = ""

    def with_name(self, name) -> "LogConfig":
        self._name = name
        return self

    def with_level(self, level) -> "LogConfig":
        self._level = level
        return self

    def with_console_logger_level(self, level) -> "LogConfig":
        self._console_logger_level = level
        return self

    def with_console_format(self, fmt) -> "LogConfig":
        self._console_format = fmt
        return self

    def with_file_logger_level(self, level) -> "LogConfig":
        self._file_logger_level = level
        return self

    def with_file_format(self, fmt) -> "LogConfig":
        self._file_format = fmt
        return self

    def with_file_filename(self, filename) -> "LogConfig":
        self._file_filename = filename
        return self

    def build(self) -> logging.Logger:
        logger = logging.getLogger(self._name)
        logger.setLevel(self._level)

        console_handler = logging.StreamHandler(sys.stdout)
        console_handler.setLevel(self._console_logger_level)

        console_formatter = logging.Formatter(self._console_format)
        console_handler.setFormatter(console_formatter)

        file_handler = logging.FileHandler(self._file_filename, mode='a')
        file_handler.setLevel(self._file_logger_level)

        file_formatter = logging.Formatter(self._file_format)
        file_handler.setFormatter(file_formatter)

        logger.addHandler(console_handler)
        logger.addHandler(file_handler)

        return logger

    @staticmethod
    def default(name: str, filename: str) -> logging.Logger:
        return LogConfig().with_name(name).with_level(logging.INFO).with_console_logger_level(
            DEFAULT_LOGGING_LEVEL).with_console_format(DEFAULT_LOGGING_FORMAT).with_file_logger_level(
            DEFAULT_LOGGING_LEVEL).with_file_format(DEFAULT_LOGGING_FORMAT).with_file_filename(filename).build()
