package com.example.perftester.rest;

import com.example.perftester.admin.LoggingAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.logging.LogLevel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin/logging")
@RequiredArgsConstructor
public class LoggingAdminController {

    private static final String DEFAULT_LOGGER = "com.example";

    private final LoggingAdminService loggingAdminService;

    @PostMapping("/level")
    public ResponseEntity<LogLevelResponse> changeLogLevel(
            @RequestParam(defaultValue = DEFAULT_LOGGER) String loggerName,
            @RequestParam String level) {
        var logLevel = parseLogLevel(level);
        loggingAdminService.setLogLevel(loggerName, logLevel);
        var config = loggingAdminService.getLoggerConfiguration(loggerName);
        return ResponseEntity.ok(LogLevelResponse.from(config));
    }

    @GetMapping("/level")
    public ResponseEntity<LogLevelResponse> getLogLevel(
            @RequestParam(defaultValue = DEFAULT_LOGGER) String loggerName) {
        var config = loggingAdminService.getLoggerConfiguration(loggerName);
        if (config == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Logger not found: " + loggerName);
        }
        return ResponseEntity.ok(LogLevelResponse.from(config));
    }

    private LogLevel parseLogLevel(String level) {
        try {
            return LogLevel.valueOf(level.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid log level: " + level
                            + ". Valid values: TRACE, DEBUG, INFO, WARN, ERROR, OFF", e);
        }
    }

    public record LogLevelResponse(String loggerName, String configuredLevel,
                                   String effectiveLevel) {
        static LogLevelResponse from(
                org.springframework.boot.logging.LoggerConfiguration config) {
            return new LogLevelResponse(
                    config.getName(),
                    config.getConfiguredLevel() != null
                            ? config.getConfiguredLevel().name() : null,
                    config.getEffectiveLevel().name());
        }
    }
}
