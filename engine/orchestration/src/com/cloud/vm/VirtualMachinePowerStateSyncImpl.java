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

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.messagebus.MessageBus;
import org.apache.cloudstack.framework.messagebus.PublishScope;

import com.cloud.agent.api.HostVmStateReportEntry;
import com.cloud.utils.DateUtil;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.dao.VMInstanceDao;

public class VirtualMachinePowerStateSyncImpl implements VirtualMachinePowerStateSync {
    private static final Logger s_logger = Logger.getLogger(VirtualMachinePowerStateSyncImpl.class);

    @Inject MessageBus _messageBus;
    @Inject VMInstanceDao _instanceDao;
    @Inject VirtualMachineManager _vmMgr;

    protected final ConfigKey<Integer> PingInterval = new ConfigKey<Integer>(Integer.class, "ping.interval", "Advanced", "60",
            "Interval to send application level pings to make sure the connection is still working", false);

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

        Map<Long, VirtualMachine.PowerState> translatedInfo = convertVmStateReport(report);
        processReport(hostId, translatedInfo);
    }

    @Override
    public void processHostVmStatePingReport(long hostId, Map<String, HostVmStateReportEntry> report) {
        if (s_logger.isDebugEnabled())
            s_logger.debug("Process host VM state report from ping process. host: " + hostId);

        Map<Long, VirtualMachine.PowerState> translatedInfo = convertVmStateReport(report);
        processReport(hostId, translatedInfo);
    }

    private void processReport(long hostId, Map<Long, VirtualMachine.PowerState> translatedInfo) {

        if (s_logger.isDebugEnabled())
            s_logger.debug("Process VM state report. host: " + hostId + ", number of records in report: " + translatedInfo.size());

        for (Map.Entry<Long, VirtualMachine.PowerState> entry : translatedInfo.entrySet()) {

            if (s_logger.isDebugEnabled())
                s_logger.debug("VM state report. host: " + hostId + ", vm id: " + entry.getKey() + ", power state: " + entry.getValue());

            if (_instanceDao.updatePowerState(entry.getKey(), hostId, entry.getValue())) {
                if (s_logger.isDebugEnabled())
                    s_logger.debug("VM state report is updated. host: " + hostId + ", vm id: " + entry.getKey() + ", power state: " + entry.getValue());

                _messageBus.publish(null, VirtualMachineManager.Topics.VM_POWER_STATE, PublishScope.GLOBAL, entry.getKey());
            } else {
                if (s_logger.isDebugEnabled())
                    s_logger.debug("VM power state does not change, skip DB writing. vm id: " + entry.getKey());
            }
        }

        // for all running/stopping VMs, we provide monitoring of missing report
        List<VMInstanceVO> vmsThatAreMissingReport = _instanceDao.findByHostInStates(hostId, VirtualMachine.State.Running,
                VirtualMachine.State.Stopping, VirtualMachine.State.Starting);
        java.util.Iterator<VMInstanceVO> it = vmsThatAreMissingReport.iterator();
        while (it.hasNext()) {
            VMInstanceVO instance = it.next();
            if (translatedInfo.get(instance.getId()) != null)
                it.remove();
        }

        if (vmsThatAreMissingReport.size() > 0) {
            Date currentTime = DateUtil.currentGMTTime();
            if (s_logger.isDebugEnabled())
                s_logger.debug("Run missing VM report. current time: " + currentTime.getTime());

            // 2 times of sync-update interval for graceful period
            long milliSecondsGracefullPeriod = PingInterval.value() * 2000L;

            for (VMInstanceVO instance : vmsThatAreMissingReport) {

                // Make sure powerState is up to date for missing VMs
                try {
                    if (!_instanceDao.isPowerStateUpToDate(instance.getId())) {
                        s_logger.warn("Detected missing VM but power state is outdated, wait for another process report run for VM id: " + instance.getId());
                        _instanceDao.resetVmPowerStateTracking(instance.getId());
                        continue;
                    }
                } catch (CloudRuntimeException e) {
                    s_logger.warn("Checked for missing powerstate of a none existing vm", e);
                    continue;
                }

                Date vmStateUpdateTime = instance.getPowerStateUpdateTime();
                if (vmStateUpdateTime == null) {
                    s_logger.warn("VM state was updated but update time is null?! vm id: " + instance.getId());
                    vmStateUpdateTime = currentTime;
                }

                if (s_logger.isDebugEnabled())
                    s_logger.debug("Detected missing VM. host: " + hostId + ", vm id: " + instance.getId() +
                            ", power state: PowerReportMissing, last state update: " + vmStateUpdateTime.getTime());

                long milliSecondsSinceLastStateUpdate = currentTime.getTime() - vmStateUpdateTime.getTime();

                if (milliSecondsSinceLastStateUpdate > milliSecondsGracefullPeriod) {
                    s_logger.debug("vm id: " + instance.getId() + " - time since last state update(" + milliSecondsSinceLastStateUpdate + "ms) has passed graceful period");

                    if (_instanceDao.updatePowerState(instance.getId(), hostId, VirtualMachine.PowerState.PowerReportMissing)) {
                        if (s_logger.isDebugEnabled())
                            s_logger.debug("VM state report is updated. host: " + hostId + ", vm id: " + instance.getId() + ", power state: PowerReportMissing ");

                        _messageBus.publish(null, VirtualMachineManager.Topics.VM_POWER_STATE, PublishScope.GLOBAL, instance.getId());
                    } else {
                        if (s_logger.isDebugEnabled())
                            s_logger.debug("VM power state does not change, skip DB writing. vm id: " + instance.getId());
                    }
                } else {
                    s_logger.debug("vm id: " + instance.getId() + " - time since last state update(" + milliSecondsSinceLastStateUpdate + "ms) has not passed graceful period yet");
                }
            }
        }

        if (s_logger.isDebugEnabled())
            s_logger.debug("Done with process of VM state report. host: " + hostId);
    }

    @Override
    public Map<Long, VirtualMachine.PowerState> convertVmStateReport(Map<String, HostVmStateReportEntry> states) {
        final HashMap<Long, VirtualMachine.PowerState> map = new HashMap<Long, VirtualMachine.PowerState>();
        if (states == null) {
            return map;
        }

        for (Map.Entry<String, HostVmStateReportEntry> entry : states.entrySet()) {
            VMInstanceVO vm = findVM(entry.getKey());
            if (vm != null) {
                map.put(vm.getId(), entry.getValue().getState());
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
