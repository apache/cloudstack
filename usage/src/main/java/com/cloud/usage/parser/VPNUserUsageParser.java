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

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.springframework.stereotype.Component;

import org.apache.cloudstack.usage.UsageTypes;

import com.cloud.usage.UsageVO;
import com.cloud.usage.UsageVPNUserVO;
import com.cloud.usage.dao.UsageDao;
import com.cloud.usage.dao.UsageVPNUserDao;
import com.cloud.user.AccountVO;
import com.cloud.utils.Pair;

@Component
public class VPNUserUsageParser {
    protected static Logger LOGGER = LogManager.getLogger(VPNUserUsageParser.class);

    private static UsageDao s_usageDao;
    private static UsageVPNUserDao s_usageVPNUserDao;

    @Inject
    private UsageDao _usageDao;
    @Inject
    private UsageVPNUserDao _usageVPNUserDao;

    @PostConstruct
    void init() {
        s_usageDao = _usageDao;
        s_usageVPNUserDao = _usageVPNUserDao;
    }

    public static boolean parse(AccountVO account, Date startDate, Date endDate) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Parsing all VPN user usage events for account: " + account.getId());
        }
        if ((endDate == null) || endDate.after(new Date())) {
            endDate = new Date();
        }

        List<UsageVPNUserVO> usageVUs = s_usageVPNUserDao.getUsageRecords(account.getId(), account.getDomainId(), startDate, endDate, false, 0);

        if (usageVUs.isEmpty()) {
            LOGGER.debug("No VPN user usage events for this period");
            return true;
        }

        // This map has both the running time *and* the usage amount.
        Map<String, Pair<Long, Long>> usageMap = new HashMap<String, Pair<Long, Long>>();
        Map<String, VUInfo> vuMap = new HashMap<String, VUInfo>();

        // loop through all the VPN user usage, create a usage record for each
        for (UsageVPNUserVO usageVU : usageVUs) {
            long userId = usageVU.getUserId();
            String userName = usageVU.getUsername();
            String key = "" + userId + "VU" + userName;

            vuMap.put(key, new VUInfo(userId, usageVU.getZoneId(), userName));

            Date vuCreateDate = usageVU.getCreated();
            Date vuDeleteDate = usageVU.getDeleted();

            if ((vuDeleteDate == null) || vuDeleteDate.after(endDate)) {
                vuDeleteDate = endDate;
            }

            // clip the start date to the beginning of our aggregation range if the vm has been running for a while
            if (vuCreateDate.before(startDate)) {
                vuCreateDate = startDate;
            }

            if (vuCreateDate.after(endDate)) {
                //Ignore records created after endDate
                continue;
            }

            long currentDuration = (vuDeleteDate.getTime() - vuCreateDate.getTime()) + 1; // make sure this is an inclusive check for milliseconds (i.e. use n - m + 1 to find total number of millis to charge)

            updateVUUsageData(usageMap, key, usageVU.getUserId(), currentDuration);
        }

        for (String vuIdKey : usageMap.keySet()) {
            Pair<Long, Long> vutimeInfo = usageMap.get(vuIdKey);
            long useTime = vutimeInfo.second().longValue();

            // Only create a usage record if we have a runningTime of bigger than zero.
            if (useTime > 0L) {
                VUInfo info = vuMap.get(vuIdKey);
                createUsageRecord(UsageTypes.VPN_USERS, useTime, startDate, endDate, account, info.getUserId(), info.getUserName(), info.getZoneId());
            }
        }

        return true;
    }

    private static void updateVUUsageData(Map<String, Pair<Long, Long>> usageDataMap, String key, long userId, long duration) {
        Pair<Long, Long> vuUsageInfo = usageDataMap.get(key);
        if (vuUsageInfo == null) {
            vuUsageInfo = new Pair<Long, Long>(new Long(userId), new Long(duration));
        } else {
            Long runningTime = vuUsageInfo.second();
            runningTime = new Long(runningTime.longValue() + duration);
            vuUsageInfo = new Pair<Long, Long>(vuUsageInfo.first(), runningTime);
        }
        usageDataMap.put(key, vuUsageInfo);
    }

    private static void createUsageRecord(int type, long runningTime, Date startDate, Date endDate, AccountVO account, long userId, String userName, long zoneId) {
        // Our smallest increment is hourly for now
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Total running time " + runningTime + "ms");
        }

        float usage = runningTime / 1000f / 60f / 60f;

        DecimalFormat dFormat = new DecimalFormat("#.######");
        String usageDisplay = dFormat.format(usage);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Creating VPN user:" + userId + " usage record, usage: " + usageDisplay + ", startDate: " + startDate + ", endDate: " + endDate +
                ", for account: " + account.getId());
        }

        // Create the usage record
        String usageDesc = "VPN User: " + userName + ", Id: " + userId + " usage time";

        UsageVO usageRecord =
            new UsageVO(zoneId, account.getId(), account.getDomainId(), usageDesc, usageDisplay + " Hrs", type, new Double(usage), null, null, null, null, userId, null,
                startDate, endDate);
        s_usageDao.persist(usageRecord);
    }

    private static class VUInfo {
        private long userId;
        private long zoneId;
        private String userName;

        public VUInfo(long userId, long zoneId, String userName) {
            this.userId = userId;
            this.zoneId = zoneId;
            this.userName = userName;
        }

        public long getZoneId() {
            return zoneId;
        }

        public long getUserId() {
            return userId;
        }

        public String getUserName() {
            return userName;
        }
    }

}
