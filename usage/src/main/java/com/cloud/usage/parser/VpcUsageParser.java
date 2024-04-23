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

import com.cloud.usage.UsageVpcVO;
import com.cloud.usage.dao.UsageDao;
import com.cloud.usage.UsageVO;
import com.cloud.usage.dao.UsageVpcDao;
import com.cloud.user.AccountVO;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import org.apache.cloudstack.usage.UsageTypes;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.springframework.stereotype.Component;

import java.text.DecimalFormat;
import java.util.Date;
import java.util.List;

@Component
public class VpcUsageParser {
    private static final Logger LOGGER = LogManager.getLogger(VpcUsageParser.class.getName());

    @Inject
    private UsageVpcDao vpcDao;
    @Inject
    private UsageDao usageDao;

    private static UsageDao s_usageDao;
    private static UsageVpcDao s_usageVpcDao;
    @PostConstruct
    void init() {
        s_usageDao = usageDao;
        s_usageVpcDao = vpcDao;
    }

    public static boolean parse(AccountVO account, Date startDate, Date endDate) {
        LOGGER.debug(String.format("Parsing all VPC usage events for account [%s].", account.getId()));
        if ((endDate == null) || endDate.after(new Date())) {
            endDate = new Date();
        }

        final List<UsageVpcVO> usageVPCs = s_usageVpcDao.getUsageRecords(account.getId(), startDate, endDate);
        if (usageVPCs == null || usageVPCs.isEmpty()) {
            LOGGER.debug(String.format("Cannot find any VPC usage for account [%s] in period between [%s] and [%s].", account, startDate, endDate));
            return true;
        }

        for (final UsageVpcVO usageVPC : usageVPCs) {
            Long zoneId = usageVPC.getZoneId();
            Date createdDate = usageVPC.getCreated();
            Date removedDate = usageVPC.getRemoved();
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

            long vpcId = usageVPC.getVpcId();
            LOGGER.debug(String.format("Creating VPC usage record with id [%s], usage [%s], startDate [%s], and endDate [%s], for account [%s].",
                    vpcId, usageDisplay, startDate, endDate, account.getId()));

            String description = String.format("VPC usage for VPC ID: %d", usageVPC.getVpcId());
            UsageVO usageRecord =
                    new UsageVO(zoneId, account.getAccountId(), account.getDomainId(), description, usageDisplay + " Hrs",
                            UsageTypes.VPC, (double) usage, null, null, null, null, usageVPC.getVpcId(),
                            (long)0, null, startDate, endDate);
            s_usageDao.persist(usageRecord);
        }

        return true;
    }
}
