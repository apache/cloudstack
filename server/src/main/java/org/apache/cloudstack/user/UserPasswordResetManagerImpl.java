// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package org.apache.cloudstack.user;

import com.cloud.user.AccountManager;
import com.cloud.user.UserAccount;
import com.cloud.user.UserVO;
import com.cloud.user.dao.UserDao;
import com.cloud.utils.StringUtils;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.server.ServerProperties;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.resourcedetail.UserDetailVO;
import org.apache.cloudstack.resourcedetail.dao.UserDetailsDao;
import org.apache.cloudstack.utils.mailing.MailAddress;
import org.apache.cloudstack.utils.mailing.SMTPMailProperties;
import org.apache.cloudstack.utils.mailing.SMTPMailSender;

import javax.inject.Inject;
import javax.naming.ConfigurationException;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.apache.cloudstack.config.ApiServiceConfiguration.ManagementServerAddresses;
import static org.apache.cloudstack.resourcedetail.UserDetailVO.PasswordResetToken;
import static org.apache.cloudstack.resourcedetail.UserDetailVO.PasswordResetTokenExpiryDate;
import static org.apache.cloudstack.resourcedetail.UserDetailVO.PasswordResetTokenIssuedDate;

public class UserPasswordResetManagerImpl extends ManagerBase implements UserPasswordResetManager, Configurable {

    @Inject
    private AccountManager accountManager;

    @Inject
    private UserDetailsDao userDetailsDao;

    @Inject
    private UserDao userDao;

    private SMTPMailSender mailSender;

    public static ConfigKey<String> PasswordResetMailTemplate =
            new ConfigKey<>(ConfigKey.CATEGORY_ADVANCED, String.class,
            "user.password.reset.mail.template", "Hello {{username}}!\n" +
            "You have requested to reset your password. Please click the following link to reset your password:\n" +
            "{{{resetLink}}}\n" +
            "If you did not request a password reset, please ignore this email.\n" +
            "\n" +
            "Regards,\n" +
            "The CloudStack Team",
            "Password reset mail template. This uses mustache template engine. Available " +
                    "variables are: username, firstName, lastName, resetLink, token",
            true,
            ConfigKey.Scope.Global);

    @Override
    public String getConfigComponentName() {
        return UserPasswordResetManagerImpl.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[]{
                UserPasswordResetEnabled,
                UserPasswordResetTtl,
                UserPasswordResetEmailCooldown,
                UserPasswordResetEmailSender,
                UserPasswordResetSMTPHost,
                UserPasswordResetSMTPPort,
                UserPasswordResetSMTPUseAuth,
                UserPasswordResetSMTPUseStartTLS,
                UserPasswordResetSMTPEnabledSecurityProtocols,
                UserPasswordResetSMTPUsername,
                UserPasswordResetSMTPPassword,
                UserPasswordResetDomainURL,
                PasswordResetMailTemplate
        };
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        String smtpHost = UserPasswordResetSMTPHost.value();
        Integer smtpPort = UserPasswordResetSMTPPort.value();
        Boolean useAuth = UserPasswordResetSMTPUseAuth.value();
        String username = UserPasswordResetSMTPUsername.value();
        String password = UserPasswordResetSMTPPassword.value();
        Boolean useStartTLS = UserPasswordResetSMTPUseStartTLS.value();
        String enabledSecurityProtocols = UserPasswordResetSMTPEnabledSecurityProtocols.value();

        if (!StringUtils.isEmpty(smtpHost) && smtpPort != null && smtpPort > 0) {
            String namespace = "password.reset.smtp";

            Map<String, String> configs = new HashMap<>();

            configs.put(getKey(namespace, SMTPMailSender.CONFIG_HOST), smtpHost);
            configs.put(getKey(namespace, SMTPMailSender.CONFIG_PORT), smtpPort.toString());
            configs.put(getKey(namespace, SMTPMailSender.CONFIG_USE_AUTH), useAuth.toString());
            configs.put(getKey(namespace, SMTPMailSender.CONFIG_USERNAME), username);
            configs.put(getKey(namespace, SMTPMailSender.CONFIG_PASSWORD), password);
            configs.put(getKey(namespace, SMTPMailSender.CONFIG_USE_STARTTLS), useStartTLS.toString());
            configs.put(getKey(namespace, SMTPMailSender.CONFIG_ENABLED_SECURITY_PROTOCOLS), enabledSecurityProtocols);

            mailSender = new SMTPMailSender(configs, namespace);
        }
        return true;
    }

    private String getKey(String namespace, String config) {
        return String.format("%s.%s", namespace, config);
    }


    /**
     * Removes every row for the detail before writing it. addDetail only replaces one row, so
     * duplicates left by parallel requests would otherwise persist and break every later link.
     */
    private void replaceDetail(UserAccount userAccount, String name, String value) {
        userDetailsDao.removeDetail(userAccount.getId(), name);
        userDetailsDao.addDetail(userAccount.getId(), name, value, false);
    }

    protected long getEmailCooldownSeconds() {
        return UserPasswordResetEmailCooldown.value();
    }

    protected boolean isWithinEmailCooldown(UserAccount userAccount) {
        long cooldown = getEmailCooldownSeconds();
        if (cooldown <= 0) {
            return false;
        }
        UserDetailVO issuedDate = userDetailsDao.findDetail(userAccount.getId(), PasswordResetTokenIssuedDate);
        if (issuedDate == null || StringUtils.isEmpty(issuedDate.getValue())) {
            return false;
        }
        long issuedAt;
        try {
            issuedAt = Long.parseLong(issuedDate.getValue());
        } catch (NumberFormatException e) {
            logger.warn("Ignoring invalid password reset issue time [{}] for user {}", issuedDate.getValue(), userAccount);
            return false;
        }
        // A negative value means the issue time is in the future, from clock skew between
        // management servers. Ignore it so the user is not blocked.
        long elapsed = System.currentTimeMillis() - issuedAt;
        return elapsed >= 0 && elapsed / 1000 < cooldown;
    }

    public void setResetTokenAndSend(UserAccount userAccount) {
        if (mailSender == null) {
            logger.debug("Failed to reset token and send email. SMTP mail sender is not configured.");
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR,
                    "Failed to reset token and send email. SMTP mail sender is not configured");
        }

        if (isWithinEmailCooldown(userAccount)) {
            logger.info("Password reset email not sent for user {} account id: {} domain id: {}. " +
                            "A reset was requested less than {} seconds ago.",
                    userAccount, userAccount.getAccountId(), userAccount.getDomainId(),
                    getEmailCooldownSeconds());
            return;
        }

        // Claim the cooldown before sending, so a request arriving during the send is caught.
        // Truly parallel requests can still both pass the check; the cooldown is best effort.
        final long now = System.currentTimeMillis();
        replaceDetail(userAccount, PasswordResetTokenIssuedDate, String.valueOf(now));

        // The claim is kept even if the send fails, so a mail outage cannot be used to
        // bypass the cooldown. The previous link still works because no new token is saved.
        issueTokenAndSend(userAccount, now);
    }

    private void issueTokenAndSend(UserAccount userAccount, long now) {
        final String resetToken = UUID.randomUUID().toString();
        final Date resetTokenExpiryTime = new Date(now + UserPasswordResetTtl.value() * 60 * 1000);

        final String email = userAccount.getEmail();
        final String username = userAccount.getUsername();
        final String subject = "Password Reset Request";
        String domainUrl = UserPasswordResetDomainURL.value();
        if (StringUtils.isBlank(domainUrl)) {
            String mgmtServerAddr = ManagementServerAddresses.value().split(",")[0];
            if (ServerProperties.isHttpsEnabled()) {
                domainUrl = "https://" + mgmtServerAddr + ":" + ServerProperties.getHttpsPort();
            } else {
                domainUrl = "http://" + mgmtServerAddr + ":" + ServerProperties.getHttpPort();
            }
        } else if (!domainUrl.startsWith("http://") && !domainUrl.startsWith("https://")) {
            if (ServerProperties.isHttpsEnabled()) {
                domainUrl = "https://" + domainUrl;
            } else {
                domainUrl = "http://" + domainUrl;
            }
        }

        domainUrl = domainUrl.replaceAll("/+$", "");

        String resetLink = String.format("%s/client/#/user/resetPassword?username=%s&token=%s",
                domainUrl, username, resetToken);
        String content = getMessageBody(userAccount, resetToken, resetLink);

        SMTPMailProperties mailProperties = new SMTPMailProperties();

        mailProperties.setSender(new MailAddress(UserPasswordResetEmailSender.value()));
        mailProperties.setSubject(subject);
        mailProperties.setContent(content);
        mailProperties.setContentType("text/html; charset=utf-8");

        Set<MailAddress> addresses = new HashSet<>();

        addresses.add(new MailAddress(email));

        mailProperties.setRecipients(addresses);

        // Save the token only once the email is sent. If the send fails, the previous
        // link keeps working and the user can retry without waiting for the cooldown.
        if (!mailSender.trySendMail(mailProperties)) {
            logger.error("Failed to send password reset email for user {} account id: {} domain id: {}. The existing token was kept.",
                    userAccount, userAccount.getAccountId(), userAccount.getDomainId());
            return;
        }

        // The new token replaces any existing one, so only the latest email's link works.
        replaceDetail(userAccount, PasswordResetToken, resetToken);
        replaceDetail(userAccount, PasswordResetTokenExpiryDate, String.valueOf(resetTokenExpiryTime.getTime()));
        logger.debug("User password reset email for user {} account id: {} domain id: {} sent to {} with token expiry at {}",
                userAccount, userAccount.getAccountId(), userAccount.getDomainId(), email, resetTokenExpiryTime);
    }

    @Override
    public boolean validateAndResetPassword(UserAccount user, String token, String password) {
        UserDetailVO resetTokenDetail = userDetailsDao.findDetail(user.getId(), PasswordResetToken);
        UserDetailVO resetTokenExpiryDate = userDetailsDao.findDetail(user.getId(), PasswordResetTokenExpiryDate);

        if (resetTokenDetail == null || resetTokenExpiryDate == null) {
            logger.debug("Failed to reset password. No reset token found for user {} account" +
                    " id: {} domain id: {}", user, user.getAccountId(), user.getDomainId());
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, String.format("No reset token found for user %s", user.getUsername()));
        }

        Date resetTokenExpiryTime = new Date(Long.parseLong(resetTokenExpiryDate.getValue()));

        Date now = new Date();
        String resetToken = resetTokenDetail.getValue();
        if (StringUtils.isEmpty(resetToken)) {
            logger.debug("Failed to reset password. No reset token found for user {} account" +
                    " id: {} domain id: {}", user, user.getAccountId(), user.getDomainId());
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, String.format("No reset token found for user %s", user.getUsername()));
        }
        if (!resetToken.equals(token)) {
            logger.debug("Failed to reset password. Invalid reset token for user {} " +
                    "account id: {} domain id: {}", user, user.getAccountId(), user.getDomainId());
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, String.format("Invalid reset token for user %s", user.getUsername()));
        }
        if (now.after(resetTokenExpiryTime)) {
            logger.debug("Failed to reset password. Reset token has expired for user {} " +
                    "account id: {} domain id: {}", user, user.getAccountId(), user.getDomainId());
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, String.format("Reset token has expired for user %s", user.getUsername()));
        }

        resetPassword(user, password);
        logger.debug("Password reset successful for user {} account id: {} domain id: {}", user, user.getAccountId(), user.getDomainId());
        return true;
    }


    void resetPassword(UserAccount userAccount, String password) {
        UserVO user = userDao.getUser(userAccount.getId());

        accountManager.validateUserPasswordAndUpdateIfNeeded(password, user, "", true);

        userDetailsDao.removeDetail(userAccount.getId(), PasswordResetToken);
        userDetailsDao.removeDetail(userAccount.getId(), PasswordResetTokenExpiryDate);
        userDetailsDao.removeDetail(userAccount.getId(), PasswordResetTokenIssuedDate);

        userDao.persist(user);
    }

    String getMessageBody(UserAccount userAccount, String token, String resetLink) {
        MustacheFactory mf = new DefaultMustacheFactory();
        Mustache mustache = mf.compile(new StringReader(PasswordResetMailTemplate.value()), "password.reset.mail");
        StringWriter writer = new StringWriter();

        PasswordResetMail values = new PasswordResetMail(userAccount.getUsername(), userAccount.getFirstname(), userAccount.getLastname(), resetLink, token);

        try {
            mustache.execute(writer, values).flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return writer.toString();

    }

    static class PasswordResetMail {
        private String username;
        private String firstName;
        private String lastName;
        private String resetLink;
        private String token;


        public PasswordResetMail(String username, String firstName, String lastName, String resetLink, String token) {
            this.username = username;
            this.firstName = firstName;
            this.lastName = lastName;
            this.resetLink = resetLink;
            this.token = token;
        }

        public String getUsername() {
            return username;
        }

        public String getFirstName() {
            return firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public String getResetLink() {
            return resetLink;
        }

        public String getToken() {
            return token;
        }
    }
}
