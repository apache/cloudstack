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
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.cloudstack.quota.vo.QuotaBalanceVO;
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
public class QuotaBalanceDaoImpl extends GenericDaoBase<QuotaBalanceVO, Long> implements QuotaBalanceDao {

    @Override
    public QuotaBalanceVO findLastBalanceEntry(final Long accountId, final Long domainId, final Date beforeThis) {
        return Transaction.execute(TransactionLegacy.USAGE_DB, new TransactionCallback<QuotaBalanceVO>() {
            @Override
            public QuotaBalanceVO doInTransaction(final TransactionStatus status) {
                List<QuotaBalanceVO> quotaBalanceEntries = new ArrayList<>();
                Filter filter = new Filter(QuotaBalanceVO.class, "updatedOn", false, 0L, 1L);
                QueryBuilder<QuotaBalanceVO> qb = QueryBuilder.create(QuotaBalanceVO.class);
                qb.and(qb.entity().getAccountId(), SearchCriteria.Op.EQ, accountId);
                qb.and(qb.entity().getDomainId(), SearchCriteria.Op.EQ, domainId);
                qb.and(qb.entity().getCreditsId(), SearchCriteria.Op.EQ, 0);
                qb.and(qb.entity().getUpdatedOn(), SearchCriteria.Op.LT, beforeThis);
                quotaBalanceEntries = search(qb.create(), filter);
                return !quotaBalanceEntries.isEmpty() ? quotaBalanceEntries.get(0) : null;
            }
        });
    }

    @Override
    public QuotaBalanceVO findLaterBalanceEntry(final Long accountId, final Long domainId, final Date afterThis) {
        return Transaction.execute(TransactionLegacy.USAGE_DB, new TransactionCallback<QuotaBalanceVO>() {
            @Override
            public QuotaBalanceVO doInTransaction(final TransactionStatus status) {
                List<QuotaBalanceVO> quotaBalanceEntries = new ArrayList<>();
                Filter filter = new Filter(QuotaBalanceVO.class, "updatedOn", true, 0L, 1L);
                QueryBuilder<QuotaBalanceVO> qb = QueryBuilder.create(QuotaBalanceVO.class);
                qb.and(qb.entity().getAccountId(), SearchCriteria.Op.EQ, accountId);
                qb.and(qb.entity().getDomainId(), SearchCriteria.Op.EQ, domainId);
                qb.and(qb.entity().getCreditsId(), SearchCriteria.Op.EQ, 0);
                qb.and(qb.entity().getUpdatedOn(), SearchCriteria.Op.GT, afterThis);
                quotaBalanceEntries = search(qb.create(), filter);
                return quotaBalanceEntries.size() > 0 ? quotaBalanceEntries.get(0) : null;
            }
        });
    }

    @Override
    public QuotaBalanceVO saveQuotaBalance(final QuotaBalanceVO qb) {
        return Transaction.execute(TransactionLegacy.USAGE_DB, new TransactionCallback<QuotaBalanceVO>() {
            @Override
            public QuotaBalanceVO doInTransaction(final TransactionStatus status) {
                return persist(qb);
            }
        });
    }

    @Override
    public List<QuotaBalanceVO> findCreditBalance(final Long accountId, final Long domainId, final Date lastbalancedate, final Date beforeThis) {
        return Transaction.execute(TransactionLegacy.USAGE_DB, new TransactionCallback<List<QuotaBalanceVO>>() {
            @Override
            public List<QuotaBalanceVO> doInTransaction(final TransactionStatus status) {
                if ((lastbalancedate != null) && (beforeThis != null) && lastbalancedate.before(beforeThis)) {
                    Filter filter = new Filter(QuotaBalanceVO.class, "updatedOn", true, 0L, Long.MAX_VALUE);
                    QueryBuilder<QuotaBalanceVO> qb = QueryBuilder.create(QuotaBalanceVO.class);
                    qb.and(qb.entity().getAccountId(), SearchCriteria.Op.EQ, accountId);
                    qb.and(qb.entity().getDomainId(), SearchCriteria.Op.EQ, domainId);
                    qb.and(qb.entity().getCreditsId(), SearchCriteria.Op.GT, 0);
                    qb.and(qb.entity().getUpdatedOn(), SearchCriteria.Op.BETWEEN, lastbalancedate, beforeThis);
                    return search(qb.create(), filter);
                } else {
                    return new ArrayList<QuotaBalanceVO>();
                }
            }
        });
    }

    @Override
    public List<QuotaBalanceVO> findQuotaBalance(final Long accountId, final Long domainId, final Date startDate, final Date endDate) {
        return Transaction.execute(TransactionLegacy.USAGE_DB, new TransactionCallback<List<QuotaBalanceVO>>() {
            @Override
            public List<QuotaBalanceVO> doInTransaction(final TransactionStatus status) {
                List<QuotaBalanceVO> quotaUsageRecords = null;
                QueryBuilder<QuotaBalanceVO> qb = QueryBuilder.create(QuotaBalanceVO.class);
                if (accountId != null) {
                    qb.and(qb.entity().getAccountId(), SearchCriteria.Op.EQ, accountId);
                }
                if (domainId != null) {
                    qb.and(qb.entity().getDomainId(), SearchCriteria.Op.EQ, domainId);
                }
                if ((startDate != null) && (endDate != null) && startDate.before(endDate)) {
                    qb.and(qb.entity().getUpdatedOn(), SearchCriteria.Op.BETWEEN, startDate, endDate);
                } else {
                    return Collections.<QuotaBalanceVO> emptyList();
                }
                quotaUsageRecords = listBy(qb.create());
                if (quotaUsageRecords.size() == 0) {
                    quotaUsageRecords.addAll(lastQuotaBalanceVO(accountId, domainId, startDate));
                }
                return quotaUsageRecords;

            }
        });

    }

    @Override
    public List<QuotaBalanceVO> lastQuotaBalanceVO(final Long accountId, final Long domainId, final Date pivotDate) {
        return Transaction.execute(TransactionLegacy.USAGE_DB, new TransactionCallback<List<QuotaBalanceVO>>() {
            @Override
            public List<QuotaBalanceVO> doInTransaction(final TransactionStatus status) {
                List<QuotaBalanceVO> quotaUsageRecords = null;
                List<QuotaBalanceVO> trimmedRecords = new ArrayList<QuotaBalanceVO>();
                Filter filter = new Filter(QuotaBalanceVO.class, "updatedOn", false, 0L, 100L);
                // ASSUMPTION there will be less than 100 continuous credit
                // transactions
                QueryBuilder<QuotaBalanceVO> qb = QueryBuilder.create(QuotaBalanceVO.class);
                if (accountId != null) {
                    qb.and(qb.entity().getAccountId(), SearchCriteria.Op.EQ, accountId);
                }
                if (domainId != null) {
                    qb.and(qb.entity().getDomainId(), SearchCriteria.Op.EQ, domainId);
                }
                if ((pivotDate != null)) {
                    qb.and(qb.entity().getUpdatedOn(), SearchCriteria.Op.LTEQ, pivotDate);
                }
                quotaUsageRecords = search(qb.create(), filter);

                // get records before startDate to find start balance
                for (QuotaBalanceVO entry : quotaUsageRecords) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("FindQuotaBalance Entry=" + entry);
                    }
                    if (entry.getCreditsId() > 0) {
                        trimmedRecords.add(entry);
                    } else {
                        trimmedRecords.add(entry);
                        break; // add only consecutive credit entries and last balance entry
                    }
                }
                return trimmedRecords;
            }
        });
    }

    @Override
    public BigDecimal lastQuotaBalance(final Long accountId, final Long domainId, Date startDate) {
        List<QuotaBalanceVO> quotaBalance = lastQuotaBalanceVO(accountId, domainId, startDate);
        BigDecimal finalBalance = new BigDecimal(0);
        if (quotaBalance.isEmpty()) {
            logger.info("There are no balance entries on or before the requested date.");
            return finalBalance;
        }
        for (QuotaBalanceVO entry : quotaBalance) {
            if (logger.isDebugEnabled()) {
                logger.debug("lastQuotaBalance Entry=" + entry);
            }
            finalBalance = finalBalance.add(entry.getCreditBalance());
        }
        return finalBalance;
    }

}
