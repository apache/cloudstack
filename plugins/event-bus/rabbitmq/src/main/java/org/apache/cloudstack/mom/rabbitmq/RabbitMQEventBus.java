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
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.naming.ConfigurationException;

import com.cloud.utils.UuidUtils;
import com.cloud.utils.exception.CloudRuntimeException;
import com.rabbitmq.client.BlockedListener;

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

public class RabbitMQEventBus extends ManagerBase implements EventBus {

    // details of AMQP server
    private static String amqpHost;
    private static Integer port;
    private static String username;
    private static String password;
    private static String secureProtocol = "TLSv1.2";

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

    // max number of events kept in memory while the AMQP server is unreachable, 0 disables buffering
    private static Integer maxEventBufferSize = 1000;

    // time in milliseconds after which a buffered event is dropped if it could not be sent
    private static Integer eventBufferExpiryTime = 300000;

    /**
     * heartbeat timeout, in seconds, requested from the AMQP server, which may lower it; 0 takes the value of the server.
     * The client notices a connection that silently stopped working (e.g. packets dropped by a firewall) only after about
     * two heartbeat timeouts without any data from the server. Until then events are still sent on the calling thread and
     * wait for replies that never come, so with the client default of 60 seconds API calls could hang for about 2 minutes.
     * RabbitMQ recommends values between 5 and 20 seconds.
     */
    private static Integer heartbeat = 10;

    /**
     * events, stored as {routing key, description, time of buffering}, waiting to be sent, oldest first
     */
    private static final ArrayDeque<Ternary<String, String, Long>> eventBuffer = new ArrayDeque<>();

    /**
     * indicates whether a connection has ever been made, as events are buffered only after the AMQP server was reachable at least once
     */
    private static volatile boolean hasBeenConnectedOnce = false;

    // hashmap to book keep the registered subscribers
    private static ConcurrentHashMap<String, Ternary<String, Channel, EventSubscriber>> subscribers;

    // connection to AMQP server,
    private static volatile Connection connection = null;

    // AMQP server should consider messages acknowledged once delivered if autoAck is true
    private static boolean autoAck = true;

    private ScheduledExecutorService executorService;
    private static DisconnectHandler disconnectHandler;
    private static BlockedConnectionHandler blockedConnectionHandler;

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

            if (retryInterval == null || retryInterval <= 0) {
                retryInterval = 10000;// default to 10s to try out reconnect
            }

        } catch (NumberFormatException e) {
            throw new ConfigurationException("Invalid port number/retry interval");
        }

        subscribers = new ConcurrentHashMap<String, Ternary<String, Channel, EventSubscriber>>();
        executorService = Executors.newSingleThreadScheduledExecutor();
        disconnectHandler = new DisconnectHandler();
        blockedConnectionHandler = new BlockedConnectionHandler();

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

    /**
     * time, in milliseconds, between two runs of the ReconnectionTask, set by the bean property "retryInterval".
     * Default 10000 (10 seconds), also used for values of 0 or less. Each run reconnects if needed, drops the expired
     * events and sends the buffered ones.
     */
    public static void setRetryInterval(Integer retryInterval) {
        RabbitMQEventBus.retryInterval = retryInterval;
    }

    /**
     * maximum size to which the event buffer may grow, set by the bean property "bufferSize".
     * Default 1000; when the buffer is full, the oldest event is dropped. 0 disables buffering: events that cannot be
     * sent are dropped and publish() connects on the calling thread, as before buffering was added.
     */
    public static void setBufferSize(Integer bufferSize) {
        maxEventBufferSize = bufferSize;
    }

    /**
     * expiry time, in milliseconds, to set for events that cannot be sent, set by the bean property "bufferExpiry".
     * Default 300000 (5 minutes). Expired events are dropped by the next run of the ReconnectionTask, also while the
     * AMQP server is unreachable, so an event is kept for at most bufferExpiry plus retryInterval.
     */
    public static void setBufferExpiry(Integer bufferExpiry) {
        eventBufferExpiryTime = bufferExpiry;
    }

    /**
     * heartbeat timeout, in seconds, to request from the AMQP server, set by the bean property "heartbeat".
     * Default 10; 0 takes the value of the server. A lower value notices a connection that silently stopped working
     * sooner, see the field heartbeat for the reasoning.
     */
    public static void setHeartbeat(Integer heartbeat) {
        RabbitMQEventBus.heartbeat = heartbeat;
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

        logger.debug("subscribing '{}' to events of type '{}' from '{}'", subscriber.toString(), topic.getEventType(), topic.getEventSource());

        // create a UUID, that will be used for managing subscriptions and also used as queue name
        // for on the queue used for the subscriber on the AMQP broker
        UUID queueId = UUID.randomUUID();
        String queueName = queueId.toString();

        try {
            String bindingKey = createBindingKey(topic);

            // store the subscriber details before creating channel
            subscribers.put(queueName, new Ternary(bindingKey, null, subscriber));

            registerSubscriber(getConnection(), queueName);
        } catch (AlreadyClosedException closedException) {
            logger.warn("Connection to AMQP service is lost. Subscription:" + queueName + " will be active after reconnection", closedException);
        } catch (ConnectException connectException) {
            logger.warn("Connection to AMQP service is lost. Subscription:" + queueName + " will be active after reconnection", connectException);
        } catch (Exception e) {
            throw new EventBusException("Failed to subscribe to event due to " + e.getMessage());
        }

        return queueId;
    }

    /**
     * creates a queue for the subscriber, binds it to the exchange and registers a consumer that delivers the events to the subscriber
     */
    private void registerSubscriber(Connection connection, String queueName) throws Exception {
        Ternary<String, Channel, EventSubscriber> subscriberDetails = subscribers.get(queueName);

        // create a channel dedicated for this subscription
        Channel channel = createChannel(connection);

        // create a queue and bind it to the exchange with binding key formed from event topic
        createExchange(channel, amqpExchangeName);
        channel.queueDeclare(queueName, false, false, false, null);
        channel.queueBind(queueName, amqpExchangeName, subscriberDetails.first());

        // register a callback handler to receive the events that a subscriber subscribed to
        channel.basicConsume(queueName, autoAck, queueName, new SubscriberConsumer(channel));

        // update the channel details for the subscription
        subscriberDetails.second(channel);
        subscribers.put(queueName, subscriberDetails);
    }

    /**
     * registers the subscribers that have no open channel, i.e. all of them after a loss of connection
     */
    private void registerDisconnectedSubscribers(Connection connection) {
        for (Map.Entry<String, Ternary<String, Channel, EventSubscriber>> entry : subscribers.entrySet()) {
            Channel channel = entry.getValue().second();
            if (channel != null && channel.isOpen()) {
                continue;
            }
            try {
                registerSubscriber(connection, entry.getKey());
            } catch (Exception e) {
                logger.warn("Failed to recreate queues and binding for the subscribers due to " + e.getMessage());
            }
        }
    }

    @Override
    public void unsubscribe(UUID subscriberId, EventSubscriber subscriber) throws EventBusException {
        logger.debug("unsubscribing '{}'", subscriberId);
        try {
            String classname = subscriber.getClass().getName();
            String queueName = UuidUtils.nameUUIDFromBytes(classname.getBytes()).toString();
            Ternary<String, Channel, EventSubscriber> queueDetails = subscribers.get(queueName);
            Channel channel = queueDetails.second();
            channel.basicCancel(queueName);
            subscribers.remove(queueName, queueDetails);
        } catch (Exception e) {
            throw new EventBusException("Failed to unsubscribe from event bus due to " + e.getMessage());
        }
    }

    // publish event on to the exchange created on AMQP server
    @Override
    public void publish(Event event) throws EventBusException {
        logger.trace("publish '{}'", event.getDescription());

        String routingKey = createRoutingKey(event);
        String eventDescription = event.getDescription();

        // keep the order: nothing is sent directly while older events are waiting or the connection is down
        if (bufferEvent(routingKey, eventDescription, false)) {
            return;
        }

        try {
            sendEvent(getConnection(), routingKey, eventDescription);
        } catch (AlreadyClosedException e) {
            closeConnection();
            if (bufferEvent(routingKey, eventDescription, true)) {
                return;
            }
            throw new EventBusException("Failed to publish event to message broker as connection to AMQP broker in lost");
        } catch (IOException e) {
            if (bufferEvent(routingKey, eventDescription, true)) {
                return;
            }
            throw new EventBusException("Failed to publish event to message broker due to " + e.getMessage());
        } catch (Exception e) {
            throw new EventBusException("Failed to publish event to message broker due to " + e.getMessage());
        }
    }

    /**
     * sends the event on a channel of its own. Once basicPublish() has returned, the event counts as sent even if closing the
     * channel fails: publishing gets no reply, so the event may already have reached the AMQP server and sending it again could
     * duplicate it. Losing it instead is the same as losing an event still in the TCP send buffer when the connection breaks.
     * This also covers events the AMQP server refused, e.g. due to missing write permissions on the exchange: the refusal is
     * reported only when the channel is closed, and sending such an event again would fail the same way.
     */
    private void sendEvent(Connection connection, String routingKey, String eventDescription) throws Exception {
        Channel channel = createChannel(connection);
        createExchange(channel, amqpExchangeName);
        publishEventToExchange(channel, amqpExchangeName, routingKey, eventDescription);
        try {
            channel.close();
        } catch (Exception e) {
            logger.warn("Failed to close the channel after publishing event " + routingKey + ", the event might not have been delivered: " + getErrorMessage(e));
        }
    }

    /** adds the event to the buffer, dropping the oldest one if the buffer is full
     *
     * @param sendFailed false if the event was not tried yet and has to be buffered only to keep the order
     * @return true if the event was buffered, false if the caller has to deal with it
     */
    private boolean bufferEvent(String routingKey, String eventDescription, boolean sendFailed) {
        if (!hasBeenConnectedOnce || maxEventBufferSize <= 0) {
            return false;
        }
        synchronized (eventBuffer) {
            if (!sendFailed && eventBuffer.isEmpty() && connection != null) {
                return false;
            }
            if (eventBuffer.isEmpty()) {
                logger.warn("Unable to send events to AMQP server, buffering them until they can be sent");
            }
            if (eventBuffer.size() >= maxEventBufferSize) {
                logger.warn("Event buffer is full, dropping the oldest event " + eventBuffer.poll().first());
            }
            eventBuffer.add(new Ternary<>(routingKey, eventDescription, System.currentTimeMillis()));
        }
        return true;
    }

    /**
     * drops the expired events and, if connected, sends the buffered ones in the order they were added.
     * Must only be called by the ReconnectionTask: with more than one thread events could be sent twice or out of order.
     */
    private void flushBuffer() {
        int sent = 0;
        Ternary<String, String, Long> bufferedEvent;
        while ((bufferedEvent = peekBuffer()) != null) {
            boolean expired = System.currentTimeMillis() - bufferedEvent.third() > eventBufferExpiryTime;
            Connection currentConnection = connection;
            if (!expired && currentConnection == null) {
                break;
            }

            if (expired) {
                logger.warn("Dropping expired event " + bufferedEvent.first() + " as it could not be sent to AMQP server in time");
            } else {
                try {
                    sendEvent(currentConnection, bufferedEvent.first(), bufferedEvent.second());
                    sent++;
                } catch (AlreadyClosedException e) {
                    closeConnection();
                    break;
                } catch (IOException e) {
                    break; // retry with the next run
                } catch (Exception e) {
                    logger.warn("Dropping buffered event " + bufferedEvent.first() + " due to " + e.getMessage());
                }
            }

            // the event stays in the buffer while being sent, so that newer events queue up behind it
            removeFromBuffer(bufferedEvent);
        }
        if (sent > 0) {
            logger.info("Sent " + sent + " buffered events to AMQP server");
        }
    }

    private Ternary<String, String, Long> peekBuffer() {
        synchronized (eventBuffer) {
            return eventBuffer.peek();
        }
    }

    /**
     * removes the event if it is still the oldest one, it might have been dropped from a full buffer already
     */
    private void removeFromBuffer(Ternary<String, String, Long> bufferedEvent) {
        synchronized (eventBuffer) {
            if (eventBuffer.peek() == bufferedEvent) {
                eventBuffer.poll();
            }
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
        if (connection == null) {
            try {
                return createConnection();
            } catch (KeyManagementException | NoSuchAlgorithmException | IOException  | TimeoutException e) {
                logger.error(String.format("Failed to create a connection to AMQP server [AMQP host:%s, port:%d] due to: %s", amqpHost, port, e));
                throw e;
            }
        } else {
            return connection;
        }
    }

    /**
     * @return the current connection, a new one if there was none, or null if the AMQP server cannot be reached
     */
    private synchronized Connection connectIfDisconnected() {
        if (connection == null) {
            try {
                Connection newConnection = createConnection();
                logger.info("Connected to AMQP server [AMQP host:{}, port:{}]", amqpHost, port);
                return newConnection;
            } catch (IOException e) {
                logger.trace("Unable to establish connection to AMQP server yet", e);
            } catch (Exception e) {
                logger.warn("Failed to reconnect to AMQP server due to " + e.getMessage());
            }
        }
        return connection;
    }

    private synchronized Connection createConnection() throws KeyManagementException, NoSuchAlgorithmException, IOException, TimeoutException {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setUsername(username);
            factory.setPassword(password);
            factory.setHost(amqpHost);
            factory.setPort(port);
            factory.setRequestedHeartbeat(heartbeat);

            if (virtualHost != null && !virtualHost.isEmpty()) {
                factory.setVirtualHost(virtualHost);
            } else {
                factory.setVirtualHost("/");
            }

            if (useSsl != null && !useSsl.isEmpty() && useSsl.equalsIgnoreCase("true")) {
                factory.useSslProtocol(secureProtocol);
            }
            Connection newConnection = factory.newConnection();
            newConnection.addShutdownListener(disconnectHandler);
            newConnection.addBlockedListener(blockedConnectionHandler);
            connection = newConnection;
            hasBeenConnectedOnce = true;
            return connection;
    }

    private synchronized void closeConnection() {
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (Exception e) {
            logger.warn("Failed to close connection to AMQP server due to " + e.getMessage());
        }
        connection = null;
    }

    private synchronized void abortConnection() {
        if (connection == null)
            return;

        try {
            connection.abort();
        } catch (Exception e) {
            logger.warn("Failed to abort connection due to " + e.getMessage());
        }
        connection = null;
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
            logger.warn("Failed to create a channel due to " + getErrorMessage(exception));
            throw exception;
        }
    }

    /**
     * @return the messages of the exception and of its causes. The AMQP client reports a lost connection as an IOException
     * without a message, wrapping a ShutdownSignalException ("connection error") that in turn wraps the actual reason, e.g.
     * "Connection reset" or "Heartbeat missing", so logging only getMessage() showed "null".
     */
    private static String getErrorMessage(Throwable exception) {
        StringBuilder message = new StringBuilder();
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            // skip messages already included, e.g. by exceptions created with the cause as their message
            if (cause.getMessage() != null && message.indexOf(cause.getMessage()) < 0) {
                message.append(message.length() > 0 ? ": " : "").append(cause.getMessage());
            }
        }
        return message.length() > 0 ? message.toString() : exception.toString();
    }

    private void createExchange(Channel channel, String exchangeName) throws Exception {
        try {
            channel.exchangeDeclare(exchangeName, "topic", true);
        } catch (java.io.IOException exception) {
            logger.error("Failed to create exchange" + exchangeName + " on RabbitMQ server");
            throw exception;
        }
    }

    private void publishEventToExchange(Channel channel, String exchangeName, String routingKey, String eventDescription) throws Exception {
        try {
            byte[] messageBodyBytes = eventDescription.getBytes();
            channel.basicPublish(exchangeName, routingKey, MessageProperties.PERSISTENT_TEXT_PLAIN, messageBodyBytes);
        } catch (Exception e) {
            logger.error("Failed to publish event " + routingKey + " on exchange " + exchangeName + "  of message broker due to " + getErrorMessage(e));
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
        // initiate connection to AMQP server
        executorService.scheduleWithFixedDelay(new ReconnectionTask(), retryInterval, retryInterval, TimeUnit.MILLISECONDS);
        return true;
    }

    @Override
    public synchronized boolean stop() {
        if (connection != null && connection.isOpen()) {
            for (String subscriberId : subscribers.keySet()) {
                Ternary<String, Channel, EventSubscriber> subscriberDetails = subscribers.get(subscriberId);
                Channel channel = subscriberDetails.second();
                String queueName = subscriberId;
                try {
                    channel.queueDelete(queueName);
                    channel.abort();
                } catch (IOException ioe) {
                    logger.warn("Failed to delete queue: " + queueName + " on AMQP server due to " + ioe.getMessage());
                }
            }
        }

        closeConnection();
        executorService.shutdownNow();
        return true;
    }

    //logic to deal with blocked connection. connections are blocked for example when the rabbitmq server is out of space. https://www.rabbitmq.com/connection-blocked.html
    private class BlockedConnectionHandler implements BlockedListener {

        @Override
        public void handleBlocked(String reason) throws IOException {
            logger.error("rabbitmq connection is blocked with reason: " + reason);
            closeConnection();
            throw new CloudRuntimeException("unblocking the parent thread as publishing to rabbitmq server is blocked with reason: " + reason);
        }

        @Override
        public void handleUnblocked() throws IOException {
            logger.info("rabbitmq connection in unblocked");
        }
    }
    // logic to deal with loss of connection to AMQP server
    private class DisconnectHandler implements ShutdownListener {

        @Override
        public void shutdownCompleted(ShutdownSignalException shutdownSignalException) {
            if (!shutdownSignalException.isInitiatedByApplication()) {

                for (String subscriberId : subscribers.keySet()) {
                    Ternary<String, Channel, EventSubscriber> subscriberDetails = subscribers.get(subscriberId);
                    subscriberDetails.second(null);
                    subscribers.put(subscriberId, subscriberDetails);
                }

                abortConnection(); // disconnected to AMQP server, so abort the connection and channels
                logger.warn("Connection has been shutdown by AMQP server. Attempting to reconnect.");
            }
        }
    }

    /**
     * delivers the events received from AMQP server to the subscriber
     */
    private class SubscriberConsumer extends DefaultConsumer {

        SubscriberConsumer(Channel channel) {
            super(channel);
        }

        @Override
        public void handleDelivery(String queueName, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
            Ternary<String, Channel, EventSubscriber> subscriberDetails = subscribers.get(queueName); // queue name == subscriber ID
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
    }

    /**
     * scheduled for the lifetime of the event bus: connects back to AMQP server after any loss of connection
     * and sends the events buffered in the meantime
     */
    private class ReconnectionTask extends ManagedContextRunnable {

        @Override
        public void run() {
            try {
                super.run();
            } catch (Throwable t) {
                // the executor would never run the task again if it ended with an exception
                logger.error("Failed to reconnect to AMQP server or to send buffered events", t);
            }
        }

        @Override
        protected void runInContext() {
            Connection currentConnection = connectIfDisconnected();
            if (currentConnection != null) {
                registerDisconnectedSubscribers(currentConnection);
            }
            flushBuffer();
        }
    }
}
