### Processing flow

1. metaphor-processing-orchestrator sends the chunk text to processing; all subsequent components receive a payload needed to run their stage
2. to update the status of a chunk, each service in the pipeline calls the chunk-state-manager. That's a REST API service that has the following endpoints:

- PUT `/chunks/{chunkId}` - to update the state of that chunk
- GET `/chunks/{chunkId}` - to retrieve the complete chunk data
- GET `/chunks/{chunkId}/{stage}`

### Reprocessing flow

There are two cases when the reprocessing can happen:

1. A failure in one of the stages if the error is retryable
2. A user requests an explicit reprocessing (only the LLM processing makes sense)

#### A failure in one of the stages if the error is retryable

1. metaphor-processing-orchestrator dictates the execution of reprocessing - it sends only the chunkId
2. based on status of a chunk it knows to which queue to send the message
3. the target service retrieves the chunk data using a chunk-state-manager call

### A user requests an explicit reprocessing (only the LLM processing makes sense)

1. metaphor-processing-orchestrator sets the document status to `REPROCESSING`
2. it sends data (documentId) to the LLM service which reruns the processing

#### Example

1. the document is indexed, all its chunks are in `PENDING` state
2. when the processing starts, it moves it to `PENDING_PROCESSING` state
3. when the LU processing receives the message, it updates its status to `LEXICAL_UNIT_PROCESSING__IN_PROGRESS`

- if it succeeds, it moves it to `LEXICAL_UNIT_PROCESSING__COMPLETE`
- if it fails, it moves it to `LEXICAL_UNIT_PROCESSING__FAILED` with the information whether it should be retried or not

#### Which documents are eligible for reprocessing?

1. If their `shouldBeReprocessed` is true and the number of attempts is less than number of allowed attempts
2. If the user explicitly requested that
3. If they are stuck in some non-terminal and active state for longer than some period. If that's the case, their status is updated by the chunk-state-manager which has a cron for that purpose

Terminal states: `PROCESSING_COMPLETE` and `PROCESSING_FAILED`
Inactive state: `PENDING` and `PENDING_REPROCESSING`

#### How the reprocessing flow works now?
