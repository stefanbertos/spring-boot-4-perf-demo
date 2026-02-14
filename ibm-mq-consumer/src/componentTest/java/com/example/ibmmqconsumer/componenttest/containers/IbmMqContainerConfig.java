package com.example.ibmmqconsumer.componenttest.containers;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration
public class IbmMqContainerConfig {

    @Bean
    public GenericContainer<?> ibmMqContainer() {
        var container = new GenericContainer<>(DockerImageName.parse("icr.io/ibm-messaging/mq:9.4.1.0-r1"))
                .withExposedPorts(1414, 9443)
                .withEnv("LICENSE", "accept")
                .withEnv("MQ_QMGR_NAME", "QM1")
                .withEnv("MQ_APP_PASSWORD", "passw0rd")
                .withEnv("MQ_ADMIN_PASSWORD", "passw0rd")
                .waitingFor(Wait.forListeningPort());
        container.start();
        return container;
    }

    @Bean
    public DynamicPropertyRegistrar ibmMqPropertyRegistrar(GenericContainer<?> ibmMqContainer) {
        return registry -> {
            var host = ibmMqContainer.getHost();
            var port = ibmMqContainer.getMappedPort(1414);
            registry.add("ibm.mq.conn-name", () -> host + "(" + port + ")");
            registry.add("ibm.mq.queue-manager", () -> "QM1");
            registry.add("ibm.mq.channel", () -> "DEV.ADMIN.SVRCONN");
            registry.add("ibm.mq.user", () -> "admin");
            registry.add("ibm.mq.password", () -> "passw0rd");
        };
    }
}
