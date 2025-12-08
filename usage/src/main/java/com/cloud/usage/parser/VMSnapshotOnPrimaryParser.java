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

import com.cloud.usage.UsageManagerImpl;
import com.cloud.utils.DateUtil;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.springframework.stereotype.Component;

import org.apache.cloudstack.usage.UsageTypes;

import com.cloud.usage.UsageSnapshotOnPrimaryVO;
import com.cloud.usage.UsageVO;
import com.cloud.usage.dao.UsageDao;
import com.cloud.usage.dao.UsageVMSnapshotOnPrimaryDao;
import com.cloud.user.AccountVO;

import static com.cloud.utils.NumbersUtil.toHumanReadableSize;

@Component
public class VMSnapshotOnPrimaryParser {
    protected static Logger LOGGER = LogManager.getLogger(VMSnapshotOnPrimaryParser.class);

    private static UsageDao s_usageDao;
    private static UsageVMSnapshotOnPrimaryDao s_usageSnapshotOnPrimaryDao;

    @Inject
    private UsageDao _usageDao;
    @Inject
    private UsageVMSnapshotOnPrimaryDao _usageSnapshotOnPrimaryDao;

    @PostConstruct
    void init() {
        s_usageDao = _usageDao;
        s_usageSnapshotOnPrimaryDao = _usageSnapshotOnPrimaryDao;
    }

    public static boolean parse(AccountVO account, Date startDate, Date endDate) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Parsing all VmSnapshot on primary usage events for account: " + account.getId());
        }
        if ((endDate == null) || endDate.after(new Date())) {
            endDate = new Date();
        }

        List<UsageSnapshotOnPrimaryVO> usageUsageVMSnapshots = s_usageSnapshotOnPrimaryDao.getUsageRecords(account.getId(), account.getDomainId(), startDate, endDate);

        if (usageUsageVMSnapshots.isEmpty()) {
            LOGGER.debug("No VM snapshot on primary usage events for this period");
            return true;
        }

        Map<String, UsageSnapshotOnPrimaryVO> unprocessedUsage = new HashMap<String, UsageSnapshotOnPrimaryVO>();
        for (UsageSnapshotOnPrimaryVO usageRec : usageUsageVMSnapshots) {
            LOGGER.debug("usageRec for VMsnap on primary " + usageRec.toString());
            String key = usageRec.getName();
            if (usageRec.getPhysicalSize() == 0) {
                usageRec.setDeleted(new Date());
                s_usageSnapshotOnPrimaryDao.updateDeleted(usageRec);
            } else {
                unprocessedUsage.put(key, usageRec);
            }
        }

        for (String key : unprocessedUsage.keySet()) {
            UsageSnapshotOnPrimaryVO usageRec = unprocessedUsage.get(key);
            Date created = usageRec.getCreated();
            if (created.before(startDate)) {
                created = startDate;
            }
            Date endDateEffective = endDate;
            if (usageRec.getDeleted() != null && usageRec.getDeleted().before(endDate)){
                endDateEffective = usageRec.getDeleted();
                LOGGER.debug("Remoevd vm snapshot found endDateEffective " + endDateEffective + " period end data " + endDate);
            }
            long duration = (endDateEffective.getTime() - created.getTime()) + 1;
            createUsageRecord(UsageTypes.VM_SNAPSHOT_ON_PRIMARY, duration, created, endDateEffective, account, usageRec.getVolumeId(), usageRec.getName(), usageRec.getZoneId(),
                    usageRec.getVirtualSize(), usageRec.getPhysicalSize(), usageRec.getVmSnapshotId());
        }

        return true;
    }

    private static void createUsageRecord(int usageType, long runningTime, Date startDate, Date endDate, AccountVO account, long vmId, String name, long zoneId, long virtualSize,
                                          long physicalSize, Long vmSnapshotId) {
        // Our smallest increment is hourly for now
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Total running time " + runningTime + "ms");
        }

        float usage = runningTime / 1000f / 60f / 60f;

        DecimalFormat dFormat = new DecimalFormat("#.######");
        String usageDisplay = dFormat.format(usage);

        LOGGER.debug("Creating usage record for VMSnapshot with id [{}] in primary, vm [{}], usage [{}], startDate [{}], and endDate [{}], for account [{}].",
                vmSnapshotId, vmId, usageDisplay, DateUtil.displayDateInTimezone(UsageManagerImpl.getUsageAggregationTimeZone(), startDate),
                DateUtil.displayDateInTimezone(UsageManagerImpl.getUsageAggregationTimeZone(), endDate), account.getId());

        // Create the usage record
        String usageDesc = "VMSnapshot Id: " + vmSnapshotId + " On Primary Usage: VM Id: " + vmId;
        usageDesc += " Size: " + toHumanReadableSize(virtualSize);

        UsageVO usageRecord = new UsageVO(zoneId, account.getId(), account.getDomainId(), usageDesc, usageDisplay + " Hrs", usageType, new Double(usage), vmId, name, null, null,
                vmSnapshotId, physicalSize, virtualSize, startDate, endDate);
        s_usageDao.persist(usageRecord);
    }

}
