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

public interface QuotaConfig  {

    public static final ConfigKey<Boolean> QuotaPluginEnabled = new ConfigKey<Boolean>("Advanced", Boolean.class, "quota.enable.service", "false",
            "Indicates whether Quota plugin is enabled or not", true);

    public static final ConfigKey<String> QuotaPeriodType = new ConfigKey<String>("Advanced", String.class, "quota.period.type", "2",
            "Quota period type: 1 for every x days, 2 for certain day of the month, 3 for yearly on activation day - default quota usage reporting cycl", true);

    public static final ConfigKey<String> QuotaPeriod = new ConfigKey<String>("Advanced", String.class, "quota.period.config", "15",
            "The period config in number of days for the quota period type", true);

    public static final ConfigKey<String> QuotaGenerateActivity = new ConfigKey<String>("Advanced", String.class, "quota.activity.generate", "true",
            "Set true to enable a detailed log of the quota usage, rating and billing activity, on daily basis. Valid values (true, false)", true);

    public static final ConfigKey<String> QuotaEmailRecordOutgoing = new ConfigKey<String>("Advanced", String.class, "quota.email.outgoing.record", "false",
            "true means all the emails sent out will be stored in local DB, by default it is false", true);

    public static final ConfigKey<String> QuotaEnableEnforcement = new ConfigKey<String>("Advanced", String.class, "quota.enable.enforcement", "true",
            "Enable the usage quota enforcement, i.e. on true when exceeding quota the respective account will be locked.", true);

    public static final ConfigKey<String> QuotaCurrencySymbol = new ConfigKey<String>("Advanced", String.class, "quota.currency.symbol", "R",
            "The symbol for the currency in use to measure usage.", true);

    public static final ConfigKey<String> QuotaLimitCritical = new ConfigKey<String>("Advanced", String.class, "quota.limit.critical", "80",
            "The quota amount when reached, the account users are sent low quota email alerts.", true);

    public static final ConfigKey<String> QuotaLimitIncremental = new ConfigKey<String>("Advanced", String.class, "quota.limit.increment", "5",
            "Quota limit incremental", true);

    public static final ConfigKey<String> QuotaSmtpHost = new ConfigKey<String>("Advanced", String.class, "quota.usage.smtp.host", "",
            "Quota SMTP host for quota related emails", true);

    public static final ConfigKey<String> QuotaSmtpTimeout = new ConfigKey<String>("Advanced", String.class, "quota.usage.smtp.connection.timeout", "60",
            "Quota SMTP server connection timeout duration", true);

    public static final ConfigKey<String> QuotaSmtpUser = new ConfigKey<String>("Advanced", String.class, "quota.usage.smtp.user", "",
            "Quota SMTP server username", true);

    public static final ConfigKey<String> QuotaSmtpPassword = new ConfigKey<String>("Advanced", String.class, "quota.usage.smtp.password", "",
            "Quota SMTP server password", true);

    public static final ConfigKey<String> QuotaSmtpPort = new ConfigKey<String>("Advanced", String.class, "quota.usage.smtp.port", "",
            "Quota SMTP port", true);

    public static final ConfigKey<String> QuotaSmtpAuthType = new ConfigKey<String>("Advanced", String.class, "quota.usage.smtp.useAuth", "",
            "If true, use secure SMTP authentication when sending emails.", true);

    public static final ConfigKey<String> QuotaSmtpSender = new ConfigKey<String>("Advanced", String.class, "quota.usage.smtp.sender", "",
            "Sender of quota alert email (will be in the From header of the email)", true);

    enum QuotaEmailTemplateTypes {
        QUOTA_LOW, QUOTA_EMPTY, QUOTA_UNLOCK_ACCOUNT, QUOTA_STATEMENT
    }
}