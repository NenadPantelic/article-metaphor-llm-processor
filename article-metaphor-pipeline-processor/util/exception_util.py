def is_error_retryable(e: Exception) -> bool:
    """
    Is the exception retryable?
    :param e: exception to be assessed
    :return: a boolean indicating if exception is retryable
    """
    return isinstance(e, (TimeoutError,))
