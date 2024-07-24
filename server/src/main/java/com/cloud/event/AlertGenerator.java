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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.stereotype.Component;

import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.framework.events.EventBus;
import org.apache.cloudstack.framework.events.EventBusException;

import com.cloud.configuration.Config;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.HostPodVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.server.ManagementService;
import com.cloud.utils.component.ComponentContext;

@Component
public class AlertGenerator {

    private static final Logger s_logger = Logger.getLogger(AlertGenerator.class);
    private static DataCenterDao s_dcDao;
    private static HostPodDao s_podDao;
    protected static EventBus s_eventBus = null;
    protected static ConfigurationDao s_configDao;

    @Inject
    DataCenterDao dcDao;
    @Inject
    HostPodDao podDao;
    @Inject
    ConfigurationDao configDao;

    public AlertGenerator() {
    }

    @PostConstruct
    void init() {
        s_dcDao = dcDao;
        s_podDao = podDao;
        s_configDao = configDao;
    }

    public static void publishAlertOnEventBus(String alertType, long dataCenterId, Long podId, String subject, String body) {

        String configKey = Config.PublishAlertEvent.key();
        String value = s_configDao.getValue(configKey);
        boolean configValue = Boolean.parseBoolean(value);
        if(!configValue)
            return;
        try {
            s_eventBus = ComponentContext.getComponent(EventBus.class);
        } catch (NoSuchBeanDefinitionException nbe) {
            return; // no provider is configured to provide events bus, so just return
        }

        org.apache.cloudstack.framework.events.Event event =
            new org.apache.cloudstack.framework.events.Event(ManagementService.Name, EventCategory.ALERT_EVENT.getName(), alertType, null, null);

        Map<String, String> eventDescription = new HashMap<String, String>();
        DataCenterVO dc = s_dcDao.findById(dataCenterId);
        HostPodVO pod = s_podDao.findById(podId);

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
        eventDescription.put("subject", subject);
        eventDescription.put("body", body);

        String eventDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z").format(new Date());
        eventDescription.put("eventDateTime", eventDate);

        event.setDescription(eventDescription);

        try {
            s_eventBus.publish(event);
        } catch (EventBusException e) {
            s_logger.warn("Failed to publish alert on the event bus.");
        }
    }
}
