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

package org.apache.cloudstack.framework.events;

import java.util.List;
import java.util.Map;

import org.apache.commons.collections.MapUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class EventDistributorImplTest {

    @InjectMocks
    EventDistributorImpl eventDistributor = new EventDistributorImpl();

    @Test
    public void testSetEventBuses() {
        Assert.assertNull(ReflectionTestUtils.getField(eventDistributor, "eventBuses"));
        EventBus eventBus = Mockito.mock(EventBus.class);
        EventBus eventBus1 = Mockito.mock(EventBus.class);
        eventDistributor.setEventBuses(List.of(eventBus, eventBus1));
        Assert.assertNotNull(ReflectionTestUtils.getField(eventDistributor, "eventBuses"));
    }

    @Test
    public void testPublishNullEvent() {
        Map<String, EventBusException> exceptionMap = eventDistributor.publish(null);
        Assert.assertTrue(MapUtils.isEmpty(exceptionMap));
    }

    @Test
    public void testPublishOneReturnsException() throws EventBusException {
        String busName = "Test";
        EventBus eventBus = Mockito.mock(EventBus.class);
        Mockito.doReturn(busName).when(eventBus).getName();
        Mockito.doThrow(EventBusException.class).when(eventBus).publish(Mockito.any(Event.class));
        EventBus eventBus1 = Mockito.mock(EventBus.class);
        Mockito.doNothing().when(eventBus1).publish(Mockito.any(Event.class));
        eventDistributor.eventBuses = List.of(eventBus, eventBus1);
        Map<String, EventBusException> exceptionMap = eventDistributor.publish(Mockito.mock(Event.class));
        Assert.assertTrue(MapUtils.isNotEmpty(exceptionMap));
        Assert.assertEquals(1, exceptionMap.size());
        Assert.assertTrue(exceptionMap.containsKey(busName));
    }
}
