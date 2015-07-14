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
import com.cloud.user.User;
import com.cloud.user.dao.UserDao;
import com.cloud.utils.Pair;
import com.cloud.utils.db.TransactionLegacy;
import org.apache.cloudstack.api.command.QuotaEditMappingCmd;
import org.apache.cloudstack.api.command.QuotaMappingCmd;
import org.apache.cloudstack.api.response.QuotaConfigurationResponse;
import org.apache.cloudstack.api.response.QuotaCreditsResponse;
import org.apache.cloudstack.api.response.QuotaStatementResponse;
import org.apache.cloudstack.quota.dao.QuotaCreditsDao;
import org.apache.cloudstack.quota.dao.QuotaMappingDao;
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
    private QuotaMappingDao _quotaMappingDao;

    @Inject
    private QuotaCreditsDao _quotaCreditsDao;

    @Inject
    private UserDao _userDao;

    @Override
    public QuotaConfigurationResponse createQuotaConfigurationResponse(QuotaMappingVO configuration) {
        final QuotaConfigurationResponse response = new QuotaConfigurationResponse();
        response.setUsageType(configuration.getUsageType());
        response.setUsageName(configuration.getUsageName());
        response.setUsageUnit(configuration.getUsageUnit());
        response.setUsageDiscriminator(configuration.getUsageDiscriminator());
        response.setCurrencyValue(configuration.getCurrencyValue());
        response.setInclude(configuration.getInclude());
        response.setDescription(configuration.getDescription());
        return response;
    }

    @Override
    public List<QuotaStatementResponse> createQuotaStatementResponse(List<QuotaUsageVO> quotaUsage) {
        TransactionLegacy.open(TransactionLegacy.USAGE_DB);
        Collections.sort(quotaUsage, new Comparator<QuotaUsageVO>() {
            public int compare(QuotaUsageVO o1, QuotaUsageVO o2) {
                if (o1.getUsageType() == o2.getUsageType())
                    return 0;
                return o1.getUsageType() < o2.getUsageType() ? -1 : 1;
            }
        });

        HashMap<Integer, String> map = new HashMap<Integer, String>();
        List<QuotaMappingVO> result = _quotaMappingDao.listAll();
        for (QuotaMappingVO mapping : result) {
            map.put(mapping.getUsageType(), mapping.getUsageUnit());
        }

        List<QuotaStatementResponse> statement = new ArrayList<QuotaStatementResponse>();
        QuotaStatementResponse lineitem;
        int type = -1;
        BigDecimal totalUsage = new BigDecimal(0);
        for (final QuotaUsageVO quotaRecord : quotaUsage) {
            if (type != quotaRecord.getUsageType()) {
                if (type != -1) {
                    lineitem = new QuotaStatementResponse();
                    lineitem.setUsageType(type);
                    lineitem.setQuotaUsed(totalUsage);
                    lineitem.setUsageUnit(map.get(type));
                    statement.add(lineitem);
                    totalUsage = new BigDecimal(0);
                }
                type = quotaRecord.getUsageType();
            }
            totalUsage = totalUsage.add(quotaRecord.getQuotaUsed());
        }

        TransactionLegacy.open(TransactionLegacy.CLOUD_DB);
        return statement;
    }

    @Override
    public Pair<List<QuotaMappingVO>, Integer> listConfigurations(final QuotaMappingCmd cmd) {
        final Pair<List<QuotaMappingVO>, Integer> result = _quotaMappingDao.listAllMapping();
        TransactionLegacy.open(TransactionLegacy.CLOUD_DB);
        return result;
    }

    @Override
    public Pair<List<QuotaMappingVO>, Integer> editQuotaMapping(QuotaEditMappingCmd cmd) {
        int resourceType = cmd.getUsageType();
        BigDecimal quotaCost = new BigDecimal(cmd.getValue());

        TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.USAGE_DB);
        try {
            QuotaMappingVO result = _quotaMappingDao.findByUsageType(resourceType);
            if (result == null)
                throw new InvalidParameterValueException("Resource type " + resourceType);
            s_logger.info("Old value=" + result.getCurrencyValue() + ", new value = " + quotaCost + ", for resource =" + resourceType);
            result.setCurrencyValue(quotaCost);
            _quotaMappingDao.persist(result);
        } finally {
            txn.close();
        }
        final Pair<List<QuotaMappingVO>, Integer> result = _quotaMappingDao.listAllMapping();
        TransactionLegacy.open(TransactionLegacy.CLOUD_DB);
        return result;
    }

    @Override
    public QuotaCreditsResponse addQuotaCredits(Long accountId, Long domainId, String amount, Long updatedBy) {
        QuotaCreditsVO result = null;
        TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.USAGE_DB);
        try {
            QuotaCreditsVO credits = new QuotaCreditsVO(accountId, domainId, amount, updatedBy);
            credits.setUpdatedOn(new Date());
            result = _quotaCreditsDao.persist(credits);
        } finally {
            txn.close();
        }
        TransactionLegacy.open(TransactionLegacy.CLOUD_DB);
        String creditor = "1";
        User creditorUser = _userDao.getUser(updatedBy);
        if (creditorUser != null) {
            creditor = creditorUser.getUsername();
        }
        return new QuotaCreditsResponse(result, creditor);
    }

}
