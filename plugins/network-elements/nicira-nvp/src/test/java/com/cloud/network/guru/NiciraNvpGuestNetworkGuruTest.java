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

package com.cloud.network.guru;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;

import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.CreateLogicalSwitchAnswer;
import com.cloud.agent.api.DeleteLogicalSwitchAnswer;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.domain.Domain;
import com.cloud.exception.InsufficientVirtualNetworkCapacityException;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.network.Network;
import com.cloud.network.Network.GuestType;
import com.cloud.network.Network.Service;
import com.cloud.network.Network.State;
import com.cloud.network.NetworkModel;
import com.cloud.network.NetworkProfile;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.NiciraNvpDeviceVO;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.NiciraNvpDao;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.dao.PhysicalNetworkVO;
import com.cloud.offering.NetworkOffering;
import com.cloud.offerings.dao.NetworkOfferingServiceMapDao;
import com.cloud.user.Account;
import com.cloud.vm.ReservationContext;

public class NiciraNvpGuestNetworkGuruTest {
    private static final long NETWORK_ID = 42L;
    PhysicalNetworkDao physnetdao = mock(PhysicalNetworkDao.class);
    NiciraNvpDao nvpdao = mock(NiciraNvpDao.class);
    DataCenterDao dcdao = mock(DataCenterDao.class);
    NetworkOfferingServiceMapDao nosd = mock(NetworkOfferingServiceMapDao.class);
    AgentManager agentmgr = mock(AgentManager.class);
    NetworkOrchestrationService netmgr = mock(NetworkOrchestrationService.class);
    NetworkModel netmodel = mock(NetworkModel.class);

    HostDao hostdao = mock(HostDao.class);
    NetworkDao netdao = mock(NetworkDao.class);
    NiciraNvpGuestNetworkGuru guru;

    @Before
    public void setUp() {
        guru = new NiciraNvpGuestNetworkGuru();
        ((GuestNetworkGuru)guru)._physicalNetworkDao = physnetdao;
        guru.physicalNetworkDao = physnetdao;
        guru.niciraNvpDao = nvpdao;
        guru._dcDao = dcdao;
        guru.ntwkOfferingSrvcDao = nosd;
        ((GuestNetworkGuru)guru)._networkModel = netmodel;
        guru.hostDao = hostdao;
        guru.agentMgr = agentmgr;
        guru.networkDao = netdao;

        final DataCenterVO dc = mock(DataCenterVO.class);
        when(dc.getNetworkType()).thenReturn(NetworkType.Advanced);
        when(dc.getGuestNetworkCidr()).thenReturn("10.1.1.1/24");

        when(dcdao.findById((Long)any())).thenReturn(dc);
    }

    @Test
    public void testCanHandle() {
        final NetworkOffering offering = mock(NetworkOffering.class);
        when(offering.getId()).thenReturn(NETWORK_ID);
        when(offering.getTrafficType()).thenReturn(TrafficType.Guest);
        when(offering.getGuestType()).thenReturn(GuestType.Isolated);

        final PhysicalNetworkVO physnet = mock(PhysicalNetworkVO.class);
        when(physnet.getIsolationMethods()).thenReturn(Arrays.asList(new String[] {"STT", "VXLAN"}));
        when(physnet.getId()).thenReturn(NETWORK_ID);

        when(nosd.areServicesSupportedByNetworkOffering(NETWORK_ID, Service.Connectivity)).thenReturn(true);

        assertTrue(guru.canHandle(offering, NetworkType.Advanced, physnet) == true);

        // Supported: IsolationMethod == VXLAN
        when(physnet.getIsolationMethods()).thenReturn(Arrays.asList(new String[] {"VXLAN"}));
        assertTrue(guru.canHandle(offering, NetworkType.Advanced, physnet) == true);

        // Not supported TrafficType != Guest
        when(offering.getTrafficType()).thenReturn(TrafficType.Management);
        assertFalse(guru.canHandle(offering, NetworkType.Advanced, physnet) == true);

        // Supported: GuestType Shared
        when(offering.getTrafficType()).thenReturn(TrafficType.Guest);
        when(offering.getGuestType()).thenReturn(GuestType.Shared);
        assertTrue(guru.canHandle(offering, NetworkType.Advanced, physnet));

        // Not supported: Basic networking
        when(offering.getGuestType()).thenReturn(GuestType.Isolated);
        assertFalse(guru.canHandle(offering, NetworkType.Basic, physnet) == true);

        // Not supported: IsolationMethod != STT, VXLAN
        when(physnet.getIsolationMethods()).thenReturn(Arrays.asList(new String[] {"VLAN"}));
        assertFalse(guru.canHandle(offering, NetworkType.Advanced, physnet) == true);

    }

    @Test
    public void testDesign() {
        final PhysicalNetworkVO physnet = mock(PhysicalNetworkVO.class);
        when(physnetdao.findById((Long)any())).thenReturn(physnet);
        when(physnet.getIsolationMethods()).thenReturn(Arrays.asList(new String[] {"STT", "VXLAN"}));
        when(physnet.getId()).thenReturn(NETWORK_ID);

        final NiciraNvpDeviceVO device = mock(NiciraNvpDeviceVO.class);
        when(nvpdao.listByPhysicalNetwork(NETWORK_ID)).thenReturn(Arrays.asList(new NiciraNvpDeviceVO[] {device}));
        when(device.getId()).thenReturn(1L);

        final NetworkOffering offering = mock(NetworkOffering.class);
        when(offering.getId()).thenReturn(NETWORK_ID);
        when(offering.getTrafficType()).thenReturn(TrafficType.Guest);
        when(offering.getGuestType()).thenReturn(GuestType.Isolated);

        when(nosd.areServicesSupportedByNetworkOffering(NETWORK_ID, Service.Connectivity)).thenReturn(true);

        when(netmodel.listNetworkOfferingServices(NETWORK_ID)).thenReturn(Arrays.asList(Service.Connectivity));

        final DeploymentPlan plan = mock(DeploymentPlan.class);
        final Network network = mock(Network.class);
        final Account account = mock(Account.class);

        final Network designednetwork = guru.design(offering, plan, network, account);
        assertTrue(designednetwork != null);
        assertTrue(designednetwork.getBroadcastDomainType() == BroadcastDomainType.Lswitch);
    }

    @Test
    public void testDesignNoElementOnPhysicalNetwork() {
        final PhysicalNetworkVO physnet = mock(PhysicalNetworkVO.class);
        when(physnetdao.findById((Long)any())).thenReturn(physnet);
        when(physnet.getIsolationMethods()).thenReturn(Arrays.asList(new String[] {"STT", "VXLAN"}));
        when(physnet.getId()).thenReturn(NETWORK_ID);

        mock(NiciraNvpDeviceVO.class);
        when(nvpdao.listByPhysicalNetwork(NETWORK_ID)).thenReturn(Collections.<NiciraNvpDeviceVO> emptyList());

        final NetworkOffering offering = mock(NetworkOffering.class);
        when(offering.getId()).thenReturn(NETWORK_ID);
        when(offering.getTrafficType()).thenReturn(TrafficType.Guest);
        when(offering.getGuestType()).thenReturn(GuestType.Isolated);

        final DeploymentPlan plan = mock(DeploymentPlan.class);
        final Network network = mock(Network.class);
        final Account account = mock(Account.class);

        final Network designednetwork = guru.design(offering, plan, network, account);
        assertTrue(designednetwork == null);
    }

    @Test
    public void testDesignNoIsolationMethodSTT() {
        final PhysicalNetworkVO physnet = mock(PhysicalNetworkVO.class);
        when(physnetdao.findById((Long)any())).thenReturn(physnet);
        when(physnet.getIsolationMethods()).thenReturn(Arrays.asList(new String[] {"VLAN"}));
        when(physnet.getId()).thenReturn(NETWORK_ID);

        mock(NiciraNvpDeviceVO.class);
        when(nvpdao.listByPhysicalNetwork(NETWORK_ID)).thenReturn(Collections.<NiciraNvpDeviceVO> emptyList());

        final NetworkOffering offering = mock(NetworkOffering.class);
        when(offering.getId()).thenReturn(NETWORK_ID);
        when(offering.getTrafficType()).thenReturn(TrafficType.Guest);
        when(offering.getGuestType()).thenReturn(GuestType.Isolated);

        final DeploymentPlan plan = mock(DeploymentPlan.class);
        final Network network = mock(Network.class);
        final Account account = mock(Account.class);

        final Network designednetwork = guru.design(offering, plan, network, account);
        assertTrue(designednetwork == null);
    }

    @Test
    public void testDesignNoConnectivityInOffering() {
        final PhysicalNetworkVO physnet = mock(PhysicalNetworkVO.class);
        when(physnetdao.findById((Long)any())).thenReturn(physnet);
        when(physnet.getIsolationMethods()).thenReturn(Arrays.asList(new String[] {"STT", "VXLAN"}));
        when(physnet.getId()).thenReturn(NETWORK_ID);

        final NiciraNvpDeviceVO device = mock(NiciraNvpDeviceVO.class);
        when(nvpdao.listByPhysicalNetwork(NETWORK_ID)).thenReturn(Arrays.asList(new NiciraNvpDeviceVO[] {device}));
        when(device.getId()).thenReturn(1L);

        final NetworkOffering offering = mock(NetworkOffering.class);
        when(offering.getId()).thenReturn(NETWORK_ID);
        when(offering.getTrafficType()).thenReturn(TrafficType.Guest);
        when(offering.getGuestType()).thenReturn(GuestType.Isolated);

        when(nosd.areServicesSupportedByNetworkOffering(NETWORK_ID, Service.Connectivity)).thenReturn(false);

        final DeploymentPlan plan = mock(DeploymentPlan.class);
        final Network network = mock(Network.class);
        final Account account = mock(Account.class);

        final Network designednetwork = guru.design(offering, plan, network, account);
        assertTrue(designednetwork == null);
    }

    @Test
    public void testImplement() throws InsufficientVirtualNetworkCapacityException {
        final PhysicalNetworkVO physnet = mock(PhysicalNetworkVO.class);
        when(physnetdao.findById((Long)any())).thenReturn(physnet);
        when(physnet.getIsolationMethods()).thenReturn(Arrays.asList(new String[] {"STT", "VXLAN"}));
        when(physnet.getId()).thenReturn(NETWORK_ID);

        final NiciraNvpDeviceVO device = mock(NiciraNvpDeviceVO.class);
        when(nvpdao.listByPhysicalNetwork(NETWORK_ID)).thenReturn(Arrays.asList(new NiciraNvpDeviceVO[] {device}));
        when(device.getId()).thenReturn(1L);

        final NetworkOffering offering = mock(NetworkOffering.class);
        when(offering.getId()).thenReturn(NETWORK_ID);
        when(offering.getTrafficType()).thenReturn(TrafficType.Guest);
        when(offering.getGuestType()).thenReturn(GuestType.Isolated);

        when(nosd.areServicesSupportedByNetworkOffering(NETWORK_ID, Service.Connectivity)).thenReturn(false);

        mock(DeploymentPlan.class);

        final NetworkVO network = mock(NetworkVO.class);
        when(network.getName()).thenReturn("testnetwork");
        when(network.getState()).thenReturn(State.Implementing);
        when(network.getPhysicalNetworkId()).thenReturn(NETWORK_ID);

        final DeployDestination dest = mock(DeployDestination.class);

        final DataCenter dc = mock(DataCenter.class);
        when(dest.getDataCenter()).thenReturn(dc);

        final HostVO niciraHost = mock(HostVO.class);
        when(hostdao.findById(anyLong())).thenReturn(niciraHost);
        when(niciraHost.getDetail("transportzoneuuid")).thenReturn("aaaa");
        when(niciraHost.getDetail("transportzoneisotype")).thenReturn("stt");
        when(niciraHost.getId()).thenReturn(NETWORK_ID);

        when(netmodel.findPhysicalNetworkId(anyLong(), (String)any(), (TrafficType)any())).thenReturn(NETWORK_ID);
        final Domain dom = mock(Domain.class);
        when(dom.getName()).thenReturn("domain");
        final Account acc = mock(Account.class);
        when(acc.getAccountName()).thenReturn("accountname");
        final ReservationContext res = mock(ReservationContext.class);
        when(res.getDomain()).thenReturn(dom);
        when(res.getAccount()).thenReturn(acc);

        final CreateLogicalSwitchAnswer answer = mock(CreateLogicalSwitchAnswer.class);
        when(answer.getResult()).thenReturn(true);
        when(answer.getLogicalSwitchUuid()).thenReturn("aaaaa");
        when(agentmgr.easySend(eq(NETWORK_ID), (Command)any())).thenReturn(answer);

        final Network implementednetwork = guru.implement(network, offering, dest, res);
        assertTrue(implementednetwork != null);
        verify(agentmgr, times(1)).easySend(eq(NETWORK_ID), (Command)any());
    }

    @Test
    public void testImplementWithCidr() throws InsufficientVirtualNetworkCapacityException {
        final PhysicalNetworkVO physnet = mock(PhysicalNetworkVO.class);
        when(physnetdao.findById((Long)any())).thenReturn(physnet);
        when(physnet.getIsolationMethods()).thenReturn(Arrays.asList(new String[] {"STT"}));
        when(physnet.getId()).thenReturn(NETWORK_ID);

        final NiciraNvpDeviceVO device = mock(NiciraNvpDeviceVO.class);
        when(nvpdao.listByPhysicalNetwork(NETWORK_ID)).thenReturn(Arrays.asList(new NiciraNvpDeviceVO[] {device}));
        when(device.getId()).thenReturn(1L);

        final NetworkOffering offering = mock(NetworkOffering.class);
        when(offering.getId()).thenReturn(NETWORK_ID);
        when(offering.getTrafficType()).thenReturn(TrafficType.Guest);
        when(offering.getGuestType()).thenReturn(GuestType.Isolated);

        when(nosd.areServicesSupportedByNetworkOffering(NETWORK_ID, Service.Connectivity)).thenReturn(false);

        mock(DeploymentPlan.class);

        final NetworkVO network = mock(NetworkVO.class);
        when(network.getName()).thenReturn("testnetwork");
        when(network.getState()).thenReturn(State.Implementing);
        when(network.getGateway()).thenReturn("10.1.1.1");
        when(network.getCidr()).thenReturn("10.1.1.0/24");
        when(network.getPhysicalNetworkId()).thenReturn(NETWORK_ID);

        final DeployDestination dest = mock(DeployDestination.class);

        final DataCenter dc = mock(DataCenter.class);
        when(dest.getDataCenter()).thenReturn(dc);

        final HostVO niciraHost = mock(HostVO.class);
        when(hostdao.findById(anyLong())).thenReturn(niciraHost);
        when(niciraHost.getDetail("transportzoneuuid")).thenReturn("aaaa");
        when(niciraHost.getDetail("transportzoneisotype")).thenReturn("stt");
        when(niciraHost.getId()).thenReturn(NETWORK_ID);

        when(netmodel.findPhysicalNetworkId(anyLong(), (String)any(), (TrafficType)any())).thenReturn(NETWORK_ID);
        final Domain dom = mock(Domain.class);
        when(dom.getName()).thenReturn("domain");
        final Account acc = mock(Account.class);
        when(acc.getAccountName()).thenReturn("accountname");
        final ReservationContext res = mock(ReservationContext.class);
        when(res.getDomain()).thenReturn(dom);
        when(res.getAccount()).thenReturn(acc);

        final CreateLogicalSwitchAnswer answer = mock(CreateLogicalSwitchAnswer.class);
        when(answer.getResult()).thenReturn(true);
        when(answer.getLogicalSwitchUuid()).thenReturn("aaaaa");
        when(agentmgr.easySend(eq(NETWORK_ID), (Command)any())).thenReturn(answer);

        final Network implementednetwork = guru.implement(network, offering, dest, res);
        assertTrue(implementednetwork != null);
        assertTrue(implementednetwork.getCidr().equals("10.1.1.0/24"));
        assertTrue(implementednetwork.getGateway().equals("10.1.1.1"));
        verify(agentmgr, times(1)).easySend(eq(NETWORK_ID), (Command)any());
    }

    @Test
    public void testImplementURIException() throws InsufficientVirtualNetworkCapacityException {
        final PhysicalNetworkVO physnet = mock(PhysicalNetworkVO.class);
        when(physnetdao.findById((Long)any())).thenReturn(physnet);
        when(physnet.getIsolationMethods()).thenReturn(Arrays.asList(new String[] {"STT"}));
        when(physnet.getId()).thenReturn(NETWORK_ID);

        final NiciraNvpDeviceVO device = mock(NiciraNvpDeviceVO.class);
        when(nvpdao.listByPhysicalNetwork(NETWORK_ID)).thenReturn(Arrays.asList(new NiciraNvpDeviceVO[] {device}));
        when(device.getId()).thenReturn(1L);

        final NetworkOffering offering = mock(NetworkOffering.class);
        when(offering.getId()).thenReturn(NETWORK_ID);
        when(offering.getTrafficType()).thenReturn(TrafficType.Guest);
        when(offering.getGuestType()).thenReturn(GuestType.Isolated);

        when(nosd.areServicesSupportedByNetworkOffering(NETWORK_ID, Service.Connectivity)).thenReturn(false);

        mock(DeploymentPlan.class);

        final NetworkVO network = mock(NetworkVO.class);
        when(network.getName()).thenReturn("testnetwork");
        when(network.getState()).thenReturn(State.Implementing);
        when(network.getPhysicalNetworkId()).thenReturn(NETWORK_ID);

        final DeployDestination dest = mock(DeployDestination.class);

        final DataCenter dc = mock(DataCenter.class);
        when(dest.getDataCenter()).thenReturn(dc);

        final HostVO niciraHost = mock(HostVO.class);
        when(hostdao.findById(anyLong())).thenReturn(niciraHost);
        when(niciraHost.getDetail("transportzoneuuid")).thenReturn("aaaa");
        when(niciraHost.getDetail("transportzoneisotype")).thenReturn("stt");
        when(niciraHost.getId()).thenReturn(NETWORK_ID);

        when(netmodel.findPhysicalNetworkId(anyLong(), (String)any(), (TrafficType)any())).thenReturn(NETWORK_ID);
        final Domain dom = mock(Domain.class);
        when(dom.getName()).thenReturn("domain");
        final Account acc = mock(Account.class);
        when(acc.getAccountName()).thenReturn("accountname");
        final ReservationContext res = mock(ReservationContext.class);
        when(res.getDomain()).thenReturn(dom);
        when(res.getAccount()).thenReturn(acc);

        final CreateLogicalSwitchAnswer answer = mock(CreateLogicalSwitchAnswer.class);
        when(answer.getResult()).thenReturn(true);
        //when(answer.getLogicalSwitchUuid()).thenReturn("aaaaa");
        when(agentmgr.easySend(eq(NETWORK_ID), (Command)any())).thenReturn(answer);

        final Network implementednetwork = guru.implement(network, offering, dest, res);
        assertTrue(implementednetwork == null);
        verify(agentmgr, times(1)).easySend(eq(NETWORK_ID), (Command)any());
    }

    @Test
    public void testShutdown() throws InsufficientVirtualNetworkCapacityException, URISyntaxException {
        final PhysicalNetworkVO physnet = mock(PhysicalNetworkVO.class);
        when(physnetdao.findById((Long)any())).thenReturn(physnet);
        when(physnet.getIsolationMethods()).thenReturn(Arrays.asList(new String[] {"STT", "VXLAN"}));
        when(physnet.getId()).thenReturn(NETWORK_ID);

        final NiciraNvpDeviceVO device = mock(NiciraNvpDeviceVO.class);
        when(nvpdao.listByPhysicalNetwork(NETWORK_ID)).thenReturn(Arrays.asList(new NiciraNvpDeviceVO[] {device}));
        when(device.getId()).thenReturn(1L);

        final NetworkOffering offering = mock(NetworkOffering.class);
        when(offering.getId()).thenReturn(NETWORK_ID);
        when(offering.getTrafficType()).thenReturn(TrafficType.Guest);
        when(offering.getGuestType()).thenReturn(GuestType.Isolated);

        when(nosd.areServicesSupportedByNetworkOffering(NETWORK_ID, Service.Connectivity)).thenReturn(false);

        mock(DeploymentPlan.class);

        final NetworkVO network = mock(NetworkVO.class);
        when(network.getName()).thenReturn("testnetwork");
        when(network.getState()).thenReturn(State.Implementing);
        when(network.getBroadcastDomainType()).thenReturn(BroadcastDomainType.Lswitch);
        when(network.getBroadcastUri()).thenReturn(new URI("lswitch:aaaaa"));
        when(network.getPhysicalNetworkId()).thenReturn(NETWORK_ID);
        when(netdao.findById(NETWORK_ID)).thenReturn(network);

        final DeployDestination dest = mock(DeployDestination.class);

        final DataCenter dc = mock(DataCenter.class);
        when(dest.getDataCenter()).thenReturn(dc);

        final HostVO niciraHost = mock(HostVO.class);
        when(hostdao.findById(anyLong())).thenReturn(niciraHost);
        when(niciraHost.getDetail("transportzoneuuid")).thenReturn("aaaa");
        when(niciraHost.getDetail("transportzoneisotype")).thenReturn("stt");
        when(niciraHost.getId()).thenReturn(NETWORK_ID);

        when(netmodel.findPhysicalNetworkId(anyLong(), (String)any(), (TrafficType)any())).thenReturn(NETWORK_ID);
        final Domain dom = mock(Domain.class);
        when(dom.getName()).thenReturn("domain");
        final Account acc = mock(Account.class);
        when(acc.getAccountName()).thenReturn("accountname");
        final ReservationContext res = mock(ReservationContext.class);
        when(res.getDomain()).thenReturn(dom);
        when(res.getAccount()).thenReturn(acc);

        final DeleteLogicalSwitchAnswer answer = mock(DeleteLogicalSwitchAnswer.class);
        when(answer.getResult()).thenReturn(true);
        when(agentmgr.easySend(eq(NETWORK_ID), (Command)any())).thenReturn(answer);

        final NetworkProfile implementednetwork = mock(NetworkProfile.class);
        when(implementednetwork.getId()).thenReturn(NETWORK_ID);
        when(implementednetwork.getBroadcastUri()).thenReturn(new URI("lswitch:aaaa"));
        when(offering.isSpecifyVlan()).thenReturn(false);

        guru.shutdown(implementednetwork, offering);
        verify(agentmgr, times(1)).easySend(eq(NETWORK_ID), (Command)any());
        verify(implementednetwork, times(1)).setBroadcastUri(null);
    }
}
