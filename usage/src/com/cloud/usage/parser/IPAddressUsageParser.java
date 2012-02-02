
/**
 * *  Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved
*
 *
 * This software is licensed under the GNU General Public License v3 or later.
 *
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.cloud.usage.parser;

import java.text.DecimalFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.cloud.usage.UsageIPAddressVO;
import com.cloud.usage.UsageServer;
import com.cloud.usage.UsageTypes;
import com.cloud.usage.UsageVO;
import com.cloud.usage.dao.UsageDao;
import com.cloud.usage.dao.UsageIPAddressDao;
import com.cloud.user.AccountVO;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ComponentLocator;

public class IPAddressUsageParser {
    public static final Logger s_logger = Logger.getLogger(IPAddressUsageParser.class.getName());

    private static ComponentLocator _locator = ComponentLocator.getLocator(UsageServer.Name, "usage-components.xml", "log4j-cloud_usage");
    private static UsageDao m_usageDao = _locator.getDao(UsageDao.class);
    private static UsageIPAddressDao m_usageIPAddressDao = _locator.getDao(UsageIPAddressDao.class);


    public static boolean parse(AccountVO account, Date startDate, Date endDate) {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Parsing IP Address usage for account: " + account.getId());
        }
        if ((endDate == null) || endDate.after(new Date())) {
            endDate = new Date();
        }

        // - query usage_ip_address table with the following criteria:
        //     - look for an entry for accountId with start date in the given range
        //     - look for an entry for accountId with end date in the given range
        //     - look for an entry for accountId with end date null (currently running vm or owned IP)
        //     - look for an entry for accountId with start date before given range *and* end date after given range
        List<UsageIPAddressVO> usageIPAddress = m_usageIPAddressDao.getUsageRecords(account.getId(), account.getDomainId(), startDate, endDate);

        if(usageIPAddress.isEmpty()){
            s_logger.debug("No IP Address usage for this period");
            return true;
        }

        // This map has both the running time *and* the usage amount.
        Map<String, Pair<Long, Long>> usageMap = new HashMap<String, Pair<Long, Long>>();

        Map<String, IpInfo> IPMap = new HashMap<String, IpInfo>();

        // loop through all the usage IPs, create a usage record for each
        for (UsageIPAddressVO usageIp : usageIPAddress) {
            long IpId = usageIp.getId();

            String key = ""+IpId;

            // store the info in the IP map
            IPMap.put(key, new IpInfo(usageIp.getZoneId(), IpId, usageIp.getAddress(), usageIp.isSourceNat(), usageIp.isElastic()));

            Date IpAssignDate = usageIp.getAssigned();
            Date IpReleaseDeleteDate = usageIp.getReleased();

            if ((IpReleaseDeleteDate == null) || IpReleaseDeleteDate.after(endDate)) {
                IpReleaseDeleteDate = endDate;
            }

            // clip the start date to the beginning of our aggregation range if the vm has been running for a while
            if (IpAssignDate.before(startDate)) {
                IpAssignDate = startDate;
            }

            long currentDuration = (IpReleaseDeleteDate.getTime() - IpAssignDate.getTime()) + 1; // make sure this is an inclusive check for milliseconds (i.e. use n - m + 1 to find total number of millis to charge)

            updateIpUsageData(usageMap, key, usageIp.getId(), currentDuration);
        }

        for (String ipIdKey : usageMap.keySet()) {
            Pair<Long, Long> ipTimeInfo = usageMap.get(ipIdKey);
            long useTime = ipTimeInfo.second().longValue();

            // Only create a usage record if we have a runningTime of bigger than zero.
            if (useTime > 0L) {
                IpInfo info = IPMap.get(ipIdKey);
                createUsageRecord(info.getZoneId(), useTime, startDate, endDate, account, info.getIpId(), info.getIPAddress(), info.isSourceNat(), info.isElastic);
            }
        }

        return true;
    }

    private static void updateIpUsageData(Map<String, Pair<Long, Long>> usageDataMap, String key, long ipId, long duration) {
        Pair<Long, Long> ipUsageInfo = usageDataMap.get(key);
        if (ipUsageInfo == null) {
            ipUsageInfo = new Pair<Long, Long>(new Long(ipId), new Long(duration));
        } else {
            Long runningTime = ipUsageInfo.second();
            runningTime = new Long(runningTime.longValue() + duration);
            ipUsageInfo = new Pair<Long, Long>(ipUsageInfo.first(), runningTime);
        }
        usageDataMap.put(key, ipUsageInfo);
    }

    private static void createUsageRecord(long zoneId, long runningTime, Date startDate, Date endDate, AccountVO account, long IpId, String IPAddress, boolean isSourceNat, boolean isElastic) {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Total usage time " + runningTime + "ms");
        }

        float usage = runningTime / 1000f / 60f / 60f;

        DecimalFormat dFormat = new DecimalFormat("#.######");
        String usageDisplay = dFormat.format(usage);

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Creating IP usage record with id: " + IpId + ", usage: " + usageDisplay + ", startDate: " + startDate + ", endDate: " + endDate + ", for account: " + account.getId());
        }

        String usageDesc = "IPAddress: "+IPAddress;

        // Create the usage record

        UsageVO usageRecord = new UsageVO(zoneId, account.getAccountId(), account.getDomainId(), usageDesc, usageDisplay + " Hrs", UsageTypes.IP_ADDRESS, new Double(usage), IpId, 
        		(isElastic?1:0), (isSourceNat?"SourceNat":""), startDate, endDate);
        m_usageDao.persist(usageRecord);
    }

    private static class IpInfo {
        private long zoneId;
        private long IpId;
        private String IPAddress;
        private boolean isSourceNat;
        private boolean isElastic;

        public IpInfo(long zoneId,long IpId, String IPAddress, boolean isSourceNat, boolean isElastic) {
            this.zoneId = zoneId;
            this.IpId = IpId;
            this.IPAddress = IPAddress;
            this.isSourceNat = isSourceNat;
            this.isElastic = isElastic;
        }

        public long getZoneId() {
            return zoneId;
        }

        public long getIpId() {
            return IpId;
        }

        public String getIPAddress() {
            return IPAddress;
        }

        public boolean isSourceNat() {
            return isSourceNat;
        }
        
        public boolean isElastic() {
            return isElastic;
        }
    }

}
