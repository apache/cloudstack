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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import net.nuage.vsp.acs.client.api.model.NetworkRelatedVsdIds;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.internal.util.collections.Sets;

import com.google.common.collect.ImmutableMap;

import com.cloud.NuageTest;
import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.guru.ImplementNetworkVspCommand;
import com.cloud.agent.api.manager.ImplementNetworkVspAnswer;
import com.cloud.configuration.ConfigurationManager;
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
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.Networks.Mode;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.NuageVspDeviceVO;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkDetailsDao;
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

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NuageVspGuestNetworkGuruTest extends NuageTest {
    private static final long NETWORK_ID = 42L;
    @Mock private PhysicalNetworkDao _physicalNetworkDao;
    @Mock private DataCenterDao _dataCenterDao;
    @Mock private NetworkOfferingServiceMapDao _networkOfferingServiceMapDao;
    @Mock private AgentManager _agentManager;
    @Mock private AccountDao _accountDao;
    @Mock private DomainDao _domainDao;
    @Mock private NicDao _nicDao;
    @Mock private NetworkOfferingDao _networkOfferingDao;
    @Mock private NuageVspDao _nuageVspDao;
    @Mock private HostDao _hostDao;
    @Mock private NetworkDao _networkDao;
    @Mock private IPAddressDao _ipAddressDao;
    @Mock private NuageVspManager _nuageVspManager;
    @Mock private ConfigurationManager _configurationManager;
    @Mock private NetworkDetailsDao _networkDetailsDao;
    @Mock private PhysicalNetworkVO physnet;

    @InjectMocks
    private NuageVspGuestNetworkGuru _nuageVspGuestNetworkGuru;

    @Before
    public void setUp() throws Exception {
        _nuageVspGuestNetworkGuru = new NuageVspGuestNetworkGuru();

        super.setUp();

        _nuageVspGuestNetworkGuru._nuageVspEntityBuilder = _nuageVspEntityBuilder;

        final DataCenterVO dc = mock(DataCenterVO.class);
        when(dc.getNetworkType()).thenReturn(NetworkType.Advanced);
        when(dc.getGuestNetworkCidr()).thenReturn("10.1.1.1/24");

        when(_dataCenterDao.findById((Long)any())).thenReturn(dc);

        when(_physicalNetworkDao.findById(any(Long.class))).thenReturn(physnet);
        when(physnet.getIsolationMethods()).thenReturn(Arrays.asList("VSP"));
        when(physnet.getId()).thenReturn(NETWORK_ID);

        final HostVO host = mock(HostVO.class);
        when(_hostDao.findById(NETWORK_ID)).thenReturn(host);
        when(host.getId()).thenReturn(NETWORK_ID);
        when(_agentManager.easySend(eq(NETWORK_ID), any(Command.class))).thenReturn(new Answer(null));
        when(_agentManager.easySend(eq(NETWORK_ID), any(ImplementNetworkVspCommand.class))).thenReturn(new ImplementNetworkVspAnswer(null, new NetworkRelatedVsdIds.Builder().build()));
        when(_nuageVspManager.getNuageVspHost(NETWORK_ID)).thenReturn(host);

        final NuageVspDeviceVO device = mock(NuageVspDeviceVO.class);
        when(_nuageVspDao.listByPhysicalNetwork(NETWORK_ID)).thenReturn(Arrays.asList(device));
        when(device.getId()).thenReturn(1L);
        when(device.getHostId()).thenReturn(NETWORK_ID);
    }

    @Test
    public void testCanHandle() {
        final NetworkOffering offering = mock(NetworkOffering.class);
        when(offering.getId()).thenReturn(NETWORK_ID);
        when(offering.getIsPersistent()).thenReturn(false);
        when(_configurationManager.isOfferingForVpc(any(NetworkOffering.class))).thenReturn(false);

        when(_networkOfferingServiceMapDao.canProviderSupportServiceInNetworkOffering(NETWORK_ID, Service.Connectivity, Network.Provider.NuageVsp)).thenReturn(true);
        when(_networkOfferingServiceMapDao.canProviderSupportServiceInNetworkOffering(NETWORK_ID, Service.SourceNat, Network.Provider.NuageVsp)).thenReturn(true);

        when(_networkModel.getNetworkOfferingServiceProvidersMap(NETWORK_ID)).thenReturn(ImmutableMap.of(
                Service.Connectivity, Sets.newSet(Network.Provider.NuageVsp),
                Service.SourceNat, Sets.newSet(Network.Provider.NuageVsp)
        ));

        when(offering.getTrafficType()).thenReturn(TrafficType.Guest);
        when(offering.getGuestType()).thenReturn(GuestType.Isolated);
        assertThat(_nuageVspGuestNetworkGuru.canHandle(offering, NetworkType.Advanced, physnet), is(true));

        // Not supported TrafficType != Guest
        when(offering.getTrafficType()).thenReturn(TrafficType.Management);
        assertThat(_nuageVspGuestNetworkGuru.canHandle(offering, NetworkType.Advanced, physnet), is(false));

        // Supported: GuestType Shared
        when(offering.getTrafficType()).thenReturn(TrafficType.Guest);
        when(offering.getGuestType()).thenReturn(GuestType.Shared);
        assertThat(_nuageVspGuestNetworkGuru.canHandle(offering, NetworkType.Advanced, physnet), is(true));

        // Not supported: Basic networking
        when(offering.getGuestType()).thenReturn(GuestType.Isolated);
        assertThat(_nuageVspGuestNetworkGuru.canHandle(offering, NetworkType.Basic, physnet), is(false));

        // Not supported: IsolationMethod != STT
        when(physnet.getIsolationMethods()).thenReturn(Arrays.asList("VLAN"));
        assertThat(_nuageVspGuestNetworkGuru.canHandle(offering, NetworkType.Basic, physnet), is(false));

        // Not supported: Non-persistent VPC tier
        when(_configurationManager.isOfferingForVpc(any(NetworkOffering.class))).thenReturn(true);
        assertFalse(_nuageVspGuestNetworkGuru.canHandle(offering, NetworkType.Advanced, physnet));
    }

    @Test
    public void testDesign() {
        final NuageVspDeviceVO device = mock(NuageVspDeviceVO.class);
        when(_nuageVspDao.listByPhysicalNetwork(NETWORK_ID)).thenReturn(Arrays.asList(device));
        when(device.getId()).thenReturn(1L);

        final NetworkOffering offering = mock(NetworkOffering.class);
        when(offering.getId()).thenReturn(NETWORK_ID);
        when(offering.getTrafficType()).thenReturn(TrafficType.Guest);
        when(offering.getGuestType()).thenReturn(GuestType.Isolated);
        when(offering.getIsPersistent()).thenReturn(false);
        when(_configurationManager.isOfferingForVpc(any(NetworkOffering.class))).thenReturn(false);

        when(_networkOfferingServiceMapDao.canProviderSupportServiceInNetworkOffering(NETWORK_ID, Service.Connectivity, Network.Provider.NuageVsp)).thenReturn(true);
        when(_networkOfferingServiceMapDao.canProviderSupportServiceInNetworkOffering(NETWORK_ID, Service.SourceNat, Network.Provider.NuageVsp)).thenReturn(true);

        when(_networkModel.getNetworkOfferingServiceProvidersMap(NETWORK_ID)).thenReturn(ImmutableMap.of(
                Service.Connectivity, Sets.newSet(Network.Provider.NuageVsp),
                Service.SourceNat, Sets.newSet(Network.Provider.NuageVsp)
        ));

        final DeploymentPlan plan = mock(DeploymentPlan.class);
        final Network network = mock(Network.class);
        final Account account = mock(Account.class);

        final Network designednetwork = _nuageVspGuestNetworkGuru.design(offering, plan, network, account);
        assertThat(designednetwork, notNullValue(Network.class));
        assertThat(designednetwork.getBroadcastDomainType(), is(BroadcastDomainType.Vsp));

        // Can't design non-persistent VPC tier
        when(_configurationManager.isOfferingForVpc(any(NetworkOffering.class))).thenReturn(true);
        assertThat(_nuageVspGuestNetworkGuru.design(offering, plan, network, account), nullValue(Network.class));
    }

    @Test
    public void testDesignNoElementOnPhysicalNetwork() {
        when(physnet.getIsolationMethods()).thenReturn(Arrays.asList("STT"));
        when(_nuageVspDao.listByPhysicalNetwork(NETWORK_ID)).thenReturn(Collections.<NuageVspDeviceVO>emptyList());

        final NetworkOffering offering = mock(NetworkOffering.class);
        when(offering.getId()).thenReturn(NETWORK_ID);
        when(offering.getTrafficType()).thenReturn(TrafficType.Guest);
        when(offering.getGuestType()).thenReturn(GuestType.Isolated);

        final DeploymentPlan plan = mock(DeploymentPlan.class);
        final Network network = mock(Network.class);
        final Account account = mock(Account.class);

        final Network designednetwork = _nuageVspGuestNetworkGuru.design(offering, plan, network, account);
        assertTrue(designednetwork == null);
    }

    @Test
    public void testDesignNoIsolationMethodVSP() {
        when(physnet.getIsolationMethods()).thenReturn(Arrays.asList("VLAN"));
        when(_nuageVspDao.listByPhysicalNetwork(NETWORK_ID)).thenReturn(Collections.<NuageVspDeviceVO>emptyList());

        final NetworkOffering offering = mock(NetworkOffering.class);
        when(offering.getId()).thenReturn(NETWORK_ID);
        when(offering.getTrafficType()).thenReturn(TrafficType.Guest);
        when(offering.getGuestType()).thenReturn(GuestType.Isolated);

        final DeploymentPlan plan = mock(DeploymentPlan.class);
        final Network network = mock(Network.class);
        final Account account = mock(Account.class);

        final Network designednetwork = _nuageVspGuestNetworkGuru.design(offering, plan, network, account);
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
        when(_dataCenterDao.findById(NETWORK_ID)).thenReturn(dataCenter);
        final AccountVO networksAccount = mock(AccountVO.class);
        when(networksAccount.getId()).thenReturn(NETWORK_ID);
        when(networksAccount.getUuid()).thenReturn("aaaa-abbbb");
        when(networksAccount.getType()).thenReturn(Account.ACCOUNT_TYPE_NORMAL);
        when(_accountDao.findById(NETWORK_ID)).thenReturn(networksAccount);
        final DomainVO networksDomain = mock(DomainVO.class);
        when(networksDomain.getId()).thenReturn(NETWORK_ID);
        when(networksDomain.getUuid()).thenReturn("aaaaa-bbbbb");
        when(_domainDao.findById(NETWORK_ID)).thenReturn(networksDomain);

        final NicVO nicvo = mock(NicVO.class);
        when(nicvo.getId()).thenReturn(NETWORK_ID);
        when(nicvo.getMacAddress()).thenReturn("aa-aa-aa-aa-aa-aa");
        when(nicvo.getUuid()).thenReturn("aaaa-fffff");
        when(nicvo.getNetworkId()).thenReturn(NETWORK_ID);
        when(nicvo.getInstanceId()).thenReturn(NETWORK_ID);
        when(_nicDao.findById(NETWORK_ID)).thenReturn(nicvo);
        when(_nicDao.findDefaultNicForVM(NETWORK_ID)).thenReturn(nicvo);

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
        when(_networkOfferingDao.findById(NETWORK_ID)).thenReturn(ntwkoffering);

        when(_networkDao.acquireInLockTable(NETWORK_ID, 1200)).thenReturn(network);
        when(_ipAddressDao.findByVmIdAndNetworkId(NETWORK_ID, NETWORK_ID)).thenReturn(null);
        when(_domainDao.findById(NETWORK_ID)).thenReturn(mock(DomainVO.class));

        final Answer answer = mock(Answer.class);
        when(answer.getResult()).thenReturn(true);
        when(_agentManager.easySend(eq(NETWORK_ID), (Command)any())).thenReturn(answer);

        final ReservationContext reservationContext = mock(ReservationContext.class);
        when(reservationContext.getAccount()).thenReturn(networksAccount);
        when(reservationContext.getDomain()).thenReturn(networksDomain);

        _nuageVspGuestNetworkGuru.reserve(nicProfile, network, vmProfile, mock(DeployDestination.class), reservationContext);
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

        when(_networkModel.findPhysicalNetworkId(NETWORK_ID, "aaa", TrafficType.Guest)).thenReturn(NETWORK_ID);

        final ReservationContext reserveContext = mock(ReservationContext.class);
        final Domain domain = mock(Domain.class);
        when(domain.getId()).thenReturn(NETWORK_ID);
        when(reserveContext.getDomain()).thenReturn(domain);
        when(domain.getName()).thenReturn("aaaaa");
        final Account account = mock(Account.class);
        when(account.getId()).thenReturn(NETWORK_ID);
        when(account.getAccountId()).thenReturn(NETWORK_ID);
        when(reserveContext.getAccount()).thenReturn(account);
        final DomainVO domainVo = mock(DomainVO.class);
        when(_domainDao.findById(NETWORK_ID)).thenReturn(domainVo);
        final AccountVO accountVo = mock(AccountVO.class);
        when(_accountDao.findById(NETWORK_ID)).thenReturn(accountVo);

        when(_networkDao.acquireInLockTable(NETWORK_ID, 1200)).thenReturn(network);
        when(_nuageVspManager.getDnsDetails(network.getDataCenterId())).thenReturn(new ArrayList<String>());
        when(_nuageVspManager.getGatewaySystemIds()).thenReturn(new ArrayList<String>());

        final DataCenter dc = mock(DataCenter.class);
        when(dc.getId()).thenReturn(NETWORK_ID);
        final DeployDestination deployDest = mock(DeployDestination.class);
        when(deployDest.getDataCenter()).thenReturn(dc);
        _nuageVspGuestNetworkGuru.implement(network, offering, deployDest, reserveContext);
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
        when(_networkDao.acquireInLockTable(NETWORK_ID, 1200)).thenReturn(network);

        final NetworkOfferingVO offering = mock(NetworkOfferingVO.class);
        when(offering.getId()).thenReturn(NETWORK_ID);
        when(offering.getTrafficType()).thenReturn(TrafficType.Guest);
        when(_networkOfferingDao.findById(NETWORK_ID)).thenReturn(offering);

        final DomainVO domain = mock(DomainVO.class);
        when(domain.getUuid()).thenReturn("aaaaaa");
        when(_domainDao.findById(NETWORK_ID)).thenReturn(domain);

        final NicVO nic = mock(NicVO.class);
        when(nic.getId()).thenReturn(NETWORK_ID);
        when(nic.getIPv4Address()).thenReturn("10.10.10.10");
        when(nic.getMacAddress()).thenReturn("c8:60:00:56:e5:58");
        when(_nicDao.findById(NETWORK_ID)).thenReturn(nic);

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

        _nuageVspGuestNetworkGuru.deallocate(network, nicProfile, vmProfile);
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
        when(network.getDataCenterId()).thenReturn(NETWORK_ID);
        when(network.getVpcId()).thenReturn(null);
        when(_networkDao.acquireInLockTable(NETWORK_ID, 1200)).thenReturn(network);

        final NetworkOfferingVO offering = mock(NetworkOfferingVO.class);
        when(offering.getId()).thenReturn(NETWORK_ID);
        when(offering.getTrafficType()).thenReturn(TrafficType.Guest);
        when(_networkOfferingDao.findById(NETWORK_ID)).thenReturn(offering);

        final DomainVO domain = mock(DomainVO.class);
        when(domain.getUuid()).thenReturn("aaaaaa");
        when(_domainDao.findById(NETWORK_ID)).thenReturn(domain);

        final HostVO host = mock(HostVO.class);
        when(host.getId()).thenReturn(NETWORK_ID);
        final NuageVspDeviceVO nuageVspDevice = mock(NuageVspDeviceVO.class);
        when(nuageVspDevice.getHostId()).thenReturn(NETWORK_ID);
        when(_nuageVspDao.listByPhysicalNetwork(NETWORK_ID)).thenReturn(Arrays.asList(new NuageVspDeviceVO[]{nuageVspDevice}));
        when(_hostDao.findById(NETWORK_ID)).thenReturn(host);
        when(_nuageVspManager.getDnsDetails(network.getDataCenterId())).thenReturn(new ArrayList<String>());
        when(_nuageVspManager.getGatewaySystemIds()).thenReturn(new ArrayList<String>());

        assertTrue(_nuageVspGuestNetworkGuru.trash(network, offering));
    }

}
