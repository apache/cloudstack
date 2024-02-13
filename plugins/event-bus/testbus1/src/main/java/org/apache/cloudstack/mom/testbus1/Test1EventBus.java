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

package org.apache.cloudstack.mom.testbus1;

import java.util.Map;
import java.util.UUID;

import javax.naming.ConfigurationException;

import org.apache.cloudstack.framework.events.Event;
import org.apache.cloudstack.framework.events.EventBus;
import org.apache.cloudstack.framework.events.EventBusException;
import org.apache.cloudstack.framework.events.EventSubscriber;
import org.apache.cloudstack.framework.events.EventTopic;
import org.apache.log4j.Logger;

import com.cloud.utils.component.ManagerBase;

public class Test1EventBus extends ManagerBase implements EventBus {

    private static final Logger s_logger = Logger.getLogger(Test1EventBus.class);

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        _name = name;
        return true;
    }

    @Override
    public void setName(String name) {
        _name = name;
    }

    @Override
    public UUID subscribe(EventTopic topic, EventSubscriber subscriber) throws EventBusException {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug(String.format("subscribing \'%s\' to events of type \'%s\' from \'%s\'",subscriber.toString(), topic.getEventType(), topic.getEventSource()));
        }

        /* NOOP */
        return UUID.randomUUID();
    }

    @Override
    public void unsubscribe(UUID subscriberId, EventSubscriber subscriber) throws EventBusException {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug(String.format("unsubscribing \'%s\'",subscriberId));
        }
        /* NOOP */
    }

    @Override
    public void publish(Event event) throws EventBusException {
        s_logger.info(String.format("New event: %s", event.getDescription()));
    }

    @Override
    public String getName() {
        return _name;
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }
}
