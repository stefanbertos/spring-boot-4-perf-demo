package com.example.perftester.rest;

import com.example.perftester.messaging.MessageSender;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/perf")
@RequiredArgsConstructor
public class PerfController {

    private final MessageSender messageSender;

    @PostMapping("/send")
    public String sendMessages(
            @RequestBody String message,
            @RequestParam(defaultValue = "1000") int count) {
        for (int i = 0; i < count; i++) {
            messageSender.sendMessage(message + "-" + i);
        }
        return "Sent " + count + " messages";
    }
}
