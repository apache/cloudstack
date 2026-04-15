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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network;
import com.cloud.network.NetworkModel;
import com.cloud.network.dao.NetworkServiceMapDao;
import com.cloud.network.element.FirewallServiceProvider;
import com.cloud.network.element.NetworkElement;
import com.cloud.network.element.PortForwardingServiceProvider;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.PortForwardingRule;

@RunWith(MockitoJUnitRunner.Silent.class)
public class FirewallManagerImplTest {

    interface MockFwElement extends NetworkElement, FirewallServiceProvider {}
    interface MockPfElement extends NetworkElement, PortForwardingServiceProvider {}

    @Spy
    @InjectMocks
    private FirewallManagerImpl firewallManager;

    @Mock
    private NetworkModel _networkModel;

    @Mock
    private NetworkServiceMapDao networkServiceMapDao;

    @Before
    public void setUp() {
        ReflectionTestUtils.setField(firewallManager, "_firewallElements", Collections.emptyList());
        ReflectionTestUtils.setField(firewallManager, "_pfElements", Collections.emptyList());
        ReflectionTestUtils.setField(firewallManager, "_staticNatElements", Collections.emptyList());
        ReflectionTestUtils.setField(firewallManager, "_networkAclElements", Collections.emptyList());
    }

    // -----------------------------------------------------------------------
    // Tests for applyRules with extension-backed Firewall provider
    // -----------------------------------------------------------------------

    @Test
    public void applyRulesFirewallHandledByExtensionProvider() throws ResourceUnavailableException {
        Network network = mock(Network.class);
        when(network.getId()).thenReturn(1L);

        FirewallRule rule = mock(FirewallRule.class);
        List<? extends FirewallRule> rules = List.of(rule);

        // No standard firewall elements handle it
        String extProviderName = "my-ext-fw-provider";
        when(networkServiceMapDao.getProviderForServiceInNetwork(1L, Network.Service.Firewall))
                .thenReturn(extProviderName);

        // The element implementing the provider is both NetworkElement and FirewallServiceProvider
        MockFwElement element = mock(MockFwElement.class);
        when(element.applyFWRules(eq(network), any())).thenReturn(true);
        when(_networkModel.getElementImplementingProvider(extProviderName)).thenReturn(element);

        boolean result = firewallManager.applyRules(network, FirewallRule.Purpose.Firewall, rules);
        assertTrue(result);
        verify(element).applyFWRules(eq(network), any());
    }

    @Test
    public void applyRulesFirewallReturnsFalseWhenNoExtensionProviderFound() throws ResourceUnavailableException {
        Network network = mock(Network.class);
        when(network.getId()).thenReturn(2L);

        FirewallRule rule = mock(FirewallRule.class);
        List<? extends FirewallRule> rules = List.of(rule);

        // No standard provider and no extension provider found
        when(networkServiceMapDao.getProviderForServiceInNetwork(2L, Network.Service.Firewall))
                .thenReturn(null);

        boolean result = firewallManager.applyRules(network, FirewallRule.Purpose.Firewall, rules);
        assertFalse(result);
        verify(_networkModel, never()).getElementImplementingProvider(any());
    }

    // -----------------------------------------------------------------------
    // Tests for applyRules with extension-backed PortForwarding provider
    // -----------------------------------------------------------------------

    @Test
    public void applyRulesPortForwardingHandledByExtensionProvider() throws ResourceUnavailableException {
        Network network = mock(Network.class);
        when(network.getId()).thenReturn(3L);

        PortForwardingRule rule = mock(PortForwardingRule.class);
        @SuppressWarnings("unchecked")
        List<PortForwardingRule> rules = List.of(rule);

        String extProviderName = "my-ext-pf-provider";
        when(networkServiceMapDao.getProviderForServiceInNetwork(3L, Network.Service.PortForwarding))
                .thenReturn(extProviderName);

        MockPfElement element = mock(MockPfElement.class);
        when(element.applyPFRules(eq(network), any())).thenReturn(true);
        when(_networkModel.getElementImplementingProvider(extProviderName)).thenReturn(element);

        boolean result = firewallManager.applyRules(network, FirewallRule.Purpose.PortForwarding, rules);
        assertTrue(result);
        verify(element).applyPFRules(eq(network), any());
    }

    @Test
    public void applyRulesPortForwardingReturnsFalseWhenNoExtensionProviderFound() throws ResourceUnavailableException {
        Network network = mock(Network.class);
        when(network.getId()).thenReturn(4L);

        PortForwardingRule rule = mock(PortForwardingRule.class);
        List<? extends FirewallRule> rules = List.of(rule);

        when(networkServiceMapDao.getProviderForServiceInNetwork(4L, Network.Service.PortForwarding))
                .thenReturn(null);

        boolean result = firewallManager.applyRules(network, FirewallRule.Purpose.PortForwarding, rules);
        assertFalse(result);
    }

    // -----------------------------------------------------------------------
    // Tests for StaticNat (handled by Firewall elements)
    // -----------------------------------------------------------------------

    @Test
    public void applyRulesStaticNatReturnsFalseWhenNoProviderFound() throws ResourceUnavailableException {
        Network network = mock(Network.class);
        when(network.getId()).thenReturn(5L);

        FirewallRule rule = mock(FirewallRule.class);
        List<? extends FirewallRule> rules = List.of(rule);

        when(networkServiceMapDao.getProviderForServiceInNetwork(5L, Network.Service.Firewall))
                .thenReturn(null);

        boolean result = firewallManager.applyRules(network, FirewallRule.Purpose.StaticNat, rules);
        assertFalse(result);
    }
}

