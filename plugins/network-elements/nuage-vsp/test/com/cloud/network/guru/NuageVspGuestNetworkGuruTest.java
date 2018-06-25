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

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

import net.nuage.vsp.acs.client.api.model.NetworkRelatedVsdIds;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.internal.util.collections.Sets;
import org.mockito.invocation.InvocationOnMock;

import com.google.common.collect.ImmutableMap;

import com.cloud.NuageTest;
import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.guru.DeallocateVmVspCommand;
import com.cloud.agent.api.guru.ImplementNetworkVspCommand;
import com.cloud.agent.api.guru.ReserveVmInterfaceVspCommand;
import com.cloud.agent.api.manager.ImplementNetworkVspAnswer;
import com.cloud.configuration.ConfigurationManager;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.DataCenterDetailsDao;
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
import com.cloud.network.router.VirtualRouter;
import com.cloud.offering.NetworkOffering;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.offerings.dao.NetworkOfferingServiceMapDao;
import com.cloud.user.Account;
import com.cloud.user.AccountVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.NicProfile;
import com.cloud.vm.NicVO;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.dao.DomainRouterDao;
import com.cloud.vm.dao.NicDao;

public class NuageVspGuestNetworkGuruTest extends NuageTest {
    private static final long DATACENTER_ID = 100L;
    private static final long HOST_ID = 101L;
    private static final long DOMAIN_ID = 1L;
    private static final long ACCOUNT_ID = 2L;
    private static final long OFFERING_ID = 40L;
    private static final long NETWORK_ID = 42L;
    private static final long VM_ID = 242L;
    private static final long NIC_ID = 342L;

    private static final String DATACENTER_UUID = "uuid-datacenter-100";
    private static final String HOST_UUID = "uuid-host-101";
    private static final String DOMAIN_UUID = "uuid-domain-001";
    private static final String ACCOUNT_UUID = "uuid-account-002";
    private static final String OFFERING_UUID = "uuid-offering-040";
    private static final String NETWORK_UUID = "uuid-network-000-42";
    private static final String VM_UUID = "uuid-vm-002-42";
    private static final String NIC_UUID = "uuid-nic-003-42";

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
    @Mock private DataCenterDetailsDao _dcDetailsDao;
    @Mock private NetworkDetailsDao _networkDetailsDao;
    @Mock private PhysicalNetworkVO physnet;
    @Mock private DomainRouterDao _routerDao;

    private Account _account;
    private Domain _domain;
    private DataCenterVO _dc;
    private ReservationContext _reservationContext;

    @InjectMocks
    private NuageVspGuestNetworkGuru _nuageVspGuestNetworkGuru = new NuageVspGuestNetworkGuru();

    @Before
    public void setUp() throws Exception {
        super.setUp();

        _account = getMockAccount();
        _domain = getMockDomain();
        _dc = mockDataCenter();
        _reservationContext = getMockReservationContext(_account, _domain);

        when(_physicalNetworkDao.findById(any(Long.class))).thenReturn(physnet);
        when(physnet.getIsolationMethods()).thenReturn(Collections.singletonList("VSP"));
        when(physnet.getId()).thenReturn(NETWORK_ID);

        final HostVO host = mock(HostVO.class);
        when(host.getId()).thenReturn(HOST_ID);
        when(host.getUuid()).thenReturn(HOST_UUID);
        when(_hostDao.findById(HOST_ID)).thenReturn(host);

        when(_agentManager.easySend(eq(HOST_ID), any(Command.class))).thenReturn(new Answer(null));
        when(_agentManager.easySend(eq(HOST_ID), any(ImplementNetworkVspCommand.class))).thenAnswer(this::mockImplement);
        when(_nuageVspManager.getNuageVspHost(NETWORK_ID)).thenReturn(host);

        final NuageVspDeviceVO device = mock(NuageVspDeviceVO.class);
        when(_nuageVspDao.listByPhysicalNetwork(NETWORK_ID)).thenReturn(Collections.singletonList(device));
        when(device.getId()).thenReturn(1L);
        when(device.getHostId()).thenReturn(HOST_ID);
    }

    Answer mockImplement(InvocationOnMock invocation) {
        if (invocation.getArguments()[1] instanceof ImplementNetworkVspCommand) {
            ImplementNetworkVspCommand command = (ImplementNetworkVspCommand)(invocation.getArguments()[1]);
            return new ImplementNetworkVspAnswer(command, command.getNetwork(), new NetworkRelatedVsdIds.Builder().build());
        } else {
            return new Answer(null);
        }
    }

    @Test
    public void testCanHandle() {
        final NetworkOffering offering = mockNetworkOffering(false);

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

        // Not supported: IsolationMethod != VSP
        when(physnet.getIsolationMethods()).thenReturn(Collections.singletonList("VLAN"));
        assertThat(_nuageVspGuestNetworkGuru.canHandle(offering, NetworkType.Basic, physnet), is(false));

        // Not supported: Non-persistent VPC tier
        when(_configurationManager.isOfferingForVpc(any(NetworkOffering.class))).thenReturn(true);
        assertFalse(_nuageVspGuestNetworkGuru.canHandle(offering, NetworkType.Advanced, physnet));
    }

    @Test
    public void testDesign() {
        final NetworkOffering offering = mockNetworkOffering(false);
        when(offering.getIsPersistent()).thenReturn(false);

        final DeploymentPlan plan = mockDeploymentPlan();
        final Network network = mock(Network.class);

        final Network designednetwork = _nuageVspGuestNetworkGuru.design(offering, plan, network, _account);
        assertThat(designednetwork, notNullValue(Network.class));
        assertThat(designednetwork.getBroadcastDomainType(), is(BroadcastDomainType.Vsp));

        // Can't design non-persistent VPC tier
        when(_configurationManager.isOfferingForVpc(any(NetworkOffering.class))).thenReturn(true);
        assertThat(_nuageVspGuestNetworkGuru.design(offering, plan, network, _account), nullValue(Network.class));
    }

    @Test
    public void testDesignNoIsolationMethodVSP() {
        when(physnet.getIsolationMethods()).thenReturn(Collections.singletonList("VLAN"));

        final NetworkOffering offering = mockNetworkOffering(false);

        final DeploymentPlan plan = mockDeploymentPlan();
        final Network network = mock(Network.class);

        assertThat(_nuageVspGuestNetworkGuru.design(offering, plan, network, _account), nullValue(Network.class));
    }

    @Test
    public void testReserve() throws InsufficientVirtualNetworkCapacityException, InsufficientAddressCapacityException, URISyntaxException {
        final NetworkVO network = mockNetwork();
        final NicProfile nicProfile = mockNicProfile();
        final VirtualMachineProfile vmProfile = mockVirtualMachineProfile(VirtualMachine.State.Starting);

        when(_networkDao.findById(NETWORK_ID)).thenReturn(network);

        _nuageVspGuestNetworkGuru.reserve(nicProfile, network, vmProfile, mock(DeployDestination.class), _reservationContext);

        verify(_agentManager).easySend(anyLong(), any(Command.class));
    }

    @Test
    public void testReserveVRRollingRestart() throws InsufficientVirtualNetworkCapacityException, InsufficientAddressCapacityException, URISyntaxException {
        final NetworkVO network = mockNetwork();
        final NicProfile nicProfile = mockNicProfile();
        final VirtualMachineProfile vmProfile = mockVRProfile(VirtualMachine.State.Starting);

        when(_networkDao.findById(NETWORK_ID)).thenReturn(network);

        _nuageVspGuestNetworkGuru.reserve(nicProfile, network, vmProfile, mock(DeployDestination.class), _reservationContext);

        verifyZeroInteractions(_agentManager);
        verify(network).setRollingRestart(true);
    }

    @Test
    public void testImplementNetwork() throws URISyntaxException, InsufficientVirtualNetworkCapacityException {
        final NetworkVO network = mockNetwork();

        when(network.getState()).thenReturn(com.cloud.network.Network.State.Implementing);

        final NetworkOffering offering = mockNetworkOffering(false);

        final DeployDestination deployDest = mock(DeployDestination.class);
        when(deployDest.getDataCenter()).thenReturn(_dc);
        _nuageVspGuestNetworkGuru.implement(network, offering, deployDest, _reservationContext);
    }

    @Test
    public void testDeallocate() throws Exception {
        final NetworkVO network = mockNetwork();
        final NicProfile nicProfile = mockNicProfile();
        final VirtualMachineProfile vmProfile = mockVirtualMachineProfile(VirtualMachine.State.Expunging);

        _nuageVspGuestNetworkGuru.deallocate(network, nicProfile, vmProfile);
    }

    @Test
    public void testDeallocateVR() throws InsufficientVirtualNetworkCapacityException, InsufficientAddressCapacityException, URISyntaxException {
        final NetworkVO network = mockNetwork();
        final NicProfile nicProfile = mockNicProfile();
        final VirtualMachineProfile vmProfile = mockVRProfile(VirtualMachine.State.Expunging);

        when(_networkDao.findById(NETWORK_ID)).thenReturn(network);

        _nuageVspGuestNetworkGuru.deallocate(network, nicProfile, vmProfile);
    }

    @Test
    public void testDeallocateVRRollingRestart() throws InsufficientVirtualNetworkCapacityException, InsufficientAddressCapacityException, URISyntaxException {
        final NetworkVO network = mockNetwork();
        final NicProfile nicProfile = mockNicProfile();
        final VirtualMachineProfile vmProfile = mockVRProfile(VirtualMachine.State.Expunging);

        DomainRouterVO newVR = mock(DomainRouterVO.class);

        when(_routerDao.listByNetworkAndRole(NETWORK_ID, VirtualRouter.Role.VIRTUAL_ROUTER)).thenReturn(Collections.singletonList(newVR));
        when(_networkDao.findById(NETWORK_ID)).thenReturn(network);

        _nuageVspGuestNetworkGuru.deallocate(network, nicProfile, vmProfile);

        ArgumentCaptor<Command> argumentCaptor = ArgumentCaptor.forClass(Command.class);
        verify(_agentManager, times(2)).easySend(eq(HOST_ID), argumentCaptor.capture());
        final List<Command> commands = argumentCaptor.getAllValues();
        assertThat(commands.get(0) instanceof DeallocateVmVspCommand, is(true));
        assertThat(commands.get(1) instanceof ReserveVmInterfaceVspCommand, is(true));
    }

    @Test
    public void testTrash() throws Exception {
        final NetworkVO network = mock(NetworkVO.class);
        when(network.getId()).thenReturn(NETWORK_ID);
        when(network.getUuid()).thenReturn(NETWORK_UUID);
        when(network.getName()).thenReturn("trash");
        when(network.getDomainId()).thenReturn(DOMAIN_ID);
        when(network.getNetworkOfferingId()).thenReturn(OFFERING_ID);
        when(network.getPhysicalNetworkId()).thenReturn(NETWORK_ID);
        when(network.getDataCenterId()).thenReturn(DATACENTER_ID);
        when(network.getVpcId()).thenReturn(null);
        when(_networkDao.acquireInLockTable(NETWORK_ID, 1200)).thenReturn(network);

        final NetworkOffering offering = mockNetworkOffering(false);

        when(_nuageVspManager.getDnsDetails(network.getDataCenterId())).thenReturn(new ArrayList<>());
        when(_nuageVspManager.getGatewaySystemIds()).thenReturn(new ArrayList<>());

        assertTrue(_nuageVspGuestNetworkGuru.trash(network, offering));
    }

    @Nonnull
    private NetworkVO mockNetwork() throws URISyntaxException {
        final NetworkVO network = mock(NetworkVO.class);
        when(network.getId()).thenReturn(NETWORK_ID);
        when(network.getUuid()).thenReturn(NETWORK_UUID);
        when(network.getDataCenterId()).thenReturn(DATACENTER_ID);
        when(network.getNetworkOfferingId()).thenReturn(OFFERING_ID);
        when(network.getPhysicalNetworkId()).thenReturn(NETWORK_ID);
        when(network.getDomainId()).thenReturn(DOMAIN_ID);
        when(network.getAccountId()).thenReturn(ACCOUNT_ID);
        when(network.getVpcId()).thenReturn(null);
        when(network.getTrafficType()).thenReturn(TrafficType.Guest);
        when(network.getMode()).thenReturn(Mode.Dhcp);
        when(network.getBroadcastDomainType()).thenReturn(BroadcastDomainType.Vsp);
        when(network.getBroadcastUri()).thenReturn(new URI("vsp://" + NETWORK_UUID + "/10.1.1.1"));
        when(network.getGateway()).thenReturn("10.1.1.1");
        when(network.getCidr()).thenReturn("10.1.1.0/24");
        when(network.getName()).thenReturn("iso");

        when(_networkDao.acquireInLockTable(NETWORK_ID, 1200)).thenReturn(network);
        when(_networkDao.findById(NETWORK_ID)).thenReturn(network);

        return network;
    }

    @Nonnull
    private NetworkOffering mockNetworkOffering(boolean forVpc) {
        final NetworkOfferingVO offering = mock(NetworkOfferingVO.class);
        when(offering.getId()).thenReturn(OFFERING_ID);
        when(offering.getUuid()).thenReturn(OFFERING_UUID);
        when(offering.getTrafficType()).thenReturn(TrafficType.Guest);
        when(offering.getGuestType()).thenReturn(GuestType.Isolated);
        when(offering.getForVpc()).thenReturn(forVpc);
        when(offering.getIsPersistent()).thenReturn(false);
        when(offering.getTags()).thenReturn("aaaa");
        when(offering.getEgressDefaultPolicy()).thenReturn(true);

        when(_networkOfferingDao.findById(OFFERING_ID)).thenReturn(offering);

        when(_configurationManager.isOfferingForVpc(offering)).thenReturn(forVpc);

        when(_networkOfferingServiceMapDao.canProviderSupportServiceInNetworkOffering(OFFERING_ID, Service.Connectivity, Network.Provider.NuageVsp)).thenReturn(true);
        when(_networkOfferingServiceMapDao.canProviderSupportServiceInNetworkOffering(OFFERING_ID, Service.SourceNat, Network.Provider.NuageVsp)).thenReturn(true);

        when(_networkModel.getNetworkOfferingServiceProvidersMap(OFFERING_ID)).thenReturn(ImmutableMap.of(
                Service.Connectivity, Sets.newSet(Network.Provider.NuageVsp),
                Service.SourceNat, Sets.newSet(Network.Provider.NuageVsp)
        ));

        return offering;
    }

    private DeploymentPlan mockDeploymentPlan() {
        final DeploymentPlan deploymentPlan = mock(DeploymentPlan.class);
        when(deploymentPlan.getDataCenterId()).thenReturn(DATACENTER_ID);
        return deploymentPlan;
    }

    private DataCenterVO mockDataCenter() {
        DataCenterVO dc = mock(DataCenterVO.class);
        when(dc.getId()).thenReturn(DATACENTER_ID);
        when(dc.getUuid()).thenReturn(DATACENTER_UUID);
        when(dc.getNetworkType()).thenReturn(NetworkType.Advanced);
        when(dc.getGuestNetworkCidr()).thenReturn("10.1.1.1/24");
        when(_dataCenterDao.findById(DATACENTER_ID)).thenReturn(dc);

        return dc;
    }

    @Nonnull
    private Account getMockAccount() {
        final AccountVO account = mock(AccountVO.class);
        when(account.getId()).thenReturn(ACCOUNT_ID);
        when(account.getAccountId()).thenReturn(ACCOUNT_ID);
        when(account.getUuid()).thenReturn(ACCOUNT_UUID);
        when(account.getDomainId()).thenReturn(DOMAIN_ID);
        when(account.getType()).thenReturn(Account.ACCOUNT_TYPE_NORMAL);

        when(_accountDao.findById(ACCOUNT_ID)).thenReturn(account);

        return account;
    }

    @Nonnull
    private Domain getMockDomain() {
        final DomainVO domain = mock(DomainVO.class);
        when(domain.getId()).thenReturn(DOMAIN_ID);
        when(domain.getUuid()).thenReturn(DOMAIN_UUID);
        when(domain.getName()).thenReturn("aaaaa");

        when(_domainDao.findById(DOMAIN_ID)).thenReturn(domain);

        return domain;
    }

    @Nonnull
    private VirtualMachineProfile mockVirtualMachineProfile(VirtualMachine.State state) {
        final VirtualMachine vm = mock(VirtualMachine.class);
        when(vm.getId()).thenReturn(VM_ID);
        when(vm.getType()).thenReturn(VirtualMachine.Type.User);
        when(vm.getState()).thenReturn(state);

        final VirtualMachineProfile vmProfile = mock(VirtualMachineProfile.class);
        when(vmProfile.getType()).thenReturn(VirtualMachine.Type.User);
        when(vmProfile.getInstanceName()).thenReturn("Test-VM");
        when(vmProfile.getUuid()).thenReturn(VM_UUID);
        when(vmProfile.getVirtualMachine()).thenReturn(vm);
        return vmProfile;
    }

    @Nonnull
    private VirtualMachineProfile mockVRProfile(VirtualMachine.State state) {
        final VirtualMachine vm = mock(VirtualMachine.class);
        when(vm.getId()).thenReturn(VM_ID);
        when(vm.getUuid()).thenReturn(VM_UUID);
        when(vm.getType()).thenReturn(VirtualMachine.Type.DomainRouter);
        when(vm.getState()).thenReturn(state);


        final VirtualMachineProfile vmProfile = mock(VirtualMachineProfile.class);
        when(vmProfile.getType()).thenReturn(VirtualMachine.Type.DomainRouter);
        when(vmProfile.getInstanceName()).thenReturn("Test-VR");
        when(vmProfile.getId()).thenReturn(VM_ID);
        when(vmProfile.getUuid()).thenReturn(VM_UUID);
        when(vmProfile.getVirtualMachine()).thenReturn(vm);
        when(vmProfile.isRollingRestart()).thenReturn(true);
        return vmProfile;
    }

    @Nonnull
    private NicProfile mockNicProfile() {
        final NicVO nicvo = mock(NicVO.class);
        when(nicvo.getId()).thenReturn(NIC_ID);
        when(nicvo.getMacAddress()).thenReturn("c8:60:00:56:e5:58");
        when(nicvo.getIPv4Address()).thenReturn("10.10.10.10");
        when(nicvo.getUuid()).thenReturn("aaaa-fffff");
        when(nicvo.getNetworkId()).thenReturn(NETWORK_ID);
        when(nicvo.getInstanceId()).thenReturn(VM_ID);
        when(_nicDao.findById(NIC_ID)).thenReturn(nicvo);
        when(_nicDao.findDefaultNicForVM(VM_ID)).thenReturn(nicvo);

        NicProfile nicProfile = mock(NicProfile.class);
        when(nicProfile.getUuid()).thenReturn("aaa-bbbb");
        when(nicProfile.getId()).thenReturn(NIC_ID);
        when(nicProfile.getMacAddress()).thenReturn("c8:60:00:56:e5:58");
        when(nicProfile.getIPv4Address()).thenReturn("10.10.10.10");
        return nicProfile;
    }

    @Nonnull
    private static ReservationContext getMockReservationContext(Account networksAccount, Domain networksDomain) {
        final ReservationContext reservationContext = mock(ReservationContext.class);
        when(reservationContext.getAccount()).thenReturn(networksAccount);
        when(reservationContext.getDomain()).thenReturn(networksDomain);
        return reservationContext;
    }

}
