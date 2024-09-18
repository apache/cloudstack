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

import com.cloud.usage.UsageNetworksVO;
import com.cloud.usage.UsageVO;
import com.cloud.usage.dao.UsageDao;
import com.cloud.usage.dao.UsageNetworksDao;
import com.cloud.user.AccountVO;
import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.cloudstack.usage.UsageTypes;
import org.apache.commons.collections.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import java.text.DecimalFormat;
import java.util.Date;
import java.util.List;

@Component
public class NetworksUsageParser {
    private static final Logger LOGGER = LogManager.getLogger(NetworksUsageParser.class.getName());

    @Inject
    private UsageNetworksDao networksDao;
    @Inject
    private UsageDao usageDao;

    private static UsageDao staticUsageDao;
    private static UsageNetworksDao staticNetworksDao;

    @PostConstruct
    void init() {
        staticUsageDao = usageDao;
        staticNetworksDao = networksDao;
    }

    public static boolean parse(AccountVO account, Date startDate, Date endDate) {
        LOGGER.debug(String.format("Parsing all networks usage events for account [%s].", account.getId()));
        if ((endDate == null) || endDate.after(new Date())) {
            endDate = new Date();
        }

        final List<UsageNetworksVO> usageNetworksVO = staticNetworksDao.getUsageRecords(account.getId(), startDate, endDate);
        if (CollectionUtils.isEmpty(usageNetworksVO)) {
            LOGGER.debug(String.format("Cannot find any VPC usage for account [%s] in period between [%s] and [%s].", account, startDate, endDate));
            return true;
        }

        for (final UsageNetworksVO usageNetwork : usageNetworksVO) {
            Long zoneId = usageNetwork.getZoneId();
            Date createdDate = usageNetwork.getCreated();
            Date removedDate = usageNetwork.getRemoved();
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

            long networkId = usageNetwork.getNetworkId();
            long networkOfferingId = usageNetwork.getNetworkOfferingId();
            LOGGER.debug(String.format("Creating network usage record with id [%s], network offering [%s], usage [%s], startDate [%s], and endDate [%s], for account [%s].",
                    networkId, networkOfferingId, usageDisplay, startDate, endDate, account.getId()));

            String description = String.format("Network usage for network ID: %d, network offering: %d", usageNetwork.getNetworkId(), usageNetwork.getNetworkOfferingId());
            UsageVO usageRecord =
                    new UsageVO(zoneId, account.getAccountId(), account.getDomainId(), description, usageDisplay + " Hrs",
                            UsageTypes.NETWORK, (double) usage, null, null, usageNetwork.getNetworkOfferingId(), null, usageNetwork.getNetworkId(),
                            (long)0, null, startDate, endDate);
            usageRecord.setState(usageNetwork.getState());
            staticUsageDao.persist(usageRecord);
        }

        return true;
    }
}
