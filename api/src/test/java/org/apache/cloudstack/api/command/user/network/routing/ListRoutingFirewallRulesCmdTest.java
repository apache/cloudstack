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

import com.cloud.network.rules.FirewallRule;
import com.cloud.utils.Pair;

import org.apache.cloudstack.api.ResponseGenerator;
import org.apache.cloudstack.api.response.FirewallResponse;
import org.apache.cloudstack.api.response.ListResponse;
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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class ListRoutingFirewallRulesCmdTest {

    RoutedIpv4Manager routedIpv4Manager = Mockito.spy(RoutedIpv4Manager.class);

    ResponseGenerator _responseGenerator = Mockito.spy(ResponseGenerator.class);

    @Test
    public void testIsDisplay() {
        ListRoutingFirewallRulesCmd cmd = new ListRoutingFirewallRulesCmd();
        assertTrue(cmd.getDisplay());

        ReflectionTestUtils.setField(cmd, "display", Boolean.TRUE);
        assertTrue(cmd.getDisplay());

        ReflectionTestUtils.setField(cmd, "display", Boolean.FALSE);
        assertFalse(cmd.getDisplay());
    }

    @Test
    public void testTrafficType() {
        ListRoutingFirewallRulesCmd cmd = new ListRoutingFirewallRulesCmd();
        assertNull(cmd.getTrafficType());

        ReflectionTestUtils.setField(cmd, "trafficType", "Ingress");
        assertEquals(FirewallRule.TrafficType.Ingress, cmd.getTrafficType());

        ReflectionTestUtils.setField(cmd, "trafficType", "Egress");
        assertEquals(FirewallRule.TrafficType.Egress, cmd.getTrafficType());
    }

    @Test
    public void testOtherProperties() {
        ListRoutingFirewallRulesCmd cmd = new ListRoutingFirewallRulesCmd();

        long id = 1L;
        long networkId = 3L;

        ReflectionTestUtils.setField(cmd, "id", id);
        ReflectionTestUtils.setField(cmd, "networkId", networkId);

        assertEquals(id, (long) cmd.getId());
        assertEquals(networkId, (long) cmd.getNetworkId());
        assertNull(cmd.getIpAddressId());
    }


    @Test
    public void testExecute() throws Exception {
        ListRoutingFirewallRulesCmd cmd = new ListRoutingFirewallRulesCmd();
        ReflectionTestUtils.setField(cmd, "routedIpv4Manager", routedIpv4Manager);
        ReflectionTestUtils.setField(cmd, "_responseGenerator", _responseGenerator);

        Long id = 1L;
        FirewallRule firewallRule = Mockito.spy(FirewallRule.class);
        List<FirewallRule> firewallRules = Arrays.asList(firewallRule);
        Pair<List<? extends FirewallRule>, Integer> result = new Pair<>(firewallRules, 1);

        Mockito.when(routedIpv4Manager.listRoutingFirewallRules(cmd)).thenReturn(result);

        FirewallResponse ruleResponse = Mockito.mock(FirewallResponse.class);
        Mockito.when(_responseGenerator.createFirewallResponse(firewallRule)).thenReturn(ruleResponse);

        try {
            cmd.execute();
        } catch (Exception ignored) {
        }

        Assert.assertTrue(cmd.getResponseObject() instanceof ListResponse);
        ListResponse listResponse = (ListResponse) cmd.getResponseObject();
        Assert.assertEquals(1, (int) listResponse.getCount());
        Assert.assertEquals(ruleResponse, listResponse.getResponses().get(0));
    }
}
