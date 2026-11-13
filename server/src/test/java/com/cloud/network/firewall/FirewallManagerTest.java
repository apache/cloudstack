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

package com.cloud.network.firewall;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.IpAddress;
import com.cloud.network.IpAddressManager;
import com.cloud.network.Network;
import com.cloud.network.Network.Capability;
import com.cloud.network.Network.Service;
import com.cloud.network.NetworkModel;
import com.cloud.network.NetworkModelImpl;
import com.cloud.network.NetworkRuleApplier;
import com.cloud.network.dao.FirewallRulesDao;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.element.FirewallServiceProvider;
import com.cloud.network.element.NetworkElement;
import com.cloud.network.element.VirtualRouterElement;
import com.cloud.network.element.VpcVirtualRouterElement;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.FirewallRule.FirewallRuleType;
import com.cloud.network.rules.FirewallRule.Purpose;
import com.cloud.network.rules.FirewallRuleVO;
import com.cloud.network.vpc.Vpc;
import com.cloud.network.vpc.VpcManager;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.DomainManager;
import com.cloud.user.User;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallbackWithException;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.Ip;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.apache.cloudstack.network.RoutedIpv4Manager;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class FirewallManagerTest {
    private AutoCloseable closeable;

    @Mock
    AccountManager _accountMgr;
    @Mock
    NetworkOrchestrationService _networkMgr;
    @Mock
    NetworkModel _networkModel;
    @Mock
    DomainManager _domainMgr;
    @Mock
    VpcManager _vpcMgr;
    @Mock
    IpAddressManager _ipAddrMgr;
    @Mock
    RoutedIpv4Manager routedIpv4Manager;
    @Mock
    FirewallRulesDao _firewallDao;
    @Mock
    NetworkDao _networkDao;
    @Mock
    IPAddressDao _ipAddressDao;

    @Spy
    @InjectMocks
    FirewallManagerImpl _firewallMgr;

    FirewallRule fwRule50to150;
    FirewallRule fwRule100to200;
    FirewallRule fwRule151to200;

    FirewallRule pfRule50to150;
    FirewallRule pfRule100to200;
    FirewallRule pfRule151to200;


    @Before
    public void initMocks() {
        closeable = MockitoAnnotations.openMocks(this);

        fwRule50to150 = createFirewallRule(50, 150, Purpose.Firewall);
        fwRule100to200 = createFirewallRule(100, 150, Purpose.Firewall);
        fwRule151to200 = createFirewallRule(151, 200, Purpose.Firewall);

        pfRule50to150 = createFirewallRule(50, 150, Purpose.PortForwarding);
        pfRule100to200 = createFirewallRule(100, 150, Purpose.PortForwarding);
        pfRule151to200 = createFirewallRule(151, 200, Purpose.PortForwarding);
    }

    @After
    public void tearDown() throws Exception {
        closeable.close();
    }

    private FirewallRule createFirewallRule(int startPort, int endPort, Purpose purpose) {
        return new FirewallRuleVO("xid", 1L, startPort, endPort, "TCP", 2L, 3, 4, purpose, new ArrayList<>(),
                new ArrayList<>(), 5, 6, null, FirewallRule.TrafficType.Ingress);
    }

    @Ignore("Requires database to be set up")
    @Test
    public void testApplyRules() {
        List<FirewallRuleVO> ruleList = new ArrayList<FirewallRuleVO>();
        FirewallRuleVO rule = new FirewallRuleVO("rule1", 1, 80, "TCP", 1, 2, 1, FirewallRule.Purpose.Firewall, null, null, null, null);
        ruleList.add(rule);
        FirewallManagerImpl firewallMgr = (FirewallManagerImpl)_firewallMgr;

        NetworkOrchestrationService netMgr = mock(NetworkOrchestrationService.class);
        IpAddressManager addrMgr = mock(IpAddressManager.class);
        firewallMgr._networkMgr = netMgr;

        try {
            firewallMgr.applyRules(ruleList, false, false);
            verify(addrMgr).applyRules(any(List.class), any(FirewallRule.Purpose.class), any(NetworkRuleApplier.class), anyBoolean());

        } catch (ResourceUnavailableException e) {
            Assert.fail("Unreachable code");
        }
    }

    @Ignore("Requires database to be set up")
    @Test
    public void testApplyFWRules() {
        List<FirewallRuleVO> ruleList = new ArrayList<FirewallRuleVO>();
        FirewallRuleVO rule = new FirewallRuleVO("rule1", 1, 80, "TCP", 1, 2, 1, FirewallRule.Purpose.Firewall, null, null, null, null);
        ruleList.add(rule);
        FirewallManagerImpl firewallMgr = (FirewallManagerImpl)_firewallMgr;
        VirtualRouterElement virtualRouter = mock(VirtualRouterElement.class);
        VpcVirtualRouterElement vpcVirtualRouter = mock(VpcVirtualRouterElement.class);

        List<FirewallServiceProvider> fwElements = new ArrayList<FirewallServiceProvider>();
        fwElements.add(ComponentContext.inject(VirtualRouterElement.class));
        fwElements.add(ComponentContext.inject(VpcVirtualRouterElement.class));

        firewallMgr._firewallElements = fwElements;

        try {
            when(virtualRouter.applyFWRules(any(Network.class), any(List.class))).thenReturn(false);
            when(vpcVirtualRouter.applyFWRules(any(Network.class), any(List.class))).thenReturn(true);
            //Network network, Purpose purpose, List<? extends FirewallRule> rules
            firewallMgr.applyRules(mock(Network.class), Purpose.Firewall, ruleList);
            verify(vpcVirtualRouter).applyFWRules(any(Network.class), any(List.class));
            verify(virtualRouter).applyFWRules(any(Network.class), any(List.class));

        } catch (ResourceUnavailableException e) {
            Assert.fail("Unreachable code");
        }
    }

    private List<FirewallRuleVO> createExistingFirewallListRulesList(long existingNetworkId) {
        List<FirewallRuleVO> ruleList = new ArrayList<>();
        FirewallRuleVO rule1 = spy(new FirewallRuleVO("rule1", 3, 500, "UDP", existingNetworkId, 2, 1, Purpose.Vpn, null, null, null, null));
        FirewallRuleVO rule2 = spy(new FirewallRuleVO("rule2", 3, 1701, "UDP", existingNetworkId, 2, 1, Purpose.Vpn, null, null, null, null));
        FirewallRuleVO rule3 = spy(new FirewallRuleVO("rule3", 3, 4500, "UDP", existingNetworkId, 2, 1, Purpose.Vpn, null, null, null, null));

        List<String> sString = Arrays.asList("10.1.1.1/24","192.168.1.1/24");
        List<String> dString1 = Arrays.asList("10.1.1.1/25");

        FirewallRuleVO rule4 = spy(new FirewallRuleVO("rule4", 3L, 10, 20, "TCP", existingNetworkId, 2, 1, Purpose.Firewall, sString, dString1, null, null,
                null, FirewallRule.TrafficType.Egress));

        when(rule1.getId()).thenReturn(1L);
        when(rule2.getId()).thenReturn(2L);
        when(rule3.getId()).thenReturn(3L);
        when(rule4.getId()).thenReturn(4L);

        ruleList.add(rule1);
        ruleList.add(rule2);
        ruleList.add(rule3);
        ruleList.add(rule4);

        return ruleList;
    }

    private List<FirewallRule> createNewRuleList(long newNetworkId) {
        List<String> sString = Arrays.asList("10.1.1.1/24","192.168.1.1/24");
        List<String> dString2 = Arrays.asList("10.1.1.128/25");

        FirewallRule newRule1 = new FirewallRuleVO("newRule1", 3, 500, "TCP", newNetworkId, 2, 1, Purpose.PortForwarding, null, null, null, null);
        FirewallRule newRule2 = new FirewallRuleVO("newRule2", 3, 1701, "TCP", newNetworkId, 2, 1, Purpose.PortForwarding, null, null, null, null);
        FirewallRule newRule3 = new FirewallRuleVO("newRule3", 3, 4500, "TCP", newNetworkId, 2, 1, Purpose.PortForwarding, null, null, null, null);
        FirewallRule newRule4 = new FirewallRuleVO("newRule4", 3L, 15, 25, "TCP", newNetworkId, 2, 1, Purpose.Firewall, sString, dString2, null, null,
                null, FirewallRule.TrafficType.Egress);
        return Arrays.asList(newRule1, newRule2, newRule3, newRule4);
    }

    @Test
    public void testDetectRulesConflictIsolatedNetwork() {
        List<FirewallRuleVO> ruleList = createExistingFirewallListRulesList(1L);
        when(_firewallMgr._firewallDao.listByIpAndPurposeAndNotRevoked(3,null)).thenReturn(ruleList);

        List<FirewallRule> newRuleList = createNewRuleList(1L);

        NetworkVO networkVO = Mockito.mock(NetworkVO.class);
        when(_firewallMgr._networkDao.findById(1L)).thenReturn(networkVO);
        when(networkVO.getVpcId()).thenReturn(null);

        try {
            for (FirewallRule newRule : newRuleList) {
                _firewallMgr.detectRulesConflict(newRule);
            }
        }
        catch (NetworkRuleConflictException ex) {
            Assert.fail();
        }
    }

    private void testDetectRulesConflictVpcBase(boolean vpcConserveMode) throws NetworkRuleConflictException {
        long existingNetworkId = 1L;
        long newNetworkId = 2L;
        long vpcId = 10L;

        List<FirewallRuleVO> ruleList = createExistingFirewallListRulesList(existingNetworkId);
        when(_firewallMgr._firewallDao.listByIpAndPurposeAndNotRevoked(3,null)).thenReturn(ruleList);

        List<FirewallRule> newRuleList = createNewRuleList(newNetworkId);

        NetworkVO newNetworkVO = Mockito.mock(NetworkVO.class);
        Vpc vpc = Mockito.mock(Vpc.class);
        when(_firewallMgr._networkDao.findById(2L)).thenReturn(newNetworkVO);
        when(newNetworkVO.getVpcId()).thenReturn(vpcId);
        when(_vpcMgr.getActiveVpc(Mockito.eq(vpcId))).thenReturn(vpc);
        when(_vpcMgr.isNetworkOnVpcEnabledConserveMode(Mockito.eq(newNetworkVO))).thenReturn(vpcConserveMode);

        for (FirewallRule newRule : newRuleList) {
            _firewallMgr.detectRulesConflict(newRule);
        }
    }

    @Test
    public void testDetectRulesConflictVpcConserveMode() throws NetworkRuleConflictException {
        // When VPC conserve mode is enabled, rules can be created for multiple network tiers
        testDetectRulesConflictVpcBase(true);
    }

    @Test(expected = NetworkRuleConflictException.class)
    public void testDetectRulesConflictVpcConserveModeFalse() throws NetworkRuleConflictException {
        // When VPC conserve mode is disabled, an exception should be thrown when attempting to create rules on different network tiers
        testDetectRulesConflictVpcBase(false);
    }

    @Test
    public void checkIfRulesHaveConflictingPortRangesTestOnlyOneRuleIsFirewallReturnsFalse()
    {
        boolean result = _firewallMgr.checkIfRulesHaveConflictingPortRanges(fwRule50to150, pfRule50to150, true, false, false, true);

        Assert.assertFalse(result);
    }

    @Test
    public void checkIfRulesHaveConflictingPortRangesTestBothRulesAreFirewallButNoDuplicateCidrsReturnsFalse()
    {
        boolean result = _firewallMgr.checkIfRulesHaveConflictingPortRanges(fwRule50to150, fwRule50to150, false, true, false, false);

        Assert.assertFalse(result);
    }

    @Test
    public void checkIfRulesHaveConflictingPortRangesTestBothRulesArePortForwardingButNoDuplicateCidrsReturnsFalse()
    {
        boolean result = _firewallMgr.checkIfRulesHaveConflictingPortRanges(pfRule50to150, pfRule50to150, false, false, true, false);

        Assert.assertFalse(result);
    }

    @Test
    public void checkIfRulesHaveConflictingPortRangesTestBothRulesAreFirewallAndDuplicatedCidrsAndNewRuleSourceStartPortIsInsideExistingRangeReturnsTrue()
    {
        boolean result = _firewallMgr.checkIfRulesHaveConflictingPortRanges(fwRule100to200, fwRule50to150, false, true, false, true);

        Assert.assertTrue(result);
    }

    @Test
    public void checkIfRulesHaveConflictingPortRangesTestBothRulesAreFirewallAndDuplicatedCidrsAndNewRuleSourceEndPortIsInsideExistingRangeReturnsTrue()
    {
        boolean result = _firewallMgr.checkIfRulesHaveConflictingPortRanges(fwRule50to150, fwRule100to200, false, true, false, true);

        Assert.assertTrue(result);
    }

    @Test
    public void checkIfRulesHaveConflictingPortRangesTestBothRulesArePortForwardingAndDuplicatedCidrsAndNewRuleSourceStartPortIsInsideExistingRangeReturnsTrue()
    {
        boolean result = _firewallMgr.checkIfRulesHaveConflictingPortRanges(pfRule50to150, pfRule100to200, false, false, true, true);

        Assert.assertTrue(result);
    }

    @Test
    public void checkIfRulesHaveConflictingPortRangesTestBothRulesArePortForwardingAndDuplicatedCidrsAndNewRuleSourceEndPortIsInsideExistingRangeReturnsTrue()
    {
        boolean result = _firewallMgr.checkIfRulesHaveConflictingPortRanges(pfRule50to150, pfRule100to200, false, false, true, true);

        Assert.assertTrue(result);
    }

    @Test
    public void checkIfRulesHaveConflictingPortRangesTestBothRulesAreFirewallAndDuplicatedCidrsAndNoRangeConflictReturnsFalse()
    {
        boolean result = _firewallMgr.checkIfRulesHaveConflictingPortRanges(fwRule50to150, fwRule151to200, false, true, false, true);

        Assert.assertFalse(result);
    }

    @Test
    public void checkIfRulesHaveConflictingPortRangesTestBothRulesArePortForwardingAndDuplicatedCidrsAndNoRangeConflictReturnsFalse()
    {
        boolean result = _firewallMgr.checkIfRulesHaveConflictingPortRanges(pfRule50to150, pfRule151to200, false, false, true, true);

        Assert.assertFalse(result);
    }

    @Test
    public void testValidateFirewallRuleVpcWithoutAssociatedNetworkUsesVpcCapabilities() {
        final Account caller = Mockito.mock(Account.class);
        final IPAddressVO ipAddress = Mockito.mock(IPAddressVO.class);
        final NetworkVO network = Mockito.mock(NetworkVO.class);
        final FirewallServiceProvider firewallServiceProvider = Mockito.mock(FirewallServiceProvider.class);
        final Map<Capability, String> firewallCaps = new HashMap<>();
        final Map<Service, Map<Capability, String>> capabilities = new HashMap<>();

        firewallCaps.put(Capability.SupportedTrafficDirection, "ingress, egress");
        firewallCaps.put(Capability.SupportedProtocols, "tcp,udp,icmp");
        firewallCaps.put(Capability.SupportedEgressProtocols, "tcp,udp,icmp");
        capabilities.put(Service.Firewall, firewallCaps);

        when(ipAddress.getVpcId()).thenReturn(10L);
        when(_networkModel.getNetwork(2L)).thenReturn(network);
        when(network.getVpcId()).thenReturn(10L);
        when(routedIpv4Manager.isVirtualRouterGateway(network)).thenReturn(false);
        when(firewallServiceProvider.getProvider()).thenReturn(Network.Provider.VPCVirtualRouter);
        when(firewallServiceProvider.getCapabilities()).thenReturn(capabilities);
        when(_vpcMgr.isProviderSupportServiceInVpc(10L, Service.Firewall, Network.Provider.VPCVirtualRouter)).thenReturn(true);
        _firewallMgr._firewallElements = List.of(firewallServiceProvider);

        _firewallMgr.validateFirewallRule(caller, ipAddress, 80, 80, "tcp", Purpose.Firewall, FirewallRuleType.User, 2L, FirewallRule.TrafficType.Ingress);

        verify(_networkModel, Mockito.never()).getNetworkServiceCapabilities(Mockito.anyLong(), Mockito.eq(Service.Firewall));
    }

    @Test
    public void testIsVpcIpAddressReturnsTrueWhenVpcIdPresent() {
        IPAddressVO ipAddress = Mockito.mock(IPAddressVO.class);
        when(ipAddress.getVpcId()).thenReturn(5L);
        Assert.assertTrue(_firewallMgr.isVpcIpAddress(ipAddress));
    }

    @Test
    public void testIsVpcIpAddressReturnsFalseWhenVpcIdNull() {
        IPAddressVO ipAddress = Mockito.mock(IPAddressVO.class);
        when(ipAddress.getVpcId()).thenReturn(null);
        Assert.assertFalse(_firewallMgr.isVpcIpAddress(ipAddress));
    }

    @Test
    public void testValidateFirewallRuleForIsolatedIpReturnsNetworkId() {
        IPAddressVO ipAddress = Mockito.mock(IPAddressVO.class);
        when(ipAddress.getAssociatedWithNetworkId()).thenReturn(42L);
        Long result = _firewallMgr.validateFirewallRuleForIsolatedIp(ipAddress);
        Assert.assertEquals(Long.valueOf(42L), result);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testValidateFirewallRuleForIsolatedIpThrowsWhenNotAssociated() {
        IPAddressVO ipAddress = Mockito.mock(IPAddressVO.class);
        when(ipAddress.getAssociatedWithNetworkId()).thenReturn(null);
        _firewallMgr.validateFirewallRuleForIsolatedIp(ipAddress);
    }

    @Test
    public void testValidateFirewallRuleForVpcIpReturnsNetworkId() {
        IPAddressVO ipAddress = Mockito.mock(IPAddressVO.class);
        Long result = _firewallMgr.validateFirewallRuleForVpcIp(ipAddress, 99L);
        Assert.assertEquals(Long.valueOf(99L), result);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testValidateFirewallRuleForVpcIpThrowsWhenNetworkIdNull() {
        IPAddressVO ipAddress = Mockito.mock(IPAddressVO.class);
        _firewallMgr.validateFirewallRuleForVpcIp(ipAddress, null);
    }

    @Test
    public void testGetFirewallServiceCapabilitiesForNonVpcNetworkUsesNetworkModel() {
        NetworkVO network = Mockito.mock(NetworkVO.class);
        when(network.getId()).thenReturn(1L);
        when(network.getVpcId()).thenReturn(null);
        Map<Capability, String> caps = new HashMap<>();
        caps.put(Capability.SupportedProtocols, "tcp,udp");
        when(_networkModel.getNetworkServiceCapabilities(1L, Service.Firewall)).thenReturn(caps);

        Map<Network.Capability, String> result = _firewallMgr.getFirewallServiceCapabilities(network);

        Assert.assertEquals(caps, result);
        verify(_networkModel, times(1)).getNetworkServiceCapabilities(1L, Service.Firewall);
    }

    @Test
    public void testGetFirewallServiceCapabilitiesForVpcNetworkUsesVpcProvider() {
        NetworkVO network = Mockito.mock(NetworkVO.class);
        FirewallServiceProvider fwProvider = Mockito.mock(FirewallServiceProvider.class);
        Map<Capability, String> firewallCaps = new HashMap<>();
        firewallCaps.put(Capability.SupportedProtocols, "tcp,udp,icmp");
        Map<Service, Map<Capability, String>> providerCapabilities = new HashMap<>();
        providerCapabilities.put(Service.Firewall, firewallCaps);

        when(network.getVpcId()).thenReturn(10L);
        when(fwProvider.getProvider()).thenReturn(Network.Provider.VPCVirtualRouter);
        when(fwProvider.getCapabilities()).thenReturn(providerCapabilities);
        when(_vpcMgr.isProviderSupportServiceInVpc(10L, Service.Firewall, Network.Provider.VPCVirtualRouter)).thenReturn(true);
        _firewallMgr._firewallElements = List.of(fwProvider);

        Map<Network.Capability, String> result = _firewallMgr.getFirewallServiceCapabilities(network);

        Assert.assertEquals(firewallCaps, result);
        verify(_networkModel, never()).getNetworkServiceCapabilities(Mockito.anyLong(), Mockito.eq(Service.Firewall));
    }

    @Test
    public void testGetFirewallServiceCapabilitiesForVpcNetworkFallsBackToNetworkModelWhenNoProvider() {
        NetworkVO network = Mockito.mock(NetworkVO.class);
        FirewallServiceProvider fwProvider = Mockito.mock(FirewallServiceProvider.class);
        Map<Capability, String> fallbackCaps = new HashMap<>();

        when(network.getId()).thenReturn(1L);
        when(network.getVpcId()).thenReturn(10L);
        when(fwProvider.getProvider()).thenReturn(Network.Provider.VPCVirtualRouter);
        when(_vpcMgr.isProviderSupportServiceInVpc(10L, Service.Firewall, Network.Provider.VPCVirtualRouter)).thenReturn(false);
        when(_networkModel.getNetworkServiceCapabilities(1L, Service.Firewall)).thenReturn(fallbackCaps);
        _firewallMgr._firewallElements = List.of(fwProvider);

        Map<Network.Capability, String> result = _firewallMgr.getFirewallServiceCapabilities(network);

        Assert.assertEquals(fallbackCaps, result);
        verify(_networkModel, times(1)).getNetworkServiceCapabilities(1L, Service.Firewall);
    }

    @Test
    public void testGetFirewallServiceCapabilitiesForVpcReturnsCapabilitiesWhenProviderSupports() {
        FirewallServiceProvider fwProvider = Mockito.mock(FirewallServiceProvider.class);
        Map<Capability, String> firewallCaps = new HashMap<>();
        firewallCaps.put(Capability.SupportedProtocols, "tcp,udp");
        Map<Service, Map<Capability, String>> caps = new HashMap<>();
        caps.put(Service.Firewall, firewallCaps);

        when(fwProvider.getProvider()).thenReturn(Network.Provider.VPCVirtualRouter);
        when(fwProvider.getCapabilities()).thenReturn(caps);
        when(_vpcMgr.isProviderSupportServiceInVpc(10L, Service.Firewall, Network.Provider.VPCVirtualRouter)).thenReturn(true);
        _firewallMgr._firewallElements = List.of(fwProvider);

        Map<Network.Capability, String> result = _firewallMgr.getFirewallServiceCapabilitiesForVpc(10L);

        Assert.assertNotNull(result);
        Assert.assertEquals("tcp,udp", result.get(Capability.SupportedProtocols));
    }

    @Test
    public void testGetFirewallServiceCapabilitiesForVpcReturnsNullWhenNoProviderSupports() {
        FirewallServiceProvider fwProvider = Mockito.mock(FirewallServiceProvider.class);
        when(fwProvider.getProvider()).thenReturn(Network.Provider.VPCVirtualRouter);
        when(_vpcMgr.isProviderSupportServiceInVpc(10L, Service.Firewall, Network.Provider.VPCVirtualRouter)).thenReturn(false);
        _firewallMgr._firewallElements = List.of(fwProvider);

        Map<Network.Capability, String> result = _firewallMgr.getFirewallServiceCapabilitiesForVpc(10L);

        Assert.assertNull(result);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testValidateFirewallRuleForVpcThrowsOnInvalidStartPort() {
        Account caller = Mockito.mock(Account.class);
        _firewallMgr.validateFirewallRuleForVpc(caller, null, -1, 80, "tcp", Purpose.Firewall, FirewallRuleType.User, 10L, FirewallRule.TrafficType.Ingress);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testValidateFirewallRuleForVpcThrowsOnInvalidEndPort() {
        Account caller = Mockito.mock(Account.class);
        _firewallMgr.validateFirewallRuleForVpc(caller, null, 80, 70000, "tcp", Purpose.Firewall, FirewallRuleType.User, 10L, FirewallRule.TrafficType.Ingress);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testValidateFirewallRuleForVpcThrowsWhenStartPortGreaterThanEndPort() {
        Account caller = Mockito.mock(Account.class);
        _firewallMgr.validateFirewallRuleForVpc(caller, null, 200, 100, "tcp", Purpose.Firewall, FirewallRuleType.User, 10L, FirewallRule.TrafficType.Ingress);
    }

    @Test
    public void testValidateFirewallRuleForVpcSystemTypeWithNullIpReturnsEarly() {
        // System rule type + null IP should return without further validation
        Account caller = Mockito.mock(Account.class);
        // Should not throw even though vpcId checks come after this
        _firewallMgr.validateFirewallRuleForVpc(caller, null, 80, 80, "tcp", Purpose.Firewall, FirewallRuleType.System, 10L, FirewallRule.TrafficType.Ingress);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testValidateFirewallRuleForVpcThrowsWhenVpcIdNullAndNotSystemRule() {
        Account caller = Mockito.mock(Account.class);
        IPAddressVO ipAddress = Mockito.mock(IPAddressVO.class);
        _firewallMgr.validateFirewallRuleForVpc(caller, ipAddress, 80, 80, "tcp", Purpose.Firewall, FirewallRuleType.User, null, FirewallRule.TrafficType.Ingress);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testValidateFirewallRuleForVpcThrowsWhenActiveVpcNotFound() {
        Account caller = Mockito.mock(Account.class);
        IPAddressVO ipAddress = Mockito.mock(IPAddressVO.class);
        when(_vpcMgr.getActiveVpc(10L)).thenReturn(null);
        _firewallMgr.validateFirewallRuleForVpc(caller, ipAddress, 80, 80, "tcp", Purpose.Firewall, FirewallRuleType.User, 10L, FirewallRule.TrafficType.Ingress);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testValidateFirewallRuleForVpcThrowsWhenFirewallServiceNotSupported() {
        Account caller = Mockito.mock(Account.class);
        IPAddressVO ipAddress = Mockito.mock(IPAddressVO.class);
        Vpc vpc = Mockito.mock(Vpc.class);
        when(_vpcMgr.getActiveVpc(10L)).thenReturn(vpc);
        _firewallMgr._firewallElements = Collections.emptyList();

        _firewallMgr.validateFirewallRuleForVpc(caller, ipAddress, 80, 80, "tcp", Purpose.Firewall, FirewallRuleType.User, 10L, FirewallRule.TrafficType.Ingress);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testValidateFirewallRuleForVpcThrowsOnUnsupportedProtocol() {
        Account caller = Mockito.mock(Account.class);
        IPAddressVO ipAddress = Mockito.mock(IPAddressVO.class);
        Vpc vpc = Mockito.mock(Vpc.class);
        FirewallServiceProvider fwProvider = Mockito.mock(FirewallServiceProvider.class);
        Map<Capability, String> firewallCaps = new HashMap<>();
        firewallCaps.put(Capability.SupportedProtocols, "tcp,udp");
        firewallCaps.put(Capability.SupportedTrafficDirection, "ingress,egress");
        Map<Service, Map<Capability, String>> caps = new HashMap<>();
        caps.put(Service.Firewall, firewallCaps);

        when(_vpcMgr.getActiveVpc(10L)).thenReturn(vpc);
        when(fwProvider.getProvider()).thenReturn(Network.Provider.VPCVirtualRouter);
        when(fwProvider.getCapabilities()).thenReturn(caps);
        when(_vpcMgr.isProviderSupportServiceInVpc(10L, Service.Firewall, Network.Provider.VPCVirtualRouter)).thenReturn(true);
        _firewallMgr._firewallElements = List.of(fwProvider);

        _firewallMgr.validateFirewallRuleForVpc(caller, ipAddress, 80, 80, "gre", Purpose.Firewall, FirewallRuleType.User, 10L, FirewallRule.TrafficType.Ingress);
    }

    @Test
    public void testValidateFirewallRuleForVpcSucceedsWithSupportedProtocolAndTrafficType() {
        Account caller = Mockito.mock(Account.class);
        IPAddressVO ipAddress = Mockito.mock(IPAddressVO.class);
        Vpc vpc = Mockito.mock(Vpc.class);
        FirewallServiceProvider fwProvider = Mockito.mock(FirewallServiceProvider.class);
        Map<Capability, String> firewallCaps = new HashMap<>();
        firewallCaps.put(Capability.SupportedProtocols, "tcp,udp,icmp");
        firewallCaps.put(Capability.SupportedTrafficDirection, "ingress,egress");
        Map<Service, Map<Capability, String>> caps = new HashMap<>();
        caps.put(Service.Firewall, firewallCaps);

        when(_vpcMgr.getActiveVpc(10L)).thenReturn(vpc);
        when(fwProvider.getProvider()).thenReturn(Network.Provider.VPCVirtualRouter);
        when(fwProvider.getCapabilities()).thenReturn(caps);
        when(_vpcMgr.isProviderSupportServiceInVpc(10L, Service.Firewall, Network.Provider.VPCVirtualRouter)).thenReturn(true);
        _firewallMgr._firewallElements = List.of(fwProvider);

        // Should not throw
        _firewallMgr.validateFirewallRuleForVpc(caller, ipAddress, 80, 80, "tcp", Purpose.Firewall, FirewallRuleType.User, 10L, FirewallRule.TrafficType.Ingress);

        verify(_accountMgr, times(1)).checkAccess(caller, null, true, ipAddress);
    }

    @Test
    public void testCreateIngressFirewallRuleRejectsReservedVpcIpUnderLock() throws NetworkRuleConflictException {
        Account caller = mock(Account.class);
        IPAddressVO sourceIp = createVpcIpAddress();
        IPAddressVO lockedIp = createVpcIpAddress();
        when(_ipAddressDao.findById(1L)).thenReturn(sourceIp);
        when(_ipAddressDao.acquireInLockTable(1L)).thenReturn(lockedIp);
        configureVpcFirewallCapabilities();
        NetworkElement owningProvider = mock(NetworkElement.class);
        NetworkModelImpl networkModel = useProviderReservation(owningProvider);
        when(owningProvider.isIpAddressReserved(lockedIp)).thenReturn(true);

        CallContext.register(mock(User.class), caller);
        try (MockedStatic<Transaction> transactions = Mockito.mockStatic(Transaction.class)) {
            InvalidParameterValueException exception = Assert.assertThrows(InvalidParameterValueException.class,
                    () -> _firewallMgr.createIngressFirewallRule(createVpcIngressRule()));
            Assert.assertTrue(exception.getMessage().contains("reserved by a network provider"));
            transactions.verifyNoInteractions();
        } finally {
            CallContext.unregister();
        }

        org.mockito.InOrder order = Mockito.inOrder(_ipAddressDao, _accountMgr, owningProvider);
        order.verify(_ipAddressDao).findById(1L);
        order.verify(_ipAddressDao).acquireInLockTable(1L);
        order.verify(_accountMgr).checkAccess(caller, null, true, lockedIp);
        order.verify(owningProvider).isIpAddressReserved(lockedIp);
        order.verify(_ipAddressDao).releaseFromLockTable(1L);
        verify(owningProvider, never()).isIpAddressReserved(sourceIp);
        verify(networkModel, never()).checkIpForService(any(), any(), any());
        Mockito.verifyNoInteractions(_firewallDao);
    }

    @Test
    public void testCreateIngressFirewallRuleAllowsUnreservedVpcIpWithoutTierAssociation() throws NetworkRuleConflictException {
        Account caller = mock(Account.class);
        IPAddressVO ipAddress = createVpcIpAddress();
        Assert.assertNull(ipAddress.getAssociatedWithNetworkId());
        when(_ipAddressDao.findById(1L)).thenReturn(ipAddress);
        when(_ipAddressDao.acquireInLockTable(1L)).thenReturn(ipAddress);
        configureVpcFirewallCapabilities();
        NetworkElement owningProvider = mock(NetworkElement.class);
        NetworkModelImpl networkModel = useProviderReservation(owningProvider);
        List<FirewallRuleVO> persistedRules = mockFirewallRulePersistence();
        when(_firewallDao.listByIpAndPurposeAndNotRevoked(1L, null)).thenReturn(persistedRules);

        FirewallRule result;
        CallContext.register(mock(User.class), caller);
        try (MockedStatic<Transaction> transactions = mockFirewallRuleTransactions()) {
            result = _firewallMgr.createIngressFirewallRule(createVpcIngressRule());
            Assert.assertEquals(result.getId(), CallContext.current().getContextParameter(FirewallRule.class));
        } finally {
            CallContext.unregister();
        }

        Assert.assertSame(persistedRules.get(0), result);
        Assert.assertEquals(Long.valueOf(10L), result.getVpcId());
        Assert.assertNull(result.getNetworkId());
        Assert.assertEquals(Long.valueOf(1L), result.getSourceIpAddressId());
        Assert.assertEquals(3L, result.getAccountId());
        Assert.assertEquals(4L, result.getDomainId());
        Assert.assertEquals(FirewallRule.State.Add, result.getState());
        org.mockito.InOrder order = Mockito.inOrder(_ipAddressDao, _accountMgr, owningProvider, _firewallDao);
        order.verify(_ipAddressDao).acquireInLockTable(1L);
        order.verify(_accountMgr).checkAccess(caller, null, true, ipAddress);
        order.verify(owningProvider).isIpAddressReserved(ipAddress);
        order.verify(_firewallDao).persist((FirewallRuleVO) result);
        order.verify(_firewallDao).setStateToAdd((FirewallRuleVO) result);
        order.verify(_ipAddressDao).releaseFromLockTable(1L);
        verify(networkModel, never()).checkIpForService(any(), any(), any());
    }

    @Test
    public void testCreateIngressFirewallRuleChecksVpcIpAccessBeforeReservation() throws NetworkRuleConflictException {
        Account caller = mock(Account.class);
        IPAddressVO ipAddress = createVpcIpAddress();
        when(_ipAddressDao.findById(1L)).thenReturn(ipAddress);
        when(_ipAddressDao.acquireInLockTable(1L)).thenReturn(ipAddress);
        PermissionDeniedException denied = new PermissionDeniedException("No access to this IP address");
        Mockito.doThrow(denied).when(_accountMgr).checkAccess(caller, null, true, ipAddress);

        CallContext.register(mock(User.class), caller);
        try (MockedStatic<Transaction> transactions = Mockito.mockStatic(Transaction.class)) {
            Assert.assertSame(denied, Assert.assertThrows(PermissionDeniedException.class,
                    () -> _firewallMgr.createIngressFirewallRule(createVpcIngressRule())));
            transactions.verifyNoInteractions();
        } finally {
            CallContext.unregister();
        }

        org.mockito.InOrder order = Mockito.inOrder(_ipAddressDao, _accountMgr);
        order.verify(_ipAddressDao).acquireInLockTable(1L);
        order.verify(_accountMgr).checkAccess(caller, null, true, ipAddress);
        order.verify(_ipAddressDao).releaseFromLockTable(1L);
        Mockito.verifyNoInteractions(_networkModel, _vpcMgr, _firewallDao);
    }

    @Test
    public void testCreateIngressFirewallRuleCannotUseVpcIpWithoutLock() throws NetworkRuleConflictException {
        Account caller = mock(Account.class);
        when(_ipAddressDao.findById(1L)).thenReturn(createVpcIpAddress());

        CallContext.register(mock(User.class), caller);
        try (MockedStatic<Transaction> transactions = Mockito.mockStatic(Transaction.class)) {
            InvalidParameterValueException exception = Assert.assertThrows(InvalidParameterValueException.class,
                    () -> _firewallMgr.createIngressFirewallRule(createVpcIngressRule()));
            Assert.assertTrue(exception.getMessage().contains("couldn't locate IP address"));
            transactions.verifyNoInteractions();
        } finally {
            CallContext.unregister();
        }

        org.mockito.InOrder order = Mockito.inOrder(_ipAddressDao);
        order.verify(_ipAddressDao).acquireInLockTable(1L);
        order.verify(_ipAddressDao).releaseFromLockTable(1L);
        Mockito.verifyNoInteractions(_accountMgr, _networkModel, _vpcMgr, _firewallDao);
    }

    @Test
    public void testCreateFirewallRuleAllowsVpcSystemRuleWithoutIp() throws NetworkRuleConflictException {
        Account caller = mock(Account.class);
        Vpc vpc = mock(Vpc.class);
        when(_vpcMgr.getActiveVpc(10L)).thenReturn(vpc);
        when(vpc.getAccountId()).thenReturn(3L);
        when(vpc.getDomainId()).thenReturn(4L);
        List<FirewallRuleVO> persistedRules = mockFirewallRulePersistence();

        FirewallRule result;
        CallContext.register(mock(User.class), caller);
        try (MockedStatic<Transaction> transactions = mockFirewallRuleTransactions()) {
            result = _firewallMgr.createFirewallRule(null, caller, "xid", 80, 80, "tcp",
                    Collections.singletonList("0.0.0.0/0"), null, null, null, null,
                    FirewallRuleType.System, null, 10L, FirewallRule.TrafficType.Egress, true);
        } finally {
            CallContext.unregister();
        }

        Assert.assertSame(persistedRules.get(0), result);
        Assert.assertEquals(Long.valueOf(10L), result.getVpcId());
        Assert.assertNull(result.getNetworkId());
        Assert.assertNull(result.getSourceIpAddressId());
        Assert.assertEquals(3L, result.getAccountId());
        Assert.assertEquals(4L, result.getDomainId());
        Assert.assertEquals(FirewallRuleType.System, result.getType());
        Assert.assertEquals(FirewallRule.State.Add, result.getState());
        verify(_firewallDao).setStateToAdd((FirewallRuleVO) result);
        Mockito.verifyNoInteractions(_ipAddressDao, _accountMgr, _networkModel);
    }

    private IPAddressVO createVpcIpAddress() {
        IPAddressVO ipAddress = new IPAddressVO(new Ip("198.51.100.10"), 1L, 0L, 1L, false);
        ReflectionTestUtils.setField(ipAddress, "id", 1L);
        ipAddress.setState(IpAddress.State.Allocated);
        ipAddress.setVpcId(10L);
        ipAddress.setAllocatedToAccountId(3L);
        ipAddress.setAllocatedInDomainId(4L);
        return ipAddress;
    }

    private FirewallRule createVpcIngressRule() {
        return new FirewallRuleVO("xid", 1L, 80, 80, "tcp", null, 3L, 4L, Purpose.Firewall,
                Collections.singletonList("0.0.0.0/0"), null, null, null, FirewallRule.TrafficType.Ingress);
    }

    private void configureVpcFirewallCapabilities() {
        FirewallServiceProvider firewallProvider = mock(FirewallServiceProvider.class);
        Map<Capability, String> firewallCaps = new HashMap<>();
        firewallCaps.put(Capability.SupportedProtocols, "tcp,udp,icmp");
        firewallCaps.put(Capability.SupportedTrafficDirection, "ingress,egress");
        when(_vpcMgr.getActiveVpc(10L)).thenReturn(mock(Vpc.class));
        when(firewallProvider.getProvider()).thenReturn(Network.Provider.VPCVirtualRouter);
        when(firewallProvider.getCapabilities()).thenReturn(Collections.singletonMap(Service.Firewall, firewallCaps));
        when(_vpcMgr.isProviderSupportServiceInVpc(10L, Service.Firewall, Network.Provider.VPCVirtualRouter)).thenReturn(true);
        _firewallMgr._firewallElements = Collections.singletonList(firewallProvider);
    }

    private NetworkModelImpl useProviderReservation(NetworkElement owningProvider) {
        NetworkModelImpl networkModel = spy(new NetworkModelImpl());
        networkModel.setNetworkElements(Collections.singletonList(owningProvider));
        _firewallMgr._networkModel = networkModel;
        return networkModel;
    }

    private List<FirewallRuleVO> mockFirewallRulePersistence() {
        List<FirewallRuleVO> persistedRules = new ArrayList<>();
        when(_firewallDao.persist(any(FirewallRuleVO.class))).thenAnswer(invocation -> {
            FirewallRuleVO rule = invocation.getArgument(0);
            ReflectionTestUtils.setField(rule, "id", 100L);
            persistedRules.add(rule);
            return rule;
        });
        when(_firewallDao.setStateToAdd(any(FirewallRuleVO.class))).thenAnswer(invocation -> {
            FirewallRuleVO rule = invocation.getArgument(0);
            rule.setState(FirewallRule.State.Add);
            return true;
        });
        return persistedRules;
    }

    private MockedStatic<Transaction> mockFirewallRuleTransactions() {
        MockedStatic<Transaction> transactions = Mockito.mockStatic(Transaction.class);
        transactions.when(() -> Transaction.execute(Mockito.<TransactionCallbackWithException<FirewallRuleVO, NetworkRuleConflictException>>any()))
                .thenAnswer(invocation -> {
                    TransactionCallbackWithException<FirewallRuleVO, NetworkRuleConflictException> callback = invocation.getArgument(0);
                    return callback.doInTransaction(null);
                });
        return transactions;
    }

    @Test
    public void testCreateFirewallRuleRoutesToVpcWhenVpcIdProvided() throws NetworkRuleConflictException {
        Account caller = Mockito.mock(Account.class);
        FirewallRule vpcRule = Mockito.mock(FirewallRule.class);

        doReturn(vpcRule).when(_firewallMgr).createFirewallRuleForVpc(
                Mockito.anyLong(), Mockito.eq(caller), Mockito.any(), Mockito.anyInt(), Mockito.anyInt(),
                Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
                Mockito.any(), Mockito.any(FirewallRuleType.class), Mockito.anyLong(),
                Mockito.any(FirewallRule.TrafficType.class), Mockito.anyBoolean());

        _firewallMgr.createFirewallRule(1L, caller, "xid", 80, 80, "tcp",
                Collections.singletonList("0.0.0.0/0"), null, null, null, null,
                FirewallRuleType.User, null, 10L, FirewallRule.TrafficType.Ingress, true);

        verify(_firewallMgr, times(1)).createFirewallRuleForVpc(
                Mockito.anyLong(), Mockito.eq(caller), Mockito.any(), Mockito.anyInt(), Mockito.anyInt(),
                Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
                Mockito.any(), Mockito.any(FirewallRuleType.class), Mockito.anyLong(),
                Mockito.any(FirewallRule.TrafficType.class), Mockito.anyBoolean());

        verify(_firewallMgr, never()).createFirewallRuleForNonVPC(
                Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
                Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
                Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    public void testCreateFirewallRuleRoutesToNonVpcWhenVpcIdNull() throws NetworkRuleConflictException {
        Account caller = Mockito.mock(Account.class);
        FirewallRule nonVpcRule = Mockito.mock(FirewallRule.class);

        doReturn(nonVpcRule).when(_firewallMgr).createFirewallRuleForNonVPC(
                Mockito.any(), Mockito.eq(caller), Mockito.any(), Mockito.any(), Mockito.any(),
                Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
                Mockito.any(), Mockito.any(FirewallRuleType.class), Mockito.anyLong(),
                Mockito.any(FirewallRule.TrafficType.class), Mockito.anyBoolean());

        _firewallMgr.createFirewallRule(null, caller, "xid", 80, 80, "tcp",
                Collections.singletonList("0.0.0.0/0"), null, null, null, null,
                FirewallRuleType.User, 2L, null, FirewallRule.TrafficType.Ingress, true);

        verify(_firewallMgr, times(1)).createFirewallRuleForNonVPC(
                Mockito.any(), Mockito.eq(caller), Mockito.any(), Mockito.any(), Mockito.any(),
                Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
                Mockito.any(), Mockito.any(FirewallRuleType.class), Mockito.anyLong(),
                Mockito.any(FirewallRule.TrafficType.class), Mockito.anyBoolean());

        verify(_firewallMgr, never()).createFirewallRuleForVpc(
                Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
                Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
                Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    public void testApplyRulesForVpcNetworkUsesVpcProviderCheck() throws ResourceUnavailableException {
        FirewallManagerImpl firewallMgr = new FirewallManagerImpl();
        firewallMgr._networkModel = _networkModel;
        firewallMgr._vpcMgr = _vpcMgr;

        Network network = Mockito.mock(Network.class);
        FirewallServiceProvider fwProvider = Mockito.mock(FirewallServiceProvider.class);
        List<FirewallRule> rules = new ArrayList<>();
        FirewallRuleVO rule = new FirewallRuleVO("rule1", 1L, 80, 80, "tcp", 1L, 2, 3, Purpose.Firewall,
                Collections.emptyList(), Collections.emptyList(), null, null, null, FirewallRule.TrafficType.Ingress);
        rules.add(rule);

        when(network.getVpcId()).thenReturn(10L);
        when(fwProvider.getProvider()).thenReturn(Network.Provider.VPCVirtualRouter);
        when(_vpcMgr.isProviderSupportServiceInVpc(10L, Service.Firewall, Network.Provider.VPCVirtualRouter)).thenReturn(true);
        when(fwProvider.applyFWRules(Mockito.eq(network), Mockito.anyList())).thenReturn(true);
        firewallMgr._firewallElements = List.of(fwProvider);

        boolean result = firewallMgr.applyRules(network, Purpose.Firewall, rules);

        Assert.assertTrue(result);
        verify(_vpcMgr, times(1)).isProviderSupportServiceInVpc(10L, Service.Firewall, Network.Provider.VPCVirtualRouter);
        verify(_networkModel, never()).isProviderSupportServiceInNetwork(Mockito.anyLong(), Mockito.eq(Service.Firewall), Mockito.any());
    }

    @Test
    public void testApplyRulesForNonVpcNetworkUsesNetworkModelProviderCheck() throws ResourceUnavailableException {
        FirewallManagerImpl firewallMgr = new FirewallManagerImpl();
        firewallMgr._networkModel = _networkModel;
        firewallMgr._vpcMgr = _vpcMgr;

        Network network = Mockito.mock(Network.class);
        FirewallServiceProvider fwProvider = Mockito.mock(FirewallServiceProvider.class);
        List<FirewallRule> rules = new ArrayList<>();
        FirewallRuleVO rule = new FirewallRuleVO("rule1", 1L, 80, 80, "tcp", 1L, 2, 3, Purpose.Firewall,
                Collections.emptyList(), Collections.emptyList(), null, null, null, FirewallRule.TrafficType.Ingress);
        rules.add(rule);

        when(network.getId()).thenReturn(1L);
        when(network.getVpcId()).thenReturn(null);
        when(fwProvider.getProvider()).thenReturn(Network.Provider.VirtualRouter);
        when(_networkModel.isProviderSupportServiceInNetwork(1L, Service.Firewall, Network.Provider.VirtualRouter)).thenReturn(true);
        when(fwProvider.applyFWRules(Mockito.eq(network), Mockito.anyList())).thenReturn(true);
        firewallMgr._firewallElements = List.of(fwProvider);

        boolean result = firewallMgr.applyRules(network, Purpose.Firewall, rules);

        Assert.assertTrue(result);
        verify(_networkModel, times(1)).isProviderSupportServiceInNetwork(1L, Service.Firewall, Network.Provider.VirtualRouter);
        verify(_vpcMgr, never()).isProviderSupportServiceInVpc(Mockito.anyLong(), Mockito.eq(Service.Firewall), Mockito.any());
    }

    @Test
    public void testGetSourceIpForIngressRuleReturnsNullWhenIdIsNull() {
        IPAddressVO result = _firewallMgr.getSourceIpForIngressRule(null);
        Assert.assertNull(result);
    }

    @Test(expected = CloudRuntimeException.class)
    public void testGetSourceIpForIngressRuleReturnsNullWhenIpIsnotPresent() {
        when(_ipAddressDao.findById(1L)).thenReturn(null);
        _firewallMgr.getSourceIpForIngressRule(1L);
    }
}
