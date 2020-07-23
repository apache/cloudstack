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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import org.apache.cloudstack.usage.UsageTypes;

import com.cloud.usage.UsageNetworkVO;
import com.cloud.usage.UsageVO;
import com.cloud.usage.dao.UsageDao;
import com.cloud.usage.dao.UsageNetworkDao;
import com.cloud.user.AccountVO;
import com.cloud.utils.db.SearchCriteria;

import static com.cloud.utils.NumbersUtil.toHumanReadableSize;

@Component
public class NetworkUsageParser {
    public static final Logger s_logger = Logger.getLogger(NetworkUsageParser.class.getName());

    private static UsageDao s_usageDao;
    private static UsageNetworkDao s_usageNetworkDao;

    @Inject
    private UsageDao _usageDao;
    @Inject
    private UsageNetworkDao _usageNetworkDao;

    @PostConstruct
    void init() {
        s_usageDao = _usageDao;
        s_usageNetworkDao = _usageNetworkDao;
    }

    public static boolean parse(AccountVO account, Date startDate, Date endDate) {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Parsing all Network usage events for account: " + account.getId());
        }

        if ((endDate == null) || endDate.after(new Date())) {
            endDate = new Date();
        }

        // - query usage_network table for all entries for userId with
        // event_date in the given range
        SearchCriteria<UsageNetworkVO> sc = s_usageNetworkDao.createSearchCriteria();
        sc.addAnd("accountId", SearchCriteria.Op.EQ, account.getId());
        sc.addAnd("eventTimeMillis", SearchCriteria.Op.BETWEEN, startDate.getTime(), endDate.getTime());
        List<UsageNetworkVO> usageNetworkVOs = s_usageNetworkDao.search(sc, null);

        Map<String, NetworkInfo> networkUsageByZone = new HashMap<String, NetworkInfo>();

        // Calculate the total bytes since last parsing
        for (UsageNetworkVO usageNetwork : usageNetworkVOs) {
            long zoneId = usageNetwork.getZoneId();
            String key = "" + zoneId;
            if (usageNetwork.getHostId() != 0) {
                key += "-Host" + usageNetwork.getHostId();
            }
            NetworkInfo networkInfo = networkUsageByZone.get(key);

            long bytesSent = usageNetwork.getBytesSent();
            long bytesReceived = usageNetwork.getBytesReceived();
            if (networkInfo != null) {
                bytesSent += networkInfo.getBytesSent();
                bytesReceived += networkInfo.getBytesRcvd();
            }

            networkUsageByZone.put(key, new NetworkInfo(zoneId, usageNetwork.getHostId(), usageNetwork.getHostType(), usageNetwork.getNetworkId(), bytesSent,
                bytesReceived));
        }

        List<UsageVO> usageRecords = new ArrayList<UsageVO>();
        for (String key : networkUsageByZone.keySet()) {
            NetworkInfo networkInfo = networkUsageByZone.get(key);
            long totalBytesSent = networkInfo.getBytesSent();
            long totalBytesReceived = networkInfo.getBytesRcvd();

            if ((totalBytesSent > 0L) || (totalBytesReceived > 0L)) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Creating usage record, total bytes sent: " + toHumanReadableSize(totalBytesSent) + ", total bytes received: " + toHumanReadableSize(totalBytesReceived) + " for account: " +
                        account.getId() + " in availability zone " + networkInfo.getZoneId() + ", start: " + startDate + ", end: " + endDate);
                }

                Long hostId = null;

                // Create the usage record for bytes sent
                String usageDesc = "network bytes sent";
                if (networkInfo.getHostId() != 0) {
                    hostId = networkInfo.getHostId();
                    usageDesc += " for Host: " + networkInfo.getHostId();
                }
                UsageVO usageRecord =
                    new UsageVO(networkInfo.getZoneId(), account.getId(), account.getDomainId(), usageDesc, totalBytesSent + " bytes sent",
                        UsageTypes.NETWORK_BYTES_SENT, new Double(totalBytesSent), hostId, networkInfo.getHostType(), networkInfo.getNetworkId(), startDate, endDate);
                usageRecords.add(usageRecord);

                // Create the usage record for bytes received
                usageDesc = "network bytes received";
                if (networkInfo.getHostId() != 0) {
                    usageDesc += " for Host: " + networkInfo.getHostId();
                }
                usageRecord =
                    new UsageVO(networkInfo.getZoneId(), account.getId(), account.getDomainId(), usageDesc, totalBytesReceived + " bytes received",
                        UsageTypes.NETWORK_BYTES_RECEIVED, new Double(totalBytesReceived), hostId, networkInfo.getHostType(), networkInfo.getNetworkId(), startDate,
                        endDate);
                usageRecords.add(usageRecord);
            } else {
                // Don't charge anything if there were zero bytes processed
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("No usage record (0 bytes used) generated for account: " + account.getId());
                }
            }
        }

        s_usageDao.saveUsageRecords(usageRecords);

        return true;
    }

    private static class NetworkInfo {
        private long zoneId;
        private long hostId;
        private String hostType;
        private Long networkId;
        private long bytesSent;
        private long bytesRcvd;

        public NetworkInfo(long zoneId, long hostId, String hostType, Long networkId, long bytesSent, long bytesRcvd) {
            this.zoneId = zoneId;
            this.hostId = hostId;
            this.hostType = hostType;
            this.networkId = networkId;
            this.bytesSent = bytesSent;
            this.bytesRcvd = bytesRcvd;
        }

        public long getZoneId() {
            return zoneId;
        }

        public long getHostId() {
            return hostId;
        }

        public Long getNetworkId() {
            return networkId;
        }

        public long getBytesSent() {
            return bytesSent;
        }

        public long getBytesRcvd() {
            return bytesRcvd;
        }

        public String getHostType() {
            return hostType;
        }

    }
}
