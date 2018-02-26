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
package org.apache.cloudstack.agent.mslb;

import com.cloud.agent.AgentManager;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.resource.ResourceManager;
import com.cloud.resource.ResourceState;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.config.ApiServiceConfiguration;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.messagebus.MessageBus;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.when;

import com.cloud.utils.exception.CloudRuntimeException;

public class AgentMSLBServiceImplTest {

    @Mock
    ResourceManager resourceManager;
    @Mock
    MessageBus messageBus;
    @Mock
    AgentManager agentManager;

    @Mock
    HostVO host1;
    @Mock
    HostVO host2;
    @Mock
    HostVO host3;
    @Mock
    HostVO host4;

    @Spy
    @InjectMocks
    private AgentMSLBServiceImpl agentMgmtLB = new AgentMSLBServiceImpl();

    private final String msCSVList = "192.168.10.10, 192.168.10.11, 192.168.10.12";
    private final List<String> msList = Arrays.asList(msCSVList.replace(" ","").split(","));

    private static final long DC_1_ID = 1L;
    private static final long DC_2_ID = 2L;

    private void overrideDefaultConfigValue(final ConfigKey configKey, final String name, final Object o) throws IllegalAccessException, NoSuchFieldException {
        final Field f = ConfigKey.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(configKey, o);
    }

    private void configureMocks() {
        long id = 1;
        for (HostVO h : Arrays.asList(host1, host2, host3, host4)) {
            when(h.getId()).thenReturn(id);
            when(h.getDataCenterId()).thenReturn(DC_1_ID);
            when(h.getHypervisorType()).thenReturn(Hypervisor.HypervisorType.KVM);
            when(h.getRemoved()).thenReturn(null);
            when(h.getResourceState()).thenReturn(ResourceState.Enabled);
            id++;
        }
        when(resourceManager.listAllHostsInOneZoneByType(Host.Type.Routing, DC_1_ID))
                .thenReturn(Arrays.asList(host4, host2, host1, host3));
        when(resourceManager.listAllHostsInAllZonesByType(Host.Type.Routing)).thenReturn(Arrays.asList(host4, host3, host1, host2));
    }

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        configureMocks();
        agentMgmtLB.configure("someName", null);
        overrideDefaultConfigValue(ApiServiceConfiguration.ManagementServerAddresses, "_defaultValue", msCSVList);
    }

    @Test
    public void testStaticLBSetting() throws NoSuchFieldException, IllegalAccessException {
        overrideDefaultConfigValue(AgentMSLBServiceImpl.ConnectedAgentLBAlgorithm, "_defaultValue", "static");
        //Assert that each host receives the same list when algorithm is static, therefore, the same primary host
        for (HostVO hostVO : Arrays.asList(host1, host2, host3, host4)) {
            List<String> listToSend = agentMgmtLB.getManagementServerList(hostVO.getId(), hostVO.getDataCenterId());
            Assert.assertEquals(msList, listToSend);
            Assert.assertEquals(msList.get(0), listToSend.get(0));
        }
    }

    @Test
    public void testStaticLBSettingNullHostId() throws NoSuchFieldException, IllegalAccessException {
        overrideDefaultConfigValue(AgentMSLBServiceImpl.ConnectedAgentLBAlgorithm, "_defaultValue", "static");
        //Assert new agents will be setup with the same list as existing hosts
        List<String> listToSend = agentMgmtLB.getManagementServerList(host2.getId(), host2.getDataCenterId());
        Assert.assertEquals(listToSend, agentMgmtLB.getManagementServerList(null, DC_1_ID));
    }

    private void testRoundRobinForExistingHosts(List<String> list) {
        for (HostVO hostVO : Arrays.asList(host1, host2, host3, host4)) {
            List<String> listToSend = agentMgmtLB.getManagementServerList(hostVO.getId(), hostVO.getDataCenterId());
            Assert.assertEquals(list, listToSend);
            Assert.assertEquals(list.get(0), listToSend.get(0));
            list.add(list.get(0));
            list.remove(0);
        }
    }
    @Test
    public void testRoundRobinLBSettingConnectedAgents() throws NoSuchFieldException, IllegalAccessException {
        overrideDefaultConfigValue(AgentMSLBServiceImpl.ConnectedAgentLBAlgorithm, "_defaultValue", "roundrobin");
        List<String> list = new ArrayList<>(msList);
        testRoundRobinForExistingHosts(list);
    }

    @Test
    public void testRoundRobinDeterministicOrder() throws NoSuchFieldException, IllegalAccessException {
        overrideDefaultConfigValue(AgentMSLBServiceImpl.ConnectedAgentLBAlgorithm, "_defaultValue", "roundrobin");
        //Assert that host order is deterministic for round robin. i.e. a given host receives the same list (order is given by host id)
        List<String> listHost2 = agentMgmtLB.getManagementServerList(host2.getId(), host2.getDataCenterId());
        Assert.assertEquals(listHost2, agentMgmtLB.getManagementServerList(host2.getId(), host2.getDataCenterId()));
    }

    @Test
    public void testRoundRobinLBSettingNullHostId() throws NoSuchFieldException, IllegalAccessException {
        overrideDefaultConfigValue(AgentMSLBServiceImpl.ConnectedAgentLBAlgorithm, "_defaultValue", "roundrobin");
        //Assert new agents will be setup with list assuming new host has the greatest id
        List<String> list = new ArrayList<>(msList);
        testRoundRobinForExistingHosts(list);
        List<String> listToSend = agentMgmtLB.getManagementServerList(null, DC_1_ID);
        Assert.assertEquals(list, listToSend);
        Assert.assertEquals(list.get(0), listToSend.get(0));
        list.add(list.get(0));
        list.remove(0);
    }

    @Test
    public void testShuffleLBSetting() throws NoSuchFieldException, IllegalAccessException {
        overrideDefaultConfigValue(AgentMSLBServiceImpl.ConnectedAgentLBAlgorithm, "_defaultValue", "shuffle");
        List<String> shuffleListHost2 = agentMgmtLB.getManagementServerList(host2.getId(), host2.getDataCenterId());
        Assert.assertEquals(new HashSet<>(msList), new HashSet<>(shuffleListHost2));
    }

    @Test
    public void testShuffleLBSettingNullHostId() throws NoSuchFieldException, IllegalAccessException {
        overrideDefaultConfigValue(AgentMSLBServiceImpl.ConnectedAgentLBAlgorithm, "_defaultValue", "shuffle");
        Assert.assertEquals(new HashSet<>(msList), new HashSet<>(agentMgmtLB.getManagementServerList(null, DC_1_ID)));
    }

    @Test(expected = CloudRuntimeException.class)
    public void testInvalidAlgorithmSetting() throws NoSuchFieldException, IllegalAccessException {
        overrideDefaultConfigValue(AgentMSLBServiceImpl.ConnectedAgentLBAlgorithm, "_defaultValue", "invalid-algo");
        agentMgmtLB.getManagementServerList(host1.getId(), host1.getDataCenterId());
    }

    @Test(expected = CloudRuntimeException.class)
    public void testExceptionOnEmptyHostSetting() throws NoSuchFieldException, IllegalAccessException {
        overrideDefaultConfigValue(ApiServiceConfiguration.ManagementServerAddresses, "_defaultValue", "");
        // This should throw exception
        agentMgmtLB.getManagementServerList(host1.getId(), host1.getDataCenterId());
    }

    @Test
    public void testGetOrderedRunningHostIdsNullList() {
        when(resourceManager.listAllHostsInOneZoneByType(Host.Type.Routing, DC_1_ID)).thenReturn(null);
        Assert.assertNull(agentMgmtLB.getOrderedRunningHostIds(DC_1_ID));
    }

    @Test
    public void testGetOrderedRunningHostIdsEmptyList() {
        when(resourceManager.listAllHostsInOneZoneByType(Host.Type.Routing, DC_1_ID))
                .thenReturn(new ArrayList<HostVO>());
        Assert.assertEquals(new ArrayList<>(), agentMgmtLB.getOrderedRunningHostIds(DC_1_ID));
    }

    @Test
    public void testGetOrderedRunningHostIdsOrderList() {
        when(resourceManager.listAllHostsInOneZoneByType(Host.Type.Routing, DC_1_ID))
                .thenReturn(Arrays.asList(host4, host2, host1, host3));
        Assert.assertEquals(Arrays.asList(host1.getId(), host2.getId(), host3.getId(), host4.getId()),
                agentMgmtLB.getOrderedRunningHostIds(DC_1_ID));
    }

    @Test
    public void testGetHostsPerZoneAllHostsInOneZone() {
        Map<Long, List<Long>> hostsPerZone = agentMgmtLB.getHostsPerZone();
        Assert.assertEquals(1, hostsPerZone.keySet().size());
        Assert.assertTrue(hostsPerZone.containsKey(DC_1_ID));
        Assert.assertEquals(new HashSet<>(Arrays.asList(host1.getId(), host2.getId(), host3.getId(), host4.getId())),
                new HashSet<>(hostsPerZone.get(DC_1_ID)));
    }

    @Test
    public void testGetHostsPerZoneHostsInDifferentZones() {
        when(host2.getDataCenterId()).thenReturn(DC_2_ID);
        when(host4.getDataCenterId()).thenReturn(DC_2_ID);
        Map<Long, List<Long>> hostsPerZone = agentMgmtLB.getHostsPerZone();
        Assert.assertEquals(2, hostsPerZone.keySet().size());
        Assert.assertTrue(hostsPerZone.containsKey(DC_1_ID));
        Assert.assertTrue(hostsPerZone.containsKey(DC_2_ID));
        Assert.assertEquals(new HashSet<>(Arrays.asList(host1.getId(), host3.getId())),
                new HashSet<>(hostsPerZone.get(DC_1_ID)));
        Assert.assertEquals(new HashSet<>(Arrays.asList(host2.getId(), host4.getId())),
                new HashSet<>(hostsPerZone.get(DC_2_ID)));
    }

    @Test
    public void testGetHostsPerZoneNullHosts() {
        when(resourceManager.listAllHostsInAllZonesByType(Host.Type.Routing)).thenReturn(null);
        Assert.assertNull(agentMgmtLB.getHostsPerZone());
    }

    @Test
    public void testGetHostsPerZoneEmptyHosts() {
        when(resourceManager.listAllHostsInAllZonesByType(Host.Type.Routing)).thenReturn(new ArrayList<>());
        Map<Long, List<Long>> map = agentMgmtLB.getHostsPerZone();
        Assert.assertNotNull(map);
        Assert.assertTrue(map.isEmpty());
    }
}