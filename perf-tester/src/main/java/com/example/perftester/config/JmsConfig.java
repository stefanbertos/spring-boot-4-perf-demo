package com.example.perftester.config;

import jakarta.jms.ConnectionFactory;
import org.messaginghub.pooled.jms.JmsPoolConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.core.JmsTemplate;

@Configuration
public class JmsConfig {

    @Bean
    public JmsPoolConnectionFactory pooledConnectionFactory(
            ConnectionFactory connectionFactory,
            @Value("${app.mq.pool.max-connections:20}") int maxConnections,
            @Value("${app.mq.pool.max-sessions-per-connection:10}") int maxSessionsPerConnection) {
        var pool = new JmsPoolConnectionFactory();
        pool.setConnectionFactory(connectionFactory);
        pool.setMaxConnections(maxConnections);
        pool.setMaxSessionsPerConnection(maxSessionsPerConnection);
        return pool;
    }

    @Bean
    public JmsTemplate jmsTemplate(JmsPoolConnectionFactory pooledConnectionFactory) {
        return new JmsTemplate(pooledConnectionFactory);
    }
}
