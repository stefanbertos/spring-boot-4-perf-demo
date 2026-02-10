package com.example.perftester.rest;

import com.example.perftester.admin.KafkaAdminService;
import com.example.perftester.admin.KafkaAdminService.TopicInfo;
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
@RequestMapping("/api/admin/kafka")
@RequiredArgsConstructor
public class KafkaAdminController {

    private final KafkaAdminService kafkaAdminService;

    @PostMapping("/topics/resize")
    public ResponseEntity<TopicInfo> resizeTopic(
            @RequestParam String topicName,
            @RequestParam int partitions) {
        try {
            kafkaAdminService.resizeTopic(topicName, partitions);
            var info = kafkaAdminService.getTopicInfo(topicName);
            return ResponseEntity.ok(info);
        } catch (Exception e) {
            log.error("Failed to resize topic '{}': {}", topicName, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to resize topic: " + e.getMessage(), e);
        }
    }

    @GetMapping("/topics")
    public ResponseEntity<TopicInfo> getTopicInfo(@RequestParam String topicName) {
        try {
            var info = kafkaAdminService.getTopicInfo(topicName);
            return ResponseEntity.ok(info);
        } catch (Exception e) {
            log.error("Failed to get topic info for '{}': {}", topicName, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to get topic info: " + e.getMessage(), e);
        }
    }
}
