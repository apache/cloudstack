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

package org.apache.cloudstack.hypervisor.external.discoverer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.cloudstack.extension.ExtensionResourceMap;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.framework.extensions.dao.ExtensionDao;
import org.apache.cloudstack.framework.extensions.dao.ExtensionResourceMapDao;
import org.apache.cloudstack.framework.extensions.manager.ExtensionsManager;
import org.apache.cloudstack.framework.extensions.vo.ExtensionResourceMapVO;
import org.apache.cloudstack.framework.extensions.vo.ExtensionVO;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.agent.AgentManager;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.exception.DiscoveryException;
import com.cloud.host.HostVO;
import com.cloud.hypervisor.Hypervisor;

@RunWith(MockitoJUnitRunner.class)
public class ExternalServerDiscovererTest {

    @Mock
    private AgentManager agentManager;
    @Mock
    private ExtensionDao extensionDao;
    @Mock
    private ExtensionResourceMapDao extensionResourceMapDao;
    @Mock
    private ExtensionsManager extensionsManager;
    @Mock
    ClusterDao _clusterDao;
    @Mock
    ConfigurationDao _configDao;
    @Mock
    private ClusterVO clusterVO;
    @Mock
    private ExtensionResourceMapVO extensionResourceMapVO;
    @Mock
    private ExtensionVO extensionVO;
    @Mock
    private HostVO hostVO;

    @InjectMocks
    private ExternalServerDiscoverer discoverer;

    @Before
    public void setUp() {
    }

    @Test
    public void testGetResourceGuidFromName() {
        String name = "test-resource";
        String guid = discoverer.getResourceGuidFromName(name);
        assertTrue(guid.startsWith("External:"));
        assertTrue(guid.length() > "External:".length());
    }

    @Test
    public void testAddExtensionDataToResourceParams() {
        Map<String, Object> params = new HashMap<>();
        when(extensionVO.getName()).thenReturn("ext");
        when(extensionVO.getRelativePath()).thenReturn("entry.sh");
        when(extensionVO.getState()).thenReturn(ExtensionVO.State.Enabled);

        discoverer.addExtensionDataToResourceParams(extensionVO, params);

        assertEquals("ext", params.get("extensionName"));
        assertEquals("entry.sh", params.get("extensionRelativePath"));
        assertEquals(ExtensionVO.State.Enabled, params.get("extensionState"));
    }

    @Test(expected = DiscoveryException.class)
    public void testFindThrowsWhenClusterIdNull() throws Exception {
        discoverer.find(1L, 2L, null, new URI("http://host"), "user", "pass", Collections.emptyList());
    }

    @Test(expected = DiscoveryException.class)
    public void testFindThrowsWhenClusterNotExternal() throws Exception {
        when(clusterVO.getHypervisorType()).thenReturn(Hypervisor.HypervisorType.KVM);
        when(_clusterDao.findById(1L)).thenReturn(clusterVO);
        discoverer.find(1L, 2L, 1L, new URI("http://host"), "user", "pass", Collections.emptyList());
    }

    @Test(expected = DiscoveryException.class)
    public void testFindThrowsWhenPodIdNull() throws Exception {
        when(clusterVO.getHypervisorType()).thenReturn(Hypervisor.HypervisorType.External);
        when(_clusterDao.findById(1L)).thenReturn(clusterVO);
        discoverer.find(1L, null, 1L, new URI("http://host"), "user", "pass", Collections.emptyList());
    }

    @Test(expected = DiscoveryException.class)
    public void testFindThrowsWhenNoExtensionResourceMap() throws Exception {
        when(clusterVO.getHypervisorType()).thenReturn(Hypervisor.HypervisorType.External);
        when(_clusterDao.findById(1L)).thenReturn(clusterVO);
        when(extensionResourceMapDao.findByResourceIdAndType(1L, ExtensionResourceMap.ResourceType.Cluster)).thenReturn(null);
        discoverer.find(1L, 2L, 1L, new URI("http://host"), "user", "pass", Collections.emptyList());
    }

    @Test(expected = DiscoveryException.class)
    public void testFindThrowsWhenNoExtensionVO() throws Exception {
        when(clusterVO.getHypervisorType()).thenReturn(Hypervisor.HypervisorType.External);
        when(_clusterDao.findById(1L)).thenReturn(clusterVO);
        when(extensionResourceMapDao.findByResourceIdAndType(1L, ExtensionResourceMap.ResourceType.Cluster)).thenReturn(extensionResourceMapVO);
        when(extensionResourceMapVO.getExtensionId()).thenReturn(10L);
        when(extensionDao.findById(10L)).thenReturn(null);
        discoverer.find(1L, 2L, 1L, new URI("http://host"), "user", "pass", Collections.emptyList());
    }

    @Test
    public void testBuildConfigParamsAddsExtensionData() {
        when(hostVO.getClusterId()).thenReturn(1L);
        HashMap<String, Object> params = new HashMap<>();
        params.put("cluster", "1");
        discoverer.extensionResourceMapDao = extensionResourceMapDao;
        discoverer.extensionDao = extensionDao;
        when(extensionResourceMapDao.findByResourceIdAndType(1L, ExtensionResourceMap.ResourceType.Cluster)).thenReturn(extensionResourceMapVO);
        when(extensionResourceMapVO.getExtensionId()).thenReturn(10L);
        when(extensionDao.findById(10L)).thenReturn(extensionVO);
        when(extensionVO.getName()).thenReturn("ext");
        when(extensionVO.getRelativePath()).thenReturn("entry.sh");
        when(extensionVO.getState()).thenReturn(ExtensionVO.State.Enabled);
        when(_clusterDao.findById(anyLong())).thenReturn(clusterVO);
        when(clusterVO.getGuid()).thenReturn(UUID.randomUUID().toString());

        HashMap<String, Object> result = discoverer.buildConfigParams(hostVO);
        assertEquals("ext", result.get("extensionName"));
        assertEquals("entry.sh", result.get("extensionRelativePath"));
        assertEquals(ExtensionVO.State.Enabled, result.get("extensionState"));
    }

    @Test
    public void testMatchHypervisor() {
        assertTrue(discoverer.matchHypervisor(null));
        assertTrue(discoverer.matchHypervisor("External"));
        assertFalse(discoverer.matchHypervisor("KVM"));
    }

    @Test
    public void testGetHypervisorType() {
        assertEquals(Hypervisor.HypervisorType.External, discoverer.getHypervisorType());
    }

    @Test
    public void testIsRecurringAndTimeout() {
        assertFalse(discoverer.isRecurring());
        assertEquals(0, discoverer.getTimeout());
    }
}
