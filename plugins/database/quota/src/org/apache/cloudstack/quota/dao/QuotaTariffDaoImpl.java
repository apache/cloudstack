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

import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.TransactionLegacy;

import org.apache.cloudstack.api.response.QuotaTypeResponse;
import org.apache.cloudstack.quota.QuotaTariffVO;
import org.apache.cloudstack.quota.QuotaTypes;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import javax.ejb.Local;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Component
@Local(value = { QuotaTariffDao.class })
public class QuotaTariffDaoImpl extends GenericDaoBase<QuotaTariffVO, Long> implements QuotaTariffDao {
    private static final Logger s_logger = Logger.getLogger(QuotaTariffDaoImpl.class.getName());

    private final SearchBuilder<QuotaTariffVO> searchUsageType;
    private final SearchBuilder<QuotaTariffVO> listAllIncludedUsageType;

    public QuotaTariffDaoImpl() {
        super();
        searchUsageType = createSearchBuilder();
        searchUsageType.and("usage_type", searchUsageType.entity().getUsageType(), SearchCriteria.Op.EQ);
        searchUsageType.done();

        listAllIncludedUsageType = createSearchBuilder();
        listAllIncludedUsageType.and("include", listAllIncludedUsageType.entity().getInclude(), SearchCriteria.Op.EQ);
        listAllIncludedUsageType.and("onorbeforedate", listAllIncludedUsageType.entity().getEffectiveOn(), SearchCriteria.Op.LTEQ);
        listAllIncludedUsageType.and("quotatype", listAllIncludedUsageType.entity().getUsageType(), SearchCriteria.Op.EQ);
        listAllIncludedUsageType.done();
    }

    @Override
    public QuotaTariffVO findTariffPlanByUsageType(final int quotaType, final Date onOrBeforeDate) {
        final short opendb = TransactionLegacy.currentTxn().getDatabaseId();
        List<QuotaTariffVO> result = null;
        TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.USAGE_DB);
        try {
            final Filter filter = new Filter(QuotaTariffVO.class, "effectiveOn", false, 0L, 1L);
            final SearchCriteria<QuotaTariffVO> sc = listAllIncludedUsageType.create();
            sc.setParameters("include", 1);
            sc.setParameters("onorbeforedate", onOrBeforeDate);
            sc.setParameters("quotatype", quotaType);
            result = search(sc, filter);
        } finally {
            txn.close();
        }
        // Switch back
        TransactionLegacy.open(opendb).close();
        if (result.size() > 0) {
            //s_logger.info(onOrBeforeDate.toGMTString() + "quota type " + quotaType + " , effective Date=" + result.get(0).getEffectiveOn() + " val=" + result.get(0).getCurrencyValue());
            return result.get(0);
        } else {
            s_logger.info("Missing quota type " + quotaType);
            return null;
        }
    }

    @Override
    public List<QuotaTariffVO> listAllTariffPlans(final Date onOrBeforeDate) {
        final short opendb = TransactionLegacy.currentTxn().getDatabaseId();
        List<QuotaTariffVO> tariffs = new ArrayList<QuotaTariffVO>();
        TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.USAGE_DB);
        try {
            final Filter filter = new Filter(QuotaTariffVO.class, "effectiveOn", false, 0L, 1L);
            final SearchCriteria<QuotaTariffVO> sc = listAllIncludedUsageType.create();
            sc.setParameters("include", 1);
            sc.setParameters("onorbeforedate", onOrBeforeDate);
            for (QuotaTypeResponse resp : QuotaTypes.listQuotaUsageTypes()) {
                sc.setParameters("quotatype", resp.getQuotaType());
                List<QuotaTariffVO> result = search(sc, filter);
                if (result.size() > 0) {
                    tariffs.add(result.get(0));
                    //s_logger.info(onOrBeforeDate.toGMTString() + "quota type " + resp.getDescription() + " , effective Date=" + result.get(0).getEffectiveOn() + " val="
                            //+ result.get(0).getCurrencyValue());
                }
            }
        } finally {
            txn.close();
        }
        // Switch back
        TransactionLegacy.open(opendb).close();
        return tariffs;
    }

    @Override
    public boolean updateQuotaTariff(QuotaTariffVO plan) {
        final short opendb = TransactionLegacy.currentTxn().getDatabaseId();
        TransactionLegacy.open(TransactionLegacy.USAGE_DB).close(); // Switch to
                                                                    // Usage DB
        boolean result = this.update(plan.getId(), plan);
        TransactionLegacy.open(opendb).close(); // Switch back
        return result;
    }

    @Override
    public QuotaTariffVO addQuotaTariff(QuotaTariffVO plan) {
        final short opendb = TransactionLegacy.currentTxn().getDatabaseId();
        TransactionLegacy.open(TransactionLegacy.USAGE_DB).close(); // Switch to
        // Usage DB
        plan.setId(null);
        QuotaTariffVO result = this.persist(plan);
        TransactionLegacy.open(opendb).close(); // Switch back
        return result;
    }
}
