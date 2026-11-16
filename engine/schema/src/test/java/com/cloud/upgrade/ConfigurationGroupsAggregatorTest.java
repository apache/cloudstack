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
package com.cloud.upgrade;

import static org.mockito.Mockito.when;

import java.util.Collections;

import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.framework.config.dao.ConfigurationGroupDao;
import org.apache.cloudstack.framework.config.dao.ConfigurationSubGroupDao;
import org.apache.cloudstack.framework.config.impl.ConfigurationSubGroupVO;
import org.apache.cloudstack.framework.config.impl.ConfigurationVO;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ConfigurationGroupsAggregatorTest {
    @InjectMocks
    private ConfigurationGroupsAggregator configurationGroupsAggregator = new ConfigurationGroupsAggregator();

    @Mock
    private ConfigurationDao configDao;

    @Mock
    private ConfigurationGroupDao configGroupDao;

    @Mock
    private ConfigurationSubGroupDao configSubGroupDao;

    @Mock
    private Logger logger;

    @Test
    public void testUpdateConfigurationGroups() {
        ConfigurationVO config = new ConfigurationVO("Advanced", "DEFAULT", "management-server",
                "test.config.name", null, "description");
        config.setGroupId(1L);
        config.setSubGroupId(1L);

        when(configDao.searchPartialConfigurations()).thenReturn(Collections.singletonList(config));

        ConfigurationSubGroupVO configSubGroup = Mockito.mock(ConfigurationSubGroupVO.class);
        when(configSubGroupDao.findByName("name")).thenReturn(configSubGroup);
        Mockito.when(configSubGroup.getId()).thenReturn(10L);
        Mockito.when(configSubGroup.getGroupId()).thenReturn(5L);

        configurationGroupsAggregator.updateConfigurationGroups();

        Assert.assertEquals(Long.valueOf(5), config.getGroupId());
        Assert.assertEquals(Long.valueOf(10), config.getSubGroupId());
        Mockito.verify(configDao, Mockito.times(1)).persist(config);
        Mockito.verify(logger, Mockito.times(1)).debug("Updating configuration groups");
        Mockito.verify(logger, Mockito.times(1)).debug("Successfully updated configuration groups.");
    }
}
