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
package org.apache.cloudstack.quota.job;

import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.usage.UsageVO;
import com.cloud.usage.dao.UsageDao;
import com.cloud.user.Account;
//import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.user.UserVO;
import com.cloud.user.Account.State;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.UserDao;
import com.cloud.utils.DateUtil;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.utils.exception.CloudRuntimeException;
import com.sun.mail.smtp.SMTPMessage;
import com.sun.mail.smtp.SMTPSSLTransport;
import com.sun.mail.smtp.SMTPTransport;

import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.managed.context.ManagedContextRunnable;
import org.apache.cloudstack.quota.constant.QuotaConfig;
import org.apache.cloudstack.quota.constant.QuotaTypes;
import org.apache.cloudstack.quota.dao.QuotaAccountDao;
import org.apache.cloudstack.quota.dao.QuotaBalanceDao;
import org.apache.cloudstack.quota.dao.QuotaEmailTemplatesDao;
import org.apache.cloudstack.quota.dao.QuotaTariffDao;
import org.apache.cloudstack.quota.dao.QuotaUsageDao;
import org.apache.cloudstack.quota.vo.QuotaAccountVO;
import org.apache.cloudstack.quota.vo.QuotaBalanceVO;
import org.apache.cloudstack.quota.vo.QuotaEmailTemplatesVO;
import org.apache.cloudstack.quota.vo.QuotaTariffVO;
import org.apache.cloudstack.quota.vo.QuotaUsageVO;
import org.apache.cloudstack.quota.vo.ServiceOfferingVO;
import org.apache.cloudstack.quota.dao.ServiceOfferingDao;
import org.apache.cloudstack.utils.usage.UsageUtils;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import javax.ejb.Local;
import javax.inject.Inject;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.URLName;
import javax.mail.internet.InternetAddress;
import javax.naming.ConfigurationException;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;

@Component
@Local(value = QuotaManager.class)
public class QuotaManagerImpl extends ManagerBase implements QuotaManager, Runnable {
    private static final Logger s_logger = Logger.getLogger(QuotaManagerImpl.class.getName());

    @Inject
    private AccountDao _accountDao;
    @Inject
    private QuotaAccountDao _quotaAcc;
    @Inject
    private UserDao _userDao;
    @Inject
    private DomainDao _domainDao;
    @Inject
    private UsageDao _usageDao;
    @Inject
    private QuotaTariffDao _quotaTariffDao;
    @Inject
    private QuotaUsageDao _quotaUsageDao;
    @Inject
    private QuotaEmailTemplatesDao _quotaEmailTemplateDao;
    @Inject
    private ServiceOfferingDao _serviceOfferingDao;
    @Inject
    private QuotaBalanceDao _quotaBalanceDao;
    @Inject
    private ConfigurationDao _configDao;

    private TimeZone _usageTimezone;
    private int _aggregationDuration = 0;

    private EmailQuotaAlert _emailQuotaAlert;

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

        final String smtpHost = QuotaConfig.QuotaSmtpHost.value();
        int smtpPort = NumbersUtil.parseInt(QuotaConfig.QuotaSmtpPort.value(), 25);
        String useAuthStr = QuotaConfig.QuotaSmtpAuthType.value();
        boolean useAuth = ((useAuthStr != null) && Boolean.parseBoolean(useAuthStr));
        String smtpUsername = QuotaConfig.QuotaSmtpUser.value();
        String smtpPassword = QuotaConfig.QuotaSmtpPassword.value();
        String emailSender = QuotaConfig.QuotaSmtpSender.value();
        boolean smtpDebug = false;
        _emailQuotaAlert = new EmailQuotaAlert(smtpHost, smtpPort, useAuth, smtpUsername, smtpPassword, emailSender, smtpDebug);

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
    public void run() {
        (new ManagedContextRunnable() {
            @Override
            protected void runInContext() {
                System.out.println("Running Quota thread .....");
                calculateQuotaUsage();
            }
        }).run();
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
                            quotalistforaccount.addAll(updateQuotaRunningVMUsage(usageRecord, aggregationRatio));
                            break;
                        case QuotaTypes.ALLOCATED_VM:
                            quotalistforaccount.add(updateQuotaAllocatedVMUsage(usageRecord, aggregationRatio));
                            break;
                        case QuotaTypes.SNAPSHOT:
                            quotalistforaccount.add(updateQuotaDiskUsage(usageRecord, aggregationRatio, QuotaTypes.SNAPSHOT));
                            break;
                        case QuotaTypes.TEMPLATE:
                            quotalistforaccount.add(updateQuotaDiskUsage(usageRecord, aggregationRatio, QuotaTypes.TEMPLATE));
                            break;
                        case QuotaTypes.ISO:
                            quotalistforaccount.add(updateQuotaDiskUsage(usageRecord, aggregationRatio, QuotaTypes.ISO));
                            break;
                        case QuotaTypes.VOLUME:
                            quotalistforaccount.add(updateQuotaDiskUsage(usageRecord, aggregationRatio, QuotaTypes.VOLUME));
                            break;
                        case QuotaTypes.VM_SNAPSHOT:
                            quotalistforaccount.add(updateQuotaDiskUsage(usageRecord, aggregationRatio, QuotaTypes.VM_SNAPSHOT));
                            break;
                        case QuotaTypes.LOAD_BALANCER_POLICY:
                            quotalistforaccount.add(updateQuotaRaw(usageRecord, aggregationRatio, QuotaTypes.LOAD_BALANCER_POLICY));
                            break;
                        case QuotaTypes.PORT_FORWARDING_RULE:
                            quotalistforaccount.add(updateQuotaRaw(usageRecord, aggregationRatio, QuotaTypes.PORT_FORWARDING_RULE));
                            break;
                        case QuotaTypes.IP_ADDRESS:
                            quotalistforaccount.add(updateQuotaRaw(usageRecord, aggregationRatio, QuotaTypes.IP_ADDRESS));
                            break;
                        case QuotaTypes.NETWORK_OFFERING:
                            quotalistforaccount.add(updateQuotaRaw(usageRecord, aggregationRatio, QuotaTypes.NETWORK_OFFERING));
                            break;
                        case QuotaTypes.SECURITY_GROUP:
                            quotalistforaccount.add(updateQuotaRaw(usageRecord, aggregationRatio, QuotaTypes.SECURITY_GROUP));
                            break;
                        case QuotaTypes.VPN_USERS:
                            quotalistforaccount.add(updateQuotaRaw(usageRecord, aggregationRatio, QuotaTypes.VPN_USERS));
                            break;
                        case QuotaTypes.NETWORK_BYTES_RECEIVED:
                            quotalistforaccount.add(updateQuotaNetwork(usageRecord, QuotaTypes.NETWORK_BYTES_RECEIVED));
                            break;
                        case QuotaTypes.NETWORK_BYTES_SENT:
                            quotalistforaccount.add(updateQuotaNetwork(usageRecord, QuotaTypes.NETWORK_BYTES_SENT));
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
                            // s_logger.info("Balance entry=" + aggrUsage + " on Date=" + endDate);
                            _quotaBalanceDao.persist(newbalance);
                            aggrUsage = new BigDecimal(0);
                        }
                        startDate = entry.getStartDate();
                        endDate = entry.getEndDate();
                        aggrUsage = aggrUsage.subtract(entry.getQuotaUsed());
                    }
                    // update is quota_accounts
                    QuotaAccountVO quota_account = _quotaAcc.findById(account.getAccountId());
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
            e.printStackTrace();
        } finally {
            txn.close();
        }
        TransactionLegacy.open(opendb).close();
        checkAndSendQuotaAlertEmails();
        return jobResult;
    }

    private void checkAndSendQuotaAlertEmails() {
        List<DeferredQuotaEmail> deferredQuotaEmailList = new ArrayList<DeferredQuotaEmail>();
        final Date currentDate = startOfNextDay();
        final BigDecimal zeroBalance = new BigDecimal(0);
        final BigDecimal thresholdBalance = new BigDecimal(QuotaConfig.QuotaLimitCritical.value());
        final boolean lockAccountEnforcement = QuotaConfig.QuotaEnableEnforcement.value().equalsIgnoreCase("true");
        for (final AccountVO account : _accountDao.listAll()) {
            final BigDecimal accountBalance = _quotaBalanceDao.lastQuotaBalance(account.getId(), account.getDomainId(), currentDate);
            if (accountBalance != null) {
                if (accountBalance.compareTo(zeroBalance) <= 0) {
                    if (lockAccountEnforcement && account.getType() == Account.ACCOUNT_TYPE_NORMAL) {
                        lockAccount(account.getId());
                    }
                    deferredQuotaEmailList.add(new DeferredQuotaEmail(account, accountBalance, QuotaConfig.QuotaEmailTemplateTypes.QUOTA_EMPTY));
                } else if (accountBalance.compareTo(thresholdBalance) <= 0) {
                    deferredQuotaEmailList.add(new DeferredQuotaEmail(account, accountBalance, QuotaConfig.QuotaEmailTemplateTypes.QUOTA_LOW));
                }
            }
        }

        for (DeferredQuotaEmail emailToBeSent : deferredQuotaEmailList) {
            s_logger.debug("Attempting to send quota alert email to users of account: " + emailToBeSent.getAccount().getAccountName());
            sendQuotaAlert(emailToBeSent);
        }
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
    private List<QuotaUsageVO> updateQuotaRunningVMUsage(UsageVO usageRecord, final BigDecimal aggregationRatio) {
        List<QuotaUsageVO> quotalist = new ArrayList<QuotaUsageVO>();
        QuotaUsageVO quota_usage;
        BigDecimal cpuquotausgage, speedquotausage, memoryquotausage, vmusage;
        BigDecimal onehourcostpercpu, onehourcostper100mhz, onehourcostper1mb, onehourcostforvmusage;
        BigDecimal rawusage;
        // s_logger.info(usageRecord.getDescription() + ", " +
        // usageRecord.getType() + ", " + usageRecord.getOfferingId() + ", " +
        // usageRecord.getVmInstanceId() + ", " + usageRecord.getUsageDisplay()
        // + ", aggrR=" + aggregationRatio);
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
            // s_logger.info("Quotatariff onehourcostforvmusage=" +
            // onehourcostforvmusage);
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
            // s_logger.info("Quotatariff onehourcost=" + onehourcost);
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
            // s_logger.info("Quotatariff onegbcost=" + onegbcost);
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

    public void sendQuotaAlert(AccountVO account, BigDecimal balance, QuotaConfig.QuotaEmailTemplateTypes emailType) {
        sendQuotaAlert(new DeferredQuotaEmail(account, balance, emailType));
    }

    private void sendQuotaAlert(DeferredQuotaEmail emailToBeSent) {
        final AccountVO account = emailToBeSent.getAccount();
        final BigDecimal balance = emailToBeSent.getQuotaBalance();
        final QuotaConfig.QuotaEmailTemplateTypes emailType = emailToBeSent.getEmailTemplateType();

        final List<QuotaEmailTemplatesVO> emailTemplates = _quotaEmailTemplateDao.listAllQuotaEmailTemplates(emailType.toString());
        if (emailTemplates != null && emailTemplates.get(0) != null) {
            final QuotaEmailTemplatesVO emailTemplate = emailTemplates.get(0);

            final DomainVO accountDomain = _domainDao.findByIdIncludingRemoved(account.getDomainId());
            final List<UserVO> usersInAccount = _userDao.listByAccount(account.getId());

            String userNames = "";
            final List<String> emailRecipients = new ArrayList<String>();
            for (UserVO user : usersInAccount) {
                userNames += String.format("%s <%s>,", user.getUsername(), user.getEmail());
                emailRecipients.add(user.getEmail());
            }
            if (userNames.endsWith(",")) {
                userNames = userNames.substring(0, userNames.length() - 1);
            }

            final Map<String, String> optionMap = new HashMap<String, String>();
            optionMap.put("accountName", account.getAccountName());
            optionMap.put("accountID", account.getUuid());
            optionMap.put("accountUsers", userNames);
            optionMap.put("domainName", accountDomain.getName());
            optionMap.put("domainID", accountDomain.getUuid());
            optionMap.put("quotaBalance", QuotaConfig.QuotaCurrencySymbol.value() + " " + balance.toString());

            final StrSubstitutor templateEngine = new StrSubstitutor(optionMap);
            final String subject = templateEngine.replace(emailTemplate.getTemplateSubject());
            final String body = templateEngine.replace(emailTemplate.getTemplateBody());
            try {
                _emailQuotaAlert.sendQuotaAlert(emailRecipients, subject, body);
            } catch (Exception e) {
                s_logger.error(String.format("Unable to send quota alert email (subject=%s; body=%s) to account %s (%s) recipients (%s) due to error (%s)", subject, body, account.getAccountName(),
                        account.getUuid(), emailRecipients, e));
            }
        } else {
            s_logger.error(String.format("No quota email template found for type %s, cannot send quota alert email to account %s(%s)", emailType, account.getAccountName(), account.getUuid()));
        }
    }

    class DeferredQuotaEmail {
        AccountVO account;
        BigDecimal quotaBalance;
        QuotaConfig.QuotaEmailTemplateTypes emailTemplateType;

        public DeferredQuotaEmail(AccountVO account, BigDecimal quotaBalance, QuotaConfig.QuotaEmailTemplateTypes emailTemplateType) {
            this.account = account;
            this.quotaBalance = quotaBalance;
            this.emailTemplateType = emailTemplateType;
        }

        public AccountVO getAccount() {
            return account;
        }

        public BigDecimal getQuotaBalance() {
            return quotaBalance;
        }

        public QuotaConfig.QuotaEmailTemplateTypes getEmailTemplateType() {
            return emailTemplateType;
        }
    };

    class EmailQuotaAlert {
        private Session _smtpSession;
        private final String _smtpHost;
        private int _smtpPort = -1;
        private boolean _smtpUseAuth = false;
        private final String _smtpUsername;
        private final String _smtpPassword;
        private final String _emailSender;

        public EmailQuotaAlert(String smtpHost, int smtpPort, boolean smtpUseAuth, final String smtpUsername, final String smtpPassword, String emailSender, boolean smtpDebug) {
            _smtpHost = smtpHost;
            _smtpPort = smtpPort;
            _smtpUseAuth = smtpUseAuth;
            _smtpUsername = smtpUsername;
            _smtpPassword = smtpPassword;
            _emailSender = emailSender;

            if (_smtpHost != null) {
                Properties smtpProps = new Properties();
                smtpProps.put("mail.smtp.host", smtpHost);
                smtpProps.put("mail.smtp.port", smtpPort);
                smtpProps.put("mail.smtp.auth", "" + smtpUseAuth);
                if (smtpUsername != null) {
                    smtpProps.put("mail.smtp.user", smtpUsername);
                }

                smtpProps.put("mail.smtps.host", smtpHost);
                smtpProps.put("mail.smtps.port", smtpPort);
                smtpProps.put("mail.smtps.auth", "" + smtpUseAuth);
                if (smtpUsername != null) {
                    smtpProps.put("mail.smtps.user", smtpUsername);
                }

                if ((smtpUsername != null) && (smtpPassword != null)) {
                    _smtpSession = Session.getInstance(smtpProps, new Authenticator() {
                        @Override
                        protected PasswordAuthentication getPasswordAuthentication() {
                            return new PasswordAuthentication(smtpUsername, smtpPassword);
                        }
                    });
                } else {
                    _smtpSession = Session.getInstance(smtpProps);
                }
                _smtpSession.setDebug(smtpDebug);
            } else {
                _smtpSession = null;
            }
        }

        public void sendQuotaAlert(List<String> emails, String subject, String body) throws MessagingException, UnsupportedEncodingException {
            if (_smtpSession != null) {
                SMTPMessage msg = new SMTPMessage(_smtpSession);
                msg.setSender(new InternetAddress(_emailSender, _emailSender));
                msg.setFrom(new InternetAddress(_emailSender, _emailSender));

                for (String email : emails) {
                    if (email != null && !email.isEmpty()) {
                        try {
                            InternetAddress address = new InternetAddress(email, email);
                            msg.addRecipient(Message.RecipientType.TO, address);
                        } catch (Exception pokemon) {
                            s_logger.error("Exception in creating address for:" + email, pokemon);
                        }
                    }
                }

                msg.setSubject(subject);
                msg.setSentDate(new Date(DateUtil.currentGMTTime().getTime() >> 10));
                msg.setContent(body, "text/html; charset=utf-8");
                msg.saveChanges();

                SMTPTransport smtpTrans = null;
                if (_smtpUseAuth) {
                    smtpTrans = new SMTPSSLTransport(_smtpSession, new URLName("smtp", _smtpHost, _smtpPort, null, _smtpUsername, _smtpPassword));
                } else {
                    smtpTrans = new SMTPTransport(_smtpSession, new URLName("smtp", _smtpHost, _smtpPort, null, _smtpUsername, _smtpPassword));
                }
                smtpTrans.connect();
                smtpTrans.sendMessage(msg, msg.getAllRecipients());
                smtpTrans.close();
            } else {
                throw new CloudRuntimeException("Unable to send quota alert email");
            }
        }
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
