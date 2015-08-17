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

import com.cloud.usage.UsageVO;
import com.cloud.usage.dao.UsageDao;
import com.cloud.user.Account;
import com.cloud.user.AccountVO;
import com.cloud.user.Account.State;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.TransactionLegacy;

import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.quota.constant.QuotaTypes;
import org.apache.cloudstack.quota.dao.QuotaAccountDao;
import org.apache.cloudstack.quota.dao.QuotaBalanceDao;
import org.apache.cloudstack.quota.dao.QuotaTariffDao;
import org.apache.cloudstack.quota.dao.QuotaUsageDao;
import org.apache.cloudstack.quota.vo.QuotaAccountVO;
import org.apache.cloudstack.quota.vo.QuotaBalanceVO;
import org.apache.cloudstack.quota.vo.QuotaTariffVO;
import org.apache.cloudstack.quota.vo.QuotaUsageVO;
import org.apache.cloudstack.quota.vo.ServiceOfferingVO;
import org.apache.cloudstack.quota.dao.ServiceOfferingDao;
import org.apache.cloudstack.utils.usage.UsageUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import javax.ejb.Local;
import javax.inject.Inject;
import javax.naming.ConfigurationException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

@Component
@Local(value = QuotaManager.class)
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

    int _pid = 0;

    public QuotaManagerImpl() {
        super();
    }

    private void mergeConfigs(Map<String, String> dbParams, Map<String, Object> xmlParams) {
        for (Map.Entry<String, Object> param : xmlParams.entrySet()) {
            dbParams.put(param.getKey(), (String) param.getValue());
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
        _pid = Integer.parseInt(System.getProperty("pid"));
        return true;
    }

    @Override
    public boolean stop() {
        if (s_logger.isInfoEnabled()) {
            s_logger.info("Stopping Quota Manager");
        }
        return true;
    }

    @Override
    public boolean calculateQuotaUsage() {
        final short opendb = TransactionLegacy.currentTxn().getDatabaseId();
        boolean jobResult = false;
        TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.USAGE_DB);
        try {
            // get all the active accounts for which there is usage
            List<AccountVO> accounts = _accountDao.listAll();
            for (AccountVO account : accounts) { // START ACCOUNT
                Pair<List<? extends UsageVO>, Integer> usageRecords = null;
                List<QuotaUsageVO> quotalistforaccount = new ArrayList<QuotaUsageVO>();
                do {
                    s_logger.info("Account =" + account.getAccountName());
                    usageRecords = _usageDao.getUsageRecordsPendingQuotaAggregation(account.getAccountId(), account.getDomainId());
                    s_logger.debug("Usage records found " + usageRecords.second());
                    for (UsageVO usageRecord : usageRecords.first()) {
                        BigDecimal aggregationRatio = new BigDecimal(_aggregationDuration).divide(s_minutesInMonth, 8, RoundingMode.HALF_EVEN);
                        switch (usageRecord.getUsageType()) {
                        case QuotaTypes.RUNNING_VM:
                        case QuotaTypes.ALLOCATED_VM:
                            quotalistforaccount.add(updateQuotaAllocatedVMUsage(usageRecord, aggregationRatio));
                            break;
                        case QuotaTypes.SNAPSHOT:
                        case QuotaTypes.TEMPLATE:
                        case QuotaTypes.ISO:
                        case QuotaTypes.VOLUME:
                        case QuotaTypes.VM_SNAPSHOT:
                            quotalistforaccount.add(updateQuotaDiskUsage(usageRecord, aggregationRatio, usageRecord.getUsageType()));
                            break;
                        case QuotaTypes.LOAD_BALANCER_POLICY:
                        case QuotaTypes.PORT_FORWARDING_RULE:
                        case QuotaTypes.IP_ADDRESS:
                        case QuotaTypes.NETWORK_OFFERING:
                        case QuotaTypes.SECURITY_GROUP:
                        case QuotaTypes.VPN_USERS:
                            quotalistforaccount.add(updateQuotaRaw(usageRecord, aggregationRatio, usageRecord.getUsageType()));
                            break;
                        case QuotaTypes.NETWORK_BYTES_RECEIVED:
                        case QuotaTypes.NETWORK_BYTES_SENT:
                            quotalistforaccount.add(updateQuotaNetwork(usageRecord, usageRecord.getUsageType()));
                            break;
                        case QuotaTypes.VM_DISK_IO_READ:
                        case QuotaTypes.VM_DISK_IO_WRITE:
                        case QuotaTypes.VM_DISK_BYTES_READ:
                        case QuotaTypes.VM_DISK_BYTES_WRITE:
                        default:
                            break;
                        }
                    }
                } while ((usageRecords != null) && !usageRecords.first().isEmpty());
                // list of quotas for this account
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Quota entries size = " + quotalistforaccount.size() + ", accId" + account.getAccountId() + ", domId" + account.getDomainId());
                }
                if (quotalistforaccount.size() > 0) { // balance to be processed
                    quotalistforaccount.add(new QuotaUsageVO());
                    Date startDate = quotalistforaccount.get(0).getStartDate();
                    Date endDate = quotalistforaccount.get(0).getEndDate();
                    BigDecimal aggrUsage = new BigDecimal(0);
                    for (QuotaUsageVO entry : quotalistforaccount) {
                        if (startDate.compareTo(entry.getStartDate()) != 0) {
                            QuotaBalanceVO lastrealbalanceentry = _quotaBalanceDao.findLastBalanceEntry(account.getAccountId(), account.getDomainId(), startDate);
                            Date lastbalancedate;
                            if (lastrealbalanceentry != null) {
                                lastbalancedate = lastrealbalanceentry.getUpdatedOn();
                                aggrUsage = aggrUsage.add(lastrealbalanceentry.getCreditBalance());
                            } else {
                                lastbalancedate = new Date(0);
                            }

                            List<QuotaBalanceVO> creditsrcvd = _quotaBalanceDao.findCreditBalance(account.getAccountId(), account.getDomainId(), lastbalancedate, endDate);
                            for (QuotaBalanceVO credit : creditsrcvd) {
                                aggrUsage = aggrUsage.add(credit.getCreditBalance());
                            }

                            QuotaBalanceVO newbalance = new QuotaBalanceVO(account.getAccountId(), account.getDomainId(), aggrUsage, endDate);

                            if (s_logger.isDebugEnabled()) {
                                s_logger.debug("Balance entry=" + aggrUsage + " on Date=" + endDate);
                            }
                            _quotaBalanceDao.persist(newbalance);
                            aggrUsage = new BigDecimal(0);
                        }
                        startDate = entry.getStartDate();
                        endDate = entry.getEndDate();
                        aggrUsage = aggrUsage.subtract(entry.getQuotaUsed());
                    }
                    // update is quota_accounts
                    QuotaAccountVO quota_account = _quotaAcc.findById(account.getAccountId());
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Updating quota account bal=" + aggrUsage + " date=" + endDate);
                    }
                    if (quota_account == null) {
                        quota_account = new QuotaAccountVO(account.getAccountId());
                        quota_account.setQuotaBalance(aggrUsage);
                        quota_account.setQuotaBalanceDate(endDate);
                        _quotaAcc.persist(quota_account);
                    } else {
                        quota_account.setQuotaBalance(aggrUsage);
                        quota_account.setQuotaBalanceDate(endDate);
                        _quotaAcc.update(account.getAccountId(), quota_account);
                    }
                }// balance processed
            } // END ACCOUNT
            jobResult = true;
        } catch (Exception e) {
            s_logger.error("Quota Manager error", e);
        } finally {
            txn.close();
        }
        TransactionLegacy.open(opendb).close();
        return jobResult;
    }

    @DB
    private QuotaUsageVO updateQuotaDiskUsage(UsageVO usageRecord, final BigDecimal aggregationRatio, final int quotaType) {
        QuotaUsageVO quota_usage = null;
        QuotaTariffVO tariff = _quotaTariffDao.findTariffPlanByUsageType(quotaType, usageRecord.getEndDate());
        if (tariff != null && !tariff.getCurrencyValue().equals(0)) {
            BigDecimal quotaUsgage;
            BigDecimal onehourcostpergb;
            BigDecimal noofgbinuse;
            onehourcostpergb = tariff.getCurrencyValue().multiply(aggregationRatio);
            noofgbinuse = new BigDecimal(usageRecord.getSize()).divide(s_gb, 8, RoundingMode.HALF_EVEN);
            quotaUsgage = new BigDecimal(usageRecord.getRawUsage()).multiply(onehourcostpergb).multiply(noofgbinuse);
            quota_usage = new QuotaUsageVO(usageRecord.getId(), usageRecord.getZoneId(), usageRecord.getAccountId(), usageRecord.getDomainId(), usageRecord.getUsageType(), quotaUsgage,
                    usageRecord.getStartDate(), usageRecord.getEndDate());
            _quotaUsageDao.persist(quota_usage);
        }
        usageRecord.setQuotaCalculated(1);
        _usageDao.persist(usageRecord);
        return quota_usage;
    }

    @DB
    private List<QuotaUsageVO> updateQuotaRunningVMUsage(UsageVO usageRecord, final BigDecimal aggregationRatio) {
        List<QuotaUsageVO> quotalist = new ArrayList<QuotaUsageVO>();
        QuotaUsageVO quota_usage;
        BigDecimal cpuquotausgage, speedquotausage, memoryquotausage, vmusage;
        BigDecimal onehourcostpercpu, onehourcostper100mhz, onehourcostper1mb, onehourcostforvmusage;
        BigDecimal rawusage;
        // get service offering details
        ServiceOfferingVO serviceoffering = _serviceOfferingDao.findServiceOffering(usageRecord.getVmInstanceId(), usageRecord.getOfferingId());
        rawusage = new BigDecimal(usageRecord.getRawUsage());

        QuotaTariffVO tariff = _quotaTariffDao.findTariffPlanByUsageType(QuotaTypes.CPU_NUMBER, usageRecord.getEndDate());
        if (tariff != null && !tariff.getCurrencyValue().equals(0)) {
            BigDecimal cpu = new BigDecimal(serviceoffering.getCpu());
            onehourcostpercpu = tariff.getCurrencyValue().multiply(aggregationRatio);
            cpuquotausgage = rawusage.multiply(onehourcostpercpu).multiply(cpu);
            quota_usage = new QuotaUsageVO(usageRecord.getId(), usageRecord.getZoneId(), usageRecord.getAccountId(), usageRecord.getDomainId(), QuotaTypes.CPU_NUMBER, cpuquotausgage,
                    usageRecord.getStartDate(), usageRecord.getEndDate());
            _quotaUsageDao.persist(quota_usage);
            quotalist.add(quota_usage);
        }
        tariff = _quotaTariffDao.findTariffPlanByUsageType(QuotaTypes.CPU_CLOCK_RATE, usageRecord.getEndDate());
        if (tariff != null && !tariff.getCurrencyValue().equals(0)) {
            BigDecimal speed = new BigDecimal(serviceoffering.getSpeed() / 100.00);
            onehourcostper100mhz = tariff.getCurrencyValue().multiply(aggregationRatio);
            speedquotausage = rawusage.multiply(onehourcostper100mhz).multiply(speed);
            quota_usage = new QuotaUsageVO(usageRecord.getId(), usageRecord.getZoneId(), usageRecord.getAccountId(), usageRecord.getDomainId(), QuotaTypes.CPU_CLOCK_RATE, speedquotausage,
                    usageRecord.getStartDate(), usageRecord.getEndDate());
            _quotaUsageDao.persist(quota_usage);
            quotalist.add(quota_usage);
        }
        tariff = _quotaTariffDao.findTariffPlanByUsageType(QuotaTypes.MEMORY, usageRecord.getEndDate());
        if (tariff != null && !tariff.getCurrencyValue().equals(0)) {
            BigDecimal memory = new BigDecimal(serviceoffering.getRamSize());
            onehourcostper1mb = tariff.getCurrencyValue().multiply(aggregationRatio);
            memoryquotausage = rawusage.multiply(onehourcostper1mb).multiply(memory);
            quota_usage = new QuotaUsageVO(usageRecord.getId(), usageRecord.getZoneId(), usageRecord.getAccountId(), usageRecord.getDomainId(), QuotaTypes.MEMORY, memoryquotausage,
                    usageRecord.getStartDate(), usageRecord.getEndDate());
            _quotaUsageDao.persist(quota_usage);
            quotalist.add(quota_usage);
        }
        tariff = _quotaTariffDao.findTariffPlanByUsageType(QuotaTypes.RUNNING_VM, usageRecord.getEndDate());
        if (tariff != null && !tariff.getCurrencyValue().equals(0)) {
            onehourcostforvmusage = tariff.getCurrencyValue().multiply(aggregationRatio);
            vmusage = rawusage.multiply(onehourcostforvmusage);
            quota_usage = new QuotaUsageVO(usageRecord.getId(), usageRecord.getZoneId(), usageRecord.getAccountId(), usageRecord.getDomainId(), QuotaTypes.RUNNING_VM, vmusage,
                    usageRecord.getStartDate(), usageRecord.getEndDate());
            _quotaUsageDao.persist(quota_usage);
            quotalist.add(quota_usage);
        }

        usageRecord.setQuotaCalculated(1);
        _usageDao.persist(usageRecord);
        return quotalist;
    }

    @DB
    private QuotaUsageVO updateQuotaAllocatedVMUsage(UsageVO usageRecord, final BigDecimal aggregationRatio) {
        QuotaUsageVO quota_usage = null;
        QuotaTariffVO tariff = _quotaTariffDao.findTariffPlanByUsageType(QuotaTypes.ALLOCATED_VM, usageRecord.getEndDate());
        if (tariff != null && !tariff.getCurrencyValue().equals(0)) {
            BigDecimal vmusage;
            BigDecimal onehourcostforvmusage;
            onehourcostforvmusage = tariff.getCurrencyValue().multiply(aggregationRatio);
            vmusage = new BigDecimal(usageRecord.getRawUsage()).multiply(onehourcostforvmusage);
            quota_usage = new QuotaUsageVO(usageRecord.getId(), usageRecord.getZoneId(), usageRecord.getAccountId(), usageRecord.getDomainId(), QuotaTypes.ALLOCATED_VM, vmusage,
                    usageRecord.getStartDate(), usageRecord.getEndDate());
            _quotaUsageDao.persist(quota_usage);
        }

        usageRecord.setQuotaCalculated(1);
        _usageDao.persist(usageRecord);
        return quota_usage;
    }

    @DB
    private QuotaUsageVO updateQuotaRaw(UsageVO usageRecord, final BigDecimal aggregationRatio, final int ruleType) {
        QuotaUsageVO quota_usage = null;
        QuotaTariffVO tariff = _quotaTariffDao.findTariffPlanByUsageType(ruleType, usageRecord.getEndDate());
        if (tariff != null && !tariff.getCurrencyValue().equals(0)) {
            BigDecimal ruleusage;
            BigDecimal onehourcost;
            onehourcost = tariff.getCurrencyValue().multiply(aggregationRatio);
            ruleusage = new BigDecimal(usageRecord.getRawUsage()).multiply(onehourcost);
            quota_usage = new QuotaUsageVO(usageRecord.getId(), usageRecord.getZoneId(), usageRecord.getAccountId(), usageRecord.getDomainId(), ruleType, ruleusage, usageRecord.getStartDate(),
                    usageRecord.getEndDate());
            _quotaUsageDao.persist(quota_usage);
        }

        usageRecord.setQuotaCalculated(1);
        _usageDao.persist(usageRecord);
        return quota_usage;
    }

    @DB
    private QuotaUsageVO updateQuotaNetwork(UsageVO usageRecord, final int transferType) {
        QuotaUsageVO quota_usage = null;
        QuotaTariffVO tariff = _quotaTariffDao.findTariffPlanByUsageType(transferType, usageRecord.getEndDate());
        if (tariff != null && !tariff.getCurrencyValue().equals(0)) {
            BigDecimal onegbcost;
            BigDecimal rawusageingb;
            BigDecimal networkusage;
            onegbcost = tariff.getCurrencyValue();
            rawusageingb = new BigDecimal(usageRecord.getRawUsage()).divide(s_gb, 8, RoundingMode.HALF_EVEN);
            networkusage = rawusageingb.multiply(onegbcost);
            quota_usage = new QuotaUsageVO(usageRecord.getId(), usageRecord.getZoneId(), usageRecord.getAccountId(), usageRecord.getDomainId(), transferType, networkusage, usageRecord.getStartDate(),
                    usageRecord.getEndDate());
            _quotaUsageDao.persist(quota_usage);
        }

        usageRecord.setQuotaCalculated(1);
        _usageDao.persist(usageRecord);
        return quota_usage;
    }

    public Date startOfNextDay() {
        Calendar c = Calendar.getInstance();
        c.setTime(new Date());
        c.add(Calendar.DATE, 1);
        Date dt = c.getTime();
        return dt;
    }

    protected boolean lockAccount(long accountId) {
        final short opendb = TransactionLegacy.currentTxn().getDatabaseId();
        TransactionLegacy.open(TransactionLegacy.CLOUD_DB).close();
        boolean success = false;
        Account account = _accountDao.findById(accountId);
        if (account != null) {
            if (account.getState().equals(State.locked)) {
                return true; // already locked, no-op
            } else if (account.getState().equals(State.enabled)) {
                AccountVO acctForUpdate = _accountDao.createForUpdate();
                acctForUpdate.setState(State.locked);
                success = _accountDao.update(Long.valueOf(accountId), acctForUpdate);
            } else {
                if (s_logger.isInfoEnabled()) {
                    s_logger.info("Attempting to lock a non-enabled account, current state is " + account.getState() + " (accountId: " + accountId + "), locking failed.");
                }
            }
        } else {
            s_logger.warn("Failed to lock account " + accountId + ", account not found.");
        }
        TransactionLegacy.open(opendb).close();
        return success;
    }

    public boolean enableAccount(long accountId) {
        final short opendb = TransactionLegacy.currentTxn().getDatabaseId();
        TransactionLegacy.open(TransactionLegacy.CLOUD_DB).close();
        boolean success = false;
        AccountVO acctForUpdate = _accountDao.createForUpdate();
        acctForUpdate.setState(State.enabled);
        acctForUpdate.setNeedsCleanup(false);
        success = _accountDao.update(Long.valueOf(accountId), acctForUpdate);
        TransactionLegacy.open(opendb).close();
        return success;
    }
}
