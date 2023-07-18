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
package org.apache.cloudstack.framework.config.impl;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;

import junit.framework.TestCase;

import org.apache.cloudstack.framework.config.dao.ConfigurationGroupDao;
import org.apache.cloudstack.framework.config.dao.ConfigurationSubGroupDao;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import org.apache.cloudstack.framework.config.ConfigDepot;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.framework.config.ScopedConfigStorage;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;

import com.cloud.utils.Pair;
import com.cloud.utils.Ternary;
import com.cloud.utils.db.EntityManager;

public class ConfigDepotAdminTest extends TestCase {
    private final static ConfigKey<Integer> DynamicIntCK = new ConfigKey<Integer>(Integer.class, "dynIntKey", "Advance", "10", "Test Key", true);
    private final static ConfigKey<Integer> StaticIntCK = new ConfigKey<Integer>(Integer.class, "statIntKey", "Advance", "10", "Test Key", false);
    private final static ConfigKey<Integer> TestCK = new ConfigKey<>(Integer.class, "testKey", "Advance", "30", "Test Key", false,
            ConfigKey.Scope.Global, null, "Test Display Text", null, new Ternary<>("TestGroup", "Test Group", 3L), new Pair<>("Test SubGroup", 1L));

    @Mock
    Configurable _configurable;

    @Mock
    ConfigDepot _configDepot;

    ConfigDepotImpl _depotAdmin;

    @Mock
    EntityManager _entityMgr;

    @Mock
    ConfigurationDao _configDao;

    @Mock
    ConfigurationGroupDao _configGroupDao;

    @Mock
    ConfigurationSubGroupDao _configSubGroupDao;

    @Mock
    ScopedConfigStorage _scopedStorage;

    /**
     * @throws java.lang.Exception
     */
    @Override
    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        _depotAdmin = new ConfigDepotImpl();
        _depotAdmin._configDao = _configDao;
        _depotAdmin._configGroupDao = _configGroupDao;
        _depotAdmin._configSubGroupDao = _configSubGroupDao;
        _depotAdmin._configurables = new ArrayList<Configurable>();
        _depotAdmin._configurables.add(_configurable);
        _depotAdmin._scopedStorages = new ArrayList<ScopedConfigStorage>();
        _depotAdmin._scopedStorages.add(_scopedStorage);
    }

    @Test
    public void testAutoPopulation() {
        ConfigurationVO dynamicIntCV = new ConfigurationVO("UnitTestComponent", DynamicIntCK);
        dynamicIntCV.setValue("100");
        ConfigurationVO staticIntCV = new ConfigurationVO("UnitTestComponent", StaticIntCK);
        dynamicIntCV.setValue("200");
        ConfigurationVO testCV = new ConfigurationVO("UnitTestComponent", TestCK);
        ConfigurationGroupVO groupVO = new ConfigurationGroupVO();
        ConfigurationSubGroupVO subGroupVO = new ConfigurationSubGroupVO();

        when(_configurable.getConfigComponentName()).thenReturn("UnitTestComponent");
        when(_configurable.getConfigKeys()).thenReturn(new ConfigKey<?>[] {DynamicIntCK, StaticIntCK, TestCK});
        when(_configDao.findById(StaticIntCK.key())).thenReturn(null);
        when(_configDao.findById(DynamicIntCK.key())).thenReturn(dynamicIntCV);
        when(_configDao.findById(TestCK.key())).thenReturn(testCV);
        when(_configDao.persist(any(ConfigurationVO.class))).thenReturn(dynamicIntCV);
        when(_configGroupDao.persist(any(ConfigurationGroupVO.class))).thenReturn(groupVO);
        when(_configSubGroupDao.persist(any(ConfigurationSubGroupVO.class))).thenReturn(subGroupVO);
        _depotAdmin.populateConfigurations();

        // This is once because DynamicIntCK is returned.
        verify(_configDao, times(1)).persist(any(ConfigurationVO.class));

        when(_configDao.findById(DynamicIntCK.key())).thenReturn(dynamicIntCV);
        when(_configDao.findById(TestCK.key())).thenReturn(null);
        _depotAdmin._configured.clear();
        _depotAdmin.populateConfigurations();
        // This is three because DynamicIntCK, TestCK also returns null.
        verify(_configDao, times(3)).persist(any(ConfigurationVO.class));
    }

    @Test
    public void testDefaultConfigurationGroupAndSubGroup() {
        Mockito.when(_configSubGroupDao.findByName(anyString())).thenReturn(null);
        Mockito.when(_configSubGroupDao.findByKeyword(anyString())).thenReturn(null);

        Pair<Long, Long> configGroupAndSubGroup = _depotAdmin.getConfigurationGroupAndSubGroupByName("test.storage.config.setting");

        Assert.assertEquals(1L, configGroupAndSubGroup.first().longValue());
        Assert.assertEquals(1L, configGroupAndSubGroup.second().longValue());
    }

    @Test
    public void testConfigurationGroupAndSubGroup() {
        ConfigurationGroupVO testGroup = new ConfigurationGroupVO("TestGroup", "Test Group", 3L);
        ConfigurationSubGroupVO testSubGroup = new ConfigurationSubGroupVO("TestSubGroup", null, 1L);
        testSubGroup.setGroupId(9L);
        Mockito.when(_configSubGroupDao.findByName("storage")).thenReturn(testSubGroup);
        Mockito.when(_configSubGroupDao.findByKeyword(anyString())).thenReturn(null);

        Pair<Long, Long> configGroupAndSubGroup = _depotAdmin.getConfigurationGroupAndSubGroupByName("test.storage.config.setting");

        Assert.assertEquals(9L, configGroupAndSubGroup.first().longValue());
        Assert.assertEquals(1L, configGroupAndSubGroup.second().longValue());

        testSubGroup.setGroupId(5L);
        Mockito.when(_configSubGroupDao.findByName(anyString())).thenReturn(null);
        Mockito.when(_configSubGroupDao.findByKeyword("storage")).thenReturn(testSubGroup);

        configGroupAndSubGroup = _depotAdmin.getConfigurationGroupAndSubGroupByName("test.storage.config.setting");

        Assert.assertEquals(5L, configGroupAndSubGroup.first().longValue());
        Assert.assertEquals(1L, configGroupAndSubGroup.second().longValue());
    }
}
