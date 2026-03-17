from typing import List
from bs4 import BeautifulSoup

from client.http_client import HttpClient, HttpMethod

HEADERS = {
    "User-Agent": "Mozilla/5.0 (academic-research-bot)"
}

HTML_SELECTOR = ".def.ddef_d.db"


class CambridgeDictionaryClient(HttpClient):
    def __init__(self):
        super().__init__(scheme="https", host="www.cambridge.org", port="",
                         api_base_path="", headers=HEADERS)

    def lookup(self, lemma: str) -> List[str]:
        """
        Do lookup the Cambridge dictionary.
        :param lemma: a lemma to be looked up
        :return: a list of meanings
        """
        data = self._call(HttpMethod.GET, f"dictionary/english/{lemma}", to_json=False)
        soup = BeautifulSoup(data, "lxml")
        definitions = []

        for def_block in soup.select(HTML_SELECTOR):
            text = def_block.get_text()
            if text:
                definitions.append(text.strip())

        return definitions
