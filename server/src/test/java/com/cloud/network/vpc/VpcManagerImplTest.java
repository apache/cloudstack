/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.cloud.network.vpc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

import static org.powermock.api.mockito.PowerMockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;


import com.cloud.alert.AlertManager;
import com.cloud.network.NetworkService;
import org.apache.cloudstack.acl.SecurityChecker;
import org.apache.cloudstack.alert.AlertService;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.junit.After;
import org.junit.Assert;
import org.apache.cloudstack.api.command.admin.vpc.CreateVPCOfferingCmd;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.reflect.Whitebox;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.routing.UpdateNetworkCommand;
import com.cloud.agent.api.to.IpAddressTO;
import com.cloud.agent.manager.Commands;
import com.cloud.dc.VlanVO;
import com.cloud.dc.dao.VlanDao;
import com.cloud.configuration.Resource;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network;
import com.cloud.network.Network.Capability;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.NetworkModel;
import com.cloud.network.PhysicalNetwork;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.element.NetworkElement;
import com.cloud.network.router.CommandSetupHelper;
import com.cloud.network.router.NetworkHelper;
import com.cloud.network.router.VirtualRouter;
import com.cloud.network.vpc.dao.VpcDao;
import com.cloud.network.vpc.dao.VpcOfferingServiceMapDao;
import com.cloud.offering.NetworkOffering;
import com.cloud.offerings.NetworkOfferingServiceMapVO;
import com.cloud.offerings.dao.NetworkOfferingServiceMapDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.user.User;
import com.cloud.utils.Pair;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.net.Ip;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.NicVO;
import com.cloud.vm.dao.DomainRouterDao;
import com.cloud.vm.dao.NicDao;
import com.cloud.network.vpc.dao.VpcOfferingDao;
import com.cloud.user.ResourceLimitService;
import com.cloud.user.UserVO;
import com.cloud.utils.net.NetUtils;

public class VpcManagerImplTest {

    @Mock
    VpcOfferingServiceMapDao vpcOffSvcMapDao;
    @Mock
    VpcDao vpcDao;
    @Mock
    NetworkOrchestrationService networkMgr;
    @Mock
    AccountManager accountManager;
    @Mock
    ResourceLimitService resourceLimitService;
    @Mock
    VpcOfferingDao vpcOfferingDao;
    @Mock
    DataCenterDao dataCenterDao;
    @Mock
    VpcOfferingServiceMapDao vpcOfferingServiceMapDao;
    VpcManagerImpl manager;
    @Mock
    EntityManager entityMgr;
    @Mock
    NetworkDao networkDao;
    @Mock
    NetworkModel networkModel;
    @Mock
    NetworkOfferingServiceMapDao networkOfferingServiceMapDao;
    @Mock
    private CallContext callContextMock;
    @Mock
    IPAddressDao ipAddressDao;
    @Mock
    DomainRouterDao routerDao;
    @Mock
    CommandSetupHelper commandSetupHelper;
    @Mock
    NetworkHelper networkHelper;
    @Mock
    VlanDao vlanDao;
    @Mock
    NicDao nicDao;
    @Mock
    AlertManager alertManager;
    @Mock
    NetworkService networkServiceMock;

    public static final long ACCOUNT_ID = 1;
    private AccountVO account;
    private UserVO user;

    private static final String IP4_GATEWAY = "10.0.16.1";
    private static final String IP4_NETMASK = "255.255.255.0";
    private static final String IP6_GATEWAY = "fd17:ac56:1234:2000::1";
    private static final String IP6_CIDR = "fd17:ac56:1234:2000::/64";

    final String[] ip4Dns = {"5.5.5.5", "6.6.6.6"};
    final String[] ip6Dns = {"2001:4860:4860::5555", "2001:4860:4860::6666"};
    final String ip4Cidr = "172.16.0.0/22";
    final Long zoneId = 1L;
    final Long vpcOfferingId = 1L;
    final Long vpcOwnerId = 1L;
    final String vpcName = "Test-VPC";
    final String vpcDomain = "domain";

    private void registerCallContext() {
        account = new AccountVO("testaccount", 1L, "networkdomain", Account.Type.NORMAL, "uuid");
        account.setId(ACCOUNT_ID);
        user = new UserVO(1, "testuser", "password", "firstname", "lastName", "email", "timezone",
                UUID.randomUUID().toString(), User.Source.UNKNOWN);
        CallContext.register(user, account);
    }

    @Before
    public void setup()
    {
        MockitoAnnotations.initMocks(this);
        manager = new VpcManagerImpl();
        manager._vpcOffSvcMapDao = vpcOfferingServiceMapDao;
        manager.vpcDao = vpcDao;
        manager._ntwkMgr = networkMgr;
        manager._accountMgr = accountManager;
        manager._entityMgr = entityMgr;
        manager._ntwkDao = networkDao;
        manager._ntwkModel = networkModel;
        manager._ntwkOffServiceDao = networkOfferingServiceMapDao;
        manager._ipAddressDao = ipAddressDao;
        manager.routerDao = routerDao;
        manager.commandSetupHelper = commandSetupHelper;
        manager.networkHelper = networkHelper;
        manager._vlanDao = vlanDao;
        manager.nicDao = nicDao;
        manager.alertManager = alertManager;
        manager._resourceLimitMgr = resourceLimitService;
        manager._vpcOffDao = vpcOfferingDao;
        manager._dcDao = dataCenterDao;
        manager._ntwkSvc = networkServiceMock;
        CallContext.register(Mockito.mock(User.class), Mockito.mock(Account.class));
        registerCallContext();
    }

    @After
    public void tearDown() {
        CallContext.unregister();
    }
    @Test
    public void getVpcOffSvcProvidersMapForEmptyServiceTest() {
        long vpcOffId = 1L;
        List<VpcOfferingServiceMapVO> list = new ArrayList<VpcOfferingServiceMapVO>();
        list.add(mock(VpcOfferingServiceMapVO.class));
        when(manager._vpcOffSvcMapDao.listByVpcOffId(vpcOffId)).thenReturn(list);

        Map<Service, Set<Provider>> map = manager.getVpcOffSvcProvidersMap(vpcOffId);

        assertNotNull(map);
        assertEquals(map.size(),1);
    }

    protected Map<String, String> createFakeCapabilityInputMap() {
        Map<String, String> map = new HashMap<String, String>();
        map.put(VpcManagerImpl.CAPABILITYVALUE, VpcManagerImpl.TRUE_VALUE);
        map.put(VpcManagerImpl.CAPABILITYTYPE, Network.Capability.SupportedProtocols.getName());
        map.put(VpcManagerImpl.SERVICE, "");
        return map;
    }

    @Test
    public void testIsVpcOfferingForRegionLevelVpc() throws Exception {
        // Prepare
        Map<String, Map<String, String>> serviceCapabilitystList = new HashMap<>();
        // First add some other capabilities to be ignored
        serviceCapabilitystList.put("", createFakeCapabilityInputMap());

        Map<String, String> servicePair = new HashMap<>();
        servicePair.put(VpcManagerImpl.SERVICE, Service.Connectivity.getName());
        servicePair.put(VpcManagerImpl.CAPABILITYTYPE, Network.Capability.RegionLevelVpc.getName());
        servicePair.put(VpcManagerImpl.CAPABILITYVALUE, VpcManagerImpl.TRUE_VALUE);
        serviceCapabilitystList.put("", servicePair);


        // Execute
        boolean result = Whitebox.invokeMethod(this.manager, "isVpcOfferingForRegionLevelVpc",
                serviceCapabilitystList); //, Network.Capability.RedundantRouter.getName(), Service.SourceNat);

        // Assert
        assertEquals("VpcOffering should be created for Region Level Vpc", true, result);
    }

    @Test
    public void testIsVpcOfferingForRegionLevelVpcFalse() throws Exception {
        // Prepare
        Map<String, Map<String, String>> serviceCapabilitystList = new HashMap<>();
        // First add some other capabilities to be ignored
        serviceCapabilitystList.put("", createFakeCapabilityInputMap());
        serviceCapabilitystList.put("", createFakeCapabilityInputMap());

        // Execute
        boolean result = Whitebox.invokeMethod(this.manager, "isVpcOfferingForRegionLevelVpc",
                serviceCapabilitystList);

        // Assert
        assertEquals("VpcOffering should be created NOT for Region Level Vpc", false, result);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testCheckCapabilityPerServiceProviderFail() {
        // Prepare
        final Map<Capability, String> capabilities = new HashMap<>();
        capabilities.put(Capability.RegionLevelVpc, "");
        capabilities.put(Capability.DistributedRouter, "");
        Set<Network.Provider> providers = this.prepareVpcManagerForCheckingCapabilityPerService(Service.Connectivity, capabilities);

        // Execute
        this.manager.checkCapabilityPerServiceProvider(providers, Capability.RedundantRouter, Service.SourceNat);
    }

    @Test
    public void testCheckCapabilityPerServiceProvider() {
        // Prepare
        final Map<Capability, String> capabilities = new HashMap<>();
        capabilities.put(Capability.RegionLevelVpc, "");
        capabilities.put(Capability.DistributedRouter, "");
        Set<Network.Provider> providers = this.prepareVpcManagerForCheckingCapabilityPerService(Service.Connectivity, capabilities);

        // Execute
        this.manager.checkCapabilityPerServiceProvider(providers, Capability.DistributedRouter, Service.Connectivity);
        this.manager.checkCapabilityPerServiceProvider(providers, Capability.RegionLevelVpc, Service.Connectivity);
    }

    protected Set<Network.Provider> prepareVpcManagerForCheckingCapabilityPerService(Service service, Map<Capability, String> capabilities) {
        final Set<Network.Provider> providers = new HashSet<>();
        providers.add(Provider.VPCVirtualRouter);
        final Network.Capability capability = Capability.DistributedRouter;
        final boolean regionLevel = true;
        final boolean distributedRouter = true;
        final NetworkElement nwElement1 = mock(NetworkElement.class);
        this.manager._ntwkModel = mock(NetworkModel.class);
        when(this.manager._ntwkModel.getElementImplementingProvider(Provider.VPCVirtualRouter.getName()))
                .thenReturn(nwElement1);
        final Map<Service, Map<Network.Capability, String>> capabilitiesService1 = new HashMap<>();
        when(nwElement1.getCapabilities()).thenReturn(capabilitiesService1);
        capabilities.put(Capability.RegionLevelVpc, "");
        capabilities.put(Capability.DistributedRouter, "");
        capabilitiesService1.put(service, capabilities);

        return providers;
    }

    @Test
    public void testCreateVpcNetwork() throws InsufficientCapacityException, ResourceAllocationException {
        final long VPC_ID = 201L;
        manager._maxNetworks = 3;
        VpcVO vpcMockVO = Mockito.mock(VpcVO.class);
        Vpc vpcMock = Mockito.mock(Vpc.class);
        Account accountMock = Mockito.mock(Account.class);
        PhysicalNetwork physicalNetwork = Mockito.mock(PhysicalNetwork.class);
        NetworkOffering offering = Mockito.mock(NetworkOffering.class);
        List<Network.Service> services = new ArrayList<>();
        services.add(Service.SourceNat);
        List<NetworkOfferingServiceMapVO> serviceMap = new ArrayList<>();

        Mockito.when(vpcDao.findById(anyLong())).thenReturn(vpcMockVO);
        Mockito.when(manager.getActiveVpc(anyLong())).thenReturn(vpcMock);
        lenient().doNothing().when(accountManager).checkAccess(any(Account.class), nullable(SecurityChecker.AccessType.class), anyBoolean(), any(Vpc.class));
        Mockito.when(vpcMock.isRegionLevelVpc()).thenReturn(true);
        Mockito.when(entityMgr.findById(NetworkOffering.class, 1L)).thenReturn(offering);
        Mockito.when(vpcMock.getId()).thenReturn(VPC_ID);
        Mockito.when(vpcDao.acquireInLockTable(VPC_ID)).thenReturn(vpcMockVO);
        Mockito.when(networkDao.countVpcNetworks(anyLong())).thenReturn(1L);
        Mockito.when(offering.getGuestType()).thenReturn(Network.GuestType.Isolated);
        Mockito.when(networkModel.listNetworkOfferingServices(anyLong())).thenReturn(services);
        Mockito.when(networkOfferingServiceMapDao.listByNetworkOfferingId(anyLong())).thenReturn(serviceMap);
        Mockito.when(vpcMock.getCidr()).thenReturn("10.0.0.0/8");
        Mockito.when(vpcMock.getNetworkDomain()).thenReturn("cs1cloud.internal");

        manager.validateNewVpcGuestNetwork("10.10.10.0/24", "10.10.10.1", accountMock, vpcMock, "cs1cloud.internal");
        manager.validateNtwkOffForNtwkInVpc(2L, 1, "10.10.10.0/24", "111-", vpcMock, "10.1.1.1", new AccountVO(), null);
        manager.validateNtwkOffForVpc(offering, services);
        manager.createVpcGuestNetwork(1L, "vpcNet1", "vpc tier 1", null,
                "10.10.10.0/24", null, null, accountMock, null, physicalNetwork,
                1L, null, null, 1L, null, accountMock,
                true, null, null, null, null, null, null, null, new Pair<>(1000, 1000));

        Mockito.verify(networkMgr, times(1)).createGuestNetwork(1L, "vpcNet1", "vpc tier 1", null,
                "10.10.10.0/24", null, false, "cs1cloud.internal", accountMock, null,
                physicalNetwork, zoneId, null, null, 1L, null, null,
                true, null, null, null, null,
                null, null, null, null, null, new Pair<>(1000, 1000));
    }

    @Test
    public void testUpdateVpcNetwork() throws ResourceUnavailableException {
        long vpcId = 1L;
        Integer publicMtu = 1450;
        Account accountMock = Mockito.mock(Account.class);
        VpcVO vpcVO = new VpcVO();

        Answer answer = Mockito.mock(Answer.class);
        Mockito.when(answer.getResult()).thenReturn(true);
        VirtualRouter routerMock = Mockito.mock(VirtualRouter.class);
        List<IPAddressVO> ipAddresses = new ArrayList<>();
        IPAddressVO ipAddressVO = Mockito.mock(IPAddressVO.class);
        Mockito.when(ipAddressVO.getAddress()).thenReturn(Mockito.mock(Ip.class));
        ipAddresses.add(ipAddressVO);
        List<IpAddressTO> ips = new ArrayList<>();
        List<DomainRouterVO> routers = new ArrayList<>();
        DomainRouterVO router = Mockito.mock(DomainRouterVO.class);
        routers.add(router);

        IpAddressTO[] ipsToSend = ips.toArray(new IpAddressTO[0]);

        Mockito.when(callContextMock.getCallingAccount()).thenReturn(accountMock);
        Mockito.when(vpcDao.findById(vpcId)).thenReturn(vpcVO);
        Mockito.when(vpcDao.createForUpdate(anyLong())).thenReturn(vpcVO);
        Mockito.when(ipAddressDao.listByAssociatedVpc(anyLong(), nullable(Boolean.class))).thenReturn(ipAddresses);
        Mockito.when(routerDao.listByVpcId(anyLong())).thenReturn(routers);
        VlanVO vlanVO = Mockito.mock(VlanVO.class);
        Mockito.when(vlanVO.getVlanNetmask()).thenReturn("netmask");
        Mockito.when(vlanDao.findById(anyLong())).thenReturn(vlanVO);
        Mockito.doAnswer((org.mockito.stubbing.Answer<Void>) invocation -> {
            Commands commands = (Commands)invocation.getArguments()[2];
            commands.addCommand("updateNetwork", new UpdateNetworkCommand(ipsToSend));
            return null;
        }).when(commandSetupHelper).setupUpdateNetworkCommands(Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.doAnswer((org.mockito.stubbing.Answer<Boolean>) invocation -> {
            Commands commands = (Commands)invocation.getArguments()[1];
            commands.setAnswers(new Answer[]{answer});
            return true;
        }).when(networkHelper).sendCommandsToRouter(Mockito.any(), Mockito.any());
        Mockito.when(nicDao.findByIpAddressAndVmType(anyString(), any())).thenReturn(Mockito.mock(NicVO.class));
        Mockito.when(nicDao.update(anyLong(), any())).thenReturn(true);
        Mockito.when(networkDao.listByVpc(vpcId)).thenReturn(List.of(Mockito.mock(NetworkVO.class)));
        Mockito.when(networkDao.update(anyLong(), any())).thenReturn(true);
        Mockito.when(vpcDao.update(vpcId, vpcVO)).thenReturn(true);

        manager.updateVpc(vpcId, null, null, null, true, publicMtu);
        Assert.assertEquals(publicMtu, vpcVO.getPublicMtu());

    }

    @Test
    public void testUpdatePublicMtuToGreaterThanThreshold() {
        Integer publicMtu = 2500;
        Integer expectedMtu = 1500;
        Long vpcId = 1L;

        VpcVO vpcVO = new VpcVO();

        Mockito.when(vpcDao.findById(vpcId)).thenReturn(vpcVO);
        Mockito.when(vpcDao.createForUpdate(anyLong())).thenReturn(vpcVO);
        lenient().doNothing().when(alertManager).sendAlert(any(AlertService.AlertType.class), anyLong(), anyLong(), anyString(), anyString());
        Integer mtu = manager.validateMtu(vpcVO, publicMtu);
        Assert.assertEquals(expectedMtu, mtu);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testDisabledConfigCreateIpv6VpcOffering() {
        CreateVPCOfferingCmd cmd = Mockito.mock(CreateVPCOfferingCmd.class);
        Mockito.when(cmd.getInternetProtocol()).thenReturn(NetUtils.InternetProtocol.DualStack.toString());
        Mockito.doNothing().when(networkServiceMock).validateIfServiceOfferingIsActiveAndSystemVmTypeIsDomainRouter(Mockito.any());
        manager.createVpcOffering(cmd);
    }

    private void mockVpcDnsResources(boolean supportDnsService, boolean isIpv6) {
        Mockito.when(accountManager.getAccount(vpcOwnerId)).thenReturn(account);
        VpcOfferingVO vpcOfferingVO = Mockito.mock(VpcOfferingVO.class);
        Mockito.when(vpcOfferingVO.getId()).thenReturn(vpcOfferingId);
        Mockito.when(vpcOfferingVO.getState()).thenReturn(VpcOffering.State.Enabled);
        Mockito.when(vpcOfferingDao.findById(vpcOfferingId)).thenReturn(vpcOfferingVO);
        DataCenterVO dataCenterVO = Mockito.mock(DataCenterVO.class);
        Mockito.when(dataCenterVO.getId()).thenReturn(zoneId);
        Mockito.when(dataCenterDao.findById(zoneId)).thenReturn(dataCenterVO);
        Mockito.doNothing().when(accountManager).checkAccess(account, vpcOfferingVO, dataCenterVO);
        Mockito.when(vpcOfferingServiceMapDao.areServicesSupportedByVpcOffering(vpcOfferingId, new Service[]{Service.Dns})).thenReturn(supportDnsService);
        Mockito.when(vpcOfferingDao.isIpv6Supported(vpcOfferingId)).thenReturn(isIpv6);
        try {
            Mockito.doNothing().when(resourceLimitService).checkResourceLimit(account, Resource.ResourceType.vpc);
        } catch (ResourceAllocationException e) {
            Assert.fail(String.format("checkResourceLimit failure with exception: %s", e.getMessage()));
        }
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testCreateVpcDnsOfferingServiceFailure() {
        mockVpcDnsResources(false, false);
        try {
            Mockito.doNothing().when(resourceLimitService).checkResourceLimit(account, Resource.ResourceType.vpc);
            manager.createVpc(zoneId, vpcOfferingId, vpcOwnerId, vpcName, vpcName, ip4Cidr, vpcDomain,
                    ip4Dns[0], null, null, null, true, 1500);
        } catch (ResourceAllocationException e) {
            Assert.fail(String.format("failure with exception: %s", e.getMessage()));
        }
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testCreateVpcDnsIpv6OfferingFailure() {
        mockVpcDnsResources(true, false);
        try {
            Mockito.doNothing().when(resourceLimitService).checkResourceLimit(account, Resource.ResourceType.vpc);
            manager.createVpc(zoneId, vpcOfferingId, vpcOwnerId, vpcName, vpcName, ip4Cidr, vpcDomain,
                    ip4Dns[0], ip4Dns[1], ip6Dns[0], null, true, 1500);
        } catch (ResourceAllocationException e) {
            Assert.fail(String.format("failure with exception: %s", e.getMessage()));
        }
    }
}
