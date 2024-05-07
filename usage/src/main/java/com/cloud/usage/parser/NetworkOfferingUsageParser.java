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

import com.cloud.usage.UsageNetworkOfferingVO;
import com.cloud.usage.UsageVO;
import com.cloud.usage.dao.UsageDao;
import com.cloud.usage.dao.UsageNetworkOfferingDao;
import com.cloud.user.AccountVO;
import com.cloud.utils.Pair;

@Component
public class NetworkOfferingUsageParser {
    protected static Logger LOGGER = LogManager.getLogger(NetworkOfferingUsageParser.class);

    private static UsageDao s_usageDao;
    private static UsageNetworkOfferingDao s_usageNetworkOfferingDao;

    @Inject
    private UsageDao _usageDao;
    @Inject
    private UsageNetworkOfferingDao _usageNetworkOfferingDao;

    @PostConstruct
    void init() {
        s_usageDao = _usageDao;
        s_usageNetworkOfferingDao = _usageNetworkOfferingDao;
    }

    public static boolean parse(AccountVO account, Date startDate, Date endDate) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Parsing all NetworkOffering usage events for account: " + account.getId());
        }
        if ((endDate == null) || endDate.after(new Date())) {
            endDate = new Date();
        }

        // - query usage_volume table with the following criteria:
        //     - look for an entry for accountId with start date in the given range
        //     - look for an entry for accountId with end date in the given range
        //     - look for an entry for accountId with end date null (currently running vm or owned IP)
        //     - look for an entry for accountId with start date before given range *and* end date after given range
        List<UsageNetworkOfferingVO> usageNOs = s_usageNetworkOfferingDao.getUsageRecords(account.getId(), account.getDomainId(), startDate, endDate, false, 0);

        if (usageNOs.isEmpty()) {
            LOGGER.debug("No NetworkOffering usage events for this period");
            return true;
        }

        // This map has both the running time *and* the usage amount.
        Map<String, Pair<Long, Long>> usageMap = new HashMap<String, Pair<Long, Long>>();
        Map<String, NOInfo> noMap = new HashMap<String, NOInfo>();

        // loop through all the network offerings, create a usage record for each
        for (UsageNetworkOfferingVO usageNO : usageNOs) {
            long vmId = usageNO.getVmInstanceId();
            long noId = usageNO.getNetworkOfferingId();
            String key = "" + vmId + "NO" + noId;

            noMap.put(key, new NOInfo(vmId, usageNO.getZoneId(), noId, usageNO.isDefault()));

            Date noCreateDate = usageNO.getCreated();
            Date noDeleteDate = usageNO.getDeleted();

            if ((noDeleteDate == null) || noDeleteDate.after(endDate)) {
                noDeleteDate = endDate;
            }

            // clip the start date to the beginning of our aggregation range if the vm has been running for a while
            if (noCreateDate.before(startDate)) {
                noCreateDate = startDate;
            }

            if (noCreateDate.after(endDate)) {
                //Ignore records created after endDate
                continue;
            }

            long currentDuration = (noDeleteDate.getTime() - noCreateDate.getTime()) + 1; // make sure this is an inclusive check for milliseconds (i.e. use n - m + 1 to find total number of millis to charge)

            updateNOUsageData(usageMap, key, usageNO.getVmInstanceId(), currentDuration);
        }

        for (String noIdKey : usageMap.keySet()) {
            Pair<Long, Long> notimeInfo = usageMap.get(noIdKey);
            long useTime = notimeInfo.second().longValue();

            // Only create a usage record if we have a runningTime of bigger than zero.
            if (useTime > 0L) {
                NOInfo info = noMap.get(noIdKey);
                createUsageRecord(UsageTypes.NETWORK_OFFERING, useTime, startDate, endDate, account, info.getVmId(), info.getNOId(), info.getZoneId(), info.isDefault());
            }
        }

        return true;
    }

    private static void updateNOUsageData(Map<String, Pair<Long, Long>> usageDataMap, String key, long vmId, long duration) {
        Pair<Long, Long> noUsageInfo = usageDataMap.get(key);
        if (noUsageInfo == null) {
            noUsageInfo = new Pair<Long, Long>(new Long(vmId), new Long(duration));
        } else {
            Long runningTime = noUsageInfo.second();
            runningTime = new Long(runningTime.longValue() + duration);
            noUsageInfo = new Pair<Long, Long>(noUsageInfo.first(), runningTime);
        }
        usageDataMap.put(key, noUsageInfo);
    }

    private static void createUsageRecord(int type, long runningTime, Date startDate, Date endDate, AccountVO account, long vmId, long noId, long zoneId,
        boolean isDefault) {
        // Our smallest increment is hourly for now
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Total running time " + runningTime + "ms");
        }

        float usage = runningTime / 1000f / 60f / 60f;

        DecimalFormat dFormat = new DecimalFormat("#.######");
        String usageDisplay = dFormat.format(usage);

        LOGGER.debug("Creating network offering usage record for id [{}], vm [{}], usage [{}], startDate [{}], and endDate [{}], for account [{}].",
                noId, vmId, usageDisplay, DateUtil.displayDateInTimezone(UsageManagerImpl.getUsageAggregationTimeZone(), startDate),
                DateUtil.displayDateInTimezone(UsageManagerImpl.getUsageAggregationTimeZone(), endDate), account.getId());

        // Create the usage record
        String usageDesc = "Network offering:" + noId + " for Vm : " + vmId + " usage time";

        long defaultNic = (isDefault) ? 1 : 0;
        UsageVO usageRecord =
            new UsageVO(zoneId, account.getId(), account.getDomainId(), usageDesc, usageDisplay + " Hrs", type, new Double(usage), vmId, null, noId, null, defaultNic,
                null, startDate, endDate);
        s_usageDao.persist(usageRecord);
    }

    private static class NOInfo {
        private final long vmId;
        private final long zoneId;
        private final long noId;
        private final boolean isDefault;

        public NOInfo(long vmId, long zoneId, long noId, boolean isDefault) {
            this.vmId = vmId;
            this.zoneId = zoneId;
            this.noId = noId;
            this.isDefault = isDefault;
        }

        public long getZoneId() {
            return zoneId;
        }

        public long getVmId() {
            return vmId;
        }

        public long getNOId() {
            return noId;
        }

        public boolean isDefault() {
            return isDefault;
        }
    }

}
