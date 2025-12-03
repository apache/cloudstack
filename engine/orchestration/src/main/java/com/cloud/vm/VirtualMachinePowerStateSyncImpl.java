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
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.cloudstack.framework.messagebus.MessageBus;
import org.apache.cloudstack.framework.messagebus.PublishScope;
import org.apache.cloudstack.utils.cache.LazyCache;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cloud.agent.api.HostVmStateReportEntry;
import com.cloud.configuration.ManagementServiceConfiguration;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.utils.DateUtil;
import com.cloud.vm.dao.VMInstanceDao;

public class VirtualMachinePowerStateSyncImpl implements VirtualMachinePowerStateSync {
    protected Logger logger = LogManager.getLogger(getClass());

    @Inject MessageBus _messageBus;
    @Inject VMInstanceDao _instanceDao;
    @Inject HostDao hostDao;
    @Inject ManagementServiceConfiguration mgmtServiceConf;

    private LazyCache<Long, VMInstanceVO> vmCache;
    private LazyCache<Long, HostVO> hostCache;

    public VirtualMachinePowerStateSyncImpl() {
        vmCache = new LazyCache<>(16, 10, this::getVmFromId);
        hostCache = new LazyCache<>(16, 10, this::getHostFromId);
    }

    @Override
    public void resetHostSyncState(Host host) {
        logger.info("Reset VM power state sync for host: {}", host);
        _instanceDao.resetHostPowerStateTracking(host.getId());
    }

    @Override
    public void processHostVmStateReport(long hostId, Map<String, HostVmStateReportEntry> report) {
        logger.debug("Process host VM state report. host: {}", hostCache.get(hostId));
        Map<Long, VirtualMachine.PowerState> translatedInfo = convertVmStateReport(report);
        processReport(hostId, translatedInfo, false);
    }

    @Override
    public void processHostVmStatePingReport(long hostId, Map<String, HostVmStateReportEntry> report, boolean force) {
        logger.debug("Process host VM state report from ping process. host: {}", hostCache.get(hostId));
        Map<Long, VirtualMachine.PowerState> translatedInfo = convertVmStateReport(report);
        processReport(hostId, translatedInfo, force);
    }

    protected void updateAndPublishVmPowerStates(long hostId, Map<Long, VirtualMachine.PowerState> instancePowerStates,
               Date updateTime) {
        if (instancePowerStates.isEmpty()) {
            return;
        }
        Set<Long> vmIds = instancePowerStates.keySet();
        Map<Long, VirtualMachine.PowerState> notUpdated =
                _instanceDao.updatePowerState(instancePowerStates, hostId, updateTime);
        if (notUpdated.size() > vmIds.size()) {
            return;
        }
        for (Long vmId : vmIds) {
            if (!notUpdated.containsKey(vmId)) {
                logger.debug("VM state report is updated. {}, {}, power state: {}",
                        () -> hostCache.get(hostId), () -> vmCache.get(vmId), () -> instancePowerStates.get(vmId));
                _messageBus.publish(null, VirtualMachineManager.Topics.VM_POWER_STATE,
                        PublishScope.GLOBAL, vmId);
                continue;
            }
            logger.trace("VM power state does not change, skip DB writing. {}", () -> vmCache.get(vmId));
        }
    }

    private List<VMInstanceVO> filterOutdatedFromMissingVmReport(List<VMInstanceVO> vmsThatAreMissingReport) {
        List<Long> outdatedVms = vmsThatAreMissingReport.stream()
                .filter(v -> !_instanceDao.isPowerStateUpToDate(v))
                .map(VMInstanceVO::getId)
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(outdatedVms)) {
            return vmsThatAreMissingReport;
        }
        _instanceDao.resetVmPowerStateTracking(outdatedVms);
        return vmsThatAreMissingReport.stream()
                .filter(v -> !outdatedVms.contains(v.getId()))
                .collect(Collectors.toList());
    }

    private void processMissingVmReport(long hostId, Set<Long> vmIds, boolean force) {
        // any state outdates should be checked against the time before this list was retrieved
        Date startTime = DateUtil.currentGMTTime();
        // for all running/stopping VMs, we provide monitoring of missing report
        List<VMInstanceVO> vmsThatAreMissingReport = _instanceDao.findByHostInStatesExcluding(hostId, vmIds,
                VirtualMachine.State.Running, VirtualMachine.State.Stopping, VirtualMachine.State.Starting);
        // here we need to be wary of out of band migration as opposed to other, more unexpected state changes
        if (vmsThatAreMissingReport.isEmpty()) {
            return;
        }
        Date currentTime = DateUtil.currentGMTTime();
        logger.debug("Run missing VM report. current time: {}", currentTime.getTime());
        if (!force) {
            vmsThatAreMissingReport = filterOutdatedFromMissingVmReport(vmsThatAreMissingReport);
        }

        // 2 times of sync-update interval for graceful period
        long milliSecondsGracefulPeriod = mgmtServiceConf.getPingInterval() * 2000L;
        Map<Long, VirtualMachine.PowerState> instancePowerStates = new HashMap<>();
        for (VMInstanceVO instance : vmsThatAreMissingReport) {
            Date vmStateUpdateTime = instance.getPowerStateUpdateTime();
            if (vmStateUpdateTime == null) {
                logger.warn("VM power state update time is null, falling back to update time for {}", instance);
                vmStateUpdateTime = instance.getUpdateTime();
                if (vmStateUpdateTime == null) {
                    logger.warn("VM update time is null, falling back to creation time for {}", instance);
                    vmStateUpdateTime = instance.getCreated();
                }
            }
            logger.debug("Detected missing VM. host: {}, vm id: {}({}), power state: {}, last state update: {}",
                    hostId,
                    instance.getId(),
                    instance.getUuid(),
                    VirtualMachine.PowerState.PowerReportMissing,
                    DateUtil.getOutputString(vmStateUpdateTime));
            long milliSecondsSinceLastStateUpdate = currentTime.getTime() - vmStateUpdateTime.getTime();
            if (force || (milliSecondsSinceLastStateUpdate > milliSecondsGracefulPeriod)) {
                logger.debug("vm id: {} - time since last state update({} ms) has passed graceful period",
                        instance.getId(), milliSecondsSinceLastStateUpdate);
                // this is where a race condition might have happened if we don't re-fetch the instance;
                // between the startime of this job and the currentTime of this missing-branch
                // an update might have occurred that we should not override in case of out of band migration
                instancePowerStates.put(instance.getId(), VirtualMachine.PowerState.PowerReportMissing);
            } else {
                logger.debug("vm id: {} - time since last state update({} ms) has not passed graceful period yet",
                        instance.getId(), milliSecondsSinceLastStateUpdate);
            }
        }
        updateAndPublishVmPowerStates(hostId, instancePowerStates, startTime);
    }

    private void processReport(long hostId, Map<Long, VirtualMachine.PowerState> translatedInfo, boolean force) {
        logger.debug("Process VM state report. {}, number of records in report: {}. VMs: [{}]",
                () -> hostCache.get(hostId),
                translatedInfo::size,
                () -> translatedInfo.entrySet().stream().map(entry -> entry.getKey() + ":" + entry.getValue())
                        .collect(Collectors.joining(", ")) + "]");
        updateAndPublishVmPowerStates(hostId, translatedInfo, DateUtil.currentGMTTime());

        processMissingVmReport(hostId, translatedInfo.keySet(), force);

        logger.debug("Done with process of VM state report. host: {}", () -> hostCache.get(hostId));
    }

    public Map<Long, VirtualMachine.PowerState> convertVmStateReport(Map<String, HostVmStateReportEntry> states) {
        final HashMap<Long, VirtualMachine.PowerState> map = new HashMap<>();
        if (MapUtils.isEmpty(states)) {
            return map;
        }
        Map<String, Long> nameIdMap = _instanceDao.getNameIdMapForVmInstanceNames(states.keySet());
        for (Map.Entry<String, HostVmStateReportEntry> entry : states.entrySet()) {
            Long id = nameIdMap.get(entry.getKey());
            if (id != null) {
                map.put(id, entry.getValue().getState());
            } else {
                logger.debug("Unable to find matched VM in CloudStack DB. name: {} powerstate: {}", entry.getKey(), entry.getValue());
            }
        }
        return map;
    }

    protected VMInstanceVO getVmFromId(long vmId) {
        return _instanceDao.findById(vmId);
    }

    protected HostVO getHostFromId(long hostId) {
        return hostDao.findById(hostId);
    }
}
