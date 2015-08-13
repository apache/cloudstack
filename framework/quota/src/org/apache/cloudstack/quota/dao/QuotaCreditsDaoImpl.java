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

import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.ejb.Local;
import javax.inject.Inject;

import org.springframework.stereotype.Component;
import org.apache.cloudstack.quota.vo.QuotaBalanceVO;
import org.apache.cloudstack.quota.vo.QuotaCreditsVO;

import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.QueryBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.utils.db.TransactionStatus;

@Component
@Local(value = { QuotaCreditsDao.class })
public class QuotaCreditsDaoImpl extends GenericDaoBase<QuotaCreditsVO, Long> implements QuotaCreditsDao {

    @Inject
    QuotaBalanceDao _quotaBalanceDao;

    @Override
    public List<QuotaCreditsVO> findCredits(final long accountId, final long domainId, final Date startDate, final Date endDate) {
        return Transaction.execute(TransactionLegacy.USAGE_DB, new TransactionCallback<List<QuotaCreditsVO>>() {
            @Override
            public List<QuotaCreditsVO> doInTransaction(final TransactionStatus status) {
                if ((startDate != null) && (endDate != null) && startDate.before(endDate)) {
                    Filter filter = new Filter(QuotaCreditsVO.class, "updatedOn", true, 0L, Long.MAX_VALUE);
                    QueryBuilder<QuotaCreditsVO> qb = QueryBuilder.create(QuotaCreditsVO.class);
                    qb.and(qb.entity().getAccountId(), SearchCriteria.Op.EQ, accountId);
                    qb.and(qb.entity().getDomainId(), SearchCriteria.Op.EQ, domainId);
                    qb.and(qb.entity().getUpdatedOn(), SearchCriteria.Op.BETWEEN, startDate, endDate);
                    return search(qb.create(), filter);
                } else {
                    return Collections.<QuotaCreditsVO> emptyList();
                }
            }
        });
    }

    @Override
    public QuotaCreditsVO saveCredits(final QuotaCreditsVO credits) {
        return Transaction.execute(TransactionLegacy.USAGE_DB, new TransactionCallback<QuotaCreditsVO>() {
            @Override
            public QuotaCreditsVO doInTransaction(final TransactionStatus status) {
                persist(credits);
                // make an entry in the balance table
                QuotaBalanceVO bal = new QuotaBalanceVO(credits);
                _quotaBalanceDao.persist(bal);
                return credits;
            }
        });
    }
}
