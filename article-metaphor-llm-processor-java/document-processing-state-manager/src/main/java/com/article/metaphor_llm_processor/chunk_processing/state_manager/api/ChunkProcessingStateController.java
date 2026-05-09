package com.article.metaphor_llm_processor.chunk_processing.state_manager.api;

import com.article.metaphor_llm_processor.chunk_processing.state_manager.dto.in.ChunkProcessingStateUpdate;
import com.article.metaphor_llm_processor.chunk_processing.state_manager.dto.out.ChunkStateUpdateResult;
import com.article.metaphor_llm_processor.chunk_processing.state_manager.service.ChunkProcessingStateService;
import com.article.metaphor_llm_processor.chunk_processing.state_manager.dto.out.ChunkProcessingStateData;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/processing-chunks")
public class ChunkProcessingStateController {

    private final ChunkProcessingStateService chunkProcessingStateService;

    public ChunkProcessingStateController(ChunkProcessingStateService chunkProcessingStateService) {
        this.chunkProcessingStateService = chunkProcessingStateService;
    }

    @PutMapping("/{chunkId}")
    public ChunkStateUpdateResult updateChunkProcessingState(@PathVariable("chunkId") String chunkId,
                                                             @Valid @RequestBody ChunkProcessingStateUpdate newState) {
        log.info("Received a request to update the processing chunk state");
        return chunkProcessingStateService.updateChunkProcessingState(chunkId, newState);
    }

    @GetMapping("/{chunkId}/{state}")
    public ChunkProcessingStateData getProcessingChunk(@PathVariable("chunkId") String chunkId,
                                                       @PathVariable("state") String state) {
        log.info("Received a request to get the processing chunk");
        return chunkProcessingStateService.getChunkProcessingInfo(chunkId, state);
    }

}
