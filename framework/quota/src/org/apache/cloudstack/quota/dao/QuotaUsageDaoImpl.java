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
import org.apache.cloudstack.quota.vo.QuotaUsageVO;

import com.cloud.utils.Pair;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.TransactionLegacy;

@Component
@Local(value = { QuotaUsageDao.class })
public class QuotaUsageDaoImpl extends GenericDaoBase<QuotaUsageVO, Long> implements QuotaUsageDao {

    @Override
    public Pair<List<QuotaUsageVO>, Integer> searchAndCountAllRecords(SearchCriteria<QuotaUsageVO> sc, Filter filter) {
        return listAndCountIncludingRemovedBy(sc, filter);
    }

    @Override
    public void saveQuotaUsage(List<QuotaUsageVO> records) {
        for (QuotaUsageVO usageRecord : records) {
            persist(usageRecord);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public List<QuotaUsageVO> findQuotaUsage(final Long accountId, final Long domainId, final Integer usageType, final Date startDate, final Date endDate) {
        final short opendb = TransactionLegacy.currentTxn().getDatabaseId();
        TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.USAGE_DB);
        List<QuotaUsageVO> quotaUsageRecords = null;
        try {
            // TODO instead of max value query with reasonable number and
            // iterate
            SearchCriteria<QuotaUsageVO> sc = createSearchCriteria();
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
            if (usageType != null) {
                sc.addAnd("usageType", SearchCriteria.Op.EQ, usageType);
            }
            if ((startDate != null) && (endDate != null) && startDate.before(endDate)) {
                sc.addAnd("startDate", SearchCriteria.Op.BETWEEN, startDate, endDate);
                sc.addAnd("endDate", SearchCriteria.Op.BETWEEN, startDate, endDate);
            } else {
                return new ArrayList<QuotaUsageVO>();
            }
            quotaUsageRecords = listBy(sc);
        } finally {
            txn.close();
        }

        TransactionLegacy.open(opendb).close();
        return quotaUsageRecords;
    }

}
