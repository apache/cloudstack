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
package com.cloud.resource;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.apache.cloudstack.framework.config.ConfigKey;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloud.agent.AgentManager;
import com.cloud.host.HostVO;
import com.cloud.server.ManagementServer;
import com.cloud.storage.secondary.SecondaryStorageVmManager;

public class DiscovererBaseTest {

    private DiscovererBase discoverer;
    private HostVO host;

    @Before
    public void setUp() {
        discoverer = Mockito.mock(DiscovererBase.class, Mockito.CALLS_REAL_METHODS);
        host = Mockito.mock(HostVO.class);
        Mockito.when(host.getDetails()).thenReturn(new HashMap<>());
        Mockito.when(host.getGuid()).thenReturn("host-guid");
        Mockito.when(host.getDataCenterId()).thenReturn(1L);
        Mockito.when(host.getPodId()).thenReturn(null);
        Mockito.when(host.getClusterId()).thenReturn(null);
        Mockito.when(host.getPrivateIpAddress()).thenReturn("10.0.0.5");
    }

    private void overrideDefaultConfigValue(ConfigKey configKey, String value) throws NoSuchFieldException, IllegalAccessException {
        Field f = ConfigKey.class.getDeclaredField("_defaultValue");
        f.setAccessible(true);
        f.set(configKey, value);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void buildConfigParamsPassesThroughTheConfiguredConfigKeyValues() throws Exception {
        overrideDefaultConfigValue(AgentManager.MigrateWait, "1000");
        overrideDefaultConfigValue(AgentManager.XenServerMaxNics, "8");
        overrideDefaultConfigValue(ManagementServer.XenServerHeartBeatInterval, "45");
        overrideDefaultConfigValue(ManagementServer.XenServerHeartBeatTimeout, "90");
        overrideDefaultConfigValue(ManagementServer.RouterAggregationCommandEachTimeout, "300");
        overrideDefaultConfigValue(SecondaryStorageVmManager.MaxTemplateAndIsoSize, "4096");

        Map<String, Object> params = (Map<String, Object>) ReflectionTestUtils.invokeMethod(discoverer, "buildConfigParams", host);

        assertEquals("1000", params.get("migratewait"));
        assertEquals("8", params.get(AgentManager.XenServerMaxNics.toString().toLowerCase()));
        assertEquals("45", params.get(ManagementServer.XenServerHeartBeatInterval.key().toLowerCase()));
        assertEquals("90", params.get(ManagementServer.XenServerHeartBeatTimeout.key().toLowerCase()));
        assertEquals("300", params.get("router.aggregation.command.each.timeout"));
        assertEquals("4096", params.get("max.template.iso.size"));
    }
}
