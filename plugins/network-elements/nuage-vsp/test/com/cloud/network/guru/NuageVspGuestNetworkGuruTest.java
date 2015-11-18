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

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.domain.Domain;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientVirtualNetworkCapacityException;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.network.Network;
import com.cloud.network.Network.GuestType;
import com.cloud.network.Network.Service;
import com.cloud.network.NetworkModel;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.Networks.Mode;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.NuageVspDeviceVO;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.NuageVspDao;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.dao.PhysicalNetworkVO;
import com.cloud.network.manager.NuageVspManager;
import com.cloud.offering.NetworkOffering;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.offerings.dao.NetworkOfferingServiceMapDao;
import com.cloud.user.Account;
import com.cloud.user.AccountVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.vm.NicProfile;
import com.cloud.vm.NicVO;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.dao.NicDao;
import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static com.cloud.network.manager.NuageVspManager.NuageVspIsolatedNetworkDomainTemplateName;
import static com.cloud.network.manager.NuageVspManager.NuageVspSharedNetworkDomainTemplateName;
import static com.cloud.network.manager.NuageVspManager.NuageVspVpcDomainTemplateName;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NuageVspGuestNetworkGuruTest {
    private static final long NETWORK_ID = 42L;
    PhysicalNetworkDao physnetdao = mock(PhysicalNetworkDao.class);
    DataCenterDao dcdao = mock(DataCenterDao.class);
    NetworkOfferingServiceMapDao nosd = mock(NetworkOfferingServiceMapDao.class);
    AgentManager agentManager = mock(AgentManager.class);
    NetworkOrchestrationService netmgr = mock(NetworkOrchestrationService.class);
    NetworkModel networkModel = mock(NetworkModel.class);
    AccountDao accountDao = mock(AccountDao.class);
    DomainDao domainDao = mock(DomainDao.class);
    NicDao nicDao = mock(NicDao.class);
    NetworkOfferingDao ntwkOfferDao = mock(NetworkOfferingDao.class);
    NuageVspDao nuageVspDao = mock(NuageVspDao.class);
    HostDao hostDao = mock(HostDao.class);
    NetworkDao networkDao = mock(NetworkDao.class);
    ConfigurationDao configDao = mock(ConfigurationDao.class);
    IPAddressDao ipAddressDao = mock(IPAddressDao.class);
    NuageVspManager nuageVspManager = mock(NuageVspManager.class);

    NetworkDao netdao = mock(NetworkDao.class);
    NuageVspGuestNetworkGuru guru;

    @Before
    public void setUp() {
        guru = new NuageVspGuestNetworkGuru();
        guru._physicalNetworkDao = physnetdao;
        guru._physicalNetworkDao = physnetdao;
        guru._nuageVspDao = nuageVspDao;
        guru._dcDao = dcdao;
        guru._ntwkOfferingSrvcDao = nosd;
        guru._networkModel = networkModel;
        guru._hostDao = hostDao;
        guru._agentMgr = agentManager;
        guru._networkDao = netdao;
        guru._networkDao = networkDao;
        guru._accountDao = accountDao;
        guru._domainDao = domainDao;
        guru._nicDao = nicDao;
        guru._ntwkOfferingDao = ntwkOfferDao;
        guru._configDao = configDao;
        guru._ipAddressDao = ipAddressDao;
        guru._nuageVspManager = nuageVspManager;

        final DataCenterVO dc = mock(DataCenterVO.class);
        when(dc.getNetworkType()).thenReturn(NetworkType.Advanced);
        when(dc.getGuestNetworkCidr()).thenReturn("10.1.1.1/24");

        when(dcdao.findById((Long)any())).thenReturn(dc);

        when(configDao.getValue(NuageVspIsolatedNetworkDomainTemplateName.key())).thenReturn("IsolatedDomainTemplate");
        when(configDao.getValue(NuageVspVpcDomainTemplateName.key())).thenReturn("VpcDomainTemplate");
        when(configDao.getValue(NuageVspSharedNetworkDomainTemplateName.key())).thenReturn("SharedDomainTemplate");
    }

    @Test
    public void testCanHandle() {
        final NetworkOffering offering = mock(NetworkOffering.class);
        when(offering.getId()).thenReturn(NETWORK_ID);
        when(offering.getTrafficType()).thenReturn(TrafficType.Guest);
        when(offering.getGuestType()).thenReturn(GuestType.Isolated);

        final PhysicalNetworkVO physnet = mock(PhysicalNetworkVO.class);
        when(physnet.getIsolationMethods()).thenReturn(Arrays.asList(new String[] {"VSP"}));
        when(physnet.getId()).thenReturn(NETWORK_ID);

        when(nosd.areServicesSupportedByNetworkOffering(NETWORK_ID, Service.Connectivity)).thenReturn(true);

        assertTrue(guru.canHandle(offering, NetworkType.Advanced, physnet) == true);

        // Not supported TrafficType != Guest
        when(offering.getTrafficType()).thenReturn(TrafficType.Management);
        assertFalse(guru.canHandle(offering, NetworkType.Advanced, physnet) == true);

        // Supported: GuestType Shared
        when(offering.getTrafficType()).thenReturn(TrafficType.Guest);
        when(offering.getGuestType()).thenReturn(GuestType.Shared);
        assertTrue(guru.canHandle(offering, NetworkType.Advanced, physnet) == true);

        // Not supported: Basic networking
        when(offering.getGuestType()).thenReturn(GuestType.Isolated);
        assertFalse(guru.canHandle(offering, NetworkType.Basic, physnet) == true);

        // Not supported: IsolationMethod != STT
        when(physnet.getIsolationMethods()).thenReturn(Arrays.asList(new String[] {"VLAN"}));
        assertFalse(guru.canHandle(offering, NetworkType.Advanced, physnet) == true);

    }

    @Test
    public void testDesign() {
        final PhysicalNetworkVO physnet = mock(PhysicalNetworkVO.class);
        when(physnetdao.findById((Long)any())).thenReturn(physnet);
        when(physnet.getIsolationMethods()).thenReturn(Arrays.asList(new String[] {"VSP"}));
        when(physnet.getId()).thenReturn(NETWORK_ID);

        final NuageVspDeviceVO device = mock(NuageVspDeviceVO.class);
        when(nuageVspDao.listByPhysicalNetwork(NETWORK_ID)).thenReturn(Arrays.asList(new NuageVspDeviceVO[] {device}));
        when(device.getId()).thenReturn(1L);

        final NetworkOffering offering = mock(NetworkOffering.class);
        when(offering.getId()).thenReturn(NETWORK_ID);
        when(offering.getTrafficType()).thenReturn(TrafficType.Guest);
        when(offering.getGuestType()).thenReturn(GuestType.Isolated);

        when(nosd.areServicesSupportedByNetworkOffering(NETWORK_ID, Service.Connectivity)).thenReturn(true);

        final DeploymentPlan plan = mock(DeploymentPlan.class);
        final Network network = mock(Network.class);
        final Account account = mock(Account.class);

        final Network designednetwork = guru.design(offering, plan, network, account);
        assertTrue(designednetwork != null);
        assertTrue(designednetwork.getBroadcastDomainType() == BroadcastDomainType.Vsp);
    }

    @Test
    public void testDesignNoElementOnPhysicalNetwork() {
        final PhysicalNetworkVO physnet = mock(PhysicalNetworkVO.class);
        when(physnetdao.findById((Long)any())).thenReturn(physnet);
        when(physnet.getIsolationMethods()).thenReturn(Arrays.asList(new String[] {"STT"}));
        when(physnet.getId()).thenReturn(NETWORK_ID);

        mock(NuageVspDeviceVO.class);
        when(nuageVspDao.listByPhysicalNetwork(NETWORK_ID)).thenReturn(Collections.<NuageVspDeviceVO> emptyList());

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
    public void testDesignNoIsolationMethodVSP() {
        final PhysicalNetworkVO physnet = mock(PhysicalNetworkVO.class);
        when(physnetdao.findById((Long)any())).thenReturn(physnet);
        when(physnet.getIsolationMethods()).thenReturn(Arrays.asList(new String[] {"VLAN"}));
        when(physnet.getId()).thenReturn(NETWORK_ID);

        mock(NuageVspDeviceVO.class);
        when(nuageVspDao.listByPhysicalNetwork(NETWORK_ID)).thenReturn(Collections.<NuageVspDeviceVO> emptyList());

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
    public void testReserve() throws InsufficientVirtualNetworkCapacityException, InsufficientAddressCapacityException, URISyntaxException {
        final NetworkVO network = mock(NetworkVO.class);
        when(network.getId()).thenReturn(NETWORK_ID);
        when(network.getUuid()).thenReturn("aaaaaa");
        when(network.getDataCenterId()).thenReturn(NETWORK_ID);
        when(network.getNetworkOfferingId()).thenReturn(NETWORK_ID);
        when(network.getPhysicalNetworkId()).thenReturn(NETWORK_ID);
        when(network.getDomainId()).thenReturn(NETWORK_ID);
        when(network.getAccountId()).thenReturn(NETWORK_ID);
        when(network.getVpcId()).thenReturn(null);
        when(network.getBroadcastUri()).thenReturn(new URI("vsp://aaaaaa-aavvv/10.1.1.1"));

        final DataCenterVO dataCenter = mock(DataCenterVO.class);
        when(dcdao.findById(NETWORK_ID)).thenReturn(dataCenter);
        final AccountVO networksAccount = mock(AccountVO.class);
        when(networksAccount.getUuid()).thenReturn("aaaa-abbbb");
        when(networksAccount.getType()).thenReturn(Account.ACCOUNT_TYPE_NORMAL);
        when(accountDao.findById(NETWORK_ID)).thenReturn(networksAccount);
        final DomainVO networksDomain = mock(DomainVO.class);
        when(networksDomain.getUuid()).thenReturn("aaaaa-bbbbb");
        when(domainDao.findById(NETWORK_ID)).thenReturn(networksDomain);

        final NicVO nicvo = mock(NicVO.class);
        when(nicvo.getId()).thenReturn(NETWORK_ID);
        when(nicvo.getMacAddress()).thenReturn("aa-aa-aa-aa-aa-aa");
        when(nicvo.getUuid()).thenReturn("aaaa-fffff");
        when(nicDao.findById(NETWORK_ID)).thenReturn(nicvo);

        final VirtualMachine vm = mock(VirtualMachine.class);
        when(vm.getId()).thenReturn(NETWORK_ID);
        when(vm.getType()).thenReturn(VirtualMachine.Type.User);

        final VirtualMachineProfile vmProfile = mock(VirtualMachineProfile.class);
        when(vmProfile.getType()).thenReturn(VirtualMachine.Type.User);
        when(vmProfile.getInstanceName()).thenReturn("");
        when(vmProfile.getUuid()).thenReturn("aaaa-bbbbb");
        when(vmProfile.getVirtualMachine()).thenReturn(vm);

        NicProfile nicProfile = mock(NicProfile.class);
        when(nicProfile.getUuid()).thenReturn("aaa-bbbb");
        when(nicProfile.getId()).thenReturn(NETWORK_ID);
        when(nicProfile.getMacAddress()).thenReturn("aa-aa-aa-aa-aa-aa");

        final NetworkOfferingVO ntwkoffering = mock(NetworkOfferingVO.class);
        when(ntwkoffering.getId()).thenReturn(NETWORK_ID);
        when(ntwkOfferDao.findById(NETWORK_ID)).thenReturn(ntwkoffering);

        final HostVO host = mock(HostVO.class);
        when(host.getId()).thenReturn(NETWORK_ID);
        final NuageVspDeviceVO nuageVspDevice = mock(NuageVspDeviceVO.class);
        when(nuageVspDevice.getHostId()).thenReturn(NETWORK_ID);
        when(nuageVspDao.listByPhysicalNetwork(NETWORK_ID)).thenReturn(Arrays.asList(new NuageVspDeviceVO[] {nuageVspDevice}));
        when(hostDao.findById(NETWORK_ID)).thenReturn(host);

        when(networkDao.acquireInLockTable(NETWORK_ID, 1200)).thenReturn(network);
        when(ipAddressDao.findByVmIdAndNetworkId(NETWORK_ID, NETWORK_ID)).thenReturn(null);
        when(domainDao.findById(NETWORK_ID)).thenReturn(mock(DomainVO.class));

        final Answer answer = mock(Answer.class);
        when(answer.getResult()).thenReturn(true);
        when(agentManager.easySend(eq(NETWORK_ID), (Command)any())).thenReturn(answer);

        guru.reserve(nicProfile, network, vmProfile, mock(DeployDestination.class), mock(ReservationContext.class));
    }

    @Test
    public void testImplementNetwork() throws URISyntaxException, InsufficientVirtualNetworkCapacityException {
        final NetworkVO network = mock(NetworkVO.class);
        when(network.getId()).thenReturn(NETWORK_ID);
        when(network.getUuid()).thenReturn("aaaaaa");
        when(network.getDataCenterId()).thenReturn(NETWORK_ID);
        when(network.getNetworkOfferingId()).thenReturn(NETWORK_ID);
        when(network.getPhysicalNetworkId()).thenReturn(NETWORK_ID);
        when(network.getDomainId()).thenReturn(NETWORK_ID);
        when(network.getAccountId()).thenReturn(NETWORK_ID);
        when(network.getVpcId()).thenReturn(null);
        when(network.getState()).thenReturn(com.cloud.network.Network.State.Implementing);
        when(network.getTrafficType()).thenReturn(TrafficType.Guest);
        when(network.getMode()).thenReturn(Mode.Static);
        when(network.getBroadcastDomainType()).thenReturn(BroadcastDomainType.Vsp);
        when(network.getBroadcastUri()).thenReturn(new URI("vsp://aaaaaa-aavvv/10.1.1.1"));
        when(network.getGateway()).thenReturn("10.1.1.1");
        when(network.getCidr()).thenReturn("10.1.1.0/24");
        when(network.getName()).thenReturn("iso");

        final NetworkOffering offering = mock(NetworkOffering.class);
        when(offering.getId()).thenReturn(NETWORK_ID);
        when(offering.getTrafficType()).thenReturn(TrafficType.Guest);
        when(offering.getTags()).thenReturn("aaaa");
        when(offering.getEgressDefaultPolicy()).thenReturn(true);

        when(networkModel.findPhysicalNetworkId(NETWORK_ID, "aaa", TrafficType.Guest)).thenReturn(NETWORK_ID);

        final ReservationContext reserveContext = mock(ReservationContext.class);
        final Domain domain = mock(Domain.class);
        when(reserveContext.getDomain()).thenReturn(domain);
        when(domain.getName()).thenReturn("aaaaa");
        final Account account = mock(Account.class);
        when(account.getAccountId()).thenReturn(NETWORK_ID);
        when(reserveContext.getAccount()).thenReturn(account);
        final DomainVO domainVo = mock(DomainVO.class);
        when(domainDao.findById(NETWORK_ID)).thenReturn(domainVo);
        final AccountVO accountVo = mock(AccountVO.class);
        when(accountDao.findById(NETWORK_ID)).thenReturn(accountVo);

        final HostVO host = mock(HostVO.class);
        when(host.getId()).thenReturn(NETWORK_ID);
        final NuageVspDeviceVO nuageVspDevice = mock(NuageVspDeviceVO.class);
        when(nuageVspDevice.getHostId()).thenReturn(NETWORK_ID);
        when(nuageVspDao.listByPhysicalNetwork(NETWORK_ID)).thenReturn(Arrays.asList(new NuageVspDeviceVO[] {nuageVspDevice}));
        when(hostDao.findById(NETWORK_ID)).thenReturn(host);
        when(networkDao.acquireInLockTable(NETWORK_ID, 1200)).thenReturn(network);
        when(nuageVspManager.getDnsDetails(network)).thenReturn(new ArrayList<String>());
        when(nuageVspManager.getGatewaySystemIds()).thenReturn(new ArrayList<String>());

        final Answer answer = mock(Answer.class);
        when(answer.getResult()).thenReturn(true);
        when(agentManager.easySend(eq(NETWORK_ID), (Command)any())).thenReturn(answer);

        final DataCenter dc = mock(DataCenter.class);
        when(dc.getId()).thenReturn(NETWORK_ID);
        final DeployDestination deployDest = mock(DeployDestination.class);
        when(deployDest.getDataCenter()).thenReturn(dc);
        guru.implement(network, offering, deployDest, reserveContext);
    }

    @Test
    public void testDeallocate() throws Exception {
        final NetworkVO network = mock(NetworkVO.class);
        when(network.getId()).thenReturn(NETWORK_ID);
        when(network.getUuid()).thenReturn("aaaaaa");
        when(network.getNetworkOfferingId()).thenReturn(NETWORK_ID);
        when(network.getPhysicalNetworkId()).thenReturn(NETWORK_ID);
        when(network.getVpcId()).thenReturn(null);
        when(network.getDomainId()).thenReturn(NETWORK_ID);
        when(networkDao.acquireInLockTable(NETWORK_ID, 1200)).thenReturn(network);

        final NetworkOfferingVO offering = mock(NetworkOfferingVO.class);
        when(offering.getId()).thenReturn(NETWORK_ID);
        when(offering.getTrafficType()).thenReturn(TrafficType.Guest);
        when(ntwkOfferDao.findById(NETWORK_ID)).thenReturn(offering);

        final DomainVO domain = mock(DomainVO.class);
        when(domain.getUuid()).thenReturn("aaaaaa");
        when(domainDao.findById(NETWORK_ID)).thenReturn(domain);

        final NicVO nic = mock(NicVO.class);
        when(nic.getId()).thenReturn(NETWORK_ID);
        when(nic.getIPv4Address()).thenReturn("10.10.10.10");
        when(nic.getMacAddress()).thenReturn("c8:60:00:56:e5:58");
        when(nicDao.findById(NETWORK_ID)).thenReturn(nic);

        final NicProfile nicProfile = mock(NicProfile.class);
        when(nicProfile.getId()).thenReturn(NETWORK_ID);
        when(nicProfile.getIPv4Address()).thenReturn("10.10.10.10");
        when(nicProfile.getMacAddress()).thenReturn("c8:60:00:56:e5:58");

        final VirtualMachine vm = mock(VirtualMachine.class);
        when(vm.getType()).thenReturn(VirtualMachine.Type.User);
        when(vm.getState()).thenReturn(VirtualMachine.State.Expunging);

        final VirtualMachineProfile vmProfile = mock(VirtualMachineProfile.class);
        when(vmProfile.getUuid()).thenReturn("aaaaaa");
        when(vmProfile.getInstanceName()).thenReturn("Test-VM");
        when(vmProfile.getVirtualMachine()).thenReturn(vm);

        final HostVO host = mock(HostVO.class);
        when(host.getId()).thenReturn(NETWORK_ID);
        final NuageVspDeviceVO nuageVspDevice = mock(NuageVspDeviceVO.class);
        when(nuageVspDevice.getHostId()).thenReturn(NETWORK_ID);
        when(nuageVspDao.listByPhysicalNetwork(NETWORK_ID)).thenReturn(Arrays.asList(new NuageVspDeviceVO[] {nuageVspDevice}));
        when(hostDao.findById(NETWORK_ID)).thenReturn(host);

        final Answer answer = mock(Answer.class);
        when(answer.getResult()).thenReturn(true);
        when(agentManager.easySend(eq(NETWORK_ID), (Command)any())).thenReturn(answer);

        guru.deallocate(network, nicProfile, vmProfile);
    }

    @Test
    public void testTrash() throws Exception {
        final NetworkVO network = mock(NetworkVO.class);
        when(network.getId()).thenReturn(NETWORK_ID);
        when(network.getUuid()).thenReturn("aaaaaa");
        when(network.getName()).thenReturn("trash");
        when(network.getDomainId()).thenReturn(NETWORK_ID);
        when(network.getNetworkOfferingId()).thenReturn(NETWORK_ID);
        when(network.getPhysicalNetworkId()).thenReturn(NETWORK_ID);
        when(network.getVpcId()).thenReturn(null);
        when(networkDao.acquireInLockTable(NETWORK_ID, 1200)).thenReturn(network);

        final NetworkOfferingVO offering = mock(NetworkOfferingVO.class);
        when(offering.getId()).thenReturn(NETWORK_ID);
        when(offering.getTrafficType()).thenReturn(TrafficType.Guest);
        when(ntwkOfferDao.findById(NETWORK_ID)).thenReturn(offering);

        final DomainVO domain = mock(DomainVO.class);
        when(domain.getUuid()).thenReturn("aaaaaa");
        when(domainDao.findById(NETWORK_ID)).thenReturn(domain);

        final HostVO host = mock(HostVO.class);
        when(host.getId()).thenReturn(NETWORK_ID);
        final NuageVspDeviceVO nuageVspDevice = mock(NuageVspDeviceVO.class);
        when(nuageVspDevice.getHostId()).thenReturn(NETWORK_ID);
        when(nuageVspDao.listByPhysicalNetwork(NETWORK_ID)).thenReturn(Arrays.asList(new NuageVspDeviceVO[] {nuageVspDevice}));
        when(hostDao.findById(NETWORK_ID)).thenReturn(host);
        when(nuageVspManager.getDnsDetails(network)).thenReturn(new ArrayList<String>());
        when(nuageVspManager.getGatewaySystemIds()).thenReturn(new ArrayList<String>());

        final Answer answer = mock(Answer.class);
        when(answer.getResult()).thenReturn(true);
        when(agentManager.easySend(eq(NETWORK_ID), (Command)any())).thenReturn(answer);

        assertTrue(guru.trash(network, offering));
    }

}
