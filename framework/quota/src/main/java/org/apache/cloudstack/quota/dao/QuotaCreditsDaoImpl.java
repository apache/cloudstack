//Licensed to the Apache Software Foundation (ASF) under one
//or more contributor license agreements.  See the NOTICE file
//distributed with this work for additional information
//regarding copyright ownership.  The ASF licenses this file
//to you under the Apache License, Version 2.0 (the
//"License"); you may not use this file except in compliance
//with the License.  You may obtain a copy of the License at
//
//http://www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing,
//software distributed under the License is distributed on an
//"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
//KIND, either express or implied.  See the License for the
//specific language governing permissions and limitations
//under the License.
package org.apache.cloudstack.quota.dao;

import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import com.cloud.domain.dao.DomainDao;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.SearchBuilder;
import org.apache.cloudstack.quota.vo.QuotaBalanceVO;
import org.apache.cloudstack.quota.vo.QuotaCreditsVO;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Component;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.utils.db.TransactionStatus;

@Component
public class QuotaCreditsDaoImpl extends GenericDaoBase<QuotaCreditsVO, Long> implements QuotaCreditsDao {

    @Inject
    DomainDao domainDao;
    @Inject
    QuotaBalanceDao quotaBalanceDao;

    private SearchBuilder<QuotaCreditsVO> quotaCreditsVoSearch;

    public QuotaCreditsDaoImpl() {
        quotaCreditsVoSearch = createSearchBuilder();
        quotaCreditsVoSearch.and("updatedOn", quotaCreditsVoSearch.entity().getUpdatedOn(), SearchCriteria.Op.BETWEEN);
        quotaCreditsVoSearch.and("accountId", quotaCreditsVoSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        quotaCreditsVoSearch.and("domainId", quotaCreditsVoSearch.entity().getDomainId(), SearchCriteria.Op.IN);
        quotaCreditsVoSearch.done();
    }

    @Override
    public List<QuotaCreditsVO> findCredits(Long accountId, Long domainId, Date startDate, Date endDate, boolean recursive) {
        SearchCriteria<QuotaCreditsVO> sc = quotaCreditsVoSearch.create();
        Filter filter = new Filter(QuotaCreditsVO.class, "updatedOn", true, 0L, Long.MAX_VALUE);

        sc.setParametersIfNotNull("accountId", accountId);
        if (domainId != null) {
            List<Long> domainIds = recursive ? domainDao.getDomainAndChildrenIds(domainId) : List.of(domainId);
            sc.setParameters("domainId", domainIds.toArray());
        }

        if (ObjectUtils.allNotNull(startDate, endDate)) {
            sc.setParameters("updatedOn", startDate, endDate);
        }

        return Transaction.execute(TransactionLegacy.USAGE_DB, (TransactionCallback<List<QuotaCreditsVO>>) status -> search(sc, filter));
    }

    @Override
    public QuotaCreditsVO saveCredits(final QuotaCreditsVO credits) {
        return Transaction.execute(TransactionLegacy.USAGE_DB, new TransactionCallback<QuotaCreditsVO>() {
            @Override
            public QuotaCreditsVO doInTransaction(final TransactionStatus status) {
                persist(credits);
                // make an entry in the balance table
                QuotaBalanceVO bal = new QuotaBalanceVO(credits);
                quotaBalanceDao.persist(bal);
                return credits;
            }
        });
    }
}
