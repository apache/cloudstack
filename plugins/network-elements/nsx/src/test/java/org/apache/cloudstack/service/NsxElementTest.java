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
package org.apache.cloudstack.service;

import com.cloud.api.ApiDBUtils;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.network.IpAddress;
import com.cloud.network.IpAddressManager;
import com.cloud.network.Network;
import com.cloud.network.NetworkModel;
import com.cloud.network.Networks;
import com.cloud.network.Site2SiteVpnConnection;
import com.cloud.network.Site2SiteVpnGateway;
import com.cloud.network.dao.FirewallRulesDao;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.LoadBalancerVMMapDao;
import com.cloud.network.dao.LoadBalancerDao;
import com.cloud.network.dao.LoadBalancerVO;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.dao.PhysicalNetworkVO;
import com.cloud.network.dao.Site2SiteCustomerGatewayDao;
import com.cloud.network.dao.Site2SiteCustomerGatewayVO;
import com.cloud.network.dao.Site2SiteVpnGatewayDao;
import com.cloud.network.dao.Site2SiteVpnGatewayVO;
import com.cloud.network.element.PortForwardingServiceProvider;
import com.cloud.network.lb.LoadBalancingRule;
import com.cloud.network.nsx.NsxVpnGatewayResult;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.FirewallRuleVO;
import com.cloud.network.rules.PortForwardingRule;
import com.cloud.network.rules.PortForwardingRuleVO;
import com.cloud.network.rules.StaticNatImpl;
import com.cloud.network.rules.dao.PortForwardingRulesDao;
import com.cloud.network.vpc.NetworkACLItem;
import com.cloud.network.vpc.NetworkACLItemVO;
import com.cloud.network.vpc.Vpc;
import com.cloud.network.vpc.VpcManager;
import com.cloud.network.vpc.VpcService;
import com.cloud.network.vpc.VpcVO;
import com.cloud.network.vpc.dao.VpcDao;
import com.cloud.network.vpc.dao.VpcOfferingServiceMapDao;
import com.cloud.resource.ResourceManager;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.User;
import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.Ip;
import com.cloud.vm.NicVO;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.VMInstanceDao;
import org.apache.cloudstack.acl.ControlledEntity;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.resource.NsxNetworkRule;
import org.apache.cloudstack.resourcedetail.UserIpAddressDetailVO;
import org.apache.cloudstack.resourcedetail.dao.FirewallRuleDetailsDao;
import org.apache.cloudstack.resourcedetail.dao.UserIpAddressDetailsDao;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class NsxElementTest {

    @Mock
    DataCenterDao dataCenterDao;
    @Mock
    NsxServiceImpl nsxService;
    @Mock
    AccountManager accountManager;
    @Mock
    NetworkDao networkDao;
    @Mock
    ResourceManager resourceManager;
    @Mock
    PhysicalNetworkDao physicalNetworkDao;
    @Mock
    NetworkModel networkModel;
    @Mock
    Vpc vpc;
    @Mock
    DataCenterVO zone;
    @Mock
    DataCenterVO dataCenterVO;
    @Mock
    Account account;
    @Mock
    DomainVO domain;
    @Mock
    IPAddressDao ipAddressDao;
    @Mock
    VMInstanceDao vmInstanceDao;
    @Mock
    VpcDao vpcDao;
    @Mock
    UserVmDao userVmDao;
    @Mock
    private VpcOfferingServiceMapDao vpcOfferingServiceMapDao;
    @Mock
    LoadBalancerVMMapDao lbVmMapDao;
    @Mock
    LoadBalancerDao loadBalancerDao;
    @Mock
    FirewallRuleDetailsDao firewallRuleDetailsDao;
    @Mock
    IpAddressManager ipAddressManager;
    @Mock
    VpcService vpcService;
    @Mock
    VpcManager vpcManager;
    @Mock
    Site2SiteVpnGatewayDao vpnGatewayDao;
    @Mock
    Site2SiteCustomerGatewayDao customerGatewayDao;
    @Mock
    UserIpAddressDetailsDao userIpAddressDetailsDao;
    @Mock
    FirewallRulesDao firewallRulesDao;
    @Mock
    PortForwardingRulesDao portForwardingRulesDao;

    NsxElement nsxElement;
    ReservationContext reservationContext;
    DeployDestination deployDestination;
    @Mock
    DomainDao domainDao;

    @Before
    public void setup() throws NoSuchFieldException, IllegalAccessException {
        nsxElement = new NsxElement();

        nsxElement.dataCenterDao = dataCenterDao;
        nsxElement.nsxService = nsxService;
        nsxElement.accountMgr = accountManager;
        nsxElement.networkDao = networkDao;
        nsxElement.resourceManager = resourceManager;
        nsxElement.physicalNetworkDao = physicalNetworkDao;
        nsxElement.domainDao = domainDao;
        nsxElement.networkModel = networkModel;
        nsxElement.vpcOfferingServiceMapDao = vpcOfferingServiceMapDao;
        nsxElement.ipAddressDao = ipAddressDao;
        nsxElement.vmInstanceDao = vmInstanceDao;
        nsxElement.vpcDao = vpcDao;
        nsxElement.lbVmMapDao = lbVmMapDao;
        nsxElement.loadBalancerDao = loadBalancerDao;
        nsxElement.firewallRuleDetailsDao = firewallRuleDetailsDao;
        nsxElement.ipAddressManager = ipAddressManager;
        nsxElement.vpcService = vpcService;
        nsxElement.vpcManager = vpcManager;
        nsxElement.vpnGatewayDao = vpnGatewayDao;
        nsxElement.customerGatewayDao = customerGatewayDao;
        nsxElement.userIpAddressDetailsDao = userIpAddressDetailsDao;
        nsxElement.firewallRulesDao = firewallRulesDao;
        nsxElement.portForwardingRulesDao = portForwardingRulesDao;
        Mockito.lenient().when(ipAddressManager.disassociatePublicIpAddress(any(), anyLong(), any())).thenReturn(true);
        Mockito.lenient().when(loadBalancerDao.listByIpAddress(anyLong())).thenReturn(List.of());
        Mockito.lenient().when(firewallRulesDao.listByIpAndNotRevoked(anyLong())).thenReturn(List.of());
        Mockito.lenient().when(portForwardingRulesDao.listByIpAndNotRevoked(anyLong())).thenReturn(List.of());

        Field field = ApiDBUtils.class.getDeclaredField("s_ipAddressDao");
        field.setAccessible(true);
        field.set(null, ipAddressDao);

        field = ApiDBUtils.class.getDeclaredField("s_userVmDao");
        field.setAccessible(true);
        field.set(null, userVmDao);
        reservationContext = mock(ReservationContext.class);
        deployDestination = mock(DeployDestination.class);

        when(vpc.getZoneId()).thenReturn(1L);
        when(vpc.getAccountId()).thenReturn(2L);
        when(dataCenterVO.getId()).thenReturn(1L);
        when(vpc.getName()).thenReturn("VPC01");
        when(accountManager.getAccount(2L)).thenReturn(account);
        when(dataCenterDao.findById(anyLong())).thenReturn(dataCenterVO);
        when(domainDao.findById(anyLong())).thenReturn(domain);
        when(vpc.getZoneId()).thenReturn(1L);
        when(vpc.getName()).thenReturn("testVPC");

        PhysicalNetworkVO physicalNetworkVO = new PhysicalNetworkVO();
        physicalNetworkVO.setIsolationMethods(List.of("NSX"));
        List<PhysicalNetworkVO> physicalNetworkVOList = List.of(physicalNetworkVO);

        when(physicalNetworkDao.listByZoneAndTrafficType(1L, Networks.TrafficType.Guest)).thenReturn(physicalNetworkVOList);
    }

    @Test
    public void testImplementVpc() throws ResourceUnavailableException, InsufficientCapacityException {
        assertTrue(nsxElement.implementVpc(vpc, deployDestination, reservationContext));
    }

    @Test
    public void testShutdownVpc() {
        when(nsxService.deleteVpcNetwork(anyLong(), anyLong(), anyLong(), anyLong(), anyString())).thenReturn(true);

        assertTrue(nsxElement.shutdownVpc(vpc, reservationContext));
    }

    @Test
    public void testTransformActionValue() {
        NsxNetworkRule.NsxRuleAction action = nsxElement.transformActionValue(NetworkACLItem.Action.Deny);
        Assert.assertEquals(NsxNetworkRule.NsxRuleAction.DROP, action);
    }

    @Test
    public void testTransformCidrListValuesEmptyList() {
        List<String> values = nsxElement.transformCidrListValues(null);
        Assert.assertNotNull(values);
        Assert.assertTrue(values.isEmpty());
    }

    @Test
    public void testTransformCidrListValuesList() {
        List<String> values = nsxElement.transformCidrListValues(List.of("0.0.0.0/0"));
        Assert.assertEquals(1, values.size());
        Assert.assertEquals("ANY", values.get(0));
    }

    @Test
    public void testCanHandleService() {
        when(networkModel.isProviderForNetwork(any(Network.Provider.class), anyLong())).thenReturn(true);

        Network.Service service = new Network.Service("service1", new Network.Capability("capability"));
        NetworkVO network = new NetworkVO();
        network.setName("network1");
        assertTrue(nsxElement.canHandle(network, service));
    }

    @Test
    public void testApplyStaticNatRules() throws ResourceUnavailableException {
        StaticNatImpl rule = new StaticNatImpl(1L , 1L, 3L, 7L, "172.30.10.15", false);
        NetworkVO networkVO = new NetworkVO(1L, Networks.TrafficType.Public, Networks.Mode.Static,
                Networks.BroadcastDomainType.NSX, 12L, 2L, 5L, 1L, "network1",
                "network1", null, Network.GuestType.Isolated, 2L, 2L,
                ControlledEntity.ACLType.Domain, false, 1L, false );

        Ip ip = new Ip("10.1.13.15");
        IPAddressVO ipAddress = new IPAddressVO(ip, 2L, 0xaabbccddeeffL, 3L, false);
        ipAddress.setAssociatedWithVmId(10L);

        VMInstanceVO vm = new VMInstanceVO(10L, 9L, "vm1", "i-5-10-VM" , VirtualMachine.Type.User,
                18L, Hypervisor.HypervisorType.VMware, 26L,
        2L, 5L, 6L, false, false);

        NicVO nic = Mockito.mock(NicVO.class);
        VpcVO vpc = Mockito.mock(VpcVO.class);

        when(ipAddressDao.findByIdIncludingRemoved(anyLong())).thenReturn(ipAddress);
        when(vmInstanceDao.findByIdIncludingRemoved(anyLong())).thenReturn(vm);
        when(networkModel.getNicInNetworkIncludingRemoved(anyLong(), anyLong())).thenReturn(nic);
        when(vpcDao.findById(anyLong())).thenReturn(vpc);
        when(vpc.getId()).thenReturn(1L);
        when(vpc.getName()).thenReturn("vpc1");
        when(nsxService.createStaticNatRule(anyLong(), anyLong(), anyLong(), anyLong(), anyString(), anyBoolean(), anyLong(), anyString(), anyString())).thenReturn(true);

        assertTrue(nsxElement.applyStaticNats(networkVO, List.of(rule)));
    }

    @Test
    public void testApplyPFRules_add() throws ResourceUnavailableException {
        NetworkVO networkVO = new NetworkVO(1L, Networks.TrafficType.Public, Networks.Mode.Static,
                Networks.BroadcastDomainType.NSX, 12L, 2L, 5L, 1L, "network1",
                "network1", null, Network.GuestType.Isolated, 2L, 2L,
                ControlledEntity.ACLType.Domain, false, 1L, false );
        PortForwardingRuleVO rule = new PortForwardingRuleVO("1", 11L, 80, 90, new Ip("172.30.10.11"), 8080, 8090, "tcp", 12L,
        5L, 2L, 15L);
        rule.setState(FirewallRule.State.Add);
        Network.Service service = new Network.Service("service1", new Network.Capability("capability"));

        when(nsxElement.canHandle(networkVO, service)).thenReturn(true);
        assertTrue(nsxElement.applyPFRules(networkVO, List.of(rule)));
    }

    @Test
    public void testApplyPFRules_delete() throws ResourceUnavailableException {
        NetworkVO networkVO = new NetworkVO(1L, Networks.TrafficType.Public, Networks.Mode.Static,
                Networks.BroadcastDomainType.NSX, 12L, 2L, 5L, 1L, "network1",
                "network1", null, Network.GuestType.Isolated, 2L, 2L,
                ControlledEntity.ACLType.Domain, false, 1L, false );
        PortForwardingRuleVO rule = new PortForwardingRuleVO("1", 11L, 80, 90, new Ip("172.30.10.11"), 8080, 8090, "tcp", 12L,
                5L, 2L, 15L);
        rule.setState(FirewallRule.State.Revoke);
        Network.Service service = new Network.Service("service1", new Network.Capability("capability"));
        VpcVO vpcVO = Mockito.mock(VpcVO.class);
        when(vpcDao.findById(1L)).thenReturn(vpcVO);
        when(vpcVO.getDomainId()).thenReturn(2L);
        IPAddressVO ipAddress = new IPAddressVO(new Ip("10.1.13.10"), 1L, 1L, 1L,false);
        when(ApiDBUtils.findIpAddressById(anyLong())).thenReturn(ipAddress);
        when(nsxElement.canHandle(networkVO, service)).thenReturn(true);
        when(nsxService.deletePortForwardRule(any(NsxNetworkRule.class))).thenReturn(true);
        assertTrue(nsxElement.applyPFRules(networkVO, List.of(rule)));
    }

    @Test
    public void testGetVpcOrNetworkReturnsVpcIfVpcIdPresent() {
        VpcVO vpc = new VpcVO();
        when(vpcDao.findById(anyLong())).thenReturn(vpc);

        Pair<VpcVO, NetworkVO> vpcNetworkPair = nsxElement.getVpcOrNetwork(1L, 1L);
        assertNotNull(vpcNetworkPair.first());
        assertNull(vpcNetworkPair.second());
    }

    @Test
    public void testGetVpcOrNetworkReturnsNetworkIfVpcIdNotPresent() {
        NetworkVO network = new NetworkVO();
        when(networkDao.findById(anyLong())).thenReturn(network);

        Pair<VpcVO, NetworkVO> vpcNetworkPair = nsxElement.getVpcOrNetwork(null, 1L);
        assertNull(vpcNetworkPair.first());
        assertNotNull(vpcNetworkPair.second());
    }

    @Test
    public void testGetPublicPortRangeWhenStartAndEndPortNumbersAreDifferent() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        PortForwardingRule rule = new PortForwardingRuleVO("1", 11L, 80, 90, new Ip("172.30.10.11"), 8080, 8090, "tcp", 12L,
                5L, 2L, 15L);
        assertEquals("80-90", PortForwardingServiceProvider.getPublicPortRange(rule));
    }

    @Test
    public void testGetPublicPortRangeWhenStartAndEndPortNumbersAreSame() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        PortForwardingRule rule = new PortForwardingRuleVO("1", 11L, 80, 80, new Ip("172.30.10.11"), 8080, 8080, "tcp", 12L,
                5L, 2L, 15L);
        assertEquals("80", PortForwardingServiceProvider.getPublicPortRange(rule));
    }

    @Test
    public void testGetPrivatePFPortRangeWhenStartAndEndPortNumbersAreDifferent() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        PortForwardingRule rule = new PortForwardingRuleVO("1", 11L, 80, 90, new Ip("172.30.10.11"), 8080, 8090, "tcp", 12L,
                5L, 2L, 15L);
        assertEquals("8080-8090", PortForwardingServiceProvider.getPrivatePFPortRange(rule));
    }

    @Test
    public void testGetPrivatePFPortRangeWhenStartAndEndPortNumbersAreSame() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        PortForwardingRule rule = new PortForwardingRuleVO("1", 11L, 80, 80, new Ip("172.30.10.11"), 8080, 8080, "tcp", 12L,
                5L, 2L, 15L);
        assertEquals("8080", PortForwardingServiceProvider.getPrivatePFPortRange(rule));
    }

    @Test
    public void testGetPrivatePortRangeWhenStartAndEndPortNumbersAreSame() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        FirewallRuleVO rule = new FirewallRuleVO("1", 11L, 80, 80, "tcp", 23L, 5L, 2L,
        FirewallRule.Purpose.Firewall, List.of("172.30.10.0/24"), null, null, null, null, FirewallRule.TrafficType.Egress, FirewallRule.FirewallRuleType.User);
        assertEquals("80", PortForwardingServiceProvider.getPrivatePortRange(rule));
    }

    @Test
    public void testGetPrivatePortRangeWhenStartAndEndPortNumbersAreDifferent() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        FirewallRuleVO rule = new FirewallRuleVO("1", 11L, 80, 90, "tcp", 23L, 5L, 2L,
                FirewallRule.Purpose.Firewall, List.of("172.30.10.0/24"), null, null, null, null, FirewallRule.TrafficType.Egress, FirewallRule.FirewallRuleType.User);
        assertEquals("80-90", PortForwardingServiceProvider.getPrivatePortRange(rule));
    }

    @Test
    public void testGetPrivatePortRangeForACLWhenStartAndEndPortNumbersAreSame() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        NetworkACLItem rule = new NetworkACLItemVO(80, 80, "udp", 10L, List.of("172.30.10.0/24"), null, null, NetworkACLItem.TrafficType.Ingress, NetworkACLItem.Action.Allow,
        2, null);
        assertEquals("80", PortForwardingServiceProvider.getPrivatePortRangeForACLRule(rule));
    }

    @Test
    public void testGetPrivatePortRangeForACLWhenStartAndEndPortNumbersAreDifferent() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        NetworkACLItem rule = new NetworkACLItemVO(80, 90, "udp", 10L, List.of("172.30.10.0/24"), null, null, NetworkACLItem.TrafficType.Ingress, NetworkACLItem.Action.Allow,
                2, null);
        assertEquals("80-90", PortForwardingServiceProvider.getPrivatePortRangeForACLRule(rule));
    }

    @Test
    public void testApplyLBRules_add() throws ResourceUnavailableException {
        NetworkVO networkVO = new NetworkVO(1L, Networks.TrafficType.Public, Networks.Mode.Static,
                Networks.BroadcastDomainType.NSX, 12L, 2L, 5L, 1L, "network1",
                "network1", null, Network.GuestType.Isolated, 2L, 2L,
                ControlledEntity.ACLType.Domain, false, 1L, false );
        LoadBalancerVO lb = new LoadBalancerVO(null, null, null, 0L, 8080, 8081, null, 0L, 0L, 1L, null, null);
        lb.setState(FirewallRule.State.Add);
        LoadBalancingRule.LbDestination destination = new LoadBalancingRule.LbDestination(6443, 6443, "172.30.110.11", false);
        LoadBalancingRule rule = new LoadBalancingRule(lb, List.of(destination), null, null, new Ip("10.1.13.10"), null, "TCP");

        VpcVO vpc = Mockito.mock(VpcVO.class);

        IPAddressVO ipAddress = new IPAddressVO(new Ip("10.1.13.10"), 1L, 1L, 1L,false);
        when(vpcDao.findById(anyLong())).thenReturn(vpc);
        when(vpc.getDomainId()).thenReturn(2L);
        when(vpc.getAccountId()).thenReturn(5L);
        when(ipAddressDao.findByIpAndDcId(anyLong(), anyString())).thenReturn(ipAddress);
        when(nsxService.createLbRule(any(NsxNetworkRule.class))).thenReturn(true);

        assertTrue(nsxElement.applyLBRules(networkVO, List.of(rule)));
    }

    @Test
    public void testApplyLBRules_delete() throws ResourceUnavailableException {
        NetworkVO networkVO = new NetworkVO(1L, Networks.TrafficType.Public, Networks.Mode.Static,
                Networks.BroadcastDomainType.NSX, 12L, 2L, 5L, 1L, "network1",
                "network1", null, Network.GuestType.Isolated, 2L, 2L,
                ControlledEntity.ACLType.Domain, false, 1L, false );
        LoadBalancerVO lb = new LoadBalancerVO(null, null, null, 0L, 8080, 8081, null, 0L, 0L, 1L, null, null);
        lb.setState(FirewallRule.State.Revoke);
        LoadBalancingRule.LbDestination destination = new LoadBalancingRule.LbDestination(6443, 6443, "172.30.110.11", false);
        LoadBalancingRule rule = new LoadBalancingRule(lb, List.of(destination), null, null, new Ip("10.1.13.10"), null, "TCP");

        VpcVO vpc = Mockito.mock(VpcVO.class);

        IPAddressVO ipAddress = new IPAddressVO(new Ip("10.1.13.10"), 1L, 1L, 1L,false);
        when(vpcDao.findById(anyLong())).thenReturn(vpc);
        when(vpc.getDomainId()).thenReturn(2L);
        when(vpc.getAccountId()).thenReturn(5L);
        when(ipAddressDao.findByIpAndDcId(anyLong(), anyString())).thenReturn(ipAddress);
        when(nsxService.deleteLbRule(any(NsxNetworkRule.class))).thenReturn(true);

        assertTrue(nsxElement.applyLBRules(networkVO, List.of(rule)));
    }

    @Test
    public void testApplyNetworkAclRules() throws ResourceUnavailableException {
        NetworkVO networkVO = new NetworkVO(1L, Networks.TrafficType.Public, Networks.Mode.Static,
                Networks.BroadcastDomainType.NSX, 12L, 2L, 5L, 1L, "network1",
                "network1", null, Network.GuestType.Isolated, 2L, 2L,
                ControlledEntity.ACLType.Domain, false, 1L, false );
        NetworkACLItem rule = new NetworkACLItemVO(80, 80, "udp", 10L, List.of("172.30.10.0/24"), null, null, NetworkACLItem.TrafficType.Ingress, NetworkACLItem.Action.Allow,
                2, null);
        Network.Service service = new Network.Service("service1", new Network.Capability("capability"));

        when(nsxElement.canHandle(networkVO, service)).thenReturn(true);
        assertTrue(nsxElement.applyNetworkACLs(networkVO, List.of(rule)));
    }

    @Test
    public void testDeleteNetworkAclRules() throws ResourceUnavailableException {
        NetworkVO networkVO = new NetworkVO(1L, Networks.TrafficType.Public, Networks.Mode.Static,
                Networks.BroadcastDomainType.NSX, 12L, 2L, 5L, 1L, "network1",
                "network1", null, Network.GuestType.Isolated, 2L, 2L,
                ControlledEntity.ACLType.Domain, false, 1L, false );
        NetworkACLItemVO rule = new NetworkACLItemVO(80, 80, "udp", 10L, List.of("172.30.10.0/24"), null, null, NetworkACLItem.TrafficType.Ingress, NetworkACLItem.Action.Allow,
                2, null);
        rule.setState(NetworkACLItem.State.Revoke);
        Network.Service service = new Network.Service("service1", new Network.Capability("capability"));

        when(nsxElement.canHandle(networkVO, service)).thenReturn(true);
        when(nsxService.deleteFirewallRules(any(Network.class), any(List.class))).thenReturn(true);
        assertTrue(nsxElement.applyNetworkACLs(networkVO, List.of(rule)));
    }

    @Test
    public void testApplyFirewallRules() throws ResourceUnavailableException {
        NetworkVO networkVO = new NetworkVO(1L, Networks.TrafficType.Public, Networks.Mode.Static,
                Networks.BroadcastDomainType.NSX, 12L, 2L, 5L, 1L, "network1",
                "network1", null, Network.GuestType.Isolated, 2L, 2L,
                ControlledEntity.ACLType.Domain, false, 1L, false );
        FirewallRuleVO rule = new FirewallRuleVO("1", 11L, 80, 80, "tcp", 23L, 5L, 2L,
                FirewallRule.Purpose.Firewall, List.of("172.30.10.0/24"), null, null, null, null, FirewallRule.TrafficType.Egress, FirewallRule.FirewallRuleType.User);
        Network.Service service = new Network.Service("service1", new Network.Capability("capability"));

        when(nsxElement.canHandle(networkVO, service)).thenReturn(true);
        when(nsxService.addFirewallRules(any(Network.class), any(List.class))).thenReturn(true);
        assertTrue(nsxElement.applyFWRules(networkVO, List.of(rule)));
    }

    @Test
    public void testRevokeFirewallRules() throws ResourceUnavailableException {
        NetworkVO networkVO = new NetworkVO(1L, Networks.TrafficType.Public, Networks.Mode.Static,
                Networks.BroadcastDomainType.NSX, 12L, 2L, 5L, 1L, "network1",
                "network1", null, Network.GuestType.Isolated, 2L, 2L,
                ControlledEntity.ACLType.Domain, false, 1L, false );
        FirewallRuleVO rule = new FirewallRuleVO("1", 11L, 80, 80, "tcp", 23L, 5L, 2L,
                FirewallRule.Purpose.Firewall, List.of("172.30.10.0/24"), null, null, null, null, FirewallRule.TrafficType.Egress, FirewallRule.FirewallRuleType.User);
        rule.setState(FirewallRule.State.Revoke);
        Network.Service service = new Network.Service("service1", new Network.Capability("capability"));

        when(nsxElement.canHandle(networkVO, service)).thenReturn(true);
        when(nsxService.deleteFirewallRules(any(Network.class), any(List.class))).thenReturn(true);
        when(nsxService.addFirewallRules(any(Network.class), any(List.class))).thenReturn(true);
        assertTrue(nsxElement.applyFWRules(networkVO, List.of(rule)));
    }

    private VpcVO mockVpcWithNsxVpnSupport() {
        VpcVO vpcVO = Mockito.mock(VpcVO.class);
        Mockito.lenient().when(vpcVO.getId()).thenReturn(9L);
        when(vpcManager.isProviderSupportServiceInVpc(9L, Network.Service.Vpn, Network.Provider.Nsx))
                .thenReturn(true);
        return vpcVO;
    }

    private IPAddressVO mockIpAddressVO(long id, String address) {
        IPAddressVO ipAddressVO = Mockito.mock(IPAddressVO.class);
        when(ipAddressDao.findById(id)).thenReturn(ipAddressVO);
        Mockito.lenient().when(ipAddressVO.getAddress()).thenReturn(new Ip(address));
        Mockito.lenient().when(ipAddressVO.readyToUse()).thenReturn(true);
        Mockito.lenient().when(ipAddressVO.getRemoved()).thenReturn(null);
        return ipAddressVO;
    }

    @Test
    public void testAcquireVpnGatewayIpWithRequestedIp() {
        VpcVO vpcVO = mockVpcWithNsxVpnSupport();
        IpAddress requestedIp = Mockito.mock(IpAddress.class);
        when(requestedIp.getId()).thenReturn(20L);
        IPAddressVO ipAddressVO = mockIpAddressVO(20L, "10.1.13.20");
        when(ipAddressVO.getId()).thenReturn(20L);
        when(ipAddressVO.getVpcId()).thenReturn(9L);
        when(loadBalancerDao.listByIpAddress(20L)).thenReturn(List.of());
        when(nsxService.createVpnGateway(vpcVO, "10.1.13.20", false)).thenReturn(new NsxVpnGatewayResult(true, true));

        IpAddress result = nsxElement.acquireVpnGatewayIp(vpcVO, requestedIp);
        assertEquals(ipAddressVO, result);
        verify(userIpAddressDetailsDao).addDetail(20L, "nsxVpnGatewayIp", "false", false);
    }

    @Test
    public void testAcquireVpnGatewayIpDoesNotCallNsxWhenRequestedIpOwnershipCannotBeRecorded() {
        VpcVO vpcVO = mockVpcWithNsxVpnSupport();
        IpAddress requestedIp = Mockito.mock(IpAddress.class);
        when(requestedIp.getId()).thenReturn(20L);
        IPAddressVO ipAddressVO = mockIpAddressVO(20L, "10.1.13.20");
        when(ipAddressVO.getId()).thenReturn(20L);
        when(ipAddressVO.getVpcId()).thenReturn(9L);
        when(loadBalancerDao.listByIpAddress(20L)).thenReturn(List.of());
        Mockito.doThrow(new CloudRuntimeException("marker write failed")).when(userIpAddressDetailsDao)
                .addDetail(20L, "nsxVpnGatewayIp", "false", false);

        Assert.assertThrows(CloudRuntimeException.class,
                () -> nsxElement.acquireVpnGatewayIp(vpcVO, requestedIp));

        verify(nsxService, never()).createVpnGateway(any(Vpc.class), anyString(), anyBoolean());
    }

    @Test
    public void testAcquireVpnGatewayIpRemovesRequestedIpOwnershipWhenNsxRejectsGateway() {
        VpcVO vpcVO = mockVpcWithNsxVpnSupport();
        IpAddress requestedIp = Mockito.mock(IpAddress.class);
        when(requestedIp.getId()).thenReturn(20L);
        IPAddressVO ipAddressVO = mockIpAddressVO(20L, "10.1.13.20");
        when(ipAddressVO.getId()).thenReturn(20L);
        when(ipAddressVO.getVpcId()).thenReturn(9L);
        when(loadBalancerDao.listByIpAddress(20L)).thenReturn(List.of());
        when(nsxService.createVpnGateway(vpcVO, "10.1.13.20", false)).thenReturn(new NsxVpnGatewayResult(false, false));

        Assert.assertThrows(CloudRuntimeException.class,
                () -> nsxElement.acquireVpnGatewayIp(vpcVO, requestedIp));

        verify(userIpAddressDetailsDao).removeDetail(20L, "nsxVpnGatewayIp");
    }

    @Test
    public void testAcquireVpnGatewayIpRetainsRequestedIpOwnershipWhenNsxResultIsAmbiguous() {
        VpcVO vpcVO = mockVpcWithNsxVpnSupport();
        IpAddress requestedIp = Mockito.mock(IpAddress.class);
        when(requestedIp.getId()).thenReturn(20L);
        IPAddressVO ipAddressVO = mockIpAddressVO(20L, "10.1.13.20");
        when(ipAddressVO.getId()).thenReturn(20L);
        when(ipAddressVO.getVpcId()).thenReturn(9L);
        when(loadBalancerDao.listByIpAddress(20L)).thenReturn(List.of());
        when(nsxService.createVpnGateway(vpcVO, "10.1.13.20", false)).thenReturn(new NsxVpnGatewayResult(false, true));

        Assert.assertThrows(CloudRuntimeException.class,
                () -> nsxElement.acquireVpnGatewayIp(vpcVO, requestedIp));

        verify(userIpAddressDetailsDao, never()).removeDetail(20L, "nsxVpnGatewayIp");
    }

    @Test
    public void testAcquireVpnGatewayIpPreservesExistingRequestedIpOwnershipOnUnambiguousFailure() {
        VpcVO vpcVO = mockVpcWithNsxVpnSupport();
        IpAddress requestedIp = Mockito.mock(IpAddress.class);
        when(requestedIp.getId()).thenReturn(20L);
        IPAddressVO ipAddressVO = mockIpAddressVO(20L, "10.1.13.20");
        when(ipAddressVO.getId()).thenReturn(20L);
        when(ipAddressVO.getVpcId()).thenReturn(9L);
        when(loadBalancerDao.listByIpAddress(20L)).thenReturn(List.of());
        UserIpAddressDetailVO existingMarker = new UserIpAddressDetailVO(
                20L, "nsxVpnGatewayIp", "false", false);
        when(userIpAddressDetailsDao.findDetail(20L, "nsxVpnGatewayIp")).thenReturn(existingMarker);
        when(nsxService.createVpnGateway(vpcVO, "10.1.13.20", true))
                .thenReturn(new NsxVpnGatewayResult(false, false));

        Assert.assertThrows(CloudRuntimeException.class,
                () -> nsxElement.acquireVpnGatewayIp(vpcVO, requestedIp));

        verify(userIpAddressDetailsDao, never()).addDetail(anyLong(), anyString(), anyString(), anyBoolean());
        verify(userIpAddressDetailsDao, never()).removeDetail(20L, "nsxVpnGatewayIp");
        verify(nsxService).createVpnGateway(vpcVO, "10.1.13.20", true);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testAcquireVpnGatewayIpRejectsSourceNatIp() {
        VpcVO vpcVO = mockVpcWithNsxVpnSupport();
        IpAddress requestedIp = Mockito.mock(IpAddress.class);
        when(requestedIp.getId()).thenReturn(20L);
        IPAddressVO ipAddressVO = Mockito.mock(IPAddressVO.class);
        when(ipAddressDao.findById(20L)).thenReturn(ipAddressVO);
        when(ipAddressVO.getVpcId()).thenReturn(9L);
        when(ipAddressVO.readyToUse()).thenReturn(true);
        when(ipAddressVO.getRemoved()).thenReturn(null);
        when(ipAddressVO.isSourceNat()).thenReturn(true);
        when(ipAddressVO.getAddress()).thenReturn(new Ip("10.1.13.20"));

        nsxElement.acquireVpnGatewayIp(vpcVO, requestedIp);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testAcquireVpnGatewayIpRejectsIpWithPortForwardingRule() {
        VpcVO vpcVO = mockVpcWithNsxVpnSupport();
        IpAddress requestedIp = Mockito.mock(IpAddress.class);
        when(requestedIp.getId()).thenReturn(20L);
        IPAddressVO ipAddressVO = mockIpAddressVO(20L, "10.1.13.20");
        when(ipAddressVO.getId()).thenReturn(20L);
        when(ipAddressVO.getVpcId()).thenReturn(9L);
        when(portForwardingRulesDao.listByIpAndNotRevoked(20L))
                .thenReturn(List.of(Mockito.mock(PortForwardingRuleVO.class)));

        nsxElement.acquireVpnGatewayIp(vpcVO, requestedIp);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testAcquireVpnGatewayIpRejectsAnUnallocatedIp() {
        VpcVO vpcVO = mockVpcWithNsxVpnSupport();
        IpAddress requestedIp = Mockito.mock(IpAddress.class);
        when(requestedIp.getId()).thenReturn(21L);
        IPAddressVO ipAddressVO = Mockito.mock(IPAddressVO.class);
        when(ipAddressDao.findById(21L)).thenReturn(ipAddressVO);
        when(ipAddressVO.getVpcId()).thenReturn(9L);
        when(ipAddressVO.readyToUse()).thenReturn(false);

        nsxElement.acquireVpnGatewayIp(vpcVO, requestedIp);
    }

    @Test
    public void testAcquireVpnGatewayIpReturnsNullWhenVpnIsNotProvidedByNsx() {
        VpcVO vpcVO = Mockito.mock(VpcVO.class);

        assertNull(nsxElement.acquireVpnGatewayIp(vpcVO, null));
    }

    @Test
    public void testAcquireVpnGatewayIpAutoAcquiresWhenNoIpIsRequested() throws Exception {
        CallContext.register(Mockito.mock(User.class), Mockito.mock(Account.class));
        try {
            VpcVO vpcVO = mockVpcWithNsxVpnSupport();
            when(vpcVO.getAccountId()).thenReturn(2L);
            when(vpcVO.getZoneId()).thenReturn(1L);
            IpAddress allocatedIp = Mockito.mock(IpAddress.class);
            when(allocatedIp.getId()).thenReturn(30L);
            when(ipAddressManager.allocateIp(any(), anyBoolean(), any(), any(), any(), any(), any())).thenReturn(allocatedIp);
            IPAddressVO ipAddressVO = mockIpAddressVO(30L, "10.1.13.30");
            when(nsxService.createVpnGateway(vpcVO, "10.1.13.30", false)).thenReturn(new NsxVpnGatewayResult(true, true));

            IpAddress result = nsxElement.acquireVpnGatewayIp(vpcVO, null);
            assertEquals(ipAddressVO, result);
            verify(vpcService).associateIPToVpc(30L, 9L);
            verify(userIpAddressDetailsDao).addDetail(30L, "nsxVpnGatewayIp", "true", false);
        } finally {
            CallContext.unregister();
        }
    }

    @Test
    public void testAcquireVpnGatewayIpReusesRetainedIpAfterAmbiguousCreate() throws Exception {
        CallContext.register(Mockito.mock(User.class), Mockito.mock(Account.class));
        try {
            VpcVO vpcVO = mockVpcWithNsxVpnSupport();
            IPAddressVO retainedIp = mockIpAddressVO(32L, "10.1.13.32");
            when(retainedIp.getId()).thenReturn(32L);
            when(retainedIp.getVpcId()).thenReturn(9L);
            UserIpAddressDetailVO retainedMarker = new UserIpAddressDetailVO(
                    32L, "nsxVpnGatewayIp", "true", false);
            when(ipAddressDao.listByAssociatedVpc(9L, false)).thenReturn(List.of(retainedIp));
            when(userIpAddressDetailsDao.findDetail(32L, "nsxVpnGatewayIp")).thenReturn(retainedMarker);
            when(nsxService.createVpnGateway(vpcVO, "10.1.13.32", true))
                    .thenReturn(new NsxVpnGatewayResult(true, true));

            IpAddress result = nsxElement.acquireVpnGatewayIp(vpcVO, null);

            assertEquals(retainedIp, result);
            verify(nsxService).createVpnGateway(vpcVO, "10.1.13.32", true);
            verify(ipAddressManager, never()).allocateIp(any(), anyBoolean(), any(), any(), any(), any(), any());
            verify(vpcService, never()).associateIPToVpc(anyLong(), anyLong());
        } finally {
            CallContext.unregister();
        }
    }

    @Test(expected = CloudRuntimeException.class)
    public void testAcquireVpnGatewayIpReleasesAutoAcquiredIpWhenNsxRejectsGateway() throws Exception {
        CallContext.register(Mockito.mock(User.class), Mockito.mock(Account.class));
        try {
            VpcVO vpcVO = mockVpcWithNsxVpnSupport();
            when(vpcVO.getAccountId()).thenReturn(2L);
            when(vpcVO.getZoneId()).thenReturn(1L);
            IpAddress allocatedIp = Mockito.mock(IpAddress.class);
            when(allocatedIp.getId()).thenReturn(31L);
            when(ipAddressManager.allocateIp(any(), anyBoolean(), any(), any(), any(), any(), any())).thenReturn(allocatedIp);
            IPAddressVO ipAddressVO = mockIpAddressVO(31L, "10.1.13.31");
            when(ipAddressVO.getId()).thenReturn(31L);
            when(nsxService.createVpnGateway(vpcVO, "10.1.13.31", false)).thenReturn(new NsxVpnGatewayResult(false, false));
            when(ipAddressManager.disassociatePublicIpAddress(any(IPAddressVO.class), anyLong(), any())).thenReturn(true);

            nsxElement.acquireVpnGatewayIp(vpcVO, null);
        } finally {
            verify(userIpAddressDetailsDao).removeDetail(31L, "nsxVpnGatewayIp");
            verify(ipAddressManager).disassociatePublicIpAddress(any(IPAddressVO.class), anyLong(), any());
            CallContext.unregister();
        }
    }

    @Test(expected = CloudRuntimeException.class)
    public void testAcquireVpnGatewayIpRetainsAutoAcquiredIpWhenEndpointMayBeInUse() throws Exception {
        CallContext.register(Mockito.mock(User.class), Mockito.mock(Account.class));
        try {
            VpcVO vpcVO = mockVpcWithNsxVpnSupport();
            when(vpcVO.getAccountId()).thenReturn(2L);
            when(vpcVO.getZoneId()).thenReturn(1L);
            IpAddress allocatedIp = Mockito.mock(IpAddress.class);
            when(allocatedIp.getId()).thenReturn(32L);
            when(ipAddressManager.allocateIp(any(), anyBoolean(), any(), any(), any(), any(), any())).thenReturn(allocatedIp);
            mockIpAddressVO(32L, "10.1.13.32");
            when(nsxService.createVpnGateway(vpcVO, "10.1.13.32", false)).thenReturn(new NsxVpnGatewayResult(false, true));

            nsxElement.acquireVpnGatewayIp(vpcVO, null);
        } finally {
            verify(userIpAddressDetailsDao, never()).removeDetail(32L, "nsxVpnGatewayIp");
            verify(ipAddressManager, never()).disassociatePublicIpAddress(any(IPAddressVO.class), anyLong(), any());
            CallContext.unregister();
        }
    }

    @Test
    public void testReleaseVpnGatewayIpReleasesAutoAcquiredIp() {
        CallContext.register(Mockito.mock(User.class), Mockito.mock(Account.class));
        try {
            VpcVO vpcVO = mockVpcWithNsxVpnSupport();
            when(vpcDao.findById(9L)).thenReturn(vpcVO);
            Site2SiteVpnGateway vpnGateway = Mockito.mock(Site2SiteVpnGateway.class);
            when(vpnGateway.getVpcId()).thenReturn(9L);
            when(vpnGateway.getAddrId()).thenReturn(30L);
            when(nsxService.deleteVpnGateway(vpcVO)).thenReturn(true);
            IPAddressVO ipAddressVO = mockIpAddressVO(30L, "10.1.13.30");
            when(ipAddressVO.getId()).thenReturn(30L);
            UserIpAddressDetailVO detail = Mockito.mock(UserIpAddressDetailVO.class);
            when(detail.getValue()).thenReturn("true");
            when(userIpAddressDetailsDao.findDetail(30L, "nsxVpnGatewayIp")).thenReturn(detail);

            nsxElement.releaseVpnGatewayIp(vpnGateway);
            verify(userIpAddressDetailsDao).removeDetail(30L, "nsxVpnGatewayIp");
            verify(ipAddressManager).disassociatePublicIpAddress(eq(ipAddressVO), anyLong(), any());
        } finally {
            CallContext.unregister();
        }
    }

    @Test
    public void testReleaseVpnGatewayIpKeepsOperatorSpecifiedIp() {
        VpcVO vpcVO = mockVpcWithNsxVpnSupport();
        when(vpcDao.findById(9L)).thenReturn(vpcVO);
        Site2SiteVpnGateway vpnGateway = Mockito.mock(Site2SiteVpnGateway.class);
        when(vpnGateway.getVpcId()).thenReturn(9L);
        when(vpnGateway.getAddrId()).thenReturn(30L);
        when(nsxService.deleteVpnGateway(vpcVO)).thenReturn(true);
        IPAddressVO ipAddressVO = mockIpAddressVO(30L, "10.1.13.30");
        when(ipAddressVO.getId()).thenReturn(30L);
        when(userIpAddressDetailsDao.findDetail(30L, "nsxVpnGatewayIp")).thenReturn(null);

        nsxElement.releaseVpnGatewayIp(vpnGateway);
        verify(ipAddressManager, Mockito.never()).disassociatePublicIpAddress(any(), anyLong(), any());
    }

    @Test
    public void testReleaseVpnGatewayIpRejectsInvalidOwnershipStateWhenVpcRowIsGone() {
        when(vpcDao.findById(9L)).thenReturn(null);
        Site2SiteVpnGateway vpnGateway = Mockito.mock(Site2SiteVpnGateway.class);
        when(vpnGateway.getVpcId()).thenReturn(9L);
        when(vpnGateway.getAddrId()).thenReturn(30L);
        IPAddressVO ipAddressVO = mockIpAddressVO(30L, "10.1.13.30");
        when(ipAddressVO.getId()).thenReturn(30L);
        when(ipAddressVO.getVpcId()).thenReturn(9L);
        UserIpAddressDetailVO detail = new UserIpAddressDetailVO(30L, "nsxVpnGatewayIp", "invalid", false);
        when(userIpAddressDetailsDao.findDetail(30L, "nsxVpnGatewayIp")).thenReturn(detail);

        CloudRuntimeException exception = Assert.assertThrows(CloudRuntimeException.class,
                () -> nsxElement.releaseVpnGatewayIp(vpnGateway));

        assertTrue(exception.getMessage().contains("VPC 9"));
        verify(nsxService, never()).deleteVpnGateway(any(Vpc.class));
        verify(userIpAddressDetailsDao, never()).removeDetail(30L, "nsxVpnGatewayIp");
        verify(ipAddressManager, never()).disassociatePublicIpAddress(any(), anyLong(), any());
    }

    @Test
    public void testNsxVpnGatewayOwnershipDoesNotDependOnMarkerValue() {
        Site2SiteVpnGateway vpnGateway = Mockito.mock(Site2SiteVpnGateway.class);
        when(vpnGateway.getAddrId()).thenReturn(30L);
        UserIpAddressDetailVO operatorSpecified = new UserIpAddressDetailVO(30L, "nsxVpnGatewayIp", "false", false);
        UserIpAddressDetailVO autoAcquired = new UserIpAddressDetailVO(30L, "nsxVpnGatewayIp", "true", false);
        when(userIpAddressDetailsDao.findDetail(30L, "nsxVpnGatewayIp"))
                .thenReturn(operatorSpecified, autoAcquired);

        assertTrue(nsxElement.ownsVpnGateway(vpnGateway));
        assertTrue(nsxElement.ownsVpnGateway(vpnGateway));
    }

    @Test
    public void testNsxVpnGatewayOwnershipRequiresMarker() {
        Site2SiteVpnGateway vpnGateway = Mockito.mock(Site2SiteVpnGateway.class);
        when(vpnGateway.getAddrId()).thenReturn(30L);
        when(userIpAddressDetailsDao.findDetail(30L, "nsxVpnGatewayIp")).thenReturn(null);

        assertFalse(nsxElement.ownsVpnGateway(vpnGateway));
    }

    @Test
    public void testReleaseVpnGatewayIpUsesPersistedOwnershipWhenOfferingMappingIsGone() {
        VpcVO vpcVO = Mockito.mock(VpcVO.class);
        when(vpcDao.findById(9L)).thenReturn(vpcVO);
        Site2SiteVpnGateway vpnGateway = Mockito.mock(Site2SiteVpnGateway.class);
        when(vpnGateway.getVpcId()).thenReturn(9L);
        when(vpnGateway.getAddrId()).thenReturn(30L);
        when(nsxService.deleteVpnGateway(vpcVO)).thenReturn(true);
        IPAddressVO ipAddressVO = mockIpAddressVO(30L, "10.1.13.30");
        when(ipAddressVO.getId()).thenReturn(30L);
        UserIpAddressDetailVO detail = Mockito.mock(UserIpAddressDetailVO.class);
        when(detail.getValue()).thenReturn("false");
        when(userIpAddressDetailsDao.findDetail(30L, "nsxVpnGatewayIp")).thenReturn(detail);

        nsxElement.releaseVpnGatewayIp(vpnGateway);

        verify(nsxService).deleteVpnGateway(vpcVO);
        verify(userIpAddressDetailsDao).removeDetail(30L, "nsxVpnGatewayIp");
        verify(ipAddressManager, Mockito.never()).disassociatePublicIpAddress(any(), anyLong(), any());
    }

    @Test
    public void testReleaseVpnGatewayIpRemovesOperatorMarkerWhenVpcRowIsGone() {
        when(vpcDao.findById(9L)).thenReturn(null);
        Site2SiteVpnGateway vpnGateway = Mockito.mock(Site2SiteVpnGateway.class);
        when(vpnGateway.getVpcId()).thenReturn(9L);
        when(vpnGateway.getAddrId()).thenReturn(30L);
        IPAddressVO ipAddressVO = mockIpAddressVO(30L, "10.1.13.30");
        when(ipAddressVO.getId()).thenReturn(30L);
        UserIpAddressDetailVO detail = Mockito.mock(UserIpAddressDetailVO.class);
        when(detail.getValue()).thenReturn("false");
        when(userIpAddressDetailsDao.findDetail(30L, "nsxVpnGatewayIp")).thenReturn(detail);

        nsxElement.releaseVpnGatewayIp(vpnGateway);

        verify(nsxService, Mockito.never()).deleteVpnGateway(any(Vpc.class));
        verify(userIpAddressDetailsDao).removeDetail(30L, "nsxVpnGatewayIp");
        verify(ipAddressManager, Mockito.never()).disassociatePublicIpAddress(any(), anyLong(), any());
    }

    @Test
    public void testReleaseVpnGatewayIpPreservesOwnershipWhenNsxDeletionFails() {
        CallContext.register(Mockito.mock(User.class), Mockito.mock(Account.class));
        try {
            VpcVO vpcVO = mockVpcWithNsxVpnSupport();
            when(vpcDao.findById(9L)).thenReturn(vpcVO);
            Site2SiteVpnGateway vpnGateway = Mockito.mock(Site2SiteVpnGateway.class);
            when(vpnGateway.getVpcId()).thenReturn(9L);
            when(nsxService.deleteVpnGateway(vpcVO))
                    .thenThrow(new CloudRuntimeException("NSX Tier-1 lookup failed"));

            CloudRuntimeException exception = Assert.assertThrows(CloudRuntimeException.class,
                    () -> nsxElement.releaseVpnGatewayIp(vpnGateway));

            assertTrue(exception.getMessage().contains("Failed to delete the NSX VPN gateway"));
            verify(ipAddressDao, never()).findById(30L);
            verify(userIpAddressDetailsDao, never()).removeDetail(30L, "nsxVpnGatewayIp");
            verify(ipAddressManager, never()).disassociatePublicIpAddress(any(), anyLong(), any());
        } finally {
            CallContext.unregister();
        }
    }

    @Test(expected = CloudRuntimeException.class)
    public void testReleaseVpnGatewayIpKeepsTheMarkerWhenIpDisassociationFails() {
        CallContext.register(Mockito.mock(User.class), Mockito.mock(Account.class));
        try {
            VpcVO vpcVO = mockVpcWithNsxVpnSupport();
            when(vpcDao.findById(9L)).thenReturn(vpcVO);
            Site2SiteVpnGateway vpnGateway = Mockito.mock(Site2SiteVpnGateway.class);
            when(vpnGateway.getVpcId()).thenReturn(9L);
            when(vpnGateway.getAddrId()).thenReturn(30L);
            when(nsxService.deleteVpnGateway(vpcVO)).thenReturn(true);
            IPAddressVO ipAddressVO = mockIpAddressVO(30L, "10.1.13.30");
            when(ipAddressVO.getId()).thenReturn(30L);
            UserIpAddressDetailVO detail = Mockito.mock(UserIpAddressDetailVO.class);
            when(detail.getValue()).thenReturn("true");
            when(userIpAddressDetailsDao.findDetail(30L, "nsxVpnGatewayIp")).thenReturn(detail);
            when(ipAddressManager.disassociatePublicIpAddress(any(), anyLong(), any())).thenReturn(false);

            nsxElement.releaseVpnGatewayIp(vpnGateway);
        } finally {
            verify(userIpAddressDetailsDao, Mockito.never()).removeDetail(30L, "nsxVpnGatewayIp");
            CallContext.unregister();
        }
    }

    private Site2SiteCustomerGatewayVO mockCustomerGateway(String ikePolicy, String espPolicy) {
        Site2SiteCustomerGatewayVO customerGateway = Mockito.mock(Site2SiteCustomerGatewayVO.class);
        when(customerGatewayDao.findById(3L)).thenReturn(customerGateway);
        when(customerGateway.getIkePolicy()).thenReturn(ikePolicy);
        when(customerGateway.getEspPolicy()).thenReturn(espPolicy);
        when(customerGateway.getIkeVersion()).thenReturn("ikev2");
        when(customerGateway.getIkeLifetime()).thenReturn(86400L);
        when(customerGateway.getEspLifetime()).thenReturn(3600L);
        when(customerGateway.getIpsecPsk()).thenReturn("presharedkey");
        return customerGateway;
    }

    private Site2SiteVpnConnection mockVpnConnection(VpcVO vpcVO) {
        return mockVpnConnection(vpcVO, true);
    }

    private Site2SiteVpnConnection mockVpnConnection(VpcVO vpcVO, boolean nsxOwned) {
        Site2SiteVpnConnection connection = Mockito.mock(Site2SiteVpnConnection.class);
        when(connection.getVpnGatewayId()).thenReturn(7L);
        Mockito.lenient().when(connection.getCustomerGatewayId()).thenReturn(3L);
        Site2SiteVpnGatewayVO vpnGateway = Mockito.mock(Site2SiteVpnGatewayVO.class);
        when(vpnGatewayDao.findById(7L)).thenReturn(vpnGateway);
        when(vpnGateway.getVpcId()).thenReturn(9L);
        when(vpnGateway.getAddrId()).thenReturn(30L);
        if (nsxOwned) {
            when(userIpAddressDetailsDao.findDetail(30L, "nsxVpnGatewayIp"))
                    .thenReturn(Mockito.mock(UserIpAddressDetailVO.class));
        }
        when(vpcDao.findById(9L)).thenReturn(vpcVO);
        return connection;
    }

    @Test
    public void testStartSite2SiteVpnUsesImmutableConnectionId() throws ResourceUnavailableException {
        VpcVO vpcVO = mockVpcWithNsxVpnSupport();
        Site2SiteVpnConnection connection = mockVpnConnection(vpcVO);
        when(connection.getId()).thenReturn(5L);
        Site2SiteVpnGatewayVO vpnGateway = vpnGatewayDao.findById(7L);
        when(vpnGateway.getAddrId()).thenReturn(30L);
        mockIpAddressVO(30L, "10.1.13.30");
        Site2SiteCustomerGatewayVO customerGateway = mockCustomerGateway("aes256-sha256;modp2048", "aes128-sha1");
        when(customerGateway.getGatewayIp()).thenReturn("203.0.113.10");
        when(customerGateway.getGuestCidrList()).thenReturn("192.168.100.0/24,192.168.200.0/24");
        when(nsxService.createVpnConnection(any(Vpc.class), eq(5L), anyString(), anyString(), anyString(), anyString(),
                anyLong(), anyLong(), anyBoolean(), anyString(), anyBoolean(), anyList(),
                eq("169.254.64.21"), eq("169.254.64.22"), anyInt(), eq("10.1.13.30"))).thenReturn(true);

        assertTrue(nsxElement.startSite2SiteVpn(connection));
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testStartSite2SiteVpnRejectsUnsupportedCrypto() throws ResourceUnavailableException {
        VpcVO vpcVO = mockVpcWithNsxVpnSupport();
        Site2SiteVpnConnection connection = mockVpnConnection(vpcVO);
        mockCustomerGateway("3des-md5;modp1024", "3des-md5");

        nsxElement.startSite2SiteVpn(connection);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testStartSite2SiteVpnRejectsDnsPeerAddress() throws ResourceUnavailableException {
        VpcVO vpcVO = mockVpcWithNsxVpnSupport();
        Site2SiteVpnConnection connection = mockVpnConnection(vpcVO);
        Site2SiteCustomerGatewayVO customerGateway = mockCustomerGateway("aes256-sha256;modp2048", "aes128-sha1");
        when(customerGateway.getName()).thenReturn("remote-site");
        when(customerGateway.getGatewayIp()).thenReturn("vpn.example.test");

        nsxElement.startSite2SiteVpn(connection);
    }

    @Test
    public void testSite2SiteVpnOperationsRejectGatewayNotOwnedByNsx() {
        VpcVO vpcVO = Mockito.mock(VpcVO.class);
        Site2SiteVpnConnection connection = mockVpnConnection(vpcVO, false);

        Assert.assertThrows(CloudRuntimeException.class, () -> nsxElement.startSite2SiteVpn(connection));
        Assert.assertThrows(CloudRuntimeException.class, () -> nsxElement.stopSite2SiteVpn(connection));
        Assert.assertThrows(CloudRuntimeException.class, () -> nsxElement.deleteSite2SiteVpn(connection));
        verify(nsxService, Mockito.never()).createVpnConnection(any(Vpc.class), anyLong(), anyString(), anyString(),
                anyString(), anyString(), anyLong(), anyLong(), anyBoolean(), anyString(), anyBoolean(), anyList(),
                anyString(), anyString(), anyInt(), anyString());
        verify(nsxService, Mockito.never()).updateVpnConnectionState(any(Vpc.class), anyLong(), anyBoolean());
        verify(nsxService, Mockito.never()).deleteVpnConnection(any(Vpc.class), anyLong());
    }

    @Test(expected = CloudRuntimeException.class)
    public void testStartSite2SiteVpnThrowsWhenVpcIsMissing() throws ResourceUnavailableException {
        Site2SiteVpnConnection connection = mockVpnConnection(null);

        nsxElement.startSite2SiteVpn(connection);
    }

    @Test
    public void testStopSite2SiteVpn() throws ResourceUnavailableException {
        VpcVO vpcVO = mockVpcWithNsxVpnSupport();
        Site2SiteVpnConnection connection = mockVpnConnection(vpcVO);
        when(connection.getId()).thenReturn(5L);
        when(nsxService.updateVpnConnectionState(vpcVO, 5L, false)).thenReturn(true);

        assertTrue(nsxElement.stopSite2SiteVpn(connection));
    }

    @Test(expected = CloudRuntimeException.class)
    public void testStopSite2SiteVpnThrowsWhenVpcIsMissing() throws ResourceUnavailableException {
        Site2SiteVpnConnection connection = mockVpnConnection(null);

        nsxElement.stopSite2SiteVpn(connection);
    }

    @Test
    public void testDeleteSite2SiteVpnRemovesTheProviderConnection() throws ResourceUnavailableException {
        VpcVO vpcVO = mockVpcWithNsxVpnSupport();
        Site2SiteVpnConnection connection = mockVpnConnection(vpcVO);
        when(connection.getId()).thenReturn(5L);
        when(nsxService.deleteVpnConnection(vpcVO, 5L)).thenReturn(true);

        assertTrue(nsxElement.deleteSite2SiteVpn(connection));
    }

    @Test(expected = CloudRuntimeException.class)
    public void testStopSite2SiteVpnThrowsWhenVpnGatewayIsMissing() throws ResourceUnavailableException {
        Site2SiteVpnConnection connection = Mockito.mock(Site2SiteVpnConnection.class);
        when(connection.getVpnGatewayId()).thenReturn(7L);
        when(vpnGatewayDao.findById(7L)).thenReturn(null);

        nsxElement.stopSite2SiteVpn(connection);
    }
}
