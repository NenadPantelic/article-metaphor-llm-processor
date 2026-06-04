from pika import BlockingConnection, PlainCredentials, ConnectionParameters

from config.config_properties import RabbitMQConfig


class RabbitMQConsumer:
    def __init__(self, rabbitmq_config: RabbitMQConfig, queue: str):
        credentials = PlainCredentials(rabbitmq_config.username, rabbitmq_config.password)
        parameters = ConnectionParameters(host=rabbitmq_config.host, port=rabbitmq_config.port,
                                          virtual_host=rabbitmq_config.vhost, credentials=credentials)
        self._connection = BlockingConnection(parameters=parameters)
        self._channel = self._connection.channel()

        self._queue = queue

        self._channel.basic_consume(
            queue=self._queue, on_message_callback=self.callback)

    def start(self):
        """
        Start the consumer.
        :return:
        """
        self._channel.start_consuming()

    def callback(self, method, properties, body):
        # TODO: add a log statement
        # Acknowledge the message to the broker
        self._channel.basic_ack(delivery_tag=method.delivery_tag)

    def close(self):
        """
        Close the connection.
        :return None
        """
        if self._connection.is_open():
            self._connection.close()
