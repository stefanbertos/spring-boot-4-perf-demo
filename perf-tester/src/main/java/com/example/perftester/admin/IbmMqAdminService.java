package com.example.perftester.admin;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.regex.Pattern;

import com.example.perftester.config.IbmMqConnectionProperties;
import com.ibm.mq.MQQueueManager;
import com.ibm.mq.constants.CMQC;
import com.ibm.mq.constants.CMQCFC;
import com.ibm.mq.headers.pcf.PCFMessage;
import com.ibm.mq.headers.pcf.PCFMessageAgent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class IbmMqAdminService {

    private static final Pattern CONN_NAME_PATTERN = Pattern.compile("(.+)\\((\\d+)\\)");

    private final String queueManagerName;
    private final String host;
    private final int port;
    private final String channel;
    private final String user;
    private final String password;

    public IbmMqAdminService(IbmMqConnectionProperties mqConnectionProperties) {
        this.queueManagerName = mqConnectionProperties.queueManager();
        this.channel = mqConnectionProperties.channel();
        this.user = mqConnectionProperties.user();
        this.password = mqConnectionProperties.password();

        var connName = mqConnectionProperties.connName();
        var matcher = CONN_NAME_PATTERN.matcher(connName);
        if (matcher.matches()) {
            this.host = matcher.group(1);
            this.port = Integer.parseInt(matcher.group(2));
        } else {
            throw new IllegalArgumentException("Invalid conn-name format: " + connName
                    + ". Expected format: host(port)");
        }
    }

    public List<QueueInfo> listQueues() throws Exception {
        var agent = createAgent();
        try {
            var request = new PCFMessage(CMQCFC.MQCMD_INQUIRE_Q);
            request.addParameter(CMQC.MQCA_Q_NAME, "*");
            request.addParameter(CMQC.MQIA_Q_TYPE, CMQC.MQQT_LOCAL);
            var responses = agent.send(request);
            var queues = new ArrayList<QueueInfo>();
            for (var response : responses) {
                var name = response.getStringParameterValue(CMQC.MQCA_Q_NAME).trim();
                if (name.startsWith("SYSTEM.") || name.startsWith("AMQ.")) {
                    continue;
                }
                var currentDepth = response.getIntParameterValue(CMQC.MQIA_CURRENT_Q_DEPTH);
                var maxDepth = response.getIntParameterValue(CMQC.MQIA_MAX_Q_DEPTH);
                queues.add(new QueueInfo(name, currentDepth, maxDepth));
            }
            queues.sort((a, b) -> a.queueName().compareTo(b.queueName()));
            return queues;
        } finally {
            agent.disconnect();
        }
    }

    public void changeQueueMaxDepth(String queueName, int maxDepth) throws Exception {
        var agent = createAgent();
        try {
            var request = new PCFMessage(CMQCFC.MQCMD_CHANGE_Q);
            request.addParameter(CMQC.MQCA_Q_NAME, queueName);
            request.addParameter(CMQC.MQIA_Q_TYPE, CMQC.MQQT_LOCAL);
            request.addParameter(CMQC.MQIA_MAX_Q_DEPTH, maxDepth);
            agent.send(request);
            log.info("Changed max depth of queue '{}' to {}", queueName, maxDepth);
        } finally {
            agent.disconnect();
        }
    }

    public QueueInfo getQueueInfo(String queueName) throws Exception {
        var agent = createAgent();
        try {
            var request = new PCFMessage(CMQCFC.MQCMD_INQUIRE_Q);
            request.addParameter(CMQC.MQCA_Q_NAME, queueName);
            request.addParameter(CMQC.MQIA_Q_TYPE, CMQC.MQQT_LOCAL);
            var responses = agent.send(request);
            var maxDepth = responses[0].getIntParameterValue(CMQC.MQIA_MAX_Q_DEPTH);
            var currentDepth = responses[0].getIntParameterValue(CMQC.MQIA_CURRENT_Q_DEPTH);
            return new QueueInfo(queueName, currentDepth, maxDepth);
        } finally {
            agent.disconnect();
        }
    }

    @SuppressWarnings("PMD.ReplaceHashtableWithMap")
    private PCFMessageAgent createAgent() throws Exception {
        var properties = new Hashtable<String, Object>();
        properties.put(CMQC.HOST_NAME_PROPERTY, host);
        properties.put(CMQC.PORT_PROPERTY, port);
        properties.put(CMQC.CHANNEL_PROPERTY, channel);
        properties.put(CMQC.USER_ID_PROPERTY, user);
        properties.put(CMQC.PASSWORD_PROPERTY, password);
        var queueManager = new MQQueueManager(queueManagerName, properties);
        return new PCFMessageAgent(queueManager);
    }

    public record QueueInfo(String queueName, int currentDepth, int maxDepth) {
    }
}
