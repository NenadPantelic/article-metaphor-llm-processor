import json

from pika import BlockingConnection, PlainCredentials, ConnectionParameters
from json import dumps
from common.config.config_data import RabbitMQConfig


class RabbitMQHandler:
    def __init__(self, rabbitmq_config: RabbitMQConfig):
        credentials = PlainCredentials(rabbitmq_config.username, rabbitmq_config.password)
        parameters = ConnectionParameters(host=rabbitmq_config.host, port=rabbitmq_config.port,
                                               virtual_host=rabbitmq_config.vhost, credentials=credentials)
        self._connection = BlockingConnection(parameters=parameters)
        self._channel = self._connection.channel()

    def close(self):
        """
        Close the connection.
        :return None
        """
        if self._connection.is_open():
            self._connection.close()