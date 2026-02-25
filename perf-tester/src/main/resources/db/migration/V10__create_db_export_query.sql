CREATE SEQUENCE db_export_query_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE db_export_query (
    id            BIGINT       NOT NULL DEFAULT nextval('db_export_query_seq') PRIMARY KEY,
    name          VARCHAR(255) NOT NULL UNIQUE,
    sql_query     TEXT         NOT NULL,
    display_order INT          NOT NULL DEFAULT 0
);
