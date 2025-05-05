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
//
package org.apache.cloudstack.quota.dao;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.JoinBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.utils.db.TransactionLegacy;
import org.apache.cloudstack.quota.vo.QuotaUsageDetailVO;
import org.apache.cloudstack.quota.vo.QuotaUsageJoinVO;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.Date;
import java.util.List;

@Component
public class QuotaUsageJoinDaoImpl extends GenericDaoBase<QuotaUsageJoinVO, Long> implements QuotaUsageJoinDao {

    private SearchBuilder<QuotaUsageJoinVO> searchQuotaUsages;

    private SearchBuilder<QuotaUsageJoinVO> searchQuotaUsagesJoinDetails;

    @Inject
    private QuotaUsageDetailDao quotaUsageDetailDao;

    @PostConstruct
    public void init() {
        searchQuotaUsages = createSearchBuilder();
        prepareQuotaUsageSearchBuilder(searchQuotaUsages);
        searchQuotaUsages.done();

        SearchBuilder<QuotaUsageDetailVO> searchQuotaUsageDetails = quotaUsageDetailDao.createSearchBuilder();
        searchQuotaUsageDetails.and("tariffId", searchQuotaUsageDetails.entity().getTariffId(), SearchCriteria.Op.EQ);
        searchQuotaUsagesJoinDetails = createSearchBuilder();
        prepareQuotaUsageSearchBuilder(searchQuotaUsagesJoinDetails);
        searchQuotaUsagesJoinDetails.join("searchQuotaUsageDetails", searchQuotaUsageDetails, searchQuotaUsagesJoinDetails.entity().getId(),
                searchQuotaUsageDetails.entity().getQuotaUsageId(), JoinBuilder.JoinType.INNER);
        searchQuotaUsagesJoinDetails.done();
    }

    private void prepareQuotaUsageSearchBuilder(SearchBuilder<QuotaUsageJoinVO> searchBuilder) {
        searchBuilder.and("accountId", searchBuilder.entity().getAccountId(), SearchCriteria.Op.EQ);
        searchBuilder.and("domainId", searchBuilder.entity().getDomainId(), SearchCriteria.Op.EQ);
        searchBuilder.and("usageType", searchBuilder.entity().getUsageType(), SearchCriteria.Op.EQ);
        searchBuilder.and("resourceId", searchBuilder.entity().getResourceId(), SearchCriteria.Op.EQ);
        searchBuilder.and("networkId", searchBuilder.entity().getNetworkId(), SearchCriteria.Op.EQ);
        searchBuilder.and("offeringId", searchBuilder.entity().getOfferingId(), SearchCriteria.Op.EQ);
        searchBuilder.and("startDate", searchBuilder.entity().getStartDate(), SearchCriteria.Op.BETWEEN);
        searchBuilder.and("endDate", searchBuilder.entity().getEndDate(), SearchCriteria.Op.BETWEEN);
    }

    @Override
    public List<QuotaUsageJoinVO> findQuotaUsage(Long accountId, Long domainId, Integer usageType, Long resourceId, Long networkId, Long offeringId, Date startDate, Date endDate, Long tariffId) {
        SearchCriteria<QuotaUsageJoinVO> sc = tariffId == null ? searchQuotaUsages.create() : searchQuotaUsagesJoinDetails.create();

        sc.setParametersIfNotNull("accountId", accountId);
        sc.setParametersIfNotNull("domainId", domainId);
        sc.setParametersIfNotNull("usageType", usageType);
        sc.setParametersIfNotNull("resourceId", resourceId);
        sc.setParametersIfNotNull("networkId", networkId);
        sc.setParametersIfNotNull("offeringId", offeringId);

        if (ObjectUtils.allNotNull(startDate, endDate)) {
            sc.setParameters("startDate", startDate, endDate);
            sc.setParameters("endDate", startDate, endDate);
        }

        sc.setJoinParametersIfNotNull("searchQuotaUsageDetails", "tariffId", tariffId);

        return Transaction.execute(TransactionLegacy.USAGE_DB, (TransactionCallback<List<QuotaUsageJoinVO>>) status -> listBy(sc));
    }

}
