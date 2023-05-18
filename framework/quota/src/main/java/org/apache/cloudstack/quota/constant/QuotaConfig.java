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

package org.apache.cloudstack.quota.constant;

import org.apache.cloudstack.framework.config.ConfigKey;

public interface QuotaConfig {

    public static final ConfigKey<Boolean> QuotaPluginEnabled = new ConfigKey<Boolean>("Advanced", Boolean.class, "quota.enable.service", "false",
            "Indicates whether Quota plugin is enabled or not.", true);

    public static final ConfigKey<String> QuotaEnableEnforcement = new ConfigKey<String>("Advanced", String.class, "quota.enable.enforcement", "false",
            "Enable the usage quota enforcement, i.e. on true when exceeding quota the respective account will be locked.", true);

    public static final ConfigKey<String> QuotaCurrencySymbol = new ConfigKey<String>("Advanced", String.class, "quota.currency.symbol", "$",
            "The symbol for the currency in use to measure usage.", true);

    public static final ConfigKey<Integer> QuotaStatementPeriod = new ConfigKey<Integer>("Advanced", Integer.class, "quota.statement.period", "1",
            "This variables define the statement generation interval. Values correspond to bimonthly=0, monthly=1, quarterly=2, half-yearly=3 and yearly=4.", true);

    public static final ConfigKey<String> QuotaSmtpHost = new ConfigKey<String>("Advanced", String.class, "quota.usage.smtp.host", "", "Quota SMTP host for quota related emails.",
            true);

    public static final ConfigKey<String> QuotaSmtpTimeout = new ConfigKey<String>("Advanced", String.class, "quota.usage.smtp.connection.timeout", "60",
            "Quota SMTP server connection timeout duration.", true);

    public static final ConfigKey<String> QuotaSmtpUser = new ConfigKey<String>("Advanced", String.class, "quota.usage.smtp.user", "", "Quota SMTP server username.", true);

    public static final ConfigKey<String> QuotaSmtpPassword = new ConfigKey<String>("Advanced", String.class, "quota.usage.smtp.password", "", "Quota SMTP server password.", true);

    public static final ConfigKey<String> QuotaSmtpPort = new ConfigKey<String>("Advanced", String.class, "quota.usage.smtp.port", "", "Quota SMTP port.", true);

    public static final ConfigKey<String> QuotaSmtpAuthType = new ConfigKey<String>("Advanced", String.class, "quota.usage.smtp.useAuth", "",
            "If true, use secure SMTP authentication when sending emails.", true);

    public static final ConfigKey<String> QuotaSmtpSender = new ConfigKey<String>("Advanced", String.class, "quota.usage.smtp.sender", "",
            "Sender of quota alert email (will be in the From header of the email).", true);

    public static final ConfigKey<String> QuotaSmtpEnabledSecurityProtocols = new ConfigKey<String>("Advanced", String.class, "quota.usage.smtp.enabledSecurityProtocols", "",
            "White-space separated security protocols; ex: \"TLSv1 TLSv1.1\". Supported protocols: SSLv2Hello, SSLv3, TLSv1, TLSv1.1 and TLSv1.2.", true);

    public static final ConfigKey<String> QuotaSmtpUseStartTLS = new ConfigKey<String>("Advanced", String.class, "quota.usage.smtp.useStartTLS", "false",
            "If set to true and if we enable security via quota.usage.smtp.useAuth, this will enable StartTLS to secure the connection.", true);

    public static final ConfigKey<Long> QuotaActivationRuleTimeout = new ConfigKey<>("Advanced", Long.class, "quota.activationrule.timeout", "2000", "The maximum runtime,"
            + " in milliseconds, to execute the quota tariff's activation rule; if it is reached, a timeout will happen.", true);

    ConfigKey<Boolean> QuotaAccountEnabled = new ConfigKey<>("Advanced", Boolean.class, "quota.account.enabled", "true", "Indicates whether Quota plugin is enabled or not for " +
            "the account.", true, ConfigKey.Scope.Account);

    enum QuotaEmailTemplateTypes {
        QUOTA_LOW, QUOTA_EMPTY, QUOTA_UNLOCK_ACCOUNT, QUOTA_STATEMENT
    }
}
