from exception.client_exception import ClientException
from exception.rmq_exception import RabbitMQException


def is_error_retryable(e: Exception) -> bool:
    """
    Is the exception retryable?
    :param e: exception to be assessed
    :return: a boolean indicating if exception is retryable
    """
    if isinstance(e, RabbitMQException):
        return e.is_retryable

    if isinstance(e, ClientException):
        return e.is_retryable()

    return isinstance(e, (TimeoutError,))
