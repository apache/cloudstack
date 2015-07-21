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
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.TransactionLegacy;
import org.apache.cloudstack.quota.QuotaTariffVO;
import org.springframework.stereotype.Component;

import javax.ejb.Local;
import java.util.List;

@Component
@Local(value = { QuotaTariffDao.class })
public class QuotaTariffDaoImpl extends GenericDaoBase<QuotaTariffVO, Long> implements QuotaTariffDao {
    private final SearchBuilder<QuotaTariffVO> searchUsageType;
    private final SearchBuilder<QuotaTariffVO> listAllIncludedUsageType;

    public QuotaTariffDaoImpl() {
        super();
        searchUsageType = createSearchBuilder();
        searchUsageType.and("usage_type", searchUsageType.entity().getUsageType(), SearchCriteria.Op.EQ);
        searchUsageType.done();

        listAllIncludedUsageType = createSearchBuilder();
        listAllIncludedUsageType.and("include", listAllIncludedUsageType.entity().getInclude(), SearchCriteria.Op.EQ);
        listAllIncludedUsageType.done();
    }

    @Override
    public QuotaTariffVO findTariffPlanByUsageType(final int usageType) {
        short opendb = TransactionLegacy.currentTxn().getDatabaseId();
        QuotaTariffVO result = null;
        TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.USAGE_DB);
        try {
            final SearchCriteria<QuotaTariffVO> sc = searchUsageType.create();
            sc.setParameters("usage_type", usageType);
            result = findOneBy(sc);
        } finally {
            txn.close();
            // Switch back
            TransactionLegacy.open(opendb).close();
        }
        return result;
    }

    @Override
    public List<QuotaTariffVO> listAllTariffPlans() {
        short opendb = TransactionLegacy.currentTxn().getDatabaseId();
        List<QuotaTariffVO> result = null;
        TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.USAGE_DB);
        try {
            final SearchCriteria<QuotaTariffVO> sc = listAllIncludedUsageType.create();
            sc.setParameters("include", 1);
            result = listBy(sc);
        } finally {
            txn.close();
            // Switch back
            TransactionLegacy.open(opendb).close();
        }
        return result;
    }

    @Override
    public boolean updateQuotaTariff(QuotaTariffVO plan) {
        short opendb = TransactionLegacy.currentTxn().getDatabaseId();
        TransactionLegacy.open(TransactionLegacy.USAGE_DB).close(); // Switch to
                                                                    // Usage DB
        boolean result = this.update(plan.getId(), plan);
        TransactionLegacy.open(opendb).close(); // Switch back
        return result;
    }
}
