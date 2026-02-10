package com.example.perftester.rest;

import com.example.perftester.admin.IbmMqAdminService;
import com.example.perftester.admin.IbmMqAdminService.QueueInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RestController
@RequestMapping("/api/admin/mq")
@RequiredArgsConstructor
public class IbmMqAdminController {

    private final IbmMqAdminService ibmMqAdminService;

    @PostMapping("/queues/depth")
    public ResponseEntity<QueueInfo> changeQueueMaxDepth(
            @RequestParam String queueName,
            @RequestParam int maxDepth) {
        try {
            ibmMqAdminService.changeQueueMaxDepth(queueName, maxDepth);
            var info = ibmMqAdminService.getQueueInfo(queueName);
            return ResponseEntity.ok(info);
        } catch (Exception e) {
            log.error("Failed to set max depth for queue '{}': {}",
                    queueName, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to set queue max depth: " + e.getMessage(), e);
        }
    }

    @GetMapping("/queues")
    public ResponseEntity<QueueInfo> getQueueInfo(@RequestParam String queueName) {
        try {
            var info = ibmMqAdminService.getQueueInfo(queueName);
            return ResponseEntity.ok(info);
        } catch (Exception e) {
            log.error("Failed to get queue info for '{}': {}", queueName, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to get queue info: " + e.getMessage(), e);
        }
    }
}
