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
package com.cloud.vm;

import com.cloud.event.EventCategory;
import com.cloud.event.EventTypes;
import com.cloud.event.UsageEventUtils;
import com.cloud.event.dao.UsageEventDao;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.Network;
import com.cloud.server.ManagementServer;
import com.cloud.utils.fsm.StateListener;
import com.cloud.vm.VirtualMachine.Event;
import com.cloud.vm.VirtualMachine.State;
import com.cloud.vm.dao.NicDao;

import org.apache.log4j.Logger;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

public class UserVmStateListener implements StateListener<State, VirtualMachine.Event, VirtualMachine> {

    @Inject protected UsageEventDao _usageEventDao;
    @Inject protected NetworkDao _networkDao;
    @Inject protected NicDao _nicDao;
    private static final Logger s_logger = Logger.getLogger(UserVmStateListener.class);

    // get the event bus provider if configured
    @Inject protected org.apache.cloudstack.framework.events.EventBus _eventBus = null;
    
    public UserVmStateListener(UsageEventDao usageEventDao, NetworkDao networkDao, NicDao nicDao) {
        this._usageEventDao = usageEventDao;
        this._networkDao = networkDao;
        this._nicDao = nicDao;
    }
    
    @Override
    public boolean preStateTransitionEvent(State oldState, Event event, State newState, VirtualMachine vo, boolean status, Object opaque) {
        pubishOnEventBus(event.name(), "preStateTransitionEvent", vo, oldState, newState);
        return true;
    }

    @Override
    public boolean postStateTransitionEvent(State oldState, Event event, State newState, VirtualMachine vo, boolean status, Object opaque) {
        if(!status){
            return false;
        }
        
        if(vo.getType() != VirtualMachine.Type.User){
            return true;
        }
        
        pubishOnEventBus(event.name(), "postStateTransitionEvent", vo, oldState, newState);

        if (VirtualMachine.State.isVmCreated(oldState, event, newState)) {
            UsageEventUtils.saveUsageEvent(EventTypes.EVENT_VM_CREATE, vo.getAccountId(), vo.getDataCenterId(), vo.getId(), vo.getHostName(), vo.getServiceOfferingId(),
                    vo.getTemplateId(), vo.getHypervisorType().toString());
        } else if (VirtualMachine.State.isVmStarted(oldState, event, newState)) {
            UsageEventUtils.saveUsageEvent(EventTypes.EVENT_VM_START, vo.getAccountId(), vo.getDataCenterId(), vo.getId(), vo.getHostName(), vo.getServiceOfferingId(),
                    vo.getTemplateId(), vo.getHypervisorType().toString());
        } else if (VirtualMachine.State.isVmStopped(oldState, event, newState)) {
            UsageEventUtils.saveUsageEvent(EventTypes.EVENT_VM_STOP, vo.getAccountId(), vo.getDataCenterId(), vo.getId(), vo.getHostName());
            List<NicVO> nics = _nicDao.listByVmId(vo.getId());
            for (NicVO nic : nics) {
                NetworkVO network = _networkDao.findById(nic.getNetworkId());
                UsageEventUtils.saveUsageEvent(EventTypes.EVENT_NETWORK_OFFERING_REMOVE, vo.getAccountId(), vo.getDataCenterId(), vo.getId(), null, network.getNetworkOfferingId(), null, 0L);
            }
        } else if (VirtualMachine.State.isVmDestroyed(oldState, event, newState)) {
            UsageEventUtils.saveUsageEvent(EventTypes.EVENT_VM_DESTROY, vo.getAccountId(), vo.getDataCenterId(), vo.getId(), vo.getHostName(), vo.getServiceOfferingId(),
                    vo.getTemplateId(), vo.getHypervisorType().toString());
        } 
        return true;
    }

    private void pubishOnEventBus(String event, String status, VirtualMachine vo, VirtualMachine.State oldState, VirtualMachine.State newState) {

        if (_eventBus == null) {
            return; // no provider is configured to provide events bus, so just return
        }

        String resourceName = getEntityFromClassName(Network.class.getName());
        org.apache.cloudstack.framework.events.Event eventMsg =  new org.apache.cloudstack.framework.events.Event(
                ManagementServer.Name,
                EventCategory.RESOURCE_STATE_CHANGE_EVENT.getName(),
                event,
                resourceName,
                vo.getUuid());
        Map<String, String> eventDescription = new HashMap<String, String>();
        eventDescription.put("resource", resourceName);
        eventDescription.put("id", vo.getUuid());
        eventDescription.put("old-state", oldState.name());
        eventDescription.put("new-state", newState.name());
        eventMsg.setDescription(eventDescription);
        try {
            _eventBus.publish(eventMsg);
        } catch (org.apache.cloudstack.framework.events.EventBusException e) {
            s_logger.warn("Failed to publish state change event on the the event bus.");
        }

    }

    private String getEntityFromClassName(String entityClassName) {
        int index = entityClassName.lastIndexOf(".");
        String entityName = entityClassName;
        if (index != -1) {
            entityName = entityClassName.substring(index+1);
        }
        return entityName;
    }
}
