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
import static org.mockito.Mockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.cloudstack.api.command.admin.vpc.CreateVPCOfferingCmd;
import org.apache.cloudstack.context.CallContext;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.reflect.Whitebox;

import com.cloud.configuration.Resource;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.network.Network;
import com.cloud.network.Network.Capability;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.NetworkModel;
import com.cloud.network.element.NetworkElement;
import com.cloud.network.vpc.dao.VpcOfferingDao;
import com.cloud.network.vpc.dao.VpcOfferingServiceMapDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.user.ResourceLimitService;
import com.cloud.user.User;
import com.cloud.user.UserVO;
import com.cloud.utils.net.NetUtils;


public class VpcManagerImplTest {

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
        manager._accountMgr = accountManager;
        manager._resourceLimitMgr = resourceLimitService;
        manager._vpcOffDao = vpcOfferingDao;
        manager._dcDao = dataCenterDao;
        manager._vpcOffSvcMapDao = vpcOfferingServiceMapDao;
        registerCallContext();
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

    @Test(expected = InvalidParameterValueException.class)
    public void testDisabledConfigCreateIpv6VpcOffering() {
        CreateVPCOfferingCmd cmd = Mockito.mock(CreateVPCOfferingCmd.class);
        Mockito.when(cmd.getInternetProtocol()).thenReturn(NetUtils.InternetProtocol.DualStack.toString());
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
                    ip4Dns[0], null, null, null, true);
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
                    ip4Dns[0], ip4Dns[1], ip6Dns[0], null, true);
        } catch (ResourceAllocationException e) {
            Assert.fail(String.format("failure with exception: %s", e.getMessage()));
        }
    }
}
