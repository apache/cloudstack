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

public interface PasswordResetManager {
    ConfigKey<Long> PasswordResetTtl = new ConfigKey<>(ConfigKey.CATEGORY_ADVANCED, Long.class,
            "user.password.reset.ttl", "30",
            "Password reset ttl in minutes", true, ConfigKey.Scope.Global);

    ConfigKey<String> PasswordResetEmailSender = new ConfigKey<>(ConfigKey.CATEGORY_ADVANCED,
            String.class, "user.password.reset.email.sender", null,
            "Password reset email sender", true, ConfigKey.Scope.Global);

    ConfigKey<String> PasswordResetSMTPHost = new ConfigKey<>(ConfigKey.CATEGORY_ADVANCED,
            String.class, "user.password.reset.smtp.host", null,
            "Password reset smtp host", false, ConfigKey.Scope.Global);

    ConfigKey<Integer> PasswordResetSMTPPort = new ConfigKey<>(ConfigKey.CATEGORY_ADVANCED,
            Integer.class, "user.password.reset.smtp.port", "25",
            "Password reset smtp port", false, ConfigKey.Scope.Global);

    ConfigKey<Boolean> PasswordResetSMTPUseAuth = new ConfigKey<>(ConfigKey.CATEGORY_ADVANCED,
            Boolean.class, "user.password.reset.smtp.useAuth", "false",
            "Use auth for smtp in Password reset", false, ConfigKey.Scope.Global);

    ConfigKey<String> PasswordResetSMTPUsername = new ConfigKey<>(ConfigKey.CATEGORY_ADVANCED,
            String.class, "user.password.reset.smtp.username", null,
            "Password reset smtp username", false, ConfigKey.Scope.Global);

    ConfigKey<String> PasswordResetSMTPPassword = new ConfigKey<>("Secure", String.class,
            "user.password.reset.smtp.password", null,
            "Password reset smtp password", false, ConfigKey.Scope.Global);

    void setResetTokenAndSend(UserAccount userAccount);

    boolean validateAndResetPassword(UserAccount user, String token, String password);
}
