package com.example.perftester.rest;

import com.example.perftester.admin.IbmMqAdminService;
import com.example.perftester.admin.IbmMqAdminService.QueueInfo;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/admin/mq")
@RequiredArgsConstructor
public class IbmMqAdminController {

    private final IbmMqAdminService ibmMqAdminService;

    @PostMapping("/queues/depth")
    public ResponseEntity<QueueInfo> changeQueueMaxDepth(
            @RequestParam @NotBlank String queueName,
            @RequestParam @Min(1) int maxDepth) throws Exception {
        ibmMqAdminService.changeQueueMaxDepth(queueName, maxDepth);
        var info = ibmMqAdminService.getQueueInfo(queueName);
        return ResponseEntity.ok(info);
    }

    @GetMapping("/queues")
    public ResponseEntity<QueueInfo> getQueueInfo(@RequestParam @NotBlank String queueName) throws Exception {
        var info = ibmMqAdminService.getQueueInfo(queueName);
        return ResponseEntity.ok(info);
    }
}
