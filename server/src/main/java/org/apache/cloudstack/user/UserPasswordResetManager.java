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

import com.cloud.user.UserAccount;
import org.apache.cloudstack.framework.config.ConfigKey;

public interface UserPasswordResetManager {
    ConfigKey<Boolean> UserPasswordResetEnabled = new ConfigKey<>(ConfigKey.CATEGORY_ADVANCED,
            Boolean.class,
            "user.password.reset.enabled", "false",
            "Setting this to true allows the ACS user to request an email to reset their password",
            false,
            ConfigKey.Scope.Global);

    ConfigKey<Long> UserPasswordResetTtl = new ConfigKey<>(ConfigKey.CATEGORY_ADVANCED, Long.class,
            "user.password.reset.ttl", "30",
            "TTL in minutes for the token generated to reset the ACS user's password", true,
            ConfigKey.Scope.Global);

    ConfigKey<String> UserPasswordResetEmailSender = new ConfigKey<>(ConfigKey.CATEGORY_ADVANCED,
            String.class, "user.password.reset.email.sender", null,
            "Sender for emails sent to the user to reset ACS user's password ", true,
            ConfigKey.Scope.Global);

    ConfigKey<String> UserPasswordResetSMTPHost = new ConfigKey<>(ConfigKey.CATEGORY_ADVANCED,
            String.class, "user.password.reset.smtp.host", null,
            "Host for SMTP server for sending emails for resetting password for ACS users",
            false,
            ConfigKey.Scope.Global);

    ConfigKey<Integer> UserPasswordResetSMTPPort = new ConfigKey<>(ConfigKey.CATEGORY_ADVANCED,
            Integer.class, "user.password.reset.smtp.port", "25",
            "Port for SMTP server for sending emails for resetting password for ACS users",
            false,
            ConfigKey.Scope.Global);

    ConfigKey<Boolean> UserPasswordResetSMTPUseAuth = new ConfigKey<>(ConfigKey.CATEGORY_ADVANCED,
            Boolean.class, "user.password.reset.smtp.useAuth", "false",
            "Use auth in the SMTP server for sending emails for resetting password for ACS users",
            false, ConfigKey.Scope.Global);

    ConfigKey<Boolean> UserPasswordResetSMTPUseStartTLS = new ConfigKey<>(ConfigKey.CATEGORY_ADVANCED,
            Boolean.class, "user.password.reset.smtp.useStartTLS", "false",
            "If set to true and if we enable security via user.password.reset.smtp.useAuth, this will enable StartTLS to secure the connection.",
            true,
            ConfigKey.Scope.Global);

    ConfigKey<String> UserPasswordResetSMTPEnabledSecurityProtocols = new ConfigKey<String>(ConfigKey.CATEGORY_ADVANCED,
            String.class, "user.password.reset.smtp.enabledSecurityProtocols", "",
            "White-space separated security protocols; ex: \"TLSv1 TLSv1.1\". Supported protocols: SSLv2Hello, SSLv3, TLSv1, TLSv1.1 and TLSv1.2",
            true, ConfigKey.Kind.WhitespaceSeparatedListWithOptions, "SSLv2Hello,SSLv3,TLSv1,TLSv1.1,TLSv1.2");

    ConfigKey<String> UserPasswordResetSMTPUsername = new ConfigKey<>(ConfigKey.CATEGORY_ADVANCED,
            String.class, "user.password.reset.smtp.username", null,
            "Username for SMTP server for sending emails for resetting password for ACS users",
            false, ConfigKey.Scope.Global);

    ConfigKey<String> UserPasswordResetSMTPPassword = new ConfigKey<>("Secure", String.class,
            "user.password.reset.smtp.password", null,
            "Password for SMTP server for sending emails for resetting password for ACS users",
            false, ConfigKey.Scope.Global);

    ConfigKey<String> UserPasswordResetDomainURL = new ConfigKey<>(ConfigKey.CATEGORY_ADVANCED,
            String.class, "user.password.reset.mail.domain.url", null,
            "Domain URL for reset password links sent to the user via email", true,
            ConfigKey.Scope.Global);

    void setResetTokenAndSend(UserAccount userAccount);

    boolean validateAndResetPassword(UserAccount user, String token, String password);
}
