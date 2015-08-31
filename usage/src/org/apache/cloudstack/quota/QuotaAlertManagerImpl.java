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

import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.user.Account;
import com.cloud.user.AccountVO;
import com.cloud.user.UserVO;
import com.cloud.user.Account.State;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.UserDao;
import com.cloud.utils.DateUtil;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.utils.exception.CloudRuntimeException;
import com.sun.mail.smtp.SMTPMessage;
import com.sun.mail.smtp.SMTPSSLTransport;
import com.sun.mail.smtp.SMTPTransport;

import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.quota.constant.QuotaConfig;
import org.apache.cloudstack.quota.constant.QuotaConfig.QuotaEmailTemplateTypes;
import org.apache.cloudstack.quota.dao.QuotaAccountDao;
import org.apache.cloudstack.quota.dao.QuotaEmailTemplatesDao;
import org.apache.cloudstack.quota.dao.QuotaUsageDao;
import org.apache.cloudstack.quota.vo.QuotaAccountVO;
import org.apache.cloudstack.quota.vo.QuotaEmailTemplatesVO;
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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

@Component
@Local(value = QuotaAlertManager.class)
public class QuotaAlertManagerImpl extends ManagerBase implements QuotaAlertManager {
    private static final Logger s_logger = Logger.getLogger(QuotaAlertManagerImpl.class.getName());

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
    private QuotaUsageDao _quotaUsage;

    private EmailQuotaAlert _emailQuotaAlert;
    private boolean _lockAccountEnforcement = false;

    final static BigDecimal s_hoursInMonth = new BigDecimal(30 * 24);
    final static BigDecimal s_minutesInMonth = new BigDecimal(30 * 24 * 60);
    final static BigDecimal s_gb = new BigDecimal(1024 * 1024 * 1024);

    boolean _smtpDebug = false;

    int _pid = 0;

    public QuotaAlertManagerImpl() {
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

        final String smtpHost = configs.get(QuotaConfig.QuotaSmtpHost.key());
        int smtpPort = NumbersUtil.parseInt(configs.get(QuotaConfig.QuotaSmtpPort.key()), 25);
        String useAuthStr = configs.get(QuotaConfig.QuotaSmtpAuthType.key());
        boolean useAuth = ((useAuthStr != null) && Boolean.parseBoolean(useAuthStr));
        String smtpUsername = configs.get(QuotaConfig.QuotaSmtpUser.key());
        String smtpPassword = configs.get(QuotaConfig.QuotaSmtpPassword.key());
        String emailSender = configs.get(QuotaConfig.QuotaSmtpSender.key());
        _lockAccountEnforcement = "true".equalsIgnoreCase(configs.get(QuotaConfig.QuotaEnableEnforcement.key()));
        _emailQuotaAlert = new EmailQuotaAlert(smtpHost, smtpPort, useAuth, smtpUsername, smtpPassword, emailSender, _smtpDebug);

        return true;
    }

    @Override
    public boolean start() {
        if (s_logger.isInfoEnabled()) {
            s_logger.info("Starting Alert Manager");
        }
        _pid = Integer.parseInt(System.getProperty("pid"));
        return true;
    }

    @Override
    public boolean stop() {
        if (s_logger.isInfoEnabled()) {
            s_logger.info("Stopping Alert Manager");
        }
        return true;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void sendMonthlyStatement() {
        Date now = new Date();
        Calendar aCalendar = Calendar.getInstance();
        aCalendar.add(Calendar.MONTH, -1);
        aCalendar.set(Calendar.DATE, 1);
        aCalendar.set(Calendar.HOUR, 0);
        aCalendar.set(Calendar.MINUTE, 0);
        aCalendar.set(Calendar.SECOND, 0);
        Date firstDateOfPreviousMonth = aCalendar.getTime();
        aCalendar.set(Calendar.DATE, aCalendar.getActualMaximum(Calendar.DAY_OF_MONTH) + 1);
        Date lastDateOfPreviousMonth = aCalendar.getTime(); // actually the first day of this month

        List<DeferredQuotaEmail> deferredQuotaEmailList = new ArrayList<DeferredQuotaEmail>();
        for (final QuotaAccountVO quotaAccount : _quotaAcc.listAll()) {
            if (quotaAccount.getQuotaBalance() == null) {
                continue; // no quota usage for this account ever, ignore
            }
            Date lastStatementDate = quotaAccount.getLastStatementDate();
            if (now.getDate() < 6) {
                AccountVO account = _accountDao.findById(quotaAccount.getId());
                if (lastStatementDate == null) {
                    BigDecimal quotaUsage = _quotaUsage.findTotalQuotaUsage(account.getAccountId(), account.getDomainId(), null, firstDateOfPreviousMonth, lastDateOfPreviousMonth);
                    s_logger.info("For account=" + quotaAccount.getId() + ", quota used = " + quotaUsage);
                    // send statement
                    deferredQuotaEmailList.add(new DeferredQuotaEmail(account, quotaAccount, quotaUsage, QuotaConfig.QuotaEmailTemplateTypes.QUOTA_STATEMENT));
                } else if (getDifferenceDays(lastStatementDate, new Date()) < 7) {
                    s_logger.debug("For " + quotaAccount.getId() + " the statement has been sent recently");
                } else {
                    BigDecimal quotaUsage = _quotaUsage.findTotalQuotaUsage(account.getAccountId(), account.getDomainId(), null, firstDateOfPreviousMonth, lastDateOfPreviousMonth);
                    s_logger.info("For account=" + quotaAccount.getId() + ", quota used = " + quotaUsage);
                    deferredQuotaEmailList.add(new DeferredQuotaEmail(account, quotaAccount, quotaUsage, QuotaConfig.QuotaEmailTemplateTypes.QUOTA_STATEMENT));
                }
            } else {
                s_logger.info("For " + quotaAccount.getId() + " it is already more than " + getDifferenceDays(lastStatementDate, new Date()) + " days, will send statement in next cycle");
            }

        }

        for (DeferredQuotaEmail emailToBeSent : deferredQuotaEmailList) {
            s_logger.debug("Attempting to send quota STATEMENT email to users of account: " + emailToBeSent.getAccount().getAccountName());
            sendQuotaAlert(emailToBeSent);
        }
    }

    @Override
    public void checkAndSendQuotaAlertEmails() {
        s_logger.info("Running checkAndSendQuotaAlertEmails");
        List<DeferredQuotaEmail> deferredQuotaEmailList = new ArrayList<DeferredQuotaEmail>();
        final BigDecimal zeroBalance = new BigDecimal(0);
        for (final QuotaAccountVO quotaAccount : _quotaAcc.listAll()) {
            s_logger.info("checkAndSendQuotaAlertEmails accId=" + quotaAccount.getId());
            BigDecimal accountBalance = quotaAccount.getQuotaBalance();
            Date balanceDate = quotaAccount.getQuotaBalanceDate();
            Date alertDate = quotaAccount.getQuotaAlertDate();
            int lockable = quotaAccount.getQuotaEnforce();
            BigDecimal thresholdBalance = quotaAccount.getQuotaMinBalance();
            if (accountBalance != null) {
                AccountVO account = _accountDao.findById(quotaAccount.getId());
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Check id " + account.getId() + " bal=" + accountBalance + " alertDate" + alertDate + " diff" + getDifferenceDays(alertDate, new Date()));
                }
                if (accountBalance.compareTo(zeroBalance) <= 0) {
                    if (_lockAccountEnforcement && (lockable == 1)) {
                        if (account.getType() == Account.ACCOUNT_TYPE_NORMAL) {
                            lockAccount(account.getId());
                        }
                    }
                    if (alertDate == null || (balanceDate.after(alertDate) && getDifferenceDays(alertDate, new Date()) > 1)) {
                        deferredQuotaEmailList.add(new DeferredQuotaEmail(account, quotaAccount, QuotaConfig.QuotaEmailTemplateTypes.QUOTA_EMPTY));
                    }
                } else if (accountBalance.compareTo(thresholdBalance) <= 0) {
                    if (alertDate == null || (balanceDate.after(alertDate) && getDifferenceDays(alertDate, new Date()) > 1)) {
                        deferredQuotaEmailList.add(new DeferredQuotaEmail(account, quotaAccount, QuotaConfig.QuotaEmailTemplateTypes.QUOTA_LOW));
                    }
                }
            }
        }

        for (DeferredQuotaEmail emailToBeSent : deferredQuotaEmailList) {
            s_logger.debug("Attempting to send quota alert email to users of account: " + emailToBeSent.getAccount().getAccountName());
            sendQuotaAlert(emailToBeSent);
        }
    }

    private void sendQuotaAlert(DeferredQuotaEmail emailToBeSent) {
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

            final Map<String, String> optionMap = new HashMap<String, String>();
            optionMap.put("accountName", account.getAccountName());
            optionMap.put("accountID", account.getUuid());
            optionMap.put("accountUsers", userNames);
            optionMap.put("domainName", accountDomain.getName());
            optionMap.put("domainID", accountDomain.getUuid());
            optionMap.put("quotaBalance", QuotaConfig.QuotaCurrencySymbol.value() + " " + balance.toString());
            if (emailType == QuotaEmailTemplateTypes.QUOTA_STATEMENT) {
                optionMap.put("quotaUsage", QuotaConfig.QuotaCurrencySymbol.value() + " " + usage.toString());
            }

            if (s_logger.isDebugEnabled()) {
                s_logger.debug("accountName" + account.getAccountName() + "accountID" + account.getUuid() + "accountUsers" + userNames + "domainName" + accountDomain.getName() + "domainID"
                        + accountDomain.getUuid());
            }

            final StrSubstitutor templateEngine = new StrSubstitutor(optionMap);
            final String subject = templateEngine.replace(emailTemplate.getTemplateSubject());
            final String body = templateEngine.replace(emailTemplate.getTemplateBody());
            try {
                _emailQuotaAlert.sendQuotaAlert(emailRecipients, subject, body);
                emailToBeSent.sentSuccessfully();
            } catch (Exception e) {
                s_logger.error(String.format("Unable to send quota alert email (subject=%s; body=%s) to account %s (%s) recipients (%s) due to error (%s)", subject, body, account.getAccountName(),
                        account.getUuid(), emailRecipients, e));
            }
        } else {
            s_logger.error(String.format("No quota email template found for type %s, cannot send quota alert email to account %s(%s)", emailType, account.getAccountName(), account.getUuid()));
        }
    }

    class DeferredQuotaEmail {
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

        public QuotaConfig.QuotaEmailTemplateTypes getEmailTemplateType() {
            return emailTemplateType;
        }

        public void sentSuccessfully() {
            if (emailTemplateType == QuotaEmailTemplateTypes.QUOTA_STATEMENT) {
                quotaAccount.setLastStatementDate(new Date());
                _quotaAcc.update(quotaAccount.getAccountId(), quotaAccount);
            } else {
                quotaAccount.setQuotaAlertDate(new Date());
                quotaAccount.setQuotaAlertType(emailTemplateType.ordinal());
                _quotaAcc.update(quotaAccount.getAccountId(), quotaAccount);
            }
        }
    };

    class EmailQuotaAlert {
        private Session _smtpSession;
        private final String _smtpHost;
        private final int _smtpPort;
        private final boolean _smtpUseAuth;
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
                throw new CloudRuntimeException("Unable to create smtp session.");
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

    public static long getDifferenceDays(Date d1, Date d2) {
        long diff = d2.getTime() - d1.getTime();
        return TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS);
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
