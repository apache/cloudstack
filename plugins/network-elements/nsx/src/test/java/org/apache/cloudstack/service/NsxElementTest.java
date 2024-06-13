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
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.network.Network;
import com.cloud.network.NetworkModel;
import com.cloud.network.Networks;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.LoadBalancerVMMapDao;
import com.cloud.network.dao.LoadBalancerVO;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.dao.PhysicalNetworkVO;
import com.cloud.network.lb.LoadBalancingRule;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.FirewallRuleVO;
import com.cloud.network.rules.PortForwardingRule;
import com.cloud.network.rules.PortForwardingRuleVO;
import com.cloud.network.rules.StaticNatImpl;
import com.cloud.network.vpc.NetworkACLItem;
import com.cloud.network.vpc.NetworkACLItemVO;
import com.cloud.network.vpc.Vpc;
import com.cloud.network.vpc.VpcVO;
import com.cloud.network.vpc.dao.VpcDao;
import com.cloud.network.vpc.dao.VpcOfferingServiceMapDao;
import com.cloud.resource.ResourceManager;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.utils.Pair;
import com.cloud.utils.net.Ip;
import com.cloud.vm.NicVO;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.VMInstanceDao;
import org.apache.cloudstack.acl.ControlledEntity;
import org.apache.cloudstack.resource.NsxNetworkRule;
import org.apache.cloudstack.resourcedetail.dao.FirewallRuleDetailsDao;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
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
    FirewallRuleDetailsDao firewallRuleDetailsDao;

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
        nsxElement.firewallRuleDetailsDao = firewallRuleDetailsDao;

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

    private Method getPublicPortRangeMethod() throws NoSuchMethodException {
        Method method = NsxElement.class.getDeclaredMethod("getPublicPortRange", PortForwardingRule.class);
        method.setAccessible(true);
        return method;
    }

    private Method getPrivatePFPortRangeMethod() throws NoSuchMethodException {
        Method method = NsxElement.class.getDeclaredMethod("getPrivatePFPortRange", PortForwardingRule.class);
        method.setAccessible(true);
        return method;
    }

    private Method getPrivatePortRangeMethod() throws NoSuchMethodException {
        Method method = NsxElement.class.getDeclaredMethod("getPrivatePortRange", FirewallRule.class);
        method.setAccessible(true);
        return method;
    }

    private Method getPrivatePortRangeForACLRuleMethod() throws NoSuchMethodException {
        Method method = NsxElement.class.getDeclaredMethod("getPrivatePortRangeForACLRule", NetworkACLItem.class);
        method.setAccessible(true);
        return method;
    }

    @Test
    public void testGetPublicPortRangeWhenStartAndEndPortNumbersAreDifferent() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        PortForwardingRule rule = new PortForwardingRuleVO("1", 11L, 80, 90, new Ip("172.30.10.11"), 8080, 8090, "tcp", 12L,
                5L, 2L, 15L);
        assertEquals("80-90", getPublicPortRangeMethod().invoke(null, rule));
    }

    @Test
    public void testGetPublicPortRangeWhenStartAndEndPortNumbersAreSame() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        PortForwardingRule rule = new PortForwardingRuleVO("1", 11L, 80, 80, new Ip("172.30.10.11"), 8080, 8080, "tcp", 12L,
                5L, 2L, 15L);
        assertEquals("80", getPublicPortRangeMethod().invoke(null, rule));
    }

    @Test
    public void testGetPrivatePFPortRangeWhenStartAndEndPortNumbersAreDifferent() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        PortForwardingRule rule = new PortForwardingRuleVO("1", 11L, 80, 90, new Ip("172.30.10.11"), 8080, 8090, "tcp", 12L,
                5L, 2L, 15L);
        assertEquals("8080-8090", getPrivatePFPortRangeMethod().invoke(null, rule));
    }

    @Test
    public void testGetPrivatePFPortRangeWhenStartAndEndPortNumbersAreSame() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        PortForwardingRule rule = new PortForwardingRuleVO("1", 11L, 80, 80, new Ip("172.30.10.11"), 8080, 8080, "tcp", 12L,
                5L, 2L, 15L);
        assertEquals("8080", getPrivatePFPortRangeMethod().invoke(null, rule));
    }

    @Test
    public void testGetPrivatePortRangeWhenStartAndEndPortNumbersAreSame() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        FirewallRuleVO rule = new FirewallRuleVO("1", 11L, 80, 80, "tcp", 23L, 5L, 2L,
        FirewallRule.Purpose.Firewall, List.of("172.30.10.0/24"), null, null, null, null, FirewallRule.TrafficType.Egress, FirewallRule.FirewallRuleType.User);
        assertEquals("80", getPrivatePortRangeMethod().invoke(null, rule));
    }

    @Test
    public void testGetPrivatePortRangeWhenStartAndEndPortNumbersAreDifferent() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        FirewallRuleVO rule = new FirewallRuleVO("1", 11L, 80, 90, "tcp", 23L, 5L, 2L,
                FirewallRule.Purpose.Firewall, List.of("172.30.10.0/24"), null, null, null, null, FirewallRule.TrafficType.Egress, FirewallRule.FirewallRuleType.User);
        assertEquals("80-90", getPrivatePortRangeMethod().invoke(null, rule));
    }

    @Test
    public void testGetPrivatePortRangeForACLWhenStartAndEndPortNumbersAreSame() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        NetworkACLItem rule = new NetworkACLItemVO(80, 80, "udp", 10L, List.of("172.30.10.0/24"), null, null, NetworkACLItem.TrafficType.Ingress, NetworkACLItem.Action.Allow,
        2, null);
        assertEquals("80", getPrivatePortRangeForACLRuleMethod().invoke(null, rule));
    }

    @Test
    public void testGetPrivatePortRangeForACLWhenStartAndEndPortNumbersAreDifferent() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        NetworkACLItem rule = new NetworkACLItemVO(80, 90, "udp", 10L, List.of("172.30.10.0/24"), null, null, NetworkACLItem.TrafficType.Ingress, NetworkACLItem.Action.Allow,
                2, null);
        assertEquals("80-90", getPrivatePortRangeForACLRuleMethod().invoke(null, rule));
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
}
