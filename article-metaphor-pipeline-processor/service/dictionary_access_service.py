"""
Online Dictionary Lookup (Cambridge + LDOCE)

Fetches basic meanings directly from:
- https://dictionary.cambridge.org
- https://www.ldoceonline.com

No LLM usage.
"""
import enum
import time
from typing import List, Dict, Union

from cache.lemma_meaning_cache import LemmaMeaningsCache
from client.cambridge_dictionary_client import CambridgeDictionaryClient
from client.ldoce_dictionary_client import LDOCEDictionaryClient
from config.logconfig import get_logger

REQUEST_DELAY = 1.0  # seconds

log = get_logger()


class DictionaryType(enum.Enum):
    CAMBRIDGE = "CAMBRIDGE"
    LDOCE = "LDOCE"


class DictionaryAccessService:
    def __init__(self, cache: LemmaMeaningsCache = None):
        super().__init__()
        self._ldoce_client = LDOCEDictionaryClient()
        self._cambridge_client = CambridgeDictionaryClient()
        self._cache = cache

    def _lookup_cambridge(self, lemma: str) -> List[str]:
        """
        Do lookup the Cambridge dictionary.
        :param lemma: lemma to look up
        :return: a list of Cambridge explanations
        """
        cached_result = self._get_cached_result(lemma, DictionaryType.CAMBRIDGE)
        if cached_result:
            log.debug("Returning cached result")
            return cached_result

        meanings = self._cambridge_client.lookup(lemma)
        self._set_cached_result(lemma, meanings, DictionaryType.CAMBRIDGE)
        return meanings

    def _lookup_ldoce(self, lemma: str) -> List[str]:
        """
        Do lookup the LDOCE dictionary.
        :param lemma: a lemma to look up
        :return: a list of LDOCE explanations
        """
        cached_result = self._get_cached_result(lemma, DictionaryType.LDOCE)
        if cached_result:
            log.debug("Returning cached result")
            return cached_result

        meanings = self._ldoce_client.lookup(lemma)
        self._set_cached_result(lemma, meanings, DictionaryType.LDOCE)
        return meanings

    def _get_cached_result(self, key: str, dict_type: DictionaryType) -> Union[List[str], None]:
        if self._cache:
            log.debug("Cache is available")
            if dict_type == DictionaryType.LDOCE:
                return self._cache.get_ldoce_lemma_meanings(key)

            if dict_type == DictionaryType.CAMBRIDGE:
                return self._cache.get_cambridge_lemma_meanings(key)

            log.debug(f"Unknown dictionary type: {dict_type}, defaulting to None")

        log.debug("Cache unavailable, defaulting to None")
        return None

    def _set_cached_result(self, key, value, dict_type: DictionaryType) -> None:
        if self._cache:
            log.debug("Cache is available")
            if dict_type == DictionaryType.LDOCE:
                self._cache.set_ldoce_lemma_meanings(key, value)

            if dict_type == DictionaryType.CAMBRIDGE:
                self._cache.set_cambridge_lemma_meanings(key, value)

            log.debug(f"Unknown dictionary type: {dict_type}, skipping the cache update")
        else:
            log.debug("Cache unavailable, skipping the cache update")

    def lookup_basic_lemma_meanings(self, lemmas: List[str]) -> Dict[str, Dict[str, List[str]]]:
        """
        Returns:
        {
          "drown": {
            "cambridge": [...],
            "ldoce": [...]
          }
        }
        """

        results = {}

        for lemma in lemmas:
            print(f"NP: {lemma}")
            cambridge_defs = self._lookup_cambridge(lemma)
            log.debug(f"Cambridge defs: {cambridge_defs}")
            time.sleep(REQUEST_DELAY)

            ldoce_defs = self._lookup_ldoce(lemma)
            log.debug(f"LDOCE defs: {ldoce_defs}")
            time.sleep(REQUEST_DELAY)

            results[lemma] = {
                "cambridge": cambridge_defs,
                "ldoce": ldoce_defs
            }

        return results


#
# if __name__ == "__main__":
#     lexical_units = [
#         {
#             "lu_id": "lu_001",
#             "surface": "drowning",
#             "lemma": "drown",
#             "pos": "VERB",
#             "offset_start": 22,
#             "offset_end": 30
#         }
#     ]
#
#     unique_lemmas = ["drown"]
#
#     enriched = attach_meanings_to_lus(lexical_units, meaning_map)
#
#     import json
#
#     print(json.dumps(enriched, indent=2, ensure_ascii=False))

"""
<div class="def ddef_d db">to <a class="query" href="https://dictionary.cambridge.org/dictionary/english/die" rel="" title="die">die</a> by being <a class="query" href="https://dictionary.cambridge.org/dictionary/english/unable" rel="" title="unable">unable</a> to <a class="query" href="https://dictionary.cambridge.org/dictionary/english/breathe" rel="" title="breathe">breathe</a> <a class="query" href="https://dictionary.cambridge.org/dictionary/english/underwater" rel="" title="underwater">underwater</a>, or to <a class="query" href="https://dictionary.cambridge.org/dictionary/english/cause" rel="" title="cause">cause</a> a <a class="query" href="https://dictionary.cambridge.org/dictionary/english/person" rel="" title="person">person</a> or <a class="query" href="https://dictionary.cambridge.org/dictionary/english/animal" rel="" title="animal">animal</a> to <a class="query" href="https://dictionary.cambridge.org/dictionary/english/die" rel="" title="die">die</a> like this: </div>
todieby beingunabletobreatheunderwater, or tocauseapersonoranimaltodielike this:


"""
