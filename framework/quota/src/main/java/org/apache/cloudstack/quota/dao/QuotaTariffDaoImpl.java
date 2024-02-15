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
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.cloudstack.quota.constant.QuotaTypes;
import org.apache.cloudstack.quota.vo.QuotaTariffVO;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Component;

import com.cloud.utils.Pair;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.utils.db.TransactionStatus;

@Component
public class QuotaTariffDaoImpl extends GenericDaoBase<QuotaTariffVO, Long> implements QuotaTariffDao {

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

    @Override
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
                    if (logger.isDebugEnabled()) {
                        logger.debug("QuotaTariffDaoImpl::findTariffPlanByUsageType: Missing quota type " + quotaType);
                    }
                    return null;
                }
            }
        });
    }

    @Override
    public Pair<List<QuotaTariffVO>, Integer> listAllTariffPlans() {
        return listAllTariffPlans(null, null);
    }

    @Override
    public Pair<List<QuotaTariffVO>, Integer> listAllTariffPlans(final Long startIndex, final Long pageSize) {
        return Transaction.execute(TransactionLegacy.USAGE_DB, new TransactionCallback<Pair<List<QuotaTariffVO>, Integer>>() {
            @Override
            public Pair<List<QuotaTariffVO>, Integer> doInTransaction(final TransactionStatus status) {
                return searchAndCount(null, new Filter(QuotaTariffVO.class, "updatedOn", false, startIndex, pageSize));
            }
        });
    }


    private <T> List<T> paginateList(final List<T> list, final Long startIndex, final Long pageSize) {
        if (startIndex == null || pageSize == null) {
            return list;
        }
        if (list.size() < startIndex){
            return Collections.emptyList();
        }
        return list.subList(startIndex.intValue(), (int) Math.min(startIndex + pageSize, list.size()));
    }

    @Override
    public Pair<List<QuotaTariffVO>, Integer> listAllTariffPlans(final Date effectiveDate) {
        return listAllTariffPlans(effectiveDate, null, null);
    }

    @Override
    public Pair<List<QuotaTariffVO>, Integer> listAllTariffPlans(final Date effectiveDate, final Long startIndex, final Long pageSize) {
        return Transaction.execute(TransactionLegacy.USAGE_DB, new TransactionCallback<Pair<List<QuotaTariffVO>, Integer>>() {
            @Override
            public Pair<List<QuotaTariffVO>, Integer> doInTransaction(final TransactionStatus status) {
                List<QuotaTariffVO> tariffs = new ArrayList<QuotaTariffVO>();
                final Filter filter = new Filter(QuotaTariffVO.class, "updatedOn", false, 0L, 1L);
                final SearchCriteria<QuotaTariffVO> sc = listAllIncludedUsageType.create();
                sc.setParameters("onorbefore", effectiveDate);
                for (Integer quotaType : QuotaTypes.listQuotaTypes().keySet()) {
                    sc.setParameters("quotatype", quotaType);
                    List<QuotaTariffVO> result = search(sc, filter);
                    if (result != null && !result.isEmpty()) {
                        tariffs.add(result.get(0));
                        if (logger.isDebugEnabled()) {
                            logger.debug("ListAllTariffPlans on or before " + effectiveDate + " quota type " + result.get(0).getUsageTypeDescription() + " , effective Date="
                                    + result.get(0).getEffectiveOn() + " val=" + result.get(0).getCurrencyValue());
                        }
                    }
                }
                return new Pair<>(paginateList(tariffs, startIndex, pageSize), tariffs.size());
            }
        });
    }

    @Override
    public Boolean updateQuotaTariff(final QuotaTariffVO plan) {
        return Transaction.execute(TransactionLegacy.USAGE_DB, new TransactionCallback<Boolean>() {
            @Override
            public Boolean doInTransaction(final TransactionStatus status) {
                return update(plan.getId(), plan);
            }
        });
    }

    @Override
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

    @Override
    public Pair<List<QuotaTariffVO>, Integer> listQuotaTariffs(Date startDate, Date endDate, Integer usageType, String name, String uuid, boolean listAll, Long startIndex, Long pageSize) {
        SearchCriteria<QuotaTariffVO> searchCriteria = createListQuotaTariffsSearchCriteria(startDate, endDate, usageType, name, uuid);
        Filter sorter = new Filter(QuotaTariffVO.class, "usageType", false, startIndex, pageSize);
        sorter.addOrderBy(QuotaTariffVO.class, "effectiveOn", false);
        sorter.addOrderBy(QuotaTariffVO.class, "updatedOn", false);

        return Transaction.execute(TransactionLegacy.USAGE_DB, (TransactionCallback<Pair<List<QuotaTariffVO>, Integer>>) status -> searchAndCount(searchCriteria, sorter, listAll));
    }

    protected SearchCriteria<QuotaTariffVO> createListQuotaTariffsSearchCriteria(Date startDate, Date endDate, Integer usageType, String name, String uuid) {
        SearchCriteria<QuotaTariffVO> searchCriteria = createListQuotaTariffsSearchBuilder(startDate, endDate, usageType, name, uuid).create();

        searchCriteria.setParametersIfNotNull("start_date", startDate);
        searchCriteria.setParametersIfNotNull("end_date", endDate);
        searchCriteria.setParametersIfNotNull("usage_type", usageType);
        searchCriteria.setParametersIfNotNull("name", name);
        searchCriteria.setParametersIfNotNull("uuid", uuid);

        return searchCriteria;
    }

    protected SearchBuilder<QuotaTariffVO> createListQuotaTariffsSearchBuilder(Date startDate, Date endDate, Integer usageType, String name, String uuid) {
        SearchBuilder<QuotaTariffVO> searchBuilder = createSearchBuilder();

        if (startDate != null) {
            searchBuilder.and("start_date", searchBuilder.entity().getEffectiveOn(), SearchCriteria.Op.GTEQ);
        }

        if (endDate != null) {
            searchBuilder.and("end_date", searchBuilder.entity().getEndDate(), SearchCriteria.Op.LTEQ);
        }

        if (usageType != null) {
            searchBuilder.and("usage_type", searchBuilder.entity().getUsageType(), SearchCriteria.Op.EQ);
        }

        if (name != null) {
            searchBuilder.and("name", searchBuilder.entity().getName(), SearchCriteria.Op.EQ);
        }

        if (uuid != null) {
            searchBuilder.and("uuid", searchBuilder.entity().getUuid(), SearchCriteria.Op.EQ);
        }

        return searchBuilder;
    }

    @Override
    public QuotaTariffVO findByName(String name) {
        Pair<List<QuotaTariffVO>, Integer> pairQuotaTariffs = listQuotaTariffs(null, null, null, name, null, false, null, null);
        List<QuotaTariffVO> quotaTariffs = pairQuotaTariffs.first();

        if (CollectionUtils.isEmpty(quotaTariffs)) {
            logger.debug(String.format("Could not find quota tariff with name [%s].", name));
            return null;
        }

        return quotaTariffs.get(0);
    }

    @Override
    public QuotaTariffVO findByUuid(String uuid) {
        Pair<List<QuotaTariffVO>, Integer> pairQuotaTariffs = listQuotaTariffs(null, null, null, null, uuid, false, null, null);
        List<QuotaTariffVO> quotaTariffs = pairQuotaTariffs.first();

        if (CollectionUtils.isEmpty(quotaTariffs)) {
            logger.debug(String.format("Could not find quota tariff with UUID [%s].", uuid));
            return null;
        }

        return quotaTariffs.get(0);
    }
}
