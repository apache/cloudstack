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

package com.cloud.event;

import com.cloud.utils.component.Adapters;
import com.cloud.utils.component.ComponentLocator;
import org.apache.cloudstack.framework.events.*;
import org.apache.log4j.Logger;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

public class AlertGenerator {

    private static final Logger s_logger = Logger.getLogger(AlertGenerator.class);
    protected static EventBus _eventBus = null;
    protected static boolean _eventBusLoaded = false;

    public static void publishAlertOnEventBus(String alertType, long dataCenterId, Long podId, String subject, String body) {
        if (getEventBus() != null) {
            Map<String, String> eventDescription = new HashMap<String, String>();
            eventDescription.put("alertType", alertType);
            eventDescription.put("dataCenterId", Long.toString(dataCenterId));
            eventDescription.put("podId", Long.toString(podId));
            eventDescription.put("subject", subject);
            eventDescription.put("body", body);
            org.apache.cloudstack.framework.events.Event event =
                    new org.apache.cloudstack.framework.events.Event(null,
                            EventCategory.ALERT_EVENT.getName(),
                            alertType,
                            null,
                            null);
            event.setDescription(eventDescription);
            try {
                _eventBus.publish(event);
            } catch (EventBusException e) {
                s_logger.warn("Failed to publish alert on the the event bus.");
            }
        }
    }

    private static EventBus getEventBus() {
        if (_eventBus == null) {
            if (!_eventBusLoaded) {
                ComponentLocator locator = ComponentLocator.getLocator("management-server");
                Adapters<EventBus> eventBusImpls = locator.getAdapters(EventBus.class);
                if (eventBusImpls != null) {
                    Enumeration<EventBus> eventBusenum = eventBusImpls.enumeration();
                   _eventBus = eventBusenum.nextElement();
                }
                _eventBusLoaded = true;
            }
        }
        return _eventBus;
    }
}
