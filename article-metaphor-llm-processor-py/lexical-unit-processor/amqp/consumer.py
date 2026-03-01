
from config.config_data import RabbitMQConfig
from helper.serialization import deserialize_body
from rabbitmq.rmq_consumer import RabbitMQConsumer

from data import Chunk

class ChunkConsumer(RabbitMQConsumer):
    def __init__(self, rabbitmq_config: RabbitMQConfig, queue: str):
        super().__init__(rabbitmq_config, queue)


    def callback(self, method, properties, body):
        data = deserialize_body(body)
        chunk_data = Chunk(**data)

