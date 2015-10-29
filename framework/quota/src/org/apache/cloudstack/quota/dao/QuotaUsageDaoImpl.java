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

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.QueryBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.utils.db.TransactionStatus;

import org.apache.cloudstack.quota.vo.QuotaUsageVO;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import javax.ejb.Local;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Component
@Local(value = {QuotaUsageDao.class})
public class QuotaUsageDaoImpl extends GenericDaoBase<QuotaUsageVO, Long> implements QuotaUsageDao {
    private static final Logger s_logger = Logger.getLogger(QuotaUsageDaoImpl.class);

    public BigDecimal findTotalQuotaUsage(final Long accountId, final Long domainId, final Integer usageType, final Date startDate, final Date endDate) {
        List<QuotaUsageVO> quotaUsage = findQuotaUsage(accountId, domainId, null, startDate, endDate);
        BigDecimal total = new BigDecimal(0);
        for (QuotaUsageVO quotaRecord : quotaUsage) {
            total = total.add(quotaRecord.getQuotaUsed());
        }
        return total;
    }

    public List<QuotaUsageVO> findQuotaUsage(final Long accountId, final Long domainId, final Integer usageType, final Date startDate, final Date endDate) {
        return Transaction.execute(TransactionLegacy.USAGE_DB, new TransactionCallback<List<QuotaUsageVO>>() {
            @Override
            public List<QuotaUsageVO> doInTransaction(final TransactionStatus status) {
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
                    return listBy(qb.create());
                } else {
                    return new ArrayList<QuotaUsageVO>();
                }
            }
        });
    }

    public QuotaUsageVO persistQuotaUsage(final QuotaUsageVO quotaUsage) {
        return Transaction.execute(TransactionLegacy.USAGE_DB, new TransactionCallback<QuotaUsageVO>() {
            @Override
            public QuotaUsageVO doInTransaction(final TransactionStatus status) {
                return persist(quotaUsage);
            }
        });
    }

}
