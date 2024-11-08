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

package org.apache.cloudstack.api.command.user.network.routing;

import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.network.firewall.FirewallService;
import com.cloud.network.rules.FirewallRule;
import com.cloud.utils.net.NetUtils;

import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ResponseGenerator;
import org.apache.cloudstack.api.response.FirewallResponse;
import org.apache.cloudstack.network.RoutedIpv4Manager;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class CreateRoutingFirewallRuleCmdTest {

    RoutedIpv4Manager routedIpv4Manager = Mockito.spy(RoutedIpv4Manager.class);

    FirewallService _firewallService = Mockito.spy(FirewallService.class);

    ResponseGenerator _responseGenerator = Mockito.spy(ResponseGenerator.class);

    @Test
    public void testIsDisplay() {
        CreateRoutingFirewallRuleCmd cmd = new CreateRoutingFirewallRuleCmd();
        assertTrue(cmd.isDisplay());

        ReflectionTestUtils.setField(cmd, "display", Boolean.TRUE);
        assertTrue(cmd.isDisplay());

        ReflectionTestUtils.setField(cmd, "display", Boolean.FALSE);
        assertFalse(cmd.isDisplay());
    }

    @Test
    public void testGetProtocolValid() {
        CreateRoutingFirewallRuleCmd cmd = new CreateRoutingFirewallRuleCmd();
        assertEquals("", cmd.getProtocol());

        ReflectionTestUtils.setField(cmd, "protocol", "1");
        assertEquals(NetUtils.ICMP_PROTO, cmd.getProtocol());

        ReflectionTestUtils.setField(cmd, "protocol", "icmp");
        assertEquals(NetUtils.ICMP_PROTO, cmd.getProtocol());

        ReflectionTestUtils.setField(cmd, "protocol", "6");
        assertEquals(NetUtils.TCP_PROTO, cmd.getProtocol());

        ReflectionTestUtils.setField(cmd, "protocol", "tcp");
        assertEquals(NetUtils.TCP_PROTO, cmd.getProtocol());

        ReflectionTestUtils.setField(cmd, "protocol", "17");
        assertEquals(NetUtils.UDP_PROTO, cmd.getProtocol());

        ReflectionTestUtils.setField(cmd, "protocol", "udp");
        assertEquals(NetUtils.UDP_PROTO, cmd.getProtocol());
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testGetProtocolInValid() {
        CreateRoutingFirewallRuleCmd cmd = new CreateRoutingFirewallRuleCmd();

        ReflectionTestUtils.setField(cmd, "protocol", "100");
        cmd.getProtocol();
    }

    @Test
    public void testGetSourceCidrListNull() {
        CreateRoutingFirewallRuleCmd cmd = new CreateRoutingFirewallRuleCmd();

        List<String> result = cmd.getSourceCidrList();
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(NetUtils.ALL_IP4_CIDRS, result.get(0));
    }

    @Test
    public void testGetSourceCidrList() {
        CreateRoutingFirewallRuleCmd cmd = new CreateRoutingFirewallRuleCmd();

        List<String> cidrList = Arrays.asList("192.168.0.0/24", "10.0.0.0/8");
        cmd.sourceCidrList = cidrList;
        List<String> result = cmd.getSourceCidrList();
        assertNotNull(result);
        assertEquals(cidrList, result);
    }

    @Test
    public void testGetDestinationCidrListNull() {
        CreateRoutingFirewallRuleCmd cmd = new CreateRoutingFirewallRuleCmd();

        List<String> result = cmd.getDestinationCidrList();
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(NetUtils.ALL_IP4_CIDRS, result.get(0));
    }

    @Test
    public void testGetDestinationCidrList() {
        CreateRoutingFirewallRuleCmd cmd = new CreateRoutingFirewallRuleCmd();

        List<String> cidrList = Arrays.asList("192.168.0.0/24", "10.0.0.0/8");
        cmd.destinationCidrlist = cidrList;
        List<String> result = cmd.getDestinationCidrList();
        assertNotNull(result);
        assertEquals(cidrList, result);
    }

    @Test
    public void testGetTrafficTypeValid() {
        CreateRoutingFirewallRuleCmd cmd = new CreateRoutingFirewallRuleCmd();
        assertEquals(FirewallRule.TrafficType.Ingress, cmd.getTrafficType());

        ReflectionTestUtils.setField(cmd, "trafficType", "ingress");
        assertEquals(FirewallRule.TrafficType.Ingress, cmd.getTrafficType());

        ReflectionTestUtils.setField(cmd, "trafficType", "egress");
        assertEquals(FirewallRule.TrafficType.Egress, cmd.getTrafficType());
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testGetTrafficTypeInValid() {
        CreateRoutingFirewallRuleCmd cmd = new CreateRoutingFirewallRuleCmd();

        ReflectionTestUtils.setField(cmd, "trafficType", "invalid");
        cmd.getTrafficType();
    }

    @Test
    public void testSourcePortStartEnd() {
        CreateRoutingFirewallRuleCmd cmd = new CreateRoutingFirewallRuleCmd();
        assertNull(cmd.getSourcePortStart());
        assertNull(cmd.getSourcePortEnd());

        ReflectionTestUtils.setField(cmd, "publicStartPort", 1111);
        assertEquals(1111, (int) cmd.getSourcePortStart());
        assertEquals(1111, (int) cmd.getSourcePortEnd());

        ReflectionTestUtils.setField(cmd, "publicEndPort", 2222);
        assertEquals(1111, (int) cmd.getSourcePortStart());
        assertEquals(2222, (int) cmd.getSourcePortEnd());
    }

    @Test
    public void testNetworkId() {
        CreateRoutingFirewallRuleCmd cmd = new CreateRoutingFirewallRuleCmd();

        ReflectionTestUtils.setField(cmd, "networkId", 1111L);
        assertEquals(1111L, (long) cmd.getNetworkId());

        assertEquals(1111L, (long) cmd.getApiResourceId());
        assertEquals(ApiCommandResourceType.Network, cmd.getApiResourceType());
        assertEquals(EventTypes.EVENT_ROUTING_IPV4_FIREWALL_RULE_CREATE, cmd.getEventType());
        assertEquals("Creating ipv4 firewall rule for routed network", cmd.getEventDescription());
    }

    @Test
    public void testIcmpCodeAndType() {
        CreateRoutingFirewallRuleCmd cmd = new CreateRoutingFirewallRuleCmd();
        ReflectionTestUtils.setField(cmd, "protocol", "tcp");
        assertNull(cmd.getIcmpType());
        assertNull(cmd.getIcmpCode());

        ReflectionTestUtils.setField(cmd, "protocol", "icmp");
        assertEquals(-1, (int) cmd.getIcmpType());
        assertEquals(-1, (int) cmd.getIcmpCode());

        ReflectionTestUtils.setField(cmd, "icmpType", 1111);
        ReflectionTestUtils.setField(cmd, "icmpCode", 2222);
        assertEquals(1111, (int) cmd.getIcmpType());
        assertEquals(2222, (int) cmd.getIcmpCode());
    }

    @Test
    public void testCreate() throws Exception {
        CreateRoutingFirewallRuleCmd cmd = new CreateRoutingFirewallRuleCmd();
        ReflectionTestUtils.setField(cmd, "routedIpv4Manager", routedIpv4Manager);

        Long id = 1L;
        String uuid = "uuid";
        FirewallRule firewallRule = Mockito.spy(FirewallRule.class);
        Mockito.when(firewallRule.getId()).thenReturn(id);
        Mockito.when(firewallRule.getUuid()).thenReturn(uuid);
        Mockito.when(routedIpv4Manager.createRoutingFirewallRule(cmd)).thenReturn(firewallRule);

        try {
            cmd.create();
        } catch (Exception ignored) {
        }

        assertEquals(id, cmd.getEntityId());
        assertEquals(uuid, cmd.getEntityUuid());
    }

    @Test
    public void testExecute() throws Exception {
        CreateRoutingFirewallRuleCmd cmd = new CreateRoutingFirewallRuleCmd();
        ReflectionTestUtils.setField(cmd, "routedIpv4Manager", routedIpv4Manager);
        ReflectionTestUtils.setField(cmd, "_firewallService", _firewallService);
        ReflectionTestUtils.setField(cmd, "_responseGenerator", _responseGenerator);

        Long id = 1L;
        FirewallRule firewallRule = Mockito.spy(FirewallRule.class);
        Mockito.when(firewallRule.getId()).thenReturn(id);
        Mockito.when(_firewallService.getFirewallRule(id)).thenReturn(firewallRule);
        Mockito.when(routedIpv4Manager.applyRoutingFirewallRule(id)).thenReturn(true);

        FirewallResponse ruleResponse = Mockito.mock(FirewallResponse.class);
        Mockito.when(_responseGenerator.createFirewallResponse(firewallRule)).thenReturn(ruleResponse);

        try {
            ReflectionTestUtils.setField(cmd, "id", id);
            cmd.execute();
        } catch (Exception ignored) {
        }

        Assert.assertEquals(ruleResponse, cmd.getResponseObject());
    }
}
