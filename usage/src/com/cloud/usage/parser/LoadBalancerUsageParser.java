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

import com.cloud.usage.UsageLoadBalancerPolicyVO;
import com.cloud.usage.UsageVO;
import com.cloud.usage.dao.UsageDao;
import com.cloud.usage.dao.UsageLoadBalancerPolicyDao;
import com.cloud.user.AccountVO;
import com.cloud.utils.Pair;

@Component
public class LoadBalancerUsageParser {
    public static final Logger s_logger = Logger.getLogger(LoadBalancerUsageParser.class.getName());

    private static UsageDao s_usageDao;
    private static UsageLoadBalancerPolicyDao s_usageLoadBalancerPolicyDao;

    @Inject
    private UsageDao _usageDao;
    @Inject
    private UsageLoadBalancerPolicyDao _usageLoadBalancerPolicyDao;

    @PostConstruct
    void init() {
        s_usageDao = _usageDao;
        s_usageLoadBalancerPolicyDao = _usageLoadBalancerPolicyDao;
    }

    public static boolean parse(AccountVO account, Date startDate, Date endDate) {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Parsing all LoadBalancerPolicy usage events for account: " + account.getId());
        }
        if ((endDate == null) || endDate.after(new Date())) {
            endDate = new Date();
        }

        // - query usage_volume table with the following criteria:
        //     - look for an entry for accountId with start date in the given range
        //     - look for an entry for accountId with end date in the given range
        //     - look for an entry for accountId with end date null (currently running vm or owned IP)
        //     - look for an entry for accountId with start date before given range *and* end date after given range
        List<UsageLoadBalancerPolicyVO> usageLBs = s_usageLoadBalancerPolicyDao.getUsageRecords(account.getId(), account.getDomainId(), startDate, endDate, false, 0);

        if (usageLBs.isEmpty()) {
            s_logger.debug("No load balancer usage events for this period");
            return true;
        }

        // This map has both the running time *and* the usage amount.
        Map<String, Pair<Long, Long>> usageMap = new HashMap<String, Pair<Long, Long>>();
        Map<String, LBInfo> lbMap = new HashMap<String, LBInfo>();

        // loop through all the load balancer policies, create a usage record for each
        for (UsageLoadBalancerPolicyVO usageLB : usageLBs) {
            long lbId = usageLB.getId();
            String key = "" + lbId;

            lbMap.put(key, new LBInfo(lbId, usageLB.getZoneId()));

            Date lbCreateDate = usageLB.getCreated();
            Date lbDeleteDate = usageLB.getDeleted();

            if ((lbDeleteDate == null) || lbDeleteDate.after(endDate)) {
                lbDeleteDate = endDate;
            }

            // clip the start date to the beginning of our aggregation range if the vm has been running for a while
            if (lbCreateDate.before(startDate)) {
                lbCreateDate = startDate;
            }

            if (lbCreateDate.after(endDate)) {
                //Ignore records created after endDate
                continue;
            }

            long currentDuration = (lbDeleteDate.getTime() - lbCreateDate.getTime()) + 1; // make sure this is an inclusive check for milliseconds (i.e. use n - m + 1 to find total number of millis to charge)

            updateLBUsageData(usageMap, key, usageLB.getId(), currentDuration);
        }

        for (String lbIdKey : usageMap.keySet()) {
            Pair<Long, Long> sgtimeInfo = usageMap.get(lbIdKey);
            long useTime = sgtimeInfo.second().longValue();

            // Only create a usage record if we have a runningTime of bigger than zero.
            if (useTime > 0L) {
                LBInfo info = lbMap.get(lbIdKey);
                createUsageRecord(UsageTypes.LOAD_BALANCER_POLICY, useTime, startDate, endDate, account, info.getId(), info.getZoneId());
            }
        }

        return true;
    }

    private static void updateLBUsageData(Map<String, Pair<Long, Long>> usageDataMap, String key, long lbId, long duration) {
        Pair<Long, Long> lbUsageInfo = usageDataMap.get(key);
        if (lbUsageInfo == null) {
            lbUsageInfo = new Pair<Long, Long>(new Long(lbId), new Long(duration));
        } else {
            Long runningTime = lbUsageInfo.second();
            runningTime = new Long(runningTime.longValue() + duration);
            lbUsageInfo = new Pair<Long, Long>(lbUsageInfo.first(), runningTime);
        }
        usageDataMap.put(key, lbUsageInfo);
    }

    private static void createUsageRecord(int type, long runningTime, Date startDate, Date endDate, AccountVO account, long lbId, long zoneId) {
        // Our smallest increment is hourly for now
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Total running time " + runningTime + "ms");
        }

        float usage = runningTime / 1000f / 60f / 60f;

        DecimalFormat dFormat = new DecimalFormat("#.######");
        String usageDisplay = dFormat.format(usage);

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Creating Volume usage record for load balancer: " + lbId + ", usage: " + usageDisplay + ", startDate: " + startDate + ", endDate: " +
                endDate + ", for account: " + account.getId());
        }

        // Create the usage record
        String usageDesc = "Load Balancing Policy: " + lbId + " usage time";

        //ToDo: get zone id
        UsageVO usageRecord =
            new UsageVO(zoneId, account.getId(), account.getDomainId(), usageDesc, usageDisplay + " Hrs", type, new Double(usage), null, null, null, null, lbId, null,
                startDate, endDate);
        s_usageDao.persist(usageRecord);
    }

    private static class LBInfo {
        private long id;
        private long zoneId;

        public LBInfo(long id, long zoneId) {
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
