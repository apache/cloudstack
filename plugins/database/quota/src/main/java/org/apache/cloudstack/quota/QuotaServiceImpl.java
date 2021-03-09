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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.api.command.QuotaBalanceCmd;
import org.apache.cloudstack.api.command.QuotaCreditsCmd;
import org.apache.cloudstack.api.command.QuotaEmailTemplateListCmd;
import org.apache.cloudstack.api.command.QuotaEmailTemplateUpdateCmd;
import org.apache.cloudstack.api.command.QuotaEnabledCmd;
import org.apache.cloudstack.api.command.QuotaStatementCmd;
import org.apache.cloudstack.api.command.QuotaSummaryCmd;
import org.apache.cloudstack.api.command.QuotaTariffListCmd;
import org.apache.cloudstack.api.command.QuotaTariffUpdateCmd;
import org.apache.cloudstack.api.command.QuotaUpdateCmd;
import org.apache.cloudstack.api.response.QuotaResponseBuilder;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.quota.constant.QuotaConfig;
import org.apache.cloudstack.quota.dao.QuotaAccountDao;
import org.apache.cloudstack.quota.dao.QuotaBalanceDao;
import org.apache.cloudstack.quota.dao.QuotaUsageDao;
import org.apache.cloudstack.quota.vo.QuotaAccountVO;
import org.apache.cloudstack.quota.vo.QuotaBalanceVO;
import org.apache.cloudstack.quota.vo.QuotaUsageVO;
import org.apache.cloudstack.utils.usage.UsageUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.configuration.Config;
import com.cloud.domain.dao.DomainDao;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.user.Account;
import com.cloud.user.AccountVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.Filter;

@Component
public class QuotaServiceImpl extends ManagerBase implements QuotaService, Configurable, QuotaConfig {
    private static final Logger s_logger = Logger.getLogger(QuotaServiceImpl.class);

    @Inject
    private AccountDao _accountDao;
    @Inject
    private QuotaAccountDao _quotaAcc;
    @Inject
    private QuotaUsageDao _quotaUsageDao;
    @Inject
    private DomainDao _domainDao;
    @Inject
    private ConfigurationDao _configDao;
    @Inject
    private QuotaBalanceDao _quotaBalanceDao;
    @Inject
    private QuotaResponseBuilder _respBldr;

    private TimeZone _usageTimezone;
    private int _aggregationDuration = 0;

    public QuotaServiceImpl() {
        super();
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);
        String timeZoneStr = _configDao.getValue(Config.UsageAggregationTimezone.toString());
        String aggregationRange = _configDao.getValue(Config.UsageStatsJobAggregationRange.toString());
        if (timeZoneStr == null) {
            timeZoneStr = "GMT";
        }
        _usageTimezone = TimeZone.getTimeZone(timeZoneStr);

        _aggregationDuration = Integer.parseInt(aggregationRange);
        if (_aggregationDuration < UsageUtils.USAGE_AGGREGATION_RANGE_MIN) {
            s_logger.warn("Usage stats job aggregation range is to small, using the minimum value of " + UsageUtils.USAGE_AGGREGATION_RANGE_MIN);
            _aggregationDuration = UsageUtils.USAGE_AGGREGATION_RANGE_MIN;
        }
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Usage timezone = " + _usageTimezone + " AggregationDuration=" + _aggregationDuration);
        }
        return true;
    }

    @Override
    public List<Class<?>> getCommands() {
        final List<Class<?>> cmdList = new ArrayList<Class<?>>();
        cmdList.add(QuotaEnabledCmd.class);
        if (!isQuotaServiceEnabled()) {
            return cmdList;
        }
        cmdList.add(QuotaStatementCmd.class);
        cmdList.add(QuotaBalanceCmd.class);
        cmdList.add(QuotaSummaryCmd.class);
        cmdList.add(QuotaUpdateCmd.class);
        cmdList.add(QuotaTariffListCmd.class);
        cmdList.add(QuotaTariffUpdateCmd.class);
        cmdList.add(QuotaCreditsCmd.class);
        cmdList.add(QuotaEmailTemplateListCmd.class);
        cmdList.add(QuotaEmailTemplateUpdateCmd.class);
        return cmdList;
    }

    @Override
    public String getConfigComponentName() {
        return "QUOTA-PLUGIN";
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[] {QuotaPluginEnabled, QuotaEnableEnforcement, QuotaCurrencySymbol, QuotaStatementPeriod, QuotaSmtpHost, QuotaSmtpPort, QuotaSmtpTimeout,
                QuotaSmtpUser, QuotaSmtpPassword, QuotaSmtpAuthType, QuotaSmtpSender, QuotaSmtpEnabledSecurityProtocols, QuotaSmtpUseStartTLS};
    }

    @Override
    public Boolean isQuotaServiceEnabled() {
        return QuotaPluginEnabled.value();
    }

    @Override
    public List<QuotaBalanceVO> findQuotaBalanceVO(Long accountId, String accountName, Long domainId, Date startDate, Date endDate) {
        if ((accountId == null) && (accountName != null) && (domainId != null)) {
            Account userAccount = null;
            Account caller = CallContext.current().getCallingAccount();
            if (_domainDao.isChildDomain(caller.getDomainId(), domainId)) {
                Filter filter = new Filter(AccountVO.class, "id", Boolean.FALSE, null, null);
                List<AccountVO> accounts = _accountDao.listAccounts(accountName, domainId, filter);
                if (!accounts.isEmpty()) {
                    userAccount = accounts.get(0);
                }
                if (userAccount != null) {
                    accountId = userAccount.getId();
                } else {
                    throw new InvalidParameterValueException("Unable to find account " + accountName + " in domain " + domainId);
                }
            } else {
                throw new PermissionDeniedException("Invalid Domain Id or Account");
            }
        }

        startDate = startDate == null ? new Date() : startDate;

        if (endDate == null) {
            // adjust start date to end of day as there is no end date
            Date adjustedStartDate = computeAdjustedTime(_respBldr.startOfNextDay(startDate));
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("getQuotaBalance1: Getting quota balance records for account: " + accountId + ", domainId: " + domainId + ", on or before " + adjustedStartDate);
            }
            List<QuotaBalanceVO> qbrecords = _quotaBalanceDao.lastQuotaBalanceVO(accountId, domainId, adjustedStartDate);
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Found records size=" + qbrecords.size());
            }
            if (qbrecords.isEmpty()) {
                s_logger.info("Incorrect Date there are no quota records before this date " + adjustedStartDate);
                return qbrecords;
            } else {
                return qbrecords;
            }
        } else {
            Date adjustedStartDate = computeAdjustedTime(startDate);
            if (endDate.after(_respBldr.startOfNextDay())) {
                throw new InvalidParameterValueException("Incorrect Date Range. End date:" + endDate + " should not be in future. ");
            } else if (startDate.before(endDate)) {
                Date adjustedEndDate = computeAdjustedTime(endDate);
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("getQuotaBalance2: Getting quota balance records for account: " + accountId + ", domainId: " + domainId + ", between " + adjustedStartDate
                            + " and " + adjustedEndDate);
                }
                List<QuotaBalanceVO> qbrecords = _quotaBalanceDao.findQuotaBalance(accountId, domainId, adjustedStartDate, adjustedEndDate);
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("getQuotaBalance3: Found records size=" + qbrecords.size());
                }
                if (qbrecords.isEmpty()) {
                    s_logger.info("There are no quota records between these dates start date " + adjustedStartDate + " and end date:" + endDate);
                    return qbrecords;
                } else {
                    return qbrecords;
                }
            } else {
                throw new InvalidParameterValueException("Incorrect Date Range. Start date: " + startDate + " is after end date:" + endDate);
            }
        }
    }

    @Override
    public List<QuotaUsageVO> getQuotaUsage(Long accountId, String accountName, Long domainId, Integer usageType, Date startDate, Date endDate) {
        // if accountId is not specified, use accountName and domainId
        if ((accountId == null) && (accountName != null) && (domainId != null)) {
            Account userAccount = null;
            Account caller = CallContext.current().getCallingAccount();
            if (_domainDao.isChildDomain(caller.getDomainId(), domainId)) {
                Filter filter = new Filter(AccountVO.class, "id", Boolean.FALSE, null, null);
                List<AccountVO> accounts = _accountDao.listAccounts(accountName, domainId, filter);
                if (!accounts.isEmpty()) {
                    userAccount = accounts.get(0);
                }
                if (userAccount != null) {
                    accountId = userAccount.getId();
                } else {
                    throw new InvalidParameterValueException("Unable to find account " + accountName + " in domain " + domainId);
                }
            } else {
                throw new PermissionDeniedException("Invalid Domain Id or Account");
            }
        }

        if (startDate.after(endDate)) {
            throw new InvalidParameterValueException("Incorrect Date Range. Start date: " + startDate + " is after end date:" + endDate);
        }
        if (endDate.after(_respBldr.startOfNextDay())) {
            throw new InvalidParameterValueException("Incorrect Date Range. End date:" + endDate + " should not be in future. ");
        }
        Date adjustedEndDate = computeAdjustedTime(endDate);
        Date adjustedStartDate = computeAdjustedTime(startDate);
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Getting quota records for account: " + accountId + ", domainId: " + domainId + ", between " + adjustedStartDate + " and " + adjustedEndDate);
        }
        return _quotaUsageDao.findQuotaUsage(accountId, domainId, usageType, adjustedStartDate, adjustedEndDate);
    }

    @Override
    public Date computeAdjustedTime(final Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        TimeZone localTZ = cal.getTimeZone();
        int timezoneOffset = cal.get(Calendar.ZONE_OFFSET);
        if (localTZ.inDaylightTime(date)) {
            timezoneOffset += (60 * 60 * 1000);
        }
        cal.add(Calendar.MILLISECOND, timezoneOffset);

        Date newTime = cal.getTime();

        Calendar calTS = Calendar.getInstance(_usageTimezone);
        calTS.setTime(newTime);
        timezoneOffset = calTS.get(Calendar.ZONE_OFFSET);
        if (_usageTimezone.inDaylightTime(date)) {
            timezoneOffset += (60 * 60 * 1000);
        }

        calTS.add(Calendar.MILLISECOND, -1 * timezoneOffset);

        return calTS.getTime();
    }

    @Override
    public void setLockAccount(Long accountId, Boolean enforce) {
        QuotaAccountVO acc = _quotaAcc.findByIdQuotaAccount(accountId);
        if (acc == null) {
            acc = new QuotaAccountVO(accountId);
            acc.setQuotaEnforce(enforce ? 1 : 0);
            _quotaAcc.persistQuotaAccount(acc);
        } else {
            acc.setQuotaEnforce(enforce ? 1 : 0);
            _quotaAcc.updateQuotaAccount(accountId, acc);
        }
    }

    @Override
    public boolean saveQuotaAccount(final AccountVO account, final BigDecimal aggrUsage, final Date endDate) {
        // update quota_accounts
        QuotaAccountVO quota_account = _quotaAcc.findByIdQuotaAccount(account.getAccountId());

        if (quota_account == null) {
            quota_account = new QuotaAccountVO(account.getAccountId());
            quota_account.setQuotaBalance(aggrUsage);
            quota_account.setQuotaBalanceDate(endDate);
            if (s_logger.isDebugEnabled()) {
                s_logger.debug(quota_account);
            }
            _quotaAcc.persistQuotaAccount(quota_account);
            return true;
        } else {
            quota_account.setQuotaBalance(aggrUsage);
            quota_account.setQuotaBalanceDate(endDate);
            if (s_logger.isDebugEnabled()) {
                s_logger.debug(quota_account);
            }
            return _quotaAcc.updateQuotaAccount(account.getAccountId(), quota_account);
        }
    }

    @Override
    public void setMinBalance(Long accountId, Double balance) {
        QuotaAccountVO acc = _quotaAcc.findByIdQuotaAccount(accountId);
        if (acc == null) {
            acc = new QuotaAccountVO(accountId);
            acc.setQuotaMinBalance(new BigDecimal(balance));
            _quotaAcc.persistQuotaAccount(acc);
        } else {
            acc.setQuotaMinBalance(new BigDecimal(balance));
            _quotaAcc.updateQuotaAccount(accountId, acc);
        }
    }

}
