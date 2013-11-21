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

// test imports
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.ConfigurationException;

import org.junit.Before;
import org.junit.Test;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.routing.IpAssocAnswer;
import com.cloud.agent.api.routing.IpAssocCommand;
import com.cloud.agent.api.routing.NetworkElementCommand;
import com.cloud.agent.api.routing.SetFirewallRulesCommand;
import com.cloud.agent.api.routing.SetPortForwardingRulesCommand;
import com.cloud.agent.api.routing.SetStaticNatRulesCommand;
import com.cloud.agent.api.to.FirewallRuleTO;
import com.cloud.agent.api.to.IpAddressTO;
import com.cloud.agent.api.to.PortForwardingRuleTO;
import com.cloud.agent.api.to.StaticNatRuleTO;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.FirewallRule.Purpose;
import com.cloud.network.rules.FirewallRule.State;
import com.cloud.network.rules.FirewallRuleVO;
import com.cloud.utils.exception.ExecutionException;
// basic imports
// http client handling
// for prettyFormat()

public class PaloAltoResourceTest {
    // configuration data
    private String _test_name = "PaloAltoTestDevice";
    private String _test_zoneId = "TestZone";
    private String _test_ip = "192.168.80.2";
    private String _test_username = "admin";
    private String _test_password = "admin";
    private String _test_publicInterface = "ethernet1/1";
    private String _test_privateInterface = "ethernet1/2";
    private String _test_publicZone = "untrust";
    private String _test_privateZone = "trust";
    private String _test_virtualRouter = "default";

    MockablePaloAltoResource _resource;
    Map<String, Object> _resource_params;
    HashMap<String, String> _context;

    @Before
    public void setUp() {
        _resource = new MockablePaloAltoResource();
        _resource_params = new HashMap<String, Object>(); // params to be passed to configure()
        _resource_params.put("name", _test_name);
        _resource_params.put("zoneId", _test_zoneId);
        _resource_params.put("ip", _test_ip);
        _resource_params.put("username", _test_username);
        _resource_params.put("password", _test_password);
        _resource_params.put("publicinterface", _test_publicInterface);
        _resource_params.put("privateinterface", _test_privateInterface);
        _resource_params.put("publicnetwork", _test_publicZone);
        _resource_params.put("privatenetwork", _test_privateZone);
        _resource_params.put("pavr", _test_virtualRouter);
        _resource_params.put("guid", "aaaaa-bbbbb-ccccc");

        _context = new HashMap<String, String>(); // global context
        _context.put("name", _test_name);
        _context.put("zone_id", _test_zoneId);
        _context.put("ip", _test_ip);
        _context.put("username", _test_username);
        _context.put("password", _test_password);
        _context.put("public_interface", _test_publicInterface);
        _context.put("private_interface", _test_privateInterface);
        _context.put("public_zone", _test_publicZone);
        _context.put("private_zone", _test_privateZone);
        _context.put("pa_vr", _test_virtualRouter);
        // --
        _context.put("public_using_ethernet", "true");
        _context.put("private_using_ethernet", "true");
        _context.put("has_management_profile", "true");
        _context.put("enable_console_output", "false"); // CHANGE TO "true" TO ENABLE CONSOLE LOGGING OF TESTS
        _resource.setMockContext(_context);
    }

    @Test(expected = ConfigurationException.class)
    public void resourceConfigureFailure() throws ConfigurationException {
        _resource.configure("PaloAltoResource", new HashMap<String, Object>());
    }

    @Test
    public void resourceConfigureWithoutManagementProfile() throws ConfigurationException {
        if (_context.containsKey("enable_console_output") && _context.get("enable_console_output").equals("true")) {
            System.out.println("\nTEST: resourceConfigureWithoutManagementProfile");
            System.out.println("---------------------------------------------------");
        }
        _context.remove("has_management_profile");
        _resource.setMockContext(_context);
        _resource.configure("PaloAltoResource", _resource_params);
    }

    @Test
    public void resourceConfigureWithManagementProfile() throws ConfigurationException {
        if (_context.containsKey("enable_console_output") && _context.get("enable_console_output").equals("true")) {
            System.out.println("\nTEST: resourceConfigureWithManagementProfile");
            System.out.println("---------------------------------------------------");
        }
        _resource.configure("PaloAltoResource", _resource_params);
    }

    @Test(expected = ConfigurationException.class)
    public void simulateFirewallNotConfigurable() throws ConfigurationException {
        if (_context.containsKey("enable_console_output") && _context.get("enable_console_output").equals("true")) {
            System.out.println("\nTEST: simulateFirewallNotConfigurable");
            System.out.println("---------------------------------------------------");
        }
        _context.put("firewall_has_pending_changes", "true");
        _context.remove("has_management_profile");
        _resource.setMockContext(_context);
        _resource.configure("PaloAltoResource", _resource_params);
    }

    @Test(expected = ConfigurationException.class)
    public void simulateFirewallCommitFailure() throws ConfigurationException {
        if (_context.containsKey("enable_console_output") && _context.get("enable_console_output").equals("true")) {
            System.out.println("\nTEST: simulateFirewallCommitFailure");
            System.out.println("---------------------------------------------------");
        }
        _context.put("simulate_commit_failure", "true");
        _context.remove("has_management_profile");
        _resource.setMockContext(_context);
        _resource.configure("PaloAltoResource", _resource_params);
    }

    @Test
    public void testInitialize() throws ConfigurationException {
        if (_context.containsKey("enable_console_output") && _context.get("enable_console_output").equals("true")) {
            System.out.println("\nTEST: testInitialization");
            System.out.println("---------------------------------------------------");
        }
        _resource.configure("PaloAltoResource", _resource_params);

        StartupCommand[] sc = _resource.initialize();
        assertTrue(sc.length == 1);
        assertTrue("aaaaa-bbbbb-ccccc".equals(sc[0].getGuid()));
        assertTrue("PaloAltoTestDevice".equals(sc[0].getName()));
        assertTrue("TestZone".equals(sc[0].getDataCenter()));
    }

    @Test
    // implement public & private interfaces, source nat, guest network
        public
        void implementGuestNetwork() throws ConfigurationException, ExecutionException {
        if (_context.containsKey("enable_console_output") && _context.get("enable_console_output").equals("true")) {
            System.out.println("\nTEST: implementGuestNetwork");
            System.out.println("---------------------------------------------------");
        }
        _resource.configure("PaloAltoResource", _resource_params);

        IpAddressTO ip = new IpAddressTO(Long.valueOf("1"), "192.168.80.102", true, false, true, "untagged", null, null, null, 100, false);
        IpAddressTO[] ips = new IpAddressTO[1];
        ips[0] = ip;
        IpAssocCommand cmd = new IpAssocCommand(ips);
        cmd.setAccessDetail(NetworkElementCommand.GUEST_NETWORK_GATEWAY, "10.3.96.1");
        cmd.setAccessDetail(NetworkElementCommand.GUEST_NETWORK_CIDR, "10.3.96.1/20");
        cmd.setAccessDetail(NetworkElementCommand.GUEST_VLAN_TAG, "3954");

        IpAssocAnswer answer = (IpAssocAnswer)_resource.executeRequest(cmd);
        assertTrue(answer.getResult());
    }

    @Test
    // remove public & private interface details, source nat, guest network
        public
        void shutdownGuestNetwork() throws ConfigurationException, ExecutionException {
        if (_context.containsKey("enable_console_output") && _context.get("enable_console_output").equals("true")) {
            System.out.println("\nTEST: shutdownGuestNetwork");
            System.out.println("---------------------------------------------------");
        }
        _context.put("has_public_interface", "true");
        _context.put("has_private_interface", "true");
        _context.put("has_src_nat_rule", "true");
        _context.put("has_isolation_fw_rule", "true");
        _resource.setMockContext(_context);
        _resource.configure("PaloAltoResource", _resource_params);

        IpAddressTO ip = new IpAddressTO(Long.valueOf("1"), "192.168.80.102", false, false, true, "untagged", null, null, null, 100, false);
        IpAddressTO[] ips = new IpAddressTO[1];
        ips[0] = ip;
        IpAssocCommand cmd = new IpAssocCommand(ips);
        cmd.setAccessDetail(NetworkElementCommand.GUEST_NETWORK_GATEWAY, "10.3.96.1");
        cmd.setAccessDetail(NetworkElementCommand.GUEST_NETWORK_CIDR, "10.3.96.1/20");
        cmd.setAccessDetail(NetworkElementCommand.GUEST_VLAN_TAG, "3954");

        IpAssocAnswer answer = (IpAssocAnswer)_resource.executeRequest(cmd);
        assertTrue(answer.getResult());
    }

    @Test
    public void addIngressFirewallRule() throws ConfigurationException, Exception {
        if (_context.containsKey("enable_console_output") && _context.get("enable_console_output").equals("true")) {
            System.out.println("\nTEST: addIngressFirewallRule");
            System.out.println("---------------------------------------------------");
        }
        _context.put("has_public_interface", "true");
        _context.put("has_private_interface", "true");
        _context.put("has_src_nat_rule", "true");
        _context.put("has_isolation_fw_rule", "true");
        _context.put("has_service_tcp_80", "true");
        _resource.setMockContext(_context);
        _resource.configure("PaloAltoResource", _resource_params);

        long vlanId = 3954;
        List<FirewallRuleTO> rules = new ArrayList<FirewallRuleTO>();
        List<String> cidrList = new ArrayList<String>();
        cidrList.add("0.0.0.0/0");
        FirewallRuleTO active = new FirewallRuleTO(8, null, "192.168.80.103", "tcp", 80, 80, false, false, FirewallRule.Purpose.Firewall, cidrList, null, null);
        rules.add(active);

        SetFirewallRulesCommand cmd = new SetFirewallRulesCommand(rules);
        cmd.setContextParam(NetworkElementCommand.GUEST_VLAN_TAG, Long.toString(vlanId));
        cmd.setContextParam(NetworkElementCommand.GUEST_NETWORK_CIDR, "10.3.96.1/20");

        Answer answer = _resource.executeRequest(cmd);
        assertTrue(answer.getResult());
    }

    @Test
    public void removeIngressFirewallRule() throws ConfigurationException, Exception {
        if (_context.containsKey("enable_console_output") && _context.get("enable_console_output").equals("true")) {
            System.out.println("\nTEST: removeIngressFirewallRule");
            System.out.println("---------------------------------------------------");
        }
        _context.put("has_public_interface", "true");
        _context.put("has_private_interface", "true");
        _context.put("has_src_nat_rule", "true");
        _context.put("has_isolation_fw_rule", "true");
        _context.put("has_service_tcp_80", "true");
        _context.put("has_ingress_fw_rule", "true");
        _resource.setMockContext(_context);
        _resource.configure("PaloAltoResource", _resource_params);

        long vlanId = 3954;
        List<FirewallRuleTO> rules = new ArrayList<FirewallRuleTO>();
        FirewallRuleTO revoked = new FirewallRuleTO(8, null, "192.168.80.103", "tcp", 80, 80, true, false, FirewallRule.Purpose.Firewall, null, null, null);
        rules.add(revoked);

        SetFirewallRulesCommand cmd = new SetFirewallRulesCommand(rules);
        cmd.setContextParam(NetworkElementCommand.GUEST_VLAN_TAG, Long.toString(vlanId));
        cmd.setContextParam(NetworkElementCommand.GUEST_NETWORK_CIDR, "10.3.96.1/20");

        Answer answer = _resource.executeRequest(cmd);
        assertTrue(answer.getResult());
    }

    @Test
    public void addEgressFirewallRule() throws ConfigurationException, Exception {
        if (_context.containsKey("enable_console_output") && _context.get("enable_console_output").equals("true")) {
            System.out.println("\nTEST: addEgressFirewallRule");
            System.out.println("---------------------------------------------------");
        }
        _context.put("has_public_interface", "true");
        _context.put("has_private_interface", "true");
        _context.put("has_src_nat_rule", "true");
        _context.put("has_isolation_fw_rule", "true");
        _context.put("has_service_tcp_80", "true");
        _resource.setMockContext(_context);
        _resource.configure("PaloAltoResource", _resource_params);

        long vlanId = 3954;
        List<FirewallRuleTO> rules = new ArrayList<FirewallRuleTO>();
        List<String> cidrList = new ArrayList<String>();
        cidrList.add("0.0.0.0/0");
        FirewallRuleVO activeVO = new FirewallRuleVO(null, null, 80, 80, "tcp", 1, 1, 1, Purpose.Firewall, cidrList, null, null, null, FirewallRule.TrafficType.Egress);
        FirewallRuleTO active = new FirewallRuleTO(activeVO, Long.toString(vlanId), null, Purpose.Firewall, FirewallRule.TrafficType.Egress);
        rules.add(active);

        SetFirewallRulesCommand cmd = new SetFirewallRulesCommand(rules);
        cmd.setContextParam(NetworkElementCommand.GUEST_VLAN_TAG, Long.toString(vlanId));
        cmd.setContextParam(NetworkElementCommand.GUEST_NETWORK_CIDR, "10.3.96.1/20");

        Answer answer = _resource.executeRequest(cmd);
        assertTrue(answer.getResult());
    }

    @Test
    public void removeEgressFirewallRule() throws ConfigurationException, Exception {
        if (_context.containsKey("enable_console_output") && _context.get("enable_console_output").equals("true")) {
            System.out.println("\nTEST: removeEgressFirewallRule");
            System.out.println("---------------------------------------------------");
        }
        _context.put("has_public_interface", "true");
        _context.put("has_private_interface", "true");
        _context.put("has_src_nat_rule", "true");
        _context.put("has_isolation_fw_rule", "true");
        _context.put("has_service_tcp_80", "true");
        _context.put("has_egress_fw_rule", "true");
        _resource.setMockContext(_context);
        _resource.configure("PaloAltoResource", _resource_params);

        long vlanId = 3954;
        List<FirewallRuleTO> rules = new ArrayList<FirewallRuleTO>();
        FirewallRuleVO revokedVO = new FirewallRuleVO(null, null, 80, 80, "tcp", 1, 1, 1, Purpose.Firewall, null, null, null, null, FirewallRule.TrafficType.Egress);
        revokedVO.setState(State.Revoke);
        FirewallRuleTO revoked = new FirewallRuleTO(revokedVO, Long.toString(vlanId), null, Purpose.Firewall, FirewallRule.TrafficType.Egress);
        rules.add(revoked);

        SetFirewallRulesCommand cmd = new SetFirewallRulesCommand(rules);
        cmd.setContextParam(NetworkElementCommand.GUEST_VLAN_TAG, Long.toString(vlanId));
        cmd.setContextParam(NetworkElementCommand.GUEST_NETWORK_CIDR, "10.3.96.1/20");

        Answer answer = _resource.executeRequest(cmd);
        assertTrue(answer.getResult());
    }

    @Test
    public void addStaticNatRule() throws ConfigurationException, Exception {
        if (_context.containsKey("enable_console_output") && _context.get("enable_console_output").equals("true")) {
            System.out.println("\nTEST: addStaticNatRule");
            System.out.println("---------------------------------------------------");
        }
        _context.put("has_public_interface", "true");
        _context.put("has_private_interface", "true");
        _context.put("has_src_nat_rule", "true");
        _context.put("has_isolation_fw_rule", "true");
        _context.put("has_service_tcp_80", "true");
        _resource.setMockContext(_context);
        _resource.configure("PaloAltoResource", _resource_params);

        long vlanId = 3954;
        List<StaticNatRuleTO> rules = new ArrayList<StaticNatRuleTO>();
        StaticNatRuleTO active = new StaticNatRuleTO(0, "192.168.80.103", null, null, "10.3.97.158", null, null, null, false, false);
        rules.add(active);

        SetStaticNatRulesCommand cmd = new SetStaticNatRulesCommand(rules, null);
        cmd.setContextParam(NetworkElementCommand.GUEST_VLAN_TAG, Long.toString(vlanId));
        cmd.setContextParam(NetworkElementCommand.GUEST_NETWORK_CIDR, "10.3.96.1/20");

        Answer answer = _resource.executeRequest(cmd);
        assertTrue(answer.getResult());
    }

    @Test
    public void removeStaticNatRule() throws ConfigurationException, Exception {
        if (_context.containsKey("enable_console_output") && _context.get("enable_console_output").equals("true")) {
            System.out.println("\nTEST: removeStaticNatRule");
            System.out.println("---------------------------------------------------");
        }
        _context.put("has_public_interface", "true");
        _context.put("has_private_interface", "true");
        _context.put("has_src_nat_rule", "true");
        _context.put("has_isolation_fw_rule", "true");
        _context.put("has_service_tcp_80", "true");
        _context.put("has_stc_nat_rule", "true");
        _resource.setMockContext(_context);
        _resource.configure("PaloAltoResource", _resource_params);

        long vlanId = 3954;
        List<StaticNatRuleTO> rules = new ArrayList<StaticNatRuleTO>();
        StaticNatRuleTO revoked = new StaticNatRuleTO(0, "192.168.80.103", null, null, "10.3.97.158", null, null, null, true, false);
        rules.add(revoked);

        SetStaticNatRulesCommand cmd = new SetStaticNatRulesCommand(rules, null);
        cmd.setContextParam(NetworkElementCommand.GUEST_VLAN_TAG, Long.toString(vlanId));
        cmd.setContextParam(NetworkElementCommand.GUEST_NETWORK_CIDR, "10.3.96.1/20");

        Answer answer = _resource.executeRequest(cmd);
        assertTrue(answer.getResult());
    }

    @Test
    public void addPortForwardingRule() throws ConfigurationException, Exception {
        if (_context.containsKey("enable_console_output") && _context.get("enable_console_output").equals("true")) {
            System.out.println("\nTEST: addPortForwardingRule");
            System.out.println("---------------------------------------------------");
        }
        _context.put("has_public_interface", "true");
        _context.put("has_private_interface", "true");
        _context.put("has_src_nat_rule", "true");
        _context.put("has_isolation_fw_rule", "true");
        _context.put("has_service_tcp_80", "true");
        _resource.setMockContext(_context);
        _resource.configure("PaloAltoResource", _resource_params);

        long vlanId = 3954;
        List<PortForwardingRuleTO> rules = new ArrayList<PortForwardingRuleTO>();
        PortForwardingRuleTO active = new PortForwardingRuleTO(9, "192.168.80.103", 80, 80, "10.3.97.158", 8080, 8080, "tcp", false, false);
        rules.add(active);

        SetPortForwardingRulesCommand cmd = new SetPortForwardingRulesCommand(rules);
        cmd.setContextParam(NetworkElementCommand.GUEST_VLAN_TAG, Long.toString(vlanId));
        cmd.setContextParam(NetworkElementCommand.GUEST_NETWORK_CIDR, "10.3.96.1/20");

        Answer answer = _resource.executeRequest(cmd);
        assertTrue(answer.getResult());
    }

    @Test
    public void removePortForwardingRule() throws ConfigurationException, Exception {
        if (_context.containsKey("enable_console_output") && _context.get("enable_console_output").equals("true")) {
            System.out.println("\nTEST: removePortForwardingRule");
            System.out.println("---------------------------------------------------");
        }
        _context.put("has_public_interface", "true");
        _context.put("has_private_interface", "true");
        _context.put("has_src_nat_rule", "true");
        _context.put("has_isolation_fw_rule", "true");
        _context.put("has_service_tcp_80", "true");
        _context.put("has_dst_nat_rule", "true");
        _resource.setMockContext(_context);
        _resource.configure("PaloAltoResource", _resource_params);

        long vlanId = 3954;
        List<PortForwardingRuleTO> rules = new ArrayList<PortForwardingRuleTO>();
        PortForwardingRuleTO revoked = new PortForwardingRuleTO(9, "192.168.80.103", 80, 80, "10.3.97.158", 8080, 8080, "tcp", true, false);
        rules.add(revoked);

        SetPortForwardingRulesCommand cmd = new SetPortForwardingRulesCommand(rules);
        cmd.setContextParam(NetworkElementCommand.GUEST_VLAN_TAG, Long.toString(vlanId));
        cmd.setContextParam(NetworkElementCommand.GUEST_NETWORK_CIDR, "10.3.96.1/20");

        Answer answer = _resource.executeRequest(cmd);
        assertTrue(answer.getResult());
    }
}
