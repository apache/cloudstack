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
package com.cloud.network;

import com.cloud.agent.api.to.IpAddressTO;
import com.cloud.alert.AlertManager;
import com.cloud.configuration.ConfigurationManager;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.dao.PhysicalNetworkVO;
import com.cloud.network.router.CommandSetupHelper;
import com.cloud.network.router.NetworkHelper;
import com.cloud.network.vpc.VpcManager;
import com.cloud.network.vpc.VpcVO;
import com.cloud.network.vpc.dao.VpcDao;
import com.cloud.offering.NetworkOffering;
import com.cloud.offering.ServiceOffering;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.offerings.dao.NetworkOfferingServiceMapDao;
import com.cloud.org.Grouping;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountService;
import com.cloud.user.AccountVO;
import com.cloud.user.User;
import com.cloud.user.UserVO;
import com.cloud.user.dao.UserDao;
import com.cloud.utils.Pair;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.NicVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.DomainRouterDao;
import com.cloud.vm.dao.NicDao;
import org.apache.cloudstack.alert.AlertService;
import org.apache.cloudstack.api.command.user.network.CreateNetworkCmd;
import org.apache.cloudstack.api.command.user.network.UpdateNetworkCmd;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class NetworkServiceImplTest {
    @Mock
    AccountManager accountManager;
    @Mock
    NetworkOfferingDao networkOfferingDao;
    @Mock
    PhysicalNetworkDao physicalNetworkDao;
    @Mock
    DataCenterDao dataCenterDao;
    @Mock
    NetworkOrchestrationService networkOrchestrationService;
    @Mock
    Ipv6Service ipv6Service;
    @Mock
    NetworkModel networkModel;
    @Mock
    NetworkOfferingServiceMapDao networkOfferingServiceMapDao;
    @Mock
    EntityManager entityMgr;
    @Mock
    NetworkService networkService;
    @Mock
    VpcManager vpcMgr;
    @Mock
    NetworkOrchestrationService networkManager;
    @Mock
    AlertManager alertManager;
    @Mock
    DataCenterDao dcDao;
    @Mock
    UserDao userDao;
    @Mock
    NetworkDao networkDao;
    @Mock
    NicDao nicDao;
    @Mock
    IPAddressDao ipAddressDao;
    @Mock
    ConfigurationManager configMgr;
    @Mock
    ConfigKey<Integer> publicMtuKey;
    @Mock
    ConfigKey<Boolean> userChangeMtuKey;
    @Mock
    VpcDao vpcDao;
    @Mock
    DomainRouterDao routerDao;
    @Mock
    AccountService accountService;
    @Mock
    NetworkHelper networkHelper;
    @Mock
    ServiceOfferingDao serviceOfferingDaoMock;
    @Mock
    ServiceOfferingVO serviceOfferingVoMock;

    @Mock
    IpAddressManager ipAddressManager;
    @Mock
    private CallContext callContextMock;
    @InjectMocks
    CreateNetworkCmd createNetworkCmd = new CreateNetworkCmd();

    @InjectMocks
    UpdateNetworkCmd updateNetworkCmd = new UpdateNetworkCmd();
    @Mock
    CommandSetupHelper commandSetupHelper;
    @Mock
    private Account accountMock;
    @InjectMocks
    NetworkServiceImpl service = new NetworkServiceImpl();

    private static final String VLAN_ID_900 = "900";
    private static final String VLAN_ID_901 = "901";
    private static final String VLAN_ID_902 = "902";
    public static final long ACCOUNT_ID = 1;

    private static final String IP4_GATEWAY = "10.0.16.1";
    private static final String IP4_NETMASK = "255.255.255.0";
    private static final String IP6_GATEWAY = "fd17:ac56:1234:2000::1";
    private static final String IP6_CIDR = "fd17:ac56:1234:2000::/64";
    final String[] ip4Dns = {"5.5.5.5", "6.6.6.6"};
    final String[] ip6Dns = {"2001:4860:4860::5555", "2001:4860:4860::6666"};

    private AccountVO account;
    private UserVO user;
    private NetworkOfferingVO offering;
    private DataCenterVO dc;
    private Network network;
    private  PhysicalNetworkVO phyNet;
    private VpcVO vpc;

    private MockedStatic<CallContext> callContextMocked;

    private AutoCloseable closeable;

    private void registerCallContext() {
        account = new AccountVO("testaccount", 1L, "networkdomain", Account.Type.NORMAL, "uuid");
        account.setId(ACCOUNT_ID);
        user = new UserVO(1, "testuser", "password", "firstname", "lastName", "email", "timezone",
                UUID.randomUUID().toString(), User.Source.UNKNOWN);
        CallContext.register(user, account);
    }

    Class<InvalidParameterValueException> expectedException = InvalidParameterValueException.class;

    @Before
    public void setup() throws Exception {
        closeable = MockitoAnnotations.openMocks(this);
        overrideDefaultConfigValue(NetworkService.AllowUsersToSpecifyVRMtu, "_defaultValue", "true");
        offering = Mockito.mock(NetworkOfferingVO.class);
        network = Mockito.mock(Network.class);
        dc = Mockito.mock(DataCenterVO.class);
        phyNet = Mockito.mock(PhysicalNetworkVO.class);
        vpc = Mockito.mock(VpcVO.class);
        service._networkOfferingDao = networkOfferingDao;
        service._physicalNetworkDao = physicalNetworkDao;
        service._dcDao = dcDao;
        service._accountMgr = accountManager;
        service._networkMgr = networkManager;
        service.alertManager = alertManager;
        service._configMgr = configMgr;
        service._vpcDao = vpcDao;
        service._vpcMgr = vpcMgr;
        service._accountService = accountService;
        service._networksDao = networkDao;
        service._nicDao = nicDao;
        service._ipAddressDao = ipAddressDao;
        service.routerDao = routerDao;
        service.commandSetupHelper = commandSetupHelper;
        service.networkHelper = networkHelper;
        service._ipAddrMgr = ipAddressManager;
        callContextMocked = Mockito.mockStatic(CallContext.class);
        CallContext callContextMock = Mockito.mock(CallContext.class);
        callContextMocked.when(CallContext::current).thenReturn(callContextMock);
        accountMock = Mockito.mock(Account.class);
        Mockito.when(service._accountMgr.finalizeOwner(any(Account.class), nullable(String.class), nullable(Long.class), nullable(Long.class))).thenReturn(accountMock);
        Mockito.when(callContextMock.getCallingAccount()).thenReturn(accountMock);
        NetworkOffering networkOffering = Mockito.mock(NetworkOffering.class);
        Mockito.when(entityMgr.findById(NetworkOffering.class, 1L)).thenReturn(networkOffering);
        Mockito.when(networkOfferingDao.findById(1L)).thenReturn(offering);
        Mockito.when(physicalNetworkDao.findById(Mockito.anyLong())).thenReturn(phyNet);
        Mockito.when(dcDao.findById(Mockito.anyLong())).thenReturn(dc);
        Mockito.lenient().doNothing().when(accountManager).checkAccess(accountMock, networkOffering, dc);
        Mockito.when(accountManager.isRootAdmin(accountMock.getId())).thenReturn(true);
    }

    @After
    public void tearDown() throws Exception {
        callContextMocked.close();
        closeable.close();
    }

    private void overrideDefaultConfigValue(final ConfigKey configKey, final String name, final Object o) throws IllegalAccessException, NoSuchFieldException {
        Field f = ConfigKey.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(configKey, o);
    }

    @Test
    public void testGetPrivateVlanPairNoVlans() {
        Pair<String, Network.PVlanType> pair = service.getPrivateVlanPair(null, null, null);
        Assert.assertNull(pair.first());
        Assert.assertNull(pair.second());
    }

    @Test
    public void testGetPrivateVlanPairVlanPrimaryOnly() {
        Pair<String, Network.PVlanType> pair = service.getPrivateVlanPair(null, null, VLAN_ID_900);
        Assert.assertNull(pair.first());
        Assert.assertNull(pair.second());
    }

    @Test
    public void testGetPrivateVlanPairVlanPrimaryPromiscuousType() {
        Pair<String, Network.PVlanType> pair = service.getPrivateVlanPair(null, Network.PVlanType.Promiscuous.toString(), VLAN_ID_900);
        Assert.assertEquals(VLAN_ID_900, pair.first());
        Assert.assertEquals(Network.PVlanType.Promiscuous, pair.second());
    }

    @Test
    public void testGetPrivateVlanPairPromiscuousType() {
        Pair<String, Network.PVlanType> pair = service.getPrivateVlanPair(VLAN_ID_900, Network.PVlanType.Promiscuous.toString(), VLAN_ID_900);
        Assert.assertEquals(VLAN_ID_900, pair.first());
        Assert.assertEquals(Network.PVlanType.Promiscuous, pair.second());
    }

    @Test
    public void testGetPrivateVlanPairPromiscuousTypeOnSecondaryVlanId() {
        Pair<String, Network.PVlanType> pair = service.getPrivateVlanPair(VLAN_ID_900, "promiscuous", VLAN_ID_900);
        Assert.assertEquals(VLAN_ID_900, pair.first());
        Assert.assertEquals(Network.PVlanType.Promiscuous, pair.second());
    }

    @Test
    public void testGetPrivateVlanPairIsolatedType() {
        Pair<String, Network.PVlanType> pair = service.getPrivateVlanPair(VLAN_ID_901, Network.PVlanType.Isolated.toString(), VLAN_ID_900);
        Assert.assertEquals(VLAN_ID_901, pair.first());
        Assert.assertEquals(Network.PVlanType.Isolated, pair.second());
    }

    @Test
    public void testGetPrivateVlanPairIsolatedTypeOnSecondaryVlanId() {
        Pair<String, Network.PVlanType> pair = service.getPrivateVlanPair(VLAN_ID_901, "isolated", VLAN_ID_900);
        Assert.assertEquals(VLAN_ID_901, pair.first());
        Assert.assertEquals(Network.PVlanType.Isolated, pair.second());
    }

    @Test
    public void testGetPrivateVlanPairCommunityType() {
        Pair<String, Network.PVlanType> pair = service.getPrivateVlanPair(VLAN_ID_902, Network.PVlanType.Community.toString(), VLAN_ID_900);
        Assert.assertEquals(VLAN_ID_902, pair.first());
        Assert.assertEquals(Network.PVlanType.Community, pair.second());
    }

    @Test
    public void testGetPrivateVlanPairCommunityTypeOnSecondaryVlanId() {
        Pair<String, Network.PVlanType> pair = service.getPrivateVlanPair(VLAN_ID_902, "community", VLAN_ID_900);
        Assert.assertEquals(VLAN_ID_902, pair.first());
        Assert.assertEquals(Network.PVlanType.Community, pair.second());
    }

    @Test(expected = CloudRuntimeException.class)
    public void testPerformBasicChecksPromiscuousTypeExpectedIsolatedSet() {
        service.performBasicPrivateVlanChecks(VLAN_ID_900, VLAN_ID_900, Network.PVlanType.Isolated);
    }

    @Test(expected = CloudRuntimeException.class)
    public void testPerformBasicChecksPromiscuousTypeExpectedCommunitySet() {
        service.performBasicPrivateVlanChecks(VLAN_ID_900, VLAN_ID_900, Network.PVlanType.Community);
    }

    @Test(expected = CloudRuntimeException.class)
    public void testPerformBasicChecksPromiscuousTypeExpectedSecondaryVlanNullIsolatedSet() {
        service.performBasicPrivateVlanChecks(VLAN_ID_900, null, Network.PVlanType.Isolated);
    }

    @Test(expected = CloudRuntimeException.class)
    public void testPerformBasicChecksPromiscuousTypeExpectedSecondaryVlanNullCommunitySet() {
        service.performBasicPrivateVlanChecks(VLAN_ID_900, null, Network.PVlanType.Community);
    }

    @Test(expected = CloudRuntimeException.class)
    public void testPerformBasicChecksPromiscuousTypeExpectedDifferentVlanIds() {
        service.performBasicPrivateVlanChecks(VLAN_ID_900, VLAN_ID_901, Network.PVlanType.Promiscuous);
    }

    @Test
    public void testCreateGuestNetwork() throws InsufficientCapacityException, ResourceAllocationException {
        Integer publicMtu = 2450;
        Integer privateMtu = 1200;
        ReflectionTestUtils.setField(createNetworkCmd, "name", "testNetwork");
        ReflectionTestUtils.setField(createNetworkCmd, "displayText", "Test Network");
        ReflectionTestUtils.setField(createNetworkCmd, "networkOfferingId", 1L);
        ReflectionTestUtils.setField(createNetworkCmd, "zoneId", 1L);
        ReflectionTestUtils.setField(createNetworkCmd, "publicMtu", publicMtu);
        ReflectionTestUtils.setField(createNetworkCmd, "privateMtu", privateMtu);
        ReflectionTestUtils.setField(createNetworkCmd, "physicalNetworkId", null);
        Mockito.when(offering.isSystemOnly()).thenReturn(false);
        Mockito.when(dc.getId()).thenReturn(1L);
        Mockito.when(dc.getAllocationState()).thenReturn(Grouping.AllocationState.Enabled);
        Map<String, String> networkProvidersMap = new HashMap<String, String>();
        Mockito.when(networkManager.finalizeServicesAndProvidersForNetwork(ArgumentMatchers.any(NetworkOffering.class), anyLong())).thenReturn(networkProvidersMap);
        lenient().doNothing().when(alertManager).sendAlert(any(AlertService.AlertType.class), anyLong(), anyLong(), anyString(), anyString());
        Mockito.when(configMgr.isOfferingForVpc(offering)).thenReturn(false);
        Mockito.when(offering.isInternalLb()).thenReturn(false);

        service.createGuestNetwork(createNetworkCmd);
        Mockito.verify(networkManager, times(1)).createGuestNetwork(1L, "testNetwork", "Test Network", null,
                null, null, false, null, accountMock, null, phyNet,
                1L, null, null, null, null, null,
                true, null, null, null, null, null,
                null, null, null, null, new Pair<>(1500, privateMtu));
    }
    @Test
    public void testValidateMtuConfigWhenMtusExceedThreshold() {
        Integer publicMtu = 2450;
        Integer privateMtu = 1500;
        Long zoneId = 1L;
        Pair<Integer, Integer> interfaceMtus = service.validateMtuConfig(publicMtu, privateMtu, zoneId);
        Assert.assertNotNull(interfaceMtus);
        Assert.assertEquals(NetworkService.DEFAULT_MTU, interfaceMtus.first());
        Assert.assertEquals(NetworkService.DEFAULT_MTU, interfaceMtus.second());
        Mockito.verify(alertManager, Mockito.times(1)).sendAlert(Mockito.any(AlertService.AlertType.class),
                Mockito.anyLong(), nullable(Long.class), Mockito.anyString(), Mockito.anyString());
    }

    @Test
    public void testValidatePrivateMtuExceedingThreshold() {
        Integer publicMtu = 1500;
        Integer privateMtu = 2500;
        Long zoneId = 1L;
        Pair<Integer, Integer> interfaceMtus = service.validateMtuConfig(publicMtu, privateMtu, zoneId);
        Assert.assertNotNull(interfaceMtus);
        Assert.assertEquals(NetworkService.DEFAULT_MTU, interfaceMtus.first());
        Assert.assertEquals(NetworkService.DEFAULT_MTU, interfaceMtus.second());
        Mockito.verify(alertManager, Mockito.times(1)).sendAlert(Mockito.any(AlertService.AlertType.class),
                Mockito.anyLong(), nullable(Long.class), Mockito.anyString(), Mockito.anyString());
    }

    @Test
    public void testValidateBypassingPublicMtuPassedDuringNetworkTierCreationForVpcs() throws InsufficientCapacityException, ResourceAllocationException {
        Integer publicMtu = 1250;
        Integer privateMtu = 1000;
        Long zoneId = 1L;
        ReflectionTestUtils.setField(createNetworkCmd, "name", "testNetwork");
        ReflectionTestUtils.setField(createNetworkCmd, "displayText", "Test Network");
        ReflectionTestUtils.setField(createNetworkCmd, "networkOfferingId", 1L);
        ReflectionTestUtils.setField(createNetworkCmd, "zoneId", zoneId);
        ReflectionTestUtils.setField(createNetworkCmd, "publicMtu", publicMtu);
        ReflectionTestUtils.setField(createNetworkCmd, "privateMtu", privateMtu);
        ReflectionTestUtils.setField(createNetworkCmd, "physicalNetworkId", null);
        ReflectionTestUtils.setField(createNetworkCmd, "vpcId", 1L);
        Mockito.when(dc.getId()).thenReturn(1L);
        Mockito.when(configMgr.isOfferingForVpc(offering)).thenReturn(true);
        Mockito.when(vpcDao.findById(anyLong())).thenReturn(vpc);

        service.createGuestNetwork(createNetworkCmd);
        Mockito.verify(vpcMgr, times(1)).createVpcGuestNetwork(1L, "testNetwork", "Test Network", null,
                null, null, null, accountMock, null, phyNet,
                1L, null, null, 1L, null, accountMock,
                true, null, null, null, null, null, null, null, new Pair<>(0, 1000));

    }

    @Test
    public void testUpdateSharedNetworkMtus() throws Exception {
        Integer publicMtu = 1250;
        Integer privateMtu = 1000;
        Long networkId = 1L;
        Long zoneId = 1L;
        ReflectionTestUtils.setField(updateNetworkCmd, "id", networkId);
        ReflectionTestUtils.setField(updateNetworkCmd, "publicMtu", publicMtu);
        ReflectionTestUtils.setField(updateNetworkCmd, "privateMtu", privateMtu);

        User callingUser = mock(User.class);
        UserVO userVO = mock(UserVO.class);
        Account callingAccount = mock(Account.class);
        NetworkVO networkVO = mock(NetworkVO.class);
        NicVO nicVO = mock(NicVO.class);
        List<IPAddressVO> addresses = new ArrayList<>();
        List<IpAddressTO> ips = new ArrayList<>();
        List<DomainRouterVO> routers = new ArrayList<>();
        DomainRouterVO routerPrimary = Mockito.mock(DomainRouterVO.class);
        routers.add(routerPrimary);

        CallContext.register(callingUser, callingAccount);
        Mockito.when(CallContext.current()).thenReturn(callContextMock);
        Mockito.when(networkVO.getVpcId()).thenReturn(null);

        Pair<Integer, Integer> updatedMtus = service.validateMtuOnUpdate(networkVO, zoneId, publicMtu, privateMtu);
        Assert.assertEquals(publicMtu, updatedMtus.first());
        Assert.assertEquals(privateMtu, updatedMtus.second());
    }

    @Test
    public void testUpdatePublicInterfaceMtuViaNetworkTiersForVpcNetworks() {
        Integer vpcMtu = 1450;
        Integer publicMtu = 1250;
        Integer privateMtu = 1000;
        Long vpcId = 1L;
        Long zoneId = 1L;
        ReflectionTestUtils.setField(createNetworkCmd, "name", "testNetwork");
        ReflectionTestUtils.setField(createNetworkCmd, "displayText", "Test Network");
        ReflectionTestUtils.setField(createNetworkCmd, "networkOfferingId", 1L);
        ReflectionTestUtils.setField(createNetworkCmd, "zoneId", zoneId);
        ReflectionTestUtils.setField(createNetworkCmd, "publicMtu", publicMtu);
        ReflectionTestUtils.setField(createNetworkCmd, "privateMtu", privateMtu);
        ReflectionTestUtils.setField(createNetworkCmd, "physicalNetworkId", null);
        ReflectionTestUtils.setField(createNetworkCmd, "vpcId", vpcId);

        VpcVO vpcVO = Mockito.mock(VpcVO.class);
        Mockito.when(vpcDao.findById(anyLong())).thenReturn(vpcVO);
        Mockito.when(vpcVO.getPublicMtu()).thenReturn(vpcMtu);

        Pair<Integer, Integer> updatedMtus = service.validateMtuConfig(publicMtu, privateMtu, zoneId);
        service.mtuCheckForVpcNetwork(vpcId, updatedMtus, publicMtu, privateMtu);
        Assert.assertEquals(vpcMtu, updatedMtus.first());
        Assert.assertEquals(privateMtu, updatedMtus.second());
    }

    private void prepareCreateNetworkDnsMocks(CreateNetworkCmd cmd, Network.GuestType guestType, boolean ipv6, boolean isVpc, boolean dnsServiceSupported) {
        long networkOfferingId = 1L;
        Mockito.when(cmd.getNetworkOfferingId()).thenReturn(networkOfferingId);
        NetworkOfferingVO networkOfferingVO = Mockito.mock(NetworkOfferingVO.class);
        Mockito.when(networkOfferingVO.getId()).thenReturn(networkOfferingId);
        Mockito.when(networkOfferingVO.getGuestType()).thenReturn(guestType);
        Mockito.when(networkOfferingDao.findById(networkOfferingId)).thenReturn(networkOfferingVO);
        if (Network.GuestType.Shared.equals(guestType)) {
            Mockito.when(networkModel.isProviderForNetworkOffering(Mockito.any(), Mockito.anyLong())).thenReturn(true);
            Mockito.when(cmd.getGateway()).thenReturn(IP4_GATEWAY);
            Mockito.when(cmd.getNetmask()).thenReturn(IP4_NETMASK);
        }
        Mockito.when(physicalNetworkDao.findById(Mockito.anyLong())).thenReturn(Mockito.mock(PhysicalNetworkVO.class));
        Mockito.when(networkOfferingServiceMapDao.areServicesSupportedByNetworkOffering(networkOfferingId, Network.Service.Dns)).thenReturn(dnsServiceSupported);
        if(ipv6 && Network.GuestType.Isolated.equals(guestType)) {
            Mockito.when(networkOfferingDao.isIpv6Supported(networkOfferingId)).thenReturn(true);
            try {
                Mockito.when(ipv6Service.preAllocateIpv6SubnetForNetwork(Mockito.anyLong())).thenReturn(new Pair<>(IP6_GATEWAY, IP6_CIDR));
            } catch (ResourceAllocationException e) {
                Assert.fail(String.format("failure with exception: %s", e.getMessage()));
            }
        }
        Mockito.when(cmd.getSubdomainAccess()).thenReturn(null);
        Mockito.when(cmd.getAssociatedNetworkId()).thenReturn(null);
        if (isVpc) {
            Mockito.when(cmd.getVpcId()).thenReturn(1L);
        } else {
            Mockito.when(cmd.getVpcId()).thenReturn(null);
        }
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testCreateL2NetworkDnsFailure() {
        registerCallContext();
        CreateNetworkCmd cmd = Mockito.mock(CreateNetworkCmd.class);
        prepareCreateNetworkDnsMocks(cmd, Network.GuestType.L2, false, false, true);
        Mockito.when(cmd.getIp4Dns1()).thenReturn(ip4Dns[0]);
        try {
            service.createGuestNetwork(cmd);
        } catch (InsufficientCapacityException | ResourceAllocationException e) {
            Assert.fail(String.format("failure with exception: %s", e.getMessage()));
        }
    }

    @Test
    public void testCreateNetworkDnsVpcFailure() {
        registerCallContext();
        CreateNetworkCmd cmd = Mockito.mock(CreateNetworkCmd.class);
        prepareCreateNetworkDnsMocks(cmd, Network.GuestType.Isolated, false, false, true);
        Mockito.when(cmd.getIp4Dns1()).thenReturn(ip4Dns[0]);
        try {
            service.createGuestNetwork(cmd);
        } catch (InsufficientCapacityException | ResourceAllocationException e) {
            Assert.fail(String.format("failure with exception: %s", e.getMessage()));
        }
    }

    @Test(expected = CloudRuntimeException.class)
    public void testCreateNetworkDnsOfferingServiceFailure() {
        registerCallContext();
        CreateNetworkCmd cmd = Mockito.mock(CreateNetworkCmd.class);
        Mockito.when(cmd.getDomainId()).thenReturn(null);
        Mockito.when(cmd.getProjectId()).thenReturn(null);
        prepareCreateNetworkDnsMocks(cmd, Network.GuestType.Isolated, false, false, false);
        Mockito.when(cmd.getIp4Dns1()).thenReturn(ip4Dns[0]);
        try {
            service.createGuestNetwork(cmd);
        } catch (InsufficientCapacityException | ResourceAllocationException e) {
            Assert.fail(String.format("failure with exception: %s", e.getMessage()));
        }
    }

    @Test(expected = CloudRuntimeException.class)
    public void testCreateIp4NetworkIp6DnsFailure() {
        registerCallContext();
        CreateNetworkCmd cmd = Mockito.mock(CreateNetworkCmd.class);
        prepareCreateNetworkDnsMocks(cmd, Network.GuestType.Isolated, false, false, true);
        Mockito.when(cmd.getIp6Dns1()).thenReturn(ip4Dns[0]);
        try {
            service.createGuestNetwork(cmd);
        } catch (InsufficientCapacityException | ResourceAllocationException e) {
            Assert.fail(String.format("failure with exception: %s", e.getMessage()));
        }
    }

    @Test
    public void testCheckAndUpdateNetworkNoUpdate() {
        Assert.assertFalse(service.checkAndUpdateNetworkDns(Mockito.mock(NetworkVO.class), Mockito.mock(NetworkOffering.class), null, null, null, null));
        NetworkVO network1 = Mockito.mock(NetworkVO.class);
        Mockito.when(network1.getDns1()).thenReturn(ip4Dns[0]);
        Long offeringId = 1L;
        NetworkOffering offering = Mockito.mock(NetworkOffering.class);
        Mockito.when(offering.getId()).thenReturn(offeringId);
        Mockito.when(networkOfferingServiceMapDao.areServicesSupportedByNetworkOffering(offeringId, Network.Service.Dns)).thenReturn(true);
        Assert.assertFalse(service.checkAndUpdateNetworkDns(network1, offering, null, null, null, null));
        Assert.assertFalse(service.checkAndUpdateNetworkDns(network1, Mockito.mock(NetworkOffering.class), ip4Dns[0], null, null, null));
        Mockito.when(network1.getIp6Dns1()).thenReturn(ip6Dns[0]);
        Assert.assertFalse(service.checkAndUpdateNetworkDns(network1, Mockito.mock(NetworkOffering.class), ip4Dns[0], null, ip6Dns[0], null));
    }

    @Test
    public void testCheckAndUpdateNetworkOfferingChangeReset() {
        NetworkVO networkVO = new NetworkVO();
        networkVO.setDns1(ip4Dns[0]);
        Long offeringId = 1L;
        NetworkOffering offering = Mockito.mock(NetworkOffering.class);
        Mockito.when(offering.getId()).thenReturn(offeringId);
        Mockito.when(networkOfferingServiceMapDao.areServicesSupportedByNetworkOffering(offeringId, Network.Service.Dns)).thenReturn(false);
        Assert.assertTrue(service.checkAndUpdateNetworkDns(networkVO, offering, null, null, null, null));
        Assert.assertNull(networkVO.getDns1());
        Assert.assertNull(networkVO.getDns2());
        Assert.assertNull(networkVO.getIp6Dns1());
        Assert.assertNull(networkVO.getIp6Dns2());
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testCheckAndUpdateNetworkDnsL2NetworkFailure() {
        NetworkVO networkVO = Mockito.mock(NetworkVO.class);
        NetworkOffering offering = Mockito.mock(NetworkOffering.class);
        Mockito.when(offering.getGuestType()).thenReturn(Network.GuestType.L2);
        service.checkAndUpdateNetworkDns(networkVO, offering, ip4Dns[0], null, null, null);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testCheckAndUpdateNetworkDnsVpcTierFailure() {
        NetworkVO networkVO = Mockito.mock(NetworkVO.class);
        Mockito.when(networkVO.getVpcId()).thenReturn(1L);
        NetworkOffering offering = Mockito.mock(NetworkOffering.class);
        Mockito.when(offering.getGuestType()).thenReturn(Network.GuestType.Shared);
        service.checkAndUpdateNetworkDns(networkVO, offering, ip4Dns[0], null, null, null);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testCheckAndUpdateNetworkDnsServiceFailure() {
        NetworkVO networkVO = Mockito.mock(NetworkVO.class);
        Mockito.when(networkVO.getVpcId()).thenReturn(null);
        Long offeringId = 1L;
        NetworkOffering offering = Mockito.mock(NetworkOffering.class);
        Mockito.when(offering.getId()).thenReturn(offeringId);
        Mockito.when(offering.getGuestType()).thenReturn(Network.GuestType.Shared);
        Mockito.when(networkOfferingServiceMapDao.areServicesSupportedByNetworkOffering(offeringId, Network.Service.Dns)).thenReturn(false);
        service.checkAndUpdateNetworkDns(networkVO, offering, ip4Dns[0], null, null, null);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testCheckAndUpdateNetworkNotSharedIp6Failure() {
        NetworkVO networkVO = Mockito.mock(NetworkVO.class);
        Mockito.when(networkVO.getVpcId()).thenReturn(null);
        Mockito.when(networkVO.getIp6Cidr()).thenReturn(null);
        Mockito.when(networkVO.getGuestType()).thenReturn(Network.GuestType.Shared);
        Long offeringId = 1L;
        NetworkOffering offering = Mockito.mock(NetworkOffering.class);
        Mockito.when(offering.getId()).thenReturn(offeringId);
        Mockito.when(networkOfferingServiceMapDao.areServicesSupportedByNetworkOffering(offeringId, Network.Service.Dns)).thenReturn(true);
        service.checkAndUpdateNetworkDns(networkVO, offering, null, null, ip6Dns[0], null);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testCheckAndUpdateNetworkNotIsolatedIp6Failure() {
        NetworkVO networkVO = Mockito.mock(NetworkVO.class);
        Mockito.when(networkVO.getVpcId()).thenReturn(null);
        Mockito.when(networkVO.getGuestType()).thenReturn(Network.GuestType.Isolated);
        Long offeringId = 1L;
        NetworkOffering offering = Mockito.mock(NetworkOffering.class);
        Mockito.when(offering.getId()).thenReturn(offeringId);
        Mockito.when(networkOfferingDao.isIpv6Supported(offeringId)).thenReturn(false);
        Mockito.when(networkOfferingServiceMapDao.areServicesSupportedByNetworkOffering(offeringId, Network.Service.Dns)).thenReturn(true);
        service.checkAndUpdateNetworkDns(networkVO, offering, null, null, ip6Dns[0], null);
    }

    @Test
    public void testCheckAndUpdateNetworkSuccess() {
        NetworkVO networkVO = new NetworkVO();
        networkVO.setVpcId(null);
        try {
            Field id = networkVO.getClass().getDeclaredField("guestType");
            id.setAccessible(true);
            id.set(networkVO, Network.GuestType.Shared);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            Assert.fail(String.format("Unable to set network guestType, %s", e.getMessage()));
        }
        networkVO.setIp6Cidr("cidr");
        Long offeringId = 1L;
        NetworkOffering offering = Mockito.mock(NetworkOffering.class);
        Mockito.when(offering.getId()).thenReturn(offeringId);
        Mockito.when(networkOfferingServiceMapDao.areServicesSupportedByNetworkOffering(offeringId, Network.Service.Dns)).thenReturn(true);
        boolean updated = service.checkAndUpdateNetworkDns(networkVO, offering, ip4Dns[0], null, ip6Dns[0], null);
        Assert.assertTrue(updated);
        Assert.assertEquals(ip4Dns[0], networkVO.getDns1());
        Assert.assertNull(networkVO.getDns2());
        Assert.assertEquals(ip6Dns[0], networkVO.getIp6Dns1());
        Assert.assertNull(networkVO.getIp6Dns2());
    }

    @Test
    public void testCheckAndUpdateNetworkResetSuccess() {
        NetworkVO networkVO = new NetworkVO();
        networkVO.setVpcId(null);
        networkVO.setDns1(ip4Dns[0]);
        networkVO.setIp6Dns1(ip6Dns[0]);
        try {
            Field id = networkVO.getClass().getDeclaredField("guestType");
            id.setAccessible(true);
            id.set(networkVO, Network.GuestType.Shared);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            Assert.fail(String.format("Unable to set network guestType, %s", e.getMessage()));
        }
        networkVO.setIp6Cidr("cidr");
        NetworkOffering offering = Mockito.mock(NetworkOffering.class);
        boolean updated = service.checkAndUpdateNetworkDns(networkVO, offering, "", null, "", null);
        Assert.assertTrue(updated);
        Assert.assertNull(networkVO.getDns1());
        Assert.assertNull(networkVO.getDns2());
        Assert.assertNull(networkVO.getIp6Dns1());
        Assert.assertNull(networkVO.getIp6Dns2());
    }
    @Test
    public void validateIfServiceOfferingIsActiveAndSystemVmTypeIsDomainRouterTestMustThrowInvalidParameterValueExceptionWhenServiceOfferingIsNull() {
        doReturn(null).when(serviceOfferingDaoMock).findById(anyLong());

        String expectedMessage = String.format("Could not find specified service offering [%s].", 1l);
        InvalidParameterValueException assertThrows = Assert.assertThrows(expectedException, () -> {
            service.validateIfServiceOfferingIsActiveAndSystemVmTypeIsDomainRouter(1l);
        });

        Assert.assertEquals(expectedMessage, assertThrows.getMessage());
    }

    @Test
    public void validateIfServiceOfferingIsActiveAndSystemVmTypeIsDomainRouterTestMustThrowInvalidParameterValueExceptionWhenServiceOfferingStateIsInactive() {
        doReturn(serviceOfferingVoMock).when(serviceOfferingDaoMock).findById(anyLong());
        doReturn(ServiceOffering.State.Inactive).when(serviceOfferingVoMock).getState();

        String expectedMessage = String.format("The specified service offering [%s] is inactive.", serviceOfferingVoMock);
        InvalidParameterValueException assertThrows = Assert.assertThrows(expectedException, () -> {
            service.validateIfServiceOfferingIsActiveAndSystemVmTypeIsDomainRouter(1l);
        });

        Assert.assertEquals(expectedMessage, assertThrows.getMessage());
    }

    @Test
    public void validateIfServiceOfferingIsActiveAndSystemVmTypeIsDomainRouterTestMustThrowInvalidParameterValueExceptionWhenSystemVmTypeIsNotDomainRouter() {
        doReturn(serviceOfferingVoMock).when(serviceOfferingDaoMock).findById(anyLong());
        doReturn(ServiceOffering.State.Active).when(serviceOfferingVoMock).getState();
        doReturn(VirtualMachine.Type.ElasticLoadBalancerVm.toString()).when(serviceOfferingVoMock).getSystemVmType();

        String expectedMessage = String.format("The specified service offering [%s] is of type [%s]. Virtual routers can only be created with service offering of type [%s].",
                serviceOfferingVoMock, serviceOfferingVoMock.getSystemVmType(), VirtualMachine.Type.DomainRouter.toString().toLowerCase());
        InvalidParameterValueException assertThrows = Assert.assertThrows(expectedException, () -> {
            service.validateIfServiceOfferingIsActiveAndSystemVmTypeIsDomainRouter(1l);
        });

        Assert.assertEquals(expectedMessage, assertThrows.getMessage());
    }

    @Test
    public void validateIfServiceOfferingIsActiveAndSystemVmTypeIsDomainRouterTestMustNotThrowInvalidParameterValueExceptionWhenSystemVmTypeIsDomainRouter() {
        NetworkServiceImpl networkServiceImplMock = mock(NetworkServiceImpl.class);

        networkServiceImplMock.validateIfServiceOfferingIsActiveAndSystemVmTypeIsDomainRouter(1l);
    }

    @Test
    public void validateNotSharedNetworkRouterIPv4() {
        NetworkOffering ntwkOff = Mockito.mock(NetworkOffering.class);
        when(ntwkOff.getGuestType()).thenReturn(Network.GuestType.L2);
        service.validateSharedNetworkRouterIPs(null, null, null, null, null, null, null, null, null, ntwkOff);
    }

    @Test
    public void validateSharedNetworkRouterIPs() {
        String startIP = "10.0.16.2";
        String endIP = "10.0.16.100";
        String routerIPv4 = "10.0.16.100";
        String routerPv6 = "fd17:ac56:1234:2000::fb";
        String startIPv6 = "fd17:ac56:1234:2000::1";
        String endIPv6 = "fd17:ac56:1234:2000::fc";
        NetworkOffering ntwkOff = Mockito.mock(NetworkOffering.class);
        when(ntwkOff.getGuestType()).thenReturn(Network.GuestType.Shared);
        service.validateSharedNetworkRouterIPs(IP4_GATEWAY, startIP, endIP, IP4_NETMASK, routerIPv4, routerPv6, startIPv6, endIPv6, IP6_CIDR, ntwkOff);
    }

    @Test
    public void validateSharedNetworkWrongRouterIPv4() {
        String startIP = "10.0.16.2";
        String endIP = "10.0.16.100";
        String routerIPv4 = "10.0.16.101";
        String routerPv6 = "fd17:ac56:1234:2000::fb";
        String startIPv6 = "fd17:ac56:1234:2000::1";
        String endIPv6 = "fd17:ac56:1234:2000::fc";
        NetworkOffering ntwkOff = Mockito.mock(NetworkOffering.class);
        when(ntwkOff.getGuestType()).thenReturn(Network.GuestType.Shared);
        boolean passing = false;
        try {
            service.validateSharedNetworkRouterIPs(IP4_GATEWAY, startIP, endIP, IP4_NETMASK, routerIPv4, routerPv6, startIPv6, endIPv6, IP6_CIDR, ntwkOff);
        } catch (CloudRuntimeException e) {
            Assert.assertTrue(e.getMessage().contains("Router IPv4 IP provided is not within the specified range: "));
            passing = true;
        }
        Assert.assertTrue(passing);
    }

    @Test
    public void validateSharedNetworkNoEndOfIPv6Range() {
        String startIP = null;
        String endIP = null;
        String routerIPv4 = null;
        String routerPv6 = "fd17:ac56:1234:2000::1";
        String startIPv6 = "fd17:ac56:1234:2000::1";
        String endIPv6 = null;
        NetworkOffering ntwkOff = Mockito.mock(NetworkOffering.class);
        when(ntwkOff.getGuestType()).thenReturn(Network.GuestType.Shared);
        service.validateSharedNetworkRouterIPs(IP4_GATEWAY, startIP, endIP, IP4_NETMASK, routerIPv4, routerPv6, startIPv6, endIPv6, IP6_CIDR, ntwkOff);
    }

    @Test
    public void validateSharedNetworkIPv6RouterNotInRange() {
        String routerIPv4 = null;
        String routerIPv6 = "fd17:ac56:1234:2001::1";
        NetworkOffering ntwkOff = Mockito.mock(NetworkOffering.class);
        when(ntwkOff.getGuestType()).thenReturn(Network.GuestType.Shared);
        boolean passing = true;
        try {
            service.validateSharedNetworkRouterIPs(IP4_GATEWAY, null, null, IP4_NETMASK, routerIPv4, routerIPv6, null, null, IP6_CIDR, ntwkOff);
            passing = false;
        } catch (CloudRuntimeException e) {
            Assert.assertTrue(e.getMessage().contains("Router IPv6 address provided is not with the network range"));
        }
        Assert.assertTrue(passing);
    }

    @Test
    public void invalidateSharedNetworkIPv6RouterAddress() {
        String routerIPv6 = "fd17:ac56:1234:2000::fg";
        NetworkOffering ntwkOff = Mockito.mock(NetworkOffering.class);
        when(ntwkOff.getGuestType()).thenReturn(Network.GuestType.Shared);
        boolean passing = false;
        try {
            service.validateSharedNetworkRouterIPs(IP4_GATEWAY, null, null, IP4_NETMASK, null, routerIPv6, null, null, IP6_CIDR, ntwkOff);
        } catch (CloudRuntimeException e) {
            Assert.assertTrue(e.getMessage().contains("Router IPv6 address provided is of incorrect format"));
            passing = true;
        }
        Assert.assertTrue(passing);
    }

    @Test
    public void invalidateSharedNetworkIPv4RouterAddress() {
        String routerIPv4 = "10.100.1000.1";
        NetworkOffering ntwkOff = Mockito.mock(NetworkOffering.class);
        when(ntwkOff.getGuestType()).thenReturn(Network.GuestType.Shared);
        boolean passing = false;
        try {
            service.validateSharedNetworkRouterIPs(IP4_GATEWAY, null, null, IP4_NETMASK, routerIPv4, null, null, null, IP6_CIDR, ntwkOff);
        } catch (CloudRuntimeException e) {
            Assert.assertTrue(e.getMessage().contains("Router IPv4 IP provided is of incorrect format"));
            passing = true;
        }
        Assert.assertTrue(passing);
    }

    @Test
    public void checkAndDontSetSourceNatIp() {
        CreateNetworkCmd cmd = new CreateNetworkCmd();
        try {
            service.checkAndSetRouterSourceNatIp(account, cmd, null);
        } catch (InsufficientAddressCapacityException | ResourceAllocationException e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void checkAndSetSourceNatIp() {
        String srcNatIp = "10.100.1000.10000";
        Long networkOfferingId = 2l;
        Long zoneId = 3l;
        registerCallContext();
        ReflectionTestUtils.setField(createNetworkCmd, "networkOfferingId", networkOfferingId);
        ReflectionTestUtils.setField(createNetworkCmd, "sourceNatIP", srcNatIp);
        ReflectionTestUtils.setField(createNetworkCmd, "zoneId", zoneId);
        ReflectionTestUtils.setField(createNetworkCmd, "physicalNetworkId", null);
        NetworkVO networkVO = Mockito.spy(NetworkVO.class);
        IpAddress ipAddress = Mockito.mock(IPAddressVO.class);
        NetworkOffering ntwkOff = Mockito.mock(NetworkOffering.class);
        Long networkId = 7l;
        when(networkVO.getId()).thenReturn(networkId);
        when(networkVO.getGuestType()).thenReturn(Network.GuestType.Isolated);
        when(networkDao.findById(networkId)).thenReturn(networkVO);
        when(entityMgr.findById(NetworkOffering.class, networkOfferingId)).thenReturn(ntwkOff);
        when(entityMgr.findById(eq(DataCenter.class), anyLong())).thenReturn(dc);
        when(ipAddress.getId()).thenReturn(5l);
        when(networkVO.getId()).thenReturn(networkId);
        when(networkVO.getGuestType()).thenReturn(Network.GuestType.Isolated);
        try {
            when(ipAddressManager.allocateIp(any(), anyBoolean(), any(), anyLong(), any(), any(), eq(srcNatIp))).thenReturn(ipAddress);
            service.checkAndSetRouterSourceNatIp(account, createNetworkCmd, networkVO);
        } catch (InsufficientAddressCapacityException | ResourceAllocationException e) {
            Assert.fail(e.getMessage());
        }
    }
}
