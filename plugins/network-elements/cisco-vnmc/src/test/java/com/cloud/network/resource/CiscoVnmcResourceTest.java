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
package com.cloud.network.resource;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.ConfigurationException;

import org.junit.Before;
import org.junit.Test;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CreateLogicalEdgeFirewallCommand;
import com.cloud.agent.api.PingCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.routing.NetworkElementCommand;
import com.cloud.agent.api.routing.SetFirewallRulesCommand;
import com.cloud.agent.api.routing.SetPortForwardingRulesCommand;
import com.cloud.agent.api.routing.SetSourceNatCommand;
import com.cloud.agent.api.routing.SetStaticNatRulesCommand;
import com.cloud.agent.api.to.FirewallRuleTO;
import com.cloud.agent.api.to.IpAddressTO;
import com.cloud.agent.api.to.PortForwardingRuleTO;
import com.cloud.agent.api.to.StaticNatRuleTO;
import com.cloud.host.Host;
import com.cloud.network.cisco.CiscoVnmcConnectionImpl;
import com.cloud.network.rules.FirewallRule;
import com.cloud.utils.exception.ExecutionException;

public class CiscoVnmcResourceTest {
    CiscoVnmcConnectionImpl _connection = mock(CiscoVnmcConnectionImpl.class);
    CiscoVnmcResource _resource;
    Map<String, Object> _parameters;

    @Before
    public void setUp() throws ConfigurationException {
        _resource = new CiscoVnmcResource();

        _parameters = new HashMap<String, Object>();
        _parameters.put("name", "CiscoVnmc");
        _parameters.put("zoneId", "1");
        _parameters.put("physicalNetworkId", "100");
        _parameters.put("ip", "1.2.3.4");
        _parameters.put("username", "admin");
        _parameters.put("password", "pass");
        _parameters.put("guid", "e8e13097-0a08-4e82-b0af-1101589ec3b8");
        _parameters.put("numretries", "3");
        _parameters.put("timeout", "300");
    }

    //@Test(expected=ConfigurationException.class)
    public void resourceConfigureFailure() throws ConfigurationException {
        _resource.configure("CiscoVnmcResource", Collections.<String, Object> emptyMap());
    }

    //@Test
    public void resourceConfigure() throws ConfigurationException {
        _resource.configure("CiscoVnmcResource", _parameters);
        assertTrue("CiscoVnmc".equals(_resource.getName()));
        assertTrue(_resource.getType() == Host.Type.ExternalFirewall);
    }

    //@Test
    public void testInitialization() throws ConfigurationException {
        _resource.configure("CiscoVnmcResource", _parameters);
        StartupCommand[] sc = _resource.initialize();
        assertTrue(sc.length == 1);
        assertTrue("e8e13097-0a08-4e82-b0af-1101589ec3b8".equals(sc[0].getGuid()));
        assertTrue("CiscoVnmc".equals(sc[0].getName()));
        assertTrue("1".equals(sc[0].getDataCenter()));
    }

    @Test
    public void testPingCommandStatusOk() throws ConfigurationException, ExecutionException {
        _resource.setConnection(_connection);
        when(_connection.login()).thenReturn(true);
        PingCommand ping = _resource.getCurrentStatus(1);
        assertTrue(ping != null);
        assertTrue(ping.getHostId() == 1);
        assertTrue(ping.getHostType() == Host.Type.ExternalFirewall);
    }

    @Test
    public void testPingCommandStatusFail() throws ConfigurationException, ExecutionException {
        _resource.setConnection(_connection);
        when(_connection.login()).thenReturn(false);
        PingCommand ping = _resource.getCurrentStatus(1);
        assertTrue(ping == null);
    }

    @Test
    public void testSourceNat() throws ConfigurationException, Exception {
        long vlanId = 123;
        IpAddressTO ip = new IpAddressTO(1, "1.2.3.4", true, false, false, null, "1.2.3.1", "255.255.255.0", null, null, false);
        SetSourceNatCommand cmd = new SetSourceNatCommand(ip, true);
        cmd.setContextParam(NetworkElementCommand.GUEST_VLAN_TAG, Long.toString(vlanId));
        cmd.setContextParam(NetworkElementCommand.GUEST_NETWORK_CIDR, "1.2.3.4/32");

        _resource.setConnection(_connection);
        when(_connection.login()).thenReturn(true);
        when(_connection.createTenantVDCNatPolicySet(anyString())).thenReturn(true);
        when(_connection.createTenantVDCSourceNatPolicy(anyString(), anyString())).thenReturn(true);
        when(_connection.createTenantVDCSourceNatPolicyRef(anyString(), anyString())).thenReturn(true);
        when(_connection.createTenantVDCSourceNatIpPool(anyString(), anyString(), anyString())).thenReturn(true);
        when(_connection.createTenantVDCSourceNatRule(anyString(), anyString(), anyString(), anyString())).thenReturn(true);
        when(_connection.associateNatPolicySet(anyString())).thenReturn(true);

        Answer answer = _resource.executeRequest(cmd);
        System.out.println(answer.getDetails());
        assertTrue(answer.getResult());
    }

    @Test
    public void testFirewall() throws ConfigurationException, Exception {
        long vlanId = 123;
        List<FirewallRuleTO> rules = new ArrayList<FirewallRuleTO>();
        List<String> cidrList = new ArrayList<String>();
        cidrList.add("2.3.2.3/32");
        FirewallRuleTO active = new FirewallRuleTO(1, null, "1.2.3.4", "tcp", 22, 22, false, false, FirewallRule.Purpose.Firewall, cidrList, null, null);
        rules.add(active);
        FirewallRuleTO revoked = new FirewallRuleTO(1, null, "1.2.3.4", "tcp", 22, 22, true, false, FirewallRule.Purpose.Firewall, null, null, null);
        rules.add(revoked);

        SetFirewallRulesCommand cmd = new SetFirewallRulesCommand(rules);
        cmd.setContextParam(NetworkElementCommand.GUEST_VLAN_TAG, Long.toString(vlanId));
        cmd.setContextParam(NetworkElementCommand.GUEST_NETWORK_CIDR, "1.2.3.4/32");

        _resource.setConnection(_connection);
        when(_connection.createTenantVDCAclPolicySet(anyString(), anyBoolean())).thenReturn(true);
        when(_connection.createTenantVDCAclPolicy(anyString(), anyString())).thenReturn(true);
        when(_connection.createTenantVDCAclPolicyRef(anyString(), anyString(), anyBoolean())).thenReturn(true);
        when(_connection.deleteTenantVDCAclRule(anyString(), anyLong(), anyString())).thenReturn(true);
        when(_connection.createTenantVDCIngressAclRule(anyString(), anyLong(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn(
            true);
        when(_connection.createTenantVDCEgressAclRule(anyString(), anyLong(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn(
            true);
        when(_connection.associateAclPolicySet(anyString())).thenReturn(true);

        Answer answer = _resource.executeRequest(cmd);
        System.out.println(answer.getDetails());
        assertTrue(answer.getResult());
    }

    @Test
    public void testStaticNat() throws ConfigurationException, Exception {
        long vlanId = 123;
        List<StaticNatRuleTO> rules = new ArrayList<StaticNatRuleTO>();
        StaticNatRuleTO active = new StaticNatRuleTO(0, "1.2.3.4", null, null, "5.6.7.8", null, null, null, false, false);
        rules.add(active);
        StaticNatRuleTO revoked = new StaticNatRuleTO(0, "1.2.3.4", null, null, "5.6.7.8", null, null, null, true, false);
        rules.add(revoked);

        SetStaticNatRulesCommand cmd = new SetStaticNatRulesCommand(rules, null);
        cmd.setContextParam(NetworkElementCommand.GUEST_VLAN_TAG, Long.toString(vlanId));
        cmd.setContextParam(NetworkElementCommand.GUEST_NETWORK_CIDR, "1.2.3.4/32");

        _resource.setConnection(_connection);
        when(_connection.createTenantVDCNatPolicySet(anyString())).thenReturn(true);
        when(_connection.createTenantVDCAclPolicySet(anyString(), anyBoolean())).thenReturn(true);
        when(_connection.createTenantVDCDNatPolicy(anyString(), anyString())).thenReturn(true);
        when(_connection.createTenantVDCDNatPolicyRef(anyString(), anyString())).thenReturn(true);
        when(_connection.createTenantVDCAclPolicy(anyString(), anyString())).thenReturn(true);
        when(_connection.createTenantVDCAclPolicyRef(anyString(), anyString(), anyBoolean())).thenReturn(true);
        when(_connection.deleteTenantVDCDNatRule(anyString(), anyLong(), anyString())).thenReturn(true);
        when(_connection.deleteTenantVDCAclRule(anyString(), anyLong(), anyString())).thenReturn(true);
        when(_connection.createTenantVDCDNatIpPool(anyString(), anyString(), anyString())).thenReturn(true);
        when(_connection.createTenantVDCDNatRule(anyString(), anyLong(), anyString(), anyString())).thenReturn(true);
        when(_connection.createTenantVDCAclRuleForDNat(anyString(), anyLong(), anyString(), anyString())).thenReturn(true);
        when(_connection.associateAclPolicySet(anyString())).thenReturn(true);

        Answer answer = _resource.executeRequest(cmd);
        System.out.println(answer.getDetails());
        assertTrue(answer.getResult());
    }

    @Test
    public void testPortForwarding() throws ConfigurationException, Exception {
        long vlanId = 123;
        List<PortForwardingRuleTO> rules = new ArrayList<PortForwardingRuleTO>();
        PortForwardingRuleTO active = new PortForwardingRuleTO(1, "1.2.3.4", 22, 22, "5.6.7.8", 22, 22, "tcp", false, false);
        rules.add(active);
        PortForwardingRuleTO revoked = new PortForwardingRuleTO(1, "1.2.3.4", 22, 22, "5.6.7.8", 22, 22, "tcp", false, false);
        rules.add(revoked);

        SetPortForwardingRulesCommand cmd = new SetPortForwardingRulesCommand(rules);
        cmd.setContextParam(NetworkElementCommand.GUEST_VLAN_TAG, Long.toString(vlanId));
        cmd.setContextParam(NetworkElementCommand.GUEST_NETWORK_CIDR, "1.2.3.4/32");

        _resource.setConnection(_connection);
        when(_connection.createTenantVDCNatPolicySet(anyString())).thenReturn(true);
        when(_connection.createTenantVDCAclPolicySet(anyString(), anyBoolean())).thenReturn(true);
        when(_connection.createTenantVDCPFPolicy(anyString(), anyString())).thenReturn(true);
        when(_connection.createTenantVDCPFPolicyRef(anyString(), anyString())).thenReturn(true);
        when(_connection.createTenantVDCAclPolicy(anyString(), anyString())).thenReturn(true);
        when(_connection.createTenantVDCAclPolicyRef(anyString(), anyString(), anyBoolean())).thenReturn(true);
        when(_connection.deleteTenantVDCPFRule(anyString(), anyLong(), anyString())).thenReturn(true);
        when(_connection.deleteTenantVDCAclRule(anyString(), anyLong(), anyString())).thenReturn(true);
        when(_connection.createTenantVDCPFIpPool(anyString(), anyString(), anyString())).thenReturn(true);
        when(_connection.createTenantVDCPFPortPool(anyString(), anyString(), anyString(), anyString())).thenReturn(true);
        when(_connection.createTenantVDCPFRule(anyString(), anyLong(), anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn(true);
        when(_connection.createTenantVDCAclRuleForPF(anyString(), anyLong(), anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn(true);
        when(_connection.associateAclPolicySet(anyString())).thenReturn(true);

        Answer answer = _resource.executeRequest(cmd);
        System.out.println(answer.getDetails());
        assertTrue(answer.getResult());
    }

    @Test
    public void testCreateEdgeFirewall() throws ConfigurationException, Exception {
        long vlanId = 123;
        CreateLogicalEdgeFirewallCommand cmd = new CreateLogicalEdgeFirewallCommand(vlanId, "1.2.3.4", "5.6.7.8", "255.255.255.0", "255.255.255.0");
        cmd.getPublicGateways().add("1.1.1.1");
        cmd.getPublicGateways().add("2.2.2.2");

        _resource.setConnection(_connection);
        when(_connection.createTenant(anyString())).thenReturn(true);
        when(_connection.createTenantVDC(anyString())).thenReturn(true);
        when(_connection.createTenantVDCEdgeSecurityProfile(anyString())).thenReturn(true);
        when(_connection.createTenantVDCEdgeDeviceProfile(anyString())).thenReturn(true);
        when(_connection.createTenantVDCEdgeStaticRoutePolicy(anyString())).thenReturn(true);
        when(_connection.createTenantVDCEdgeStaticRoute(anyString(), anyString(), anyString(), anyString())).thenReturn(true);
        when(_connection.associateTenantVDCEdgeStaticRoutePolicy(anyString())).thenReturn(true);
        when(_connection.createEdgeFirewall(anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn(true);

        Answer answer = _resource.executeRequest(cmd);
        System.out.println(answer.getDetails());
        assertTrue(answer.getResult());
    }
}
