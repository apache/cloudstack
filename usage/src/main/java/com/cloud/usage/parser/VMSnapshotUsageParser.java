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
import java.util.List;

import javax.inject.Inject;

import com.cloud.usage.UsageManagerImpl;
import com.cloud.utils.DateUtil;
import org.springframework.stereotype.Component;

import org.apache.cloudstack.usage.UsageTypes;

import com.cloud.usage.UsageVMSnapshotVO;
import com.cloud.usage.UsageVO;
import com.cloud.usage.dao.UsageVMSnapshotDao;
import com.cloud.user.AccountVO;

import static com.cloud.utils.NumbersUtil.toHumanReadableSize;

@Component
public class VMSnapshotUsageParser extends UsageParser {
    @Inject
    private UsageVMSnapshotDao usageVMSnapshotDao;

    @Override
    public String getParserName() {
        return "VM Snapshot";
    }

    @Override
    protected boolean parse(AccountVO account, Date startDate, Date endDate) {
        if ((endDate == null) || endDate.after(new Date())) {
            endDate = new Date();
        }

        List<UsageVMSnapshotVO> usageUsageVMSnapshots = usageVMSnapshotDao.getUsageRecords(account.getId(), account.getDomainId(), startDate, endDate);

        if (usageUsageVMSnapshots.isEmpty()) {
            logger.debug("No Instance Snapshot usage events for this period");
            return true;
        }

        for (UsageVMSnapshotVO usageRec : usageUsageVMSnapshots) {
            // Every usage_vmsnapshot row represents one volume of one Instance Snapshot. Instance Snapshots of the same
            // Instance/volume coexist, so each row accrues usage independently, from its creation until it is deleted
            // ("processed" is set by the delete event) or until the end of the aggregation period.
            Date effectiveStart = usageRec.getCreated();
            if (effectiveStart.before(startDate)) {
                effectiveStart = startDate;
            }

            Date effectiveEnd = endDate;
            Date deleted = usageRec.getProcessed();
            if (deleted != null && deleted.before(endDate)) {
                effectiveEnd = deleted;
            }

            if (effectiveEnd.before(effectiveStart)) {
                logger.debug("Skipping Instance Snapshot usage row [{}] as it was deleted before the period [{} - {}].", usageRec, startDate, endDate);
                continue;
            }

            long duration = (effectiveEnd.getTime() - effectiveStart.getTime()) + 1;
            createUsageRecord(UsageTypes.VM_SNAPSHOT, duration, effectiveStart, effectiveEnd, account, usageRec.getVolumeId(), usageRec.getZoneId(),
                    usageRec.getDiskOfferingId(), usageRec.getVmId(), usageRec.getSize(), usageRec.getVmSnapshotId());
        }

        return true;
    }

    private void createUsageRecord(int type, long runningTime, Date startDate, Date endDate, AccountVO account, long volId, long zoneId, Long doId, Long vmId,
                                          long size, Long vmSnapshotId) {
        // Our smallest increment is hourly for now
        logger.debug("Total running time {} ms", runningTime);

        float usage = runningTime / 1000f / 60f / 60f;

        DecimalFormat dFormat = new DecimalFormat("#.######");
        String usageDisplay = dFormat.format(usage);

        logger.debug("Creating usage record for VMSnapshot with id [{}], vol [{}], usage [{}], startDate [{}], and endDate [{}], for account [{}].",
                vmSnapshotId, volId, usageDisplay, DateUtil.displayDateInTimezone(UsageManagerImpl.getUsageAggregationTimeZone(), startDate),
                DateUtil.displayDateInTimezone(UsageManagerImpl.getUsageAggregationTimeZone(), endDate), account.getId());

        // Create the usage record
        String usageDesc = "VMSnapshot Id: " + vmSnapshotId + " Usage: " + "VM Id: " + vmId + " Volume Id: " + volId + " ";

        if (doId != null) {
            usageDesc += " DiskOffering: " + doId;
        }

        usageDesc += " Size: " + toHumanReadableSize(size);

        UsageVO usageRecord =
            new UsageVO(zoneId, account.getId(), account.getDomainId(), usageDesc, usageDisplay + " Hrs", type, new Double(usage), vmId, null, doId, null, vmSnapshotId, size,
                startDate, endDate);
        usageDao.persist(usageRecord);
    }

}
