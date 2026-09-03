import json

from config.config_properties import RabbitMQConfig
from config.logconfig import get_logger
from rabbitmq.rabbitmq_handler import RabbitMQHandler

log = get_logger()


class RabbitMQPublisher(RabbitMQHandler):
    def __init__(self, rabbitmq_config: RabbitMQConfig, queue: str):
        super(RabbitMQPublisher, self).__init__(rabbitmq_config, queue)
        # Declare the queue (idempotent)
        self._channel.queue_declare(queue=queue)

    def publish(self, exchange="", routing_key="", body=None):
        if not body:
            log.info("Empty body, nothing to publish")
            return

        if not type(body) is str:
            ser_body = json.loads(body)
        else:
            ser_body = body

        self._channel.basic_publish(exchange=exchange, routing_key=routing_key, body=ser_body)
