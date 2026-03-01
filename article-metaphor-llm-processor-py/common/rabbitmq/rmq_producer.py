from common.config.config_data import RabbitMQConfig
from common.exception.rmq_exception import RabbitMQException
from common.rabbitmq.rmq_handler import RabbitMQHandler
from rabbitmq.serializable_message import SerializableMessage


class RabbitMQProducer(RabbitMQHandler):
    def __init__(self, rabbitmq_config: RabbitMQConfig, exchange: str, routing_key: str, expected_message_type=None):
        super().__init__(rabbitmq_config)
        self._exchange = exchange
        self._routing_key = routing_key
        self._expected_message_type = expected_message_type

    def send(self, body: SerializableMessage = None):
        """
        Produces a message to RabbitMQ.
        :param body: the payload to send to the right queue.
        :return:
        """
        if self._expected_message_type and not isinstance(body, self._expected_message_type):
            raise TypeError(f"This producer expects only messages of type: {self._expected_message_type}")

        try:
            self._channel.basic_publish(exchange=self._exchange,
                                        routing_key=self._routing_key,
                                        body=body.serialize())
        except Exception as e:
            # TODO: is it retryable
            raise RabbitMQException(str(e))
