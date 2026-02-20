CREATE SEQUENCE test_case_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE test_case (
    id          BIGINT       NOT NULL DEFAULT NEXTVAL('test_case_seq') PRIMARY KEY,
    name        VARCHAR(255) NOT NULL UNIQUE,
    message     TEXT         NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);
