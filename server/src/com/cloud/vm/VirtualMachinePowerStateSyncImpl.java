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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.apache.cloudstack.framework.messagebus.MessageBus;
import org.apache.cloudstack.framework.messagebus.PublishScope;
import org.apache.cloudstack.messagebus.SubjectConstants;
import org.apache.log4j.Logger;

import com.cloud.agent.api.HostVmStateReportEntry;
import com.cloud.vm.dao.VMInstanceDao;

public class VirtualMachinePowerStateSyncImpl implements VirtualMachinePowerStateSync {
    private static final Logger s_logger = Logger.getLogger(VirtualMachinePowerStateSyncImpl.class);

    @Inject MessageBus _messageBus;
    @Inject VMInstanceDao _instanceDao;
    @Inject VirtualMachineManager _vmMgr;
    
    public VirtualMachinePowerStateSyncImpl() {
    }
    
    @Override
	public void resetHostSyncState(long hostId) {
    	s_logger.info("Reset VM power state sync for host: " + hostId);
    	_instanceDao.resetHostPowerStateTracking(hostId);
    }
    
    @Override
	public void processHostVmStateReport(long hostId, Map<String, HostVmStateReportEntry> report) {
    	s_logger.info("Process host VM state report. host: " + hostId);
    	
    	Map<Long, VirtualMachine.PowerState> translatedInfo = convertToInfos(report);
    	for(Map.Entry<Long, VirtualMachine.PowerState> entry : translatedInfo.entrySet()) {
    		if(_instanceDao.updatePowerState(entry.getKey(), hostId, entry.getValue())) {
    			_messageBus.publish(null, SubjectConstants.VM_POWER_STATE, PublishScope.GLOBAL, entry.getKey());
    		}
    	}
    }
    
    protected Map<Long, VirtualMachine.PowerState> convertToInfos(Map<String, HostVmStateReportEntry> states) { 
        final HashMap<Long, VirtualMachine.PowerState> map = new HashMap<Long, VirtualMachine.PowerState>();
        if (states == null) {
            return map;
        }
        
        Collection<VirtualMachineGuru<? extends VMInstanceVO>> vmGurus = _vmMgr.getRegisteredGurus();

        for (Map.Entry<String, HostVmStateReportEntry> entry : states.entrySet()) {
            for (VirtualMachineGuru<? extends VMInstanceVO> vmGuru : vmGurus) {
                String name = entry.getKey();
                VMInstanceVO vm = vmGuru.findByName(name);
                if (vm != null) {
                    map.put(vm.getId(), entry.getValue().getState());
                    break;
                }
                
                Long id = vmGuru.convertToId(name);
                if (id != null) {
                	vm = vmGuru.findById(id);
                	if(vm != null) {
	                    map.put(id, entry.getValue().getState());
	                    break;
                	}
                }
            }
        }

        return map;
    }
}
