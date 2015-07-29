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
import java.util.Iterator;
import java.util.List;

import javax.ejb.Local;

import org.springframework.stereotype.Component;
import org.apache.cloudstack.quota.vo.QuotaBalanceVO;
import org.apache.log4j.Logger;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.TransactionLegacy;

@Component
@Local(value = { QuotaBalanceDao.class })
public class QuotaBalanceDaoImpl extends GenericDaoBase<QuotaBalanceVO, Long> implements QuotaBalanceDao {
    private static final Logger s_logger = Logger.getLogger(QuotaBalanceDaoImpl.class.getName());

    @SuppressWarnings("deprecation")
    @Override
    public QuotaBalanceVO findLastBalanceEntry(final long accountId, final long domainId, final Date beforeThis) {
        final short opendb = TransactionLegacy.currentTxn().getDatabaseId();
        TransactionLegacy.open(TransactionLegacy.USAGE_DB).close();
        Filter filter = new Filter(QuotaBalanceVO.class, "updatedOn", false, 0L, 1L);
        SearchCriteria<QuotaBalanceVO> sc = createSearchCriteria();
        sc.addAnd("accountId", SearchCriteria.Op.EQ, accountId);
        sc.addAnd("domainId", SearchCriteria.Op.EQ, domainId);
        sc.addAnd("creditsId", SearchCriteria.Op.EQ, 0);
        sc.addAnd("updatedOn", SearchCriteria.Op.LT, beforeThis);
        List<QuotaBalanceVO> quotab = this.search(sc, filter);
        TransactionLegacy.open(opendb).close();
        return quotab.size() > 0 ? quotab.get(0) : null;
    }

    @SuppressWarnings("deprecation")
    @Override
    public QuotaBalanceVO findLaterBalanceEntry(final long accountId, final long domainId, final Date afterThis) {
        final short opendb = TransactionLegacy.currentTxn().getDatabaseId();
        TransactionLegacy.open(TransactionLegacy.USAGE_DB).close();
        Filter filter = new Filter(QuotaBalanceVO.class, "updatedOn", true, 0L, 1L);
        SearchCriteria<QuotaBalanceVO> sc = createSearchCriteria();
        sc.addAnd("accountId", SearchCriteria.Op.EQ, accountId);
        sc.addAnd("domainId", SearchCriteria.Op.EQ, domainId);
        sc.addAnd("creditsId", SearchCriteria.Op.EQ, 0);
        sc.addAnd("updatedOn", SearchCriteria.Op.GT, afterThis);
        List<QuotaBalanceVO> quotab = this.search(sc, filter);
        TransactionLegacy.open(opendb).close();
        return quotab.size() > 0 ? quotab.get(0) : null;
    }

    @Override
    public void saveQuotaBalance(final List<QuotaBalanceVO> credits) {
        final short opendb = TransactionLegacy.currentTxn().getDatabaseId();
        TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.USAGE_DB);
        try {
            for (QuotaBalanceVO credit : credits) {
                persist(credit);
            }
        } finally {
            txn.close();
        }
        TransactionLegacy.open(opendb).close();
    }

    @SuppressWarnings("deprecation")
    @Override
    public List<QuotaBalanceVO> findCreditBalance(final long accountId, final long domainId, final Date lastbalancedate, final Date beforeThis) {
        final short opendb = TransactionLegacy.currentTxn().getDatabaseId();
        TransactionLegacy.open(TransactionLegacy.USAGE_DB).close();
        Filter filter = new Filter(QuotaBalanceVO.class, "updatedOn", true, 0L, Long.MAX_VALUE);
        SearchCriteria<QuotaBalanceVO> sc = createSearchCriteria();
        sc.addAnd("accountId", SearchCriteria.Op.EQ, accountId);
        sc.addAnd("domainId", SearchCriteria.Op.EQ, domainId);
        sc.addAnd("creditsId", SearchCriteria.Op.GT, 0);
        if ((lastbalancedate != null) && (beforeThis != null) && lastbalancedate.before(beforeThis)) {
            sc.addAnd("updatedOn", SearchCriteria.Op.BETWEEN, lastbalancedate, beforeThis);
        } else {
            return new ArrayList<QuotaBalanceVO>();
        }
        List<QuotaBalanceVO> qb = search(sc, filter);
        TransactionLegacy.open(opendb).close();
        return qb;
    }

    @SuppressWarnings("deprecation")
    @Override
    public List<QuotaBalanceVO> findQuotaBalance(final Long accountId, final Long domainId, final Date startDate, final Date endDate) {
    // TODO account for series of credits around boundaries
        final short opendb = TransactionLegacy.currentTxn().getDatabaseId();

        TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.USAGE_DB);
        List<QuotaBalanceVO> quotaUsageRecords = null;
        try {
            SearchCriteria<QuotaBalanceVO> sc = createSearchCriteria();
            if (accountId != null) {
                sc.addAnd("accountId", SearchCriteria.Op.EQ, accountId);
            }
            if (domainId != null) {
                sc.addAnd("domainId", SearchCriteria.Op.EQ, domainId);
            }
            if ((startDate != null) && (endDate != null) && startDate.before(endDate)) {
                sc.addAnd("updatedOn", SearchCriteria.Op.BETWEEN, startDate, endDate);
            } else {
                return new ArrayList<QuotaBalanceVO>();
            }
            quotaUsageRecords = listBy(sc);
            quotaUsageRecords.addAll(findQuotaBalance(accountId, domainId, startDate));
        } finally {
            txn.close();
        }

        TransactionLegacy.open(opendb).close();
        return quotaUsageRecords;
    }

    @SuppressWarnings("deprecation")
    @Override
    public List<QuotaBalanceVO> findQuotaBalance(final Long accountId, final Long domainId, final Date startDate) {
        final short opendb = TransactionLegacy.currentTxn().getDatabaseId();

        TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.USAGE_DB);
        List<QuotaBalanceVO> quotaUsageRecords = null;
        List<QuotaBalanceVO> trimmedRecords = new ArrayList<QuotaBalanceVO>();
        try {
            Filter filter = new Filter(QuotaBalanceVO.class, "updatedOn", false, 0L, 100L);
            // ASSUMPTION there will be less than 100 continuous credit
            // transactions
            SearchCriteria<QuotaBalanceVO> sc = createSearchCriteria();
            if (accountId != null) {
                sc.addAnd("accountId", SearchCriteria.Op.EQ, accountId);
            }
            if (domainId != null) {
                sc.addAnd("domainId", SearchCriteria.Op.EQ, domainId);
            }
            if ((startDate != null)) {
                sc.addAnd("updatedOn", SearchCriteria.Op.LTEQ, startDate);
            }
            quotaUsageRecords = search(sc, filter);

            // get records before startDate to find start balance
            for (Iterator<QuotaBalanceVO> it = quotaUsageRecords.iterator(); it.hasNext();) {
                QuotaBalanceVO entry = it.next();
                s_logger.info("findQuotaBalance Date=" + entry.getUpdatedOn().toGMTString() + " balance=" + entry.getCreditBalance() + " credit=" + entry.getCreditsId());
                if (entry.getCreditsId() > 0) {
                    trimmedRecords.add(entry);
                } else {
                    trimmedRecords.add(entry);
                    break; // add only consecutive credit entries and last
                           // balance entry
                }
            }

        } finally {
            txn.close();
        }

        TransactionLegacy.open(opendb).close();
        return trimmedRecords;
    }

    @Override
    public BigDecimal lastQuotaBalance(final Long accountId, final Long domainId, Date startDate) {
        List<QuotaBalanceVO> quotaBalance = findQuotaBalance(accountId, domainId, startDate);
        if (quotaBalance.size() == 0) {
            new InvalidParameterValueException("There are no balance entries on or before the requested date.");
        }
        BigDecimal finalBalance = new BigDecimal(0);
        for (Iterator<QuotaBalanceVO> it = quotaBalance.iterator(); it.hasNext();) {
            QuotaBalanceVO entry = it.next();
            s_logger.info("lastQuotaBalance Date=" + entry.getUpdatedOn().toGMTString() + " balance=" + entry.getCreditBalance() + " credit=" + entry.getCreditsId());
            finalBalance.add(entry.getCreditBalance());
        }
        return finalBalance;
    }

}
