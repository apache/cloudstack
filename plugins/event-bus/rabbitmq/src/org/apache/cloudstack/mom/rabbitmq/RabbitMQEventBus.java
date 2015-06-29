/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cloudstack.mom.rabbitmq;

import java.io.IOException;
import java.net.ConnectException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.AlreadyClosedException;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.MessageProperties;
import com.rabbitmq.client.ShutdownListener;
import com.rabbitmq.client.ShutdownSignalException;

import org.apache.cloudstack.framework.events.Event;
import org.apache.cloudstack.framework.events.EventBus;
import org.apache.cloudstack.framework.events.EventBusException;
import org.apache.cloudstack.framework.events.EventSubscriber;
import org.apache.cloudstack.framework.events.EventTopic;
import org.apache.cloudstack.managed.context.ManagedContextRunnable;

import com.cloud.utils.Ternary;
import com.cloud.utils.component.ManagerBase;

@Local(value = EventBus.class)
public class RabbitMQEventBus extends ManagerBase implements EventBus {

    // details of AMQP server
    private static String amqpHost;
    private static Integer port;
    private static String username;
    private static String password;
    private static String secureProtocol = "TLSv1";

    public synchronized static void setVirtualHost(String virtualHost) {
        RabbitMQEventBus.virtualHost = virtualHost;
    }

    private static String virtualHost;

    public static void setUseSsl(String useSsl) {
        RabbitMQEventBus.useSsl = useSsl;
    }

    private static String useSsl;

    // AMQP exchange name where all CloudStack events will be published
    private static String amqpExchangeName;

    private String name;

    private static Integer retryInterval;

    // hashmap to book keep the registered subscribers
    private static ConcurrentHashMap<String, Ternary<String, Channel, EventSubscriber>> s_subscribers;

    // connection to AMQP server,
    private static Connection s_connection = null;

    // AMQP server should consider messages acknowledged once delivered if _autoAck is true
    private static boolean s_autoAck = true;

    private ExecutorService executorService;
    private static DisconnectHandler disconnectHandler;
    private static final Logger s_logger = Logger.getLogger(RabbitMQEventBus.class);

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {

        try {
            if (amqpHost == null || amqpHost.isEmpty()) {
                throw new ConfigurationException("Unable to get the AMQP server details");
            }

            if (username == null || username.isEmpty()) {
                throw new ConfigurationException("Unable to get the username details");
            }

            if (password == null || password.isEmpty()) {
                throw new ConfigurationException("Unable to get the password details");
            }

            if (amqpExchangeName == null || amqpExchangeName.isEmpty()) {
                throw new ConfigurationException("Unable to get the _exchange details on the AMQP server");
            }

            if (port == null) {
                throw new ConfigurationException("Unable to get the port details of AMQP server");
            }

            if (useSsl != null && !useSsl.isEmpty()) {
                if (!useSsl.equalsIgnoreCase("true") && !useSsl.equalsIgnoreCase("false")) {
                    throw new ConfigurationException("Invalid configuration parameter for 'ssl'.");
                }
            }

            if (retryInterval == null) {
                retryInterval = 10000;// default to 10s to try out reconnect
            }

        } catch (NumberFormatException e) {
            throw new ConfigurationException("Invalid port number/retry interval");
        }

        s_subscribers = new ConcurrentHashMap<String, Ternary<String, Channel, EventSubscriber>>();
        executorService = Executors.newCachedThreadPool();
        disconnectHandler = new DisconnectHandler();

        return true;
    }

    public static void setServer(String amqpHost) {
        RabbitMQEventBus.amqpHost = amqpHost;
    }

    public static void setUsername(String username) {
        RabbitMQEventBus.username = username;
    }

    public static void setPassword(String password) {
        RabbitMQEventBus.password = password;
    }

    public static void setPort(Integer port) {
        RabbitMQEventBus.port = port;
    }

    public static void setSecureProtocol(String protocol) {
        RabbitMQEventBus.secureProtocol = protocol;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    public static void setExchange(String exchange) {
        RabbitMQEventBus.amqpExchangeName = exchange;
    }

    public static void setRetryInterval(Integer retryInterval) {
        RabbitMQEventBus.retryInterval = retryInterval;
    }

    /** Call to subscribe to interested set of events
     *
     * @param topic defines category and type of the events being subscribed to
     * @param subscriber subscriber that intends to receive event notification
     * @return UUID that represents the subscription with event bus
     * @throws EventBusException
     */
    @Override
    public UUID subscribe(EventTopic topic, EventSubscriber subscriber) throws EventBusException {

        if (subscriber == null || topic == null) {
            throw new EventBusException("Invalid EventSubscriber/EventTopic object passed.");
        }

        // create a UUID, that will be used for managing subscriptions and also used as queue name
        // for on the queue used for the subscriber on the AMQP broker
        UUID queueId = UUID.randomUUID();
        String queueName = queueId.toString();

        try {
            String bindingKey = createBindingKey(topic);

            // store the subscriber details before creating channel
            s_subscribers.put(queueName, new Ternary(bindingKey, null, subscriber));

            // create a channel dedicated for this subscription
            Connection connection = getConnection();
            Channel channel = createChannel(connection);

            // create a queue and bind it to the exchange with binding key formed from event topic
            createExchange(channel, amqpExchangeName);
            channel.queueDeclare(queueName, false, false, false, null);
            channel.queueBind(queueName, amqpExchangeName, bindingKey);

            // register a callback handler to receive the events that a subscriber subscribed to
            channel.basicConsume(queueName, s_autoAck, queueName, new DefaultConsumer(channel) {
                @Override
                public void handleDelivery(String queueName, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                    Ternary<String, Channel, EventSubscriber> queueDetails = s_subscribers.get(queueName);
                    if (queueDetails != null) {
                        EventSubscriber subscriber = queueDetails.third();
                        String routingKey = envelope.getRoutingKey();
                        String eventSource = getEventSourceFromRoutingKey(routingKey);
                        String eventCategory = getEventCategoryFromRoutingKey(routingKey);
                        String eventType = getEventTypeFromRoutingKey(routingKey);
                        String resourceType = getResourceTypeFromRoutingKey(routingKey);
                        String resourceUUID = getResourceUUIDFromRoutingKey(routingKey);
                        Event event = new Event(eventSource, eventCategory, eventType, resourceType, resourceUUID);
                        event.setDescription(new String(body));

                        // deliver the event to call back object provided by subscriber
                        subscriber.onEvent(event);
                    }
                }
            });

            // update the channel details for the subscription
            Ternary<String, Channel, EventSubscriber> queueDetails = s_subscribers.get(queueName);
            queueDetails.second(channel);
            s_subscribers.put(queueName, queueDetails);

        } catch (AlreadyClosedException closedException) {
            s_logger.warn("Connection to AMQP service is lost. Subscription:" + queueName + " will be active after reconnection");
        } catch (ConnectException connectException) {
            s_logger.warn("Connection to AMQP service is lost. Subscription:" + queueName + " will be active after reconnection");
        } catch (Exception e) {
            throw new EventBusException("Failed to subscribe to event due to " + e.getMessage());
        }

        return queueId;
    }

    @Override
    public void unsubscribe(UUID subscriberId, EventSubscriber subscriber) throws EventBusException {
        try {
            String classname = subscriber.getClass().getName();
            String queueName = UUID.nameUUIDFromBytes(classname.getBytes()).toString();
            Ternary<String, Channel, EventSubscriber> queueDetails = s_subscribers.get(queueName);
            Channel channel = queueDetails.second();
            channel.basicCancel(queueName);
            s_subscribers.remove(queueName, queueDetails);
        } catch (Exception e) {
            throw new EventBusException("Failed to unsubscribe from event bus due to " + e.getMessage());
        }
    }

    // publish event on to the exchange created on AMQP server
    @Override
    public void publish(Event event) throws EventBusException {

        String routingKey = createRoutingKey(event);
        String eventDescription = event.getDescription();

        try {
            Connection connection = getConnection();
            Channel channel = createChannel(connection);
            createExchange(channel, amqpExchangeName);
            publishEventToExchange(channel, amqpExchangeName, routingKey, eventDescription);
            channel.close();
        } catch (AlreadyClosedException e) {
            closeConnection();
            throw new EventBusException("Failed to publish event to message broker as connection to AMQP broker in lost");
        } catch (Exception e) {
            throw new EventBusException("Failed to publish event to message broker due to " + e.getMessage());
        }
    }

    /** creates a routing key from the event details.
     *  created routing key will be used while publishing the message to exchange on AMQP server
     */
    private String createRoutingKey(Event event) {

        StringBuilder routingKey = new StringBuilder();

        String eventSource = replaceNullWithWildcard(event.getEventSource());
        eventSource = eventSource.replace(".", "-");

        String eventCategory = replaceNullWithWildcard(event.getEventCategory());
        eventCategory = eventCategory.replace(".", "-");

        String eventType = replaceNullWithWildcard(event.getEventType());
        eventType = eventType.replace(".", "-");

        String resourceType = replaceNullWithWildcard(event.getResourceType());
        resourceType = resourceType.replace(".", "-");

        String resourceUuid = replaceNullWithWildcard(event.getResourceUUID());
        resourceUuid = resourceUuid.replace(".", "-");

        // routing key will be of format: eventSource.eventCategory.eventType.resourceType.resourceUuid
        routingKey.append(eventSource);
        routingKey.append(".");
        routingKey.append(eventCategory);
        routingKey.append(".");
        routingKey.append(eventType);
        routingKey.append(".");
        routingKey.append(resourceType);
        routingKey.append(".");
        routingKey.append(resourceUuid);

        return routingKey.toString();
    }

    /** creates a binding key from the event topic that subscriber specified
     *  binding key will be used to bind the queue created for subscriber to exchange on AMQP server
     */
    private String createBindingKey(EventTopic topic) {

        StringBuilder bindingKey = new StringBuilder();

        String eventSource = replaceNullWithWildcard(topic.getEventSource());
        eventSource = eventSource.replace(".", "-");

        String eventCategory = replaceNullWithWildcard(topic.getEventCategory());
        eventCategory = eventCategory.replace(".", "-");

        String eventType = replaceNullWithWildcard(topic.getEventType());
        eventType = eventType.replace(".", "-");

        String resourceType = replaceNullWithWildcard(topic.getResourceType());
        resourceType = resourceType.replace(".", "-");

        String resourceUuid = replaceNullWithWildcard(topic.getResourceUUID());
        resourceUuid = resourceUuid.replace(".", "-");

        // binding key will be of format: eventSource.eventCategory.eventType.resourceType.resourceUuid
        bindingKey.append(eventSource);
        bindingKey.append(".");
        bindingKey.append(eventCategory);
        bindingKey.append(".");
        bindingKey.append(eventType);
        bindingKey.append(".");
        bindingKey.append(resourceType);
        bindingKey.append(".");
        bindingKey.append(resourceUuid);

        return bindingKey.toString();
    }

    private synchronized Connection getConnection() throws Exception {
        if (s_connection == null) {
            try {
                return createConnection();
            } catch (Exception e) {
                s_logger.error("Failed to create a connection to AMQP server due to " + e.getMessage());
                throw e;
            }
        } else {
            return s_connection;
        }
    }

    private synchronized Connection createConnection() throws Exception {
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setUsername(username);
            factory.setPassword(password);
            factory.setHost(amqpHost);
            factory.setPort(port);

            if (virtualHost != null && !virtualHost.isEmpty()) {
                factory.setVirtualHost(virtualHost);
            } else {
                factory.setVirtualHost("/");
            }

            if (useSsl != null && !useSsl.isEmpty() && useSsl.equalsIgnoreCase("true")) {
                factory.useSslProtocol(secureProtocol);
            }
            Connection connection = factory.newConnection();
            connection.addShutdownListener(disconnectHandler);
            s_connection = connection;
            return s_connection;
        } catch (Exception e) {
            throw e;
        }
    }

    private synchronized void closeConnection() {
        try {
            if (s_connection != null) {
                s_connection.close();
            }
        } catch (Exception e) {
            s_logger.warn("Failed to close connection to AMQP server due to " + e.getMessage());
        }
        s_connection = null;
    }

    private synchronized void abortConnection() {
        if (s_connection == null)
            return;

        try {
            s_connection.abort();
        } catch (Exception e) {
            s_logger.warn("Failed to abort connection due to " + e.getMessage());
        }
        s_connection = null;
    }

    private String replaceNullWithWildcard(String key) {
        if (key == null || key.isEmpty()) {
            return "*";
        } else {
            return key;
        }
    }

    private Channel createChannel(Connection connection) throws Exception {
        try {
            return connection.createChannel();
        } catch (java.io.IOException exception) {
            s_logger.warn("Failed to create a channel due to " + exception.getMessage());
            throw exception;
        }
    }

    private void createExchange(Channel channel, String exchangeName) throws Exception {
        try {
            channel.exchangeDeclare(exchangeName, "topic", true);
        } catch (java.io.IOException exception) {
            s_logger.error("Failed to create exchange" + exchangeName + " on RabbitMQ server");
            throw exception;
        }
    }

    private void publishEventToExchange(Channel channel, String exchangeName, String routingKey, String eventDescription) throws Exception {
        try {
            byte[] messageBodyBytes = eventDescription.getBytes();
            channel.basicPublish(exchangeName, routingKey, MessageProperties.PERSISTENT_TEXT_PLAIN, messageBodyBytes);
        } catch (Exception e) {
            s_logger.error("Failed to publish event " + routingKey + " on exchange " + exchangeName + "  of message broker due to " + e.getMessage());
            throw e;
        }
    }

    private String getEventCategoryFromRoutingKey(String routingKey) {
        String[] keyParts = routingKey.split("\\.");
        return keyParts[1];
    }

    private String getEventTypeFromRoutingKey(String routingKey) {
        String[] keyParts = routingKey.split("\\.");
        return keyParts[2];
    }

    private String getEventSourceFromRoutingKey(String routingKey) {
        String[] keyParts = routingKey.split("\\.");
        return keyParts[0];
    }

    private String getResourceTypeFromRoutingKey(String routingKey) {
        String[] keyParts = routingKey.split("\\.");
        return keyParts[3];
    }

    private String getResourceUUIDFromRoutingKey(String routingKey) {
        String[] keyParts = routingKey.split("\\.");
        return keyParts[4];
    }

    @Override
    public String getName() {
        return _name;
    }

    @Override
    public boolean start() {
        ReconnectionTask reconnect = new ReconnectionTask(); // initiate connection to AMQP server
        executorService.submit(reconnect);
        return true;
    }

    @Override
    public synchronized boolean stop() {
        if (s_connection.isOpen()) {
            for (String subscriberId : s_subscribers.keySet()) {
                Ternary<String, Channel, EventSubscriber> subscriberDetails = s_subscribers.get(subscriberId);
                Channel channel = subscriberDetails.second();
                String queueName = subscriberId;
                try {
                    channel.queueDelete(queueName);
                    channel.abort();
                } catch (IOException ioe) {
                    s_logger.warn("Failed to delete queue: " + queueName + " on AMQP server due to " + ioe.getMessage());
                }
            }
        }

        closeConnection();
        return true;
    }

    // logic to deal with loss of connection to AMQP server
    private class DisconnectHandler implements ShutdownListener {

        @Override
        public void shutdownCompleted(ShutdownSignalException shutdownSignalException) {
            if (!shutdownSignalException.isInitiatedByApplication()) {

                for (String subscriberId : s_subscribers.keySet()) {
                    Ternary<String, Channel, EventSubscriber> subscriberDetails = s_subscribers.get(subscriberId);
                    subscriberDetails.second(null);
                    s_subscribers.put(subscriberId, subscriberDetails);
                }

                abortConnection(); // disconnected to AMQP server, so abort the connection and channels
                s_logger.warn("Connection has been shutdown by AMQP server. Attempting to reconnect.");

                // initiate re-connect process
                ReconnectionTask reconnect = new ReconnectionTask();
                executorService.submit(reconnect);
            }
        }
    }

    // retry logic to connect back to AMQP server after loss of connection
    private class ReconnectionTask extends ManagedContextRunnable {

        boolean connected = false;
        Connection connection = null;

        @Override
        protected void runInContext() {

            while (!connected) {
                try {
                    Thread.sleep(retryInterval);
                } catch (InterruptedException ie) {
                    // ignore timer interrupts
                }

                try {
                    try {
                        connection = createConnection();
                        connected = true;
                    } catch (IOException ie) {
                        continue; // can't establish connection to AMQP server yet, so continue
                    }

                    // prepare consumer on AMQP server for each of subscriber
                    for (String subscriberId : s_subscribers.keySet()) {
                        Ternary<String, Channel, EventSubscriber> subscriberDetails = s_subscribers.get(subscriberId);
                        String bindingKey = subscriberDetails.first();
                        EventSubscriber subscriber = subscriberDetails.third();

                        /** create a queue with subscriber ID as queue name and bind it to the exchange
                         *  with binding key formed from event topic
                         */
                        Channel channel = createChannel(connection);
                        createExchange(channel, amqpExchangeName);
                        channel.queueDeclare(subscriberId, false, false, false, null);
                        channel.queueBind(subscriberId, amqpExchangeName, bindingKey);

                        // register a callback handler to receive the events that a subscriber subscribed to
                        channel.basicConsume(subscriberId, s_autoAck, subscriberId, new DefaultConsumer(channel) {
                            @Override
                            public void handleDelivery(String queueName, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {

                                Ternary<String, Channel, EventSubscriber> subscriberDetails = s_subscribers.get(queueName); // queue name == subscriber ID

                                if (subscriberDetails != null) {
                                    EventSubscriber subscriber = subscriberDetails.third();
                                    String routingKey = envelope.getRoutingKey();
                                    String eventSource = getEventSourceFromRoutingKey(routingKey);
                                    String eventCategory = getEventCategoryFromRoutingKey(routingKey);
                                    String eventType = getEventTypeFromRoutingKey(routingKey);
                                    String resourceType = getResourceTypeFromRoutingKey(routingKey);
                                    String resourceUUID = getResourceUUIDFromRoutingKey(routingKey);

                                    // create event object from the message details obtained from AMQP server
                                    Event event = new Event(eventSource, eventCategory, eventType, resourceType, resourceUUID);
                                    event.setDescription(new String(body));

                                    // deliver the event to call back object provided by subscriber
                                    subscriber.onEvent(event);
                                }
                            }
                        });

                        // update the channel details for the subscription
                        subscriberDetails.second(channel);
                        s_subscribers.put(subscriberId, subscriberDetails);
                    }
                } catch (Exception e) {
                    s_logger.warn("Failed to recreate queues and binding for the subscribers due to " + e.getMessage());
                }
            }
            return;
        }
    }
}