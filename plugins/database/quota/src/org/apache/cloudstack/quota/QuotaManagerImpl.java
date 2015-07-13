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
import java.util.HashMap;
import java.util.List;

import javax.ejb.Local;
import javax.inject.Inject;

import org.apache.cloudstack.api.command.QuotaMapping;
import org.apache.cloudstack.api.command.QuotaCreditsCmd;
import org.apache.cloudstack.api.command.QuotaEditMappingCmd;
import org.apache.cloudstack.api.command.QuotaEmailTemplateAddCmd;
import org.apache.cloudstack.api.command.QuotaRefreshCmd;
import org.apache.cloudstack.api.command.QuotaStatementCmd;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.managed.context.ManagedContextRunnable;
import org.apache.cloudstack.quota.dao.QuotaMappingDao;
import org.apache.cloudstack.quota.dao.QuotaUsageDao;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.usage.UsageVO;
import com.cloud.usage.dao.UsageDao;
import com.cloud.user.AccountVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.TransactionLegacy;

@Component
@Local(value = QuotaManager.class)
public class QuotaManagerImpl extends ManagerBase implements QuotaManager, Configurable, QuotaConfig, Runnable  {
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

    static BigDecimal s_hoursInMonth = new BigDecimal(30.00 * 24.00);
    static BigDecimal s_gb = new BigDecimal(1024 * 1024 * 1024);

    public QuotaManagerImpl() {
        super();
    }

    @Override
    public List<Class<?>> getCommands() {
        final List<Class<?>> cmdList = new ArrayList<Class<?>>();
        cmdList.add(QuotaMapping.class);
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
    public void calculateQuotaUsage() {
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
                s_logger.info("Account =" + account.getAccountName());
                Pair<List<? extends UsageVO>, Integer> usageRecords = getUsageRecords(account.getAccountId(), account.getDomainId());
                s_logger.debug("Usage records found " + usageRecords.second());
                for (UsageVO usageRecord : usageRecords.first()) {
                    s_logger.info("Type=" + usageRecord.getUsageType());
                    switch (usageRecord.getUsageType()) {
                    case QuotaTypes.RUNNING_VM:
                        updateQuotaRunningVMUsage(usageRecord, mapping);
                        break;
                    case QuotaTypes.ALLOCATED_VM:
                        updateQuotaAllocatedVMUsage(usageRecord, mapping);
                        break;
                    case QuotaTypes.SNAPSHOT:
                        updateQuotaDiskUsage(usageRecord, mapping, QuotaTypes.SNAPSHOT);
                        break;
                    case QuotaTypes.TEMPLATE:
                        updateQuotaDiskUsage(usageRecord, mapping, QuotaTypes.TEMPLATE);
                        break;
                    case QuotaTypes.ISO:
                        updateQuotaDiskUsage(usageRecord, mapping, QuotaTypes.ISO);
                        break;
                    case QuotaTypes.VOLUME:
                        updateQuotaDiskUsage(usageRecord, mapping, QuotaTypes.VOLUME);
                        break;
                    case QuotaTypes.VM_SNAPSHOT:
                        updateQuotaDiskUsage(usageRecord, mapping, QuotaTypes.VM_SNAPSHOT);
                        break;
                    case QuotaTypes.LOAD_BALANCER_POLICY:
                        updateQuotaRaw(usageRecord, mapping, QuotaTypes.LOAD_BALANCER_POLICY);
                        break;
                    case QuotaTypes.PORT_FORWARDING_RULE:
                        updateQuotaRaw(usageRecord, mapping, QuotaTypes.PORT_FORWARDING_RULE);
                        break;
                    case QuotaTypes.IP_ADDRESS:
                        updateQuotaRaw(usageRecord, mapping, QuotaTypes.IP_ADDRESS);
                        break;
                    case QuotaTypes.NETWORK_OFFERING:
                        updateQuotaRaw(usageRecord, mapping, QuotaTypes.NETWORK_OFFERING);
                        break;
                    case QuotaTypes.SECURITY_GROUP:
                        updateQuotaRaw(usageRecord, mapping, QuotaTypes.SECURITY_GROUP);
                        break;
                    case QuotaTypes.VPN_USERS:
                        updateQuotaRaw(usageRecord, mapping, QuotaTypes.VPN_USERS);
                        break;
                    case QuotaTypes.NETWORK_BYTES_RECEIVED:
                        updateQuotaRaw(usageRecord, mapping, QuotaTypes.NETWORK_BYTES_RECEIVED);
                        break;
                    case QuotaTypes.NETWORK_BYTES_SENT:
                        updateQuotaRaw(usageRecord, mapping, QuotaTypes.NETWORK_BYTES_SENT);
                        break;
                    case QuotaTypes.VM_DISK_IO_READ:
                    case QuotaTypes.VM_DISK_IO_WRITE:
                    case QuotaTypes.VM_DISK_BYTES_READ:
                    case QuotaTypes.VM_DISK_BYTES_WRITE:
                    default:
                        break;
                    }
                }
            }

        } catch (Exception e) {
            s_logger.error("Quota Manager error", e);
            e.printStackTrace();
        } finally {
            txn.close();
        }
    }

    @DB
    private void updateQuotaDiskUsage(UsageVO usageRecord, HashMap<Integer, QuotaMappingVO> mapping, int quotaType) {
        if (mapping.get(quotaType) != null) {
            QuotaUsageVO quota_usage;
            BigDecimal quotaUsgage;
            BigDecimal onehourcostpergb;
            BigDecimal noofgbinuse;
            s_logger.info(usageRecord.getDescription() + ", " + usageRecord.getType() + ", " + usageRecord.getOfferingId() + ", " + usageRecord.getTemplateId() + ", " + usageRecord.getUsageDisplay());
            onehourcostpergb = mapping.get(quotaType).getCurrencyValue().divide(s_hoursInMonth, 4, RoundingMode.HALF_EVEN);
            noofgbinuse = new BigDecimal(usageRecord.getSize()).divide(s_gb, 4, RoundingMode.HALF_EVEN);
            quotaUsgage = new BigDecimal(usageRecord.getRawUsage()).multiply(onehourcostpergb).multiply(noofgbinuse);
            s_logger.info(" No of GB In use = " + noofgbinuse + " onehour cost=" + onehourcostpergb);
            quota_usage = new QuotaUsageVO(usageRecord.getId(), usageRecord.getUsageType(), quotaUsgage, usageRecord.getStartDate(), usageRecord.getEndDate());
            _quotaUsageDao.persist(quota_usage);
        }
        usageRecord.setQuotaCalculated(1);
        _usageDao.persist(usageRecord);
    }

    @DB
    private void updateQuotaRunningVMUsage(UsageVO usageRecord, HashMap<Integer, QuotaMappingVO> mapping) {
        QuotaUsageVO quota_usage;
        BigDecimal cpuquotausgage, speedquotausage, memoryquotausage, vmusage;
        BigDecimal onehourcostpercpu, onehourcostper100mhz, onehourcostper1mb, onehourcostforvmusage;
        BigDecimal rawusage;
        s_logger.info(usageRecord.getDescription() + ", " + usageRecord.getType() + ", " + usageRecord.getOfferingId() + ", " + usageRecord.getVmInstanceId() + ", " + usageRecord.getUsageDisplay());
        // get service offering details
        ServiceOfferingVO serviceoffering = findServiceOffering(usageRecord.getVmInstanceId(), usageRecord.getOfferingId());
        rawusage = new BigDecimal(usageRecord.getRawUsage());

        if (mapping.get(QuotaTypes.CPU_NUMBER) == null) {
            BigDecimal cpu = new BigDecimal(serviceoffering.getCpu());
            onehourcostpercpu = mapping.get(QuotaTypes.CPU_NUMBER).getCurrencyValue().divide(s_hoursInMonth, 4, RoundingMode.HALF_EVEN);
            cpuquotausgage = rawusage.multiply(onehourcostpercpu).multiply(cpu);
            quota_usage = new QuotaUsageVO(usageRecord.getId(), QuotaTypes.CPU_NUMBER, cpuquotausgage, usageRecord.getStartDate(), usageRecord.getEndDate());
            _quotaUsageDao.persist(quota_usage);
        }
        if (mapping.get(QuotaTypes.CPU_CLOCK_RATE) == null) {
            BigDecimal speed = new BigDecimal(serviceoffering.getSpeed() / 100.00);
            onehourcostper100mhz = mapping.get(QuotaTypes.CPU_CLOCK_RATE).getCurrencyValue().divide(s_hoursInMonth, 4, RoundingMode.HALF_EVEN);
            speedquotausage = rawusage.multiply(onehourcostper100mhz).multiply(speed);
            quota_usage = new QuotaUsageVO(usageRecord.getId(), QuotaTypes.CPU_CLOCK_RATE, speedquotausage, usageRecord.getStartDate(), usageRecord.getEndDate());
            _quotaUsageDao.persist(quota_usage);
        }
        if (mapping.get(QuotaTypes.MEMORY) == null) {
            BigDecimal memory = new BigDecimal(serviceoffering.getRamSize());
            onehourcostper1mb = mapping.get(QuotaTypes.MEMORY).getCurrencyValue().divide(s_hoursInMonth, 4, RoundingMode.HALF_EVEN);
            memoryquotausage = rawusage.multiply(onehourcostper1mb).multiply(memory);
            quota_usage = new QuotaUsageVO(usageRecord.getId(), QuotaTypes.MEMORY, memoryquotausage, usageRecord.getStartDate(), usageRecord.getEndDate());
            _quotaUsageDao.persist(quota_usage);
        }
        if (mapping.get(QuotaTypes.RUNNING_VM) == null) {
            onehourcostforvmusage = mapping.get(QuotaTypes.RUNNING_VM).getCurrencyValue().divide(s_hoursInMonth, 4, RoundingMode.HALF_EVEN);
            vmusage = rawusage.multiply(onehourcostforvmusage);
            quota_usage = new QuotaUsageVO(usageRecord.getId(), QuotaTypes.RUNNING_VM, vmusage, usageRecord.getStartDate(), usageRecord.getEndDate());
            _quotaUsageDao.persist(quota_usage);
        }

        usageRecord.setQuotaCalculated(1);
        _usageDao.persist(usageRecord);
    }

    @DB
    private void updateQuotaAllocatedVMUsage(UsageVO usageRecord, HashMap<Integer, QuotaMappingVO> mapping) {
        if (mapping.get(QuotaTypes.ALLOCATED_VM) != null) {
            QuotaUsageVO quota_usage;
            BigDecimal vmusage;
            BigDecimal onehourcostforvmusage;
            s_logger.info(usageRecord.getDescription() + ", " + usageRecord.getType() + ", " + usageRecord.getOfferingId() + ", " + usageRecord.getVmInstanceId() + ", "
                    + usageRecord.getUsageDisplay());

            onehourcostforvmusage = mapping.get(QuotaTypes.ALLOCATED_VM).getCurrencyValue().divide(s_hoursInMonth, 4, RoundingMode.HALF_EVEN);
            vmusage = new BigDecimal(usageRecord.getRawUsage()).multiply(onehourcostforvmusage);
            quota_usage = new QuotaUsageVO(usageRecord.getId(), QuotaTypes.ALLOCATED_VM, vmusage, usageRecord.getStartDate(), usageRecord.getEndDate());
            _quotaUsageDao.persist(quota_usage);
        }

        usageRecord.setQuotaCalculated(1);
        _usageDao.persist(usageRecord);
    }

    @DB
    private void updateQuotaRaw(UsageVO usageRecord, HashMap<Integer, QuotaMappingVO> mapping, int ruleType) {
        if (mapping.get(ruleType) != null) {
            QuotaUsageVO quota_usage;
            BigDecimal ruleusage;
            BigDecimal onehourcost;
            s_logger.info(usageRecord.getDescription() + ", " + usageRecord.getType() + ", " + usageRecord.getOfferingId() + ", " + usageRecord.getVmInstanceId() + ", "
                    + usageRecord.getUsageDisplay());

            onehourcost = mapping.get(ruleType).getCurrencyValue().divide(s_hoursInMonth, 4, RoundingMode.HALF_EVEN);
            ruleusage = new BigDecimal(usageRecord.getRawUsage()).multiply(onehourcost);
            quota_usage = new QuotaUsageVO(usageRecord.getId(), ruleType, ruleusage, usageRecord.getStartDate(), usageRecord.getEndDate());
            _quotaUsageDao.persist(quota_usage);
        }

        usageRecord.setQuotaCalculated(1);
        _usageDao.persist(usageRecord);
    }

    @DB
    private void updateQuotaNetwork(UsageVO usageRecord, HashMap<Integer, QuotaMappingVO> mapping, int transferType) {
        if (mapping.get(transferType) != null) {
            QuotaUsageVO quota_usage;
            BigDecimal onehourcost;
            BigDecimal rawusageingb;
            BigDecimal networkusage;
            s_logger.info(usageRecord.getDescription() + ", " + usageRecord.getType() + ", " + usageRecord.getOfferingId() + ", " + usageRecord.getVmInstanceId() + ", "
                    + usageRecord.getUsageDisplay());

            onehourcost = mapping.get(transferType).getCurrencyValue().divide(s_hoursInMonth, 4, RoundingMode.HALF_EVEN);
            rawusageingb = new BigDecimal(usageRecord.getRawUsage()).divide(s_gb, 4, RoundingMode.HALF_EVEN);
            networkusage = rawusageingb.multiply(onehourcost);
            quota_usage = new QuotaUsageVO(usageRecord.getId(), transferType, networkusage, usageRecord.getStartDate(), usageRecord.getEndDate());
            _quotaUsageDao.persist(quota_usage);
        }

        usageRecord.setQuotaCalculated(1);
        _usageDao.persist(usageRecord);
    }

    @SuppressWarnings("deprecation")
    public Pair<List<? extends UsageVO>, Integer> getUsageRecords(long accountId, long domainId) {
        s_logger.debug("getting usage records for account: " + accountId + ", domainId: " + domainId);
        Filter usageFilter = new Filter(UsageVO.class, "id", true, 10000L, null);
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

    @Override
    public void run() {
        (new ManagedContextRunnable() {
            @Override
            protected void runInContext() {
                calculateQuotaUsage();
            }
        }).run();
    }

}
