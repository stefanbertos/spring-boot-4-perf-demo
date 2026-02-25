CREATE SEQUENCE response_template_seq START 1 INCREMENT 1;

CREATE TABLE response_template (
    id         BIGINT       NOT NULL DEFAULT NEXTVAL('response_template_seq') PRIMARY KEY,
    name       VARCHAR(255) NOT NULL UNIQUE,
    fields     TEXT         NOT NULL DEFAULT '[]',
    created_at TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP    NOT NULL DEFAULT NOW()
);
