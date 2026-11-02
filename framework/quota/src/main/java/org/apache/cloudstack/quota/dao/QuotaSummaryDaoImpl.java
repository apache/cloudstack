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

package org.apache.cloudstack.quota.dao;

import java.util.List;

import org.apache.cloudstack.quota.QuotaAccountStateFilter;
import org.apache.cloudstack.quota.vo.QuotaSummaryVO;

import com.cloud.utils.Pair;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.utils.db.TransactionLegacy;

public class QuotaSummaryDaoImpl extends GenericDaoBase<QuotaSummaryVO, Long> implements QuotaSummaryDao {

    @Override
    public Pair<List<QuotaSummaryVO>, Integer> listQuotaSummariesForAccountAndOrDomain(Long accountId, String accountName, Long domainId, String domainPath,
                                                                                       QuotaAccountStateFilter accountStateFilter, Long startIndex, Long pageSize) {
        SearchCriteria<QuotaSummaryVO> searchCriteria = createListQuotaSummariesSearchCriteria(accountId, accountName, domainId, domainPath, accountStateFilter);
        Filter filter = new Filter(QuotaSummaryVO.class, "accountName", true, startIndex, pageSize);

        return Transaction.execute(TransactionLegacy.USAGE_DB, (TransactionCallback<Pair<List<QuotaSummaryVO>, Integer>>) status -> searchAndCount(searchCriteria, filter));
    }

    protected SearchCriteria<QuotaSummaryVO> createListQuotaSummariesSearchCriteria(Long accountId, String accountName, Long domainId, String domainPath,
                                                                                    QuotaAccountStateFilter accountStateFilter) {
        SearchCriteria<QuotaSummaryVO> searchCriteria = createListQuotaSummariesSearchBuilder(accountStateFilter).create();

        searchCriteria.setParametersIfNotNull("accountId", accountId);
        searchCriteria.setParametersIfNotNull("domainId", domainId);

        if (accountName != null) {
            searchCriteria.setParameters("accountName", "%" + accountName + "%");
        }

        if (domainPath != null) {
            searchCriteria.setParameters("domainPath", domainPath + "%");
        }

        return searchCriteria;
    }

    protected SearchBuilder<QuotaSummaryVO> createListQuotaSummariesSearchBuilder(QuotaAccountStateFilter accountStateFilter) {
        SearchBuilder<QuotaSummaryVO> searchBuilder = createSearchBuilder();

        searchBuilder.and("accountId", searchBuilder.entity().getAccountId(), SearchCriteria.Op.EQ);
        searchBuilder.and("accountName", searchBuilder.entity().getAccountName(), SearchCriteria.Op.LIKE);
        searchBuilder.and("domainId", searchBuilder.entity().getDomainId(), SearchCriteria.Op.EQ);
        searchBuilder.and("domainPath", searchBuilder.entity().getDomainPath(), SearchCriteria.Op.LIKE);

        if (QuotaAccountStateFilter.REMOVED.equals(accountStateFilter)) {
            searchBuilder.and("accountRemoved", searchBuilder.entity().getAccountRemoved(), SearchCriteria.Op.NNULL);
        } else if (QuotaAccountStateFilter.ACTIVE.equals(accountStateFilter)) {
            searchBuilder.and("accountRemoved", searchBuilder.entity().getAccountRemoved(), SearchCriteria.Op.NULL);
        }

        return searchBuilder;
    }

}
