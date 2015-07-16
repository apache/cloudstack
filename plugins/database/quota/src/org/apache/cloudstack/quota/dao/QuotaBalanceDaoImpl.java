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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.ejb.Local;

import org.springframework.stereotype.Component;
import org.apache.cloudstack.quota.QuotaBalanceVO;

import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.TransactionLegacy;

@Component
@Local(value = { QuotaBalanceDao.class })
public class QuotaBalanceDaoImpl extends GenericDaoBase<QuotaBalanceVO, Long> implements QuotaBalanceDao {

    @SuppressWarnings("deprecation")
    @Override
    public QuotaBalanceVO getLastBalanceEntry(long accountId, long domainId, Date beforeThis) {
        Filter filter = new Filter(QuotaBalanceVO.class, "updatedOn", false, 0L, 1L);
        SearchCriteria<QuotaBalanceVO> sc = createSearchCriteria();
        sc.addAnd("accountId", SearchCriteria.Op.EQ, accountId);
        sc.addAnd("domainId", SearchCriteria.Op.EQ, domainId);
        sc.addAnd("creditsId", SearchCriteria.Op.EQ, 0);
        sc.addAnd("updatedOn", SearchCriteria.Op.LT, beforeThis);
        List<QuotaBalanceVO> quotab = this.search(sc, filter);
        return quotab.size() > 0 ? quotab.get(0) : null;
    }

    @Override
    public void saveQuotaBalance(List<QuotaBalanceVO> credits) {
        for (QuotaBalanceVO credit : credits) {
            persist(credit);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public List<QuotaBalanceVO> getCreditBalance(long accountId, long domainId, Date lastbalancedate, Date beforeThis) {
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
        return search(sc, filter);
    }


    @SuppressWarnings("deprecation")
    @Override
    public List<QuotaBalanceVO> getQuotaBalance(Long accountId, Long domainId, Date startDate, Date endDate) {

        TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.USAGE_DB);
        List<QuotaBalanceVO> quotaUsageRecords = null;
        try {
            // TODO instead of max value query with reasonable number and
            // iterate
            SearchCriteria<QuotaBalanceVO> sc = createSearchCriteria();
            if (accountId != null) {
                sc.addAnd("accountId", SearchCriteria.Op.EQ, accountId);
            }
            /*
             * if (isDomainAdmin) { SearchCriteria<DomainVO> sdc =
             * _domainDao.createSearchCriteria(); sdc.addOr("path",
             * SearchCriteria.Op.LIKE,
             * _domainDao.findById(caller.getDomainId()).getPath() + "%");
             * List<DomainVO> domains = _domainDao.search(sdc, null); List<Long>
             * domainIds = new ArrayList<Long>(); for (DomainVO domain :
             * domains) domainIds.add(domain.getId()); sc.addAnd("domainId",
             * SearchCriteria.Op.IN, domainIds.toArray());
             * s_logger.debug("Account ID=" + accountId); }
             */
            if (domainId != null) {
                sc.addAnd("domainId", SearchCriteria.Op.EQ, domainId);
            }
            if ((startDate != null) && (endDate != null) && startDate.before(endDate)) {
                sc.addAnd("updatedOn", SearchCriteria.Op.BETWEEN, startDate, endDate);
            } else {
                return new ArrayList<QuotaBalanceVO>();
            }
            quotaUsageRecords = listBy(sc);
        } finally {
            txn.close();
        }

        TransactionLegacy.open(TransactionLegacy.CLOUD_DB).close();
        return quotaUsageRecords;
    }
}
