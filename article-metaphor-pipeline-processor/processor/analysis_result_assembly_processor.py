from config.logconfig import get_logger
from data.processing_milestone import ProcessingMilestone
from model.processing_data import ProcessingData, ArticleMetaphorAnalysis, AnalyzedTextSegment, MetaphorMetadata, \
    AnalyzedText
from processor.step_processor import StepProcessor

log = get_logger()


def add_missing_position_intervals(positions: list[tuple[int, int]], text_length: int) -> None:
    # 13-16
    # 22-28
    # 31-39
    first_position = 0
    next_position = 0
    no_of_positions = len(positions)
    for i in range(no_of_positions):
        mp = positions[i]
        next_position = mp[0]

        if first_position != next_position - 1:
            positions.append((first_position, next_position - 1))

        first_position = mp[1] + 1

    next_position = text_length
    if first_position != next_position:
        positions.append((first_position, next_position - 1))


class AnalysisResultAssemblyProcessor(StepProcessor):
    def __init__(self):
        super().__init__(ProcessingMilestone.RESULT_ASSEMBLY)

    def execute(self, article_metaphor_analysis: ArticleMetaphorAnalysis, document_id: str, text: str,
                last_chunk=False) -> ProcessingData:
        if not last_chunk:
            log.info("Not the last chunk, result assembly will not be executed")
            return None  # TODO

        log.info(f"Assembling results for document {document_id}.")

        metaphors = article_metaphor_analysis.metaphors
        analyzed_text_segments = []
        if not metaphors:
            # TODO: I need a chunk id here
            log.info(f"No metaphors found for {document_id}")
            analyzed_text_segments.append(AnalyzedTextSegment(text, None))
        else:
            log.info(f"Assembling metaphor results: document_id: {document_id}, no of metaphors: {len(metaphors)}")
            # (start_pos, end_pos) -> metaphor_text
            # if metaphor_text is None, take the text as is from the source text
            # True -> is metaphor, take it from metaphor
            metaphor_pos_to_metaphors_map = {(m.position_start, m.position_end): m for m in metaphors}
            metaphor_positions = list(metaphor_pos_to_metaphors_map.keys())
            add_missing_position_intervals(metaphor_positions, len(text))
            metaphor_positions.sort(key=lambda x: x[0])

            for met_position in metaphor_positions:
                metaphor = metaphor_pos_to_metaphors_map.get(met_position)
                if metaphor is None:
                    # start position, end position
                    segment_text = text[met_position[0]:met_position[1] + 1]
                    analyzed_text_segments.append(AnalyzedTextSegment(segment_text, None))
                else:
                    segment_text = metaphor.expression
                    metaphor_metadata = MetaphorMetadata(metaphor.metaphor_type, metaphor.explanation)
                    analyzed_text_segments.append(AnalyzedTextSegment(segment_text, metaphor_metadata))

        return AnalyzedText(analyzed_text_segments)
