Feature: Database export query execution
  Configured SELECT queries are executed against the live database
  and results are written as CSV files with correct headers and data.

  @dbexport
  Scenario: Configured SELECT query produces a CSV with the correct header
    Given a database export query named "ct-row-count" with SQL "SELECT 1 AS row_num"
    When the export queries are executed
    Then the result for "ct-row-count" has a CSV file with header "row_num"
