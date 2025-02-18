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
import java.util.List;
import java.util.Set;

import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.framework.config.ScopedConfigStorage;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.framework.config.dao.ConfigurationSubGroupDao;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloud.utils.Pair;

@RunWith(MockitoJUnitRunner.class)
public class ConfigDepotImplTest {

    @Mock
    ConfigurationDao _configDao;
    @Mock
    ConfigurationSubGroupDao configSubGroupDao;

    @InjectMocks
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

    private void runTestGetConfigStringValue(String key, String value) {
        ConfigurationVO configurationVO = Mockito.mock(ConfigurationVO.class);
        Mockito.when(configurationVO.getValue()).thenReturn(value);
        Mockito.when(_configDao.findById(key)).thenReturn(configurationVO);
        String result = configDepotImpl.getConfigStringValue(key, ConfigKey.Scope.Global, null);
        Assert.assertEquals(value, result);
    }

    @Test
    public void testGetConfigStringValue() {
        runTestGetConfigStringValue("test", "value");
    }

    private void runTestGetConfigStringValueExpiry(long wait, int configDBRetrieval) {
        String key = "test1";
        String value = "expiry";
        runTestGetConfigStringValue(key, value);
        try {
            Thread.sleep(wait);
        } catch (InterruptedException ie) {
            Assert.fail(ie.getMessage());
        }
        String result = configDepotImpl.getConfigStringValue(key, ConfigKey.Scope.Global, null);
        Assert.assertEquals(value, result);
        Mockito.verify(_configDao, Mockito.times(configDBRetrieval)).findById(key);

    }

    @Test
    public void testGetConfigStringValueWithinExpiry() {
        runTestGetConfigStringValueExpiry((ConfigDepotImpl.CONFIG_CACHE_EXPIRE_SECONDS * 1000 ) / 4,
                1);
    }

    @Test
    public void testGetConfigStringValueAfterExpiry() {
        runTestGetConfigStringValueExpiry(((ConfigDepotImpl.CONFIG_CACHE_EXPIRE_SECONDS) + 5) * 1000,
                2);
    }

    @Test
    public void testPopulateConfigurationNewVO() {
        ConfigKey StorageDisableThreshold = new ConfigKey<>(ConfigKey.CATEGORY_ALERT, Double.class, "pool.storage.capacity.disablethreshold", "0.85",
                "Percentage (as a value between 0 and 1) of storage utilization above which allocators will disable using the pool for low storage available.",
                true, List.of(ConfigKey.Scope.StoragePool, ConfigKey.Scope.Zone));
        Configurable configurable = new Configurable() {
            @Override
            public String getConfigComponentName() {
                return "test";
            }

            @Override
            public ConfigKey<?>[] getConfigKeys() {
                return new ConfigKey<?>[] { StorageDisableThreshold };
            }
        };
        configDepotImpl.setConfigurables(List.of(configurable));
        configDepotImpl.populateConfigurations();

        Assert.assertEquals("pool.storage.capacity.disablethreshold",
                configDepotImpl._scopeLevelConfigsMap.get(ConfigKey.Scope.Zone).iterator().next().key());
        Assert.assertEquals("pool.storage.capacity.disablethreshold",
                configDepotImpl._scopeLevelConfigsMap.get(ConfigKey.Scope.StoragePool).iterator().next().key());
        Assert.assertEquals(0, configDepotImpl._scopeLevelConfigsMap.get(ConfigKey.Scope.Cluster).size());
    }

    @Test
    public void testPopulateConfiguration() {
        ConfigKey StorageDisableThreshold = new ConfigKey<>(ConfigKey.CATEGORY_ALERT, Double.class, "pool.storage.capacity.disablethreshold", "0.85",
                "Percentage (as a value between 0 and 1) of storage utilization above which allocators will disable using the pool for low storage available.",
                true, List.of(ConfigKey.Scope.StoragePool, ConfigKey.Scope.Zone));
        Configurable configurable = new Configurable() {
            @Override
            public String getConfigComponentName() {
                return "test";
            }

            @Override
            public ConfigKey<?>[] getConfigKeys() {
                return new ConfigKey<?>[]{StorageDisableThreshold};
            }
        };
        configDepotImpl.setConfigurables(List.of(configurable));

        ConfigurationVO configurationVO = new ConfigurationVO(StorageDisableThreshold.category(), "DEFAULT", "component",
                StorageDisableThreshold.key(), StorageDisableThreshold.defaultValue(), StorageDisableThreshold.description(),
                StorageDisableThreshold.displayText(), StorageDisableThreshold.parent(), 1L, 10L);
        Mockito.when(_configDao.findById("pool.storage.capacity.disablethreshold")).thenReturn(configurationVO);
        configDepotImpl.populateConfigurations();

        Mockito.verify(_configDao, Mockito.times(1)).persist(configurationVO);
    }

    @Test
    public void getParentScopeWithValidScope() {
        ConfigKey.Scope scope = ConfigKey.Scope.Cluster;
        ScopedConfigStorage scopedConfigStorage = Mockito.mock(ScopedConfigStorage.class);
        Long id = 1L;
        ConfigKey.Scope parentScope = ConfigKey.Scope.Zone;
        Long parentId = 2L;

        Mockito.when(scopedConfigStorage.getScope()).thenReturn(scope);
        Mockito.when(scopedConfigStorage.getParentScope(id)).thenReturn(new Pair<>(parentScope, parentId));

        configDepotImpl.setScopedStorages(Collections.singletonList(scopedConfigStorage));
        Pair<ConfigKey.Scope, Long> result = configDepotImpl.getParentScope(scope, id);

        Assert.assertNotNull(result);
        Assert.assertEquals(parentScope, result.first());
        Assert.assertEquals(parentId, result.second());
    }
}
