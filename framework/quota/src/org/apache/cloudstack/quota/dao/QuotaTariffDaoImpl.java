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

import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.quota.constant.QuotaTypes;
import org.apache.cloudstack.quota.vo.QuotaTariffVO;
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
        listAllIncludedUsageType.and("onorbefore", listAllIncludedUsageType.entity().getEffectiveOn(), SearchCriteria.Op.LTEQ);
        listAllIncludedUsageType.and("quotatype", listAllIncludedUsageType.entity().getUsageType(), SearchCriteria.Op.EQ);
        listAllIncludedUsageType.done();
    }

    @Override
    public QuotaTariffVO findTariffPlanByUsageType(final int quotaType, final Date effectiveDate) {
        final short opendb = TransactionLegacy.currentTxn().getDatabaseId();
        List<QuotaTariffVO> result = new ArrayList<>();
        try (TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.USAGE_DB)) {
            final Filter filter = new Filter(QuotaTariffVO.class, "effectiveOn", false, 0L, 1L);
            final SearchCriteria<QuotaTariffVO> sc = listAllIncludedUsageType.create();
            sc.setParameters("onorbefore", effectiveDate);
            sc.setParameters("quotatype", quotaType);
            result = search(sc, filter);
        } catch (Exception e) {
            throw new CloudRuntimeException("Unable to find tariff plan by usage type");
        } finally {
            TransactionLegacy.open(opendb).close();
        }
        if (result.size() > 0) {
            if (s_logger.isDebugEnabled()){
                s_logger.debug("QuotaTariffDaoImpl::findTariffPlanByUsageType: " + effectiveDate + "quota type " + quotaType  + " val=" + result.get(0).getCurrencyValue());
            }
            return result.get(0);
        } else {
            if (s_logger.isDebugEnabled()){
                s_logger.info("QuotaTariffDaoImpl::findTariffPlanByUsageType: Missing quota type " + quotaType);
            }
            return null;
        }
    }

    @Override
    public List<QuotaTariffVO> listAllTariffPlans(final Date effectiveDate) {
        final short opendb = TransactionLegacy.currentTxn().getDatabaseId();
        List<QuotaTariffVO> tariffs = new ArrayList<QuotaTariffVO>();
        try (TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.USAGE_DB)) {
            final Filter filter = new Filter(QuotaTariffVO.class, "effectiveOn", false, 0L, 1L);
            final SearchCriteria<QuotaTariffVO> sc = listAllIncludedUsageType.create();
            sc.setParameters("onorbefore", effectiveDate);
            for (Integer quotaType : QuotaTypes.listQuotaTypes().keySet()) {
                sc.setParameters("quotatype", quotaType);
                List<QuotaTariffVO> result = search(sc, filter);
                if (result.size() > 0) {
                    tariffs.add(result.get(0));
                    s_logger.info("listAllTariffPlans onorbefore" + effectiveDate + "quota type " + result.get(0).getDescription() + " , effective Date=" + result.get(0).getEffectiveOn() + " val=" + result.get(0).getCurrencyValue());
                }
            }
        } catch (Exception e) {
            throw new CloudRuntimeException("Unable to list all tariff plans");
        } finally {
            TransactionLegacy.open(opendb).close();
        }
        return tariffs;
    }

    @Override
    public boolean updateQuotaTariff(QuotaTariffVO plan) {
        final short opendb = TransactionLegacy.currentTxn().getDatabaseId();
        boolean result = false;
        try (TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.USAGE_DB)) {
            result = this.update(plan.getId(), plan);
        } catch (Exception e) {
            throw new CloudRuntimeException("Unable to update quota tariff");
        } finally {
            TransactionLegacy.open(opendb).close();
        }
        return result;
    }

    @Override
    public QuotaTariffVO addQuotaTariff(QuotaTariffVO plan) {
        final short opendb = TransactionLegacy.currentTxn().getDatabaseId();
        QuotaTariffVO result = null;
        try (TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.USAGE_DB)) {
            plan.setId(null);
            result = this.persist(plan);
        } catch (Exception e) {
            throw new CloudRuntimeException("Unable to save quota tariff");
        } finally {
            TransactionLegacy.open(opendb).close();
        }
        return result;
    }
}
