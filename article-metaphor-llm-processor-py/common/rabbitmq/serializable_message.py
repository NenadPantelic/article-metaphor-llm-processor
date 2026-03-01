from abc import ABC, abstractmethod


class SerializableMessage(ABC):

    @abstractmethod
    def serialize(self) -> bytes:
        pass