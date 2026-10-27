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
import com.cloud.usage.dao.UsageNetworksDao;
import com.cloud.user.AccountVO;
import javax.inject.Inject;

import org.apache.cloudstack.usage.UsageTypes;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Component;

import java.text.DecimalFormat;
import java.util.Date;
import java.util.List;

@Component
public class NetworksUsageParser extends UsageParser {
    @Inject
    private UsageNetworksDao networksDao;

    @Override
    public String getParserName() {
        return "Networks";
    }

    @Override
    protected boolean parse(AccountVO account, Date startDate, Date endDate) {
        if ((endDate == null) || endDate.after(new Date())) {
            endDate = new Date();
        }

        final List<UsageNetworksVO> usageNetworksVO = networksDao.getUsageRecords(account.getId(), startDate, endDate);
        if (CollectionUtils.isEmpty(usageNetworksVO)) {
            logger.debug("Cannot find any Networks usage for account [{}] in period between [{}] and [{}].", account, startDate, endDate);
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
            logger.debug("Creating network usage record with id [{}], network offering [{}], usage [{}], startDate [{}], and endDate [{}], for account [{}].",
                    networkId, networkOfferingId, usageDisplay, startDate, endDate, account.getId());

            String description = String.format("Network usage for network ID: %d, network offering: %d", usageNetwork.getNetworkId(), usageNetwork.getNetworkOfferingId());
            UsageVO usageRecord =
                    new UsageVO(zoneId, account.getAccountId(), account.getDomainId(), description, usageDisplay + " Hrs",
                            UsageTypes.NETWORK, (double) usage, null, null, usageNetwork.getNetworkOfferingId(), null, usageNetwork.getNetworkId(),
                            (long)0, null, startDate, endDate);
            usageRecord.setState(usageNetwork.getState());
            usageDao.persist(usageRecord);
        }

        return true;
    }
}
