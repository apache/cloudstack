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

import com.cloud.usage.UsageVMSnapshotVO;
import com.cloud.usage.UsageVO;
import com.cloud.usage.dao.UsageDao;
import com.cloud.usage.dao.UsageVMSnapshotDao;
import com.cloud.user.AccountVO;

@Component
public class VMSnapshotUsageParser {
    public static final Logger s_logger = Logger.getLogger(VMSnapshotUsageParser.class.getName());

    private static UsageDao s_usageDao;
    private static UsageVMSnapshotDao s_usageVMSnapshotDao;

    @Inject
    private UsageDao _usageDao;
    @Inject
    private UsageVMSnapshotDao _usageVMSnapshotDao;

    @PostConstruct
    void init() {
        s_usageDao = _usageDao;
        s_usageVMSnapshotDao = _usageVMSnapshotDao;
    }

    public static boolean parse(AccountVO account, Date startDate, Date endDate) {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Parsing all VmSnapshot volume usage events for account: " + account.getId());
        }
        if ((endDate == null) || endDate.after(new Date())) {
            endDate = new Date();
        }

        List<UsageVMSnapshotVO> usageUsageVMSnapshots = s_usageVMSnapshotDao.getUsageRecords(account.getId(), account.getDomainId(), startDate, endDate);

        if (usageUsageVMSnapshots.isEmpty()) {
            s_logger.debug("No VM snapshot usage events for this period");
            return true;
        }

        Map<String, UsageVMSnapshotVO> unprocessedUsage = new HashMap<String, UsageVMSnapshotVO>();
        for (UsageVMSnapshotVO usageRec : usageUsageVMSnapshots) {
            long zoneId = usageRec.getZoneId();
            Long volId = usageRec.getId();
            long vmId = usageRec.getVmId();
            String key = vmId + ":" + volId;
            if (usageRec.getCreated().before(startDate)) {
                unprocessedUsage.put(key, usageRec);
                continue;
            }
            UsageVMSnapshotVO previousEvent = s_usageVMSnapshotDao.getPreviousUsageRecord(usageRec);
            if (previousEvent == null || previousEvent.getSize() == 0) {
                unprocessedUsage.put(key, usageRec);
                continue;
            }

            Date previousCreated = previousEvent.getCreated();
            if (previousCreated.before(startDate)) {
                previousCreated = startDate;
            }

            Date createDate = usageRec.getCreated();
            long duration = (createDate.getTime() - previousCreated.getTime()) + 1;

            createUsageRecord(UsageTypes.VM_SNAPSHOT, duration, previousCreated, createDate, account, volId, zoneId, previousEvent.getDiskOfferingId(), vmId,
                previousEvent.getSize(), usageRec.getVmSnapshotId());
            previousEvent.setProcessed(new Date());
            s_usageVMSnapshotDao.update(previousEvent);

            if (usageRec.getSize() == 0) {
                usageRec.setProcessed(new Date());
                s_usageVMSnapshotDao.update(usageRec);
            } else
                unprocessedUsage.put(key, usageRec);
        }

        for (String key : unprocessedUsage.keySet()) {
            UsageVMSnapshotVO usageRec = unprocessedUsage.get(key);
            Date created = usageRec.getCreated();
            if (created.before(startDate)) {
                created = startDate;
            }
            long duration = (endDate.getTime() - created.getTime()) + 1;
            createUsageRecord(UsageTypes.VM_SNAPSHOT, duration, created, endDate, account, usageRec.getId(), usageRec.getZoneId(), usageRec.getDiskOfferingId(),
                usageRec.getVmId(), usageRec.getSize(), usageRec.getVmSnapshotId());
        }

        return true;
    }

    private static void createUsageRecord(int type, long runningTime, Date startDate, Date endDate, AccountVO account, long volId, long zoneId, Long doId, Long vmId,
                                          long size, Long vmSnapshotId) {
        // Our smallest increment is hourly for now
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Total running time " + runningTime + "ms");
        }

        float usage = runningTime / 1000f / 60f / 60f;

        DecimalFormat dFormat = new DecimalFormat("#.######");
        String usageDisplay = dFormat.format(usage);

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Creating VMSnapshot Id:" + vmSnapshotId + " Volume usage record for vol: " + volId + ", usage: " + usageDisplay + ", startDate: " + startDate + ", endDate: " +
                endDate + ", for account: " + account.getId());
        }

        // Create the usage record
        String usageDesc = "VMSnapshot Id: " + vmSnapshotId + " Usage: " + "VM Id: " + vmId + " Volume Id: " + volId + " ";

        if (doId != null) {
            usageDesc += " DiskOffering: " + doId;
        }

        usageDesc += " Size: " + size;

        UsageVO usageRecord =
            new UsageVO(zoneId, account.getId(), account.getDomainId(), usageDesc, usageDisplay + " Hrs", type, new Double(usage), vmId, null, doId, null, vmSnapshotId, size,
                startDate, endDate);
        s_usageDao.persist(usageRecord);
    }

}
