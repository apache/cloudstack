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
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import com.cloud.utils.DateUtil;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.quota.constant.QuotaConfig;
import org.apache.cloudstack.quota.constant.QuotaConfig.QuotaEmailTemplateTypes;
import org.apache.cloudstack.quota.dao.QuotaAccountDao;
import org.apache.cloudstack.quota.dao.QuotaEmailConfigurationDao;
import org.apache.cloudstack.quota.dao.QuotaEmailTemplatesDao;
import org.apache.cloudstack.quota.vo.QuotaAccountVO;
import org.apache.cloudstack.quota.vo.QuotaEmailConfigurationVO;
import org.apache.cloudstack.quota.vo.QuotaEmailTemplatesVO;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.text.StrSubstitutor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Component;

import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.user.Account;
import com.cloud.user.Account.State;
import com.cloud.user.AccountVO;
import com.cloud.user.UserVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.UserDao;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.TransactionLegacy;
import java.util.HashSet;
import java.util.Set;
import org.apache.cloudstack.utils.mailing.MailAddress;
import org.apache.cloudstack.utils.mailing.SMTPMailProperties;
import org.apache.cloudstack.utils.mailing.SMTPMailSender;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;

@Component
public class QuotaAlertManagerImpl extends ManagerBase implements QuotaAlertManager {

    @Inject
    private AccountDao _accountDao;
    @Inject
    private QuotaAccountDao _quotaAcc;
    @Inject
    private UserDao _userDao;
    @Inject
    private DomainDao _domainDao;
    @Inject
    private QuotaEmailTemplatesDao _quotaEmailTemplateDao;
    @Inject
    private ConfigurationDao _configDao;
    @Inject
    private QuotaManager _quotaManager;

    @Inject
    private QuotaEmailConfigurationDao quotaEmailConfigurationDao;

    protected boolean _lockAccountEnforcement = false;
    private String senderAddress;
    protected SMTPMailSender mailSender;

    boolean _smtpDebug = false;

    static final String ACCOUNT_NAME = "accountName";
    static final String ACCOUNT_USERS = "accountUsers";
    static final String DOMAIN_NAME = "domainName";

    public QuotaAlertManagerImpl() {
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

        senderAddress = configs.get(QuotaConfig.QuotaSmtpSender.key());
        _lockAccountEnforcement = BooleanUtils.toBoolean(configs.get(QuotaConfig.QuotaEnableEnforcement.key()));
        String smtpUsername = configs.get(QuotaConfig.QuotaSmtpUser.key());

        String namespace = "quota.usage.smtp";
        configs.put(String.format("%s.debug", namespace), String.valueOf(_smtpDebug));
        configs.put(String.format("%s.username", namespace), smtpUsername);

        mailSender = new SMTPMailSender(configs, namespace);

        return true;
    }

    @Override
    public boolean start() {
        if (logger.isInfoEnabled()) {
            logger.info("Starting Alert Manager");
        }
        return true;
    }

    @Override
    public boolean stop() {
        if (logger.isInfoEnabled()) {
            logger.info("Stopping Alert Manager");
        }
        return true;
    }

    /**
     * Returns whether a Quota email type is enabled or not for the provided account.
     */
    @Override
    public boolean isQuotaEmailTypeEnabledForAccount(AccountVO account, QuotaEmailTemplateTypes quotaEmailTemplateType) {
        boolean quotaEmailsEnabled = QuotaConfig.QuotaEnableEmails.valueIn(account.getAccountId());
        if (!quotaEmailsEnabled) {
            logger.debug("Configuration [{}] is disabled for account [{}]. Therefore, the account will not receive Quota email of type [{}].", QuotaConfig.QuotaEnableEmails.key(), account, quotaEmailTemplateType);
            return false;
        }

        QuotaEmailConfigurationVO quotaEmail = quotaEmailConfigurationDao.findByAccountIdAndEmailTemplateType(account.getAccountId(), quotaEmailTemplateType);

        boolean emailEnabled = quotaEmail == null || quotaEmail.isEnabled();
        if (emailEnabled) {
            logger.debug("Quota email [{}] is enabled for account [{}].", quotaEmailTemplateType, account);
        } else {
            logger.debug("Quota email [{}] has been manually disabled for account [{}] through the API quotaConfigureEmail.", quotaEmailTemplateType, account);
        }
        return emailEnabled;
    }


    @Override
    public void checkAndSendQuotaAlertEmails() {
        List<DeferredQuotaEmail> deferredQuotaEmailList = new ArrayList<DeferredQuotaEmail>();

        logger.info("Checking and sending quota alert emails.");
        for (final QuotaAccountVO quotaAccount : _quotaAcc.listAllQuotaAccount()) {
            checkQuotaAlertEmailForAccount(deferredQuotaEmailList, quotaAccount);
        }

        for (DeferredQuotaEmail emailToBeSent : deferredQuotaEmailList) {
            logger.debug("Attempting to send a quota alert email to users of account [{}].", emailToBeSent.getAccount().getAccountName());
            sendQuotaAlert(emailToBeSent);
        }
    }

    /**
     * Checks a given quota account to see if they should receive any emails. First by checking if it has any balance at all, if its account can be found, then checks
     * if they should receive either QUOTA_EMPTY or QUOTA_LOW emails, taking into account if these email templates are disabled or not for that account.
     * */
    protected void checkQuotaAlertEmailForAccount(List<DeferredQuotaEmail> deferredQuotaEmailList, QuotaAccountVO quotaAccount) {
        logger.debug("Checking {} for email alerts.", quotaAccount);
        BigDecimal accountBalance = quotaAccount.getQuotaBalance();

        if (accountBalance == null) {
            logger.debug("{} has a null balance, therefore it will not receive quota alert emails.", quotaAccount);
            return;
        }

        AccountVO account = _accountDao.findById(quotaAccount.getId());
        if (account == null) {
            logger.debug("Account of {} is removed, thus it will not receive quota alert emails.", quotaAccount);
            return;
        }

        checkBalanceAndAddToEmailList(deferredQuotaEmailList, quotaAccount, account, accountBalance);
    }

    private void checkBalanceAndAddToEmailList(List<DeferredQuotaEmail> deferredQuotaEmailList, QuotaAccountVO quotaAccount, AccountVO account, BigDecimal accountBalance) {
        Date balanceDate = quotaAccount.getQuotaBalanceDate();
        Date alertDate = quotaAccount.getQuotaAlertDate();
        int lockable = quotaAccount.getQuotaEnforce();
        BigDecimal thresholdBalance = quotaAccount.getQuotaMinBalance();

        logger.debug("Checking {} with accountBalance [{}], alertDate [{}] and lockable [{}] to see if a quota alert email should be sent.", account,
                accountBalance, DateUtil.displayDateInTimezone(QuotaManagerImpl.getUsageAggregationTimeZone(), alertDate), lockable);

        boolean shouldSendEmail = alertDate == null || (balanceDate.after(alertDate) && getDifferenceDays(alertDate, new Date()) > 1);

        if (accountBalance.compareTo(BigDecimal.ZERO) < 0) {
            if (_lockAccountEnforcement && lockable == 1 && _quotaManager.isLockable(account)) {
                logger.info("Locking {}, as quota balance is lower than 0.", account);
                lockAccount(account.getId());
            }

            boolean quotaEmptyEmailEnabled = isQuotaEmailTypeEnabledForAccount(account, QuotaEmailTemplateTypes.QUOTA_EMPTY);
            if (quotaEmptyEmailEnabled && shouldSendEmail) {
                logger.debug("Adding {} to the deferred emails list, as quota balance is lower than 0.", account);
                deferredQuotaEmailList.add(new DeferredQuotaEmail(account, quotaAccount, QuotaEmailTemplateTypes.QUOTA_EMPTY));
                return;
            }
        } else if (accountBalance.compareTo(thresholdBalance) < 0) {
            boolean quotaLowEmailEnabled = isQuotaEmailTypeEnabledForAccount(account, QuotaEmailTemplateTypes.QUOTA_LOW);
            if (quotaLowEmailEnabled && shouldSendEmail) {
                logger.debug("Adding {} to the deferred emails list, as quota balance [{}] is below the threshold [{}].", account, accountBalance, thresholdBalance);
                deferredQuotaEmailList.add(new DeferredQuotaEmail(account, quotaAccount, QuotaEmailTemplateTypes.QUOTA_LOW));
                return;
            }
        }
        logger.debug("{} will not receive any quota alert emails in this round.", account);
    }

    @Override
    public void sendQuotaAlert(DeferredQuotaEmail emailToBeSent) {
        final AccountVO account = emailToBeSent.getAccount();
        final BigDecimal balance = emailToBeSent.getQuotaBalance();
        final BigDecimal usage = emailToBeSent.getQuotaUsage();
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

            String currencySymbol = ObjectUtils.defaultIfNull(_configDao.getValue(QuotaConfig.QuotaCurrencySymbol.key()), QuotaConfig.QuotaCurrencySymbol.defaultValue());
            NumberFormat localeFormat = getLocaleFormatIfCurrencyLocaleNotNull();

            String balanceStr = String.format("%s %s", currencySymbol, NumbersUtil.formatBigDecimalAccordingToNumberFormat(balance, localeFormat));
            String usageStr = String.format("%s %s", currencySymbol, NumbersUtil.formatBigDecimalAccordingToNumberFormat(usage, localeFormat));

            final Map<String, String> subjectOptionMap = generateOptionMap(account, userNames, accountDomain, balanceStr, usageStr, emailType, false);
            final Map<String, String> bodyOptionMap = generateOptionMap(account, userNames, accountDomain, balanceStr, usageStr, emailType, true);

            if (logger.isDebugEnabled()) {
                logger.debug(String.format("Sending quota alert with values: accountName [%s], accountID [%s], accountUsers [%s], domainName [%s], domainID [%s].",
                        account.getAccountName(), account.getUuid(), userNames, accountDomain.getName(), accountDomain.getUuid()));
            }

            final StrSubstitutor subjectSubstitutor = new StrSubstitutor(subjectOptionMap);
            final String subject = subjectSubstitutor.replace(emailTemplate.getTemplateSubject());

            final StrSubstitutor bodySubstitutor = new StrSubstitutor(bodyOptionMap);
            final String body = bodySubstitutor.replace(emailTemplate.getTemplateBody());

            try {
                sendQuotaAlert(account, emailRecipients, subject, body);
                emailToBeSent.sentSuccessfully(_quotaAcc);
            } catch (Exception e) {
                logger.error(String.format("Unable to send quota alert email (subject=%s; body=%s) to account %s (%s) recipients (%s) due to error (%s)", subject, body, account.getAccountName(),
                        account.getUuid(), emailRecipients, e));
                if (logger.isDebugEnabled()) {
                    logger.debug("Exception", e);
                }
            }
        } else {
            logger.error(String.format("No quota email template found for type %s, cannot send quota alert email to account %s(%s)", emailType, account.getAccountName(), account.getUuid()));
        }
    }

    private NumberFormat getLocaleFormatIfCurrencyLocaleNotNull() {
        String currencyLocale = _configDao.getValue(QuotaConfig.QuotaCurrencyLocale.key());
        NumberFormat localeFormat = null;
        if (currencyLocale != null) {
            Locale locale = Locale.forLanguageTag(currencyLocale);
            localeFormat = NumberFormat.getNumberInstance(locale);
        }
        return localeFormat;
    }

    /*
    *
    *
     */
    public Map<String, String> generateOptionMap(AccountVO accountVO, String userNames, DomainVO domainVO, final String balance, final String usage,
                                                 final QuotaConfig.QuotaEmailTemplateTypes emailType, boolean escapeHtml) {
        final Map<String, String> optionMap = new HashMap<>();
        optionMap.put("accountID", accountVO.getUuid());
        optionMap.put("domainID", domainVO.getUuid());
        optionMap.put("quotaBalance", balance);

        if (emailType == QuotaEmailTemplateTypes.QUOTA_STATEMENT) {
            optionMap.put("quotaUsage",  usage);
        }

        if (escapeHtml) {
            optionMap.put(ACCOUNT_NAME, StringEscapeUtils.escapeHtml(accountVO.getAccountName()));
            optionMap.put(ACCOUNT_USERS, StringEscapeUtils.escapeHtml(userNames));
            optionMap.put(DOMAIN_NAME, StringEscapeUtils.escapeHtml(domainVO.getName()));
            return optionMap;
        }

        optionMap.put(ACCOUNT_NAME, accountVO.getAccountName());
        optionMap.put(ACCOUNT_USERS, userNames);
        optionMap.put(DOMAIN_NAME, domainVO.getName());
        return optionMap;
    }

    public long getDifferenceDays(Date d1, Date d2) {
        long diff = d2.getTime() - d1.getTime();
        return TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS);
    }

    protected boolean lockAccount(long accountId) {
        final short opendb = TransactionLegacy.currentTxn().getDatabaseId();
        boolean success = false;
        try (TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.CLOUD_DB)) {
            Account account = _accountDao.findById(accountId);
            if (account != null) {
                if (account.getState() == State.LOCKED) {
                    return true; // already locked, no-op
                } else if (account.getState() == State.ENABLED) {
                    AccountVO acctForUpdate = _accountDao.createForUpdate();
                    acctForUpdate.setState(State.LOCKED);
                    success = _accountDao.update(Long.valueOf(accountId), acctForUpdate);
                } else {
                    if (logger.isInfoEnabled()) {
                        logger.info("Attempting to lock a non-enabled account, current state is " + account.getState() + " (accountId: " + accountId + "), locking failed.");
                    }
                }
            } else {
                logger.warn("Failed to lock account " + accountId + ", account not found.");
            }
        } catch (Exception e) {
            logger.error("Exception occurred while locking account by Quota Alert Manager", e);
            throw e;
        } finally {
            TransactionLegacy.open(opendb).close();
        }
        return success;
    }

    public static class DeferredQuotaEmail {
        private AccountVO account;
        private QuotaAccountVO quotaAccount;
        private QuotaConfig.QuotaEmailTemplateTypes emailTemplateType;
        private BigDecimal quotaUsage;

        public DeferredQuotaEmail(AccountVO account, QuotaAccountVO quotaAccount, BigDecimal quotaUsage, QuotaConfig.QuotaEmailTemplateTypes emailTemplateType) {
            this.account = account;
            this.quotaAccount = quotaAccount;
            this.emailTemplateType = emailTemplateType;
            this.quotaUsage = quotaUsage;
        }

        public DeferredQuotaEmail(AccountVO account, QuotaAccountVO quotaAccount, QuotaConfig.QuotaEmailTemplateTypes emailTemplateType) {
            this.account = account;
            this.quotaAccount = quotaAccount;
            this.emailTemplateType = emailTemplateType;
            this.quotaUsage = new BigDecimal(-1);
        }

        public AccountVO getAccount() {
            return account;
        }

        public BigDecimal getQuotaBalance() {
            return quotaAccount.getQuotaBalance();
        }

        public BigDecimal getQuotaUsage() {
            return quotaUsage;
        }

        public Date getSendDate() {
            if (emailTemplateType == QuotaEmailTemplateTypes.QUOTA_STATEMENT) {
                return quotaAccount.getLastStatementDate();
            } else {
                return quotaAccount.getQuotaAlertDate();
            }
        }

        public QuotaConfig.QuotaEmailTemplateTypes getEmailTemplateType() {
            return emailTemplateType;
        }

        public void sentSuccessfully(final QuotaAccountDao quotaAccountDao) {
            if (emailTemplateType == QuotaEmailTemplateTypes.QUOTA_STATEMENT) {
                quotaAccount.setLastStatementDate(new Date());
            } else {
                quotaAccount.setQuotaAlertDate(new Date());
                quotaAccount.setQuotaAlertType(emailTemplateType.ordinal());
            }
            quotaAccountDao.updateQuotaAccount(quotaAccount.getAccountId(), quotaAccount);
        }
    };

    protected void sendQuotaAlert(Account account, List<String> emails, String subject, String body) {
        SMTPMailProperties mailProperties = new SMTPMailProperties();

        mailProperties.setSender(new MailAddress(senderAddress));

        body = addHeaderAndFooter(body, QuotaConfig.QuotaEmailHeader.valueIn(account.getDomainId()), QuotaConfig.QuotaEmailFooter.valueIn(account.getDomainId()));

        mailProperties.setSubject(subject);
        mailProperties.setContent(body);
        mailProperties.setContentType("text/html; charset=utf-8");

        if (CollectionUtils.isEmpty(emails)) {
            logger.warn(String.format("Account [%s] does not have users with email registered, "
                    + "therefore we are unable to send quota alert email with subject [%s] and content [%s].", account.getUuid(), subject, body));
            return;
        }

        Set<MailAddress> addresses = new HashSet<>();
        for (String email : emails) {
            addresses.add(new MailAddress(email));
        }

        mailProperties.setRecipients(addresses);

        mailSender.sendMail(mailProperties);
    }

    protected String addHeaderAndFooter(String body, String header, String footer) {

        if (StringUtils.isNotEmpty(header)) {
            body = String.format("%s%s", header, body);
        }
        if (StringUtils.isNotEmpty(footer)) {
            body = String.format("%s%s", body, footer);
        }

        return body;
    }

}
