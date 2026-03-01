package com.example.perftester.rest;

import java.time.Instant;

public record TestRunSnapshotResponse(
        Long id,
        Long testRunId,
        Instant sampledAt,
        Integer outboundQueueDepth,
        Integer inboundQueueDepth,
        Long kafkaRequestsLag,
        Long kafkaResponsesLag) {
}
