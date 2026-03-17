from common.config.config_data import RabbitMQConfig
from common.rabbitmq.rmq_handler import RabbitMQHandler


class RabbitMQConsumer(RabbitMQHandler):
    def __init__(self, rabbitmq_config: RabbitMQConfig, queue: str):
        super().__init__(rabbitmq_config)
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
