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

import com.cloud.configuration.Config;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.usage.UsageVO;
import com.cloud.usage.dao.UsageDao;
import com.cloud.user.AccountVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.TransactionLegacy;

import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.quota.dao.QuotaTariffDao;
import org.apache.cloudstack.quota.dao.QuotaBalanceDao;
import org.apache.cloudstack.quota.dao.QuotaUsageDao;
import org.apache.cloudstack.utils.usage.UsageUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import javax.ejb.Local;
import javax.inject.Inject;
import javax.naming.ConfigurationException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
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
    private UsageDao _usageDao;
    @Inject
    private QuotaTariffDao _quotaTariffDao;
    @Inject
    private QuotaUsageDao _quotaUsageDao;
    @Inject
    private QuotaDBUtils _quotaDBUtils;
    @Inject
    private QuotaBalanceDao _quotaBalanceDao;
    @Inject
    private ConfigurationDao _configDao;

    private TimeZone _usageTimezone;
    private int _aggregationDuration = 0;

    static BigDecimal s_hoursInMonth = new BigDecimal(30 * 24);
    static BigDecimal s_minutesInMonth = new BigDecimal(30 * 24 * 60);
    static BigDecimal s_gb = new BigDecimal(1024 * 1024 * 1024);

    public QuotaManagerImpl() {
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
        s_logger.info("Usage timezone = " + _usageTimezone + " AggregationDuration=" + _aggregationDuration);
        return true;
    }

    @Override
    public boolean calculateQuotaUsage() {
        short opendb = TransactionLegacy.currentTxn().getDatabaseId();
        boolean jobResult = false;
        TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.USAGE_DB);
        try {
            // get quota tariff plans
            final List<QuotaTariffVO> result = _quotaTariffDao.listAllTariffPlans();
            HashMap<Integer, QuotaTariffVO> quotaTariffMap = new HashMap<Integer, QuotaTariffVO>();
            for (final QuotaTariffVO resource : result) {
                //s_logger.debug("QuotaConf=" + resource.getDescription() + " value=" + resource.getCurrencyValue());
                quotaTariffMap.put(resource.getUsageType(), resource);
            }

            // get all the active accounts for which there is usage
            List<AccountVO> accounts = _accountDao.listAll();
            for (AccountVO account : accounts) { // START ACCOUNT
                Pair<List<? extends UsageVO>, Integer> usageRecords = null;
                List<QuotaUsageVO> quotalistforaccount = new ArrayList<QuotaUsageVO>();
                do {
                    s_logger.info("Account =" + account.getAccountName());
                    usageRecords = _quotaDBUtils.getUsageRecords(account.getAccountId(), account.getDomainId());
                    s_logger.debug("Usage records found " + usageRecords.second());
                    for (UsageVO usageRecord : usageRecords.first()) {
                        BigDecimal aggregationRatio = new BigDecimal(_aggregationDuration).divide(s_minutesInMonth, 8, RoundingMode.HALF_EVEN);
                        switch (usageRecord.getUsageType()) {
                        case QuotaTypes.RUNNING_VM:
                            quotalistforaccount.addAll(updateQuotaRunningVMUsage(usageRecord, quotaTariffMap, aggregationRatio));
                            break;
                        case QuotaTypes.ALLOCATED_VM:
                            quotalistforaccount.add(updateQuotaAllocatedVMUsage(usageRecord, quotaTariffMap, aggregationRatio));
                            break;
                        case QuotaTypes.SNAPSHOT:
                            quotalistforaccount.add(updateQuotaDiskUsage(usageRecord, quotaTariffMap, aggregationRatio, QuotaTypes.SNAPSHOT));
                            break;
                        case QuotaTypes.TEMPLATE:
                            quotalistforaccount.add(updateQuotaDiskUsage(usageRecord, quotaTariffMap, aggregationRatio, QuotaTypes.TEMPLATE));
                            break;
                        case QuotaTypes.ISO:
                            quotalistforaccount.add(updateQuotaDiskUsage(usageRecord, quotaTariffMap, aggregationRatio, QuotaTypes.ISO));
                            break;
                        case QuotaTypes.VOLUME:
                            quotalistforaccount.add(updateQuotaDiskUsage(usageRecord, quotaTariffMap, aggregationRatio, QuotaTypes.VOLUME));
                            break;
                        case QuotaTypes.VM_SNAPSHOT:
                            quotalistforaccount.add(updateQuotaDiskUsage(usageRecord, quotaTariffMap, aggregationRatio, QuotaTypes.VM_SNAPSHOT));
                            break;
                        case QuotaTypes.LOAD_BALANCER_POLICY:
                            quotalistforaccount.add(updateQuotaRaw(usageRecord, quotaTariffMap, aggregationRatio, QuotaTypes.LOAD_BALANCER_POLICY));
                            break;
                        case QuotaTypes.PORT_FORWARDING_RULE:
                            quotalistforaccount.add(updateQuotaRaw(usageRecord, quotaTariffMap, aggregationRatio, QuotaTypes.PORT_FORWARDING_RULE));
                            break;
                        case QuotaTypes.IP_ADDRESS:
                            quotalistforaccount.add(updateQuotaRaw(usageRecord, quotaTariffMap, aggregationRatio, QuotaTypes.IP_ADDRESS));
                            break;
                        case QuotaTypes.NETWORK_OFFERING:
                            quotalistforaccount.add(updateQuotaRaw(usageRecord, quotaTariffMap, aggregationRatio, QuotaTypes.NETWORK_OFFERING));
                            break;
                        case QuotaTypes.SECURITY_GROUP:
                            quotalistforaccount.add(updateQuotaRaw(usageRecord, quotaTariffMap, aggregationRatio, QuotaTypes.SECURITY_GROUP));
                            break;
                        case QuotaTypes.VPN_USERS:
                            quotalistforaccount.add(updateQuotaRaw(usageRecord, quotaTariffMap, aggregationRatio, QuotaTypes.VPN_USERS));
                            break;
                        case QuotaTypes.NETWORK_BYTES_RECEIVED:
                            quotalistforaccount.add(updateQuotaNetwork(usageRecord, quotaTariffMap, QuotaTypes.NETWORK_BYTES_RECEIVED));
                            break;
                        case QuotaTypes.NETWORK_BYTES_SENT:
                            quotalistforaccount.add(updateQuotaNetwork(usageRecord, quotaTariffMap, QuotaTypes.NETWORK_BYTES_SENT));
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
                s_logger.info("Quota entries size = " + quotalistforaccount.size());
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
                            //s_logger.info("Balance entry=" + aggrUsage + " on Date=" + endDate);
                            _quotaBalanceDao.persist(newbalance);
                            aggrUsage = new BigDecimal(0);
                        }
                        startDate = entry.getStartDate();
                        endDate = entry.getEndDate();
                        aggrUsage = aggrUsage.subtract(entry.getQuotaUsed());
                    }
                }// balance processed
            } // END ACCOUNT
            jobResult = true;
        } catch (Exception e) {
            s_logger.error("Quota Manager error", e);
            e.printStackTrace();
        } finally {
            txn.close();
        }
        TransactionLegacy.open(opendb).close();
        return jobResult;
    }

    @DB
    private QuotaUsageVO updateQuotaDiskUsage(UsageVO usageRecord, HashMap<Integer, QuotaTariffVO> quotaTariffMap, BigDecimal aggregationRatio, int quotaType) {
        QuotaUsageVO quota_usage = null;
        if (quotaTariffMap.get(quotaType) != null) {
            BigDecimal quotaUsgage;
            BigDecimal onehourcostpergb;
            BigDecimal noofgbinuse;
            onehourcostpergb = quotaTariffMap.get(quotaType).getCurrencyValue().multiply(aggregationRatio);
            noofgbinuse = new BigDecimal(usageRecord.getSize()).divide(s_gb, 8, RoundingMode.HALF_EVEN);
            quotaUsgage = new BigDecimal(usageRecord.getRawUsage()).multiply(onehourcostpergb).multiply(noofgbinuse);
            // s_logger.info(" No of GB In use = " + noofgbinuse +
            // " onehour cost=" + onehourcostpergb);
            quota_usage = new QuotaUsageVO(usageRecord.getId(), usageRecord.getZoneId(), usageRecord.getAccountId(), usageRecord.getDomainId(), usageRecord.getUsageType(), quotaUsgage,
                    usageRecord.getStartDate(), usageRecord.getEndDate());
            _quotaUsageDao.persist(quota_usage);
        }
        usageRecord.setQuotaCalculated(1);
        _usageDao.persist(usageRecord);
        return quota_usage;
    }

    @DB
    private List<QuotaUsageVO> updateQuotaRunningVMUsage(UsageVO usageRecord, HashMap<Integer, QuotaTariffVO> quotaTariffMap, BigDecimal aggregationRatio) {
        List<QuotaUsageVO> quotalist = new ArrayList<QuotaUsageVO>();
        QuotaUsageVO quota_usage;
        BigDecimal cpuquotausgage, speedquotausage, memoryquotausage, vmusage;
        BigDecimal onehourcostpercpu, onehourcostper100mhz, onehourcostper1mb, onehourcostforvmusage;
        BigDecimal rawusage;
        //s_logger.info(usageRecord.getDescription() + ", " + usageRecord.getType() + ", " + usageRecord.getOfferingId() + ", " + usageRecord.getVmInstanceId() + ", " + usageRecord.getUsageDisplay()
                //+ ", aggrR=" + aggregationRatio);
        // get service offering details
        ServiceOfferingVO serviceoffering = _quotaDBUtils.findServiceOffering(usageRecord.getVmInstanceId(), usageRecord.getOfferingId());
        rawusage = new BigDecimal(usageRecord.getRawUsage());

        if (quotaTariffMap.get(QuotaTypes.CPU_NUMBER) != null) {
            BigDecimal cpu = new BigDecimal(serviceoffering.getCpu());
            onehourcostpercpu = quotaTariffMap.get(QuotaTypes.CPU_NUMBER).getCurrencyValue().multiply(aggregationRatio);
            cpuquotausgage = rawusage.multiply(onehourcostpercpu).multiply(cpu);
            quota_usage = new QuotaUsageVO(usageRecord.getId(), usageRecord.getZoneId(), usageRecord.getAccountId(), usageRecord.getDomainId(), QuotaTypes.CPU_NUMBER, cpuquotausgage,
                    usageRecord.getStartDate(), usageRecord.getEndDate());
            _quotaUsageDao.persist(quota_usage);
            quotalist.add(quota_usage);
        }
        if (quotaTariffMap.get(QuotaTypes.CPU_CLOCK_RATE) != null) {
            BigDecimal speed = new BigDecimal(serviceoffering.getSpeed() / 100.00);
            onehourcostper100mhz = quotaTariffMap.get(QuotaTypes.CPU_CLOCK_RATE).getCurrencyValue().multiply(aggregationRatio);
            speedquotausage = rawusage.multiply(onehourcostper100mhz).multiply(speed);
            quota_usage = new QuotaUsageVO(usageRecord.getId(), usageRecord.getZoneId(), usageRecord.getAccountId(), usageRecord.getDomainId(), QuotaTypes.CPU_CLOCK_RATE, speedquotausage,
                    usageRecord.getStartDate(), usageRecord.getEndDate());
            _quotaUsageDao.persist(quota_usage);
            quotalist.add(quota_usage);
        }
        if (quotaTariffMap.get(QuotaTypes.MEMORY) != null) {
            BigDecimal memory = new BigDecimal(serviceoffering.getRamSize());
            onehourcostper1mb = quotaTariffMap.get(QuotaTypes.MEMORY).getCurrencyValue().multiply(aggregationRatio);
            memoryquotausage = rawusage.multiply(onehourcostper1mb).multiply(memory);
            quota_usage = new QuotaUsageVO(usageRecord.getId(), usageRecord.getZoneId(), usageRecord.getAccountId(), usageRecord.getDomainId(), QuotaTypes.MEMORY, memoryquotausage,
                    usageRecord.getStartDate(), usageRecord.getEndDate());
            _quotaUsageDao.persist(quota_usage);
            quotalist.add(quota_usage);
        }
        if (quotaTariffMap.get(QuotaTypes.RUNNING_VM) != null) {
            onehourcostforvmusage = quotaTariffMap.get(QuotaTypes.RUNNING_VM).getCurrencyValue().multiply(aggregationRatio);
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
    private QuotaUsageVO updateQuotaAllocatedVMUsage(UsageVO usageRecord, HashMap<Integer, QuotaTariffVO> quotaTariffMap, BigDecimal aggregationRatio) {
        QuotaUsageVO quota_usage = null;
        if (quotaTariffMap.get(QuotaTypes.ALLOCATED_VM) != null) {
            BigDecimal vmusage;
            BigDecimal onehourcostforvmusage;
            onehourcostforvmusage = quotaTariffMap.get(QuotaTypes.ALLOCATED_VM).getCurrencyValue().multiply(aggregationRatio);
            //s_logger.info("Quotatariff onehourcostforvmusage=" + onehourcostforvmusage);
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
    private QuotaUsageVO updateQuotaRaw(UsageVO usageRecord, HashMap<Integer, QuotaTariffVO> quotaTariffMap, BigDecimal aggregationRatio, int ruleType) {
        QuotaUsageVO quota_usage = null;
        if (quotaTariffMap.get(ruleType) != null) {
            BigDecimal ruleusage;
            BigDecimal onehourcost;
            onehourcost = quotaTariffMap.get(ruleType).getCurrencyValue().multiply(aggregationRatio);
            //s_logger.info("Quotatariff onehourcost=" + onehourcost);
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
    private QuotaUsageVO updateQuotaNetwork(UsageVO usageRecord, HashMap<Integer, QuotaTariffVO> quotaTariffMap, int transferType) {
        QuotaUsageVO quota_usage = null;
        if (quotaTariffMap.get(transferType) != null) {
            BigDecimal onegbcost;
            BigDecimal rawusageingb;
            BigDecimal networkusage;
            onegbcost = quotaTariffMap.get(transferType).getCurrencyValue();
            //s_logger.info("Quotatariff onegbcost=" + onegbcost);
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

    public TimeZone getUsageTimezone() {
        return _usageTimezone;
    }

}
