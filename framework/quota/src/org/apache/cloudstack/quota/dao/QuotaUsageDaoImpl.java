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
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.quota.vo.QuotaUsageVO;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import javax.ejb.Local;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Component
@Local(value = { QuotaUsageDao.class })
public class QuotaUsageDaoImpl extends GenericDaoBase<QuotaUsageVO, Long> implements QuotaUsageDao {
    private static final Logger s_logger = Logger.getLogger(QuotaUsageDaoImpl.class.getName());

    @Override
    public BigDecimal findTotalQuotaUsage(final Long accountId, final Long domainId, final Integer usageType, final Date startDate, final Date endDate) {
        List<QuotaUsageVO> quotaUsage = findQuotaUsage(accountId, domainId, null, startDate, endDate);
        BigDecimal total = new BigDecimal(0);
        for (QuotaUsageVO quotaRecord: quotaUsage) {
            total = total.add(quotaRecord.getQuotaUsed());
        }
        return total;
    }

    @SuppressWarnings("deprecation")
    @Override
    public List<QuotaUsageVO> findQuotaUsage(final Long accountId, final Long domainId, final Integer usageType, final Date startDate, final Date endDate) {
        final short opendb = TransactionLegacy.currentTxn().getDatabaseId();
        List<QuotaUsageVO> quotaUsageRecords = new ArrayList<QuotaUsageVO>();
        try (TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.USAGE_DB)) {
            // TODO instead of max value query with reasonable number and iterate
            SearchCriteria<QuotaUsageVO> sc = createSearchCriteria();
            if (accountId != null) {
                sc.addAnd("accountId", SearchCriteria.Op.EQ, accountId);
            }
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
        } catch (Exception e) {
            s_logger.error("QuotaUsageDaoImpl::findQuotaUsage() failed due to: " + e.getMessage());
            throw new CloudRuntimeException("Unable to find quota usage");
        } finally {
            TransactionLegacy.open(opendb).close();
        }
        return quotaUsageRecords;
    }

}
