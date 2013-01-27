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

import com.cloud.dc.DataCenterVO;
import com.cloud.dc.HostPodVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.server.ManagementServer;
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
    protected static boolean _eventBusProviderLoaded = false;

    private static DataCenterDao _dcDao =  ComponentLocator.getLocator(ManagementServer.Name).getDao(DataCenterDao.class);
    private static HostPodDao _podDao = ComponentLocator.getLocator(ManagementServer.Name).getDao(HostPodDao.class);

    public static void publishAlertOnEventBus(String alertType, long dataCenterId, Long podId, String subject, String body) {
        if (getEventBusProvider() == null) {

        }

        Map<String, String> eventDescription = new HashMap<String, String>();
        DataCenterVO dc = _dcDao.findById(dataCenterId);
        HostPodVO pod = _podDao.findById(podId);

        eventDescription.put("event", alertType);
        if (dc != null) {
            eventDescription.put("dataCenterId", dc.getUuid());
        } else {
            eventDescription.put("dataCenterId", null);
        }

        if (pod != null) {
            eventDescription.put("podId", pod.getUuid());
        } else {
            eventDescription.put("podId", null);
        }
        org.apache.cloudstack.framework.events.Event event =
                new org.apache.cloudstack.framework.events.Event(ManagementServer.Name,
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

    private static EventBus getEventBusProvider() {
        if (!_eventBusProviderLoaded) {
            ComponentLocator locator = ComponentLocator.getLocator(ManagementServer.Name);
            Adapters<EventBus> eventBusImpls = locator.getAdapters(EventBus.class);
            if (eventBusImpls != null) {
                Enumeration<EventBus> eventBusenum = eventBusImpls.enumeration();
                if (eventBusenum != null && eventBusenum.hasMoreElements()) {
                    _eventBus = eventBusenum.nextElement();
                }
            }
            _eventBusProviderLoaded = true;
        }
        return _eventBus;
    }
}
