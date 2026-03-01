import configparser

from helper.singleton import SingletonMeta


class Config(metaclass=SingletonMeta):
    def __init__(self, file="config.ini"):
        self._config = configparser.ConfigParser().read(file)


config = Config("../config_data/config.ini")