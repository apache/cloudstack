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

package org.apache.cloudstack.network;

import com.cloud.api.ApiDBUtils;
import com.cloud.bgp.BGPService;
import com.cloud.dc.DataCenterVO;
import com.cloud.domain.DomainVO;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network;
import com.cloud.network.NetworkModel;
import com.cloud.network.dao.FirewallRulesDao;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkServiceMapDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.element.VirtualRouterElement;
import com.cloud.network.firewall.FirewallService;
import com.cloud.network.rules.FirewallManager;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.FirewallRuleVO;
import com.cloud.network.vpc.Vpc;
import com.cloud.network.vpc.VpcOfferingVO;
import com.cloud.network.vpc.VpcVO;
import com.cloud.network.vpc.dao.VpcDao;
import com.cloud.network.vpc.dao.VpcOfferingDao;
import com.cloud.network.vpc.dao.VpcOfferingServiceMapDao;
import com.cloud.offering.NetworkOffering;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.offerings.dao.NetworkOfferingServiceMapDao;
import com.cloud.projects.ProjectVO;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.user.User;
import com.cloud.user.UserVO;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;

import org.apache.cloudstack.api.command.admin.network.CreateIpv4SubnetForGuestNetworkCmd;
import org.apache.cloudstack.api.command.admin.network.CreateIpv4SubnetForZoneCmd;
import org.apache.cloudstack.api.command.admin.network.DedicateIpv4SubnetForZoneCmd;
import org.apache.cloudstack.api.command.admin.network.DeleteIpv4SubnetForGuestNetworkCmd;
import org.apache.cloudstack.api.command.admin.network.DeleteIpv4SubnetForZoneCmd;
import org.apache.cloudstack.api.command.admin.network.ReleaseDedicatedIpv4SubnetForZoneCmd;
import org.apache.cloudstack.api.command.admin.network.UpdateIpv4SubnetForZoneCmd;
import org.apache.cloudstack.api.command.admin.network.bgp.ChangeBgpPeersForNetworkCmd;
import org.apache.cloudstack.api.command.admin.network.bgp.ChangeBgpPeersForVpcCmd;
import org.apache.cloudstack.api.command.admin.network.bgp.CreateBgpPeerCmd;
import org.apache.cloudstack.api.command.admin.network.bgp.DedicateBgpPeerCmd;
import org.apache.cloudstack.api.command.admin.network.bgp.DeleteBgpPeerCmd;
import org.apache.cloudstack.api.command.admin.network.bgp.ReleaseDedicatedBgpPeerCmd;
import org.apache.cloudstack.api.command.admin.network.bgp.UpdateBgpPeerCmd;
import org.apache.cloudstack.api.command.user.network.routing.CreateRoutingFirewallRuleCmd;
import org.apache.cloudstack.api.command.user.network.routing.UpdateRoutingFirewallRuleCmd;
import org.apache.cloudstack.api.response.BgpPeerResponse;
import org.apache.cloudstack.api.response.DataCenterIpv4SubnetResponse;
import org.apache.cloudstack.api.response.Ipv4SubnetForGuestNetworkResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.datacenter.DataCenterIpv4GuestSubnet;
import org.apache.cloudstack.datacenter.DataCenterIpv4GuestSubnetVO;
import org.apache.cloudstack.datacenter.dao.DataCenterIpv4GuestSubnetDao;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.network.dao.BgpPeerDao;
import org.apache.cloudstack.network.dao.BgpPeerDetailsDao;
import org.apache.cloudstack.network.dao.BgpPeerNetworkMapDao;
import org.apache.cloudstack.network.dao.Ipv4GuestSubnetNetworkMapDao;
import org.apache.commons.collections.CollectionUtils;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockedStatic;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RoutedIpv4ManagerImplTest {

    @Spy
    @InjectMocks
    RoutedIpv4ManagerImpl routedIpv4Manager = new RoutedIpv4ManagerImpl();

    @Mock
    DataCenterIpv4GuestSubnetDao dataCenterIpv4GuestSubnetDao;
    @Mock
    Ipv4GuestSubnetNetworkMapDao ipv4GuestSubnetNetworkMapDao;
    @Mock
    FirewallService firewallService;
    @Mock
    FirewallManager firewallManager;
    @Mock
    FirewallRulesDao firewallDao;
    @Mock
    NetworkServiceMapDao networkServiceMapDao;
    @Mock
    NetworkOfferingServiceMapDao networkOfferingServiceMapDao;
    @Mock
    NetworkOfferingDao networkOfferingDao;
    @Mock
    NetworkModel networkModel;
    @Mock
    AccountManager accountManager;
    @Mock
    VpcOfferingDao vpcOfferingDao;
    @Mock
    VpcOfferingServiceMapDao vpcOfferingServiceMapDao;
    @Mock
    VpcDao vpcDao;
    @Mock
    BgpPeerDao bgpPeerDao;
    @Mock
    BgpPeerDetailsDao bgpPeerDetailsDao;
    @Mock
    BgpPeerNetworkMapDao bgpPeerNetworkMapDao;
    @Mock
    NetworkDao networkDao;
    @Mock
    BGPService bgpService;

    static MockedStatic<ApiDBUtils> apiDBUtilsMocked;

    @Mock
    DataCenterIpv4GuestSubnetVO subnetVO;
    @Mock
    DomainVO domain;
    @Mock
    AccountVO account;
    @Mock
    ProjectVO project;
    @Mock
    Ipv4GuestSubnetNetworkMapVO ipv4GuestSubnetNetworkMap;
    @Mock
    NetworkVO network;
    @Mock
    VpcVO vpc;
    @Mock
    DataCenterVO zone;
    @Mock
    FirewallRuleVO rule;
    @Mock
    BgpPeerVO bgpPeer;
    @Mock
    NetworkOfferingVO networkOffering;
    @Mock
    VpcOfferingVO vpcOffering;

    Long zoneId = 1L;
    String zoneUuid = "zone-uuid";
    String zoneName = "zone-name";
    Long zoneSubnetId = 2L;
    String zoneSubnetUuid = "zone-subnet-uuid";
    Long domainId = 3L;
    Long accountId = 4L;
    Long ipv4GuestSubnetNetworkMapId = 5L;
    Long networkId = 6L;
    String networkUuid = "network-uuid";
    String networkName = "network-name";
    Long vpcId = 7L;
    String vpcUuid = "vpc-uuid";
    String vpcName = "vpc-name";
    String domainName = "domain";
    String domainUuid = "domain-uuid";
    String accountName = "user";
    String subnet = "172.16.1.0/24";
    String newSubnet = "172.16.2.0/24";
    String subnetForNetwork = "172.16.1.0/28";
    String newSubnetForNetwork = "172.16.1.64/28";
    String newSubnetForNetworkTooSmall = "172.16.1.0/30";
    String newSubnetForNetworkTooBig = "172.16.1.0/23";
    Integer cidrSize = 28;
    Ipv4GuestSubnetNetworkMap.State ipv4GuestSubnetNetworkMapState = Ipv4GuestSubnetNetworkMap.State.Free;
    Date created = new Date();
    String uuid = "xxx-yyy-zzz";
    Long ruleId = 8L;
    String ip4Address = "10.10.11.11";
    String ip6Address = "fd00:10:10:11:11::1";
    Long asNumber = 9999L;
    String password = "password-text";
    Long bgpPeerId = 9L;
    String bgpPeerUuid = "bgp-peer-uuid";
    Long projectId = 10L;
    String projectName = "project";
    String projectUuid = "project-uuid";
    Long networkOfferingId = 11L;
    Long vpcOfferingId = 12L;

    @BeforeClass
    public static void setup() {
        apiDBUtilsMocked = Mockito.mockStatic(ApiDBUtils.class);
        apiDBUtilsMocked.when(() -> ApiDBUtils.findZoneById(Mockito.anyLong())).thenReturn(Mockito.mock(DataCenterVO.class));

        CallContext.unregister();
        AccountVO account = new AccountVO("admin", 1L, "", Account.Type.ADMIN, "uuid");
        account.setId(2L);
        UserVO user = new UserVO(1, "admin", "password", "firstname", "lastName", "email", "timezone", UUID.randomUUID().toString(), User.Source.UNKNOWN);
        CallContext.register(user, account);
    }

    @AfterClass
    public static void close() {
        apiDBUtilsMocked.close();

        CallContext.unregister();
    }

    @Test
    public void testGetCommands() throws NoSuchFieldException, IllegalAccessException {
        Assert.assertTrue(CollectionUtils.isNotEmpty(routedIpv4Manager.getCommands()));
        Assert.assertEquals(26, routedIpv4Manager.getCommands().size());

        overrideDefaultConfigValue(RoutedIpv4Manager.RoutedNetworkVpcEnabled, "_defaultValue", "false");
        Assert.assertTrue(CollectionUtils.isEmpty(routedIpv4Manager.getCommands()));
    }

    private static void overrideDefaultConfigValue(final ConfigKey configKey, final String name, final Object o) throws IllegalAccessException, NoSuchFieldException {
        Field f = ConfigKey.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(configKey, o);
    }

    @Test
    public void testCreateDataCenterIpv4GuestSubnet() {
        CreateIpv4SubnetForZoneCmd cmd = new CreateIpv4SubnetForZoneCmd();
        ReflectionTestUtils.setField(cmd,"zoneId", zoneId);
        ReflectionTestUtils.setField(cmd,"subnet", subnet);

        routedIpv4Manager.createDataCenterIpv4GuestSubnet(cmd);

        verify(routedIpv4Manager).checkConflicts(new ArrayList<>(), subnet, null);
        verify(dataCenterIpv4GuestSubnetDao).persist(any());
    }

    @Test
    public void testCreateDataCenterIpv4SubnetResponse() {
        DataCenterIpv4GuestSubnetVO subnetVO = new DataCenterIpv4GuestSubnetVO(zoneId, NetUtils.transformCidr(subnet));
        DataCenterIpv4SubnetResponse response = routedIpv4Manager.createDataCenterIpv4SubnetResponse(subnetVO);

        Assert.assertEquals(subnet, response.getSubnet());
        Assert.assertEquals(subnetVO.getUuid(), response.getId());
        Assert.assertEquals("zoneipv4subnet", response.getObjectName());
    }

    @Test
    public void testDeleteDataCenterIpv4GuestSubnet() {
        DeleteIpv4SubnetForZoneCmd cmd = new DeleteIpv4SubnetForZoneCmd();
        ReflectionTestUtils.setField(cmd,"id", zoneSubnetId);

        routedIpv4Manager.deleteDataCenterIpv4GuestSubnet(cmd);

        verify(ipv4GuestSubnetNetworkMapDao).deleteByParentId(zoneSubnetId);
        verify(dataCenterIpv4GuestSubnetDao).remove(zoneSubnetId);
    }

    @Test
    public void testUpdateDataCenterIpv4GuestSubnet() {
        UpdateIpv4SubnetForZoneCmd cmd = new UpdateIpv4SubnetForZoneCmd();
        ReflectionTestUtils.setField(cmd,"id", zoneSubnetId);
        ReflectionTestUtils.setField(cmd,"subnet", newSubnet);

        when(dataCenterIpv4GuestSubnetDao.findById(zoneSubnetId)).thenReturn(subnetVO);

        routedIpv4Manager.updateDataCenterIpv4GuestSubnet(cmd);

        verify(routedIpv4Manager).checkConflicts(new ArrayList<>(), newSubnet, zoneSubnetId);
        verify(ipv4GuestSubnetNetworkMapDao).listByParent(zoneSubnetId);
        verify(dataCenterIpv4GuestSubnetDao).update(eq(zoneSubnetId), any(DataCenterIpv4GuestSubnetVO.class));
    }

    @Test
    public void testCheckConflicts1() {
        DataCenterIpv4GuestSubnetVO existingSubnet = Mockito.mock(DataCenterIpv4GuestSubnetVO.class);
        when(existingSubnet.getSubnet()).thenReturn(subnet);
        List<DataCenterIpv4GuestSubnetVO> existingSubnets = Arrays.asList(existingSubnet);
        routedIpv4Manager.checkConflicts(existingSubnets, newSubnet, null);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testCheckConflicts2() {
        DataCenterIpv4GuestSubnetVO existingSubnet = Mockito.mock(DataCenterIpv4GuestSubnetVO.class);
        when(existingSubnet.getSubnet()).thenReturn(subnet);
        List<DataCenterIpv4GuestSubnetVO> existingSubnets = Arrays.asList(existingSubnet);
        routedIpv4Manager.checkConflicts(existingSubnets, subnet, null);
    }

    @Test
    public void testCheckConflicts3() {
        DataCenterIpv4GuestSubnetVO existingSubnet = Mockito.mock(DataCenterIpv4GuestSubnetVO.class);
        when(existingSubnet.getId()).thenReturn(zoneSubnetId);
        List<DataCenterIpv4GuestSubnetVO> existingSubnets = Arrays.asList(existingSubnet);
        routedIpv4Manager.checkConflicts(existingSubnets, subnet, zoneSubnetId);
    }

    @Test
    public void testDedicateDataCenterIpv4GuestSubnet() {
        DedicateIpv4SubnetForZoneCmd cmd = new DedicateIpv4SubnetForZoneCmd();
        ReflectionTestUtils.setField(cmd,"id", zoneSubnetId);
        ReflectionTestUtils.setField(cmd,"domainId", domainId);
        ReflectionTestUtils.setField(cmd,"accountName", accountName);
        ReflectionTestUtils.setField(cmd,"projectId", null);

        when(dataCenterIpv4GuestSubnetDao.findById(zoneSubnetId)).thenReturn(subnetVO);

        when(accountManager.finalyzeAccountId(accountName, domainId, null, false)).thenReturn(accountId);
        when(accountManager.getAccount(accountId)).thenReturn(account);
        when(account.getDomainId()).thenReturn(domainId);
        when(dataCenterIpv4GuestSubnetDao.findById(zoneSubnetId)).thenReturn(subnetVO);

        routedIpv4Manager.dedicateDataCenterIpv4GuestSubnet(cmd);

        verify(subnetVO).setDomainId(domainId);
        verify(subnetVO).setAccountId(accountId);
        verify(dataCenterIpv4GuestSubnetDao).update(zoneSubnetId, subnetVO);
    }

    @Test
    public void testReleaseDedicatedDataCenterIpv4GuestSubnet() {
        ReleaseDedicatedIpv4SubnetForZoneCmd cmd = new ReleaseDedicatedIpv4SubnetForZoneCmd();
        ReflectionTestUtils.setField(cmd,"id", zoneSubnetId);
        when(dataCenterIpv4GuestSubnetDao.findById(zoneSubnetId)).thenReturn(subnetVO);

        routedIpv4Manager.releaseDedicatedDataCenterIpv4GuestSubnet(cmd);

        verify(subnetVO).setDomainId(null);
        verify(subnetVO).setAccountId(null);
        verify(dataCenterIpv4GuestSubnetDao).update(zoneSubnetId, subnetVO);
    }

    @Test
    public void testCreateIpv4SubnetForGuestNetwork1() {
        CreateIpv4SubnetForGuestNetworkCmd cmd = new CreateIpv4SubnetForGuestNetworkCmd();

        try {
            routedIpv4Manager.createIpv4SubnetForGuestNetwork(cmd);
            Assert.fail("creating IPv4 subnet for guest network should fail.");
        } catch (InvalidParameterValueException ex) {
            Assert.assertEquals("One of subnet and cidrsize must be specified", ex.getMessage());
        }
    }

    @Test
    public void testCreateIpv4SubnetForGuestNetwork2() {
        CreateIpv4SubnetForGuestNetworkCmd cmd = new CreateIpv4SubnetForGuestNetworkCmd();
        ReflectionTestUtils.setField(cmd,"subnet", subnet);
        ReflectionTestUtils.setField(cmd,"cidrSize", cidrSize);
        try {
            routedIpv4Manager.createIpv4SubnetForGuestNetwork(cmd);
            Assert.fail("creating IPv4 subnet for guest network should fail.");
        } catch (InvalidParameterValueException ex) {
            Assert.assertEquals("subnet and cidrsize are mutually exclusive", ex.getMessage());
        }
    }

    @Test
    public void testCreateIpv4SubnetForGuestNetwork3() {
        CreateIpv4SubnetForGuestNetworkCmd cmd = new CreateIpv4SubnetForGuestNetworkCmd();
        ReflectionTestUtils.setField(cmd,"subnet", subnet);
        ReflectionTestUtils.setField(cmd,"parentId", zoneSubnetId);
        when(dataCenterIpv4GuestSubnetDao.findById(zoneSubnetId)).thenReturn(subnetVO);
        doReturn(ipv4GuestSubnetNetworkMap).when(routedIpv4Manager).createIpv4SubnetFromParentSubnet(subnetVO, subnet);

        Ipv4GuestSubnetNetworkMap result = routedIpv4Manager.createIpv4SubnetForGuestNetwork(cmd);

        Assert.assertEquals(ipv4GuestSubnetNetworkMap, result);
    }

    @Test
    public void testCreateIpv4SubnetForGuestNetwork4() {
        CreateIpv4SubnetForGuestNetworkCmd cmd = new CreateIpv4SubnetForGuestNetworkCmd();
        ReflectionTestUtils.setField(cmd,"cidrSize", cidrSize);
        ReflectionTestUtils.setField(cmd,"parentId", zoneSubnetId);
        when(dataCenterIpv4GuestSubnetDao.findById(zoneSubnetId)).thenReturn(subnetVO);
        doReturn(ipv4GuestSubnetNetworkMap).when(routedIpv4Manager).createIpv4SubnetFromParentSubnet(subnetVO, cidrSize);

        Ipv4GuestSubnetNetworkMap result = routedIpv4Manager.createIpv4SubnetForGuestNetwork(cmd);

        Assert.assertEquals(ipv4GuestSubnetNetworkMap, result);
    }

    @Test
    public void testDeleteIpv4SubnetForGuestNetwork() {
        DeleteIpv4SubnetForGuestNetworkCmd cmd = new DeleteIpv4SubnetForGuestNetworkCmd();
        ReflectionTestUtils.setField(cmd,"id", ipv4GuestSubnetNetworkMapId);

        when(ipv4GuestSubnetNetworkMapDao.findById(ipv4GuestSubnetNetworkMapId)).thenReturn(ipv4GuestSubnetNetworkMap);
        when(ipv4GuestSubnetNetworkMap.getState()).thenReturn(ipv4GuestSubnetNetworkMapState);
        when(ipv4GuestSubnetNetworkMap.getNetworkId()).thenReturn(null);

        routedIpv4Manager.deleteIpv4SubnetForGuestNetwork(cmd);

        verify(ipv4GuestSubnetNetworkMapDao).remove(ipv4GuestSubnetNetworkMapId);
    }

    @Test
    public void testReleaseIpv4SubnetForGuestNetwork() {
        when(ipv4GuestSubnetNetworkMapDao.findByNetworkId(networkId)).thenReturn(ipv4GuestSubnetNetworkMap);
        when(ipv4GuestSubnetNetworkMap.getId()).thenReturn(ipv4GuestSubnetNetworkMapId);

        routedIpv4Manager.releaseIpv4SubnetForGuestNetwork(networkId);

        verify(ipv4GuestSubnetNetworkMapDao).remove(ipv4GuestSubnetNetworkMapId);
    }

    @Test
    public void testReleaseIpv4SubnetForVpc() {
        when(ipv4GuestSubnetNetworkMapDao.findByVpcId(vpcId)).thenReturn(ipv4GuestSubnetNetworkMap);
        when(ipv4GuestSubnetNetworkMap.getId()).thenReturn(ipv4GuestSubnetNetworkMapId);

        routedIpv4Manager.releaseIpv4SubnetForVpc(vpcId);

        verify(ipv4GuestSubnetNetworkMapDao).remove(ipv4GuestSubnetNetworkMapId);
    }

    @Test
    public void testCreateIpv4SubnetForGuestNetworkResponse() {
        when(ipv4GuestSubnetNetworkMap.getCreated()).thenReturn(created);
        when(ipv4GuestSubnetNetworkMap.getSubnet()).thenReturn(subnet);
        when(ipv4GuestSubnetNetworkMap.getState()).thenReturn(ipv4GuestSubnetNetworkMapState);
        when(ipv4GuestSubnetNetworkMap.getUuid()).thenReturn(uuid);

        when(ipv4GuestSubnetNetworkMap.getNetworkId()).thenReturn(networkId);
        apiDBUtilsMocked.when(() -> ApiDBUtils.findNetworkById(Mockito.anyLong())).thenReturn(network);
        when(network.getName()).thenReturn(networkName);
        when(network.getUuid()).thenReturn(networkUuid);
        when(network.getDataCenterId()).thenReturn(zoneId);

        when(ipv4GuestSubnetNetworkMap.getVpcId()).thenReturn(vpcId);
        apiDBUtilsMocked.when(() -> ApiDBUtils.findVpcById(Mockito.anyLong())).thenReturn(vpc);
        when(vpc.getName()).thenReturn(vpcName);
        when(vpc.getUuid()).thenReturn(vpcUuid);
        when(vpc.getZoneId()).thenReturn(zoneId);

        when(ipv4GuestSubnetNetworkMap.getParentId()).thenReturn(zoneSubnetId);
        when(dataCenterIpv4GuestSubnetDao.findById(zoneSubnetId)).thenReturn(subnetVO);
        when(subnetVO.getSubnet()).thenReturn(subnet);
        when(subnetVO.getUuid()).thenReturn(zoneSubnetUuid);
        when(subnetVO.getDataCenterId()).thenReturn(zoneId);

        apiDBUtilsMocked.when(() -> ApiDBUtils.findZoneById(zoneId)).thenReturn(zone);
        when(zone.getName()).thenReturn(zoneName);
        when(zone.getUuid()).thenReturn(zoneUuid);

        Ipv4SubnetForGuestNetworkResponse response = routedIpv4Manager.createIpv4SubnetForGuestNetworkResponse(ipv4GuestSubnetNetworkMap);

        Assert.assertEquals(created, response.getCreated());
        Assert.assertEquals(subnet, response.getSubnet());
        Assert.assertEquals(ipv4GuestSubnetNetworkMapState.name(), response.getState());
        Assert.assertEquals(uuid, response.getId());
        Assert.assertEquals(networkName, response.getNetworkName());
        Assert.assertEquals(networkUuid, response.getNetworkId());
        Assert.assertEquals(vpcName, response.getVpcName());
        Assert.assertEquals(vpcUuid, response.getVpcId());
        Assert.assertEquals(subnet, response.getParentSubnet());
        Assert.assertEquals(zoneSubnetUuid, response.getParentId());
        Assert.assertEquals(zoneUuid, response.getZoneId());
        Assert.assertEquals(zoneName, response.getZoneName());
        Assert.assertEquals("ipv4subnetforguestnetwork", response.getObjectName());
    }

    @Test
    public void testGetOrCreateIpv4SubnetForGuestNetworkOrVpcInternalByCidr1() {
        when(ipv4GuestSubnetNetworkMapDao.findBySubnet(subnet)).thenReturn(ipv4GuestSubnetNetworkMap);
        when(ipv4GuestSubnetNetworkMap.getNetworkId()).thenReturn(null);
        when(ipv4GuestSubnetNetworkMap.getVpcId()).thenReturn(null);
        when(ipv4GuestSubnetNetworkMap.getParentId()).thenReturn(zoneSubnetId);
        when(dataCenterIpv4GuestSubnetDao.findById(zoneSubnetId)).thenReturn(subnetVO);
        when(subnetVO.getDomainId()).thenReturn(domainId);
        when(subnetVO.getAccountId()).thenReturn(accountId);

        routedIpv4Manager.getOrCreateIpv4SubnetForGuestNetworkOrVpcInternal(subnet, domainId, accountId, zoneId);
    }

    @Test
    public void testGetOrCreateIpv4SubnetForGuestNetworkOrVpcInternalByCidr2() {
        when(ipv4GuestSubnetNetworkMapDao.findBySubnet(subnet)).thenReturn(null);

        routedIpv4Manager.getOrCreateIpv4SubnetForGuestNetworkOrVpcInternal(subnet, domainId, accountId, zoneId);

        verify(ipv4GuestSubnetNetworkMapDao).persist(any(Ipv4GuestSubnetNetworkMapVO.class));
    }

    @Test
    public void testGetOrCreateIpv4SubnetForGuestNetworkOrVpcInternalByCidr3() {
        when(ipv4GuestSubnetNetworkMapDao.findBySubnet(subnet)).thenReturn(null);
        when(routedIpv4Manager.getParentOfNetworkCidr(zoneId, subnet)).thenReturn(subnetVO);
        when(subnetVO.getDomainId()).thenReturn(domainId);
        when(subnetVO.getAccountId()).thenReturn(accountId);

        routedIpv4Manager.getOrCreateIpv4SubnetForGuestNetworkOrVpcInternal(subnet, domainId, accountId, zoneId);

        verify(ipv4GuestSubnetNetworkMapDao).persist(any(Ipv4GuestSubnetNetworkMapVO.class));
    }

    @Test
    public void testGetOrCreateIpv4SubnetForGuestNetworkOrVpcInternalByCidrSize1() {
        Ipv4GuestSubnetNetworkMap result = routedIpv4Manager.getOrCreateIpv4SubnetForGuestNetworkOrVpcInternal(cidrSize, domainId, accountId, zoneId);
        Assert.assertNull(result);
    }

    @Test
    public void testGetOrCreateIpv4SubnetForGuestNetworkOrVpcInternalByCidrSize2() {
        DataCenterIpv4GuestSubnetVO subnet1 = Mockito.mock(DataCenterIpv4GuestSubnetVO.class);
        when(dataCenterIpv4GuestSubnetDao.listByDataCenterIdAndAccountId(zoneId, accountId)).thenReturn(Arrays.asList(subnet1));
        DataCenterIpv4GuestSubnetVO subnet2 = Mockito.mock(DataCenterIpv4GuestSubnetVO.class);
        when(dataCenterIpv4GuestSubnetDao.listByDataCenterIdAndDomainId(zoneId, domainId)).thenReturn(Arrays.asList(subnet2));
        DataCenterIpv4GuestSubnetVO subnet3 = Mockito.mock(DataCenterIpv4GuestSubnetVO.class);
        when(dataCenterIpv4GuestSubnetDao.listNonDedicatedByDataCenterId(zoneId)).thenReturn(Arrays.asList(subnet3));

        doReturn(null).doReturn(null).doReturn(ipv4GuestSubnetNetworkMap).when(routedIpv4Manager).getIpv4SubnetForGuestNetworkOrVpcInternal(eq(cidrSize), any());

        Ipv4GuestSubnetNetworkMap result = routedIpv4Manager.getOrCreateIpv4SubnetForGuestNetworkOrVpcInternal(cidrSize, domainId, accountId, zoneId);

        Assert.assertEquals(ipv4GuestSubnetNetworkMap, result);
        verify(routedIpv4Manager, times(3)).getIpv4SubnetForGuestNetworkOrVpcInternal(eq(cidrSize), any());
    }

    @Test
    public void testGetOrCreateIpv4SubnetForGuestNetworkOrVpcInternalByCidrAndParentSubnet1() {
        Ipv4GuestSubnetNetworkMap result = routedIpv4Manager.getOrCreateIpv4SubnetForGuestNetworkOrVpcInternal(cidrSize, subnetVO);
        Assert.assertNull(result);
    }

    @Test
    public void testGetOrCreateIpv4SubnetForGuestNetworkOrVpcInternalByCidrAndParentSubnet2() {
        when(subnetVO.getId()).thenReturn(zoneSubnetId);
        when(ipv4GuestSubnetNetworkMapDao.findFirstAvailable(zoneSubnetId, cidrSize)).thenReturn(ipv4GuestSubnetNetworkMap);

        Ipv4GuestSubnetNetworkMap result = routedIpv4Manager.getOrCreateIpv4SubnetForGuestNetworkOrVpcInternal(cidrSize, subnetVO);
        Assert.assertEquals(ipv4GuestSubnetNetworkMap, result);
    }

    @Test
    public void testGetOrCreateIpv4SubnetForGuestNetworkOrVpcInternalByCidrAndParentSubnet3() {
        when(subnetVO.getId()).thenReturn(zoneSubnetId);
        when(ipv4GuestSubnetNetworkMapDao.findFirstAvailable(zoneSubnetId, cidrSize)).thenReturn(null);
        doReturn(ipv4GuestSubnetNetworkMap).when(routedIpv4Manager).createIpv4SubnetFromParentSubnet(subnetVO, cidrSize);

        Ipv4GuestSubnetNetworkMap result = routedIpv4Manager.getOrCreateIpv4SubnetForGuestNetworkOrVpcInternal(cidrSize, subnetVO);
        Assert.assertEquals(ipv4GuestSubnetNetworkMap, result);
    }

    @Test
    public void testGetParentOfNetworkCidr() {
        when(subnetVO.getId()).thenReturn(zoneSubnetId);
        when(subnetVO.getSubnet()).thenReturn(subnet);
        when(dataCenterIpv4GuestSubnetDao.listByDataCenterId(zoneId)).thenReturn(Arrays.asList(subnetVO));
        when(ipv4GuestSubnetNetworkMapDao.listByParent(zoneSubnetId)).thenReturn(Arrays.asList(ipv4GuestSubnetNetworkMap));
        when(ipv4GuestSubnetNetworkMap.getSubnet()).thenReturn(subnetForNetwork);

        try {
            routedIpv4Manager.getParentOfNetworkCidr(zoneId, newSubnetForNetworkTooSmall);
            Assert.fail("Getting parent of network cidr should fail.");
        } catch (InvalidParameterValueException ex) {
            Assert.assertEquals(String.format("Existing subnet %s has overlap with: %s", subnetForNetwork, newSubnetForNetworkTooSmall), ex.getMessage());
        }

        try {
            routedIpv4Manager.getParentOfNetworkCidr(zoneId, newSubnetForNetworkTooBig);
            Assert.fail("Getting parent of network cidr should fail.");
        } catch (InvalidParameterValueException ex) {
            Assert.assertEquals(String.format("Existing zone subnet %s has overlap with: %s", subnet, newSubnetForNetworkTooBig), ex.getMessage());
        }

        DataCenterIpv4GuestSubnet result = routedIpv4Manager.getParentOfNetworkCidr(zoneId, newSubnetForNetwork);
        Assert.assertEquals(subnetVO, result);
    }

    @Test
    public void testCreateIpv4SubnetFromParentSubnetByCidr() {
        when(subnetVO.getSubnet()).thenReturn(subnet);
        when(subnetVO.getId()).thenReturn(zoneSubnetId);

        try {
            routedIpv4Manager.createIpv4SubnetFromParentSubnet(subnetVO, newSubnetForNetworkTooBig);
            Assert.fail("Creating ipv4 subnet should fail.");
        } catch (CloudRuntimeException ex) {
            Assert.assertEquals(String.format("networkCidr %s is not within parent cidr: %s", newSubnetForNetworkTooBig, subnet), ex.getMessage());
        }

        when(ipv4GuestSubnetNetworkMap.getSubnet()).thenReturn(subnetForNetwork);
        when(ipv4GuestSubnetNetworkMapDao.listByParent(zoneSubnetId)).thenReturn(Arrays.asList(ipv4GuestSubnetNetworkMap));
        when(ipv4GuestSubnetNetworkMapDao.persist(any())).thenReturn(ipv4GuestSubnetNetworkMap);

        Ipv4GuestSubnetNetworkMap result = routedIpv4Manager.createIpv4SubnetFromParentSubnet(subnetVO, newSubnetForNetwork);
        Assert.assertEquals(ipv4GuestSubnetNetworkMap, result);
    }

    @Test
    public void testCreateIpv4SubnetFromParentSubnetByCidrSize() {
        when(subnetVO.getId()).thenReturn(zoneSubnetId);
        when(dataCenterIpv4GuestSubnetDao.findById(zoneSubnetId)).thenReturn(subnetVO);

        Ipv4GuestSubnetNetworkMapVO ipv4GuestSubnetNetworkMap1 = new Ipv4GuestSubnetNetworkMapVO(zoneSubnetId, "172.16.1.0/28", null, Ipv4GuestSubnetNetworkMap.State.Free);
        Ipv4GuestSubnetNetworkMapVO ipv4GuestSubnetNetworkMap2 = new Ipv4GuestSubnetNetworkMapVO(zoneSubnetId, "172.16.1.64/28", null, Ipv4GuestSubnetNetworkMap.State.Free);
        when(ipv4GuestSubnetNetworkMapDao.listByParent(zoneSubnetId)).thenReturn(Arrays.asList(ipv4GuestSubnetNetworkMap1, ipv4GuestSubnetNetworkMap2));
        when(subnetVO.getSubnet()).thenReturn("172.16.1.0/24");

        String networkCidr = routedIpv4Manager.createIpv4SubnetStringFromParentSubnet(subnetVO, 28);
        Assert.assertEquals("172.16.1.16/28", networkCidr);
        networkCidr = routedIpv4Manager.createIpv4SubnetStringFromParentSubnet(subnetVO, 27);
        Assert.assertEquals("172.16.1.32/27", networkCidr);
        networkCidr = routedIpv4Manager.createIpv4SubnetStringFromParentSubnet(subnetVO, 26);
        Assert.assertEquals("172.16.1.128/26", networkCidr);
        networkCidr = routedIpv4Manager.createIpv4SubnetStringFromParentSubnet(subnetVO, 25);
        Assert.assertEquals("172.16.1.128/25", networkCidr);

        Ipv4GuestSubnetNetworkMapVO ipv4GuestSubnetNetworkMap3 = new Ipv4GuestSubnetNetworkMapVO(zoneSubnetId, "172.16.1.16/28", null, Ipv4GuestSubnetNetworkMap.State.Free);
        Ipv4GuestSubnetNetworkMapVO ipv4GuestSubnetNetworkMap4 = new Ipv4GuestSubnetNetworkMapVO(zoneSubnetId, "172.16.1.128/28", null, Ipv4GuestSubnetNetworkMap.State.Free);
        when(ipv4GuestSubnetNetworkMapDao.listByParent(zoneSubnetId)).thenReturn(Arrays.asList(ipv4GuestSubnetNetworkMap1, ipv4GuestSubnetNetworkMap2,
                ipv4GuestSubnetNetworkMap3, ipv4GuestSubnetNetworkMap4));
        networkCidr = routedIpv4Manager.createIpv4SubnetStringFromParentSubnet(subnetVO, 28);
        Assert.assertEquals("172.16.1.80/28", networkCidr);
        networkCidr = routedIpv4Manager.createIpv4SubnetStringFromParentSubnet(subnetVO, 27);
        Assert.assertEquals("172.16.1.32/27", networkCidr);
        networkCidr = routedIpv4Manager.createIpv4SubnetStringFromParentSubnet(subnetVO, 26);
        Assert.assertEquals("172.16.1.192/26", networkCidr);
        try {
            networkCidr = routedIpv4Manager.createIpv4SubnetStringFromParentSubnet(subnetVO, 25);
            Assert.fail("Creating ipv4 subnet should fail.");
        } catch (CloudRuntimeException ex) {
            Assert.assertEquals("Failed to automatically allocate a subnet with specified cidrsize", ex.getMessage());
        }
    }

    @Test
    public void testAssignIpv4SubnetToNetwork() {
        when(network.getId()).thenReturn(networkId);
        when(network.getCidr()).thenReturn(subnet);
        when(ipv4GuestSubnetNetworkMapDao.findBySubnet(subnet)).thenReturn(ipv4GuestSubnetNetworkMap);
        when(ipv4GuestSubnetNetworkMap.getId()).thenReturn(ipv4GuestSubnetNetworkMapId);

        routedIpv4Manager.assignIpv4SubnetToNetwork(network);

        verify(ipv4GuestSubnetNetworkMapDao).update(eq(ipv4GuestSubnetNetworkMapId), any());
    }

    @Test
    public void testAssignIpv4SubnetToVpc() {
        when(vpc.getId()).thenReturn(vpcId);
        when(vpc.getCidr()).thenReturn(subnet);
        when(ipv4GuestSubnetNetworkMapDao.findBySubnet(subnet)).thenReturn(ipv4GuestSubnetNetworkMap);
        when(ipv4GuestSubnetNetworkMap.getId()).thenReturn(ipv4GuestSubnetNetworkMapId);

        routedIpv4Manager.assignIpv4SubnetToVpc(vpc);

        verify(ipv4GuestSubnetNetworkMapDao).update(eq(ipv4GuestSubnetNetworkMapId), any());
    }

    @Test
    public void testCreateRoutingFirewallRule() throws NetworkRuleConflictException {
        CreateRoutingFirewallRuleCmd cmd = new CreateRoutingFirewallRuleCmd();
        ReflectionTestUtils.setField(cmd, "protocol", "tcp");
        List<String> sourceCidrList = Arrays.asList("192.168.0.0/24", "10.0.0.0/8");
        ReflectionTestUtils.setField(cmd, "sourceCidrList", sourceCidrList);
        List<String> destinationCidrlist = Arrays.asList("192.168.0.0/24", "10.0.0.0/8");
        ReflectionTestUtils.setField(cmd, "destinationCidrlist", destinationCidrlist);
        ReflectionTestUtils.setField(cmd, "publicStartPort", 1111);
        ReflectionTestUtils.setField(cmd, "publicEndPort", 2222);
        ReflectionTestUtils.setField(cmd, "networkId", networkId);

        when(networkModel.getNetwork(networkId)).thenReturn(network);
        when(network.getId()).thenReturn(networkId);
        when(network.getDomainId()).thenReturn(domainId);
        when(network.getAccountId()).thenReturn(accountId);
        VirtualRouterElement virtualRouterElement = new VirtualRouterElement();
        when(networkModel.getNetworkServiceCapabilities(networkId, Network.Service.Firewall)).thenReturn(virtualRouterElement.getCapabilities().get(Network.Service.Firewall));
        when(firewallDao.persist(any())).thenReturn(rule);
        when(firewallDao.setStateToAdd(any())).thenReturn(true);

        // create Ingress rule
        ReflectionTestUtils.setField(cmd, "trafficType", "ingress");
        FirewallRule result = routedIpv4Manager.createRoutingFirewallRule(cmd);
        Assert.assertNotNull(result);
        Assert.assertEquals(rule, result);

        // create Egress rule
        ReflectionTestUtils.setField(cmd, "trafficType", "egress");
        result = routedIpv4Manager.createRoutingFirewallRule(cmd);
        Assert.assertNotNull(result);
        Assert.assertEquals(rule, result);
    }

    @Test
    public void testUpdateRoutingFirewallRule1() {
        UpdateRoutingFirewallRuleCmd cmd = new UpdateRoutingFirewallRuleCmd();
        ReflectionTestUtils.setField(cmd, "id", ruleId);

        when(firewallDao.findById(ruleId)).thenReturn(rule);
        when(rule.getId()).thenReturn(ruleId);
        when(rule.getTrafficType()).thenReturn(FirewallRule.TrafficType.Ingress);

        routedIpv4Manager.updateRoutingFirewallRule(cmd);

        verify(firewallManager).updateIngressFirewallRule(ruleId, null, true);
    }

    @Test
    public void testUpdateRoutingFirewallRule2() {
        UpdateRoutingFirewallRuleCmd cmd = new UpdateRoutingFirewallRuleCmd();
        ReflectionTestUtils.setField(cmd, "id", ruleId);

        when(firewallDao.findById(ruleId)).thenReturn(rule);
        when(rule.getId()).thenReturn(ruleId);
        when(rule.getTrafficType()).thenReturn(FirewallRule.TrafficType.Egress);

        routedIpv4Manager.updateRoutingFirewallRule(cmd);

        verify(firewallManager).updateEgressFirewallRule(ruleId, null, true);
    }

    @Test
    public void testRevokeRoutingFirewallRule1() {
        when(firewallDao.findById(ruleId)).thenReturn(rule);
        when(rule.getId()).thenReturn(ruleId);
        when(rule.getTrafficType()).thenReturn(FirewallRule.TrafficType.Ingress);

        routedIpv4Manager.revokeRoutingFirewallRule(ruleId);

        verify(firewallManager).revokeIngressFirewallRule(ruleId, true);
    }

    @Test
    public void testRevokeRoutingFirewallRule2() {
        when(firewallDao.findById(ruleId)).thenReturn(rule);
        when(rule.getId()).thenReturn(ruleId);
        when(rule.getTrafficType()).thenReturn(FirewallRule.TrafficType.Egress);

        routedIpv4Manager.revokeRoutingFirewallRule(ruleId);

        verify(firewallManager).revokeEgressFirewallRule(ruleId, true);
    }

    @Test
    public void testApplyRoutingFirewallRule1() {
        when(firewallDao.findById(ruleId)).thenReturn(rule);
        when(rule.getPurpose()).thenReturn(FirewallRule.Purpose.Firewall);
        when(rule.getNetworkId()).thenReturn(networkId);

        FirewallRuleVO rule1 = Mockito.mock(FirewallRuleVO.class);
        when(firewallDao.listByNetworkPurposeTrafficType(networkId, FirewallRule.Purpose.Firewall, FirewallRule.TrafficType.Egress)).thenReturn(Arrays.asList(rule1));
        FirewallRuleVO rule2 = Mockito.mock(FirewallRuleVO.class);
        when(firewallDao.listByNetworkPurposeTrafficType(networkId, FirewallRule.Purpose.Firewall, FirewallRule.TrafficType.Ingress)).thenReturn(Arrays.asList(rule2));
        when(firewallManager.applyFirewallRules(any(), eq(false), any())).thenReturn(true);

        boolean result = routedIpv4Manager.applyRoutingFirewallRule(ruleId);
        Assert.assertTrue(result);
    }

    @Test
    public void testApplyRoutingFirewallRule2() {
        when(firewallDao.findById(ruleId)).thenReturn(rule);
        when(rule.getPurpose()).thenReturn(FirewallRule.Purpose.LoadBalancing);

        boolean result = routedIpv4Manager.applyRoutingFirewallRule(ruleId);
        Assert.assertFalse(result);
    }

    @Test
    public void testCreateBgpPeer() {
        CreateBgpPeerCmd cmd = new CreateBgpPeerCmd();
        ReflectionTestUtils.setField(cmd, "zoneId", zoneId);
        ReflectionTestUtils.setField(cmd, "ip4Address", ip4Address);
        ReflectionTestUtils.setField(cmd, "ip6Address", ip6Address);
        ReflectionTestUtils.setField(cmd, "asNumber", asNumber);
        ReflectionTestUtils.setField(cmd, "password", password);

        when(bgpPeerDao.findByZoneAndAsNumberAndAddress(zoneId, asNumber, ip4Address, null)).thenReturn(null);
        when(bgpPeerDao.findByZoneAndAsNumberAndAddress(zoneId, asNumber, null, ip6Address)).thenReturn(null);

        routedIpv4Manager.createBgpPeer(cmd);

        verify(bgpPeerDao).persist(any(BgpPeerVO.class), any(Map.class));
    }

    @Test
    public void testCreateBgpPeerResponse() {
        BgpPeerVO bgpPeer = Mockito.mock(BgpPeerVO.class);
        when(bgpPeer.getDataCenterId()).thenReturn(zoneId);
        when(bgpPeer.getDomainId()).thenReturn(domainId);
        when(bgpPeer.getAccountId()).thenReturn(accountId);
        when(bgpPeer.getCreated()).thenReturn(created);
        when(bgpPeer.getAsNumber()).thenReturn(asNumber);
        when(bgpPeer.getUuid()).thenReturn(bgpPeerUuid);
        when(bgpPeer.getIp4Address()).thenReturn(ip4Address);
        when(bgpPeer.getIp6Address()).thenReturn(ip6Address);

        apiDBUtilsMocked.when(() -> ApiDBUtils.findZoneById(zoneId)).thenReturn(zone);
        when(zone.getName()).thenReturn(zoneName);
        when(zone.getUuid()).thenReturn(zoneUuid);

        apiDBUtilsMocked.when(() -> ApiDBUtils.findDomainById(domainId)).thenReturn(domain);
        when(domain.getName()).thenReturn(domainName);
        when(domain.getUuid()).thenReturn(domainUuid);

        apiDBUtilsMocked.when(() -> ApiDBUtils.findAccountById(accountId)).thenReturn(account);
        when(account.getType()).thenReturn(Account.Type.PROJECT);
        when(account.getId()).thenReturn(accountId);

        apiDBUtilsMocked.when(() -> ApiDBUtils.findProjectByProjectAccountId(accountId)).thenReturn(project);
        when(project.getName()).thenReturn(projectName);
        when(project.getUuid()).thenReturn(projectUuid);

        BgpPeerResponse response = routedIpv4Manager.createBgpPeerResponse(bgpPeer);

        Assert.assertEquals(created, response.getCreated());
        Assert.assertEquals(asNumber, response.getAsNumber());
        Assert.assertEquals(ip4Address, response.getIp4Address());
        Assert.assertEquals(ip6Address, response.getIp6Address());
        Assert.assertEquals(bgpPeerUuid, response.getId());
        Assert.assertEquals(zoneUuid, response.getZoneId());
        Assert.assertEquals(zoneName, response.getZoneName());
        Assert.assertEquals(domainUuid, response.getDomainId());
        Assert.assertEquals(domainName, response.getDomainName());
        Assert.assertEquals(projectName, response.getProjectName());
        Assert.assertEquals(projectUuid, response.getProjectId());
        Assert.assertNull(response.getAccountName());

        Assert.assertEquals("bgppeer", response.getObjectName());

    }

    @Test
    public void testDeleteBgpPeer1() {
        DeleteBgpPeerCmd cmd = new DeleteBgpPeerCmd();
        ReflectionTestUtils.setField(cmd, "id", bgpPeerId);

        BgpPeerNetworkMapVO bgpPeerNetworkMapVO = Mockito.mock(BgpPeerNetworkMapVO.class);
        when(bgpPeerNetworkMapDao.listByBgpPeerId(bgpPeerId)).thenReturn(Arrays.asList(bgpPeerNetworkMapVO));

        try {
            routedIpv4Manager.deleteBgpPeer(cmd);
            Assert.fail("Deleting BGP peer should fail.");
        } catch (InvalidParameterValueException ex) {
            Assert.assertEquals("The BGP peer is being used by 1 guest networks.", ex.getMessage());
        }
    }

    @Test
    public void testDeleteBgpPeer2() {
        DeleteBgpPeerCmd cmd = new DeleteBgpPeerCmd();
        ReflectionTestUtils.setField(cmd, "id", bgpPeerId);

        routedIpv4Manager.deleteBgpPeer(cmd);

        verify(bgpPeerDao).remove(bgpPeerId);
    }

    @Test
    public void testUpdateBgpPeer() {
        UpdateBgpPeerCmd cmd = new UpdateBgpPeerCmd();
        ReflectionTestUtils.setField(cmd, "id", bgpPeerId);
        ReflectionTestUtils.setField(cmd, "asNumber", asNumber+1);
        ReflectionTestUtils.setField(cmd, "ip4Address", ip4Address + "1");
        ReflectionTestUtils.setField(cmd, "ip6Address", ip6Address + "1");

        when(bgpPeerDao.findById(bgpPeerId)).thenReturn(bgpPeer);
        when(bgpPeer.getDataCenterId()).thenReturn(zoneId);

        routedIpv4Manager.updateBgpPeer(cmd);

        verify(bgpPeerDao).update(bgpPeerId, bgpPeer);
        verify(bgpPeerDao).findByZoneAndAsNumberAndAddress(zoneId, asNumber+1, ip4Address + "1", null);
        verify(bgpPeerDao).findByZoneAndAsNumberAndAddress(zoneId, asNumber+1, null, ip6Address + "1");
    }

    @Test
    public void testDedicateBgpPeer() {
        DedicateBgpPeerCmd cmd = new DedicateBgpPeerCmd();
        ReflectionTestUtils.setField(cmd,"id", bgpPeerId);
        ReflectionTestUtils.setField(cmd,"domainId", domainId);
        ReflectionTestUtils.setField(cmd,"accountName", accountName);
        ReflectionTestUtils.setField(cmd,"projectId", null);

        when(bgpPeerDao.findById(bgpPeerId)).thenReturn(bgpPeer);
        when(accountManager.finalyzeAccountId(accountName, domainId, null, false)).thenReturn(accountId);
        when(accountManager.getAccount(accountId)).thenReturn(account);
        when(account.getDomainId()).thenReturn(domainId);

        routedIpv4Manager.dedicateBgpPeer(cmd);

        verify(bgpPeerDao).update(bgpPeerId, bgpPeer);
        verify(bgpPeerNetworkMapDao).listUsedNetworksByOtherDomains(bgpPeerId, domainId);
        verify(bgpPeerNetworkMapDao).listUsedVpcsByOtherDomains(bgpPeerId, domainId);
        verify(bgpPeerNetworkMapDao).listUsedNetworksByOtherAccounts(bgpPeerId, accountId);
        verify(bgpPeerNetworkMapDao).listUsedVpcsByOtherAccounts(bgpPeerId, accountId);
    }

    @Test
    public void testReleaseDedicatedBgpPeer() {
        ReleaseDedicatedBgpPeerCmd cmd = new ReleaseDedicatedBgpPeerCmd();
        ReflectionTestUtils.setField(cmd,"id", bgpPeerId);
        when(bgpPeerDao.findById(bgpPeerId)).thenReturn(bgpPeer);

        routedIpv4Manager.releaseDedicatedBgpPeer(cmd);

        verify(bgpPeer).setDomainId(null);
        verify(bgpPeer).setAccountId(null);
        verify(bgpPeerDao).update(bgpPeerId, bgpPeer);
    }

    @Test
    public void testChangeBgpPeersForNetwork() {
        ChangeBgpPeersForNetworkCmd cmd = new ChangeBgpPeersForNetworkCmd();
        ReflectionTestUtils.setField(cmd,"networkId", networkId);
        List<Long> bgpPeerIds = Arrays.asList(bgpPeerId);
        ReflectionTestUtils.setField(cmd,"bgpPeerIds", bgpPeerIds);

        when(networkDao.findById(networkId)).thenReturn(network);

        try {
            when(network.getVpcId()).thenReturn(vpcId);
            routedIpv4Manager.changeBgpPeersForNetwork(cmd);
            Assert.fail("Changing BGP peers for guest network should fail.");
        } catch (InvalidParameterValueException ex) {
            Assert.assertEquals("The BGP peers of VPC tiers will inherit from the VPC, do not add separately.", ex.getMessage());
        }

        when(network.getVpcId()).thenReturn(null);
        when(network.getAccountId()).thenReturn(accountId);
        when(accountManager.getAccount(accountId)).thenReturn(account);
        when(network.getNetworkOfferingId()).thenReturn(networkOfferingId);
        when(networkOfferingDao.findById(networkOfferingId)).thenReturn(networkOffering);
        when(network.getDataCenterId()).thenReturn(zoneId);

        try {
            when(networkOffering.getNetworkMode()).thenReturn(NetworkOffering.NetworkMode.ROUTED);
            when(networkOffering.getRoutingMode()).thenReturn(NetworkOffering.RoutingMode.Static);
            routedIpv4Manager.changeBgpPeersForNetwork(cmd);
            Assert.fail("Changing BGP peers for guest network should fail.");
        } catch (InvalidParameterValueException ex) {
            Assert.assertEquals("The network does not support Dynamic routing", ex.getMessage());
        }

        when(networkOffering.getRoutingMode()).thenReturn(NetworkOffering.RoutingMode.Dynamic);

        doNothing().when(routedIpv4Manager).validateBgpPeers(account, zoneId, bgpPeerIds);
        doReturn(network).when(routedIpv4Manager).changeBgpPeersForNetworkInternal(network, bgpPeerIds);

        routedIpv4Manager.changeBgpPeersForNetwork(cmd);

        verify(routedIpv4Manager).validateBgpPeers(account, zoneId, bgpPeerIds);
        verify(routedIpv4Manager).changeBgpPeersForNetworkInternal(network, bgpPeerIds);
    }

    @Test
    public void testChangeBgpPeersForNetworkInternal() throws ResourceUnavailableException {
        Long bgpPeerId1 = 11L; // to be kept
        Long bgpPeerId2 = 12L; // to be removed
        Long bgpPeerId3 = 13L; // to be added
        Long bgpPeerNetworkMapId2 = 14L; // to be removed

        when(network.getId()).thenReturn(networkId);
        when(networkDao.findById(networkId)).thenReturn(network);

        BgpPeerNetworkMapVO bgpPeerNetworkMap1 = Mockito.mock(BgpPeerNetworkMapVO.class);
        when(bgpPeerNetworkMap1.getBgpPeerId()).thenReturn(bgpPeerId1);
        when(bgpPeerNetworkMap1.getState()).thenReturn(BgpPeer.State.Active);
        BgpPeerNetworkMapVO bgpPeerNetworkMap2 = Mockito.mock(BgpPeerNetworkMapVO.class);
        when(bgpPeerNetworkMap2.getBgpPeerId()).thenReturn(bgpPeerId2);
        when(bgpPeerNetworkMap2.getState()).thenReturn(BgpPeer.State.Revoke);
        BgpPeerNetworkMapVO bgpPeerNetworkMap3 = Mockito.mock(BgpPeerNetworkMapVO.class);
        when(bgpPeerNetworkMap3.getState()).thenReturn(BgpPeer.State.Add);

        when(bgpPeerNetworkMap2.getId()).thenReturn(bgpPeerNetworkMapId2);

        when(bgpPeerNetworkMapDao.listByNetworkId(networkId)).thenReturn(Arrays.asList(bgpPeerNetworkMap1, bgpPeerNetworkMap2))
                .thenReturn(Arrays.asList(bgpPeerNetworkMap1, bgpPeerNetworkMap2, bgpPeerNetworkMap3));
        when(bgpService.applyBgpPeers(network, false)).thenReturn(true);

        Network result = routedIpv4Manager.changeBgpPeersForNetworkInternal(network, Arrays.asList(bgpPeerId1, bgpPeerId3));
        Assert.assertEquals(network, result);

        verify(bgpPeerNetworkMap2).setState(BgpPeer.State.Revoke);
        verify(bgpPeerNetworkMapDao).persist(any());
        verify(bgpPeerNetworkMap3).setState(BgpPeer.State.Active);
        verify(bgpPeerNetworkMapDao).remove(bgpPeerNetworkMapId2);
    }

    @Test
    public void testChangeBgpPeersForNetworkInternalFailure() throws ResourceUnavailableException {
        Long bgpPeerId1 = 11L; // to be kept
        Long bgpPeerId2 = 12L; // to be removed, but finally not
        Long bgpPeerId3 = 13L; // to be added, but finally not
        Long bgpPeerNetworkMapId3 = 15L; // to be added, but finally not

        when(network.getId()).thenReturn(networkId);

        BgpPeerNetworkMapVO bgpPeerNetworkMap1 = Mockito.mock(BgpPeerNetworkMapVO.class);
        when(bgpPeerNetworkMap1.getBgpPeerId()).thenReturn(bgpPeerId1);
        when(bgpPeerNetworkMap1.getState()).thenReturn(BgpPeer.State.Active);
        BgpPeerNetworkMapVO bgpPeerNetworkMap2 = Mockito.mock(BgpPeerNetworkMapVO.class);
        when(bgpPeerNetworkMap2.getBgpPeerId()).thenReturn(bgpPeerId2);
        when(bgpPeerNetworkMap2.getState()).thenReturn(BgpPeer.State.Revoke);
        BgpPeerNetworkMapVO bgpPeerNetworkMap3 = Mockito.mock(BgpPeerNetworkMapVO.class);
        when(bgpPeerNetworkMap3.getState()).thenReturn(BgpPeer.State.Add);

        when(bgpPeerNetworkMap3.getId()).thenReturn(bgpPeerNetworkMapId3);

        when(bgpPeerNetworkMapDao.listByNetworkId(networkId)).thenReturn(Arrays.asList(bgpPeerNetworkMap1, bgpPeerNetworkMap2))
                .thenReturn(Arrays.asList(bgpPeerNetworkMap1, bgpPeerNetworkMap2, bgpPeerNetworkMap3));
        when(bgpService.applyBgpPeers(network, false)).thenReturn(false);

        Network result = routedIpv4Manager.changeBgpPeersForNetworkInternal(network, Arrays.asList(bgpPeerId1, bgpPeerId3));
        Assert.assertNull(result);

        verify(bgpPeerNetworkMap2).setState(BgpPeer.State.Revoke);
        verify(bgpPeerNetworkMapDao).persist(any());
        verify(bgpPeerNetworkMap2).setState(BgpPeer.State.Add);
        verify(bgpPeerNetworkMapDao).remove(bgpPeerNetworkMapId3);
    }

    @Test
    public void testValidateBgpPeers() {
        List<Long> bgpPeerIds = Arrays.asList(bgpPeerId);
        try {
            routedIpv4Manager.validateBgpPeers(account, zoneId, bgpPeerIds);
            Assert.fail("Validating BGP peers for guest network should fail.");
        } catch (InvalidParameterValueException ex) {
            Assert.assertEquals(String.format("Invalid BGP peer ID: %s", bgpPeerId), ex.getMessage());
        }

        when(account.getDomainId()).thenReturn(domainId);
        when(account.getAccountId()).thenReturn(accountId);
        when(bgpPeerDao.findById(bgpPeerId)).thenReturn(bgpPeer);
        when(bgpPeer.getUuid()).thenReturn(bgpPeerUuid);
        try {
            when(bgpPeer.getDataCenterId()).thenReturn(zoneId + 1);
            routedIpv4Manager.validateBgpPeers(account, zoneId, bgpPeerIds);
            Assert.fail("Validating BGP peers for guest network should fail.");
        } catch (InvalidParameterValueException ex) {
            Assert.assertEquals(String.format("BGP peer (ID: %s) belongs to a different zone", bgpPeerUuid), ex.getMessage());
        }

        when(bgpPeer.getDataCenterId()).thenReturn(zoneId);
        try {
            when(bgpPeer.getDomainId()).thenReturn(domainId + 1);
            routedIpv4Manager.validateBgpPeers(account, zoneId, bgpPeerIds);
            Assert.fail("Validating BGP peers for guest network should fail.");
        } catch (InvalidParameterValueException ex) {
            Assert.assertEquals(String.format("BGP peer (ID: %s) belongs to a different domain", bgpPeerUuid), ex.getMessage());
        }

        when(bgpPeer.getDomainId()).thenReturn(domainId);
        try {
            when(bgpPeer.getAccountId()).thenReturn(accountId + 1);
            routedIpv4Manager.validateBgpPeers(account, zoneId, bgpPeerIds);
            Assert.fail("Validating BGP peers for guest network should fail.");
        } catch (InvalidParameterValueException ex) {
            Assert.assertEquals(String.format("BGP peer (ID: %s) belongs to a different account", bgpPeerUuid), ex.getMessage());
        }

        when(bgpPeer.getAccountId()).thenReturn(accountId);
        routedIpv4Manager.validateBgpPeers(account, zoneId, bgpPeerIds);
    }

    @Test
    public void testChangeBgpPeersForVpc() {
        ChangeBgpPeersForVpcCmd cmd = new ChangeBgpPeersForVpcCmd();
        ReflectionTestUtils.setField(cmd,"vpcId", vpcId);
        List<Long> bgpPeerIds = Arrays.asList(bgpPeerId);
        ReflectionTestUtils.setField(cmd,"bgpPeerIds", bgpPeerIds);

        when(vpcDao.findById(vpcId)).thenReturn(vpc);
        when(vpc.getAccountId()).thenReturn(accountId);
        when(accountManager.getAccount(accountId)).thenReturn(account);
        when(vpc.getVpcOfferingId()).thenReturn(vpcOfferingId);
        when(vpcOfferingDao.findById(vpcOfferingId)).thenReturn(vpcOffering);
        when(vpc.getZoneId()).thenReturn(zoneId);

        try {
            when(vpcOffering.getNetworkMode()).thenReturn(NetworkOffering.NetworkMode.ROUTED);
            when(vpcOffering.getRoutingMode()).thenReturn(NetworkOffering.RoutingMode.Static);
            routedIpv4Manager.changeBgpPeersForVpc(cmd);
            Assert.fail("Changing BGP peers for VPC should fail.");
        } catch (InvalidParameterValueException ex) {
            Assert.assertEquals("The VPC does not support Dynamic routing", ex.getMessage());
        }

        when(vpcOffering.getRoutingMode()).thenReturn(NetworkOffering.RoutingMode.Dynamic);

        doNothing().when(routedIpv4Manager).validateBgpPeers(account, zoneId, bgpPeerIds);
        doReturn(vpc).when(routedIpv4Manager).changeBgpPeersForVpcInternal(vpc, bgpPeerIds);

        routedIpv4Manager.changeBgpPeersForVpc(cmd);

        verify(routedIpv4Manager).validateBgpPeers(account, zoneId, bgpPeerIds);
        verify(routedIpv4Manager).changeBgpPeersForVpcInternal(vpc, bgpPeerIds);
    }

    @Test
    public void testChangeBgpPeersForVpcInternal() throws ResourceUnavailableException {
        Long bgpPeerId1 = 11L; // to be kept
        Long bgpPeerId2 = 12L; // to be removed
        Long bgpPeerId3 = 13L; // to be added
        Long bgpPeerNetworkMapId2 = 14L; // to be removed

        when(vpc.getId()).thenReturn(vpcId);
        when(vpcDao.findById(vpcId)).thenReturn(vpc);

        BgpPeerNetworkMapVO bgpPeerNetworkMap1 = Mockito.mock(BgpPeerNetworkMapVO.class);
        when(bgpPeerNetworkMap1.getBgpPeerId()).thenReturn(bgpPeerId1);
        when(bgpPeerNetworkMap1.getState()).thenReturn(BgpPeer.State.Active);
        BgpPeerNetworkMapVO bgpPeerNetworkMap2 = Mockito.mock(BgpPeerNetworkMapVO.class);
        when(bgpPeerNetworkMap2.getBgpPeerId()).thenReturn(bgpPeerId2);
        when(bgpPeerNetworkMap2.getState()).thenReturn(BgpPeer.State.Revoke);
        BgpPeerNetworkMapVO bgpPeerNetworkMap3 = Mockito.mock(BgpPeerNetworkMapVO.class);
        when(bgpPeerNetworkMap3.getState()).thenReturn(BgpPeer.State.Add);

        when(bgpPeerNetworkMap2.getId()).thenReturn(bgpPeerNetworkMapId2);

        when(bgpPeerNetworkMapDao.listByVpcId(vpcId)).thenReturn(Arrays.asList(bgpPeerNetworkMap1, bgpPeerNetworkMap2))
                .thenReturn(Arrays.asList(bgpPeerNetworkMap1, bgpPeerNetworkMap2, bgpPeerNetworkMap3));
        when(bgpService.applyBgpPeers(vpc, false)).thenReturn(true);

        Vpc result = routedIpv4Manager.changeBgpPeersForVpcInternal(vpc, Arrays.asList(bgpPeerId1, bgpPeerId3));
        Assert.assertEquals(vpc, result);

        verify(bgpPeerNetworkMap2).setState(BgpPeer.State.Revoke);
        verify(bgpPeerNetworkMapDao).persist(any());
        verify(bgpPeerNetworkMap3).setState(BgpPeer.State.Active);
        verify(bgpPeerNetworkMapDao).remove(bgpPeerNetworkMapId2);
    }

    @Test
    public void testChangeBgpPeersForVpcInternalFailure() throws ResourceUnavailableException {
        Long bgpPeerId1 = 11L; // to be kept
        Long bgpPeerId2 = 12L; // to be removed, but finally not
        Long bgpPeerId3 = 13L; // to be added, but finally not
        Long bgpPeerNetworkMapId3 = 15L; // to be added, but finally not

        when(vpc.getId()).thenReturn(vpcId);

        BgpPeerNetworkMapVO bgpPeerNetworkMap1 = Mockito.mock(BgpPeerNetworkMapVO.class);
        when(bgpPeerNetworkMap1.getBgpPeerId()).thenReturn(bgpPeerId1);
        when(bgpPeerNetworkMap1.getState()).thenReturn(BgpPeer.State.Active);
        BgpPeerNetworkMapVO bgpPeerNetworkMap2 = Mockito.mock(BgpPeerNetworkMapVO.class);
        when(bgpPeerNetworkMap2.getBgpPeerId()).thenReturn(bgpPeerId2);
        when(bgpPeerNetworkMap2.getState()).thenReturn(BgpPeer.State.Revoke);
        BgpPeerNetworkMapVO bgpPeerNetworkMap3 = Mockito.mock(BgpPeerNetworkMapVO.class);
        when(bgpPeerNetworkMap3.getState()).thenReturn(BgpPeer.State.Add);

        when(bgpPeerNetworkMap3.getId()).thenReturn(bgpPeerNetworkMapId3);

        when(bgpPeerNetworkMapDao.listByVpcId(vpcId)).thenReturn(Arrays.asList(bgpPeerNetworkMap1, bgpPeerNetworkMap2))
                .thenReturn(Arrays.asList(bgpPeerNetworkMap1, bgpPeerNetworkMap2, bgpPeerNetworkMap3));
        when(bgpService.applyBgpPeers(vpc, false)).thenReturn(false);

        Vpc result = routedIpv4Manager.changeBgpPeersForVpcInternal(vpc, Arrays.asList(bgpPeerId1, bgpPeerId3));
        Assert.assertNull(result);

        verify(bgpPeerNetworkMap2).setState(BgpPeer.State.Revoke);
        verify(bgpPeerNetworkMapDao).persist(any());
        verify(bgpPeerNetworkMap2).setState(BgpPeer.State.Add);
        verify(bgpPeerNetworkMapDao).remove(bgpPeerNetworkMapId3);
    }

    @Test
    public void testRemoveIpv4SubnetsForZoneByAccountId() {
        when(dataCenterIpv4GuestSubnetDao.listByAccountId(accountId)).thenReturn(Arrays.asList(subnetVO));
        when(subnetVO.getId()).thenReturn(zoneSubnetId);

        routedIpv4Manager.removeIpv4SubnetsForZoneByAccountId(accountId);

        verify(ipv4GuestSubnetNetworkMapDao).deleteByParentId(zoneSubnetId);
        verify(dataCenterIpv4GuestSubnetDao).remove(zoneSubnetId);
    }

    @Test
    public void testRemoveIpv4SubnetsForZoneByDomainId() {
        when(dataCenterIpv4GuestSubnetDao.listByDomainId(domainId)).thenReturn(Arrays.asList(subnetVO));
        when(subnetVO.getId()).thenReturn(zoneSubnetId);

        routedIpv4Manager.removeIpv4SubnetsForZoneByDomainId(domainId);

        verify(ipv4GuestSubnetNetworkMapDao).deleteByParentId(zoneSubnetId);
        verify(dataCenterIpv4GuestSubnetDao).remove(zoneSubnetId);
    }
}
