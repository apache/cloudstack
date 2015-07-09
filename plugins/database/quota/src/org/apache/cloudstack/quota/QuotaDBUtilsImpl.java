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

import java.util.Date;
import java.util.List;

import javax.ejb.Local;
import javax.inject.Inject;

import org.apache.cloudstack.api.command.QuotaEditMappingCmd;
import org.apache.cloudstack.api.command.QuotaMapping;
import org.apache.cloudstack.api.response.QuotaConfigurationResponse;
import org.apache.cloudstack.api.response.QuotaCreditsResponse;
import org.apache.cloudstack.quota.dao.QuotaConfigurationDao;
import org.apache.cloudstack.quota.dao.QuotaCreditsDao;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.utils.Pair;
import com.cloud.utils.db.TransactionLegacy;

@Component
@Local(value = QuotaDBUtilsImpl.class)
public class QuotaDBUtilsImpl {
    private static final Logger s_logger = Logger.getLogger(QuotaDBUtilsImpl.class.getName());

    @Inject
    private QuotaConfigurationDao _quotaConfigurationDao;

    @Inject
    private QuotaCreditsDao _quotaCreditsDao;

    public QuotaConfigurationResponse createQuotaConfigurationResponse(final QuotaConfigurationVO configuration) {
        final QuotaConfigurationResponse response = new QuotaConfigurationResponse();
        response.setUsageType(configuration.getUsageType());
        response.setUsageUnit(configuration.getUsageUnit());
        response.setUsageDiscriminator(configuration.getUsageDiscriminator());
        response.setCurrencyValue(configuration.getCurrencyValue());
        response.setInclude(configuration.getInclude());
        response.setDescription(configuration.getDescription());
        return response;
    }

    public Pair<List<QuotaConfigurationVO>, Integer> listConfigurations(final QuotaMapping cmd) {
        final Pair<List<QuotaConfigurationVO>, Integer> result = _quotaConfigurationDao.searchConfigurations();
        TransactionLegacy.open(TransactionLegacy.CLOUD_DB);
        return result;
    }

    public Pair<List<QuotaConfigurationVO>, Integer> editQuotaMapping(QuotaEditMappingCmd cmd) {
        String resourceName = cmd.getUsageType();
        Integer quotaMapping = cmd.getValue();

        TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.USAGE_DB);
        try {
            QuotaConfigurationVO result = _quotaConfigurationDao.findByUsageType(resourceName);
            if (result == null)
                throw new InvalidParameterValueException(resourceName);
            s_logger.info("Old value=" + result.getCurrencyValue() + ", new value = " + quotaMapping + ", for resource =" + resourceName);
            result.setCurrencyValue(quotaMapping);
            _quotaConfigurationDao.persist(result);
        } finally {
            txn.close();
        }
        final Pair<List<QuotaConfigurationVO>, Integer> result = _quotaConfigurationDao.searchConfigurations();
        TransactionLegacy.open(TransactionLegacy.CLOUD_DB);
        return result;
    }

    public QuotaCreditsResponse addQuotaCredits(Long accountId, Long domainId, Integer amount, Long updatedBy) {
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
        return new QuotaCreditsResponse(result);
    }

}
