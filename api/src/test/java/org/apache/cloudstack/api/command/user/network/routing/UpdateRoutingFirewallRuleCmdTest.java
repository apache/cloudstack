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
import com.cloud.network.firewall.FirewallService;
import com.cloud.network.rules.FirewallRule;

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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class UpdateRoutingFirewallRuleCmdTest {

    RoutedIpv4Manager routedIpv4Manager = Mockito.spy(RoutedIpv4Manager.class);

    FirewallService _firewallService = Mockito.spy(FirewallService.class);

    ResponseGenerator _responseGenerator = Mockito.spy(ResponseGenerator.class);

    @Test
    public void testIsDisplay() {
        UpdateRoutingFirewallRuleCmd cmd = new UpdateRoutingFirewallRuleCmd();
        assertTrue(cmd.isDisplay());

        ReflectionTestUtils.setField(cmd, "display", Boolean.TRUE);
        assertTrue(cmd.isDisplay());

        ReflectionTestUtils.setField(cmd, "display", Boolean.FALSE);
        assertFalse(cmd.isDisplay());
    }

    @Test
    public void testOtherProperties() {
        UpdateRoutingFirewallRuleCmd cmd = new UpdateRoutingFirewallRuleCmd();
        ReflectionTestUtils.setField(cmd, "_firewallService", _firewallService);

        long id = 1L;
        long accountId = 2L;
        long networkId = 3L;

        FirewallRule firewallRule = Mockito.spy(FirewallRule.class);
        Mockito.when(firewallRule.getAccountId()).thenReturn(accountId);
        Mockito.when(firewallRule.getNetworkId()).thenReturn(networkId);
        Mockito.when(_firewallService.getFirewallRule(id)).thenReturn(firewallRule);

        ReflectionTestUtils.setField(cmd, "id", id);
        assertEquals(id, (long) cmd.getId());
        assertEquals(accountId, cmd.getEntityOwnerId());
        assertEquals(networkId, (long) cmd.getApiResourceId());
        assertEquals(ApiCommandResourceType.Network, cmd.getApiResourceType());
        assertEquals(EventTypes.EVENT_ROUTING_IPV4_FIREWALL_RULE_UPDATE, cmd.getEventType());
        assertEquals("Updating ipv4 routing firewall rule", cmd.getEventDescription());
    }


    @Test
    public void testExecute() throws Exception {
        UpdateRoutingFirewallRuleCmd cmd = new UpdateRoutingFirewallRuleCmd();
        ReflectionTestUtils.setField(cmd, "routedIpv4Manager", routedIpv4Manager);
        ReflectionTestUtils.setField(cmd, "_firewallService", _firewallService);
        ReflectionTestUtils.setField(cmd, "_responseGenerator", _responseGenerator);

        Long id = 1L;
        FirewallRule firewallRule = Mockito.spy(FirewallRule.class);
        Mockito.when(routedIpv4Manager.updateRoutingFirewallRule(cmd)).thenReturn(firewallRule);

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
