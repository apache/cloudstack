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

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.routing.UpdateNetworkCommand;
import com.cloud.agent.api.to.IpAddressTO;
import com.cloud.agent.manager.Commands;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.NetworkModel;
import com.cloud.network.PhysicalNetwork;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.element.NetworkElement;

import com.cloud.network.Network;
import com.cloud.network.Network.Capability;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.cloud.network.router.CommandSetupHelper;
import com.cloud.network.router.NetworkHelper;
import com.cloud.network.router.VirtualRouter;
import com.cloud.network.vpc.dao.VpcDao;
import com.cloud.offering.NetworkOffering;
import com.cloud.offerings.NetworkOfferingServiceMapVO;
import com.cloud.offerings.dao.NetworkOfferingServiceMapDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.user.User;
import com.cloud.utils.Pair;
import com.cloud.utils.db.EntityManager;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.dao.DomainRouterDao;
import org.apache.cloudstack.acl.ControlledEntity;
import org.apache.cloudstack.acl.SecurityChecker;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.apache.cloudstack.framework.events.Event;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.vpc.dao.VpcOfferingServiceMapDao;
import org.mockito.invocation.InvocationOnMock;
import org.powermock.reflect.Whitebox;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.powermock.api.mockito.PowerMockito.when;

public class VpcManagerImplTest {

    @Mock
    VpcOfferingServiceMapDao vpcOffSvcMapDao;
    @Mock
    VpcDao vpcDao;
    @Mock
    NetworkOrchestrationService networkMgr;
    @Mock
    AccountManager accountManager;
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

    @Before
    public void setup()
    {
        MockitoAnnotations.initMocks(this);
        manager = new VpcManagerImpl();
        manager._vpcOffSvcMapDao = vpcOffSvcMapDao;
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
        CallContext.register(Mockito.mock(User.class), Mockito.mock(Account.class));
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
                true, null, null, null, new Pair<>(1000, 1000));
        Mockito.verify(networkMgr, times(1)).createGuestNetwork(anyLong(), anyString(), anyString(), nullable(String.class),
                nullable(String.class), nullable(String.class), anyBoolean(), nullable(String.class), any(Account.class), nullable(Long.class), any(PhysicalNetwork.class),
                anyLong(), nullable(ControlledEntity.ACLType.class), nullable(Boolean.class), nullable(Long.class), nullable(String.class), nullable(String.class),
                anyBoolean(), nullable(String.class), nullable(Network.PVlanType.class), nullable(String.class), nullable(String.class), nullable(String.class), any(Pair.class));
    }

    @Test
    public void testUpdateVpcNetwork() throws ResourceUnavailableException {
        long vpcId = 1L;
        Integer publicMtu = 1450;
        Account accountMock = Mockito.mock(Account.class);
        VpcVO vpcVO = Mockito.mock(VpcVO.class);
        Commands cmds = Mockito.mock(Commands.class);

        Answer answer = Mockito.mock(Answer.class);
        VirtualRouter routerMock = Mockito.mock(VirtualRouter.class);
        List<IPAddressVO> ipAddresses = new ArrayList<>();
        List<IpAddressTO> ips = new ArrayList<>();
        List<DomainRouterVO> routers = new ArrayList<>();
        DomainRouterVO router = Mockito.mock(DomainRouterVO.class);
        routers.add(router);

        IpAddressTO[] ipsToSend = ips.toArray(new IpAddressTO[0]);

        Mockito.when(callContextMock.getCallingAccount()).thenReturn(accountMock);
        Mockito.when(vpcDao.findById(anyLong())).thenReturn(vpcVO);
        Mockito.when(vpcDao.createForUpdate(anyLong())).thenReturn(vpcVO);
        Mockito.when(ipAddressDao.listByAssociatedVpc(anyLong(), nullable(Boolean.class))).thenReturn(ipAddresses);
        Mockito.when(routerDao.listByVpcId(anyLong())).thenReturn(routers);
        Mockito.when(cmds.getAnswer("updateNetwork")).thenReturn(answer);
        Mockito.doAnswer(new org.mockito.stubbing.Answer<Commands>() {
                    @Override
                    public Commands answer(InvocationOnMock invocation) throws Throwable {
                        cmds.addCommand(new UpdateNetworkCommand(ipsToSend));
                        return cmds;
                    }
                }).when(commandSetupHelper).setupUpdateNetworkCommands(routerMock, ips, cmds);
        Mockito.when(networkHelper.sendCommandsToRouter(routerMock, cmds)).thenReturn(true);

        boolean result = manager.updateMtuOnVpcVr(1L, ips);
        manager.updateVpc(vpcId, null, null, null, true, publicMtu);

    }
}
