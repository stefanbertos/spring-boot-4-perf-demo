CREATE SEQUENCE test_run_snapshot_seq START WITH 1 INCREMENT BY 1;
CREATE TABLE test_run_snapshot (
    id              BIGINT    NOT NULL DEFAULT nextval('test_run_snapshot_seq') PRIMARY KEY,
    test_run_id     BIGINT    NOT NULL REFERENCES test_run(id) ON DELETE CASCADE,
    sampled_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    outbound_queue_depth INT,
    inbound_queue_depth  INT,
    kafka_requests_lag   BIGINT,
    kafka_responses_lag  BIGINT
);
