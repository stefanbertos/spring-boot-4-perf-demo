package com.example.perftester.dashboards;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DashboardsControllerTest {

    private DashboardsController controllerWith(DashboardLink... links) {
        return new DashboardsController(new DashboardsProperties(List.of(links)));
    }

    @Test
    void getDashboardsShouldReturnOnlyEnabledLinks() {
        var controller = controllerWith(
                new DashboardLink("Grafana", "http://grafana:3000", true),
                new DashboardLink("SonarQube", "http://sonar:9000", false)
        );

        var result = controller.getDashboards();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).label()).isEqualTo("Grafana");
        assertThat(result.get(0).url()).isEqualTo("http://grafana:3000");
        assertThat(result.get(0).enabled()).isTrue();
    }

    @Test
    void getDashboardsShouldReturnEmptyWhenAllDisabled() {
        var controller = controllerWith(new DashboardLink("Grafana", "http://grafana:3000", false));

        assertThat(controller.getDashboards()).isEmpty();
    }

    @Test
    void getDashboardsShouldReturnAllWhenAllEnabled() {
        var controller = controllerWith(
                new DashboardLink("Grafana", "http://grafana:3000", true),
                new DashboardLink("Prometheus", "http://prometheus:9090", true)
        );

        assertThat(controller.getDashboards()).hasSize(2);
    }
}
