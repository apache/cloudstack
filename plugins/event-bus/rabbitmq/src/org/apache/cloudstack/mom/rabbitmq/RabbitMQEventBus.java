package org.apache.cloudstack.mom.rabbitmq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.MessageProperties;
import org.apache.cloudstack.framework.events.Event;
import org.apache.cloudstack.framework.events.EventBus;
import org.apache.cloudstack.framework.events.EventSubscriber;
import org.apache.cloudstack.framework.events.EventTopic;
import org.apache.log4j.Logger;

import javax.ejb.Local;
import javax.naming.ConfigurationException;
import java.util.Map;

@Local(value=EventBus.class)
public class RabbitMQEventBus implements EventBus {


    public static final Logger s_logger = Logger.getLogger(RabbitMQEventBus.class);
    public Connection _connection = null;
    public Channel _channel = null;
    private String _rabbitMqHost;
    private Integer _port;
    private String _username;
    private String _password;

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        _rabbitMqHost = (String) params.get("server");
        _port = Integer.parseInt((String) params.get("port"));
        _username = (String) params.get("username");
        _password = (String) params.get("password");
        return true;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public boolean publish(Event event) {
        String exchangeName = getExchangeName(event.getCategory());
        String routingKey = getRoutingKey(event.getType());
        String eventDescription = event.getDescription();

        try {
            createConnection();
            createExchange(exchangeName);
            publishEventToExchange(exchangeName, routingKey, eventDescription);
        } catch (Exception e) {
            s_logger.error("Failed to publish event to message broker due to " + e.getMessage());
            return false;
        }
        return true;
    }

    @Override
    public boolean subscribe(EventTopic topic, EventSubscriber subscriber) {
        return true;
    }

    @Override
    public boolean unsubscribe(EventTopic topic, EventSubscriber subscriber) {
        return true;
    }

    private String getExchangeName(String eventCategory) {
        return "CloudStack " + eventCategory;
    }

    private String getRoutingKey(String eventType) {
        return eventType;
    }

    private void createConnection() throws Exception {
        try {
            // obtain a connection to RabbitMQ server
            ConnectionFactory factory = new ConnectionFactory();
            factory.setUsername(_username);
            factory.setPassword(_password);
            factory.setVirtualHost("/");
            factory.setHost(_rabbitMqHost);
            factory.setPort(_port);
            _connection = factory.newConnection();
            _channel = _connection.createChannel();
        } catch (Exception e) {
            s_logger.error("Failed to create a connection to RabbitMQ server due to " + e.getMessage());
            throw e;
        }
    }

    private void createExchange(String exchangeName) throws Exception {
        try {
            _channel.exchangeDeclare(exchangeName, "topic", true);
        } catch (java.io.IOException exception) {
            s_logger.error("Failed to create exchange" + exchangeName + " on RabbitMQ server");
            throw exception;
        }
    }

    private void publishEventToExchange(String exchangeName, String routingKey, String eventDescription) throws Exception {
        try {
            _channel.txSelect();
            byte[] messageBodyBytes = eventDescription.getBytes();
            _channel.basicPublish(exchangeName, routingKey, MessageProperties.PERSISTENT_TEXT_PLAIN, messageBodyBytes);
            _channel.txCommit();
        } catch (Exception e) {
            s_logger.error("Failed to publish event " + routingKey + " on exchange " + exchangeName +
                    "  of message broker due to " + e.getMessage());
            throw e;
        }
    }
}