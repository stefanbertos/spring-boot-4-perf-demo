package com.example.perftester.rest;

import java.util.List;

import com.example.perftester.admin.KafkaAdminService;
import com.example.perftester.admin.KafkaAdminService.TopicInfo;
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
@RequestMapping("/api/admin/kafka")
@RequiredArgsConstructor
public class KafkaAdminController {

    private final KafkaAdminService kafkaAdminService;

    @PostMapping("/topics/resize")
    public ResponseEntity<TopicInfo> resizeTopic(
            @RequestParam @NotBlank String topicName,
            @RequestParam @Min(1) int partitions) throws Exception {
        kafkaAdminService.resizeTopic(topicName, partitions);
        var info = kafkaAdminService.getTopicInfo(topicName);
        return ResponseEntity.ok(info);
    }

    @GetMapping("/topics/list")
    public ResponseEntity<List<TopicInfo>> listTopics() throws Exception {
        var topics = kafkaAdminService.listTopics();
        return ResponseEntity.ok(topics);
    }

    @GetMapping("/topics")
    public ResponseEntity<TopicInfo> getTopicInfo(@RequestParam @NotBlank String topicName) throws Exception {
        var info = kafkaAdminService.getTopicInfo(topicName);
        return ResponseEntity.ok(info);
    }
}
