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

import com.cloud.usage.BucketStatisticsVO;
import com.cloud.usage.UsageVO;
import com.cloud.usage.dao.BucketStatisticsDao;
import com.cloud.user.AccountVO;
import org.apache.cloudstack.usage.UsageTypes;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Component
public class BucketUsageParser extends UsageParser {
    @Inject
    private BucketStatisticsDao bucketStatisticsDao;

    @Override
    public String getParserName() {
        return "Bucket";
    }

    @Override
    protected boolean parse(AccountVO account, Date startDate, Date endDate) {
        if ((endDate == null) || endDate.after(new Date())) {
            endDate = new Date();
        }

        List<BucketStatisticsVO> BucketStatisticsVOs = bucketStatisticsDao.listBy(account.getId());

        List<UsageVO> usageRecords = new ArrayList<>();
        for (BucketStatisticsVO bucketStatistics : BucketStatisticsVOs) {
            long bucketSize = bucketStatistics.getSize();
            if(bucketSize > 0) {
                UsageVO usageRecord =
                        new UsageVO(1L, account.getId(), account.getDomainId(), "Bucket Size", bucketSize + " bytes",
                                UsageTypes.BUCKET, new Double(bucketSize), bucketStatistics.getBucketId(), null, null, startDate, endDate);
                usageRecords.add(usageRecord);
            }
        }

        usageDao.saveUsageRecords(usageRecords);

        return true;
    }
}
