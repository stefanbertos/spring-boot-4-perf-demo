package com.example.perftester.perf;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PerformanceTrackerTest {

    private PerformanceTracker tracker;
    private MeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        tracker = new PerformanceTracker(meterRegistry);
    }

    @Test
    void startTestShouldInitializeState() {
        tracker.startTest(10);

        PerfTestResult result = tracker.getResult();
        assertEquals(0, result.completedMessages());
        assertEquals(0, result.pendingMessages());
    }

    @Test
    void recordSendAndReceiveShouldTrackMessages() throws InterruptedException {
        tracker.startTest(1);

        tracker.recordSend("msg-1");
        Thread.sleep(10); // Small delay to ensure measurable latency
        tracker.recordReceive("msg-1");

        PerfTestResult result = tracker.getResult();
        assertEquals(1, result.completedMessages());
        assertEquals(0, result.pendingMessages());
        assertTrue(result.avgLatencyMs() > 0);
    }

    @Test
    void recordReceiveForUnknownMessageShouldNotCount() {
        tracker.startTest(1);

        tracker.recordReceive("unknown-msg");

        PerfTestResult result = tracker.getResult();
        assertEquals(0, result.completedMessages());
    }

    @Test
    void awaitCompletionShouldReturnTrueWhenAllMessagesReceived() throws InterruptedException {
        tracker.startTest(2);

        tracker.recordSend("msg-1");
        tracker.recordSend("msg-2");
        tracker.recordReceive("msg-1");
        tracker.recordReceive("msg-2");

        assertTrue(tracker.awaitCompletion(1, TimeUnit.SECONDS));
    }

    @Test
    void awaitCompletionShouldReturnFalseOnTimeout() throws InterruptedException {
        tracker.startTest(2);

        tracker.recordSend("msg-1");
        tracker.recordReceive("msg-1");
        // msg-2 never received

        assertFalse(tracker.awaitCompletion(100, TimeUnit.MILLISECONDS));
    }

    @Test
    void awaitCompletionWithoutStartShouldReturnTrue() throws InterruptedException {
        assertTrue(tracker.awaitCompletion(1, TimeUnit.SECONDS));
    }

    @Test
    void getResultShouldCalculateStatistics() throws InterruptedException {
        tracker.startTest(3);

        for (int i = 0; i < 3; i++) {
            String msgId = "msg-" + i;
            tracker.recordSend(msgId);
            Thread.sleep(5);
            tracker.recordReceive(msgId);
        }

        PerfTestResult result = tracker.getResult();
        assertEquals(3, result.completedMessages());
        assertEquals(0, result.pendingMessages());
        assertTrue(result.tps() > 0);
        assertTrue(result.avgLatencyMs() > 0);
        assertTrue(result.minLatencyMs() > 0);
        assertTrue(result.maxLatencyMs() >= result.minLatencyMs());
    }

    @Test
    void getResultWithNoCompletedMessagesShouldReturnZeroLatency() {
        tracker.startTest(1);

        PerfTestResult result = tracker.getResult();
        assertEquals(0, result.avgLatencyMs());
        assertEquals(0, result.minLatencyMs());
    }

    @Test
    void extractMessageIdShouldParseValidMessage() {
        String messageId = PerformanceTracker.extractMessageId("abc-123|payload data");
        assertEquals("abc-123", messageId);
    }

    @Test
    void extractMessageIdShouldReturnNullForInvalidMessage() {
        assertNull(PerformanceTracker.extractMessageId("no separator"));
        assertNull(PerformanceTracker.extractMessageId("|starts with separator"));
    }

    @Test
    void createMessageShouldFormatCorrectly() {
        String message = PerformanceTracker.createMessage("msg-id", "test payload");
        assertEquals("msg-id|test payload", message);
    }

    @Test
    void minMaxLatencyShouldTrackExtremesCorrectly() throws InterruptedException {
        tracker.startTest(3);

        // Send all messages first
        tracker.recordSend("fast");
        Thread.sleep(5);
        tracker.recordReceive("fast");

        tracker.recordSend("slow");
        Thread.sleep(20);
        tracker.recordReceive("slow");

        tracker.recordSend("medium");
        Thread.sleep(10);
        tracker.recordReceive("medium");

        PerfTestResult result = tracker.getResult();
        assertTrue(result.minLatencyMs() < result.maxLatencyMs());
    }

    @Test
    void pendingMessagesShouldTrackUnreceivedMessages() {
        tracker.startTest(3);

        tracker.recordSend("msg-1");
        tracker.recordSend("msg-2");
        tracker.recordSend("msg-3");
        tracker.recordReceive("msg-1");

        PerfTestResult result = tracker.getResult();
        assertEquals(1, result.completedMessages());
        assertEquals(2, result.pendingMessages());
    }
}
