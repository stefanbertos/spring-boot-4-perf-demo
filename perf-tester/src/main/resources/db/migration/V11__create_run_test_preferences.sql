CREATE TABLE run_test_preferences (
    id                  BIGINT  NOT NULL DEFAULT 1 PRIMARY KEY,
    export_grafana      BOOLEAN NOT NULL DEFAULT FALSE,
    export_prometheus   BOOLEAN NOT NULL DEFAULT FALSE,
    export_kubernetes   BOOLEAN NOT NULL DEFAULT FALSE,
    export_logs         BOOLEAN NOT NULL DEFAULT FALSE,
    export_database     BOOLEAN NOT NULL DEFAULT FALSE,
    debug               BOOLEAN NOT NULL DEFAULT FALSE
);

INSERT INTO run_test_preferences (id) VALUES (1);
