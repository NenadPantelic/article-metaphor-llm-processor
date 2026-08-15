from misc.constants import RETRYABLE_STATUS_CODES


class ClientException(Exception):
    def __init__(self, message, status_code=None):
        super(ClientException, self).__init__(message)
        self.message = message
        self.status_code = status_code

    def is_retryable(self):
        return self.status_code in RETRYABLE_STATUS_CODES
