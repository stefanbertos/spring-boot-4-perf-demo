package com.example.perftester.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "PERF_MESSAGE")
@Getter
@Setter
@NoArgsConstructor
public class PerfMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "perf_message_seq")
    @SequenceGenerator(name = "perf_message_seq", sequenceName = "PERF_MESSAGE_SEQ", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TEST_RUN_ID", nullable = false)
    private TestRun testRun;

    @Column(name = "MESSAGE_ID", nullable = false, length = 100)
    private String messageId;

    @Column(name = "CORRELATION_ID", length = 100)
    private String correlationId;

    @Column(name = "TRACE_ID", length = 100)
    private String traceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 20)
    private MessageStatus status;

    @Column(name = "PAYLOAD_SIZE")
    private Integer payloadSize;

    @Column(name = "SENT_AT", nullable = false)
    private Instant sentAt;

    @Column(name = "RECEIVED_AT")
    private Instant receivedAt;

    @Column(name = "LATENCY_MS", precision = 10, scale = 3)
    private BigDecimal latencyMs;

    @Column(name = "ERROR_MESSAGE", length = 4000)
    private String errorMessage;

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private Instant createdAt;

    public PerfMessage(TestRun testRun, String messageId, int payloadSize) {
        this.testRun = testRun;
        this.messageId = messageId;
        this.payloadSize = payloadSize;
        this.status = MessageStatus.SENT;
        this.sentAt = Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    public void markReceived(BigDecimal latencyMs) {
        this.status = MessageStatus.RECEIVED;
        this.receivedAt = Instant.now();
        this.latencyMs = latencyMs;
    }

    public void markFailed(String errorMessage) {
        this.status = MessageStatus.FAILED;
        this.errorMessage = errorMessage;
    }

    public void markTimeout() {
        this.status = MessageStatus.TIMEOUT;
    }

    public enum MessageStatus {
        SENT,
        RECEIVED,
        FAILED,
        TIMEOUT
    }
}
