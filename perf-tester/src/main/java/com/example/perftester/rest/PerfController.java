package com.example.perftester.rest;

import com.example.perftester.messaging.MessageSender;
import com.example.perftester.perf.PerfTestResult;
import com.example.perftester.perf.PerformanceTracker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/api/perf")
@RequiredArgsConstructor
public class PerfController {

    private final MessageSender messageSender;
    private final PerformanceTracker performanceTracker;

    @PostMapping("/send")
    public PerfTestResult sendMessages(
            @RequestBody String message,
            @RequestParam(defaultValue = "1000") int count,
            @RequestParam(defaultValue = "60") int timeoutSeconds,
            @RequestParam(defaultValue = "0") int delayMs) throws InterruptedException {

        log.info("Starting performance test: {} messages, timeout {}s, delay {}ms", count, timeoutSeconds, delayMs);

        performanceTracker.startTest(count);

        for (int i = 0; i < count; i++) {
            messageSender.sendMessage(message + "-" + i);
            if (delayMs > 0) {
                Thread.sleep(delayMs);
            }
        }

        log.info("All {} messages sent, waiting for responses...", count);

        boolean completed = performanceTracker.awaitCompletion(timeoutSeconds, TimeUnit.SECONDS);

        PerfTestResult result = performanceTracker.getResult();

        if (completed) {
            log.info("Test completed: {} messages, TPS={}, avgLatency={}ms",
                    result.completedMessages(), String.format("%.2f", result.tps()),
                    String.format("%.2f", result.avgLatencyMs()));
        } else {
            log.warn("Test timed out: {}/{} messages completed", result.completedMessages(), count);
        }

        return result;
    }
}
