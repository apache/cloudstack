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
import com.cloud.domain.dao.DomainDao;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.usage.UsageVO;
import com.cloud.usage.dao.UsageDao;
import com.cloud.user.Account;
import com.cloud.user.AccountService;
import com.cloud.user.AccountVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.TransactionLegacy;
import org.apache.cloudstack.api.command.QuotaCreditsCmd;
import org.apache.cloudstack.api.command.QuotaEditMappingCmd;
import org.apache.cloudstack.api.command.QuotaEmailTemplateAddCmd;
import org.apache.cloudstack.api.command.QuotaMappingCmd;
import org.apache.cloudstack.api.command.QuotaRefreshCmd;
import org.apache.cloudstack.api.command.QuotaStatementCmd;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.quota.dao.QuotaMappingDao;
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
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

@Component
@Local(value = QuotaManager.class)
public class QuotaManagerImpl extends ManagerBase implements QuotaManager, Configurable, QuotaConfig {
    private static final Logger s_logger = Logger.getLogger(QuotaManagerImpl.class.getName());

    @Inject
    private AccountDao _accountDao;
    @Inject
    private UsageDao _usageDao;
    @Inject
    private QuotaMappingDao _quotaConfigurationDao;
    @Inject
    private QuotaUsageDao _quotaUsageDao;
    @Inject
    private ServiceOfferingDao _serviceOfferingDao;
    @Inject
    private DomainDao _domainDao;
    @Inject
    private ConfigurationDao _configDao;
    @Inject
    private AccountService _accountService;

    private TimeZone _usageTimezone;
    private int _aggregationDuration = 0;

    static BigDecimal s_hoursInMonth = new BigDecimal(30 * 24);
    static BigDecimal s_minutesInMonth = new BigDecimal(30 * 24 * 60);
    static BigDecimal s_gb = new BigDecimal(1024 * 1024 * 1024);
    static Long s_recordtofetch=1000L;

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
    public List<Class<?>> getCommands() {
        final List<Class<?>> cmdList = new ArrayList<Class<?>>();
        cmdList.add(QuotaMappingCmd.class);
        cmdList.add(QuotaCreditsCmd.class);
        cmdList.add(QuotaEmailTemplateAddCmd.class);
        cmdList.add(QuotaRefreshCmd.class);
        cmdList.add(QuotaStatementCmd.class);
        cmdList.add(QuotaEditMappingCmd.class);
        return cmdList;
    }

    @Override
    public String getConfigComponentName() {
        return "QUOTA-PLUGIN";
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[] { QuotaPluginEnabled, QuotaPeriodType, QuotaPeriod, QuotaGenerateActivity, QuotaEmailRecordOutgoing, QuotaEnableEnforcement, QuotaCurrencySymbol, QuotaLimitCritical,
                QuotaLimitIncremental, QuotaSmtpHost, QuotaSmtpTimeout, QuotaSmtpUser, QuotaSmtpPassword, QuotaSmtpPort, QuotaSmtpAuthType };
    }

    @Override
    public boolean calculateQuotaUsage() {
        boolean jobResult = false;
        TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.USAGE_DB);
        try {
            // get quota mappings
            final Pair<List<QuotaMappingVO>, Integer> result = _quotaConfigurationDao.listAllMapping();
            HashMap<Integer, QuotaMappingVO> mapping = new HashMap<Integer, QuotaMappingVO>();
            for (final QuotaMappingVO resource : result.first()) {
                s_logger.debug("QuotaConf=" + resource.getDescription());
                mapping.put(resource.getUsageType(), resource);
            }

            // get all the active accounts for which there is usage
            List<AccountVO> accounts = _accountDao.listAll();
            for (AccountVO account : accounts) {
                Pair<List<? extends UsageVO>, Integer> usageRecords = null;
                do {
                    s_logger.info("Account =" + account.getAccountName());
                    usageRecords = getUsageRecords(account.getAccountId(), account.getDomainId());
                    s_logger.debug("Usage records found " + usageRecords.second());
                    for (UsageVO usageRecord : usageRecords.first()) {
                        BigDecimal aggregationRatio = new BigDecimal(_aggregationDuration).divide(s_minutesInMonth, 8, RoundingMode.HALF_EVEN);
                        switch (usageRecord.getUsageType()) {
                        case QuotaTypes.RUNNING_VM:
                            updateQuotaRunningVMUsage(usageRecord, mapping, aggregationRatio);
                            break;
                        case QuotaTypes.ALLOCATED_VM:
                            updateQuotaAllocatedVMUsage(usageRecord, mapping, aggregationRatio);
                            break;
                        case QuotaTypes.SNAPSHOT:
                            updateQuotaDiskUsage(usageRecord, mapping, aggregationRatio, QuotaTypes.SNAPSHOT);
                            break;
                        case QuotaTypes.TEMPLATE:
                            updateQuotaDiskUsage(usageRecord, mapping, aggregationRatio, QuotaTypes.TEMPLATE);
                            break;
                        case QuotaTypes.ISO:
                            updateQuotaDiskUsage(usageRecord, mapping, aggregationRatio, QuotaTypes.ISO);
                            break;
                        case QuotaTypes.VOLUME:
                            updateQuotaDiskUsage(usageRecord, mapping, aggregationRatio, QuotaTypes.VOLUME);
                            break;
                        case QuotaTypes.VM_SNAPSHOT:
                            updateQuotaDiskUsage(usageRecord, mapping, aggregationRatio, QuotaTypes.VM_SNAPSHOT);
                            break;
                        case QuotaTypes.LOAD_BALANCER_POLICY:
                            updateQuotaRaw(usageRecord, mapping, aggregationRatio, QuotaTypes.LOAD_BALANCER_POLICY);
                            break;
                        case QuotaTypes.PORT_FORWARDING_RULE:
                            updateQuotaRaw(usageRecord, mapping, aggregationRatio, QuotaTypes.PORT_FORWARDING_RULE);
                            break;
                        case QuotaTypes.IP_ADDRESS:
                            updateQuotaRaw(usageRecord, mapping, aggregationRatio, QuotaTypes.IP_ADDRESS);
                            break;
                        case QuotaTypes.NETWORK_OFFERING:
                            updateQuotaRaw(usageRecord, mapping, aggregationRatio, QuotaTypes.NETWORK_OFFERING);
                            break;
                        case QuotaTypes.SECURITY_GROUP:
                            updateQuotaRaw(usageRecord, mapping, aggregationRatio, QuotaTypes.SECURITY_GROUP);
                            break;
                        case QuotaTypes.VPN_USERS:
                            updateQuotaRaw(usageRecord, mapping, aggregationRatio, QuotaTypes.VPN_USERS);
                            break;
                        case QuotaTypes.NETWORK_BYTES_RECEIVED:
                            updateQuotaRaw(usageRecord, mapping, aggregationRatio, QuotaTypes.NETWORK_BYTES_RECEIVED);
                            break;
                        case QuotaTypes.NETWORK_BYTES_SENT:
                            updateQuotaRaw(usageRecord, mapping, aggregationRatio, QuotaTypes.NETWORK_BYTES_SENT);
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
            }
            jobResult = true;
        } catch (Exception e) {
            s_logger.error("Quota Manager error", e);
            e.printStackTrace();
        } finally {
            txn.close();
        }
        return jobResult;
    }

    @SuppressWarnings("deprecation")
    @Override
    public Pair<List<QuotaUsageVO>, Integer> getQuotaUsage(QuotaStatementCmd cmd) {
        Long accountId = cmd.getAccountId();
        String accountName = cmd.getAccountName();
        Long domainId = cmd.getDomainId();
        Account userAccount = null;
        Account caller = CallContext.current().getCallingAccount();
        Integer usageType = cmd.getUsageType();

        // if accountId is not specified, use accountName and domainId
        if ((accountId == null) && (accountName != null) && (domainId != null)) {
            if (_domainDao.isChildDomain(caller.getDomainId(), domainId)) {
                Filter filter = new Filter(AccountVO.class, "id", Boolean.FALSE, null, null);
                List<AccountVO> accounts = _accountDao.listAccounts(accountName, domainId, filter);
                if (accounts.size() > 0) {
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

        boolean isAdmin = false;
        boolean isDomainAdmin = false;

        // If accountId couldn't be found using accountName and domainId, get it
        // from userContext
        if (accountId == null) {
            accountId = caller.getId();
            // List records for all the accounts if the caller account is of
            // type admin.
            // If account_id or account_name is explicitly mentioned, list
            // records for the specified account only even if the caller is of
            // type admin
            if (_accountService.isRootAdmin(caller.getId())) {
                isAdmin = true;
            } else if (_accountService.isDomainAdmin(caller.getId())) {
                isDomainAdmin = true;
            }
            s_logger.debug("Account details not available. Using userContext accountId: " + accountId);
        }

        Date startDate = cmd.getStartDate();
        Date endDate = cmd.getEndDate();
        if (startDate.after(endDate)) {
            throw new InvalidParameterValueException("Incorrect Date Range. Start date: " + startDate + " is after end date:" + endDate);
        }
        TimeZone usageTZ = getUsageTimezone();
        Date adjustedStartDate = computeAdjustedTime(startDate, usageTZ);
        Date adjustedEndDate = computeAdjustedTime(endDate, usageTZ);

        s_logger.debug("getting quota records for account: " + accountId + ", domainId: " + domainId + ", between " + adjustedStartDate + " and " + adjustedEndDate);

        TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.USAGE_DB);
        Pair<List<QuotaUsageVO>, Integer> quotaUsageRecords = null;
        try {
            //TODO instead of max value query with reasonable number and iterate
            Filter usageFilter = new Filter(QuotaUsageVO.class, "id", true, 0L, Long.MAX_VALUE);
            SearchCriteria<QuotaUsageVO> sc = _quotaUsageDao.createSearchCriteria();
            if (accountId != null) {
                sc.addAnd("accountId", SearchCriteria.Op.EQ, accountId);
                s_logger.debug("Account ID=" + accountId);
            }
            /*
             * if (isDomainAdmin) { SearchCriteria<DomainVO> sdc =
             * _domainDao.createSearchCriteria(); sdc.addOr("path",
             * SearchCriteria.Op.LIKE,
             * _domainDao.findById(caller.getDomainId()).getPath() + "%");
             * List<DomainVO> domains = _domainDao.search(sdc, null); List<Long>
             * domainIds = new ArrayList<Long>(); for (DomainVO domain :
             * domains) domainIds.add(domain.getId()); sc.addAnd("domainId",
             * SearchCriteria.Op.IN, domainIds.toArray());
             * s_logger.debug("Account ID=" + accountId); }
             */
            if (domainId != null) {
                sc.addAnd("domainId", SearchCriteria.Op.EQ, domainId);
                s_logger.debug("Domain ID=" + domainId);
            }
            if (usageType != null) {
                sc.addAnd("usageType", SearchCriteria.Op.EQ, usageType);
                s_logger.debug("usageType ID=" + usageType);
            }
            if ((adjustedStartDate != null) && (adjustedEndDate != null) && adjustedStartDate.before(adjustedEndDate)) {
                sc.addAnd("startDate", SearchCriteria.Op.BETWEEN, adjustedStartDate, adjustedEndDate);
                sc.addAnd("endDate", SearchCriteria.Op.BETWEEN, adjustedStartDate, adjustedEndDate);
                s_logger.debug("start Date=" + adjustedStartDate + ", enddate=" + adjustedEndDate);
            } else {
                s_logger.debug("Screwed up start Date=" + adjustedStartDate + ", enddate=" + adjustedEndDate);
                return new Pair<List<QuotaUsageVO>, Integer>(new ArrayList<QuotaUsageVO>(), new Integer(0));
            }
            quotaUsageRecords = _quotaUsageDao.searchAndCountAllRecords(sc, usageFilter);
        } finally {
            txn.close();
        }

        TransactionLegacy.open(TransactionLegacy.CLOUD_DB).close();
        return quotaUsageRecords;
    }

    @DB
    private void updateQuotaDiskUsage(UsageVO usageRecord, HashMap<Integer, QuotaMappingVO> mapping, BigDecimal aggregationRatio, int quotaType) {
        if (mapping.get(quotaType) != null) {
            QuotaUsageVO quota_usage;
            BigDecimal quotaUsgage;
            BigDecimal onehourcostpergb;
            BigDecimal noofgbinuse;
            s_logger.info(usageRecord.getDescription() + ", " + usageRecord.getType() + ", " + usageRecord.getOfferingId() + ", " + usageRecord.getTemplateId() + ", " + usageRecord.getUsageDisplay()
                    + ", aggrR=" + aggregationRatio);
            onehourcostpergb = mapping.get(quotaType).getCurrencyValue().multiply(aggregationRatio);
            noofgbinuse = new BigDecimal(usageRecord.getSize()).divide(s_gb, 8, RoundingMode.HALF_EVEN);
            quotaUsgage = new BigDecimal(usageRecord.getRawUsage()).multiply(onehourcostpergb).multiply(noofgbinuse);
            s_logger.info(" No of GB In use = " + noofgbinuse + " onehour cost=" + onehourcostpergb);
            quota_usage = new QuotaUsageVO(usageRecord.getId(), usageRecord.getZoneId(), usageRecord.getAccountId(), usageRecord.getDomainId(), usageRecord.getUsageType(), quotaUsgage,
                    usageRecord.getStartDate(), usageRecord.getEndDate());
            _quotaUsageDao.persist(quota_usage);
        }
        usageRecord.setQuotaCalculated(1);
        _usageDao.persist(usageRecord);
    }

    @DB
    private void updateQuotaRunningVMUsage(UsageVO usageRecord, HashMap<Integer, QuotaMappingVO> mapping, BigDecimal aggregationRatio) {
        QuotaUsageVO quota_usage;
        BigDecimal cpuquotausgage, speedquotausage, memoryquotausage, vmusage;
        BigDecimal onehourcostpercpu, onehourcostper100mhz, onehourcostper1mb, onehourcostforvmusage;
        BigDecimal rawusage;
        s_logger.info(usageRecord.getDescription() + ", " + usageRecord.getType() + ", " + usageRecord.getOfferingId() + ", " + usageRecord.getVmInstanceId() + ", " + usageRecord.getUsageDisplay()
                + ", aggrR=" + aggregationRatio);
        // get service offering details
        ServiceOfferingVO serviceoffering = findServiceOffering(usageRecord.getVmInstanceId(), usageRecord.getOfferingId());
        rawusage = new BigDecimal(usageRecord.getRawUsage());

        if (mapping.get(QuotaTypes.CPU_NUMBER) != null) {
            BigDecimal cpu = new BigDecimal(serviceoffering.getCpu());
            onehourcostpercpu = mapping.get(QuotaTypes.CPU_NUMBER).getCurrencyValue().multiply(aggregationRatio);
            cpuquotausgage = rawusage.multiply(onehourcostpercpu).multiply(cpu);
            quota_usage = new QuotaUsageVO(usageRecord.getId(), usageRecord.getZoneId(), usageRecord.getAccountId(), usageRecord.getDomainId(), QuotaTypes.CPU_NUMBER, cpuquotausgage,
                    usageRecord.getStartDate(), usageRecord.getEndDate());
            _quotaUsageDao.persist(quota_usage);
        }
        if (mapping.get(QuotaTypes.CPU_CLOCK_RATE) != null) {
            BigDecimal speed = new BigDecimal(serviceoffering.getSpeed() / 100.00);
            onehourcostper100mhz = mapping.get(QuotaTypes.CPU_CLOCK_RATE).getCurrencyValue().multiply(aggregationRatio);
            speedquotausage = rawusage.multiply(onehourcostper100mhz).multiply(speed);
            quota_usage = new QuotaUsageVO(usageRecord.getId(), usageRecord.getZoneId(), usageRecord.getAccountId(), usageRecord.getDomainId(), QuotaTypes.CPU_CLOCK_RATE, speedquotausage,
                    usageRecord.getStartDate(), usageRecord.getEndDate());
            _quotaUsageDao.persist(quota_usage);
        }
        if (mapping.get(QuotaTypes.MEMORY) != null) {
            BigDecimal memory = new BigDecimal(serviceoffering.getRamSize());
            onehourcostper1mb = mapping.get(QuotaTypes.MEMORY).getCurrencyValue().multiply(aggregationRatio);
            memoryquotausage = rawusage.multiply(onehourcostper1mb).multiply(memory);
            quota_usage = new QuotaUsageVO(usageRecord.getId(), usageRecord.getZoneId(), usageRecord.getAccountId(), usageRecord.getDomainId(), QuotaTypes.MEMORY, memoryquotausage,
                    usageRecord.getStartDate(), usageRecord.getEndDate());
            _quotaUsageDao.persist(quota_usage);
        }
        if (mapping.get(QuotaTypes.RUNNING_VM) != null) {
            onehourcostforvmusage = mapping.get(QuotaTypes.RUNNING_VM).getCurrencyValue().multiply(aggregationRatio);
            vmusage = rawusage.multiply(onehourcostforvmusage);
            quota_usage = new QuotaUsageVO(usageRecord.getId(), usageRecord.getZoneId(), usageRecord.getAccountId(), usageRecord.getDomainId(), QuotaTypes.RUNNING_VM, vmusage,
                    usageRecord.getStartDate(), usageRecord.getEndDate());
            _quotaUsageDao.persist(quota_usage);
        }

        usageRecord.setQuotaCalculated(1);
        _usageDao.persist(usageRecord);
    }

    @DB
    private void updateQuotaAllocatedVMUsage(UsageVO usageRecord, HashMap<Integer, QuotaMappingVO> mapping, BigDecimal aggregationRatio) {
        if (mapping.get(QuotaTypes.ALLOCATED_VM) != null) {
            QuotaUsageVO quota_usage;
            BigDecimal vmusage;
            BigDecimal onehourcostforvmusage;
            s_logger.info(usageRecord.getDescription() + ", " + usageRecord.getType() + ", " + usageRecord.getOfferingId() + ", " + usageRecord.getVmInstanceId() + ", "
                    + usageRecord.getUsageDisplay() + ", aggrR=" + aggregationRatio);

            onehourcostforvmusage = mapping.get(QuotaTypes.ALLOCATED_VM).getCurrencyValue().multiply(aggregationRatio);
            vmusage = new BigDecimal(usageRecord.getRawUsage()).multiply(onehourcostforvmusage);
            quota_usage = new QuotaUsageVO(usageRecord.getId(), usageRecord.getZoneId(), usageRecord.getAccountId(), usageRecord.getDomainId(), QuotaTypes.ALLOCATED_VM, vmusage,
                    usageRecord.getStartDate(), usageRecord.getEndDate());
            _quotaUsageDao.persist(quota_usage);
        }

        usageRecord.setQuotaCalculated(1);
        _usageDao.persist(usageRecord);
    }

    @DB
    private void updateQuotaRaw(UsageVO usageRecord, HashMap<Integer, QuotaMappingVO> mapping, BigDecimal aggregationRatio, int ruleType) {
        if (mapping.get(ruleType) != null) {
            QuotaUsageVO quota_usage;
            BigDecimal ruleusage;
            BigDecimal onehourcost;
            s_logger.info(usageRecord.getDescription() + ", " + usageRecord.getType() + ", " + usageRecord.getOfferingId() + ", " + usageRecord.getVmInstanceId() + ", "
                    + usageRecord.getUsageDisplay() + ", aggrR=" + aggregationRatio);

            onehourcost = mapping.get(ruleType).getCurrencyValue().multiply(aggregationRatio);
            ruleusage = new BigDecimal(usageRecord.getRawUsage()).multiply(onehourcost);
            quota_usage = new QuotaUsageVO(usageRecord.getId(), usageRecord.getZoneId(), usageRecord.getAccountId(), usageRecord.getDomainId(), ruleType, ruleusage, usageRecord.getStartDate(),
                    usageRecord.getEndDate());
            _quotaUsageDao.persist(quota_usage);
        }

        usageRecord.setQuotaCalculated(1);
        _usageDao.persist(usageRecord);
    }

    @DB
    private void updateQuotaNetwork(UsageVO usageRecord, HashMap<Integer, QuotaMappingVO> mapping, int transferType) {
        if (mapping.get(transferType) != null) {
            QuotaUsageVO quota_usage;
            BigDecimal onegbcost;
            BigDecimal rawusageingb;
            BigDecimal networkusage;
            s_logger.info(usageRecord.getDescription() + ", " + usageRecord.getType() + ", " + usageRecord.getOfferingId() + ", " + usageRecord.getVmInstanceId() + ", "
                    + usageRecord.getUsageDisplay());

            onegbcost = mapping.get(transferType).getCurrencyValue();
            rawusageingb = new BigDecimal(usageRecord.getRawUsage()).divide(s_gb, 8, RoundingMode.HALF_EVEN);
            networkusage = rawusageingb.multiply(onegbcost);
            quota_usage = new QuotaUsageVO(usageRecord.getId(), usageRecord.getZoneId(), usageRecord.getAccountId(), usageRecord.getDomainId(), transferType, networkusage, usageRecord.getStartDate(),
                    usageRecord.getEndDate());
            _quotaUsageDao.persist(quota_usage);
        }

        usageRecord.setQuotaCalculated(1);
        _usageDao.persist(usageRecord);
    }

    @SuppressWarnings("deprecation")
    public Pair<List<? extends UsageVO>, Integer> getUsageRecords(long accountId, long domainId) {
        s_logger.debug("getting usage records for account: " + accountId + ", domainId: " + domainId);
        Filter usageFilter = new Filter(UsageVO.class, "id", true, s_recordtofetch, null);
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

    public TimeZone getUsageTimezone() {
        return _usageTimezone;
    }

    private Date computeAdjustedTime(Date initialDate, TimeZone targetTZ) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(initialDate);
        TimeZone localTZ = cal.getTimeZone();
        int timezoneOffset = cal.get(Calendar.ZONE_OFFSET);
        if (localTZ.inDaylightTime(initialDate)) {
            timezoneOffset += (60 * 60 * 1000);
        }
        cal.add(Calendar.MILLISECOND, timezoneOffset);

        Date newTime = cal.getTime();

        Calendar calTS = Calendar.getInstance(targetTZ);
        calTS.setTime(newTime);
        timezoneOffset = calTS.get(Calendar.ZONE_OFFSET);
        if (targetTZ.inDaylightTime(initialDate)) {
            timezoneOffset += (60 * 60 * 1000);
        }

        calTS.add(Calendar.MILLISECOND, -1 * timezoneOffset);

        return calTS.getTime();
    }

}
