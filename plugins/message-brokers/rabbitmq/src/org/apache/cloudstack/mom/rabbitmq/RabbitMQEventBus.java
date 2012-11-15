package org.apache.cloudstack.mom.rabbitmq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.apache.cloudstack.framework.events.EventBus;
import org.apache.cloudstack.framework.events.EventCategory;
import org.apache.cloudstack.framework.events.EventSubscriber;
import org.apache.log4j.Logger;

import javax.naming.ConfigurationException;
import java.util.Map;

public class RabbitMQEventBus implements EventBus {


    public static final Logger s_logger = Logger.getLogger(RabbitMQEventBus.class);

    @Override
    public boolean publish(String category, String type, Map<String, String> description) {
        return false;
    }

    @Override
    public boolean subscribe(String category, String type, EventSubscriber subscriber) {
        return false;
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        try {
            String rabbitMqHost = (String) params.get("server");
            Integer port = (Integer) params.get("port");
            String username = (String) params.get("username");
            String password = (String) params.get("password");

            // obtain a connection to RabbitMQ server
            ConnectionFactory factory = new ConnectionFactory();
            factory.setUsername(username);
            factory.setPassword(password);
            factory.setVirtualHost("/");
            factory.setHost(rabbitMqHost);
            factory.setPort(port);
            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();

            // create the exchange for each event category
            for (EventCategory category : EventCategory.listAllEventCategory()) {
                try {
                    channel.exchangeDeclare(category.getName(), "topic", true);
                } catch (java.io.IOException exception) {
                    s_logger.debug("Failed to create exchange on RabbitMQ server for the event category " + category.getName());
                }
            }

        } catch (Exception e) {
            return false;
        }
        return true;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public boolean start() {
        return false;
    }

    @Override
    public boolean stop() {
        return false;
    }
}