from config.config_data import RabbitMQConfig
from helper.serialization import deserialize_body
from rabbitmq.rmq_consumer import RabbitMQConsumer

from data import Chunk
from rabbitmq.rmq_producer import RabbitMQProducer
from rabbitmq.serializable_message import SerializableMessage


class LexicalUnitProcessor(RabbitMQConsumer):
    def __init__(self, rabbitmq_config: RabbitMQConfig, queue: str, producer: RabbitMQProducer):
        super().__init__(rabbitmq_config, queue)
        self._producer = producer

    def process(self, chunk_data: Chunk) -> SerializableMessage:
        pass

    def callback(self, method, properties, body):
        # TODO: call the chunk-processing-state-api to update the status to processing
        try:
            data = deserialize_body(body)
            chunk_data = Chunk(**data)

            processed_data = self.process(chunk_data)
            # TODO: call the chunk-processing-state-api to update the status to complete

            self._producer.send(processed_data)
        except Exception as e:
            # TODO: call the chunk-processing-state-api to update the status to failed
            pass
