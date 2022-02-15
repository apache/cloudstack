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

import com.cloud.usage.UsagePortForwardingRuleVO;
import com.cloud.usage.UsageVO;
import com.cloud.usage.dao.UsageDao;
import com.cloud.usage.dao.UsagePortForwardingRuleDao;
import com.cloud.user.AccountVO;
import com.cloud.utils.Pair;

@Component
public class PortForwardingUsageParser {
    public static final Logger s_logger = Logger.getLogger(PortForwardingUsageParser.class.getName());

    private static UsageDao s_usageDao;
    private static UsagePortForwardingRuleDao s_usagePFRuleDao;

    @Inject
    private UsageDao _usageDao;
    @Inject
    private UsagePortForwardingRuleDao _usagePFRuleDao;

    @PostConstruct
    void init() {
        s_usageDao = _usageDao;
        s_usagePFRuleDao = _usagePFRuleDao;
    }

    public static boolean parse(AccountVO account, Date startDate, Date endDate) {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Parsing all PortForwardingRule usage events for account: " + account.getId());
        }
        if ((endDate == null) || endDate.after(new Date())) {
            endDate = new Date();
        }

        // - query usage_volume table with the following criteria:
        //     - look for an entry for accountId with start date in the given range
        //     - look for an entry for accountId with end date in the given range
        //     - look for an entry for accountId with end date null (currently running vm or owned IP)
        //     - look for an entry for accountId with start date before given range *and* end date after given range
        List<UsagePortForwardingRuleVO> usagePFs = s_usagePFRuleDao.getUsageRecords(account.getId(), account.getDomainId(), startDate, endDate, false, 0);

        if (usagePFs.isEmpty()) {
            s_logger.debug("No port forwarding usage events for this period");
            return true;
        }

        // This map has both the running time *and* the usage amount.
        Map<String, Pair<Long, Long>> usageMap = new HashMap<String, Pair<Long, Long>>();
        Map<String, PFInfo> pfMap = new HashMap<String, PFInfo>();

        // loop through all the port forwarding rule, create a usage record for each
        for (UsagePortForwardingRuleVO usagePF : usagePFs) {
            long pfId = usagePF.getPfId();
            String key = "" + pfId;

            pfMap.put(key, new PFInfo(pfId, usagePF.getZoneId()));

            Date pfCreateDate = usagePF.getCreated();
            Date pfDeleteDate = usagePF.getDeleted();

            if ((pfDeleteDate == null) || pfDeleteDate.after(endDate)) {
                pfDeleteDate = endDate;
            }

            // clip the start date to the beginning of our aggregation range if the vm has been running for a while
            if (pfCreateDate.before(startDate)) {
                pfCreateDate = startDate;
            }

            if (pfCreateDate.after(endDate)) {
                //Ignore records created after endDate
                continue;
            }

            long currentDuration = (pfDeleteDate.getTime() - pfCreateDate.getTime()) + 1; // make sure this is an inclusive check for milliseconds (i.e. use n - m + 1 to find total number of millis to charge)

            updatePFUsageData(usageMap, key, usagePF.getPfId(), currentDuration);
        }

        for (String pfIdKey : usageMap.keySet()) {
            Pair<Long, Long> sgtimeInfo = usageMap.get(pfIdKey);
            long useTime = sgtimeInfo.second().longValue();

            // Only create a usage record if we have a runningTime of bigger than zero.
            if (useTime > 0L) {
                PFInfo info = pfMap.get(pfIdKey);
                createUsageRecord(UsageTypes.PORT_FORWARDING_RULE, useTime, startDate, endDate, account, info.getId(), info.getZoneId());
            }
        }

        return true;
    }

    private static void updatePFUsageData(Map<String, Pair<Long, Long>> usageDataMap, String key, long pfId, long duration) {
        Pair<Long, Long> pfUsageInfo = usageDataMap.get(key);
        if (pfUsageInfo == null) {
            pfUsageInfo = new Pair<Long, Long>(new Long(pfId), new Long(duration));
        } else {
            Long runningTime = pfUsageInfo.second();
            runningTime = new Long(runningTime.longValue() + duration);
            pfUsageInfo = new Pair<Long, Long>(pfUsageInfo.first(), runningTime);
        }
        usageDataMap.put(key, pfUsageInfo);
    }

    private static void createUsageRecord(int type, long runningTime, Date startDate, Date endDate, AccountVO account, long pfId, long zoneId) {
        // Our smallest increment is hourly for now
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Total running time " + runningTime + "ms");
        }

        float usage = runningTime / 1000f / 60f / 60f;

        DecimalFormat dFormat = new DecimalFormat("#.######");
        String usageDisplay = dFormat.format(usage);

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Creating usage record for port forwarding rule: " + pfId + ", usage: " + usageDisplay + ", startDate: " + startDate + ", endDate: " +
                endDate + ", for account: " + account.getId());
        }

        // Create the usage record
        String usageDesc = "Port Forwarding Rule: " + pfId + " usage time";

        //ToDo: get zone id
        UsageVO usageRecord =
            new UsageVO(zoneId, account.getId(), account.getDomainId(), usageDesc, usageDisplay + " Hrs", type, new Double(usage), null, null, null, null, pfId, null,
                startDate, endDate);
        s_usageDao.persist(usageRecord);
    }

    private static class PFInfo {
        private long id;
        private long zoneId;

        public PFInfo(long id, long zoneId) {
            this.id = id;
            this.zoneId = zoneId;
        }

        public long getZoneId() {
            return zoneId;
        }

        public long getId() {
            return id;
        }
    }

}
