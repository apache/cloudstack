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
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import com.cloud.user.Account;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.quota.constant.QuotaTypes;
import org.apache.cloudstack.quota.dao.QuotaAccountDao;
import org.apache.cloudstack.quota.dao.QuotaBalanceDao;
import org.apache.cloudstack.quota.dao.QuotaTariffDao;
import org.apache.cloudstack.quota.dao.QuotaUsageDao;
import org.apache.cloudstack.quota.dao.ServiceOfferingDao;
import org.apache.cloudstack.quota.vo.QuotaAccountVO;
import org.apache.cloudstack.quota.vo.QuotaBalanceVO;
import org.apache.cloudstack.quota.vo.QuotaTariffVO;
import org.apache.cloudstack.quota.vo.QuotaUsageVO;
import org.apache.cloudstack.quota.vo.ServiceOfferingVO;
import org.apache.cloudstack.utils.usage.UsageUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.usage.UsageVO;
import com.cloud.usage.dao.UsageDao;
import com.cloud.user.AccountVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ManagerBase;

@Component
public class QuotaManagerImpl extends ManagerBase implements QuotaManager {
    private static final Logger s_logger = Logger.getLogger(QuotaManagerImpl.class.getName());

    @Inject
    private AccountDao _accountDao;
    @Inject
    private QuotaAccountDao _quotaAcc;
    @Inject
    private UsageDao _usageDao;
    @Inject
    private QuotaTariffDao _quotaTariffDao;
    @Inject
    private QuotaUsageDao _quotaUsageDao;
    @Inject
    private ServiceOfferingDao _serviceOfferingDao;
    @Inject
    private QuotaBalanceDao _quotaBalanceDao;
    @Inject
    private ConfigurationDao _configDao;

    private TimeZone _usageTimezone;
    private int _aggregationDuration = 0;

    final static BigDecimal s_hoursInMonth = new BigDecimal(30 * 24);
    final static BigDecimal s_minutesInMonth = new BigDecimal(30 * 24 * 60);
    final static BigDecimal s_gb = new BigDecimal(1024 * 1024 * 1024);

    public QuotaManagerImpl() {
        super();
    }

    private void mergeConfigs(Map<String, String> dbParams, Map<String, Object> xmlParams) {
        for (Map.Entry<String, Object> param : xmlParams.entrySet()) {
            dbParams.put(param.getKey(), (String)param.getValue());
        }
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);

        Map<String, String> configs = _configDao.getConfiguration(params);

        if (params != null) {
            mergeConfigs(configs, params);
        }

        String aggregationRange = configs.get("usage.stats.job.aggregation.range");
        String timeZoneStr = configs.get("usage.aggregation.timezone");

        if (timeZoneStr == null) {
            timeZoneStr = "GMT";
        }
        _usageTimezone = TimeZone.getTimeZone(timeZoneStr);

        _aggregationDuration = Integer.parseInt(aggregationRange);
        if (_aggregationDuration < UsageUtils.USAGE_AGGREGATION_RANGE_MIN) {
            s_logger.warn("Usage stats job aggregation range is to small, using the minimum value of " + UsageUtils.USAGE_AGGREGATION_RANGE_MIN);
            _aggregationDuration = UsageUtils.USAGE_AGGREGATION_RANGE_MIN;
        }
        s_logger.info("Usage timezone = " + _usageTimezone + " AggregationDuration=" + _aggregationDuration);

        return true;
    }

    @Override
    public boolean start() {
        if (s_logger.isInfoEnabled()) {
            s_logger.info("Starting Quota Manager");
        }
        return true;
    }

    @Override
    public boolean stop() {
        if (s_logger.isInfoEnabled()) {
            s_logger.info("Stopping Quota Manager");
        }
        return true;
    }

    public List<QuotaUsageVO> aggregatePendingQuotaRecordsForAccount(final AccountVO account, final Pair<List<? extends UsageVO>, Integer> usageRecords) {
        List<QuotaUsageVO> quotaListForAccount = new ArrayList<>();
        if (usageRecords == null || usageRecords.first() == null || usageRecords.first().isEmpty()) {
            return quotaListForAccount;
        }
        s_logger.info("Getting pending quota records for account=" + account.getAccountName());
        for (UsageVO usageRecord : usageRecords.first()) {
            switch (usageRecord.getUsageType()) {
            case QuotaTypes.RUNNING_VM:
                List<QuotaUsageVO> lq = updateQuotaRunningVMUsage(usageRecord);
                if (!lq.isEmpty()) {
                    quotaListForAccount.addAll(lq);
                }
                break;
            case QuotaTypes.ALLOCATED_VM:
                QuotaUsageVO qu = updateQuotaAllocatedVMUsage(usageRecord);
                if (qu != null) {
                    quotaListForAccount.add(qu);
                }
                break;
            case QuotaTypes.SNAPSHOT:
            case QuotaTypes.TEMPLATE:
            case QuotaTypes.ISO:
            case QuotaTypes.VOLUME:
            case QuotaTypes.VM_SNAPSHOT:
            case QuotaTypes.BACKUP:
                qu = updateQuotaDiskUsage(usageRecord, usageRecord.getUsageType());
                if (qu != null) {
                    quotaListForAccount.add(qu);
                }
                break;
            case QuotaTypes.LOAD_BALANCER_POLICY:
            case QuotaTypes.PORT_FORWARDING_RULE:
            case QuotaTypes.IP_ADDRESS:
            case QuotaTypes.NETWORK_OFFERING:
            case QuotaTypes.SECURITY_GROUP:
            case QuotaTypes.VPN_USERS:
                qu = updateQuotaRaw(usageRecord, usageRecord.getUsageType());
                if (qu != null) {
                    quotaListForAccount.add(qu);
                }
                break;
            case QuotaTypes.NETWORK_BYTES_RECEIVED:
            case QuotaTypes.NETWORK_BYTES_SENT:
                qu = updateQuotaNetwork(usageRecord, usageRecord.getUsageType());
                if (qu != null) {
                    quotaListForAccount.add(qu);
                }
                break;
            case QuotaTypes.VM_DISK_IO_READ:
            case QuotaTypes.VM_DISK_IO_WRITE:
            case QuotaTypes.VM_DISK_BYTES_READ:
            case QuotaTypes.VM_DISK_BYTES_WRITE:
            default:
                break;
            }
        }
        return quotaListForAccount;
    }

    public void processQuotaBalanceForAccount(final AccountVO account, final List<QuotaUsageVO> quotaListForAccount) {
        if (quotaListForAccount == null || quotaListForAccount.isEmpty()) {
            return;
        }
        if (s_logger.isDebugEnabled()) {
            s_logger.debug(quotaListForAccount.get(0));
        }
        Date startDate = quotaListForAccount.get(0).getStartDate();
        Date endDate = quotaListForAccount.get(0).getEndDate();
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("processQuotaBalanceForAccount startDate " + startDate + " endDate=" + endDate);
            s_logger.debug("processQuotaBalanceForAccount last items startDate " + quotaListForAccount.get(quotaListForAccount.size() - 1).getStartDate() + " items endDate="
                    + quotaListForAccount.get(quotaListForAccount.size() - 1).getEndDate());
        }
        quotaListForAccount.add(new QuotaUsageVO());
        BigDecimal aggrUsage = new BigDecimal(0);
        List<QuotaBalanceVO> creditsReceived = null;

        //bootstrapping
        QuotaUsageVO lastQuotaUsage = _quotaUsageDao.findLastQuotaUsageEntry(account.getAccountId(), account.getDomainId(), startDate);
        if (lastQuotaUsage == null) {
            aggrUsage = aggrUsage.add(aggregateCreditBetweenDates(account, new Date(0), startDate));
            // create a balance entry for these accumulated credits
            QuotaBalanceVO firstBalance = new QuotaBalanceVO(account.getAccountId(), account.getDomainId(), aggrUsage, startDate);
            _quotaBalanceDao.saveQuotaBalance(firstBalance);
        } else {
            QuotaBalanceVO lastRealBalanceEntry = _quotaBalanceDao.findLastBalanceEntry(account.getAccountId(), account.getDomainId(), endDate);
            if (lastRealBalanceEntry != null){
                aggrUsage = aggrUsage.add(lastRealBalanceEntry.getCreditBalance());
            }
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Last balance entry  " + lastRealBalanceEntry + " AggrUsage=" + aggrUsage);
            }
            // get all the credit entries after this balance and add
            aggrUsage = aggrUsage.add(aggregateCreditBetweenDates(account, lastRealBalanceEntry.getUpdatedOn(), endDate));
        }

        for (QuotaUsageVO entry : quotaListForAccount) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Usage entry found " + entry);
            }
            if (entry.getQuotaUsed().compareTo(BigDecimal.ZERO) == 0) {
                // check if there were credits and aggregate
                aggrUsage = aggrUsage.add(aggregateCreditBetweenDates(account, entry.getStartDate(), entry.getEndDate()));
                continue;
            }
            if (startDate.compareTo(entry.getStartDate()) != 0) {
                saveQuotaBalance(account, aggrUsage, endDate);

                //New balance entry
                aggrUsage = new BigDecimal(0);
                startDate = entry.getStartDate();
                endDate = entry.getEndDate();

                QuotaBalanceVO lastRealBalanceEntry = _quotaBalanceDao.findLastBalanceEntry(account.getAccountId(), account.getDomainId(), endDate);
                Date lastBalanceDate = new Date(0);
                if (lastRealBalanceEntry != null) {
                    lastBalanceDate = lastRealBalanceEntry.getUpdatedOn();
                    aggrUsage = aggrUsage.add(lastRealBalanceEntry.getCreditBalance());
                }
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Getting Balance" + account.getAccountName() + ",Balance entry=" + aggrUsage + " on Date=" + endDate);
                }
                aggrUsage = aggrUsage.add(aggregateCreditBetweenDates(account, lastBalanceDate, endDate));
            }
            aggrUsage = aggrUsage.subtract(entry.getQuotaUsed());
        }
        saveQuotaBalance(account, aggrUsage, endDate);

        // update quota_balance
        saveQuotaAccount(account, aggrUsage, endDate);
    }

    private QuotaBalanceVO saveQuotaBalance(final AccountVO account, final BigDecimal aggrUsage, final Date endDate) {
        QuotaBalanceVO newBalance = new QuotaBalanceVO(account.getAccountId(), account.getDomainId(), aggrUsage, endDate);
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Saving Balance" + newBalance);
        }
        return _quotaBalanceDao.saveQuotaBalance(newBalance);
    }

    private boolean saveQuotaAccount(final AccountVO account, final BigDecimal aggrUsage, final Date endDate) {
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

    private BigDecimal aggregateCreditBetweenDates(final AccountVO account, final Date startDate, final Date endDate) {
        BigDecimal aggrUsage = new BigDecimal(0);
        List<QuotaBalanceVO> creditsReceived = null;
        creditsReceived = _quotaBalanceDao.findCreditBalance(account.getAccountId(), account.getDomainId(), startDate, endDate);
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Credit entries count " + creditsReceived.size() + " on Before Date=" + endDate);
        }
        if (creditsReceived != null) {
            for (QuotaBalanceVO credit : creditsReceived) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Credit entry found " + credit);
                    s_logger.debug("Total = " + aggrUsage);
                }
                aggrUsage = aggrUsage.add(credit.getCreditBalance());
            }
        }
        return aggrUsage;
    }

    @Override
    public boolean calculateQuotaUsage() {
        List<AccountVO> accounts = _accountDao.listAll();
        for (AccountVO account : accounts) {
            Pair<List<? extends UsageVO>, Integer> usageRecords = _usageDao.getUsageRecordsPendingQuotaAggregation(account.getAccountId(), account.getDomainId());
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Usage entries size = " + usageRecords.second().intValue() + ", accId" + account.getAccountId() + ", domId" + account.getDomainId());
            }
            List<QuotaUsageVO> quotaListForAccount = aggregatePendingQuotaRecordsForAccount(account, usageRecords);
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Quota entries size = " + quotaListForAccount.size() + ", accId" + account.getAccountId() + ", domId" + account.getDomainId());
            }
            processQuotaBalanceForAccount(account, quotaListForAccount);
        }
        return true;
    }

    public QuotaUsageVO updateQuotaDiskUsage(UsageVO usageRecord, final int quotaType) {
        QuotaUsageVO quota_usage = null;
        QuotaTariffVO tariff = _quotaTariffDao.findTariffPlanByUsageType(quotaType, usageRecord.getEndDate());
        if (tariff != null && tariff.getCurrencyValue().compareTo(BigDecimal.ZERO) != 0) {
            BigDecimal quotaUsgage;
            BigDecimal onehourcostpergb;
            BigDecimal noofgbinuse;
            onehourcostpergb = tariff.getCurrencyValue().divide(s_hoursInMonth, 8, RoundingMode.HALF_DOWN);
            noofgbinuse = new BigDecimal(usageRecord.getSize()).divide(s_gb, 8, RoundingMode.HALF_EVEN);
            quotaUsgage = new BigDecimal(usageRecord.getRawUsage()).multiply(onehourcostpergb).multiply(noofgbinuse);
            quota_usage = new QuotaUsageVO(usageRecord.getId(), usageRecord.getZoneId(), usageRecord.getAccountId(), usageRecord.getDomainId(), usageRecord.getUsageType(),
                    quotaUsgage, usageRecord.getStartDate(), usageRecord.getEndDate());
            _quotaUsageDao.persistQuotaUsage(quota_usage);
        }
        usageRecord.setQuotaCalculated(1);
        _usageDao.persistUsage(usageRecord);
        return quota_usage;
    }

    public List<QuotaUsageVO> updateQuotaRunningVMUsage(UsageVO usageRecord) {
        List<QuotaUsageVO> quotalist = new ArrayList<QuotaUsageVO>();
        QuotaUsageVO quota_usage;
        BigDecimal cpuquotausgage, speedquotausage, memoryquotausage, vmusage;
        BigDecimal onehourcostpercpu, onehourcostper100mhz, onehourcostper1mb, onehourcostforvmusage;
        BigDecimal rawusage;
        // get service offering details
        ServiceOfferingVO serviceoffering = _serviceOfferingDao.findServiceOffering(usageRecord.getVmInstanceId(), usageRecord.getOfferingId());
        if (serviceoffering == null) {
            return quotalist;
        }
        rawusage = new BigDecimal(usageRecord.getRawUsage());

        QuotaTariffVO tariff = _quotaTariffDao.findTariffPlanByUsageType(QuotaTypes.CPU_NUMBER, usageRecord.getEndDate());
        if (tariff != null && tariff.getCurrencyValue().compareTo(BigDecimal.ZERO) != 0 && serviceoffering.getCpu() != null) {
            BigDecimal cpu = new BigDecimal(serviceoffering.getCpu());
            onehourcostpercpu = tariff.getCurrencyValue().divide(s_hoursInMonth, 8, RoundingMode.HALF_DOWN);
            cpuquotausgage = rawusage.multiply(onehourcostpercpu).multiply(cpu);
            quota_usage = new QuotaUsageVO(usageRecord.getId(), usageRecord.getZoneId(), usageRecord.getAccountId(), usageRecord.getDomainId(), QuotaTypes.CPU_NUMBER,
                    cpuquotausgage, usageRecord.getStartDate(), usageRecord.getEndDate());
            _quotaUsageDao.persistQuotaUsage(quota_usage);
            quotalist.add(quota_usage);
        }
        tariff = _quotaTariffDao.findTariffPlanByUsageType(QuotaTypes.CPU_CLOCK_RATE, usageRecord.getEndDate());
        if (tariff != null && tariff.getCurrencyValue().compareTo(BigDecimal.ZERO) != 0 && serviceoffering.getSpeed() != null) {
            BigDecimal speed = new BigDecimal(serviceoffering.getSpeed()*serviceoffering.getCpu() / 100.00);
            onehourcostper100mhz = tariff.getCurrencyValue().divide(s_hoursInMonth, 8, RoundingMode.HALF_DOWN);
            speedquotausage = rawusage.multiply(onehourcostper100mhz).multiply(speed);
            quota_usage = new QuotaUsageVO(usageRecord.getId(), usageRecord.getZoneId(), usageRecord.getAccountId(), usageRecord.getDomainId(), QuotaTypes.CPU_CLOCK_RATE,
                    speedquotausage, usageRecord.getStartDate(), usageRecord.getEndDate());
            _quotaUsageDao.persistQuotaUsage(quota_usage);
            quotalist.add(quota_usage);
        }
        tariff = _quotaTariffDao.findTariffPlanByUsageType(QuotaTypes.MEMORY, usageRecord.getEndDate());
        if (tariff != null && tariff.getCurrencyValue().compareTo(BigDecimal.ZERO) != 0 && serviceoffering.getRamSize() != null) {
            BigDecimal memory = new BigDecimal(serviceoffering.getRamSize());
            onehourcostper1mb = tariff.getCurrencyValue().divide(s_hoursInMonth, 8, RoundingMode.HALF_DOWN);
            memoryquotausage = rawusage.multiply(onehourcostper1mb).multiply(memory);
            quota_usage = new QuotaUsageVO(usageRecord.getId(), usageRecord.getZoneId(), usageRecord.getAccountId(), usageRecord.getDomainId(), QuotaTypes.MEMORY, memoryquotausage,
                    usageRecord.getStartDate(), usageRecord.getEndDate());
            _quotaUsageDao.persistQuotaUsage(quota_usage);
            quotalist.add(quota_usage);
        }
        tariff = _quotaTariffDao.findTariffPlanByUsageType(QuotaTypes.RUNNING_VM, usageRecord.getEndDate());
        if (tariff != null && tariff.getCurrencyValue().compareTo(BigDecimal.ZERO) != 0) {
            onehourcostforvmusage = tariff.getCurrencyValue().divide(s_hoursInMonth, 8, RoundingMode.HALF_DOWN);
            vmusage = rawusage.multiply(onehourcostforvmusage);
            quota_usage = new QuotaUsageVO(usageRecord.getId(), usageRecord.getZoneId(), usageRecord.getAccountId(), usageRecord.getDomainId(), QuotaTypes.RUNNING_VM, vmusage,
                    usageRecord.getStartDate(), usageRecord.getEndDate());
            _quotaUsageDao.persistQuotaUsage(quota_usage);
            quotalist.add(quota_usage);
        }

        usageRecord.setQuotaCalculated(1);
        _usageDao.persistUsage(usageRecord);
        return quotalist;
    }

    public QuotaUsageVO updateQuotaAllocatedVMUsage(UsageVO usageRecord) {
        QuotaUsageVO quota_usage = null;
        QuotaTariffVO tariff = _quotaTariffDao.findTariffPlanByUsageType(QuotaTypes.ALLOCATED_VM, usageRecord.getEndDate());
        if (tariff != null && tariff.getCurrencyValue().compareTo(BigDecimal.ZERO) != 0) {
            BigDecimal vmusage;
            BigDecimal onehourcostforvmusage;
            onehourcostforvmusage = tariff.getCurrencyValue().divide(s_hoursInMonth, 8, RoundingMode.HALF_DOWN);
            vmusage = new BigDecimal(usageRecord.getRawUsage()).multiply(onehourcostforvmusage);
            quota_usage = new QuotaUsageVO(usageRecord.getId(), usageRecord.getZoneId(), usageRecord.getAccountId(), usageRecord.getDomainId(), QuotaTypes.ALLOCATED_VM, vmusage,
                    usageRecord.getStartDate(), usageRecord.getEndDate());
            _quotaUsageDao.persistQuotaUsage(quota_usage);
        }

        usageRecord.setQuotaCalculated(1);
        _usageDao.persistUsage(usageRecord);
        return quota_usage;
    }

    public QuotaUsageVO updateQuotaRaw(UsageVO usageRecord, final int ruleType) {
        QuotaUsageVO quota_usage = null;
        QuotaTariffVO tariff = _quotaTariffDao.findTariffPlanByUsageType(ruleType, usageRecord.getEndDate());
        if (tariff != null && tariff.getCurrencyValue().compareTo(BigDecimal.ZERO) != 0) {
            BigDecimal ruleusage;
            BigDecimal onehourcost;
            onehourcost = tariff.getCurrencyValue().divide(s_hoursInMonth, 8, RoundingMode.HALF_DOWN);
            ruleusage = new BigDecimal(usageRecord.getRawUsage()).multiply(onehourcost);
            quota_usage = new QuotaUsageVO(usageRecord.getId(), usageRecord.getZoneId(), usageRecord.getAccountId(), usageRecord.getDomainId(), ruleType, ruleusage,
                    usageRecord.getStartDate(), usageRecord.getEndDate());
            _quotaUsageDao.persistQuotaUsage(quota_usage);
        }

        usageRecord.setQuotaCalculated(1);
        _usageDao.persistUsage(usageRecord);
        return quota_usage;
    }

    public QuotaUsageVO updateQuotaNetwork(UsageVO usageRecord, final int transferType) {
        QuotaUsageVO quota_usage = null;
        QuotaTariffVO tariff = _quotaTariffDao.findTariffPlanByUsageType(transferType, usageRecord.getEndDate());
        if (tariff != null && tariff.getCurrencyValue().compareTo(BigDecimal.ZERO) != 0) {
            BigDecimal onegbcost;
            BigDecimal rawusageingb;
            BigDecimal networkusage;
            onegbcost = tariff.getCurrencyValue();
            rawusageingb = new BigDecimal(usageRecord.getRawUsage()).divide(s_gb, 8, RoundingMode.HALF_EVEN);
            networkusage = rawusageingb.multiply(onegbcost);
            quota_usage = new QuotaUsageVO(usageRecord.getId(), usageRecord.getZoneId(), usageRecord.getAccountId(), usageRecord.getDomainId(), transferType, networkusage,
                    usageRecord.getStartDate(), usageRecord.getEndDate());
            _quotaUsageDao.persistQuotaUsage(quota_usage);
        }

        usageRecord.setQuotaCalculated(1);
        _usageDao.persistUsage(usageRecord);
        return quota_usage;
    }

    @Override
    public boolean isLockable(AccountVO account) {
        return (account.getType() == Account.Type.NORMAL || account.getType() == Account.Type.DOMAIN_ADMIN);
    }

}
