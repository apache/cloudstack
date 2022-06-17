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
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.exception.InsufficientCapacityException;
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
import com.cloud.org.Grouping;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountManagerImpl;
import com.cloud.user.AccountService;
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
import org.apache.cloudstack.acl.ControlledEntity;
import org.apache.cloudstack.alert.AlertService;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.command.user.network.CreateNetworkCmd;
import org.apache.cloudstack.api.command.user.network.UpdateNetworkCmd;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest(CallContext.class)
@PowerMockIgnore({"javax.xml.*", "org.w3c.dom.*", "org.apache.xerces.*", "org.xml.*", "javax.management.*"})
public class NetworkServiceImplTest {

    private NetworkServiceImpl service = new NetworkServiceImpl();
    private static final String VLAN_ID_900 = "900";
    private static final String VLAN_ID_901 = "901";
    private static final String VLAN_ID_902 = "902";

    private NetworkOfferingVO offering;
    private DataCenterVO dc;
    private Network network;
    private  PhysicalNetworkVO phyNet;
    private VpcVO vpc;

    @Mock
    NetworkOfferingDao networkOfferingDao;
    @Mock
    AccountManager accountMgr;
    @Mock
    PhysicalNetworkDao physicalNetworkDao;
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

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        CallContext.register(Mockito.mock(User.class), Mockito.mock(Account.class));
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
        Account accountMock = PowerMockito.mock(Account.class);
        PowerMockito.when(callContextMock.getCallingAccount()).thenReturn(accountMock);
        NetworkOffering networkOffering = Mockito.mock(NetworkOffering.class);
        Mockito.when(entityMgr.findById(NetworkOffering.class, 1L)).thenReturn(networkOffering);
        Mockito.when(networkService.findPhysicalNetworkId(Mockito.anyLong(), nullable(String.class), Mockito.any(Networks.TrafficType.class))).thenReturn(1L);
        Mockito.when(networkOfferingDao.findById(1L)).thenReturn(offering);
        Mockito.when(physicalNetworkDao.findById(Mockito.anyLong())).thenReturn(phyNet);
        Mockito.when(dcDao.findById(Mockito.anyLong())).thenReturn(dc);
        lenient().doNothing().when(accountMgr).checkAccess(accountMock, networkOffering, dc);
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
        Mockito.when(networkManager.finalizeServicesAndProvidersForNetwork(any(NetworkOffering.class), anyLong())).thenReturn(networkProvidersMap);
        lenient().doNothing().when(alertManager).sendAlert(any(AlertService.AlertType.class), anyLong(), anyLong(), anyString(), anyString());
        Mockito.when(configMgr.isOfferingForVpc(offering)).thenReturn(false);
        Mockito.when(offering.isInternalLb()).thenReturn(false);
        Mockito.when(networkManager.createGuestNetwork(anyLong(), anyString(), anyString(), anyString(), anyString(), anyString(),
        anyBoolean(), anyString(), any(Account.class), anyLong(), any(PhysicalNetworkVO.class), anyLong(), any(ControlledEntity.ACLType.class),
        anyBoolean(), anyLong(), anyString(), anyString(), anyBoolean(), anyString(), any(Network.PVlanType.class), anyString(),
                anyString(), anyString(), any(Pair.class))).thenReturn(network);
        service.createGuestNetwork(createNetworkCmd);

        Mockito.verify(networkManager, times(1)).createGuestNetwork(anyLong(), anyString(), anyString(), nullable(String.class),
                nullable(String.class), nullable(String.class), anyBoolean(), nullable(String.class), any(Account.class), nullable(Long.class), any(PhysicalNetwork.class),
                anyLong(), nullable(ControlledEntity.ACLType.class), nullable(Boolean.class), nullable(Long.class), nullable(String.class), nullable(String.class),
                anyBoolean(), nullable(String.class), nullable(Network.PVlanType.class), nullable(String.class), nullable(String.class), nullable(String.class), any(Pair.class));
    }
    @Test
    public void testValidateMtuConfigWhenMtusExceedThreshold() {
        Integer publicMtu = 2450;
        Integer privateMtu = 1500;
        Long zoneId = 1L;
        when(publicMtuKey.valueIn(zoneId)).thenReturn(ApiConstants.DEFAULT_MTU);
        when(privateMtuKey.valueIn(zoneId)).thenReturn(ApiConstants.DEFAULT_MTU);
        Pair<Integer, Integer> interfaceMtus = service.validateMtuConfig(publicMtu, privateMtu, zoneId);
        Assert.assertNotNull(interfaceMtus);
        Assert.assertEquals(ApiConstants.DEFAULT_MTU, interfaceMtus.first());
        Assert.assertEquals(ApiConstants.DEFAULT_MTU, interfaceMtus.second());
        Mockito.verify(alertManager, Mockito.times(1)).sendAlert(Mockito.any(AlertService.AlertType.class),
                Mockito.anyLong(), nullable(Long.class), Mockito.anyString(), Mockito.anyString());
    }

    @Test
    public void testValidatePrivateMtuExceedingThreshold() {
        Integer publicMtu = 1500;
        Integer privateMtu = 2500;
        Long zoneId = 1L;
        when(publicMtuKey.valueIn(zoneId)).thenReturn(ApiConstants.DEFAULT_MTU);
        when(privateMtuKey.valueIn(zoneId)).thenReturn(ApiConstants.DEFAULT_MTU);
        Pair<Integer, Integer> interfaceMtus = service.validateMtuConfig(publicMtu, privateMtu, zoneId);
        Assert.assertNotNull(interfaceMtus);
        Assert.assertEquals(ApiConstants.DEFAULT_MTU, interfaceMtus.first());
        Assert.assertEquals(ApiConstants.DEFAULT_MTU, interfaceMtus.second());
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
        Mockito.when(vpcMgr.createVpcGuestNetwork(anyLong(), anyString(), anyString(), anyString(),
                anyString(), anyString(), anyString(), any(Account.class), anyLong(), any(PhysicalNetwork.class),
                anyLong(), any(ControlledEntity.ACLType.class), anyBoolean(), anyLong(), anyLong(), any(Account.class),
                anyBoolean(), anyString(), anyString(), anyString(), any(Pair.class))).thenReturn(network);

        service.createGuestNetwork(createNetworkCmd);
        Mockito.verify(vpcMgr, times(1)).createVpcGuestNetwork(anyLong(), anyString(), anyString(), nullable(String.class),
                nullable(String.class), nullable(String.class), nullable(String.class), any(Account.class), nullable(Long.class), any(PhysicalNetwork.class),
                anyLong(), nullable(ControlledEntity.ACLType.class), nullable(Boolean.class), anyLong(), nullable(Long.class), any(Account.class),
                anyBoolean(), nullable(String.class), nullable(String.class), nullable(String.class), any(Pair.class));

    }

    @Test
    public void testUpdateSharedNetworkMtus() {
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
        Assert.assertNull(updatedMtus.second());
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
}
