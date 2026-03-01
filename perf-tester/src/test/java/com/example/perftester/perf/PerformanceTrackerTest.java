package com.example.perftester.perf;

import com.example.perftester.config.PerfProperties;
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
        var perfProperties = new PerfProperties(16000, 60000, 60000, 30000, 60, 15);
        tracker = new PerformanceTracker(meterRegistry, perfProperties);
    }

    @Test
    void startTestShouldInitializeState() {
        tracker.tryStart(10, "test-run");

        PerfTestResult result = tracker.getResult();
        assertEquals(0, result.completedMessages());
        assertEquals(0, result.pendingMessages());
    }

    @Test
    void recordSendAndReceiveShouldTrackMessages() throws InterruptedException {
        tracker.tryStart(1, "test-run");

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
        tracker.tryStart(1, "test-run");

        tracker.recordReceive("unknown-msg");

        PerfTestResult result = tracker.getResult();
        assertEquals(0, result.completedMessages());
    }

    @Test
    void awaitCompletionShouldReturnTrueWhenAllMessagesReceived() throws InterruptedException {
        tracker.tryStart(2, "test-run");

        tracker.recordSend("msg-1");
        tracker.recordSend("msg-2");
        tracker.recordReceive("msg-1");
        tracker.recordReceive("msg-2");

        assertTrue(tracker.awaitCompletion(1, TimeUnit.SECONDS));
    }

    @Test
    void awaitCompletionShouldReturnFalseOnTimeout() throws InterruptedException {
        tracker.tryStart(2, "test-run");

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
        tracker.tryStart(3, "test-run");

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
        tracker.tryStart(1, "test-run");

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
        tracker.tryStart(3, "test-run");

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
        tracker.tryStart(3, "test-run");

        tracker.recordSend("msg-1");
        tracker.recordSend("msg-2");
        tracker.recordSend("msg-3");
        tracker.recordReceive("msg-1");

        PerfTestResult result = tracker.getResult();
        assertEquals(1, result.completedMessages());
        assertEquals(2, result.pendingMessages());
    }

    @Test
    void updateMinShouldNotUpdateWhenValueIsLarger() throws InterruptedException {
        tracker.tryStart(2, "test-run");

        // First message with short latency
        tracker.recordSend("short");
        Thread.sleep(5);
        tracker.recordReceive("short");

        double firstMin = tracker.getResult().minLatencyMs();

        // Second message with longer latency should not update min
        tracker.recordSend("long");
        Thread.sleep(50);
        tracker.recordReceive("long");

        double secondMin = tracker.getResult().minLatencyMs();

        // Min should remain the same (from first short message)
        assertEquals(firstMin, secondMin, 1.0);
    }

    @Test
    void updateMaxShouldNotUpdateWhenValueIsSmaller() throws InterruptedException {
        tracker.tryStart(2, "test-run");

        // First message with long latency
        tracker.recordSend("long");
        Thread.sleep(50);
        tracker.recordReceive("long");

        double firstMax = tracker.getResult().maxLatencyMs();

        // Second message with shorter latency should not update max
        tracker.recordSend("short");
        Thread.sleep(5);
        tracker.recordReceive("short");

        double secondMax = tracker.getResult().maxLatencyMs();

        // Max should remain approximately the same (from first long message)
        assertTrue(secondMax >= firstMax - 5); // Allow small timing variance
    }

    @Test
    void recordReceiveWithNullCompletionLatchShouldWork() {
        // Don't call startTest to keep completionLatch null
        tracker.recordSend("msg-1");
        tracker.recordReceive("msg-1");

        // Should not throw
        PerfTestResult result = tracker.getResult();
        assertEquals(1, result.completedMessages());
    }

    @Test
    void getProgressSnapshotShouldReturnCurrentState() throws InterruptedException {
        tracker.tryStart(5, "snapshot-test");

        tracker.recordSend("msg-1");
        Thread.sleep(5);
        tracker.recordReceive("msg-1");

        var snapshot = tracker.getProgressSnapshot();
        assertEquals("snapshot-test", snapshot.testRunId());
        assertEquals("RUNNING", snapshot.status());
        assertEquals(5, snapshot.totalCount());
        assertTrue(snapshot.completedCount() >= 1);
        assertTrue(snapshot.progressPercent() > 0);
    }

    @Test
    void setStatusShouldUpdateStatus() {
        tracker.tryStart(1, "status-test");
        assertEquals("RUNNING", tracker.getProgressSnapshot().status());

        tracker.setStatus("COMPLETED");
        assertEquals("COMPLETED", tracker.getProgressSnapshot().status());
    }

    @Test
    void startWarmupPhaseShouldSetupAndTimeoutWhenNoMessages() throws InterruptedException {
        tracker.startWarmupPhase(2);
        // awaitWarmupCompletion times out because no messages arrive
        assertFalse(tracker.awaitWarmupCompletion(100, TimeUnit.MILLISECONDS));
    }

    @Test
    void awaitWarmupCompletionShouldReturnTrueWhenLatchIsNull() throws InterruptedException {
        // warmupLatch is null by default (startWarmupPhase not called)
        assertTrue(tracker.awaitWarmupCompletion(100, TimeUnit.MILLISECONDS));
    }

    @Test
    void recordReceiveDuringWarmupShouldCountDownLatch() throws InterruptedException {
        tracker.startWarmupPhase(1);
        tracker.recordSend("warmup-msg");
        tracker.recordReceive("warmup-msg");
        // Latch is counted down — should complete immediately
        assertTrue(tracker.awaitWarmupCompletion(100, TimeUnit.MILLISECONDS));
    }

    @Test
    void calculateWindowedTpsShouldCleanupOldTimestamps() throws InterruptedException {
        // Use 1ms TPS window so timestamps immediately age out
        var shortWindowProperties = new PerfProperties(100, 1, 60000, 30000, 60, 15);
        var shortWindowTracker = new PerformanceTracker(new SimpleMeterRegistry(), shortWindowProperties);
        shortWindowTracker.tryStart(1, "tps-window-test");
        shortWindowTracker.recordSend("msg-1");
        shortWindowTracker.recordReceive("msg-1");

        Thread.sleep(5); // Allow the 1ms window to expire

        // getProgressSnapshot calls calculateWindowedTps which removes old timestamps
        var snapshot = shortWindowTracker.getProgressSnapshot();
        assertEquals(0.0, snapshot.tps(), 0.01);
    }
}
