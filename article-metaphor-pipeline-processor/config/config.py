import configparser
from configparser import RawConfigParser


def get_config(file="config.ini") -> RawConfigParser:
    config = configparser.ConfigParser()
    config.read(file)
    return config
