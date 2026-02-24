CREATE SEQUENCE infra_profile_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE infra_profile (
    id                   BIGINT       NOT NULL DEFAULT NEXTVAL('infra_profile_seq') PRIMARY KEY,
    name                 VARCHAR(255) NOT NULL UNIQUE,
    log_levels           TEXT         NOT NULL DEFAULT '{}',
    kafka_topics         TEXT         NOT NULL DEFAULT '{}',
    kubernetes_replicas  TEXT         NOT NULL DEFAULT '{}',
    created_at           TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP    NOT NULL DEFAULT NOW(),
    ibm_mq_queues TEXT NOT NULL DEFAULT '{}'
);
