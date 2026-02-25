CREATE TABLE health_check_config (
    service               VARCHAR(50)  NOT NULL PRIMARY KEY,
    host                  VARCHAR(255) NOT NULL,
    port                  INT          NOT NULL,
    enabled               BOOLEAN      NOT NULL DEFAULT TRUE,
    connection_timeout_ms INT          NOT NULL DEFAULT 5000,
    interval_ms           INT          NOT NULL DEFAULT 60000
);

INSERT INTO health_check_config (service, host, port, enabled, connection_timeout_ms, interval_ms) VALUES
    ('kafka',  'localhost', 9092, TRUE,  5000, 60000),
    ('ibm-mq', 'localhost', 1414, TRUE,  5000, 60000),
    ('redis',  'localhost', 6379, TRUE,  5000, 60000),
    ('oracle', 'localhost', 1521, FALSE, 5000, 60000);
