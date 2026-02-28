package com.example.perftester.rest;

import java.util.List;

import com.example.perftester.admin.IbmMqAdminService;
import com.example.perftester.admin.IbmMqAdminService.QueueInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "Admin: IBM MQ", description = "Inspect and configure IBM MQ queues")
@Slf4j
@Validated
@RestController
@RequestMapping("/api/admin/mq")
@RequiredArgsConstructor
public class IbmMqAdminController {

    private final IbmMqAdminService ibmMqAdminService;

    @Operation(summary = "Change MQ queue maximum depth")
    @PostMapping("/queues/depth")
    public ResponseEntity<QueueInfo> changeQueueMaxDepth(
            @RequestParam @NotBlank String queueName,
            @RequestParam @Min(1) int maxDepth) throws Exception {
        ibmMqAdminService.changeQueueMaxDepth(queueName, maxDepth);
        var info = ibmMqAdminService.getQueueInfo(queueName);
        return ResponseEntity.ok(info);
    }

    @Operation(summary = "List all MQ queues")
    @GetMapping("/queues/list")
    public ResponseEntity<List<QueueInfo>> listQueues() throws Exception {
        var queues = ibmMqAdminService.listQueues();
        return ResponseEntity.ok(queues);
    }

    @Operation(summary = "Get an MQ queue by name")
    @GetMapping("/queues")
    public ResponseEntity<QueueInfo> getQueueInfo(@RequestParam @NotBlank String queueName) throws Exception {
        var info = ibmMqAdminService.getQueueInfo(queueName);
        return ResponseEntity.ok(info);
    }
}
