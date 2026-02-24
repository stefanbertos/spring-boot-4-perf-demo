CREATE SEQUENCE test_scenario_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE test_scenario (
    id          BIGINT       NOT NULL DEFAULT NEXTVAL('test_scenario_seq') PRIMARY KEY,
    name        VARCHAR(255) NOT NULL UNIQUE,
    entries     TEXT         NOT NULL DEFAULT '[]',
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    scheduled_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    scheduled_time    VARCHAR(5),
    warmup_count INT NOT NULL DEFAULT 0,
    think_time TEXT,
    test_type VARCHAR(20),
    thresholds TEXT,
    count INT NOT NULL DEFAULT 100
);
