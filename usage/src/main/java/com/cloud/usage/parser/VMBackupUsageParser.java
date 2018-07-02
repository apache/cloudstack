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

package com.cloud.usage.parser;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.cloudstack.backup.VMBackup;
import org.apache.cloudstack.usage.UsageTypes;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.usage.UsageVMBackupVO;
import com.cloud.usage.UsageVO;
import com.cloud.usage.dao.UsageDao;
import com.cloud.usage.dao.UsageVMBackupDao;
import com.cloud.user.AccountVO;

@Component
public class VMBackupUsageParser {
    public static final Logger LOGGER = Logger.getLogger(VMBackupUsageParser.class);

    private static UsageDao s_usageDao;
    private static UsageVMBackupDao s_usageVMBackupDao;

    @Inject
    private UsageDao usageDao;
    @Inject
    private UsageVMBackupDao usageVMBackupDao;

    @PostConstruct
    void init() {
        s_usageDao = usageDao;
        s_usageVMBackupDao = usageVMBackupDao;
    }

    public static boolean parse(AccountVO account, Date startDate, Date endDate) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Parsing all VM Backup usage events for account: " + account.getId());
        }
        if ((endDate == null) || endDate.after(new Date())) {
            endDate = new Date();
        }

        final List<UsageVMBackupVO> usageVMBackups = s_usageVMBackupDao.getUsageRecords(account.getId(), startDate, endDate);
        if (usageVMBackups == null || usageVMBackups.isEmpty()) {
            LOGGER.debug("No VM Backup usage for this period");
            return true;
        }

        final Map<Long, BackupInfo> vmUsageMap = new HashMap<>();
        for (final UsageVMBackupVO usageVMBackup : usageVMBackups) {
            final Long vmId = usageVMBackup.getVmId();
            final Long zoneId = usageVMBackup.getZoneId();
            if (vmUsageMap.get(vmId) == null) {
                vmUsageMap.put(vmId, new VMBackupUsageParser.BackupInfo(new VMBackup.Metric(0L, 0L), zoneId, vmId));
            }
            final VMBackup.Metric metric = vmUsageMap.get(vmId).getMetric();
            metric.setBackupSize(metric.getBackupSize() + usageVMBackup.getSize());
            metric.setDataSize(metric.getDataSize() + usageVMBackup.getProtectedSize());
        }

        for (final BackupInfo backupInfo : vmUsageMap.values()) {
            final Long vmId = backupInfo.getVmId();
            final Long zoneId = backupInfo.getZoneId();
            final Double rawUsage = (double) backupInfo.getMetric().getBackupSize();
            final Double sizeGib = rawUsage / (1024.0 * 1024.0 * 1024.0);
            final String description = String.format("VMBackup usage VM Id: %d", vmId);
            final String usageDisplay = String.format("%.4f GiB", sizeGib);

            final UsageVO usageRecord =
                    new UsageVO(zoneId, account.getAccountId(), account.getDomainId(), description, usageDisplay,
                            UsageTypes.VM_BACKUP, rawUsage, vmId, null, null, null, vmId,
                            backupInfo.getMetric().getBackupSize(), backupInfo.getMetric().getDataSize(), startDate, endDate);
            s_usageDao.persist(usageRecord);
        }

        return true;
    }

    static class BackupInfo {
        VMBackup.Metric metric;
        Long zoneId;
        Long vmId;

        public BackupInfo(VMBackup.Metric metric, Long zoneId, Long vmId) {
            this.metric = metric;
            this.zoneId = zoneId;
            this.vmId = vmId;
        }

        public VMBackup.Metric getMetric() {
            return metric;
        }

        public Long getZoneId() {
            return zoneId;
        }

        public Long getVmId() {
            return vmId;
        }
    }
}