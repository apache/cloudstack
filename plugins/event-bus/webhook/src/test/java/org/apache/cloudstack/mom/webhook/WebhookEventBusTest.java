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

package org.apache.cloudstack.mom.webhook;

import java.util.HashMap;
import java.util.UUID;

import javax.naming.ConfigurationException;

import org.apache.cloudstack.framework.events.Event;
import org.apache.cloudstack.framework.events.EventBusException;
import org.apache.cloudstack.framework.events.EventSubscriber;
import org.apache.cloudstack.framework.events.EventTopic;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class WebhookEventBusTest {

    @Mock
    WebhookService webhookService;
    @InjectMocks
    WebhookEventBus eventBus = new WebhookEventBus();

    @Test
    public void testConfigure() {
        String name = "name";
        try {
            Assert.assertTrue(eventBus.configure(name, new HashMap<>()));
            String result = (String)ReflectionTestUtils.getField(eventBus, "_name");
            Assert.assertEquals(name, result);
        } catch (ConfigurationException e) {
            Assert.fail("Error configuring");
        }
    }

    @Test
    public void testSetName() {
        String name = "name";
        eventBus.setName(name);
        String result = (String)ReflectionTestUtils.getField(eventBus, "_name");
        Assert.assertEquals(name, result);
    }

    @Test
    public void testGetName() {
        String name = "name";
        ReflectionTestUtils.setField(eventBus, "_name", name);
        Assert.assertEquals(name, eventBus.getName());
    }

    @Test
    public void testStart() {
        Assert.assertTrue(eventBus.start());
    }

    @Test
    public void testStop() {
        Assert.assertTrue(eventBus.stop());
    }

    @Test
    public void testSubscribe() {
        try {
            Assert.assertNotNull(eventBus.subscribe(Mockito.mock(EventTopic.class), Mockito.mock(EventSubscriber.class)));
        } catch (EventBusException e) {
            Assert.fail("Error subscribing");
        }
    }

    @Test
    public void testUnsubscribe() {
        try {
            eventBus.unsubscribe(Mockito.mock(UUID.class), Mockito.mock(EventSubscriber.class));
        } catch (EventBusException e) {
            Assert.fail("Error unsubscribing");
        }
    }

    @Test(expected = EventBusException.class)
    public void testPublishException() throws EventBusException {
        Mockito.doThrow(EventBusException.class).when(webhookService).handleEvent(Mockito.any(Event.class));
        eventBus.publish(Mockito.mock(Event.class));
    }
}
