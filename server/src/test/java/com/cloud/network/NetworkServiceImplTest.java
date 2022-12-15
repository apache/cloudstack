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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.cloudstack.alert.AlertService;
import org.apache.cloudstack.api.command.user.network.CreateNetworkCmd;
import org.apache.cloudstack.api.command.user.network.UpdateNetworkCmd;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloud.agent.api.to.IpAddressTO;
import com.cloud.alert.AlertManager;
import com.cloud.configuration.ConfigurationManager;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
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
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.offerings.dao.NetworkOfferingServiceMapDao;
import com.cloud.org.Grouping;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountManagerImpl;
import com.cloud.user.AccountService;
import com.cloud.user.AccountVO;
import com.cloud.user.User;
import com.cloud.user.UserVO;
import com.cloud.user.dao.UserDao;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.NicVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.DomainRouterDao;
import com.cloud.vm.dao.NicDao;


@PowerMockIgnore("javax.management.*")
@RunWith(PowerMockRunner.class)
@PrepareForTest({ComponentContext.class, CallContext.class})
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
    AccountManager accountMgr;
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

    @InjectMocks
    AccountManagerImpl accountManagerImpl;
    @Mock
    ConfigKey<Integer> privateMtuKey;
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
    private NetworkServiceImpl service = new NetworkServiceImpl();
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

    private void registerCallContext() {
        account = new AccountVO("testaccount", 1L, "networkdomain", Account.Type.NORMAL, "uuid");
        account.setId(ACCOUNT_ID);
        user = new UserVO(1, "testuser", "password", "firstname", "lastName", "email", "timezone",
                UUID.randomUUID().toString(), User.Source.UNKNOWN);
        CallContext.register(user, account);
    }

    @Before
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);
        replaceUserChangeMtuField();
        Mockito.when(userChangeMtuKey.valueIn(anyLong())).thenReturn(Boolean.TRUE);
        offering = Mockito.mock(NetworkOfferingVO.class);
        network = Mockito.mock(Network.class);
        dc = Mockito.mock(DataCenterVO.class);
        phyNet = Mockito.mock(PhysicalNetworkVO.class);
        vpc = Mockito.mock(VpcVO.class);
        service._networkOfferingDao = networkOfferingDao;
        service._physicalNetworkDao = physicalNetworkDao;
        service._dcDao = dcDao;
        service._accountMgr = accountMgr;
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
        PowerMockito.mockStatic(CallContext.class);
        CallContext callContextMock = PowerMockito.mock(CallContext.class);
        PowerMockito.when(CallContext.current()).thenReturn(callContextMock);
        accountMock = PowerMockito.mock(Account.class);
        Mockito.when(service._accountMgr.finalizeOwner(any(Account.class), nullable(String.class), nullable(Long.class), nullable(Long.class))).thenReturn(accountMock);
        PowerMockito.when(callContextMock.getCallingAccount()).thenReturn(accountMock);
        NetworkOffering networkOffering = Mockito.mock(NetworkOffering.class);
        Mockito.when(entityMgr.findById(NetworkOffering.class, 1L)).thenReturn(networkOffering);
        Mockito.when(networkService.findPhysicalNetworkId(Mockito.anyLong(), ArgumentMatchers.nullable(String.class), Mockito.any(Networks.TrafficType.class))).thenReturn(1L);
        Mockito.when(networkOfferingDao.findById(1L)).thenReturn(offering);
        Mockito.when(physicalNetworkDao.findById(Mockito.anyLong())).thenReturn(phyNet);
        Mockito.when(dcDao.findById(Mockito.anyLong())).thenReturn(dc);
        Mockito.lenient().doNothing().when(accountMgr).checkAccess(accountMock, networkOffering, dc);
        Mockito.when(accountMgr.isRootAdmin(accountMock.getId())).thenReturn(true);
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
        when(publicMtuKey.valueIn(zoneId)).thenReturn(NetworkService.DEFAULT_MTU);
        when(privateMtuKey.valueIn(zoneId)).thenReturn(NetworkService.DEFAULT_MTU);
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
        when(publicMtuKey.valueIn(zoneId)).thenReturn(NetworkService.DEFAULT_MTU);
        when(privateMtuKey.valueIn(zoneId)).thenReturn(NetworkService.DEFAULT_MTU);
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
        PowerMockito.when(CallContext.current()).thenReturn(callContextMock);
        Mockito.doReturn(1L).when(callContextMock).getCallingUserId();
        Mockito.when(userDao.findById(anyLong())).thenReturn(userVO);
        Mockito.when(accountService.getActiveUser(1L)).thenReturn(callingUser);
        Mockito.when(callingUser.getAccountId()).thenReturn(1L);
        Mockito.when(accountService.getActiveAccountById(anyLong())).thenReturn(callingAccount);
        Mockito.when(networkDao.findById(anyLong())).thenReturn(networkVO);
        Mockito.when(networkOfferingDao.findByIdIncludingRemoved(anyLong())).thenReturn(offering);
        Mockito.when(offering.isSystemOnly()).thenReturn(false);
        Mockito.when(networkVO.getTrafficType()).thenReturn(Networks.TrafficType.Guest);
        Mockito.when(networkVO.getGuestType()).thenReturn(Network.GuestType.Shared);
        Mockito.when(nicDao.listByNetworkIdAndType(anyLong(), any(VirtualMachine.Type.class))).thenReturn(List.of(Mockito.mock(NicVO.class)));
        Mockito.when(networkVO.getVpcId()).thenReturn(null);
        Mockito.when(ipAddressDao.listByNetworkId(anyLong())).thenReturn(addresses);

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

    private void replaceUserChangeMtuField() throws Exception {
        Field field = NetworkService.class.getDeclaredField("AllowUsersToSpecifyVRMtu");
        field.setAccessible(true);
        // remove final modifier from field
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
        field.set(null, userChangeMtuKey);
    }

    private void prepareCreateNetworkDnsMocks(CreateNetworkCmd cmd, Network.GuestType guestType, boolean ipv6, boolean isVpc, boolean dnsServiceSupported) {
        long networkOfferingId = 1L;
        Mockito.when(cmd.getNetworkOfferingId()).thenReturn(networkOfferingId);
        NetworkOfferingVO networkOfferingVO = Mockito.mock(NetworkOfferingVO.class);
        Mockito.when(networkOfferingVO.getId()).thenReturn(networkOfferingId);
        Mockito.when(networkOfferingVO.getGuestType()).thenReturn(guestType);
        Mockito.when(networkOfferingVO.getTrafficType()).thenReturn(Networks.TrafficType.Guest);
        Mockito.when(networkOfferingVO.isSpecifyIpRanges()).thenReturn(true);
        Mockito.when(networkOfferingDao.findById(networkOfferingId)).thenReturn(networkOfferingVO);
        if (Network.GuestType.Shared.equals(guestType)) {
            Mockito.when(networkModel.isProviderForNetworkOffering(Mockito.any(), Mockito.anyLong())).thenReturn(true);
            Mockito.when(cmd.getGateway()).thenReturn(IP4_GATEWAY);
            Mockito.when(cmd.getNetmask()).thenReturn(IP4_NETMASK);
        }
        Mockito.when(accountManager.isNormalUser(Mockito.anyLong())).thenReturn(true);
        Mockito.when(physicalNetworkDao.findById(Mockito.anyLong())).thenReturn(Mockito.mock(PhysicalNetworkVO.class));
        DataCenterVO zone = Mockito.mock(DataCenterVO.class);
        Mockito.when(zone.getNetworkType()).thenReturn(DataCenter.NetworkType.Advanced);
        Mockito.when(dataCenterDao.findById(Mockito.anyLong())).thenReturn(zone);
        Mockito.when(networkOrchestrationService.finalizeServicesAndProvidersForNetwork(Mockito.any(), Mockito.anyLong())).thenReturn(new HashMap<>());
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

    @Test(expected = InvalidParameterValueException.class)
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

    @Test(expected = InvalidParameterValueException.class)
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
        Long offeringId = 1L;
        NetworkOffering offering = Mockito.mock(NetworkOffering.class);
        Mockito.when(offering.getId()).thenReturn(offeringId);
        Mockito.when(networkOfferingServiceMapDao.areServicesSupportedByNetworkOffering(offeringId, Network.Service.Dns)).thenReturn(true);
        boolean updated = service.checkAndUpdateNetworkDns(networkVO, offering, "", null, "", null);
        Assert.assertTrue(updated);
        Assert.assertNull(networkVO.getDns1());
        Assert.assertNull(networkVO.getDns2());
        Assert.assertNull(networkVO.getIp6Dns1());
        Assert.assertNull(networkVO.getIp6Dns2());
    }
}
