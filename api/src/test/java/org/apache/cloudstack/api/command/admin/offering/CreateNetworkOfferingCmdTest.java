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

package org.apache.cloudstack.api.command.admin.offering;

import java.util.List;
import java.util.Map;

import org.apache.cloudstack.api.command.admin.network.CreateNetworkOfferingCmd;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.springframework.test.util.ReflectionTestUtils;


public class CreateNetworkOfferingCmdTest {

    @InjectMocks
    private CreateNetworkOfferingCmd createNetworkOfferingCmd = new CreateNetworkOfferingCmd();

    String netName = "network";

    @Test
    public void createVpcNtwkOffWithEmptyDisplayText() {
        ReflectionTestUtils.setField(createNetworkOfferingCmd, "networkOfferingName", netName);
        Assert.assertEquals(createNetworkOfferingCmd.getDisplayText(), netName);
    }

    @Test
    public void nsxNattedNetworkUsesNsxFirewallAndNatServices() {
        assertServiceProviders("NSX", "NATTED", false, Map.of(
                "Dhcp", List.of("VirtualRouter"), "Dns", List.of("VirtualRouter"), "UserData", List.of("VirtualRouter"),
                "Firewall", List.of("Nsx"), "SourceNat", List.of("Nsx"), "StaticNat", List.of("Nsx"), "PortForwarding", List.of("Nsx")));
    }

    @Test
    public void nsxNattedVpcTierUsesNsxAclAndNatServices() {
        assertServiceProviders("NSX", "NATTED", true, Map.of(
                "Dhcp", List.of("VPCVirtualRouter"), "Dns", List.of("VPCVirtualRouter"), "UserData", List.of("VPCVirtualRouter"),
                "NetworkACL", List.of("Nsx"), "SourceNat", List.of("Nsx"), "StaticNat", List.of("Nsx"), "PortForwarding", List.of("Nsx")));
    }

    @Test
    public void nsxRoutedNetworkUsesNsxFirewallWithoutNatServices() {
        assertServiceProviders("NSX", "ROUTED", false, Map.of(
                "Dhcp", List.of("VirtualRouter"), "Dns", List.of("VirtualRouter"), "UserData", List.of("VirtualRouter"),
                "Firewall", List.of("Nsx")));
    }

    @Test
    public void nsxRoutedVpcTierUsesNsxAclWithoutNatServices() {
        assertServiceProviders("NSX", "ROUTED", true, Map.of(
                "Dhcp", List.of("VPCVirtualRouter"), "Dns", List.of("VPCVirtualRouter"), "UserData", List.of("VPCVirtualRouter"),
                "NetworkACL", List.of("Nsx")));
    }

    @Test
    public void netrisNattedNetworkKeepsNetrisNatAndLoadBalancerProviders() {
        assertServiceProviders("Netris", "NATTED", false, Map.of(
                "Dhcp", List.of("VirtualRouter"), "Dns", List.of("VirtualRouter"), "UserData", List.of("VirtualRouter"),
                "Firewall", List.of("Netris"), "SourceNat", List.of("Netris"), "StaticNat", List.of("Netris"),
                "PortForwarding", List.of("Netris"), "Lb", List.of("Netris"), "Vpn", List.of("VirtualRouter")));
    }

    @Test
    public void netrisRoutedNetworkKeepsOnlyRouterProviders() {
        assertServiceProviders("Netris", "ROUTED", false, Map.of(
                "Dhcp", List.of("VirtualRouter"), "Dns", List.of("VirtualRouter"), "UserData", List.of("VirtualRouter")));
    }

    private void assertServiceProviders(String provider, String networkMode, boolean forVpc, Map<String, List<String>> expected) {
        ReflectionTestUtils.setField(createNetworkOfferingCmd, "provider", provider);
        ReflectionTestUtils.setField(createNetworkOfferingCmd, "networkMode", networkMode);
        ReflectionTestUtils.setField(createNetworkOfferingCmd, "forVpc", forVpc);

        Map<String, List<String>> providers = createNetworkOfferingCmd.getServiceProviders();

        Assert.assertEquals(expected, providers);
        Assert.assertTrue(createNetworkOfferingCmd.getSupportedServices().containsAll(providers.keySet()));
    }
}
