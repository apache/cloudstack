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

import java.text.DecimalFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import com.cloud.utils.Pair;
import org.apache.cloudstack.backup.Backup;
import org.apache.cloudstack.usage.UsageTypes;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.usage.UsageBackupVO;
import com.cloud.usage.UsageVO;
import com.cloud.usage.dao.UsageDao;
import com.cloud.usage.dao.UsageBackupDao;
import com.cloud.user.AccountVO;

@Component
public class BackupUsageParser {
    public static final Logger LOGGER = Logger.getLogger(BackupUsageParser.class);

    private static UsageDao s_usageDao;
    private static UsageBackupDao s_usageBackupDao;

    @Inject
    private UsageDao usageDao;
    @Inject
    private UsageBackupDao usageBackupDao;

    @PostConstruct
    void init() {
        s_usageDao = usageDao;
        s_usageBackupDao = usageBackupDao;
    }

    public static boolean parse(AccountVO account, Date startDate, Date endDate) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Parsing all VM Backup usage events for account: " + account.getId());
        }
        if ((endDate == null) || endDate.after(new Date())) {
            endDate = new Date();
        }

        final List<UsageBackupVO> usageBackups = s_usageBackupDao.getUsageRecords(account.getId(), startDate, endDate);
        if (usageBackups == null || usageBackups.isEmpty()) {
            LOGGER.debug("No VM Backup usage for this period");
            return true;
        }

        Map<String, Pair<Long, Long>> bkpUsageMap = new HashMap<String, Pair<Long, Long>>();
        final Map<String, BackupInfo> vmUsageMap = new HashMap<>();
        for (final UsageBackupVO usageBackup : usageBackups) {
            final Long vmId = usageBackup.getVmId();
            final Long zoneId = usageBackup.getZoneId();
            String key = vmId + "-" + usageBackup.getBackupOfferingId() + "-" + zoneId;
            final Long offeringId = usageBackup.getBackupOfferingId();
            if (vmUsageMap.get(key) == null) {
                vmUsageMap.put(key, new BackupUsageParser.BackupInfo(new Backup.Metric(0L, 0L), zoneId, vmId, offeringId));
            }
            final Backup.Metric metric = vmUsageMap.get(key).getMetric();
            metric.setBackupSize(metric.getBackupSize() + usageBackup.getSize());
            metric.setDataSize(metric.getDataSize() + usageBackup.getProtectedSize());
            long duration = getUsageDuration(usageBackup, startDate, endDate);
            updateBackupUsageData(bkpUsageMap, key, vmId, duration);
        }

        for (String bkpIdKey : bkpUsageMap.keySet()) {
            Pair<Long, Long> bkptimeInfo = bkpUsageMap.get(bkpIdKey);
            long useTime = bkptimeInfo.second();

            // Only create a usage record if we have a runningTime of bigger than zero.
            if (useTime > 0L) {
                BackupUsageParser.BackupInfo info = vmUsageMap.get(bkpIdKey);
                createUsageRecord(UsageTypes.BACKUP, account, useTime, startDate, endDate, info);
            }
        }

        return true;
    }

    private static void createUsageRecord(int type, AccountVO account, long runningTime, Date startDate, Date endDate, BackupInfo backupInfo) {
        // Our smallest increment is hourly for now
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Total running time " + runningTime + "ms");
        }
        final Long vmId = backupInfo.getVmId();
        final Long zoneId = backupInfo.getZoneId();
        final Long offeringId = backupInfo.getOfferingId();
        float usage = runningTime / 1000f / 60f / 60f;

        DecimalFormat dFormat = new DecimalFormat("#.######");
        String usageDisplay = dFormat.format(usage);

        final String description = String.format("Backup usage VM ID: %d", vmId);

        final UsageVO usageRecord =
                new UsageVO(zoneId, account.getAccountId(), account.getDomainId(), description, usageDisplay  + " Hrs",
                        type, (double) usage, vmId, null, offeringId, null, vmId,
                        backupInfo.getMetric().getBackupSize(), backupInfo.getMetric().getDataSize(), startDate, endDate);
        s_usageDao.persist(usageRecord);
    }

    private static long getUsageDuration(UsageBackupVO usageBackup, Date startDate, Date endDate) {
        Date backupCreateDate = usageBackup.getCreated();
        Date backupDeleteDate = usageBackup.getRemoved();

        if ((backupDeleteDate == null) || backupDeleteDate.after(endDate)) {
            backupDeleteDate = endDate;
        }

        // clip the start date to the beginning of our aggregation range if the vm has been running for a while
        if (backupCreateDate.before(startDate)) {
            backupCreateDate = startDate;
        }

        if (backupCreateDate.after(endDate)) {
            //Ignore records created after endDate
            return 0L;
        }
        return  (backupDeleteDate.getTime() - backupCreateDate.getTime()) + 1; // make sure this is an inclusive check for milliseconds (i.e. use n - m + 1 to find total number of millis to charge)
    }

    private static void updateBackupUsageData(Map<String, Pair<Long, Long>> usageDataMap, String key, long vmId, long duration) {
        Pair<Long, Long> backupUsageInfo = usageDataMap.get(key);
        if (backupUsageInfo == null) {
            backupUsageInfo = new Pair<Long, Long>(vmId, duration);
        } else {
            Long runningTime = backupUsageInfo.second();
            runningTime = runningTime + duration;
            backupUsageInfo = new Pair<Long, Long>(backupUsageInfo.first(), runningTime);
        }
        usageDataMap.put(key, backupUsageInfo);
    }

    static class BackupInfo {
        Backup.Metric metric;
        Long zoneId;
        Long vmId;
        Long offeringId;

        public BackupInfo(Backup.Metric metric, Long zoneId, Long vmId, Long offeringId) {
            this.metric = metric;
            this.zoneId = zoneId;
            this.vmId = vmId;
            this.offeringId = offeringId;
        }

        public Backup.Metric getMetric() {
            return metric;
        }

        public Long getZoneId() {
            return zoneId;
        }

        public Long getVmId() {
            return vmId;
        }

        public Long getOfferingId() {
            return offeringId;
        }
    }
}