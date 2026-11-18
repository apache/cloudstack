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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.cloudstack.quota.vo.QuotaUsageVO;
import org.springframework.stereotype.Component;

import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.QueryBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.utils.db.TransactionStatus;

@Component
public class QuotaUsageDaoImpl extends GenericDaoBase<QuotaUsageVO, Long> implements QuotaUsageDao {

    @Override
    public BigDecimal findTotalQuotaUsage(final Long accountId, final Long domainId, final Integer usageType, final Date startDate, final Date endDate) {
        List<QuotaUsageVO> quotaUsage = findQuotaUsage(accountId, domainId, null, startDate, endDate);
        BigDecimal total = new BigDecimal(0);
        for (QuotaUsageVO quotaRecord : quotaUsage) {
            total = total.add(quotaRecord.getQuotaUsed());
        }
        return total;
    }

    @Override
    public List<QuotaUsageVO> findQuotaUsage(final Long accountId, final Long domainId, final Integer usageType, final Date startDate, final Date endDate) {
        return Transaction.execute(TransactionLegacy.USAGE_DB, new TransactionCallback<List<QuotaUsageVO>>() {
            @Override
            public List<QuotaUsageVO> doInTransaction(final TransactionStatus status) {
                List<QuotaUsageVO> quv;
                if ((startDate != null) && (endDate != null) && startDate.before(endDate)) {
                    QueryBuilder<QuotaUsageVO> qb = QueryBuilder.create(QuotaUsageVO.class);
                    if (accountId != null) {
                        qb.and(qb.entity().getAccountId(), SearchCriteria.Op.EQ, accountId);
                    }
                    if (domainId != null) {
                        qb.and(qb.entity().getDomainId(), SearchCriteria.Op.EQ, domainId);
                    }
                    if (usageType != null) {
                        qb.and(qb.entity().getUsageType(), SearchCriteria.Op.EQ, usageType);
                    }
                    qb.and(qb.entity().getStartDate(), SearchCriteria.Op.BETWEEN, startDate, endDate);
                    qb.and(qb.entity().getEndDate(), SearchCriteria.Op.BETWEEN, startDate, endDate);
                    quv = listBy(qb.create());
                } else {
                    quv = new ArrayList<QuotaUsageVO>();
                }
                if (quv.isEmpty()){
                    //add a dummy entry
                    QuotaUsageVO qu = new  QuotaUsageVO();
                    qu.setAccountId(accountId);
                    qu.setDomainId(domainId);
                    qu.setStartDate(startDate);
                    qu.setEndDate(endDate);
                    qu.setQuotaUsed(new BigDecimal(0));
                    qu.setUsageType(-1);
                    quv.add(qu);
                }
                return quv;
            }
        });
    }

    @Override
    public QuotaUsageVO findLastQuotaUsageEntry(final Long accountId, final Long domainId, final Date beforeThis) {
        return Transaction.execute(TransactionLegacy.USAGE_DB, new TransactionCallback<QuotaUsageVO>() {
            @Override
            public QuotaUsageVO doInTransaction(final TransactionStatus status) {
                List<QuotaUsageVO> quotaUsageEntries = new ArrayList<>();
                Filter filter = new Filter(QuotaUsageVO.class, "startDate", false, 0L, 1L);
                QueryBuilder<QuotaUsageVO> qb = QueryBuilder.create(QuotaUsageVO.class);
                qb.and(qb.entity().getAccountId(), SearchCriteria.Op.EQ, accountId);
                qb.and(qb.entity().getDomainId(), SearchCriteria.Op.EQ, domainId);
                qb.and(qb.entity().getStartDate(), SearchCriteria.Op.LT, beforeThis);
                quotaUsageEntries = search(qb.create(), filter);
                return !quotaUsageEntries.isEmpty() ? quotaUsageEntries.get(0) : null;
            }
        });
    }

    @Override
    public QuotaUsageVO persistQuotaUsage(final QuotaUsageVO quotaUsage) {
        return Transaction.execute(TransactionLegacy.USAGE_DB, new TransactionCallback<QuotaUsageVO>() {
            @Override
            public QuotaUsageVO doInTransaction(final TransactionStatus status) {
                return persist(quotaUsage);
            }
        });
    }

}
