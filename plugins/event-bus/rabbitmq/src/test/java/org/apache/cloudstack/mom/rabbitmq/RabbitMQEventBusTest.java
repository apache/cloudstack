// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package org.apache.cloudstack.mom.rabbitmq;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.apache.cloudstack.framework.events.Event;
import org.apache.cloudstack.framework.events.EventBusException;
import org.apache.cloudstack.framework.events.EventSubscriber;
import org.apache.cloudstack.managed.context.ManagedContext;
import org.apache.cloudstack.managed.context.ManagedContextRunnable;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.InOrder;
import org.mockito.Mockito;

import com.cloud.utils.Ternary;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;

public class RabbitMQEventBusTest {

    private RabbitMQEventBus eventBus;
    private Connection connection;
    private Channel channel;
    private ConcurrentHashMap<String, Ternary<String, Channel, EventSubscriber>> subscribers;

    @Before
    public void setUp() throws Exception {
        eventBus = new RabbitMQEventBus();
        RabbitMQEventBus.setExchange("cloudstack-events");
        RabbitMQEventBus.setBufferSize(1000);
        RabbitMQEventBus.setBufferExpiry(300000);

        channel = Mockito.mock(Channel.class);
        connection = Mockito.mock(Connection.class);
        Mockito.when(connection.createChannel()).thenReturn(channel);

        subscribers = new ConcurrentHashMap<>();
        getField("subscribers").set(null, subscribers);
        ((ArrayDeque<?>)getField("eventBuffer").get(null)).clear();
        setConnection(null);
        getField("hasBeenConnectedOnce").set(null, true);
    }

    @After
    public void tearDown() throws Exception {
        setConnection(null);
        getField("hasBeenConnectedOnce").set(null, false);
    }

    private Field getField(String name) throws Exception {
        Field field = RabbitMQEventBus.class.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private void setConnection(Connection connection) throws Exception {
        getField("connection").set(null, connection);
    }

    private void flushBuffer() throws Exception {
        Method method = RabbitMQEventBus.class.getDeclaredMethod("flushBuffer");
        method.setAccessible(true);
        method.invoke(eventBus);
    }

    private Runnable reconnectionTask() throws Exception {
        Constructor<?> constructor = Class.forName(RabbitMQEventBus.class.getName() + "$ReconnectionTask").getDeclaredConstructor(RabbitMQEventBus.class);
        constructor.setAccessible(true);
        return (Runnable)constructor.newInstance(eventBus);
    }

    private void addSubscriber(String queueName, Channel subscriberChannel) {
        subscribers.put(queueName, new Ternary<>("#", subscriberChannel, Mockito.mock(EventSubscriber.class)));
    }

    private Event event(String description) {
        Event event = new Event("server", "ActionEvent", "VM.START", "VirtualMachine", "uuid");
        event.setDescription(description);
        return event;
    }

    private void verifyPublished(InOrder inOrder, String description) throws IOException {
        inOrder.verify(channel).basicPublish(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.any(),
                ArgumentMatchers.eq(description.getBytes()));
    }

    private void verifyPublishedCount(int count) throws IOException {
        Mockito.verify(channel, Mockito.times(count)).basicPublish(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(),
                ArgumentMatchers.any(), ArgumentMatchers.any(byte[].class));
    }

    @Test(expected = EventBusException.class)
    public void publishFailsWhenBrokerWasNeverReachable() throws Exception {
        getField("hasBeenConnectedOnce").set(null, false);
        RabbitMQEventBus.setServer("localhost");
        RabbitMQEventBus.setPort(1);
        RabbitMQEventBus.setUsername("guest");
        RabbitMQEventBus.setPassword("guest");

        eventBus.publish(event("1"));
    }

    @Test
    public void eventsPublishedWhileDisconnectedAreSentInOrderAfterReconnect() throws Exception {
        eventBus.publish(event("1"));
        eventBus.publish(event("2"));
        Mockito.verifyNoInteractions(channel);

        setConnection(connection);
        eventBus.publish(event("3")); // must not overtake the buffered ones
        Mockito.verifyNoInteractions(channel);

        flushBuffer();
        InOrder inOrder = Mockito.inOrder(channel);
        verifyPublished(inOrder, "1");
        verifyPublished(inOrder, "2");
        verifyPublished(inOrder, "3");

        eventBus.publish(event("4")); // buffer is empty again, sent directly
        verifyPublished(inOrder, "4");
        verifyPublishedCount(4);
    }

    @Test
    public void eventIsBufferedWhenSendingFails() throws Exception {
        setConnection(connection);
        Mockito.doThrow(new IOException("broken pipe")).doNothing().when(channel)
                .basicPublish(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.any(), ArgumentMatchers.any(byte[].class));

        eventBus.publish(event("1"));
        verifyPublishedCount(1);

        flushBuffer();
        verifyPublishedCount(2);
    }

    @Test
    public void oldestEventIsDroppedWhenBufferIsFull() throws Exception {
        RabbitMQEventBus.setBufferSize(2);
        eventBus.publish(event("1"));
        eventBus.publish(event("2"));
        eventBus.publish(event("3"));

        setConnection(connection);
        flushBuffer();
        InOrder inOrder = Mockito.inOrder(channel);
        verifyPublished(inOrder, "2");
        verifyPublished(inOrder, "3");
        verifyPublishedCount(2);
    }

    @Test
    public void expiredEventsAreDropped() throws Exception {
        RabbitMQEventBus.setBufferExpiry(-1);
        eventBus.publish(event("1"));

        flushBuffer(); // expires also without a connection
        setConnection(connection);
        flushBuffer();
        Mockito.verifyNoInteractions(channel);
    }

    @Test
    public void onlySubscribersWithoutOpenChannelAreRegistered() throws Exception {
        Channel openChannel = Mockito.mock(Channel.class);
        Mockito.when(openChannel.isOpen()).thenReturn(true);
        addSubscriber("open", openChannel);
        addSubscriber("closed", Mockito.mock(Channel.class));
        addSubscriber("none", null);
        setConnection(connection); // e.g. connected by subscribe() and not by the task itself

        reconnectionTask().run();

        Mockito.verify(channel).queueBind("closed", "cloudstack-events", "#");
        Mockito.verify(channel).queueBind("none", "cloudstack-events", "#");
        Mockito.verify(channel, Mockito.never()).queueBind(ArgumentMatchers.eq("open"), ArgumentMatchers.anyString(), ArgumentMatchers.anyString());
        Assert.assertSame(openChannel, subscribers.get("open").second());
        Assert.assertSame(channel, subscribers.get("none").second());
    }

    @Test
    public void bufferedEventsAreSentEvenIfSubscriberCannotBeRegistered() throws Exception {
        addSubscriber("failing", null);
        Mockito.when(channel.queueBind(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString()))
                .thenThrow(new IOException("bind failed"));
        eventBus.publish(event("1"));
        setConnection(connection);

        reconnectionTask().run();
        verifyPublishedCount(1);
    }

    @Test
    public void reconnectionTaskKeepsRunningAfterUnexpectedFailure() throws Exception {
        ManagedContext context = Mockito.mock(ManagedContext.class);
        Mockito.doThrow(new IllegalStateException("context failure")).when(context).runWithContext(ArgumentMatchers.any());
        ManagedContextRunnable.initializeGlobalContext(context);
        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        getField("executorService").set(eventBus, executorService);
        RabbitMQEventBus.setRetryInterval(10);
        try {
            eventBus.start();
            Mockito.verify(context, Mockito.timeout(5000).atLeast(3)).runWithContext(ArgumentMatchers.any());
        } finally {
            eventBus.stop();
            ManagedContextRunnable.setManagedContext(false);
        }
        Assert.assertTrue(executorService.isShutdown());
    }
}
