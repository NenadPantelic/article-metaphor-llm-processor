import logging
import random
import time
from functools import wraps
from typing import Callable

import openai
import requests

from exception.client_exception import ClientException
from misc.constants import RETRYABLE_STATUS_CODES

logger = logging.getLogger(__name__)

_LOCKED_CONVERSATION_ERROR = "Another process is currently operating on this conversation. Please retry in a few seconds."


def retry_http(
        *,
        retries: int = 3,
        retry_exceptions: tuple[type[Exception], ...] = (
                requests.Timeout,
                requests.ConnectionError,
        ),
        retry_statuses: tuple[int, ...] = RETRYABLE_STATUS_CODES,
        backoff: float = 1.0,
        backoff_multiplier: float = 2.0,
        max_backoff: float = 30.0,
        jitter: float = 0.5,
):
    def decorator(func: Callable):
        @wraps(func)
        def wrapper(*args, **kwargs):
            delay = backoff

            for attempt in range(retries + 1):
                try:
                    response = func(*args, **kwargs)
                    if response.status_code not in retry_statuses:
                        return response

                    if attempt == retries:
                        logger.error(
                            "HTTP request failed after %d retries: "
                            "status=%s, url=%s",
                            retries,
                            response.status_code,
                            response.url,
                        )
                        return response

                    retry_after = response.headers.get("Retry-After")

                    if retry_after is not None:
                        try:
                            sleep_for = float(retry_after)
                        except ValueError:
                            sleep_for = delay
                    else:
                        sleep_for = delay

                    sleep_for += random.uniform(0, jitter)

                    logger.warning(
                        "HTTP request failed, retrying: "
                        "attempt=%d/%d, status=%s, url=%s, "
                        "retry_in=%.2fs",
                        attempt + 1,
                        retries + 1,
                        response.status_code,
                        response.url,
                        sleep_for,
                    )

                    time.sleep(sleep_for)

                    delay = min(
                        delay * backoff_multiplier,
                        max_backoff,
                    )

                except retry_exceptions as exc:
                    if attempt == retries:
                        logger.exception(
                            "HTTP request failed after %d retries",
                            retries,
                        )
                        raise

                    if isinstance(exc, ClientException):
                        if not exc.is_retryable():
                            logger.exception(f"HTTP request failed with {exc.message}, retries = {retries}")
                            raise

                    sleep_for = (
                            delay +
                            random.uniform(0, jitter)
                    )

                    logger.warning(
                        "HTTP request raised %s, retrying: "
                        "attempt=%d/%d, retry_in=%.2fs, error=%s",
                        type(exc).__name__,
                        attempt + 1,
                        retries + 1,
                        sleep_for,
                        exc,
                    )

                    time.sleep(sleep_for)

                    delay = min(
                        delay * backoff_multiplier,
                        max_backoff,
                    )

        return wrapper

    return decorator


def retry_openai_request(
        retries: int = 3,
        backoff: float = 1.0,
        backoff_multiplier: float = 2.0,
        max_backoff: float = 30.0,
        jitter: float = 0.5,
):
    def decorator(func: Callable):
        @wraps(func)
        def wrapper(*args, **kwargs):
            delay = backoff

            for attempt in range(retries + 1):
                try:
                    response = func(*args, **kwargs)

                    if attempt == retries:
                        logger.error(
                            "OpenAI client request failed after %d retries: "
                            "status=%s, url=%s",
                            retries,
                            response.status_code,
                            response.url,
                        )
                        return response

                    sleep_for = delay + random.uniform(0, jitter)
                    logger.warning(
                        "OpenAI HTTP request failed, retrying: "
                        "attempt=%d/%d, status=%s, url=%s, "
                        "retry_in=%.2fs",
                        attempt + 1,
                        retries + 1,
                        response.status_code,
                        response.url,
                        sleep_for,
                    )

                    time.sleep(sleep_for)

                    delay = min(
                        delay * backoff_multiplier,
                        max_backoff,
                    )

                except (openai.BadRequestError, openai.RateLimitError) as exc:
                    if attempt == retries:
                        logger.exception(
                            "OpenAI HTTP request failed after %d retries",
                            retries,
                        )
                        raise

                    if isinstance(exc, openai.BadRequestError) and _LOCKED_CONVERSATION_ERROR not in exc.message:
                        raise

                    sleep_for = (
                            delay +
                            random.uniform(0, jitter)
                    )

                    logger.warning(
                        "HTTP request raised %s, retrying: "
                        "attempt=%d/%d, retry_in=%.2fs, error=%s",
                        type(exc).__name__,
                        attempt + 1,
                        retries + 1,
                        sleep_for,
                        exc,
                    )

                    time.sleep(sleep_for)

                    delay = min(
                        delay * backoff_multiplier,
                        max_backoff,
                    )

        return wrapper

    return decorator
