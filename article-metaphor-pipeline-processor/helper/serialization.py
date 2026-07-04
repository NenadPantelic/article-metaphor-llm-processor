from json import dumps, loads

def serialize_body(body: dict) -> str:
    """
    Serialize the body into stringified JSON.
    :param body: The body to be serialized.
    :return: Serialized body.
    """
    return dumps(body)


def deserialize_body(body: str) -> dict:
    """
    Deserialize the body into dictionary.
    :param body: a body to be deserialized.
    :return: deserialized body.
    """
    return loads(body)