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
import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

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

        for (final UsageBackupVO usageBackup : usageBackups) {
            final Long vmId = usageBackup.getVmId();
            final Long zoneId = usageBackup.getZoneId();
            final Long offeringId = usageBackup.getBackupOfferingId();
            Date createdDate = usageBackup.getCreated();
            Date removedDate = usageBackup.getRemoved();
            if (createdDate.before(startDate)) {
                createdDate = startDate;
            }
            if (removedDate == null || removedDate.after(endDate)) {
                removedDate = endDate;
            }
            final long duration = (removedDate.getTime() - createdDate.getTime()) + 1;
            final float usage = duration / 1000f / 60f / 60f;
            DecimalFormat dFormat = new DecimalFormat("#.######");
            String usageDisplay = dFormat.format(usage);

            final Double rawUsage = (double) usageBackup.getSize();
            final String description = String.format("Backup usage VM ID: %d, backup offering: %d", vmId, offeringId);

            final UsageVO usageRecord =
                    new UsageVO(zoneId, account.getAccountId(), account.getDomainId(), description, usageDisplay + " Hrs",
                            UsageTypes.BACKUP, new Double(usage), vmId, null, offeringId, null, vmId,
                            usageBackup.getSize(), usageBackup.getProtectedSize(), startDate, endDate);
            s_usageDao.persist(usageRecord);
        }

        return true;
    }
}
