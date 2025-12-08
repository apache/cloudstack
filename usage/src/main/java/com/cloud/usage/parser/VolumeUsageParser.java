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

import com.cloud.usage.UsageVO;
import com.cloud.usage.UsageVolumeVO;
import com.cloud.usage.dao.UsageDao;
import com.cloud.usage.dao.UsageVolumeDao;
import com.cloud.user.AccountVO;
import com.cloud.utils.Pair;

@Component
public class VolumeUsageParser {
    protected static Logger LOGGER = LogManager.getLogger(VolumeUsageParser.class);

    private static UsageDao s_usageDao;
    private static UsageVolumeDao s_usageVolumeDao;

    @Inject
    private UsageDao _usageDao;
    @Inject
    private UsageVolumeDao _usageVolumeDao;

    @PostConstruct
    void init() {
        s_usageDao = _usageDao;
        s_usageVolumeDao = _usageVolumeDao;
    }

    public static boolean parse(AccountVO account, Date startDate, Date endDate) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Parsing all Volume usage events for account: " + account.getId());
        }
        if ((endDate == null) || endDate.after(new Date())) {
            endDate = new Date();
        }

        // - query usage_volume table with the following criteria:
        //     - look for an entry for accountId with start date in the given range
        //     - look for an entry for accountId with end date in the given range
        //     - look for an entry for accountId with end date null (currently running vm or owned IP)
        //     - look for an entry for accountId with start date before given range *and* end date after given range
        List<UsageVolumeVO> usageUsageVols = s_usageVolumeDao.getUsageRecords(account.getId(), account.getDomainId(), startDate, endDate, false, 0);

        if (usageUsageVols.isEmpty()) {
            LOGGER.debug("No volume usage events for this period");
            return true;
        }

        // This map has both the running time *and* the usage amount.
        Map<String, Pair<Long, Long>> usageMap = new HashMap<String, Pair<Long, Long>>();

        Map<String, VolInfo> diskOfferingMap = new HashMap<String, VolInfo>();

        // loop through all the usage volumes, create a usage record for each
        for (UsageVolumeVO usageVol : usageUsageVols) {
            long volId = usageVol.getVolumeId();
            Long doId = usageVol.getDiskOfferingId();
            long zoneId = usageVol.getZoneId();
            Long templateId = usageVol.getTemplateId();
            long size = usageVol.getSize();
            String key = volId + "-" + doId + "-" + size;

            diskOfferingMap.put(key, new VolInfo(volId, zoneId, doId, templateId, size));

            Date volCreateDate = usageVol.getCreated();
            Date volDeleteDate = usageVol.getDeleted();

            if ((volDeleteDate == null) || volDeleteDate.after(endDate)) {
                volDeleteDate = endDate;
            }

            // clip the start date to the beginning of our aggregation range if the vm has been running for a while
            if (volCreateDate.before(startDate)) {
                volCreateDate = startDate;
            }

            if (volCreateDate.after(endDate)) {
                //Ignore records created after endDate
                continue;
            }

            long currentDuration = (volDeleteDate.getTime() - volCreateDate.getTime()) + 1; // make sure this is an inclusive check for milliseconds (i.e. use n - m + 1 to find total number of millis to charge)

            updateVolUsageData(usageMap, key, usageVol.getVolumeId(), currentDuration);
        }

        for (String volIdKey : usageMap.keySet()) {
            Pair<Long, Long> voltimeInfo = usageMap.get(volIdKey);
            long useTime = voltimeInfo.second().longValue();

            // Only create a usage record if we have a runningTime of bigger than zero.
            if (useTime > 0L) {
                VolInfo info = diskOfferingMap.get(volIdKey);
                createUsageRecord(UsageTypes.VOLUME, useTime, startDate, endDate, account, info.getVolumeId(), info.getZoneId(), info.getDiskOfferingId(),
                    info.getTemplateId(), info.getSize());
            }
        }

        return true;
    }

    private static void updateVolUsageData(Map<String, Pair<Long, Long>> usageDataMap, String key, long volId, long duration) {
        Pair<Long, Long> volUsageInfo = usageDataMap.get(key);
        if (volUsageInfo == null) {
            volUsageInfo = new Pair<Long, Long>(new Long(volId), new Long(duration));
        } else {
            Long runningTime = volUsageInfo.second();
            runningTime = new Long(runningTime.longValue() + duration);
            volUsageInfo = new Pair<Long, Long>(volUsageInfo.first(), runningTime);
        }
        usageDataMap.put(key, volUsageInfo);
    }

    private static void createUsageRecord(int type, long runningTime, Date startDate, Date endDate, AccountVO account, long volId, long zoneId, Long doId,
        Long templateId, long size) {
        // Our smallest increment is hourly for now
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Total running time " + runningTime + "ms");
        }

        float usage = runningTime / 1000f / 60f / 60f;

        DecimalFormat dFormat = new DecimalFormat("#.######");
        String usageDisplay = dFormat.format(usage);

        LOGGER.debug("Creating Volume usage record for vol [{}], usage [{}], startDate [{}], and endDate [{}], for account [{}].",
                volId, usageDisplay, DateUtil.displayDateInTimezone(UsageManagerImpl.getUsageAggregationTimeZone(), startDate),
                DateUtil.displayDateInTimezone(UsageManagerImpl.getUsageAggregationTimeZone(), endDate), account.getId());

        // Create the usage record
        String usageDesc = "Volume Id: " + volId + " usage time";

        if (templateId != null) {
            usageDesc += " (Template: " + templateId + ")";
        } else if (doId != null) {
            usageDesc += " (DiskOffering: " + doId + ")";
        }

        UsageVO usageRecord = new UsageVO(zoneId, account.getId(), account.getDomainId(), usageDesc, usageDisplay + " Hrs", type, new Double(usage), null, null, doId, templateId, volId,
                size, startDate, endDate);
        s_usageDao.persist(usageRecord);
    }

    private static class VolInfo {
        private long volId;
        private long zoneId;
        private Long diskOfferingId;
        private Long templateId;
        private long size;

        public VolInfo(long volId, long zoneId, Long diskOfferingId, Long templateId, long size) {
            this.volId = volId;
            this.zoneId = zoneId;
            this.diskOfferingId = diskOfferingId;
            this.templateId = templateId;
            this.size = size;
        }

        public long getZoneId() {
            return zoneId;
        }

        public long getVolumeId() {
            return volId;
        }

        public Long getDiskOfferingId() {
            return diskOfferingId;
        }

        public Long getTemplateId() {
            return templateId;
        }

        public long getSize() {
            return size;
        }
    }
}
