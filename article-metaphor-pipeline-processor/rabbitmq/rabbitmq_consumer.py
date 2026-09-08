from config.config_properties import RabbitMQConfig
from config.logconfig import get_logger
from rabbitmq.rabbitmq_handler import RabbitMQHandler

log = get_logger()


class RabbitMQConsumer(RabbitMQHandler):
    def __init__(self, rabbitmq_config: RabbitMQConfig, queue: str):
        super(RabbitMQConsumer, self).__init__(rabbitmq_config, queue)
        self._channel.basic_consume(queue=queue, on_message_callback=self.callback)

    def start(self):
        """
        Start the consumer.
        :return:
        """
        self._channel.start_consuming()

    def callback(self, method, properties, body):
        # TODO: add a log statement
        log.info(f"Received {body}")
        # Acknowledge the message to the broker
        self._channel.basic_ack(delivery_tag=method.delivery_tag)
