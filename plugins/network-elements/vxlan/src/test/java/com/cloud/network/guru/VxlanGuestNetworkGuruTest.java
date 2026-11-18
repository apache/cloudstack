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
package com.cloud.network.guru;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;

import com.cloud.agent.AgentManager;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.domain.Domain;
import com.cloud.exception.InsufficientVirtualNetworkCapacityException;
import com.cloud.network.Network;
import com.cloud.network.Network.GuestType;
import com.cloud.network.Network.State;
import com.cloud.network.NetworkModel;
import com.cloud.network.NetworkProfile;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.dao.PhysicalNetworkVO;
import com.cloud.offering.NetworkOffering;
import com.cloud.server.ConfigurationServer;
import com.cloud.user.Account;
import com.cloud.vm.ReservationContext;

public class VxlanGuestNetworkGuruTest {
    PhysicalNetworkDao physnetdao = mock(PhysicalNetworkDao.class);
    DataCenterDao dcdao = mock(DataCenterDao.class);
    AgentManager agentmgr = mock(AgentManager.class);
    NetworkOrchestrationService netmgr = mock(NetworkOrchestrationService.class);
    NetworkModel netmodel = mock(NetworkModel.class);
    ConfigurationServer confsvr = mock(ConfigurationServer.class);

    NetworkDao netdao = mock(NetworkDao.class);
    VxlanGuestNetworkGuru guru;

    @Before
    public void setUp() {
        guru = spy(new VxlanGuestNetworkGuru());
        ((GuestNetworkGuru)guru)._physicalNetworkDao = physnetdao;
        guru._physicalNetworkDao = physnetdao;
        guru._dcDao = dcdao;
        guru._networkModel = netmodel;
        guru._networkDao = netdao;
        ((GuestNetworkGuru)guru)._configServer = confsvr;

        DataCenterVO dc = mock(DataCenterVO.class);
        when(dc.getNetworkType()).thenReturn(NetworkType.Advanced);
        when(dc.getGuestNetworkCidr()).thenReturn("10.1.1.1/24");

        when(dcdao.findById(anyLong())).thenReturn(dc);
    }

    @Test
    public void testCanHandle() {
        NetworkOffering offering = mock(NetworkOffering.class);
        when(offering.getId()).thenReturn(42L);
        when(offering.getTrafficType()).thenReturn(TrafficType.Guest);
        when(offering.getGuestType()).thenReturn(GuestType.Isolated);

        PhysicalNetworkVO physnet = mock(PhysicalNetworkVO.class);
        when(physnet.getIsolationMethods()).thenReturn(Arrays.asList(new String[] {"VXLAN"}));
        when(physnet.getId()).thenReturn(42L);

        assertTrue(guru.canHandle(offering, NetworkType.Advanced, physnet) == true);

        // Not supported TrafficType != Guest
        when(offering.getTrafficType()).thenReturn(TrafficType.Management);
        assertFalse(guru.canHandle(offering, NetworkType.Advanced, physnet) == true);

        // Not supported: GuestType Shared
        when(offering.getTrafficType()).thenReturn(TrafficType.Guest);
        when(offering.getGuestType()).thenReturn(GuestType.Shared);
        assertFalse(guru.canHandle(offering, NetworkType.Advanced, physnet) == true);

        // Not supported: Basic networking
        when(offering.getGuestType()).thenReturn(GuestType.Isolated);
        assertFalse(guru.canHandle(offering, NetworkType.Basic, physnet) == true);

        // Not supported: IsolationMethod != VXLAN
        when(physnet.getIsolationMethods()).thenReturn(Arrays.asList(new String[] {"VLAN"}));
        assertFalse(guru.canHandle(offering, NetworkType.Advanced, physnet) == true);

    }

    @Test
    public void testDesign() {
        PhysicalNetworkVO physnet = mock(PhysicalNetworkVO.class);
        when(physnetdao.findById(anyLong())).thenReturn(physnet);
        when(physnet.getIsolationMethods()).thenReturn(Arrays.asList(new String[] {"VXLAN"}));
        when(physnet.getId()).thenReturn(42L);

        NetworkOffering offering = mock(NetworkOffering.class);
        when(offering.getId()).thenReturn(42L);
        when(offering.getTrafficType()).thenReturn(TrafficType.Guest);
        when(offering.getGuestType()).thenReturn(GuestType.Isolated);

        DeploymentPlan plan = mock(DeploymentPlan.class);
        Network network = mock(Network.class);
        Account account = mock(Account.class);

        Network designednetwork = guru.design(offering, plan, network, "", 1L, account);
        assertTrue(designednetwork != null);
        assertTrue(designednetwork.getBroadcastDomainType() == BroadcastDomainType.Vxlan);
    }

    @Test
    public void testImplement() throws InsufficientVirtualNetworkCapacityException {
        PhysicalNetworkVO physnet = mock(PhysicalNetworkVO.class);
        when(physnetdao.findById(anyLong())).thenReturn(physnet);
        when(physnet.getIsolationMethods()).thenReturn(Arrays.asList(new String[] {"VXLAN"}));
        when(physnet.getId()).thenReturn(42L);

        NetworkOffering offering = mock(NetworkOffering.class);
        when(offering.getId()).thenReturn(42L);
        when(offering.getTrafficType()).thenReturn(TrafficType.Guest);
        when(offering.getGuestType()).thenReturn(GuestType.Isolated);

        NetworkVO network = mock(NetworkVO.class);
        when(network.getName()).thenReturn("testnetwork");
        when(network.getState()).thenReturn(State.Implementing);
        when(network.getPhysicalNetworkId()).thenReturn(42L);

        DeployDestination dest = mock(DeployDestination.class);

        DataCenter dc = mock(DataCenter.class);
        when(dest.getDataCenter()).thenReturn(dc);

        when(netmodel.findPhysicalNetworkId(anyLong(), (String)any(), (TrafficType)any())).thenReturn(42L);
        //TODO(VXLAN): doesn't support VNI specified
        //when(confsvr.getConfigValue((String) any(), (String) any(), anyLong())).thenReturn("true");
        when(dcdao.allocateVnet(anyLong(), anyLong(), anyLong(), (String)any(), eq(true))).thenReturn("42");
        doNothing().when(guru).allocateVnetComplete((Network)any(), (NetworkVO)any(), anyLong(), anyLong(), (String)any(), eq("42"));

        Domain dom = mock(Domain.class);
        when(dom.getName()).thenReturn("domain");

        Account acc = mock(Account.class);
        when(acc.getAccountName()).thenReturn("accountname");

        ReservationContext res = mock(ReservationContext.class);
        when(res.getDomain()).thenReturn(dom);
        when(res.getAccount()).thenReturn(acc);

        Network implementednetwork = guru.implement(network, offering, dest, res);
        assertTrue(implementednetwork != null);
    }

    @Test
    public void testImplementWithCidr() throws InsufficientVirtualNetworkCapacityException {
        PhysicalNetworkVO physnet = mock(PhysicalNetworkVO.class);
        when(physnetdao.findById(anyLong())).thenReturn(physnet);
        when(physnet.getIsolationMethods()).thenReturn(Arrays.asList(new String[] {"VXLAN"}));
        when(physnet.getId()).thenReturn(42L);

        NetworkOffering offering = mock(NetworkOffering.class);
        when(offering.getId()).thenReturn(42L);
        when(offering.getTrafficType()).thenReturn(TrafficType.Guest);
        when(offering.getGuestType()).thenReturn(GuestType.Isolated);

        NetworkVO network = mock(NetworkVO.class);
        when(network.getName()).thenReturn("testnetwork");
        when(network.getState()).thenReturn(State.Implementing);
        when(network.getGateway()).thenReturn("10.1.1.1");
        when(network.getCidr()).thenReturn("10.1.1.0/24");
        when(network.getPhysicalNetworkId()).thenReturn(42L);

        DeployDestination dest = mock(DeployDestination.class);

        DataCenter dc = mock(DataCenter.class);
        when(dest.getDataCenter()).thenReturn(dc);

        when(netmodel.findPhysicalNetworkId(anyLong(), (String)any(), (TrafficType)any())).thenReturn(42L);

        //TODO(VXLAN): doesn't support VNI specified
        //when(confsvr.getConfigValue((String) any(), (String) any(), anyLong())).thenReturn("true");
        when(dcdao.allocateVnet(anyLong(), anyLong(), anyLong(), (String)any(), eq(true))).thenReturn("42");
        doNothing().when(guru).allocateVnetComplete((Network)any(), (NetworkVO)any(), anyLong(), anyLong(), (String)any(), eq("42"));

        Domain dom = mock(Domain.class);
        when(dom.getName()).thenReturn("domain");

        Account acc = mock(Account.class);
        when(acc.getAccountName()).thenReturn("accountname");

        ReservationContext res = mock(ReservationContext.class);
        when(res.getDomain()).thenReturn(dom);
        when(res.getAccount()).thenReturn(acc);

        Network implementednetwork = guru.implement(network, offering, dest, res);
        assertTrue(implementednetwork != null);
        assertTrue(implementednetwork.getCidr().equals("10.1.1.0/24"));
        assertTrue(implementednetwork.getGateway().equals("10.1.1.1"));
    }

    @Test
    public void testShutdown() throws InsufficientVirtualNetworkCapacityException, URISyntaxException {
        PhysicalNetworkVO physnet = mock(PhysicalNetworkVO.class);
        when(physnetdao.findById(anyLong())).thenReturn(physnet);
        when(physnet.getIsolationMethods()).thenReturn(Arrays.asList(new String[] {"VXLAN"}));
        when(physnet.getId()).thenReturn(42L);

        NetworkOffering offering = mock(NetworkOffering.class);
        when(offering.getId()).thenReturn(42L);
        when(offering.getTrafficType()).thenReturn(TrafficType.Guest);
        when(offering.getGuestType()).thenReturn(GuestType.Isolated);

        NetworkVO network = mock(NetworkVO.class);
        when(network.getName()).thenReturn("testnetwork");
        when(network.getState()).thenReturn(State.Implementing);
        when(network.getBroadcastDomainType()).thenReturn(BroadcastDomainType.Vxlan);
        when(network.getBroadcastUri()).thenReturn(new URI("vxlan:12345"));
        when(network.getPhysicalNetworkId()).thenReturn(42L);
        when(netdao.findById(42L)).thenReturn(network);

        DeployDestination dest = mock(DeployDestination.class);

        DataCenter dc = mock(DataCenter.class);
        when(dest.getDataCenter()).thenReturn(dc);

        when(netmodel.findPhysicalNetworkId(anyLong(), (String)any(), (TrafficType)any())).thenReturn(42L);

        Domain dom = mock(Domain.class);
        when(dom.getName()).thenReturn("domain");

        Account acc = mock(Account.class);
        when(acc.getAccountName()).thenReturn("accountname");

        ReservationContext res = mock(ReservationContext.class);
        when(res.getDomain()).thenReturn(dom);
        when(res.getAccount()).thenReturn(acc);

        NetworkProfile implementednetwork = mock(NetworkProfile.class);
        when(implementednetwork.getId()).thenReturn(42L);
        when(implementednetwork.getBroadcastUri()).thenReturn(new URI("vxlan:12345"));
        when(offering.isSpecifyVlan()).thenReturn(false);

        guru.shutdown(implementednetwork, offering);
        verify(implementednetwork, times(1)).setBroadcastUri(null);
    }
}
