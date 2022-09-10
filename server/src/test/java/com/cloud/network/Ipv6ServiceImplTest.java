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

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;

import org.apache.cloudstack.api.command.user.ipv6.CreateIpv6FirewallRuleCmd;
import org.apache.cloudstack.api.command.user.ipv6.UpdateIpv6FirewallRuleCmd;
import org.apache.cloudstack.api.response.Ipv6RouteResponse;
import org.apache.cloudstack.api.response.VpcResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.apache.commons.collections.CollectionUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.cloud.api.ApiDBUtils;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenterGuestIpv6PrefixVO;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.Vlan;
import com.cloud.dc.VlanVO;
import com.cloud.dc.dao.DataCenterGuestIpv6PrefixDao;
import com.cloud.dc.dao.VlanDao;
import com.cloud.event.ActionEventUtils;
import com.cloud.event.UsageEventUtils;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.network.dao.FirewallRulesDao;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.Ipv6GuestPrefixSubnetNetworkMapDao;
import com.cloud.network.dao.NetworkDetailsDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.firewall.FirewallService;
import com.cloud.network.guru.PublicNetworkGuru;
import com.cloud.network.rules.FirewallManager;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.FirewallRuleVO;
import com.cloud.network.vpc.Vpc;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.user.User;
import com.cloud.user.UserVO;
import com.cloud.utils.Pair;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.Nic;
import com.cloud.vm.NicProfile;
import com.cloud.vm.NicVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.DomainRouterDao;
import com.cloud.vm.dao.NicDao;
import com.googlecode.ipv6.IPv6Network;
import com.googlecode.ipv6.IPv6NetworkMask;

@PowerMockIgnore("javax.management.*")
@RunWith(PowerMockRunner.class)
@PrepareForTest({ApiDBUtils.class, ActionEventUtils.class, UsageEventUtils.class})
public class Ipv6ServiceImplTest {

    @Mock
    NetworkOfferingDao networkOfferingDao;
    @Mock
    VlanDao vlanDao;
    @Mock
    DataCenterGuestIpv6PrefixDao dataCenterGuestIpv6PrefixDao;
    @Mock
    Ipv6GuestPrefixSubnetNetworkMapDao ipv6GuestPrefixSubnetNetworkMapDao;
    @Mock
    FirewallRulesDao firewallDao;
    @Mock
    FirewallService firewallService;
    @Mock
    NetworkDetailsDao networkDetailsDao;
    @Mock
    NicDao nicDao;
    @Mock
    DomainRouterDao domainRouterDao;
    @Mock
    AccountManager accountManager;
    NetworkModel networkModel = Mockito.mock(NetworkModelImpl.class);
    @Mock
    IPAddressDao ipAddressDao;
    @Mock
    NetworkOrchestrationService networkOrchestrationService;
    FirewallManager firewallManager = Mockito.mock(FirewallManager.class);

    @InjectMocks
    private Ipv6ServiceImpl ipv6Service = new Ipv6ServiceImpl();

    List<Ipv6GuestPrefixSubnetNetworkMapVO> updatedPrefixSubnetMap;

    List<Ipv6GuestPrefixSubnetNetworkMapVO> persistedPrefixSubnetMap;

    final String publicReserver = PublicNetworkGuru.class.getSimpleName();
    final String vlan = "vlan";
    final Long networkId = 101L;
    final Long nicId = 100L;
    final String ipv6Prefix = "fd17:6:8a43:e2a4::/62"; // Will have 4 /64 subnets
    final String cidr = "fd17:5:8a43:e2a5::/64";
    final String gateway = "fd17:5:8a43:e2a5::1";
    final String macAddress = "1e:00:4c:00:00:03";
    final String ipv6Address = "fd17:5:8a43:e2a5:1c00:4cff:fe00:3"; // Resulting  IPv6 address using SLAAC
    final Pair<String, String> ipv6DnsPair = new Pair<>("2001:db8::53:1", "2001:db8::53:2");
    public static final long ACCOUNT_ID = 1;

    private AccountVO account;
    private UserVO user;

    @Before
    public void setup() {
        updatedPrefixSubnetMap = new ArrayList<>();
        persistedPrefixSubnetMap = new ArrayList<>();
        MockitoAnnotations.initMocks(this);
        ipv6Service.firewallManager = firewallManager;
        Mockito.when(ipv6GuestPrefixSubnetNetworkMapDao.update(Mockito.anyLong(), Mockito.any(Ipv6GuestPrefixSubnetNetworkMapVO.class))).thenAnswer((Answer<Boolean>) invocation -> {
            Ipv6GuestPrefixSubnetNetworkMapVO map = (Ipv6GuestPrefixSubnetNetworkMapVO)invocation.getArguments()[1];
            updatedPrefixSubnetMap.add(map);
            return true;
        });
        Mockito.when(ipv6GuestPrefixSubnetNetworkMapDao.persist(Mockito.any(Ipv6GuestPrefixSubnetNetworkMapVO.class))).thenAnswer((Answer<Ipv6GuestPrefixSubnetNetworkMapVO>) invocation -> {
            Ipv6GuestPrefixSubnetNetworkMapVO map = (Ipv6GuestPrefixSubnetNetworkMapVO)invocation.getArguments()[0];
            persistedPrefixSubnetMap.add(map);
            return map;
        });
        PowerMockito.mockStatic(ApiDBUtils.class);
        Mockito.when(ApiDBUtils.findZoneById(Mockito.anyLong())).thenReturn(Mockito.mock(DataCenterVO.class));
    }

    private DataCenterGuestIpv6PrefixVO prepareMocksForIpv6Subnet() {
        final long prefixId = 1L;
        DataCenterGuestIpv6PrefixVO prefix = Mockito.mock(DataCenterGuestIpv6PrefixVO.class);
        Mockito.when(prefix.getId()).thenReturn(prefixId);
        Mockito.when(prefix.getPrefix()).thenReturn(ipv6Prefix);
        List<Ipv6GuestPrefixSubnetNetworkMapVO> subnets = new ArrayList<>();
        Ipv6GuestPrefixSubnetNetworkMapVO subnetMap = new Ipv6GuestPrefixSubnetNetworkMapVO(prefixId, "subnet", 1L, Ipv6GuestPrefixSubnetNetworkMap.State.Allocated);
        subnets.add(subnetMap);
        subnetMap = new Ipv6GuestPrefixSubnetNetworkMapVO(1L, "subnet", 2L, Ipv6GuestPrefixSubnetNetworkMap.State.Allocated);
        subnets.add(subnetMap);
        Mockito.when(ipv6GuestPrefixSubnetNetworkMapDao.listUsedByPrefix(prefixId)).thenReturn(subnets);
        return prefix;
    }

    @Test
    public void testGetUsedTotalIpv6SubnetForPrefix() {
        DataCenterGuestIpv6PrefixVO prefix = prepareMocksForIpv6Subnet();
        Pair<Integer, Integer> results = ipv6Service.getUsedTotalIpv6SubnetForPrefix(prefix);
        Assert.assertEquals(2, results.first().intValue());
        Assert.assertEquals(4, results.second().intValue());
    }

    @Test
    public void testNoPrefixesGetUsedTotalIpv6SubnetForZone() {
        final long zoneId = 1L;
        final List<DataCenterGuestIpv6PrefixVO> prefixes = new ArrayList<>();
        Mockito.when(dataCenterGuestIpv6PrefixDao.listByDataCenterId(zoneId)).thenReturn(prefixes);
        Pair<Integer, Integer> results = ipv6Service.getUsedTotalIpv6SubnetForZone(zoneId);
        Assert.assertEquals(0, results.first().intValue());
        Assert.assertEquals(0, results.second().intValue());
    }

    @Test
    public void testGetUsedTotalIpv6SubnetForZone() {
        final long zoneId = 1L;
        final List<DataCenterGuestIpv6PrefixVO> prefixes = new ArrayList<>();
        DataCenterGuestIpv6PrefixVO prefix = prepareMocksForIpv6Subnet();
        prefixes.add(prefix);
        prefixes.add(prefix);
        Mockito.when(dataCenterGuestIpv6PrefixDao.listByDataCenterId(zoneId)).thenReturn(prefixes);
        Pair<Integer, Integer> results = ipv6Service.getUsedTotalIpv6SubnetForZone(zoneId);
        Assert.assertEquals(4, results.first().intValue());
        Assert.assertEquals(8, results.second().intValue());
    }

    @Test(expected = ResourceAllocationException.class)
    @DB
    public void testNoPrefixesPreAllocateIpv6SubnetForNetwork() throws ResourceAllocationException, MalformedObjectNameException, NotCompliantMBeanException, InstanceAlreadyExistsException, MBeanRegistrationException {
        final long zoneId = 1L;
        final List<DataCenterGuestIpv6PrefixVO> prefixes = new ArrayList<>();
        Mockito.when(dataCenterGuestIpv6PrefixDao.listByDataCenterId(zoneId)).thenReturn(prefixes);
        TransactionLegacy txn = TransactionLegacy.open("testNoPrefixesPreAllocateIpv6SubnetForNetwork");
        try {
            ipv6Service.preAllocateIpv6SubnetForNetwork(zoneId);
        } finally {
            txn.close("testNoPrefixesPreAllocateIpv6SubnetForNetwork");
        }
    }

    @Test
    @DB
    public void testExistingPreAllocateIpv6SubnetForNetwork() {
        final long zoneId = 1L;
        final List<DataCenterGuestIpv6PrefixVO> prefixes = new ArrayList<>();
        DataCenterGuestIpv6PrefixVO prefix = prepareMocksForIpv6Subnet();
        prefixes.add(prefix);
        Ipv6GuestPrefixSubnetNetworkMapVO ipv6GuestPrefixSubnetNetworkMap = new Ipv6GuestPrefixSubnetNetworkMapVO(1L, "fd17:5:8a43:e2a4::/64", null, Ipv6GuestPrefixSubnetNetworkMap.State.Free);
        Mockito.when(dataCenterGuestIpv6PrefixDao.listByDataCenterId(zoneId)).thenReturn(prefixes);
        Mockito.when(ipv6GuestPrefixSubnetNetworkMapDao.findFirstAvailable(prefix.getId())).thenReturn(ipv6GuestPrefixSubnetNetworkMap);
        updatedPrefixSubnetMap.clear();
        try (TransactionLegacy txn = TransactionLegacy.open("testNoPrefixesPreAllocateIpv6SubnetForNetwork")) {
            try {
                ipv6Service.preAllocateIpv6SubnetForNetwork(zoneId);
            } catch (ResourceAllocationException e) {
                Assert.fail("ResourceAllocationException");
            }
        }
        Assert.assertEquals(1, updatedPrefixSubnetMap.size());
        Ipv6GuestPrefixSubnetNetworkMapVO map = updatedPrefixSubnetMap.get(0);
        Assert.assertEquals(Ipv6GuestPrefixSubnetNetworkMap.State.Allocating, map.getState());
        Assert.assertEquals(ipv6GuestPrefixSubnetNetworkMap.getSubnet(), map.getSubnet());
        Assert.assertEquals(ipv6GuestPrefixSubnetNetworkMap.getPrefixId(), map.getPrefixId());
        Assert.assertNull(map.getNetworkId());
    }

    @Test
    @DB
    public void testNewPreAllocateIpv6SubnetForNetwork() {
        final long zoneId = 1L;
        final List<DataCenterGuestIpv6PrefixVO> prefixes = new ArrayList<>();
        DataCenterGuestIpv6PrefixVO prefix = prepareMocksForIpv6Subnet();
        final IPv6Network ip6Prefix = IPv6Network.fromString(prefix.getPrefix());
        Iterator<IPv6Network> splits = ip6Prefix.split(IPv6NetworkMask.fromPrefixLength(Ipv6Service.IPV6_SLAAC_CIDR_NETMASK));
        List<String> subnets = new ArrayList<>();
        while(splits.hasNext()) {
            subnets.add(splits.next().toString());
        }
        prefixes.add(prefix);
        Mockito.when(dataCenterGuestIpv6PrefixDao.listByDataCenterId(zoneId)).thenReturn(prefixes);
        Mockito.when(ipv6GuestPrefixSubnetNetworkMapDao.findFirstAvailable(prefix.getId())).thenReturn(null);
        Mockito.when(ipv6GuestPrefixSubnetNetworkMapDao.listUsedByPrefix(prefix.getId())).thenReturn(new ArrayList<>());
        persistedPrefixSubnetMap.clear();
        // No subnet is used from the prefix, should allocate any subnet
        try (TransactionLegacy txn = TransactionLegacy.open("testNewPreAllocateIpv6SubnetForNetwork")) {
            try {
                ipv6Service.preAllocateIpv6SubnetForNetwork(zoneId);
            } catch (ResourceAllocationException e) {
                Assert.fail("ResourceAllocationException");
            }
        }
        Assert.assertEquals(1, persistedPrefixSubnetMap.size());
        Ipv6GuestPrefixSubnetNetworkMapVO map = persistedPrefixSubnetMap.get(0);
        Assert.assertEquals(Ipv6GuestPrefixSubnetNetworkMap.State.Allocating, map.getState());
        Assert.assertTrue(subnets.contains(map.getSubnet()));
        Assert.assertEquals(prefix.getId(), map.getPrefixId());
        Assert.assertNull(map.getNetworkId());
        List<Ipv6GuestPrefixSubnetNetworkMapVO> usedSubnets = new ArrayList<>();
        for (String subnet : subnets) {
            usedSubnets.add(new Ipv6GuestPrefixSubnetNetworkMapVO(prefix.getId(), subnet, 1L, Ipv6GuestPrefixSubnetNetworkMap.State.Allocated));
        }
        Mockito.when(ipv6GuestPrefixSubnetNetworkMapDao.listUsedByPrefix(prefix.getId())).thenReturn(usedSubnets);

        // All subnets from the prefix are already in use, should return ResourceAllocationException
        try (TransactionLegacy txn = TransactionLegacy.open("testNewPreAllocateIpv6SubnetForNetwork")) {
            try {
                ipv6Service.preAllocateIpv6SubnetForNetwork(zoneId);
                Assert.fail("ResourceAllocationException expected but not returned");
            } catch (ResourceAllocationException ignored) {}
        }
        persistedPrefixSubnetMap.clear();

        // 3 out of 4 subnet from the prefix are in use, should return the remaining one
        Ipv6GuestPrefixSubnetNetworkMapVO poppedUsedSubnetMap = usedSubnets.remove(2);
        try (TransactionLegacy txn = TransactionLegacy.open("testNewPreAllocateIpv6SubnetForNetwork")) {
            try {
                ipv6Service.preAllocateIpv6SubnetForNetwork(zoneId);
            } catch (ResourceAllocationException e) {
                Assert.fail("ResourceAllocationException");
            }
        }
        Assert.assertEquals(1, persistedPrefixSubnetMap.size());
        map = persistedPrefixSubnetMap.get(0);
        Assert.assertEquals(Ipv6GuestPrefixSubnetNetworkMap.State.Allocating, map.getState());
        Assert.assertEquals(poppedUsedSubnetMap.getSubnet(), map.getSubnet());
        Assert.assertEquals(prefix.getId(), map.getPrefixId());
        Assert.assertNull(map.getNetworkId());
    }

    @Test
    @DB
    public void testAssignIpv6SubnetToNetwork() {
        final long prefixId = 1L;
        final String subnet = "fd17:5:8a43:e2a5::/64";
        final Long networkId = 100L;
        Ipv6GuestPrefixSubnetNetworkMapVO allocatingMap = new Ipv6GuestPrefixSubnetNetworkMapVO(prefixId, subnet, null, Ipv6GuestPrefixSubnetNetworkMap.State.Allocating);
        Mockito.when(ipv6GuestPrefixSubnetNetworkMapDao.findBySubnet(subnet)).thenReturn(allocatingMap);
        Mockito.when(ipv6GuestPrefixSubnetNetworkMapDao.createForUpdate(Mockito.anyLong())).thenReturn(allocatingMap);
        updatedPrefixSubnetMap.clear();
        try (TransactionLegacy txn = TransactionLegacy.open("testNewPreAllocateIpv6SubnetForNetwork")) {
            ipv6Service.assignIpv6SubnetToNetwork(subnet, networkId);
        }
        Assert.assertEquals(1, updatedPrefixSubnetMap.size());
        Ipv6GuestPrefixSubnetNetworkMapVO map = updatedPrefixSubnetMap.get(0);
        Assert.assertEquals(Ipv6GuestPrefixSubnetNetworkMap.State.Allocated, map.getState());
        Assert.assertEquals(subnet, map.getSubnet());
        Assert.assertEquals(prefixId, map.getPrefixId());
        Assert.assertEquals(networkId, map.getNetworkId());
    }

    @Test
    @DB
    public void testReleaseIpv6SubnetForNetwork() {
        final long prefixId = 1L;
        final String subnet = "fd17:5:8a43:e2a5::/64";
        Ipv6GuestPrefixSubnetNetworkMapVO allocatingMap = new Ipv6GuestPrefixSubnetNetworkMapVO(prefixId, subnet, networkId, Ipv6GuestPrefixSubnetNetworkMap.State.Allocated);
        Mockito.when(ipv6GuestPrefixSubnetNetworkMapDao.findByNetworkId(networkId)).thenReturn(allocatingMap);
        Mockito.when(ipv6GuestPrefixSubnetNetworkMapDao.createForUpdate(Mockito.anyLong())).thenReturn(allocatingMap);
        updatedPrefixSubnetMap.clear();
        try (TransactionLegacy txn = TransactionLegacy.open("testNewPreAllocateIpv6SubnetForNetwork")) {
            ipv6Service.releaseIpv6SubnetForNetwork(networkId);
        }
        Assert.assertEquals(1, updatedPrefixSubnetMap.size());
        Ipv6GuestPrefixSubnetNetworkMapVO map = updatedPrefixSubnetMap.get(0);
        Assert.assertEquals(Ipv6GuestPrefixSubnetNetworkMap.State.Free, map.getState());
        Assert.assertEquals(subnet, map.getSubnet());
        Assert.assertEquals(prefixId, map.getPrefixId());
        Assert.assertNull(map.getNetworkId());
    }

    @Test
    public void testGetAllocatedIpv6FromVlanRange() {
        Vlan vlan = Mockito.mock(Vlan.class);
        Mockito.when(vlan.getIp6Cidr()).thenReturn(null);
        Mockito.when(vlan.getIp6Gateway()).thenReturn(null);
        Assert.assertNull(ipv6Service.getAllocatedIpv6FromVlanRange(vlan));
        List<String> addresses = Arrays.asList("fd17:5:8a43:e2a5::1000", "fd17:5:8a43:e2a5::1001");
        Vlan vlan1 = Mockito.mock(Vlan.class);
        Mockito.when(vlan1.getIp6Cidr()).thenReturn(cidr);
        Mockito.when(vlan1.getIp6Gateway()).thenReturn(gateway);

        List<NicVO> nics = new ArrayList<>();
        for (String address : addresses) {
            NicVO nic = new NicVO(publicReserver, 100L, 1L, VirtualMachine.Type.DomainRouter);
            nic.setIPv6Address(address);
            nics.add(nic);
        }
        Mockito.when(nicDao.findNicsByIpv6GatewayIpv6CidrAndReserver(gateway, cidr, publicReserver)).thenReturn(nics);
        List<String> result = ipv6Service.getAllocatedIpv6FromVlanRange(vlan1);
        Assert.assertEquals(addresses.size(), result.size());
        for (String address : addresses) {
            Assert.assertTrue(result.contains(address));
        }
    }

    @Test
    public void testAlreadyExistAssignPublicIpv6ToNetwork() {
        Nic nic = Mockito.mock(Nic.class);
        Mockito.when(nic.getIPv6Address()).thenReturn(ipv6Address);
        Nic assignedNic = ipv6Service.assignPublicIpv6ToNetwork(Mockito.mock(Network.class), nic);
        Assert.assertEquals(ipv6Address, assignedNic.getIPv6Address());
    }

    @Test(expected = CloudRuntimeException.class)
    public void testNewErrorAssignPublicIpv6ToNetwork() {
        Nic nic = Mockito.mock(Nic.class);
        Mockito.when(nic.getIPv6Address()).thenReturn(null);
        Mockito.when(nic.getBroadcastUri()).thenReturn(URI.create(vlan));
        Mockito.when(vlanDao.listIpv6RangeByZoneIdAndVlanId(1L, "vlan")).thenReturn(new ArrayList<>());
        try (TransactionLegacy txn = TransactionLegacy.open("testNewErrorAssignPublicIpv6ToNetwork")) {
           ipv6Service.assignPublicIpv6ToNetwork(Mockito.mock(Network.class), nic);
        }
    }

    private List<NicVO> mockPlaceholderNics() {
        NicVO placeholderNic = Mockito.mock(NicVO.class);
        Mockito.when(placeholderNic.getIPv6Address()).thenReturn(ipv6Address);
        Mockito.when(placeholderNic.getIPv6Gateway()).thenReturn(gateway);
        Mockito.when(placeholderNic.getIPv6Cidr()).thenReturn(cidr);
        Mockito.when(placeholderNic.getReserver()).thenReturn(publicReserver);
        List<NicVO> placeholderNics = new ArrayList<>();
        placeholderNics.add(placeholderNic);
        return placeholderNics;
    }

    private void prepareMocksForPublicIpv6(boolean fromPlaceholder) {
        VlanVO vlanVO = Mockito.mock(VlanVO.class);
        Mockito.when(vlanVO.getIp6Cidr()).thenReturn(cidr);
        Mockito.when(vlanVO.getIp6Gateway()).thenReturn(gateway);
        Mockito.when(vlanVO.getVlanType()).thenReturn(Vlan.VlanType.VirtualNetwork);
        List<VlanVO> vlans = new ArrayList<>();
        vlans.add(vlanVO);
        Mockito.when(vlanDao.listIpv6RangeByZoneIdAndVlanId(Mockito.anyLong(), Mockito.anyString())).thenReturn(vlans);
        List<NicVO> placeholderNics = new ArrayList<>();
        if (fromPlaceholder) {
            placeholderNics = mockPlaceholderNics();
        }
        Mockito.when(nicDao.listPlaceholderNicsByNetworkIdAndVmType(networkId, VirtualMachine.Type.DomainRouter)).thenReturn(placeholderNics);
        Mockito.when(nicDao.createForUpdate(nicId)).thenReturn(new NicVO(publicReserver, 100L, 1L, VirtualMachine.Type.DomainRouter));
        PowerMockito.mockStatic(ActionEventUtils.class);
        Mockito.when(ActionEventUtils.onCompletedActionEvent(Mockito.anyLong(), Mockito.anyLong(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyLong(), Mockito.anyString(), Mockito.anyLong())).thenReturn(1L);
        PowerMockito.mockStatic(UsageEventUtils.class);
    }

    @Test
    @DB
    public void testNewAssignPublicIpv6ToNetwork() {
        NicVO nic = Mockito.mock(NicVO.class);
        Mockito.when(nic.getIPv6Address()).thenReturn(null);
        Mockito.when(nic.getBroadcastUri()).thenReturn(URI.create(vlan));
        Mockito.when(nic.getMacAddress()).thenReturn(macAddress);
        Mockito.when(nic.getId()).thenReturn(nicId);
        Network network = Mockito.mock(Network.class);
        Mockito.when(network.getId()).thenReturn(networkId);
        prepareMocksForPublicIpv6(false);
        Nic assignedNic;
        try (TransactionLegacy txn = TransactionLegacy.open("testNewPreAllocateIpv6SubnetForNetwork")) {
            assignedNic = ipv6Service.assignPublicIpv6ToNetwork(network, nic);
        }
        Assert.assertEquals(ipv6Address, assignedNic.getIPv6Address());
        Assert.assertEquals(gateway, assignedNic.getIPv6Gateway());
        Assert.assertEquals(cidr, assignedNic.getIPv6Cidr());
    }

    @Test
    public void testFromPlaceholderAssignPublicIpv6ToNetwork() {
        NicVO nic = Mockito.mock(NicVO.class);
        Mockito.when(nic.getIPv6Address()).thenReturn(null);
        Mockito.when(nic.getBroadcastUri()).thenReturn(URI.create(vlan));
        Mockito.when(nic.getId()).thenReturn(nicId);
        Network network = Mockito.mock(Network.class);
        Mockito.when(network.getId()).thenReturn(networkId);
        prepareMocksForPublicIpv6(true);
        Nic assignedNic = ipv6Service.assignPublicIpv6ToNetwork(network, nic);
        Assert.assertEquals(ipv6Address, assignedNic.getIPv6Address());
        Assert.assertEquals(gateway, assignedNic.getIPv6Gateway());
        Assert.assertEquals(cidr, assignedNic.getIPv6Cidr());
    }

    @Test
    public void testIpv4NetworkUpdateNicIpv6() {
        Mockito.when(networkOfferingDao.isIpv6Supported(Mockito.anyLong())).thenReturn(false);
        NicProfile nicProfile = new NicProfile();
        try {
            ipv6Service.updateNicIpv6(nicProfile, Mockito.mock(DataCenter.class), Mockito.mock(Network.class));
        } catch (InsufficientAddressCapacityException e) {
            Assert.fail("InsufficientAddressCapacityException");
        }
        Assert.assertNull(nicProfile.getIPv6Address());
        Assert.assertNull(nicProfile.getIPv6Gateway());
        Assert.assertNull(nicProfile.getIPv6Cidr());
    }

    @Test
    public void testIpv6NetworkUpdateNicIpv6() {
        Mockito.when(networkOfferingDao.isIpv6Supported(Mockito.anyLong())).thenReturn(true);
        Mockito.when(networkModel.getNetworkIp6Dns(Mockito.any(Network.class), Mockito.any(DataCenter.class))).thenReturn(ipv6DnsPair);
        NicProfile nicProfile = new NicProfile();
        nicProfile.setBroadcastUri(URI.create(vlan));
        nicProfile.setMacAddress(macAddress);
        prepareMocksForPublicIpv6(false);
        try {
            ipv6Service.updateNicIpv6(nicProfile, Mockito.mock(DataCenter.class), Mockito.mock(Network.class));
        } catch (InsufficientAddressCapacityException e) {
            Assert.fail("InsufficientAddressCapacityException");
        }
        Assert.assertEquals(ipv6Address, nicProfile.getIPv6Address());
        Assert.assertEquals(gateway, nicProfile.getIPv6Gateway());
        Assert.assertEquals(cidr, nicProfile.getIPv6Cidr());
    }

    @Test
    public void testIpv6NetworkFromPlaceholderUpdateNicIpv6() {
        Mockito.when(networkOfferingDao.isIpv6Supported(Mockito.anyLong())).thenReturn(true);
        Mockito.when(networkModel.getNetworkIp6Dns(Mockito.any(Network.class), Mockito.any(DataCenter.class))).thenReturn(ipv6DnsPair);
        NicProfile nicProfile = new NicProfile();
        nicProfile.setBroadcastUri(URI.create(vlan));
        nicProfile.setMacAddress(macAddress);
        prepareMocksForPublicIpv6(true);
        try {
            ipv6Service.updateNicIpv6(nicProfile, Mockito.mock(DataCenter.class), Mockito.mock(Network.class));
        } catch (InsufficientAddressCapacityException e) {
            Assert.fail("InsufficientAddressCapacityException");
        }
        Assert.assertEquals(ipv6Address, nicProfile.getIPv6Address());
        Assert.assertEquals(gateway, nicProfile.getIPv6Gateway());
        Assert.assertEquals(cidr, nicProfile.getIPv6Cidr());
    }

    @Test
    public void testEmptyGetPublicIpv6AddressesForNetwork(){
        Mockito.when(domainRouterDao.findByNetwork(Mockito.anyLong())).thenReturn(new ArrayList<>());
        List<String> addresses = ipv6Service.getPublicIpv6AddressesForNetwork(Mockito.mock(Network.class));
        Assert.assertTrue(CollectionUtils.isEmpty(addresses));
        List<DomainRouterVO> routers = List.of(Mockito.mock(DomainRouterVO.class));
        Mockito.when(domainRouterDao.findByNetwork(Mockito.anyLong())).thenReturn(routers);
        Mockito.when(nicDao.listByVmId(Mockito.anyLong())).thenReturn(new ArrayList<>());
        addresses = ipv6Service.getPublicIpv6AddressesForNetwork(Mockito.mock(Network.class));
        Assert.assertTrue(CollectionUtils.isEmpty(addresses));
        NicVO nic = Mockito.mock(NicVO.class);
        Mockito.when(nic.getIPv6Address()).thenReturn(null);
        List<NicVO> nics = List.of(nic);
        Mockito.when(nicDao.listByVmId(Mockito.anyLong())).thenReturn(nics);
        addresses = ipv6Service.getPublicIpv6AddressesForNetwork(Mockito.mock(Network.class));
        Assert.assertTrue(CollectionUtils.isEmpty(addresses));
    }

    @Test
    public void testGetPublicIpv6AddressesForNetwork(){
        List<DomainRouterVO> routers = List.of(Mockito.mock(DomainRouterVO.class), Mockito.mock(DomainRouterVO.class));
        Mockito.when(domainRouterDao.findByNetwork(Mockito.anyLong())).thenReturn(routers);
        NicVO nic = Mockito.mock(NicVO.class);
        Mockito.when(nic.getIPv6Address()).thenReturn(ipv6Address);
        Mockito.when(nic.getReserver()).thenReturn(publicReserver);
        List<NicVO> nics = List.of(nic);
        Mockito.when(nicDao.listByVmId(Mockito.anyLong())).thenReturn(nics);
        List<String> addresses = ipv6Service.getPublicIpv6AddressesForNetwork(Mockito.mock(Network.class));
        Assert.assertEquals(1, addresses.size());
        Assert.assertEquals(ipv6Address, addresses.get(0));
    }

    @Test
    public void testEmptyUpdateIpv6RoutesForVpcResponse() {
        VpcResponse response = new VpcResponse();
        Vpc vpc = Mockito.mock(Vpc.class);
        List<NetworkVO> networks = new ArrayList<>();
        Mockito.doReturn(networks).when(networkModel).listNetworksByVpc(Mockito.anyLong());
        ipv6Service.updateIpv6RoutesForVpcResponse(vpc, response);
        Assert.assertTrue(CollectionUtils.isEmpty(response.getIpv6Routes()));
    }

    @Test
    public void testUpdateIpv6RoutesForVpcResponse() {
        VpcResponse response = new VpcResponse();
        Vpc vpc = Mockito.mock(Vpc.class);
        List<NetworkVO> networks = new ArrayList<>();
        NetworkVO network = Mockito.mock(NetworkVO.class);
        Mockito.when(network.getIp6Cidr()).thenReturn(cidr);
        networks.add(network);
        List<DomainRouterVO> routers = List.of(Mockito.mock(DomainRouterVO.class));
        Mockito.when(domainRouterDao.findByNetwork(Mockito.anyLong())).thenReturn(routers);
        NicVO nic = Mockito.mock(NicVO.class);
        Mockito.when(nic.getIPv6Address()).thenReturn(ipv6Address);
        Mockito.when(nic.getReserver()).thenReturn(publicReserver);
        Mockito.when(nicDao.listByVmId(Mockito.anyLong())).thenReturn(List.of(nic));
        Mockito.doReturn(networks).when(networkModel).listNetworksByVpc(Mockito.anyLong());
        Mockito.when(networkOfferingDao.isIpv6Supported(Mockito.anyLong())).thenReturn(true);
        ipv6Service.updateIpv6RoutesForVpcResponse(vpc, response);
        Assert.assertEquals(1, response.getIpv6Routes().size());
        Ipv6RouteResponse routeResponse = new ArrayList<>(response.getIpv6Routes()).get(0);
        Assert.assertEquals(ipv6Address, routeResponse.getGateway());
        Assert.assertEquals(cidr, routeResponse.getSubnet());
    }

    @Test
    public void testCheckNetworkIpv6UpgradeForNoPrefixes() {
        Mockito.when(dataCenterGuestIpv6PrefixDao.listByDataCenterId(Mockito.anyLong())).thenReturn(new ArrayList<>());
        try {
            ipv6Service.checkNetworkIpv6Upgrade(Mockito.mock(Network.class));
            Assert.fail("No ResourceAllocationException");
        } catch (InsufficientAddressCapacityException | ResourceAllocationException ignored) {}
    }

    @Test
    public void testCheckNetworkIpv6UpgradeForNoIpv6Vlan() {
        final long physicalNetworkId = 1L;
        Mockito.when(dataCenterGuestIpv6PrefixDao.listByDataCenterId(Mockito.anyLong())).thenReturn(List.of(Mockito.mock(DataCenterGuestIpv6PrefixVO.class)));
        Network network = Mockito.mock(Network.class);
        Mockito.when(network.getPhysicalNetworkId()).thenReturn(physicalNetworkId);
        Mockito.when(network.getVpcId()).thenReturn(null);
        Mockito.when(ipAddressDao.listByAssociatedNetwork(Mockito.anyLong(), Mockito.anyBoolean())).thenReturn(List.of(Mockito.mock(IPAddressVO.class)));
        VlanVO vlanVO = Mockito.mock(VlanVO.class);
        Mockito.when(vlanVO.getVlanTag()).thenReturn(vlan);
        Mockito.when(vlanDao.findById(Mockito.anyLong())).thenReturn(vlanVO);
        Mockito.when(vlanDao.listIpv6RangeByZoneIdAndVlanId(Mockito.anyLong(), Mockito.anyString())).thenReturn(new ArrayList<>());
        try {
            ipv6Service.checkNetworkIpv6Upgrade(network);
            Assert.fail("No InsufficientAddressCapacityException");
        } catch (InsufficientAddressCapacityException | ResourceAllocationException ignored) {}
    }

    @Test
    public void testCheckNetworkIpv6UpgradeForNetwork() {
        final long physicalNetworkId = 1L;
        Mockito.when(dataCenterGuestIpv6PrefixDao.listByDataCenterId(Mockito.anyLong())).thenReturn(List.of(Mockito.mock(DataCenterGuestIpv6PrefixVO.class)));
        Network network = Mockito.mock(Network.class);
        Mockito.when(network.getPhysicalNetworkId()).thenReturn(physicalNetworkId);
        Mockito.when(network.getVpcId()).thenReturn(null);
        Mockito.when(ipAddressDao.listByAssociatedNetwork(Mockito.anyLong(), Mockito.anyBoolean())).thenReturn(List.of(Mockito.mock(IPAddressVO.class)));
        VlanVO vlanVO = Mockito.mock(VlanVO.class);
        Mockito.when(vlanVO.getVlanTag()).thenReturn(vlan);
        Mockito.when(vlanDao.findById(Mockito.anyLong())).thenReturn(vlanVO);
        Mockito.when(vlanDao.listIpv6RangeByZoneIdAndVlanId(physicalNetworkId, vlan)).thenReturn(List.of(vlanVO));
        try {
            ipv6Service.checkNetworkIpv6Upgrade(network);
        } catch (InsufficientAddressCapacityException | ResourceAllocationException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testUpdateIpv6FirewallRule() {
        final Long firewallRuleId = 1L;
        UpdateIpv6FirewallRuleCmd cmd = Mockito.mock(UpdateIpv6FirewallRuleCmd.class);
        Mockito.when(cmd.getId()).thenReturn(firewallRuleId);
        Mockito.when(firewallDao.findById(firewallRuleId)).thenReturn(null);
        try {
            ipv6Service.updateIpv6FirewallRule(cmd);
            Assert.fail("No InvalidParameterValueException");
        } catch (InvalidParameterValueException ignored) {}
        FirewallRuleVO ingressFirewallRule = Mockito.mock(FirewallRuleVO.class);
        Mockito.when(ingressFirewallRule.getTrafficType()).thenReturn(FirewallRule.TrafficType.Ingress);
        Mockito.when(firewallDao.findById(firewallRuleId)).thenReturn(ingressFirewallRule);
        try {
            ipv6Service.updateIpv6FirewallRule(cmd);
        } catch (InvalidParameterValueException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testDeleteIpv6FirewallRule() {
        final Long firewallRuleId = 1L;
        Mockito.when(firewallDao.findById(firewallRuleId)).thenReturn(null);
        try {
            ipv6Service.revokeIpv6FirewallRule(firewallRuleId);
            Assert.fail("No InvalidParameterValueException");
        } catch (InvalidParameterValueException ignored) {}
        FirewallRuleVO ingressFirewallRule = Mockito.mock(FirewallRuleVO.class);
        Mockito.when(ingressFirewallRule.getTrafficType()).thenReturn(FirewallRule.TrafficType.Ingress);
        Mockito.when(firewallDao.findById(firewallRuleId)).thenReturn(ingressFirewallRule);
        try {
            ipv6Service.revokeIpv6FirewallRule(firewallRuleId);
        } catch (InvalidParameterValueException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testGetIpv6FirewallRule() {
        final Long firewallRuleId = 1L;
        final String uuid = UUID.randomUUID().toString();
        Mockito.when(firewallDao.findById(firewallRuleId)).thenReturn(null);
        FirewallRule rule = ipv6Service.getIpv6FirewallRule(firewallRuleId);
        Assert.assertNull(rule);
        FirewallRuleVO ingressFirewallRule = Mockito.mock(FirewallRuleVO.class);
        Mockito.when(ingressFirewallRule.getUuid()).thenReturn(uuid);
        Mockito.when(firewallDao.findById(firewallRuleId)).thenReturn(ingressFirewallRule);
        rule = ipv6Service.getIpv6FirewallRule(firewallRuleId);
        Assert.assertEquals(uuid, rule.getUuid());
    }

    private void registerCallContext() {
        account = new AccountVO("testaccount", 1L, "networkdomain", Account.Type.NORMAL, "uuid");
        account.setId(ACCOUNT_ID);
        user = new UserVO(1, "testuser", "password", "firstname", "lastName", "email", "timezone",
                UUID.randomUUID().toString(), User.Source.UNKNOWN);
        CallContext.register(user, account);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testInvalidSourceCidrCreateIpv6FirewallRule() {
        registerCallContext();
        CreateIpv6FirewallRuleCmd cmd = Mockito.mock(CreateIpv6FirewallRuleCmd.class);
        Mockito.when(cmd.getSourceCidrList()).thenReturn(List.of("10.1.1.1"));
        try {
            ipv6Service.createIpv6FirewallRule(cmd);
        } catch (NetworkRuleConflictException e) {
            throw new RuntimeException(e);
        }
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testInvalidDestinationCidrCreateIpv6FirewallRule() {
        registerCallContext();
        CreateIpv6FirewallRuleCmd cmd = Mockito.mock(CreateIpv6FirewallRuleCmd.class);
        Mockito.when(cmd.getDestinationCidrList()).thenReturn(List.of("10.1.1.1"));
        try {
            ipv6Service.createIpv6FirewallRule(cmd);
        } catch (NetworkRuleConflictException e) {
            throw new RuntimeException(e);
        }
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testStartPortCidrCreateIpv6FirewallRule() {
        registerCallContext();
        CreateIpv6FirewallRuleCmd cmd = Mockito.mock(CreateIpv6FirewallRuleCmd.class);
        Mockito.when(cmd.getSourcePortStart()).thenReturn(800000);
        try {
            ipv6Service.createIpv6FirewallRule(cmd);
        } catch (NetworkRuleConflictException e) {
            throw new RuntimeException(e);
        }
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testEndPortCidrCreateIpv6FirewallRule() {
        registerCallContext();
        CreateIpv6FirewallRuleCmd cmd = Mockito.mock(CreateIpv6FirewallRuleCmd.class);
        Mockito.when(cmd.getSourcePortEnd()).thenReturn(800000);
        try {
            ipv6Service.createIpv6FirewallRule(cmd);
        } catch (NetworkRuleConflictException e) {
            throw new RuntimeException(e);
        }
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testPortRangeCidrCreateIpv6FirewallRule() {
        registerCallContext();
        CreateIpv6FirewallRuleCmd cmd = Mockito.mock(CreateIpv6FirewallRuleCmd.class);
        Mockito.when(cmd.getSourcePortStart()).thenReturn(900);
        Mockito.when(cmd.getSourcePortEnd()).thenReturn(800);
        try {
            ipv6Service.createIpv6FirewallRule(cmd);
        } catch (NetworkRuleConflictException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testRemovePublicIpv6PlaceholderNics() {
        Network network = Mockito.mock(NetworkVO.class);
        Mockito.when(network.getId()).thenReturn(networkId);
        NicVO nic = Mockito.mock(NicVO.class);
        Mockito.when(nic.getId()).thenReturn(nicId);
        Mockito.when(nic.getIPv6Address()).thenReturn(ipv6Address);
        Mockito.when(nic.getIPv6Cidr()).thenReturn(cidr);
        Mockito.when(nic.getIPv6Gateway()).thenReturn(gateway);
        Mockito.when(nic.getReserver()).thenReturn(publicReserver);
        Mockito.when(nicDao.listPlaceholderNicsByNetworkId(Mockito.anyLong())).thenReturn(List.of(nic));
        final List<Long> removedNics = new ArrayList<>();
        Mockito.when(nicDao.remove(Mockito.anyLong())).thenAnswer((Answer<Boolean>) invocation -> {
            removedNics.add((Long)invocation.getArguments()[0]);
            return true;
        });
        PowerMockito.mockStatic(ActionEventUtils.class);
        Mockito.when(ActionEventUtils.onCompletedActionEvent(Mockito.anyLong(), Mockito.anyLong(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyLong(), Mockito.anyString(), Mockito.anyLong())).thenReturn(1L);
        PowerMockito.mockStatic(UsageEventUtils.class);
        ipv6Service.removePublicIpv6PlaceholderNics(network);
        Assert.assertEquals(1, removedNics.size());
        Assert.assertEquals(nicId, removedNics.get(0));
        removedNics.clear();
        NicVO nic1 = Mockito.mock(NicVO.class);
        Mockito.when(nic1.getId()).thenReturn(nicId);
        Mockito.when(nic1.getIPv6Address()).thenReturn(null);
        Mockito.when(nicDao.listPlaceholderNicsByNetworkId(Mockito.anyLong())).thenReturn(List.of(nic1));
        ipv6Service.removePublicIpv6PlaceholderNics(network);
        Assert.assertEquals(0, removedNics.size());
    }
}
