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
package com.cloud.usage.dao;

import com.cloud.usage.UsageVO;
import com.cloud.user.AccountVO;
import com.cloud.user.UserStatisticsVO;
import com.cloud.user.VmDiskStatisticsVO;
import com.cloud.utils.Pair;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDao;
import com.cloud.utils.db.SearchCriteria;

import java.util.Date;
import java.util.List;

public interface UsageDao extends GenericDao<UsageVO, Long> {
    void deleteRecordsForAccount(Long accountId);

    Pair<List<UsageVO>, Integer> searchAndCountAllRecords(SearchCriteria<UsageVO> sc, Filter filter);

    void saveAccounts(List<AccountVO> accounts);

    void updateAccounts(List<AccountVO> accounts);

    void saveUserStats(List<UserStatisticsVO> userStats);

    void updateUserStats(List<UserStatisticsVO> userStats);

    Long getLastAccountId();

    Long getLastUserStatsId();

    List<Long> listPublicTemplatesByAccount(long accountId);

    Long getLastVmDiskStatsId();

    void updateVmDiskStats(List<VmDiskStatisticsVO> vmDiskStats);

    void saveVmDiskStats(List<VmDiskStatisticsVO> vmDiskStats);

    void saveUsageRecords(List<UsageVO> usageRecords);

    void removeOldUsageRecords(int days);

    UsageVO persistUsage(final UsageVO usage);

    Pair<List<UsageVO>, Integer> listUsageRecordsPendingForQuotaAggregation(long accountId, long domainId);

    List<Pair<String, String>> listAccountResourcesInThePeriod(long accountId, int usageType, Date startDate, Date endDate);
}
