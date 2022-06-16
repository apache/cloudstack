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

import java.util.HashMap;
import java.util.UUID;

import org.apache.cloudstack.api.command.user.network.CreateNetworkCmd;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.dao.PhysicalNetworkVO;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.offerings.dao.NetworkOfferingServiceMapDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.user.User;
import com.cloud.user.UserVO;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.exception.CloudRuntimeException;

@RunWith(PowerMockRunner.class)
@PrepareForTest(ComponentContext.class)
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

    private void registerCallContext() {
        account = new AccountVO("testaccount", 1L, "networkdomain", Account.Type.NORMAL, "uuid");
        account.setId(ACCOUNT_ID);
        user = new UserVO(1, "testuser", "password", "firstname", "lastName", "email", "timezone",
                UUID.randomUUID().toString(), User.Source.UNKNOWN);
        CallContext.register(user, account);
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

    private void prepareCreateNetworkDnsMocks(CreateNetworkCmd cmd, Network.GuestType guestType, boolean ipv6, boolean isVpc, boolean dnsServiceSupported) {
        long networkOfferingId = 1L;
        Mockito.when(cmd.getNetworkOfferingId()).thenReturn(networkOfferingId);
        NetworkOfferingVO networkOfferingVO = Mockito.mock(NetworkOfferingVO.class);
        Mockito.when(networkOfferingVO.getId()).thenReturn(networkOfferingId);
        Mockito.when(networkOfferingVO.getGuestType()).thenReturn(guestType);
        Mockito.when(networkOfferingVO.getTrafficType()).thenReturn(Networks.TrafficType.Guest);
        Mockito.when(networkOfferingVO.isSpecifyIpRanges()).thenReturn(true);
        Mockito.when(networkOfferingDao.findById(networkOfferingId)).thenReturn(networkOfferingVO);
        Mockito.when(accountManager.finalizeOwner(Mockito.any(), Mockito.any(), Mockito.anyLong(), Mockito.anyLong())).thenReturn(account);
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

    @Test(expected = InvalidParameterValueException.class)
    public void testCreateNetworkDnsVpcFailure() {
        registerCallContext();
        CreateNetworkCmd cmd = Mockito.mock(CreateNetworkCmd.class);
        prepareCreateNetworkDnsMocks(cmd, Network.GuestType.Isolated, false, true, true);
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
        prepareCreateNetworkDnsMocks(cmd, Network.GuestType.Isolated, false, false, false);
        Mockito.when(cmd.getIp4Dns1()).thenReturn(ip4Dns[0]);
        try {
            service.createGuestNetwork(cmd);
        } catch (InsufficientCapacityException | ResourceAllocationException e) {
            Assert.fail(String.format("failure with exception: %s", e.getMessage()));
        }
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testCreateNetworkIp4Dns1Failure() {
        registerCallContext();
        CreateNetworkCmd cmd = Mockito.mock(CreateNetworkCmd.class);
        prepareCreateNetworkDnsMocks(cmd, Network.GuestType.Isolated, false, false, true);
        Mockito.when(cmd.getIp4Dns1()).thenReturn(null);
        Mockito.when(cmd.getIp4Dns2()).thenReturn(ip4Dns[1]);
        try {
            service.createGuestNetwork(cmd);
        } catch (InsufficientCapacityException | ResourceAllocationException e) {
            Assert.fail(String.format("failure with exception: %s", e.getMessage()));
        }
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testCreateNetworkIp6Dns1Failure() {
        registerCallContext();
        CreateNetworkCmd cmd = Mockito.mock(CreateNetworkCmd.class);
        prepareCreateNetworkDnsMocks(cmd, Network.GuestType.Isolated, true, false, true);
        Mockito.when(cmd.getIp6Dns1()).thenReturn(null);
        Mockito.when(cmd.getIp6Dns2()).thenReturn(ip6Dns[0]);
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

    @Test(expected = InvalidParameterValueException.class)
    public void testCreateSharedNetworkIp6Dns1Failure() {
        registerCallContext();
        CreateNetworkCmd cmd = Mockito.mock(CreateNetworkCmd.class);
        prepareCreateNetworkDnsMocks(cmd, Network.GuestType.Shared, false, false, true);
        Mockito.when(cmd.getIp6Dns1()).thenReturn(null);
        Mockito.when(cmd.getIp6Cidr()).thenReturn(IP6_CIDR);
        Mockito.when(cmd.getIp6Gateway()).thenReturn(IP6_GATEWAY);
        Mockito.when(cmd.getIp6Dns2()).thenReturn(ip6Dns[1]);
        try {
            service.createGuestNetwork(cmd);
        } catch (InsufficientCapacityException | ResourceAllocationException e) {
            Assert.fail(String.format("failure with exception: %s", e.getMessage()));
        }
    }
}
