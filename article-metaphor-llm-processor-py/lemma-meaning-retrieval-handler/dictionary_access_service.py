"""
Online Dictionary Lookup (Cambridge + LDOCE)

Fetches basic meanings directly from:
- https://dictionary.cambridge.org
- https://www.ldoceonline.com

No LLM usage.
"""

import time
from typing import List, Dict

from client.cambridge_dictionary_client import CambridgeDictionaryClient
from client.ldoce_dictionary_client import LDOCEDictionaryClient

REQUEST_DELAY = 1.0  # seconds


# TODO: add caching

class DictionaryAccessService:
    def __init__(self):
        self._ldoce_client = LDOCEDictionaryClient()
        self._cambridge_client = CambridgeDictionaryClient()

    def lookup_cambridge(self, lemma: str) -> List[str]:
        """
        Do lookup the Cambridge dictionary.
        :param lemma: lemma to look up
        :return: a list of Cambridge explanations
        """
        # TODO: add caching
        return self._cambridge_client.lookup(lemma)

    def lookup_ldoce(self, lemma: str) -> List[str]:
        """
        Do lookup the LDOCE dictionary.
        :param lemma: a lemma to look up
        :return: a list of LDOCE explanations
        """
        # TODO: add caching
        return self._ldoce_client.lookup(lemma)

    def lookup_basic_meanings(self, lemmas: List[str]) -> Dict[str, Dict[str, List[str]]]:
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
            cambridge_defs = self.lookup_cambridge(lemma)
            time.sleep(REQUEST_DELAY)

            ldoce_defs = self.lookup_ldoce(lemma)
            time.sleep(REQUEST_DELAY)

            results[lemma] = {
                "cambridge": cambridge_defs,
                "ldoce": ldoce_defs
            }

        return results

    def get_lu_meanings(self, lexical_units, unique_lemmas):
        meaning_map = self.lookup_basic_meanings(unique_lemmas)
        enriched = []

        for lu in lexical_units:
            lemma = lu["lemma"]
            lu["basic_meanings"] = meaning_map.get(lemma, {
                "cambridge": [],
                "ldoce": []
            })
            enriched.append(lu)

        return enriched


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
