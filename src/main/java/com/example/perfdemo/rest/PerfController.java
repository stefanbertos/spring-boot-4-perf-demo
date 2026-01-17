package com.example.perfdemo.rest;

import com.example.perfdemo.messaging.MessageSender;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/perf")
@RequiredArgsConstructor
public class PerfController {

    private final MessageSender messageSender;

    @PostMapping("/send")
    public String sendMessage(@RequestBody String message) {
        messageSender.sendMessage(message);
        return "Message sent";
    }
}
