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

import com.cloud.event.EventTypes;
import com.cloud.event.UsageEventUtils;
import com.cloud.utils.fsm.StateMachine2;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.VMInstanceDao;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
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
    protected VMInstanceDao _vmInstanceDao;

    protected Logger logger = LogManager.getLogger(getClass());

    public VolumeStateListener(ConfigurationDao configDao, VMInstanceDao vmInstanceDao) {
        this._configDao = configDao;
        this._vmInstanceDao = vmInstanceDao;
    }

    @Override
    public boolean preStateTransitionEvent(State oldState, Event event, State newState, Volume vo, boolean status, Object opaque) {
        pubishOnEventBus(event.name(), "preStateTransitionEvent", vo, oldState, newState);
        return true;
    }

    @Override
    public boolean postStateTransitionEvent(StateMachine2.Transition<State, Event> transition, Volume vol, boolean status, Object opaque) {
      pubishOnEventBus(transition.getEvent().name(), "postStateTransitionEvent", vol, transition.getCurrentState(), transition.getToState());
      if(transition.isImpacted(StateMachine2.Transition.Impact.USAGE)) {
        Long instanceId = vol.getInstanceId();
        VMInstanceVO vmInstanceVO = null;
        if(instanceId != null) {
          vmInstanceVO = _vmInstanceDao.findById(instanceId);
        }
        if(instanceId == null || vmInstanceVO.getType() == VirtualMachine.Type.User) {
          if (transition.getToState() == State.Ready) {
            if (transition.getCurrentState() == State.Resizing) {
              // Log usage event for volumes belonging user VM's only
              // For the Resize Volume Event, this  publishes an event with an incorrect disk offering ID, so do nothing for now
            } else {
              UsageEventUtils.publishUsageEvent(EventTypes.EVENT_VOLUME_CREATE, vol.getAccountId(), vol.getDataCenterId(), vol.getId(), vol.getName(), vol.getDiskOfferingId(), null, vol.getSize(),
                      Volume.class.getName(), vol.getUuid(), vol.isDisplayVolume());
            }
          } else if (transition.getToState() == State.Destroy && vol.getVolumeType() != Volume.Type.ROOT) { //Do not Publish Usage Event for ROOT Disk as it would have been published already while destroying a VM
            UsageEventUtils.publishUsageEvent(EventTypes.EVENT_VOLUME_DELETE, vol.getAccountId(), vol.getDataCenterId(), vol.getId(), vol.getName(),
                    Volume.class.getName(), vol.getUuid(), vol.isDisplayVolume());
          } else if (transition.getToState() == State.Uploaded) {
            //Currently we are not capturing Usage for Secondary Storage so Usage for this operation will be captured when it is moved to primary storage
          }
        }
      }
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
            logger.warn("Failed to state change event on the event bus.");
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
