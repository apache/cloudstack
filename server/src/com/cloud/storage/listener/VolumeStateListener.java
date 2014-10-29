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

package com.cloud.storage.listener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.cloud.utils.fsm.StateMachine2;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;

import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.framework.events.EventBus;
import org.apache.cloudstack.framework.events.EventBusException;

import com.cloud.configuration.Config;
import com.cloud.event.EventCategory;
import com.cloud.server.ManagementService;
import com.cloud.storage.Volume;
import com.cloud.storage.Volume.Event;
import com.cloud.storage.Volume.State;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.fsm.StateListener;

public class VolumeStateListener implements StateListener<State, Event, Volume> {

    protected static EventBus s_eventBus = null;
    protected ConfigurationDao _configDao;

    private static final Logger s_logger = Logger.getLogger(VolumeStateListener.class);

    public VolumeStateListener(ConfigurationDao configDao) {
        this._configDao = configDao;
    }

    @Override
    public boolean preStateTransitionEvent(State oldState, Event event, State newState, Volume vo, boolean status, Object opaque) {
        pubishOnEventBus(event.name(), "preStateTransitionEvent", vo, oldState, newState);
        return true;
    }

    @Override
    public boolean postStateTransitionEvent(StateMachine2.Transition<State, Event> transition, Volume vo, boolean status, Object opaque) {
      pubishOnEventBus(transition.getEvent().name(), "postStateTransitionEvent", vo, transition.getCurrentState(), transition.getToState());
      return true;
    }

  private void pubishOnEventBus(String event, String status, Volume vo, State oldState, State newState) {

        String configKey = Config.PublishResourceStateEvent.key();
        String value = _configDao.getValue(configKey);
        boolean configValue = Boolean.parseBoolean(value);
        if(!configValue)
            return;
        try {
            s_eventBus = ComponentContext.getComponent(EventBus.class);
        } catch (NoSuchBeanDefinitionException nbe) {
            return; // no provider is configured to provide events bus, so just return
        }

        String resourceName = getEntityFromClassName(Volume.class.getName());
        org.apache.cloudstack.framework.events.Event eventMsg =
            new org.apache.cloudstack.framework.events.Event(ManagementService.Name, EventCategory.RESOURCE_STATE_CHANGE_EVENT.getName(), event, resourceName,
                vo.getUuid());
        Map<String, String> eventDescription = new HashMap<String, String>();
        eventDescription.put("resource", resourceName);
        eventDescription.put("id", vo.getUuid());
        eventDescription.put("old-state", oldState.name());
        eventDescription.put("new-state", newState.name());

        String eventDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z").format(new Date());
        eventDescription.put("eventDateTime", eventDate);

        eventMsg.setDescription(eventDescription);
        try {
            s_eventBus.publish(eventMsg);
        } catch (EventBusException e) {
            s_logger.warn("Failed to state change event on the the event bus.");
        }
    }

    private String getEntityFromClassName(String entityClassName) {
        int index = entityClassName.lastIndexOf(".");
        String entityName = entityClassName;
        if (index != -1) {
            entityName = entityClassName.substring(index + 1);
        }
        return entityName;
    }
}
