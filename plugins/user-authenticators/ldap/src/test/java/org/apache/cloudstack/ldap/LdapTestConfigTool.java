/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.  The
 * ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
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
