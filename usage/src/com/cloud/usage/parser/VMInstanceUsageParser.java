// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// the License.  You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.usage.parser;

import java.text.DecimalFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;
import org.apache.cloudstack.usage.UsageTypes;

import com.cloud.usage.UsageVMInstanceVO;
import com.cloud.usage.UsageVO;
import com.cloud.usage.dao.UsageDao;
import com.cloud.usage.dao.UsageVMInstanceDao;
import com.cloud.user.AccountVO;
import com.cloud.utils.Pair;
import com.cloud.utils.StringUtils;

@Component
public class VMInstanceUsageParser {
    public static final Logger s_logger = Logger.getLogger(VMInstanceUsageParser.class.getName());

    private static UsageDao s_usageDao;
    private static UsageVMInstanceDao s_usageInstanceDao;

    @Inject
    private UsageDao _usageDao;;
    @Inject
    private UsageVMInstanceDao _usageInstanceDao;

    @PostConstruct
    void init() {
        s_usageDao = _usageDao;
        s_usageInstanceDao = _usageInstanceDao;
    }

    public static boolean parse(AccountVO account, Date startDate, Date endDate) {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Parsing all VMInstance usage events for account: " + account.getId());
        }
        if ((endDate == null) || endDate.after(new Date())) {
            endDate = new Date();
        }

        // - query usage_vm_instance table with the following criteria:
        //     - look for an entry for accountId with start date in the given range
        //     - look for an entry for accountId with end date in the given range
        //     - look for an entry for accountId with end date null (currently running vm or owned IP)
        //     - look for an entry for accountId with start date before given range *and* end date after given range
        List<UsageVMInstanceVO> usageInstances = s_usageInstanceDao.getUsageRecords(account.getId(), startDate, endDate);
//ToDo: Add domainID for getting usage records

        // This map has both the running time *and* the usage amount.
        Map<String, Pair<String, Long>> usageVMUptimeMap = new HashMap<String, Pair<String, Long>>();
        Map<String, Pair<String, Long>> allocatedVMMap = new HashMap<String, Pair<String, Long>>();

        Map<String, VMInfo> vmInfosMap = new HashMap<String, VMInfo>();

        // loop through all the usage instances, create a usage record for each
        for (UsageVMInstanceVO usageInstance : usageInstances) {
            long vmId = usageInstance.getVmInstanceId();
            long soId = usageInstance.getSerivceOfferingId();
            long zoneId = usageInstance.getZoneId();
            long tId = usageInstance.getTemplateId();
            int usageType = usageInstance.getUsageType();
            Long cpuCores = usageInstance.getCpuCores();
            Long cpuSpeed = usageInstance.getCpuSpeed();
            Long memory = usageInstance.getMemory();
            String key = StringUtils.join("-", vmId, soId, usageType, cpuCores, cpuSpeed, memory);

            // store the info in the VMs map
            vmInfosMap.put(key, new VMInfo(vmId, zoneId, soId, tId, usageInstance.getHypervisorType(), cpuCores, cpuSpeed, memory));

            Date vmStartDate = usageInstance.getStartDate();
            Date vmEndDate = usageInstance.getEndDate();

            if ((vmEndDate == null) || vmEndDate.after(endDate)) {
                vmEndDate = endDate;
            }

            // clip the start date to the beginning of our aggregation range if the vm has been running for a while
            if (vmStartDate.before(startDate)) {
                vmStartDate = startDate;
            }

            if (vmStartDate.after(endDate)) {
                //Ignore records created after endDate
                continue;
            }

            long currentDuration = (vmEndDate.getTime() - vmStartDate.getTime()) + 1; // make sure this is an inclusive check for milliseconds (i.e. use n - m + 1 to find total number of millis to charge)

            switch (usageType) {
                case UsageTypes.ALLOCATED_VM:
                    updateVmUsageData(allocatedVMMap, key, usageInstance.getVmName(), currentDuration);
                    break;
                case UsageTypes.RUNNING_VM:
                    updateVmUsageData(usageVMUptimeMap, key, usageInstance.getVmName(), currentDuration);
                    break;
            }
        }

        for (String vmIdKey : usageVMUptimeMap.keySet()) {
            Pair<String, Long> vmUptimeInfo = usageVMUptimeMap.get(vmIdKey);
            long runningTime = vmUptimeInfo.second().longValue();

            // Only create a usage record if we have a runningTime of bigger than zero.
            if (runningTime > 0L) {
                VMInfo info = vmInfosMap.get(vmIdKey);
                createUsageRecord(UsageTypes.RUNNING_VM, runningTime, startDate, endDate, account, info.getVirtualMachineId(), vmUptimeInfo.first(), info.getZoneId(),
                    info.getServiceOfferingId(), info.getTemplateId(), info.getHypervisorType(), info.getCpuCores(), info.getCpuSpeed(), info.getMemory());
            }
        }

        for (String vmIdKey : allocatedVMMap.keySet()) {
            Pair<String, Long> vmAllocInfo = allocatedVMMap.get(vmIdKey);
            long allocatedTime = vmAllocInfo.second().longValue();

            // Only create a usage record if we have a runningTime of bigger than zero.
            if (allocatedTime > 0L) {
                VMInfo info = vmInfosMap.get(vmIdKey);
                createUsageRecord(UsageTypes.ALLOCATED_VM, allocatedTime, startDate, endDate, account, info.getVirtualMachineId(), vmAllocInfo.first(), info.getZoneId(),
                    info.getServiceOfferingId(), info.getTemplateId(), info.getHypervisorType(), info.getCpuCores(), info.getCpuSpeed(), info.getMemory());
            }
        }

        return true;
    }

    private static void updateVmUsageData(Map<String, Pair<String, Long>> usageDataMap, String key, String vmName, long duration) {
        Pair<String, Long> vmUsageInfo = usageDataMap.get(key);
        if (vmUsageInfo == null) {
            vmUsageInfo = new Pair<String, Long>(vmName, new Long(duration));
        } else {
            Long runningTime = vmUsageInfo.second();
            runningTime = new Long(runningTime.longValue() + duration);
            vmUsageInfo = new Pair<String, Long>(vmUsageInfo.first(), runningTime);
        }
        usageDataMap.put(key, vmUsageInfo);
    }

    private static void createUsageRecord(int type, long runningTime, Date startDate, Date endDate, AccountVO account, long vmId, String vmName, long zoneId,
        long serviceOfferingId, long templateId, String hypervisorType, Long cpuCores, Long cpuSpeed, Long memory) {
        // Our smallest increment is hourly for now
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Total running time " + runningTime + "ms");
        }

        float usage = runningTime / 1000f / 60f / 60f;

        DecimalFormat dFormat = new DecimalFormat("#.######");
        String usageDisplay = dFormat.format(usage);

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Creating VM usage record for vm: " + vmName + ", type: " + type + ", usage: " + usageDisplay + ", startDate: " + startDate + ", endDate: " +
                endDate + ", for account: " + account.getId());
        }

        // Create the usage record
        String usageDesc = vmName;
        if (type == UsageTypes.ALLOCATED_VM) {
            usageDesc += " allocated";
        } else {
            usageDesc += " running time";
        }
        usageDesc += " (ServiceOffering: " + serviceOfferingId + ") (Template: " + templateId + ")";
        UsageVO usageRecord =
            new UsageVO(Long.valueOf(zoneId), account.getId(), account.getDomainId(), usageDesc, usageDisplay + " Hrs", type, new Double(usage), Long.valueOf(vmId),
                vmName, cpuCores, cpuSpeed, memory, Long.valueOf(serviceOfferingId), Long.valueOf(templateId), Long.valueOf(vmId), startDate, endDate, hypervisorType);
        s_usageDao.persist(usageRecord);
    }

    private static class VMInfo {
        private final long virtualMachineId;
        private final long zoneId;
        private final long serviceOfferingId;
        private final long templateId;
        private final String hypervisorType;
        private final Long cpuCores;
        private final Long cpuSpeed;
        private final Long memory;

        public VMInfo(long vmId, long zId, long soId, long tId, String hypervisorType, Long cpuCores, Long cpuSpeed, Long memory) {
            virtualMachineId = vmId;
            zoneId = zId;
            serviceOfferingId = soId;
            templateId = tId;
            this.hypervisorType = hypervisorType;
            this.cpuCores = cpuCores;
            this.cpuSpeed = cpuSpeed;
            this.memory = memory;
        }

        public long getZoneId() {
            return zoneId;
        }

        public long getVirtualMachineId() {
            return virtualMachineId;
        }

        public long getServiceOfferingId() {
            return serviceOfferingId;
        }

        public long getTemplateId() {
            return templateId;
        }

        private String getHypervisorType() {
            return hypervisorType;
        }

        public Long getCpuCores() {
            return cpuCores;
        }

        public Long getCpuSpeed() {
            return cpuSpeed;
        }

        public Long getMemory() {
            return memory;
        }
    }
}
