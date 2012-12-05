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

package org.apache.cloudstack.Events.Notifications;

import com.cloud.utils.component.Manager;
import com.cloud.utils.component.PluggableService;
import org.apache.cloudstack.framework.events.EventTopic;

import javax.naming.ConfigurationException;
import java.util.Map;
import java.util.List;

public class EventNotificationManagerImpl implements EventNotificationManager, EventNotificationService, PluggableService, Manager {

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        return false;
    }

    @Override
    public boolean start() {
        return false;
    }

    @Override
    public boolean stop() {
        return false;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public String getPropertiesFile() {
        return null;
    }

    @Override
    public void subscribe(EventTopic topic, Endpoint endpoint) {

    }

    @Override
    public void unsubscribe(EventTopic topic, Endpoint endpoint) {

    }

    @Override
    public List<EventTopic> listSubscribedTopics() {

    }
}