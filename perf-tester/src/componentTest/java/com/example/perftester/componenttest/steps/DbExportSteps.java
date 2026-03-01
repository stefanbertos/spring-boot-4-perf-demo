package com.example.perftester.componenttest.steps;

import com.example.perftester.export.DatabaseExportService;
import com.example.perftester.export.DbExportQuery;
import com.example.perftester.export.DbExportQueryRepository;
import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class DbExportSteps {

    @Autowired
    private DbExportQueryRepository dbExportQueryRepository;

    @Autowired
    private DatabaseExportService databaseExportService;

    private Long queryId;
    private Map<String, Path> exportResults;

    @After("@dbexport")
    public void cleanup() {
        if (queryId != null) {
            dbExportQueryRepository.deleteById(queryId);
        }
        if (exportResults != null) {
            for (var path : exportResults.values()) {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    // best-effort cleanup
                }
            }
        }
    }

    @Given("a database export query named {string} with SQL {string}")
    public void aDatabaseExportQueryNamed(String name, String sql) {
        var entity = new DbExportQuery();
        entity.setName(name);
        entity.setSqlQuery(sql);
        entity.setDisplayOrder(0);
        queryId = dbExportQueryRepository.save(entity).getId();
    }

    @When("the export queries are executed")
    public void theExportQueriesAreExecuted() {
        exportResults = databaseExportService.executeExportQueries();
    }

    @Then("the result for {string} has a CSV file with header {string}")
    public void theResultForHasACsvFileWithHeader(String name, String expectedHeader) throws IOException {
        assertThat(exportResults).containsKey(name);
        var firstLine = Files.lines(exportResults.get(name)).findFirst().orElse("");
        assertThat(firstLine).isEqualTo(expectedHeader);
    }
}
