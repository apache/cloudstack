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
import javax.inject.Inject;

import org.springframework.stereotype.Component;
import org.apache.cloudstack.quota.vo.QuotaBalanceVO;
import org.apache.cloudstack.quota.vo.QuotaCreditsVO;

import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.TransactionLegacy;

@Component
@Local(value = { QuotaCreditsDao.class })
public class QuotaCreditsDaoImpl extends GenericDaoBase<QuotaCreditsVO, Long> implements QuotaCreditsDao {
    @Inject
    QuotaBalanceDao _quotaBalanceDao;

    @SuppressWarnings("deprecation")
    @Override
    public List<QuotaCreditsVO> findCredits(final long accountId, final long domainId, final Date startDate, final Date endDate) {
        final short opendb = TransactionLegacy.currentTxn().getDatabaseId();
        TransactionLegacy.open(TransactionLegacy.USAGE_DB);
        Filter filter = new Filter(QuotaCreditsVO.class, "updatedOn", true, 0L, Long.MAX_VALUE);
        SearchCriteria<QuotaCreditsVO> sc = createSearchCriteria();
        sc.addAnd("accountId", SearchCriteria.Op.EQ, accountId);
        sc.addAnd("domainId", SearchCriteria.Op.EQ, domainId);
        if ((startDate != null) && (endDate != null) && startDate.before(endDate)) {
            sc.addAnd("updatedOn", SearchCriteria.Op.BETWEEN, startDate, endDate);
        } else {
            return new ArrayList<QuotaCreditsVO>();
        }
        List<QuotaCreditsVO> qc = search(sc, filter);
        TransactionLegacy.open(opendb).close();
        return qc;
    }

    @Override
    public QuotaCreditsVO saveCredits(final QuotaCreditsVO credits) {
        final short opendb = TransactionLegacy.currentTxn().getDatabaseId();
        TransactionLegacy.open(TransactionLegacy.USAGE_DB);
        persist(credits);
        // make an entry in the balance table
        QuotaBalanceVO bal = new QuotaBalanceVO(credits);
        _quotaBalanceDao.persist(bal);
        TransactionLegacy.open(opendb).close();
        return credits;
    }

}
