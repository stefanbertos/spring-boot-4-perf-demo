-- Add template FKs to test_case
ALTER TABLE test_case
    ADD COLUMN header_template_id  BIGINT REFERENCES header_template(id)  ON DELETE SET NULL,
    ADD COLUMN response_template_id BIGINT REFERENCES response_template(id) ON DELETE SET NULL;

-- Bridge table: one row per test-case entry in a scenario
CREATE SEQUENCE scenario_test_case_seq START 1 INCREMENT 1;
CREATE TABLE scenario_test_case (
    id              BIGINT  NOT NULL DEFAULT nextval('scenario_test_case_seq') PRIMARY KEY,
    scenario_id     BIGINT  NOT NULL REFERENCES test_scenario(id) ON DELETE CASCADE,
    test_case_id    BIGINT  NOT NULL REFERENCES test_case(id),
    percentage      INT     NOT NULL DEFAULT 0,
    display_order   INT     NOT NULL DEFAULT 0
);

-- Drop old JSON blob column
ALTER TABLE test_scenario DROP COLUMN entries;
