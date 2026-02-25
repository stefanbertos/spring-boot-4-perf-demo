CREATE TABLE loki_service_label (
    name VARCHAR(100) NOT NULL PRIMARY KEY
);

INSERT INTO loki_service_label (name) VALUES ('perf-tester');
