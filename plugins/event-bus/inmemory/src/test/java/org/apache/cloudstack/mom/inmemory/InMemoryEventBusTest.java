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

package org.apache.cloudstack.mom.inmemory;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.UUID;

import org.apache.cloudstack.framework.events.Event;
import org.apache.cloudstack.framework.events.EventBusException;
import org.apache.cloudstack.framework.events.EventSubscriber;
import org.apache.cloudstack.framework.events.EventTopic;
import org.junit.Test;

import com.cloud.utils.UuidUtils;

public class InMemoryEventBusTest {

    @Test
    public void testConfigure() throws Exception {
        String name = "test001";

        InMemoryEventBus bus = new InMemoryEventBus();
        boolean success = bus.configure(name, null);

        assertTrue(success);
        assertTrue(name.equals(bus.getName()));
    }

    @Test
    public void testSubscribe() throws Exception {
        EventTopic topic = mock(EventTopic.class);
        EventSubscriber subscriber = mock(EventSubscriber.class);

        InMemoryEventBus bus = new InMemoryEventBus();

        UUID uuid = bus.subscribe(topic, subscriber);
        assertNotNull(uuid);

        String uuidStr = uuid.toString();
        assertTrue(UuidUtils.isUuid(uuidStr));
        assertTrue(bus.totalSubscribers() == 1);

        bus.unsubscribe(uuid, subscriber);
        assertTrue(bus.totalSubscribers() == 0);
    }

    @Test(expected = EventBusException.class)
    public void testSubscribeFailTopic() throws Exception {
        EventSubscriber subscriber = mock(EventSubscriber.class);

        InMemoryEventBus bus = new InMemoryEventBus();

        bus.subscribe(null, subscriber);
    }

    @Test(expected = EventBusException.class)
    public void testSubscribeFailSubscriber() throws Exception {
        EventTopic topic = mock(EventTopic.class);

        InMemoryEventBus bus = new InMemoryEventBus();

        bus.subscribe(topic, null);
    }

    @Test
    public void testUnsubscribe() throws Exception {
        EventTopic topic = mock(EventTopic.class);
        EventSubscriber subscriber = mock(EventSubscriber.class);

        InMemoryEventBus bus = new InMemoryEventBus();

        UUID uuid = bus.subscribe(topic, subscriber);
        assertNotNull(uuid);

        String uuidStr = uuid.toString();

        assertTrue(UuidUtils.isUuid(uuidStr));
        assertTrue(bus.totalSubscribers() == 1);
        //
        bus.unsubscribe(uuid, subscriber);
        assertTrue(bus.totalSubscribers() == 0);
    }

    @Test(expected = EventBusException.class)
    public void testUnsubscribeFailEmpty() throws Exception {
        UUID uuid = UUID.randomUUID();

        InMemoryEventBus bus = new InMemoryEventBus();
        bus.unsubscribe(uuid, null);
    }

    @Test(expected = EventBusException.class)
    public void testUnsubscribeFailNull() throws Exception {
        InMemoryEventBus bus = new InMemoryEventBus();
        bus.unsubscribe(null, null);
    }

    @Test(expected = EventBusException.class)
    public void testUnsubscribeFailWrongUUID() throws Exception {
        EventSubscriber subscriber = mock(EventSubscriber.class);
        InMemoryEventBus bus = new InMemoryEventBus();
        UUID uuid = UUID.randomUUID();

        bus.unsubscribe(uuid, subscriber);
    }

    @Test
    public void testPublish() throws Exception {
        EventTopic topic = mock(EventTopic.class);
        EventSubscriber subscriber = mock(EventSubscriber.class);
        Event event = mock(Event.class);

        InMemoryEventBus bus = new InMemoryEventBus();

        UUID uuid = bus.subscribe(topic, subscriber);
        assertNotNull(uuid);

        String uuidStr = uuid.toString();
        assertTrue(UuidUtils.isUuid(uuidStr));
        assertTrue(bus.totalSubscribers() == 1);

        bus.publish(event);

        verify(subscriber, times(1)).onEvent(event);

        bus.unsubscribe(uuid, subscriber);
        assertTrue(bus.totalSubscribers() == 0);
    }

    @Test
    public void testPublishEmpty() throws Exception {
        EventSubscriber subscriber = mock(EventSubscriber.class);
        Event event = mock(Event.class);

        InMemoryEventBus bus = new InMemoryEventBus();
        bus.publish(event);

        verify(subscriber, times(0)).onEvent(event);
    }
}
