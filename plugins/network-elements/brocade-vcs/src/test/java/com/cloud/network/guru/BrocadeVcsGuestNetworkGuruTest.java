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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.AssociateMacToNetworkAnswer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.CreateNetworkAnswer;
import com.cloud.agent.api.DeleteNetworkAnswer;
import com.cloud.agent.api.DisassociateMacFromNetworkAnswer;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.domain.Domain;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientVirtualNetworkCapacityException;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.network.BrocadeVcsDeviceVO;
import com.cloud.network.BrocadeVcsNetworkVlanMappingVO;
import com.cloud.network.Network;
import com.cloud.network.Network.GuestType;
import com.cloud.network.Network.Service;
import com.cloud.network.Network.State;
import com.cloud.network.NetworkModel;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.dao.BrocadeVcsDao;
import com.cloud.network.dao.BrocadeVcsNetworkVlanMappingDao;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.dao.PhysicalNetworkVO;
import com.cloud.offering.NetworkOffering;
import com.cloud.offerings.dao.NetworkOfferingServiceMapDao;
import com.cloud.user.Account;
import com.cloud.vm.Nic.ReservationStrategy;
import com.cloud.vm.NicProfile;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VirtualMachineProfile;

public class BrocadeVcsGuestNetworkGuruTest {
    private static final long NETWORK_ID = 42L;
    PhysicalNetworkDao physnetdao = mock(PhysicalNetworkDao.class);
    BrocadeVcsDao vcsdao = mock(BrocadeVcsDao.class);
    BrocadeVcsNetworkVlanMappingDao vcsmapdao = mock(BrocadeVcsNetworkVlanMappingDao.class);
    DataCenterDao dcdao = mock(DataCenterDao.class);
    NetworkOfferingServiceMapDao nosd = mock(NetworkOfferingServiceMapDao.class);
    AgentManager agentmgr = mock(AgentManager.class);
    NetworkOrchestrationService netmgr = mock(NetworkOrchestrationService.class);
    NetworkModel netmodel = mock(NetworkModel.class);

    HostDao hostdao = mock(HostDao.class);
    NetworkDao netdao = mock(NetworkDao.class);
    GuestNetworkGuru guestGuru = mock(GuestNetworkGuru.class);
    BrocadeVcsGuestNetworkGuru guru;

    @Before
    public void setUp() {

        guru = new BrocadeVcsGuestNetworkGuru();
        ((GuestNetworkGuru)guru)._physicalNetworkDao = physnetdao;
        guru._brocadeVcsDao = vcsdao;
        guru._brocadeVcsNetworkVlanDao = vcsmapdao;
        guru._hostDao = hostdao;
        guru._ntwkOfferingSrvcDao = nosd;
        guru._dcDao = dcdao;
        guru._agentMgr = agentmgr;
        ((GuestNetworkGuru)guru)._networkModel = netmodel;

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
        when(physnet.getIsolationMethods()).thenReturn(Arrays.asList(new String[] {"VCS"}));
        when(physnet.getId()).thenReturn(NETWORK_ID);

        when(nosd.areServicesSupportedByNetworkOffering(NETWORK_ID, Service.Connectivity)).thenReturn(true);

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

        // Not supported: IsolationMethod != VCS
        when(physnet.getIsolationMethods()).thenReturn(Arrays.asList(new String[] {"VLAN"}));
        assertFalse(guru.canHandle(offering, NetworkType.Advanced, physnet) == true);

    }

    @Test
    public void testDesign() {
        final PhysicalNetworkVO physnet = mock(PhysicalNetworkVO.class);
        when(physnetdao.findById((Long)any())).thenReturn(physnet);
        when(physnet.getIsolationMethods()).thenReturn(Arrays.asList(new String[] {"VCS"}));
        when(physnet.getId()).thenReturn(NETWORK_ID);

        final NetworkOffering offering = mock(NetworkOffering.class);
        when(offering.getId()).thenReturn(NETWORK_ID);
        when(offering.getTrafficType()).thenReturn(TrafficType.Guest);
        when(offering.getGuestType()).thenReturn(GuestType.Isolated);

        when(nosd.areServicesSupportedByNetworkOffering(NETWORK_ID, Service.Connectivity)).thenReturn(true);

        when(netmodel.listNetworkOfferingServices(NETWORK_ID)).thenReturn(Arrays.asList(Service.Connectivity));

        final DeploymentPlan plan = mock(DeploymentPlan.class);
        final Network network = mock(Network.class);
        final Account account = mock(Account.class);

        final Network designednetwork = guru.design(offering, plan, network, "", 1L, account);
        assertTrue(designednetwork != null);
        assertTrue(designednetwork.getBroadcastDomainType() == BroadcastDomainType.Vcs);
    }

    @Test
    public void testDesignNoIsolationMethodVCS() {
        final PhysicalNetworkVO physnet = mock(PhysicalNetworkVO.class);
        when(physnetdao.findById((Long)any())).thenReturn(physnet);
        when(physnet.getIsolationMethods()).thenReturn(Arrays.asList(new String[] {"VLAN"}));
        when(physnet.getId()).thenReturn(NETWORK_ID);

        final NetworkOffering offering = mock(NetworkOffering.class);
        when(offering.getId()).thenReturn(NETWORK_ID);
        when(offering.getTrafficType()).thenReturn(TrafficType.Guest);
        when(offering.getGuestType()).thenReturn(GuestType.Isolated);

        final DeploymentPlan plan = mock(DeploymentPlan.class);
        final Network network = mock(Network.class);
        final Account account = mock(Account.class);

        final Network designednetwork = guru.design(offering, plan, network, "", 1L, account);
        assertTrue(designednetwork == null);
    }

    @Test
    public void testDesignNoConnectivityInOffering() {
        final PhysicalNetworkVO physnet = mock(PhysicalNetworkVO.class);
        when(physnetdao.findById((Long)any())).thenReturn(physnet);
        when(physnet.getIsolationMethods()).thenReturn(Arrays.asList(new String[] {"VCS"}));
        when(physnet.getId()).thenReturn(NETWORK_ID);

        final NetworkOffering offering = mock(NetworkOffering.class);
        when(offering.getId()).thenReturn(NETWORK_ID);
        when(offering.getTrafficType()).thenReturn(TrafficType.Guest);
        when(offering.getGuestType()).thenReturn(GuestType.Isolated);

        when(nosd.areServicesSupportedByNetworkOffering(NETWORK_ID, Service.Connectivity)).thenReturn(false);

        final DeploymentPlan plan = mock(DeploymentPlan.class);
        final Network network = mock(Network.class);
        final Account account = mock(Account.class);

        final Network designednetwork = guru.design(offering, plan, network, "", 1L, account);
        assertTrue(designednetwork == null);
    }

    @Test
    public void testImplement() throws InsufficientVirtualNetworkCapacityException, URISyntaxException {
        final PhysicalNetworkVO physnet = mock(PhysicalNetworkVO.class);
        when(physnetdao.findById((Long)any())).thenReturn(physnet);
        when(physnet.getIsolationMethods()).thenReturn(Arrays.asList(new String[] {"VCS"}));
        when(physnet.getId()).thenReturn(NETWORK_ID);

        final NetworkOffering offering = mock(NetworkOffering.class);
        when(offering.getId()).thenReturn(NETWORK_ID);
        when(offering.getTrafficType()).thenReturn(TrafficType.Guest);
        when(offering.getGuestType()).thenReturn(GuestType.Isolated);

        when(nosd.areServicesSupportedByNetworkOffering(NETWORK_ID, Service.Connectivity)).thenReturn(true);

        mock(DeploymentPlan.class);

        final NetworkVO network = mock(NetworkVO.class);
        when(network.getName()).thenReturn("testnetwork");
        when(network.getState()).thenReturn(State.Implementing);
        when(network.getPhysicalNetworkId()).thenReturn(NETWORK_ID);
        when(network.getBroadcastUri()).thenReturn(new URI("vlan://14"));

        final DeployDestination dest = mock(DeployDestination.class);

        final DataCenter dc = mock(DataCenter.class);
        when(dest.getDataCenter()).thenReturn(dc);

        final HostVO brocadeHost = mock(HostVO.class);
        when(hostdao.findById(anyLong())).thenReturn(brocadeHost);
        when(brocadeHost.getId()).thenReturn(NETWORK_ID);

        when(netmodel.findPhysicalNetworkId(anyLong(), (String)any(), (TrafficType)any())).thenReturn(NETWORK_ID);

        final BrocadeVcsDeviceVO brocadeDevice = mock(BrocadeVcsDeviceVO.class);
        when(brocadeDevice.getHostId()).thenReturn(NETWORK_ID);

        List<BrocadeVcsDeviceVO> devices = new ArrayList();
        devices.add(brocadeDevice);
        when(vcsdao.listByPhysicalNetwork(anyLong())).thenReturn(devices);

        final Domain dom = mock(Domain.class);
        when(dom.getName()).thenReturn("domain");
        final Account acc = mock(Account.class);
        when(acc.getAccountName()).thenReturn("accountname");
        final ReservationContext res = mock(ReservationContext.class);
        when(res.getDomain()).thenReturn(dom);
        when(res.getAccount()).thenReturn(acc);

        when(guestGuru.implement(network, offering, dest, res)).thenReturn(network);

        final CreateNetworkAnswer answer = mock(CreateNetworkAnswer.class);
        when(answer.getResult()).thenReturn(true);

        when(agentmgr.easySend(eq(NETWORK_ID), (Command)any())).thenReturn(answer);

        final Network implementednetwork = guru.implement(network, offering, dest, res);
        assertTrue(implementednetwork != null);
        verify(agentmgr, times(1)).easySend(eq(NETWORK_ID), (Command)any());
    }

    @Test
    public void testImplementFail() throws InsufficientVirtualNetworkCapacityException, URISyntaxException {
        final PhysicalNetworkVO physnet = mock(PhysicalNetworkVO.class);
        when(physnetdao.findById((Long)any())).thenReturn(physnet);
        when(physnet.getIsolationMethods()).thenReturn(Arrays.asList(new String[] {"VCS"}));
        when(physnet.getId()).thenReturn(NETWORK_ID);

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
        when(network.getBroadcastUri()).thenReturn(new URI("vlan://14"));

        final DeployDestination dest = mock(DeployDestination.class);

        final DataCenter dc = mock(DataCenter.class);
        when(dest.getDataCenter()).thenReturn(dc);

        final HostVO brocadeHost = mock(HostVO.class);
        when(hostdao.findById(anyLong())).thenReturn(brocadeHost);
        when(brocadeHost.getId()).thenReturn(NETWORK_ID);

        when(netmodel.findPhysicalNetworkId(anyLong(), (String)any(), (TrafficType)any())).thenReturn(NETWORK_ID);

        final BrocadeVcsDeviceVO brocadeDevice = mock(BrocadeVcsDeviceVO.class);
        when(brocadeDevice.getHostId()).thenReturn(NETWORK_ID);

        final List<BrocadeVcsDeviceVO> devices = mock(List.class);
        when(devices.isEmpty()).thenReturn(true);
        when(vcsdao.listByPhysicalNetwork(anyLong())).thenReturn(devices);

        final Domain dom = mock(Domain.class);
        when(dom.getName()).thenReturn("domain");
        final Account acc = mock(Account.class);
        when(acc.getAccountName()).thenReturn("accountname");
        final ReservationContext res = mock(ReservationContext.class);
        when(res.getDomain()).thenReturn(dom);
        when(res.getAccount()).thenReturn(acc);

        when(guestGuru.implement(network, offering, dest, res)).thenReturn(network);

        final CreateNetworkAnswer answer = mock(CreateNetworkAnswer.class);
        when(answer.getResult()).thenReturn(true);
        when(agentmgr.easySend(eq(NETWORK_ID), (Command)any())).thenReturn(answer);

        final Network implementednetwork = guru.implement(network, offering, dest, res);
        assertTrue(implementednetwork == null);
        verify(agentmgr, times(0)).easySend(eq(NETWORK_ID), (Command)any());
    }

    @Test
    public void testReserve() throws InsufficientVirtualNetworkCapacityException, URISyntaxException, InsufficientAddressCapacityException {

        final NetworkVO network = mock(NetworkVO.class);
        when(network.getName()).thenReturn("testnetwork");
        when(network.getState()).thenReturn(State.Implementing);
        when(network.getPhysicalNetworkId()).thenReturn(NETWORK_ID);
        when(network.getBroadcastUri()).thenReturn(new URI("vlan://14"));
        when(network.getDataCenterId()).thenReturn(NETWORK_ID);

        final NicProfile nic = mock(NicProfile.class);
        when(nic.getMacAddress()).thenReturn("macaddress");
        when(nic.getReservationStrategy()).thenReturn(ReservationStrategy.Start);

        final VirtualMachineProfile vmProfile = mock(VirtualMachineProfile.class);

        final DeployDestination dest = mock(DeployDestination.class);

        final DataCenterVO dc = mock(DataCenterVO.class);
        when(dest.getDataCenter()).thenReturn(dc);
        when(dcdao.findById((long)anyInt())).thenReturn(dc);

        final HostVO brocadeHost = mock(HostVO.class);
        when(hostdao.findById(anyLong())).thenReturn(brocadeHost);
        when(brocadeHost.getId()).thenReturn(NETWORK_ID);

        when(netmodel.findPhysicalNetworkId(anyLong(), (String)any(), (TrafficType)any())).thenReturn(NETWORK_ID);

        final BrocadeVcsDeviceVO brocadeDevice = mock(BrocadeVcsDeviceVO.class);
        when(brocadeDevice.getHostId()).thenReturn(NETWORK_ID);

        List<BrocadeVcsDeviceVO> devices = new ArrayList();
        devices.add(brocadeDevice);
        when(vcsdao.listByPhysicalNetwork(anyLong())).thenReturn(devices);

        final Domain dom = mock(Domain.class);
        when(dom.getName()).thenReturn("domain");
        final Account acc = mock(Account.class);
        when(acc.getAccountName()).thenReturn("accountname");
        final ReservationContext res = mock(ReservationContext.class);
        when(res.getDomain()).thenReturn(dom);
        when(res.getAccount()).thenReturn(acc);

        final AssociateMacToNetworkAnswer answer = mock(AssociateMacToNetworkAnswer.class);
        when(answer.getResult()).thenReturn(true);

        when(agentmgr.easySend(eq(NETWORK_ID), (Command)any())).thenReturn(answer);

        guru.reserve(nic, network, vmProfile, dest, res);
        verify(agentmgr, times(1)).easySend(eq(NETWORK_ID), (Command)any());
    }

    @Test
    public void testReserveFail() throws InsufficientVirtualNetworkCapacityException, URISyntaxException, InsufficientAddressCapacityException {

        final NetworkVO network = mock(NetworkVO.class);
        when(network.getName()).thenReturn("testnetwork");
        when(network.getState()).thenReturn(State.Implementing);
        when(network.getPhysicalNetworkId()).thenReturn(NETWORK_ID);
        when(network.getBroadcastUri()).thenReturn(new URI("vlan://14"));
        when(network.getDataCenterId()).thenReturn(NETWORK_ID);

        final NicProfile nic = mock(NicProfile.class);
        when(nic.getMacAddress()).thenReturn("macaddress");
        when(nic.getReservationStrategy()).thenReturn(ReservationStrategy.Start);

        final VirtualMachineProfile vmProfile = mock(VirtualMachineProfile.class);

        final DeployDestination dest = mock(DeployDestination.class);

        final DataCenterVO dc = mock(DataCenterVO.class);
        when(dest.getDataCenter()).thenReturn(dc);
        when(dcdao.findById((long)anyInt())).thenReturn(dc);

        final HostVO brocadeHost = mock(HostVO.class);
        when(hostdao.findById(anyLong())).thenReturn(brocadeHost);
        when(brocadeHost.getId()).thenReturn(NETWORK_ID);

        when(netmodel.findPhysicalNetworkId(anyLong(), (String)any(), (TrafficType)any())).thenReturn(NETWORK_ID);

        final BrocadeVcsDeviceVO brocadeDevice = mock(BrocadeVcsDeviceVO.class);
        when(brocadeDevice.getHostId()).thenReturn(NETWORK_ID);

        final List<BrocadeVcsDeviceVO> devices = mock(List.class);
        when(devices.isEmpty()).thenReturn(true);
        when(vcsdao.listByPhysicalNetwork(anyLong())).thenReturn(devices);

        final Domain dom = mock(Domain.class);
        when(dom.getName()).thenReturn("domain");
        final Account acc = mock(Account.class);
        when(acc.getAccountName()).thenReturn("accountname");
        final ReservationContext res = mock(ReservationContext.class);
        when(res.getDomain()).thenReturn(dom);
        when(res.getAccount()).thenReturn(acc);

        final AssociateMacToNetworkAnswer answer = mock(AssociateMacToNetworkAnswer.class);
        when(answer.getResult()).thenReturn(true);

        when(agentmgr.easySend(eq(NETWORK_ID), (Command)any())).thenReturn(answer);

        guru.reserve(nic, network, vmProfile, dest, res);
        verify(agentmgr, times(0)).easySend(eq(NETWORK_ID), (Command)any());
    }

    @Test
    public void testDeallocate() {

        final NetworkVO network = mock(NetworkVO.class);
        when(network.getName()).thenReturn("testnetwork");
        when(network.getState()).thenReturn(State.Implementing);
        when(network.getPhysicalNetworkId()).thenReturn(NETWORK_ID);
        when(network.getDataCenterId()).thenReturn(NETWORK_ID);

        final NicProfile nic = mock(NicProfile.class);
        when(nic.getMacAddress()).thenReturn("macaddress");
        when(nic.getReservationStrategy()).thenReturn(ReservationStrategy.Start);

        final VirtualMachineProfile vmProfile = mock(VirtualMachineProfile.class);

        final HostVO brocadeHost = mock(HostVO.class);
        when(hostdao.findById(anyLong())).thenReturn(brocadeHost);
        when(brocadeHost.getId()).thenReturn(NETWORK_ID);

        when(netmodel.findPhysicalNetworkId(anyLong(), (String)any(), (TrafficType)any())).thenReturn(NETWORK_ID);

        final BrocadeVcsDeviceVO brocadeDevice = mock(BrocadeVcsDeviceVO.class);
        when(brocadeDevice.getHostId()).thenReturn(NETWORK_ID);

        final List<BrocadeVcsDeviceVO> devices = new ArrayList();
        devices.add(brocadeDevice);
        when(vcsdao.listByPhysicalNetwork(anyLong())).thenReturn(devices);

        final DisassociateMacFromNetworkAnswer answer = mock(DisassociateMacFromNetworkAnswer.class);
        when(answer.getResult()).thenReturn(true);
        when(agentmgr.easySend(eq(NETWORK_ID), (Command)any())).thenReturn(answer);

        guru.deallocate(network, nic, vmProfile);
        verify(agentmgr, times(1)).easySend(eq(NETWORK_ID), (Command)any());
    }

    @Test
    public void testDeallocateFail() {

        final NetworkVO network = mock(NetworkVO.class);
        when(network.getName()).thenReturn("testnetwork");
        when(network.getState()).thenReturn(State.Implementing);
        when(network.getPhysicalNetworkId()).thenReturn(NETWORK_ID);
        when(network.getDataCenterId()).thenReturn(NETWORK_ID);

        final NicProfile nic = mock(NicProfile.class);
        when(nic.getMacAddress()).thenReturn("macaddress");
        when(nic.getReservationStrategy()).thenReturn(ReservationStrategy.Start);

        final VirtualMachineProfile vmProfile = mock(VirtualMachineProfile.class);

        final HostVO brocadeHost = mock(HostVO.class);
        when(hostdao.findById(anyLong())).thenReturn(brocadeHost);
        when(brocadeHost.getId()).thenReturn(NETWORK_ID);

        when(netmodel.findPhysicalNetworkId(anyLong(), (String)any(), (TrafficType)any())).thenReturn(NETWORK_ID);

        final BrocadeVcsDeviceVO brocadeDevice = mock(BrocadeVcsDeviceVO.class);
        when(brocadeDevice.getHostId()).thenReturn(NETWORK_ID);

        final List<BrocadeVcsDeviceVO> devices = mock(List.class);
        when(devices.isEmpty()).thenReturn(true);
        when(vcsdao.listByPhysicalNetwork(anyLong())).thenReturn(devices);

        final DisassociateMacFromNetworkAnswer answer = mock(DisassociateMacFromNetworkAnswer.class);
        when(answer.getResult()).thenReturn(true);
        when(agentmgr.easySend(eq(NETWORK_ID), (Command)any())).thenReturn(answer);

        guru.deallocate(network, nic, vmProfile);
        verify(agentmgr, times(0)).easySend(eq(NETWORK_ID), (Command)any());
    }

    @Test
    public void testTrash() {

        final NetworkVO network = mock(NetworkVO.class);
        when(network.getName()).thenReturn("testnetwork");
        when(network.getState()).thenReturn(State.Implementing);
        when(network.getId()).thenReturn(NETWORK_ID);
        when(network.getDataCenterId()).thenReturn(NETWORK_ID);

        final NetworkOffering offering = mock(NetworkOffering.class);
        when(offering.getId()).thenReturn(NETWORK_ID);
        when(offering.getTrafficType()).thenReturn(TrafficType.Guest);
        when(offering.getGuestType()).thenReturn(GuestType.Isolated);

        final HostVO brocadeHost = mock(HostVO.class);
        when(hostdao.findById(anyLong())).thenReturn(brocadeHost);
        when(brocadeHost.getId()).thenReturn(NETWORK_ID);

        when(netmodel.findPhysicalNetworkId(anyLong(), (String)any(), (TrafficType)any())).thenReturn(NETWORK_ID);

        final BrocadeVcsNetworkVlanMappingVO mapping = mock(BrocadeVcsNetworkVlanMappingVO.class);
        when(mapping.getVlanId()).thenReturn(14);
        when(vcsmapdao.findByNetworkId(anyLong())).thenReturn(mapping);

        final BrocadeVcsDeviceVO brocadeDevice = mock(BrocadeVcsDeviceVO.class);
        when(brocadeDevice.getHostId()).thenReturn(NETWORK_ID);

        final List<BrocadeVcsDeviceVO> devices = new ArrayList();
        devices.add(brocadeDevice);
        when(vcsdao.listByPhysicalNetwork(anyLong())).thenReturn(devices);

        final DeleteNetworkAnswer answer = mock(DeleteNetworkAnswer.class);
        when(answer.getResult()).thenReturn(true);
        when(agentmgr.easySend(eq(NETWORK_ID), (Command)any())).thenReturn(answer);

        when(vcsdao.remove((long)anyInt())).thenReturn(true);

        final boolean result = guru.trash(network, offering);
        assertTrue(result == true);
        verify(agentmgr, times(1)).easySend(eq(NETWORK_ID), (Command)any());
    }

    @Test
    public void testTrashFail() {

        final NetworkVO network = mock(NetworkVO.class);
        when(network.getName()).thenReturn("testnetwork");
        when(network.getState()).thenReturn(State.Implementing);
        when(network.getId()).thenReturn(NETWORK_ID);
        when(network.getDataCenterId()).thenReturn(NETWORK_ID);

        final NetworkOffering offering = mock(NetworkOffering.class);
        when(offering.getId()).thenReturn(NETWORK_ID);
        when(offering.getTrafficType()).thenReturn(TrafficType.Guest);
        when(offering.getGuestType()).thenReturn(GuestType.Isolated);

        final HostVO brocadeHost = mock(HostVO.class);
        when(hostdao.findById(anyLong())).thenReturn(brocadeHost);
        when(brocadeHost.getId()).thenReturn(NETWORK_ID);

        when(netmodel.findPhysicalNetworkId(anyLong(), (String)any(), (TrafficType)any())).thenReturn(NETWORK_ID);

        final BrocadeVcsNetworkVlanMappingVO mapping = mock(BrocadeVcsNetworkVlanMappingVO.class);
        when(mapping.getVlanId()).thenReturn(14);
        when(vcsmapdao.findByNetworkId(anyLong())).thenReturn(mapping);
        when(vcsmapdao.remove(anyLong())).thenReturn(true);

        final BrocadeVcsDeviceVO brocadeDevice = mock(BrocadeVcsDeviceVO.class);
        when(brocadeDevice.getHostId()).thenReturn(NETWORK_ID);

        final List<BrocadeVcsDeviceVO> devices = mock(List.class);
        when(devices.isEmpty()).thenReturn(true);
        when(vcsdao.listByPhysicalNetwork(anyLong())).thenReturn(devices);

        final boolean result = guru.trash(network, offering);
        assertTrue(result == false);
        verify(agentmgr, times(0)).easySend(eq(NETWORK_ID), (Command)any());
    }
}
