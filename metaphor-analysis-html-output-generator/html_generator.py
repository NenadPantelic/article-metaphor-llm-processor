import json


ARTICLE_TEXT_TEMPLATE = "$$$ARTICLE_TEXT$$$"

analyzed_text_segments = []

with open("input/example.json", "r") as fin:
    analyzed_text_segments = json.load(fin).get("analyzed_text", [])


page_template = ""
with open("page_template.txt", "r") as fin:
    page_template = fin.read()


paragraph_segments = []


for segment in analyzed_text_segments:
    text = segment.get("text")
    if not text:
        continue

    metaphor_metadata = segment.get("metaphor_metadata", {})
    if not metaphor_metadata or metaphor_metadata.get("metaphor_type") == "NONE":
        paragraph_segments.append(text)
    else:
        span = f'''
        <span class="annotated">
                    {text}
                    <span class="tooltip">
                        {metaphor_metadata.get("explanation")}
                    </span>
                </span>
        '''
        paragraph_segments.append(span)

paragraph_text = f"<p>{''.join(paragraph_segments)}</p>"
html_text = page_template.replace(ARTICLE_TEXT_TEMPLATE, paragraph_text)

with open("index.html","w") as fout:
    fout.write(html_text)