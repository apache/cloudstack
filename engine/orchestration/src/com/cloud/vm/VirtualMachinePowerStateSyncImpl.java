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

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.apache.log4j.Logger;

import org.apache.cloudstack.framework.messagebus.MessageBus;
import org.apache.cloudstack.framework.messagebus.PublishScope;

import com.cloud.agent.api.HostVmStateReportEntry;
import com.cloud.vm.dao.VMInstanceDao;

public class VirtualMachinePowerStateSyncImpl implements VirtualMachinePowerStateSync {
    private static final Logger s_logger = Logger.getLogger(VirtualMachinePowerStateSyncImpl.class);

    @Inject
    MessageBus _messageBus;
    @Inject
    VMInstanceDao _instanceDao;
    @Inject
    VirtualMachineManager _vmMgr;

    public VirtualMachinePowerStateSyncImpl() {
    }

    @Override
    public void resetHostSyncState(long hostId) {
        s_logger.info("Reset VM power state sync for host: " + hostId);
        _instanceDao.resetHostPowerStateTracking(hostId);
    }

    @Override
    public void processHostVmStateReport(long hostId, Map<String, HostVmStateReportEntry> report) {
        if (s_logger.isDebugEnabled())
            s_logger.debug("Process host VM state report from ping process. host: " + hostId);

        Map<Long, VirtualMachine.PowerState> translatedInfo = convertToInfos(report);
        processReport(hostId, translatedInfo);
    }

    @Override
    public void processHostVmStatePingReport(long hostId, Map<String, HostVmStateReportEntry> report) {
        if (s_logger.isDebugEnabled())
            s_logger.debug("Process host VM state report from ping process. host: " + hostId);

        Map<Long, VirtualMachine.PowerState> translatedInfo = convertToInfos(report);
        processReport(hostId, translatedInfo);
    }

    private void processReport(long hostId, Map<Long, VirtualMachine.PowerState> translatedInfo) {

        for (Map.Entry<Long, VirtualMachine.PowerState> entry : translatedInfo.entrySet()) {

            if (s_logger.isDebugEnabled())
                s_logger.debug("VM state report. host: " + hostId + ", vm id: " + entry.getKey() + ", power state: " + entry.getValue());

            if (_instanceDao.updatePowerState(entry.getKey(), hostId, entry.getValue())) {

                if (s_logger.isDebugEnabled())
                    s_logger.debug("VM state report is updated. host: " + hostId + ", vm id: " + entry.getKey() + ", power state: " + entry.getValue());

                _messageBus.publish(null, VirtualMachineManager.Topics.VM_POWER_STATE, PublishScope.GLOBAL, entry.getKey());
            }
        }
    }

    private Map<Long, VirtualMachine.PowerState> convertToInfos(Map<String, HostVmStateReportEntry> states) {
        final HashMap<Long, VirtualMachine.PowerState> map = new HashMap<Long, VirtualMachine.PowerState>();
        if (states == null) {
            return map;
        }

        for (Map.Entry<String, HostVmStateReportEntry> entry : states.entrySet()) {
            VMInstanceVO vm = findVM(entry.getKey());
            if (vm != null) {
                map.put(vm.getId(), entry.getValue().getState());
                break;
            } else {
                s_logger.info("Unable to find matched VM in CloudStack DB. name: " + entry.getKey());
            }
        }

        return map;
    }

    private VMInstanceVO findVM(String vmName) {
        return _instanceDao.findVMByInstanceName(vmName);
    }
}
