package com.example.perftester.admin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggerConfiguration;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoggingAdminService {

    private final LoggingSystem loggingSystem;

    public void setLogLevel(String loggerName, LogLevel level) {
        loggingSystem.setLogLevel(loggerName, level);
        log.info("Set log level for '{}' to {}", loggerName, level);
    }

    public LoggerConfiguration getLoggerConfiguration(String loggerName) {
        return loggingSystem.getLoggerConfiguration(loggerName);
    }
}
