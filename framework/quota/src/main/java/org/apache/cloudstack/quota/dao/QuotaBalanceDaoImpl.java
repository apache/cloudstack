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

import org.apache.cloudstack.quota.vo.QuotaBalanceVO;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.QueryBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.utils.db.TransactionLegacy;

@Component
public class QuotaBalanceDaoImpl extends GenericDaoBase<QuotaBalanceVO, Long> implements QuotaBalanceDao {
    private static final Logger logger = LogManager.getLogger(QuotaBalanceDaoImpl.class);

    @Override
    public QuotaBalanceVO getLastQuotaBalanceEntry(final Long accountId, final Long domainId, final Date beforeThis) {
        return Transaction.execute(TransactionLegacy.USAGE_DB, (TransactionCallback<QuotaBalanceVO>) status -> {
            Filter filter = new Filter(QuotaBalanceVO.class, "updatedOn", false, 0L, 1L);
            QueryBuilder<QuotaBalanceVO> qb = getQuotaBalanceQueryBuilder(accountId, domainId);
            qb.and(qb.entity().getCreditsId(), SearchCriteria.Op.EQ, 0);

            if (beforeThis != null) {
                qb.and(qb.entity().getUpdatedOn(), SearchCriteria.Op.LT, beforeThis);
            }

            List<QuotaBalanceVO> quotaBalanceEntries = search(qb.create(), filter);
            return !quotaBalanceEntries.isEmpty() ? quotaBalanceEntries.get(0) : null;
        });
    }

    @Override
    public QuotaBalanceVO findLaterBalanceEntry(final Long accountId, final Long domainId, final Date afterThis) {
        return Transaction.execute(TransactionLegacy.USAGE_DB, (TransactionCallback<QuotaBalanceVO>) status -> {
            Filter filter = new Filter(QuotaBalanceVO.class, "updatedOn", true, 0L, 1L);
            QueryBuilder<QuotaBalanceVO> qb = getQuotaBalanceQueryBuilder(accountId, domainId);
            qb.and(qb.entity().getCreditsId(), SearchCriteria.Op.EQ, 0);
            qb.and(qb.entity().getUpdatedOn(), SearchCriteria.Op.GT, afterThis);

            List<QuotaBalanceVO> quotaBalanceEntries = search(qb.create(), filter);
            return !quotaBalanceEntries.isEmpty() ? quotaBalanceEntries.get(0) : null;
        });
    }

    @Override
    public QuotaBalanceVO saveQuotaBalance(final QuotaBalanceVO qb) {
        return Transaction.execute(TransactionLegacy.USAGE_DB, (TransactionCallback<QuotaBalanceVO>) status -> persist(qb));
    }

    @Override
    public List<QuotaBalanceVO> findCreditBalances(final Long accountId, final Long domainId, final Date startDate, final Date endDate) {
        return Transaction.execute(TransactionLegacy.USAGE_DB, (TransactionCallback<List<QuotaBalanceVO>>) status -> {
            if (ObjectUtils.anyNull(startDate, endDate) || startDate.after(endDate)) {
                return new ArrayList<>();
            }

            Filter filter = new Filter(QuotaBalanceVO.class, "updatedOn", true, 0L, Long.MAX_VALUE);
            QueryBuilder<QuotaBalanceVO> qb = getQuotaBalanceQueryBuilder(accountId, domainId);
            qb.and(qb.entity().getCreditsId(), SearchCriteria.Op.GT, 0);
            qb.and(qb.entity().getUpdatedOn(), SearchCriteria.Op.BETWEEN, startDate, endDate);

            return search(qb.create(), filter);
        });
    }

    @Override
    public List<QuotaBalanceVO> listQuotaBalances(final Long accountId, final Long domainId, final Date startDate, final Date endDate) {
        return Transaction.execute(TransactionLegacy.USAGE_DB, (TransactionCallback<List<QuotaBalanceVO>>) status -> {
            QueryBuilder<QuotaBalanceVO> qb = getQuotaBalanceQueryBuilder(accountId, domainId);
            qb.and(qb.entity().getCreditsId(), SearchCriteria.Op.EQ, 0);
            if (startDate != null) {
                qb.and(qb.entity().getUpdatedOn(), SearchCriteria.Op.GTEQ, startDate);
            }
            if (endDate != null) {
                qb.and(qb.entity().getUpdatedOn(), SearchCriteria.Op.LTEQ, endDate);
            }

            Filter filter = new Filter(QuotaBalanceVO.class, "updatedOn", true);
            return listBy(qb.create(), filter);
        });
    }

    @Override
    public BigDecimal getLastQuotaBalance(final Long accountId, final Long domainId) {
        QuotaBalanceVO quotaBalance = getLastQuotaBalanceEntry(accountId, domainId, null);
        BigDecimal finalBalance = BigDecimal.ZERO;
        Date startDate = DateUtils.addDays(new Date(), -1);
        if (quotaBalance == null) {
            logger.info("There are no balance entries for account [{}] and domain [{}]. Considering only new added credits.", accountId, domainId);
        } else {
            finalBalance = quotaBalance.getCreditBalance();
            startDate = quotaBalance.getUpdatedOn();
        }

        List<QuotaBalanceVO> credits = findCreditBalances(accountId, domainId, startDate, new Date());

        for (QuotaBalanceVO credit : credits) {
            finalBalance = finalBalance.add(credit.getCreditBalance());
        }

        return finalBalance;
    }

    private QueryBuilder<QuotaBalanceVO> getQuotaBalanceQueryBuilder(Long accountId, Long domainId) {
        QueryBuilder<QuotaBalanceVO> qb = QueryBuilder.create(QuotaBalanceVO.class);
        qb.and(qb.entity().getAccountId(), SearchCriteria.Op.EQ, accountId);
        qb.and(qb.entity().getDomainId(), SearchCriteria.Op.EQ, domainId);
        return qb;
    }

}
