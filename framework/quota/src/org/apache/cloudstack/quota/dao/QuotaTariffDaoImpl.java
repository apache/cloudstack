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
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.utils.db.TransactionStatus;
import org.apache.cloudstack.quota.constant.QuotaTypes;
import org.apache.cloudstack.quota.vo.QuotaTariffVO;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import javax.ejb.Local;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Component
@Local(value = {QuotaTariffDao.class})
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
        listAllIncludedUsageType.and("onorbefore", listAllIncludedUsageType.entity().getEffectiveOn(), SearchCriteria.Op.LTEQ);
        listAllIncludedUsageType.and("quotatype", listAllIncludedUsageType.entity().getUsageType(), SearchCriteria.Op.EQ);
        listAllIncludedUsageType.done();
    }

    public QuotaTariffVO findTariffPlanByUsageType(final int quotaType, final Date effectiveDate) {
        return Transaction.execute(TransactionLegacy.USAGE_DB, new TransactionCallback<QuotaTariffVO>() {
            @Override
            public QuotaTariffVO doInTransaction(final TransactionStatus status) {
                List<QuotaTariffVO> result = new ArrayList<>();
                final Filter filter = new Filter(QuotaTariffVO.class, "updatedOn", false, 0L, 1L);
                final SearchCriteria<QuotaTariffVO> sc = listAllIncludedUsageType.create();
                sc.setParameters("onorbefore", effectiveDate);
                sc.setParameters("quotatype", quotaType);
                result = search(sc, filter);
                if (result != null && !result.isEmpty()) {
                    return result.get(0);
                } else {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("QuotaTariffDaoImpl::findTariffPlanByUsageType: Missing quota type " + quotaType);
                    }
                    return null;
                }
            }
        });
    }

    public List<QuotaTariffVO> listAllTariffPlans() {
        return Transaction.execute(TransactionLegacy.USAGE_DB, new TransactionCallback<List<QuotaTariffVO>>() {
            @Override
            public List<QuotaTariffVO> doInTransaction(final TransactionStatus status) {
                return listAll();
            }
        });
    }

    public List<QuotaTariffVO> listAllTariffPlans(final Date effectiveDate) {
        return Transaction.execute(TransactionLegacy.USAGE_DB, new TransactionCallback<List<QuotaTariffVO>>() {
            @Override
            public List<QuotaTariffVO> doInTransaction(final TransactionStatus status) {
                List<QuotaTariffVO> tariffs = new ArrayList<QuotaTariffVO>();
                final Filter filter = new Filter(QuotaTariffVO.class, "updatedOn", false, 0L, 1L);
                final SearchCriteria<QuotaTariffVO> sc = listAllIncludedUsageType.create();
                sc.setParameters("onorbefore", effectiveDate);
                for (Integer quotaType : QuotaTypes.listQuotaTypes().keySet()) {
                    sc.setParameters("quotatype", quotaType);
                    List<QuotaTariffVO> result = search(sc, filter);
                    if (result != null && !result.isEmpty()) {
                        tariffs.add(result.get(0));
                        if (s_logger.isDebugEnabled()) {
                            s_logger.debug("ListAllTariffPlans on or before " + effectiveDate + " quota type " + result.get(0).getDescription() + " , effective Date="
                                    + result.get(0).getEffectiveOn() + " val=" + result.get(0).getCurrencyValue());
                        }
                    }
                }
                return tariffs;
            }
        });
    }

    public Boolean updateQuotaTariff(final QuotaTariffVO plan) {
        return Transaction.execute(TransactionLegacy.USAGE_DB, new TransactionCallback<Boolean>() {
            @Override
            public Boolean doInTransaction(final TransactionStatus status) {
                return update(plan.getId(), plan);
            }
        });
    }

    public QuotaTariffVO addQuotaTariff(final QuotaTariffVO plan) {
        if (plan.getIdObj() != null) {
            throw new IllegalStateException("The QuotaTariffVO being added should not have an Id set ");
        }
        return Transaction.execute(TransactionLegacy.USAGE_DB, new TransactionCallback<QuotaTariffVO>() {
            @Override
            public QuotaTariffVO doInTransaction(final TransactionStatus status) {
                return persist(plan);
            }
        });
    }
}
