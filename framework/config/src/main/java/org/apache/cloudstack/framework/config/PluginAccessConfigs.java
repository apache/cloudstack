package org.apache.cloudstack.framework.config;

public interface PluginAccessConfigs {

    public static final ConfigKey<Boolean> QuotaPluginEnabled = new ConfigKey<Boolean>("Advanced", Boolean.class, "quota.enable.service", "false",
            "Indicates whether Quota plugin is enabled or not.", true);

    ConfigKey<Boolean> QuotaAccountEnabled = new ConfigKey<>("Advanced", Boolean.class, "quota.account.enabled", "true", "Indicates whether Quota plugin is enabled or not for " +
            "the account.", true, ConfigKey.Scope.Account);
}
