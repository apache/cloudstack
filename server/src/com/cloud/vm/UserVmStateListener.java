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
import com.cloud.event.UsageEventVO;
import com.cloud.event.dao.UsageEventDao;
import com.cloud.network.Network;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.server.ManagementServer;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.fsm.StateListener;
import com.cloud.vm.VirtualMachine.Event;
import com.cloud.vm.VirtualMachine.State;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.UserVmDetailsDao;
import org.apache.cloudstack.framework.events.EventBus;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;

import javax.inject.Inject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserVmStateListener implements StateListener<State, VirtualMachine.Event, VirtualMachine> {

    @Inject protected UsageEventDao _usageEventDao;
    @Inject protected NetworkDao _networkDao;
    @Inject protected NicDao _nicDao;
    @Inject protected ServiceOfferingDao _offeringDao;
    private static final Logger s_logger = Logger.getLogger(UserVmStateListener.class);

    protected static EventBus _eventBus = null;
    
    public UserVmStateListener(UsageEventDao usageEventDao, NetworkDao networkDao, NicDao nicDao, ServiceOfferingDao offeringDao) {
        this._usageEventDao = usageEventDao;
        this._networkDao = networkDao;
        this._nicDao = nicDao;
        this._offeringDao = offeringDao;
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
            generateUsageEvent(vo.getServiceOfferingId(), vo, EventTypes.EVENT_VM_CREATE);
        } else if (VirtualMachine.State.isVmStarted(oldState, event, newState)) {
            generateUsageEvent(vo.getServiceOfferingId(), vo, EventTypes.EVENT_VM_START);
        } else if (VirtualMachine.State.isVmStopped(oldState, event, newState)) {
            generateUsageEvent(vo.getServiceOfferingId(), vo, EventTypes.EVENT_VM_STOP);
            List<NicVO> nics = _nicDao.listByVmId(vo.getId());
            for (NicVO nic : nics) {
                NetworkVO network = _networkDao.findById(nic.getNetworkId());
                UsageEventUtils.publishUsageEvent(EventTypes.EVENT_NETWORK_OFFERING_REMOVE, vo.getAccountId(), vo.getDataCenterId(),
                        vo.getId(), Long.toString(nic.getId()),network.getNetworkOfferingId(), null, 0L, vo.getClass().getName(), vo.getUuid());
            }
        } else if (VirtualMachine.State.isVmDestroyed(oldState, event, newState)) {
            generateUsageEvent(vo.getServiceOfferingId(), vo, EventTypes.EVENT_VM_DESTROY);
        } 
        return true;
    }

    private void generateUsageEvent(Long serviceOfferingId,VirtualMachine vm,  String eventType){
        ServiceOfferingVO serviceOffering = _offeringDao.findById(vm.getId(), serviceOfferingId);
        if (!serviceOffering.isDynamic()) {
            UsageEventUtils.publishUsageEvent(eventType, vm.getAccountId(), vm.getDataCenterId(), vm.getId(),
                    vm.getHostName(), serviceOffering.getId(), vm.getTemplateId(), vm.getHypervisorType().toString(),
                    VirtualMachine.class.getName(), vm.getUuid());
        }
        else {
            Map<String, String> customParameters = new HashMap<String, String>();
            customParameters.put(UsageEventVO.DynamicParameters.cpuNumber.name(), serviceOffering.getCpu().toString());
            customParameters.put(UsageEventVO.DynamicParameters.cpuSpeed.name(), serviceOffering.getSpeed().toString());
            customParameters.put(UsageEventVO.DynamicParameters.memory.name(), serviceOffering.getRamSize().toString());
            UsageEventUtils.publishUsageEvent(eventType, vm.getAccountId(), vm.getDataCenterId(), vm.getId(),
                    vm.getHostName(), serviceOffering.getId(), vm.getTemplateId(), vm.getHypervisorType().toString(),
                    VirtualMachine.class.getName(), vm.getUuid(), customParameters);
        }
    }

    private void pubishOnEventBus(String event, String status, VirtualMachine vo, VirtualMachine.State oldState, VirtualMachine.State newState) {

        try {
            _eventBus = ComponentContext.getComponent(EventBus.class);
        } catch(NoSuchBeanDefinitionException nbe) {
            return; // no provider is configured to provide events bus, so just return
        }

        String resourceName = getEntityFromClassName(VirtualMachine.class.getName());
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

        String eventDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z").format(new Date());
        eventDescription.put("eventDateTime", eventDate);

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
