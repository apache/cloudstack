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
package org.apache.cloudstack.quota;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.usage.UsageVO;
import com.cloud.usage.dao.UsageDao;
import com.cloud.user.User;
import com.cloud.user.dao.UserDao;
import com.cloud.utils.Pair;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.TransactionLegacy;

import org.apache.cloudstack.api.command.QuotaTariffListCmd;
import org.apache.cloudstack.api.command.QuotaTariffUpdateCmd;
import org.apache.cloudstack.api.response.QuotaTariffResponse;
import org.apache.cloudstack.api.response.QuotaCreditsResponse;
import org.apache.cloudstack.api.response.QuotaStatementBalanceResponse;
import org.apache.cloudstack.api.response.QuotaStatementItemResponse;
import org.apache.cloudstack.api.response.QuotaStatementResponse;
import org.apache.cloudstack.quota.dao.QuotaBalanceDao;
import org.apache.cloudstack.quota.dao.QuotaCreditsDao;
import org.apache.cloudstack.quota.dao.QuotaTariffDao;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import javax.ejb.Local;
import javax.inject.Inject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

@Component
@Local(value = QuotaDBUtilsImpl.class)
public class QuotaDBUtilsImpl implements QuotaDBUtils {
    private static final Logger s_logger = Logger.getLogger(QuotaDBUtilsImpl.class.getName());

    @Inject
    private QuotaTariffDao _quotaTariffDao;

    @Inject
    private QuotaCreditsDao _quotaCreditsDao;
    @Inject
    private QuotaBalanceDao _quotaBalanceDao;
    @Inject
    private ServiceOfferingDao _serviceOfferingDao;
    @Inject
    private UserDao _userDao;
    @Inject
    private UsageDao _usageDao;

    static Long s_recordtofetch = 1000L;

    @Override
    public QuotaTariffResponse createQuotaTariffResponse(QuotaTariffVO configuration) {
        final QuotaTariffResponse response = new QuotaTariffResponse();
        response.setUsageType(configuration.getUsageType());
        response.setUsageName(configuration.getUsageName());
        response.setUsageUnit(configuration.getUsageUnit());
        response.setUsageDiscriminator(configuration.getUsageDiscriminator());
        response.setTariffValue(configuration.getCurrencyValue());
        response.setInclude(configuration.getInclude());
        response.setDescription(configuration.getDescription());
        return response;
    }

    @Override
    public QuotaStatementResponse createQuotaStatementResponse(List<QuotaUsageVO> quotaUsage) {
        QuotaStatementResponse statement = new QuotaStatementResponse();
        Collections.sort(quotaUsage, new Comparator<QuotaUsageVO>() {
            public int compare(QuotaUsageVO o1, QuotaUsageVO o2) {
                if (o1.getUsageType() == o2.getUsageType())
                    return 0;
                return o1.getUsageType() < o2.getUsageType() ? -1 : 1;
            }
        });

        HashMap<Integer, QuotaTariffVO> quotaTariffMap = new HashMap<Integer, QuotaTariffVO>();
        TransactionLegacy.open(TransactionLegacy.USAGE_DB).close();
        List<QuotaTariffVO> result = _quotaTariffDao.listAll();
        for (QuotaTariffVO quotaTariff : result) {
            quotaTariffMap.put(quotaTariff.getUsageType(), quotaTariff);
        }

        List<QuotaStatementItemResponse> items = new ArrayList<QuotaStatementItemResponse>();
        QuotaStatementItemResponse lineitem;
        int type = -1;
        BigDecimal usage = new BigDecimal(0);
        BigDecimal totalUsage = new BigDecimal(0);
        quotaUsage.add(new QuotaUsageVO());// boundry
        QuotaUsageVO prev = quotaUsage.get(0);
        for (final QuotaUsageVO quotaRecord : quotaUsage) {
            if (type != quotaRecord.getUsageType()) {
                if (type != -1) {
                    lineitem = new QuotaStatementItemResponse();
                    lineitem.setUsageType(type);
                    lineitem.setQuotaUsed(usage);
                    lineitem.setAccountId(prev.getAccountId());
                    lineitem.setDomainId(prev.getDomainId());
                    lineitem.setStartDate(prev.getStartDate());
                    lineitem.setEndDate(prev.getEndDate());
                    lineitem.setUsageUnit(quotaTariffMap.get(type).getUsageUnit());
                    lineitem.setUsageName(quotaTariffMap.get(type).getUsageName());
                    lineitem.setObjectName("lineitem");
                    items.add(lineitem);
                    totalUsage = totalUsage.add(usage);
                    usage = new BigDecimal(0);
                }
                type = quotaRecord.getUsageType();
            }
            prev = quotaRecord;
            usage = usage.add(quotaRecord.getQuotaUsed());
        }

        statement.setLineItem(items);
        // calculate total quota used and balance
        QuotaStatementBalanceResponse balance = new QuotaStatementBalanceResponse();
        balance.setObjectName("balance");
        balance.setQuotaUsed(totalUsage);

        statement.setBalance(balance);
        TransactionLegacy.open(TransactionLegacy.CLOUD_DB).close();
        return statement;
    }

    @Override
    public Pair<List<QuotaTariffVO>, Integer> listQuotaTariffPlans(final QuotaTariffListCmd cmd) {
        Pair<List<QuotaTariffVO>, Integer> result = new Pair<List<QuotaTariffVO>, Integer>(new ArrayList<QuotaTariffVO>(), 0);
        if (cmd.getUsageType() != null) {
            QuotaTariffVO tariffPlan = _quotaTariffDao.findTariffPlanByUsageType(cmd.getUsageType());
            if (tariffPlan != null) {
                result.first().add(tariffPlan);
                result.second(1);
            }
        } else {
            result = _quotaTariffDao.listAllTariffPlans();
        }
        return result;
    }

    @Override
    public QuotaTariffVO updateQuotaTariffPlan(QuotaTariffUpdateCmd cmd) {
        final int resourceType = cmd.getUsageType();
        final BigDecimal quotaCost = new BigDecimal(cmd.getValue());
        QuotaTariffVO result = _quotaTariffDao.findTariffPlanByUsageType(resourceType);
        if (result == null) {
            throw new InvalidParameterValueException(String.format("Invalid Usage Resource type=%d provided", resourceType));
        }
        s_logger.debug(String.format("Updating Quota Tariff Plan: Old value=%s, new value=%s for resource type=%d", result.getCurrencyValue(), quotaCost, resourceType));
        result.setCurrencyValue(quotaCost);
        _quotaTariffDao.updateQuotaTariff(result);
        return result;
    }

    @Override
    public QuotaCreditsResponse addQuotaCredits(Long accountId, Long domainId, Double amount, Long updatedBy) {
        QuotaCreditsVO result = null;
        TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.USAGE_DB);
        try {
            QuotaCreditsVO credits = new QuotaCreditsVO(accountId, domainId, new BigDecimal(amount), updatedBy);
            credits.setUpdatedOn(new Date());
            result = _quotaCreditsDao.saveCredits(credits);
        } finally {
            txn.close();
        }
        TransactionLegacy.open(TransactionLegacy.CLOUD_DB).close();
        String creditor = "1";
        User creditorUser = _userDao.getUser(updatedBy);
        if (creditorUser != null) {
            creditor = creditorUser.getUsername();
        }
        return new QuotaCreditsResponse(result, creditor);
    }

    @Override
    @SuppressWarnings("deprecation")
    public Pair<List<? extends UsageVO>, Integer> getUsageRecords(long accountId, long domainId) {
        s_logger.debug("getting usage records for account: " + accountId + ", domainId: " + domainId);
        Filter usageFilter = new Filter(UsageVO.class, "startDate", true, 0L, s_recordtofetch);
        SearchCriteria<UsageVO> sc = _usageDao.createSearchCriteria();
        if (accountId != -1) {
            sc.addAnd("accountId", SearchCriteria.Op.EQ, accountId);
        }
        if (domainId != -1) {
            sc.addAnd("domainId", SearchCriteria.Op.EQ, domainId);
        }
        sc.addAnd("quotaCalculated", SearchCriteria.Op.EQ, 0);
        s_logger.debug("Getting usage records" + usageFilter.getOrderBy());
        Pair<List<UsageVO>, Integer> usageRecords = _usageDao.searchAndCountAllRecords(sc, usageFilter);
        return new Pair<List<? extends UsageVO>, Integer>(usageRecords.first(), usageRecords.second());
    }

    @Override
    public ServiceOfferingVO findServiceOffering(Long vmId, long serviceOfferingId) {
        TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.CLOUD_DB);
        ServiceOfferingVO result;
        try {
            result = _serviceOfferingDao.findById(vmId, serviceOfferingId);
        } finally {
            txn.close();
        }
        TransactionLegacy.open(TransactionLegacy.USAGE_DB);
        return result;
    }

}
