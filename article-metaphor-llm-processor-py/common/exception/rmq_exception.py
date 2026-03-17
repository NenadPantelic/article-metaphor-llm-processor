class RabbitMQException(Exception):
    def __init__(self, message, is_retryable=False):
        super(RabbitMQException, self).__init__(message)
        self.is_retryable = is_retryable