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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import javax.ejb.Local;
import javax.inject.Inject;

import org.apache.cloudstack.api.command.ListQuotaConfigurationsCmd;
import org.apache.cloudstack.api.command.QuotaCreditsCmd;
import org.apache.cloudstack.api.command.QuotaEditResourceMappingCmd;
import org.apache.cloudstack.api.command.QuotaEmailTemplateAddCmd;
import org.apache.cloudstack.api.command.QuotaRefreshCmd;
import org.apache.cloudstack.api.command.QuotaStatementCmd;
import org.apache.cloudstack.api.response.QuotaConfigurationResponse;
import org.apache.cloudstack.api.response.QuotaCreditsResponse;
import org.apache.cloudstack.quota.dao.QuotaConfigurationDao;
import org.apache.cloudstack.quota.dao.QuotaCreditsDao;
import org.apache.cloudstack.quota.dao.QuotaJobDao;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.usage.UsageJobVO;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.TransactionLegacy;

@Component
@Local(value = QuotaManager.class)
public class QuotaManagerImpl extends ManagerBase implements QuotaManager {
    private static final Logger s_logger = Logger
            .getLogger(QuotaManagerImpl.class.getName());

    @Inject
    private QuotaConfigurationDao _quotaConfigurationDao;

    @Inject
    private QuotaCreditsDao _quotaCreditsDao;

    @Inject
    private QuotaJobDao _quotaJobDao;

    String _hostname = null;
    int _pid = 0;
    TimeZone _usageTimezone = TimeZone.getTimeZone("GMT");

    public QuotaManagerImpl() {
        super();
    }

    public QuotaManagerImpl(final QuotaConfigurationDao quotaConfigurationDao) {
        super();
        _quotaConfigurationDao = quotaConfigurationDao;
    }

    @Override
    public List<Class<?>> getCommands() {
        final List<Class<?>> cmdList = new ArrayList<Class<?>>();
        cmdList.add(ListQuotaConfigurationsCmd.class);
        cmdList.add(QuotaCreditsCmd.class);
        cmdList.add(QuotaEmailTemplateAddCmd.class);
        cmdList.add(QuotaRefreshCmd.class);
        cmdList.add(QuotaStatementCmd.class);
        cmdList.add(QuotaEditResourceMappingCmd.class);
        return cmdList;
    }

    @Override
    public Pair<List<QuotaConfigurationVO>, Integer> listConfigurations(
            final ListQuotaConfigurationsCmd cmd) {
        final Pair<List<QuotaConfigurationVO>, Integer> result = _quotaConfigurationDao
                .searchConfigurations();
        TransactionLegacy.open(TransactionLegacy.CLOUD_DB);
        return result;
    }

    @Override
    public QuotaConfigurationResponse createQuotaConfigurationResponse(
            final QuotaConfigurationVO configuration) {
        final QuotaConfigurationResponse response = new QuotaConfigurationResponse();
        response.setUsageType(configuration.getUsageType());
        response.setUsageUnit(configuration.getUsageUnit());
        response.setUsageDiscriminator(configuration.getUsageDiscriminator());
        response.setCurrencyValue(configuration.getCurrencyValue());
        response.setInclude(configuration.getInclude());
        response.setDescription(configuration.getDescription());
        return response;
    }

    @Override
    public Pair<List<QuotaConfigurationVO>, Integer> editQuotaMapping(
            QuotaEditResourceMappingCmd cmd) {
        String resourceName = cmd.getUsageType();
        Integer quotaMapping = cmd.getValue();

        TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.USAGE_DB);
        try {
            QuotaConfigurationVO result = _quotaConfigurationDao.findByUsageType(resourceName);
            if (result==null) throw new InvalidParameterValueException(resourceName);
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


    @Override
    public QuotaCreditsResponse addQuotaCredits(Long accountId, Long domainId,
            Integer amount, Long updatedBy) {
        QuotaCreditsVO result = null;
        TransactionLegacy txn = TransactionLegacy
                .open(TransactionLegacy.USAGE_DB);
        try {
            QuotaCreditsVO credits = new QuotaCreditsVO(accountId, domainId,
                    amount, updatedBy);
            credits.setUpdatedOn(new Date());
            result = _quotaCreditsDao.persist(credits);
        } finally {
            txn.close();
        }
        TransactionLegacy.open(TransactionLegacy.CLOUD_DB);
        return new QuotaCreditsResponse(result);
    }

    @Override
    public void calculateQuotaUsage(QuotaJobVO job, long startDateMillis,
            long endDateMillis) {

        boolean success = false;
        long timeStart = System.currentTimeMillis();
        try {
            if ((endDateMillis == 0) || (endDateMillis > timeStart)) {
                endDateMillis = timeStart;
            }

            long lastSuccess = _quotaJobDao.getLastJobSuccessDateMillis();
            if (lastSuccess != 0) {
                startDateMillis = lastSuccess + 1; // 1 millisecond after
            }

            if (startDateMillis >= endDateMillis) {
                if (s_logger.isInfoEnabled()) {
                    s_logger.info("not parsing usage records since start time mills (" + startDateMillis + ") is on or after end time millis (" + endDateMillis + ")");
                }

                TransactionLegacy jobUpdateTxn = TransactionLegacy.open(TransactionLegacy.USAGE_DB);
                try {
                    jobUpdateTxn.start();
                    // everything seemed to work...set endDate as the last success date
                    _quotaJobDao.updateJobSuccess(job.getId(), startDateMillis, endDateMillis, System.currentTimeMillis() - timeStart, success);

                    // create a new job if this is a recurring job
                    if (job.getJobType() == UsageJobVO.JOB_TYPE_RECURRING) {
                        _quotaJobDao.createNewJob(_hostname, _pid, UsageJobVO.JOB_TYPE_RECURRING);
                    }
                    jobUpdateTxn.commit();
                } finally {
                    jobUpdateTxn.close();
                }

                return;
            }

            Date startDate = new Date(startDateMillis);
            Date endDate = new Date(endDateMillis);
            if (s_logger.isInfoEnabled()) {
                s_logger.info("Calculating quota usage for records between " + startDate + " and " + endDate);
            }

            //get all the accounts from usage db
            TransactionLegacy userTxn = TransactionLegacy.open(TransactionLegacy.USAGE_DB);
        } catch (Exception e) {
            s_logger.error("Quota Manager error", e);
        }
    }

}
