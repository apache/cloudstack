//
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
//
package org.apache.cloudstack.framework.config.impl;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.cloudstack.framework.config.ConfigKey;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

public class ConfigDepotImplTest {

    private ConfigDepotImpl configDepotImpl = new ConfigDepotImpl();

    @Test
    public void createEmptyScopeLevelMappingsTest() {
        configDepotImpl.createEmptyScopeLevelMappings();
        ConfigKey.Scope[] configKeyScopeArray = ConfigKey.Scope.values();

        for (int i = 0; i < configKeyScopeArray.length; i++) {
            if (configKeyScopeArray[i] == ConfigKey.Scope.Global) {
                Assert.assertFalse(configDepotImpl._scopeLevelConfigsMap.containsKey(configKeyScopeArray[i]));
            } else {
                Assert.assertTrue(configDepotImpl._scopeLevelConfigsMap.containsKey(configKeyScopeArray[i]));
            }
        }
    }

    @Test
    public void testIsNewConfig() {
        String validNewConfigKey = "CONFIG";
        ConfigKey<Boolean> validNewConfig = new ConfigKey<>(ConfigKey.CATEGORY_ADVANCED, Boolean.class, "CONFIG", "true", "", true);
        ConfigKey<Boolean> invalidNewConfig = new ConfigKey<>(ConfigKey.CATEGORY_ADVANCED, Boolean.class, "CONFIG1", "true", "", true);
        Set<String> newConfigs = Collections.synchronizedSet(new HashSet<>());
        newConfigs.add(validNewConfigKey);
        ReflectionTestUtils.setField(configDepotImpl, "newConfigs", newConfigs);
        Assert.assertTrue(configDepotImpl.isNewConfig(validNewConfig));
        Assert.assertFalse(configDepotImpl.isNewConfig(invalidNewConfig));
    }

}
