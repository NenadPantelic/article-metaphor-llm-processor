import json
import os

ARTICLE_TEXT_TEMPLATE = "$$$ARTICLE_TEXT$$$"

def load_input_files_data(dir_path):
    files_contents = {}
    for root, _, files in os.walk(dir_path):
        for file in files:
            filepath = os.path.join(root, file)
            with open(filepath, "r", encoding='utf-8') as fin:
                content = fin.read()
                files_contents[filepath] = content

    return files_contents

def generate_html_per_input_file(files_contents: dict[str, dict[str, any]], page_template: str):
    for fp, fc in files_contents.items():
        data = json.loads(fc)
        analyzed_text_segments = data.get("analyzed_text", [])
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
                                Metaphor type: <b>{metaphor_metadata.get("metaphor_type")}</b>
                                <br>
                                {metaphor_metadata.get("explanation")}
                            </span>
                        </span>
                '''
                paragraph_segments.append(span)

        file_name = str(fp).split(".")[-2].split(os.sep)[1]
        url = data.get("url")
        title = f"<a href='{url}'><h2>{file_name}</h2></a>"
        paragraph_text = f"<p>{''.join(paragraph_segments)}</p>"
        article_text = f"{title}{paragraph_text}"
        html_text = page_template.replace(ARTICLE_TEXT_TEMPLATE, article_text)
        with open(f"output/{file_name}.html","w", encoding="utf-8") as fout:
            fout.write(html_text)



def generate_single_html_for_input_files(files_contents: dict[str, dict[str, any]], page_template: str):
    html_file_paragraphs = []
    for fp, fc in files_contents.items():
        data = json.loads(fc)
        analyzed_text_segments = data.get("analyzed_text", [])
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
                                Metaphor type: <b>{metaphor_metadata.get("metaphor_type")}</b>
                                <br>
                                {metaphor_metadata.get("explanation")}
                            </span>
                        </span>
                '''
                paragraph_segments.append(span)

        file_name = str(fp).split(".")[-2].split(os.sep)[1]
        url = data.get("url")
        title = f"<a href='{url}'><h2>{file_name}</h2></a>"
        paragraph_text = f"<p>{''.join(paragraph_segments)}</p>"
        article_text = f"{title}{paragraph_text}"
        html_text = page_template.replace(ARTICLE_TEXT_TEMPLATE, article_text)

        html_file_paragraphs.append(html_text)
        
    with open(f"output/output.html","w", encoding="utf-8") as fout:
        fout.write("<hr>".join(html_file_paragraphs))


if __name__ == "__main__":
    analyzed_text_segments = []

    files_contents = load_input_files_data("./input")
    page_template = ""
    with open("page_template.txt", "r") as fin:
        page_template = fin.read()
    
    #generate_html_per_input_file(files_contents, page_template)
    generate_single_html_for_input_files(files_contents,page_template)