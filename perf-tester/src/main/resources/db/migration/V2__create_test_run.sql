CREATE SEQUENCE test_run_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE test_run (
    id              BIGINT          NOT NULL DEFAULT NEXTVAL('test_run_seq') PRIMARY KEY,
    test_run_id     VARCHAR(36)     NOT NULL,
    test_id         VARCHAR(255),
    status          VARCHAR(20)     NOT NULL,
    message_count   INTEGER         NOT NULL,
    completed_count BIGINT          NOT NULL DEFAULT 0,
    tps             DOUBLE PRECISION,
    avg_latency_ms  DOUBLE PRECISION,
    min_latency_ms  DOUBLE PRECISION,
    max_latency_ms  DOUBLE PRECISION,
    duration_ms     BIGINT,
    zip_file_path   VARCHAR(1000),
    started_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    completed_at    TIMESTAMP,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    p25_latency_ms DOUBLE PRECISION,
    p75_latency_ms DOUBLE PRECISION,
    p50_latency_ms FLOAT8,
    p90_latency_ms FLOAT8,
    p95_latency_ms FLOAT8,
    p99_latency_ms FLOAT8,
    timeout_count BIGINT NOT NULL DEFAULT 0,
    test_type VARCHAR(20),
    threshold_status VARCHAR(20),
    threshold_results TEXT
);


