package org.apache.cloudstack.ldap;

import org.apache.cloudstack.framework.config.ConfigKey;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class LdapTestConfigTool {
    public LdapTestConfigTool() {
    }

    void overrideConfigValue(LdapConfiguration ldapConfiguration, final String configKeyName, final Object o) throws IllegalAccessException, NoSuchFieldException {
        Field configKey = LdapConfiguration.class.getDeclaredField(configKeyName);
        configKey.setAccessible(true);

        ConfigKey key = (ConfigKey)configKey.get(ldapConfiguration);

        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(configKey, configKey.getModifiers() & ~Modifier.FINAL);

        Field f = ConfigKey.class.getDeclaredField("_value");
        f.setAccessible(true);
        modifiersField.setInt(f, f.getModifiers() & ~Modifier.FINAL);
        f.set(key, o);

        Field dynamic = ConfigKey.class.getDeclaredField("_isDynamic");
        dynamic.setAccessible(true);
        modifiersField.setInt(dynamic, dynamic.getModifiers() & ~Modifier.FINAL);
        dynamic.setBoolean(key, false);
    }
}