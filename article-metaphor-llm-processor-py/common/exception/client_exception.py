_RETRYABLE_STATUS_CODES = (429, 502, 503,)


class ClientException(Exception):
    def __init__(self, message, status_code=None):
        super(ClientException, self).__init__(message)
        self.status_code = status_code

    def is_retryable(self):
        return self.status_code in _RETRYABLE_STATUS_CODES