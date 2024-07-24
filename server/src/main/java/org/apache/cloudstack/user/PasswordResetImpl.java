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

public class PasswordResetImpl extends ManagerBase implements PasswordReset, Configurable {

    @Inject
    private AccountManager accountManager;

    @Inject
    private UserDetailsDao userDetailsDao;

    @Inject
    private UserDao userDao;

    private SMTPMailSender mailSender;

    public static ConfigKey<String> PasswordResetMailTemplate =
            new ConfigKey<>(ConfigKey.CATEGORY_ADVANCED, String.class,
            "password.reset.mail.template", "Hello {{username}}!\n" +
            "You have requested to reset your password. Please click the following link to reset your password:\n" +
            "{{{reset_link}}}\n" +
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
        return PasswordResetImpl.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[]{PasswordResetTtl,
                PasswordResetEmailSender,
                PasswordResetSMTPHost,
                PasswordResetSMTPPort,
                PasswordResetSMTPUseAuth,
                PasswordResetSMTPUsername,
                PasswordResetSMTPPassword,
                PasswordResetMailTemplate
        };
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        String smtpHost = PasswordResetSMTPHost.value();
        Integer smtpPort = PasswordResetSMTPPort.value();
        Boolean useAuth = PasswordResetSMTPUseAuth.value();
        String username = PasswordResetSMTPUsername.value();
        String password = PasswordResetSMTPPassword.value();

        String namespace = "password.reset.smtp";

        Map<String, String> configs = new HashMap<>();

        configs.put(getKey(namespace, SMTPMailSender.CONFIG_HOST), smtpHost);
        configs.put(getKey(namespace, SMTPMailSender.CONFIG_PORT), smtpPort.toString());
        configs.put(getKey(namespace, SMTPMailSender.CONFIG_USE_AUTH), useAuth.toString());
        configs.put(getKey(namespace, SMTPMailSender.CONFIG_USERNAME), username);
        configs.put(getKey(namespace, SMTPMailSender.CONFIG_PASSWORD), password);

        mailSender = new SMTPMailSender(configs, namespace);
        return true;
    }

    private String getKey(String namespace, String config) {
        return String.format("%s.%s", namespace, config);
    }


    public void setResetTokenAndSend(UserAccount userAccount) {
        final String resetToken = UUID.randomUUID().toString();
        final Date resetTokenExpiryTime = new Date(System.currentTimeMillis() + PasswordResetTtl.value() * 60 * 1000);

        userDetailsDao.addDetail(userAccount.getId(), PasswordResetToken, resetToken, false);
        userDetailsDao.addDetail(userAccount.getId(), PasswordResetTokenExpiryDate, String.valueOf(resetTokenExpiryTime.getTime()), false);

        final String email = userAccount.getEmail();
        final String username = userAccount.getUsername();
        final String subject = "Password Reset Request";

        String resetLink = String.format("%s/user/resetPassword?username=%s&token=%s", ManagementServerAddresses.value(), username, resetToken);
        String content = getMessageBody(userAccount, resetToken, resetLink);

        SMTPMailProperties mailProperties = new SMTPMailProperties();

        mailProperties.setSender(new MailAddress(PasswordResetEmailSender.value()));
        mailProperties.setSubject(subject);
        mailProperties.setContent(content);
        mailProperties.setContentType("text/plain");

        Set<MailAddress> addresses = new HashSet<>();

        addresses.add(new MailAddress(email));

        mailProperties.setRecipients(addresses);

        mailSender.sendMail(mailProperties);
    }

    @Override
    public boolean validateAndResetPassword(UserAccount user, String token, String password) {
        UserDetailVO resetTokenDetail = userDetailsDao.findDetail(user.getId(), PasswordResetToken);
        UserDetailVO resetTokenExpiryDate = userDetailsDao.findDetail(user.getId(), PasswordResetTokenExpiryDate);

        if (resetTokenDetail == null || resetTokenExpiryDate == null) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, String.format("No reset token found for user %s", user.getUsername()));
        }

        Date resetTokenExpiryTime = new Date(Long.parseLong(resetTokenExpiryDate.getValue()));

        Date now = new Date();
        String resetToken = resetTokenDetail.getValue();
        if (StringUtils.isEmpty(resetToken)) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, String.format("No reset token found for user %s", user.getUsername()));
        }
        if (!resetToken.equals(token)) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, String.format("Invalid reset token for user %s", user.getUsername()));
        }
        if (now.after(resetTokenExpiryTime)) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, String.format("Reset token has expired for user %s", user.getUsername()));
        }

        resetPassword(user, password);
        return true;
    }


    void resetPassword(UserAccount userAccount, String password) {
        UserVO user = userDao.getUser(userAccount.getId());

        accountManager.validateUserPasswordAndUpdateIfNeeded(password, user, "", true);

        userDetailsDao.removeDetail(userAccount.getId(), PasswordResetToken);
        userDetailsDao.removeDetail(userAccount.getId(), PasswordResetTokenExpiryDate);

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
