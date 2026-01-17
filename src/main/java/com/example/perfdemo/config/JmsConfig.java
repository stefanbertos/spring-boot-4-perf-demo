package com.example.perfdemo.config;

import com.ibm.mq.jms.MQConnectionFactory;
import com.ibm.msg.client.wmq.WMQConstants;
import jakarta.jms.ConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.core.JmsTemplate;

@Configuration
@EnableJms
public class JmsConfig {

    @Value("${ibm.mq.queue-manager}")
    private String queueManager;

    @Value("${ibm.mq.channel}")
    private String channel;

    @Value("${ibm.mq.conn-name}")
    private String connName;

    @Value("${ibm.mq.user}")
    private String user;

    @Value("${ibm.mq.password}")
    private String password;

    @Bean
    public ConnectionFactory connectionFactory() throws Exception {
        MQConnectionFactory factory = new MQConnectionFactory();
        factory.setQueueManager(queueManager);
        factory.setChannel(channel);
        factory.setConnectionNameList(connName);
        factory.setTransportType(WMQConstants.WMQ_CM_CLIENT);
        factory.setStringProperty(WMQConstants.USERID, user);
        factory.setStringProperty(WMQConstants.PASSWORD, password);
        return factory;
    }

    @Bean
    public JmsTemplate jmsTemplate(ConnectionFactory connectionFactory) {
        return new JmsTemplate(connectionFactory);
    }
}
