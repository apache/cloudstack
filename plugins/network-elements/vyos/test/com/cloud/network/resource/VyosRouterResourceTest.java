//Licensed to the Apache Software Foundation (ASF) under one
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.ConfigurationException;

import org.junit.Before;
import org.junit.Test;

import com.cloud.agent.api.Answer;
//import com.cloud.agent.api.StartupCommand;
//import com.cloud.agent.api.routing.IpAssocAnswer;
//import com.cloud.agent.api.routing.IpAssocCommand;
import com.cloud.agent.api.routing.NetworkElementCommand;
//import com.cloud.agent.api.routing.SetFirewallRulesCommand;
import com.cloud.agent.api.routing.SetPortForwardingRulesCommand;
import com.cloud.agent.api.routing.SetStaticNatRulesCommand;
//import com.cloud.agent.api.to.FirewallRuleTO;
//import com.cloud.agent.api.to.IpAddressTO;
import com.cloud.agent.api.to.PortForwardingRuleTO;
import com.cloud.agent.api.to.StaticNatRuleTO;
//import com.cloud.network.rules.FirewallRule;
//import com.cloud.network.rules.FirewallRule.Purpose;
//import com.cloud.network.rules.FirewallRule.State;
//import com.cloud.network.rules.FirewallRuleVO;
//import com.cloud.utils.exception.ExecutionException;


public class VyosRouterResourceTest {
    // configuration data
    private String _testName = "VyosRouterTestDevice";
    private String _testZoneId = "TestZone";
    private String _testIp = "192.168.99.1";
    private String _testUsername = "vyos";
    private String _testPassword = "password";
    private String _testPublicInterface = "eth0";
    private String _testPrivateInterface = "eth1";
    private String _testPublicZone = "untrust";
    private String _testPrivateZone = "trust";
    private String _testVirtualRouter = "default";

    MockableVyosRouterResource _resource;
    Map<String, Object> _resourceParams;
    HashMap<String, String> _context;

    @Before
    public void setUp() {
        _resource = new MockableVyosRouterResource();
        _resourceParams = new HashMap<String, Object>(); // params to be passed
                                                         // to configure()
        _resourceParams.put("name", _testName);
        _resourceParams.put("zoneId", _testZoneId);
        _resourceParams.put("ip", _testIp);
        _resourceParams.put("username", _testUsername);
        _resourceParams.put("password", _testPassword);
        _resourceParams.put("publicinterface", _testPublicInterface);
        _resourceParams.put("privateinterface", _testPrivateInterface);
        _resourceParams.put("publicnetwork", _testPublicZone);
        _resourceParams.put("privatenetwork", _testPrivateZone);
        _resourceParams.put("pavr", _testVirtualRouter);
        _resourceParams.put("guid", "aaaaa-bbbbb-ccccc");

        _context = new HashMap<String, String>(); // global context
        _context.put("name", _testName);
        _context.put("zone_id", _testZoneId);
        _context.put("ip", _testIp);
        _context.put("username", _testUsername);
        _context.put("password", _testPassword);
        _context.put("public_interface", _testPublicInterface);
        _context.put("private_interface", _testPrivateInterface);
        _context.put("public_zone", _testPublicZone);
        _context.put("private_zone", _testPrivateZone);
        _context.put("pa_vr", _testVirtualRouter);
        // --
        _context.put("public_using_ethernet", "true");
        _context.put("private_using_ethernet", "true");
        _context.put("has_management_profile", "true");
        _context.put("enable_console_output", "true"); // CHANGE TO "true" TO
                                                       // ENABLE CONSOLE LOGGING
                                                       // OF TESTS

        // This will cause the tests to be run against an actual Vyos Router
        // instead of the mock router.
        _context.put("use_test_router", "true");
        _resource.setMockContext(_context);
    }

    /*

     @Test(expected = ConfigurationException.class) public void
     resourceConfigureFailure() throws ConfigurationException { if
     (_context.containsKey("enable_console_output") &&
     _context.get("enable_console_output").equals("true")) {
     System.out.println("\nTEST: resourceConfigureFailure");
     System.out.println("---------------------------------------------------")
     ; } _resource.configure("VyosRouterResource", new HashMap<String,
     Object>()); }


     @Test public void resourceConfigure() throws ConfigurationException { if
     (_context.containsKey("enable_console_output") &&
     _context.get("enable_console_output").equals("true")) {
     System.out.println("\nTEST: resourceConfigure");
     System.out.println("---------------------------------------------------")
     ; } _resource.configure("VyosRouterResource", _resourceParams); }
     */
    // TODO Not sure that this state has an analog in VYOS
    /*
     @Test(expected = ConfigurationException.class) public void
     simulateFirewallNotConfigurable() throws ConfigurationException { if
     (_context.containsKey("enable_console_output") &&
     _context.get("enable_console_output").equals("true")) {
     System.out.println("\nTEST: simulateFirewallNotConfigurable");
     System.out.println("---------------------------------------------------")
     ; } _context.put("firewall_has_pending_changes", "true");
     _context.remove("has_management_profile");
     _resource.setMockContext(_context);
     _resource.configure("VyosRouterResource", _resourceParams); }
     */
    /*
     // TODO Not Done

     @Test(expected = ConfigurationException.class) public void
     simulateFirewallCommitFailure() throws ConfigurationException { if
     (_context.containsKey("enable_console_output") &&
     _context.get("enable_console_output").equals("true")) {
     System.out.println("\nTEST: simulateFirewallCommitFailure");
     System.out.println("---------------------------------------------------")
     ; } _context.put("simulate_commit_failure", "true");
     _context.remove("has_management_profile");
     _resource.setMockContext(_context);
     _resource.configure("VyosRouterResource", _resourceParams); }

     @Test public void testInitialize() throws ConfigurationException { if
     (_context.containsKey("enable_console_output") &&
     _context.get("enable_console_output").equals("true")) {
     System.out.println("\nTEST: testInitialize");
     System.out.println("---------------------------------------------------")
     ; } _resource.configure("VyosRouterResource", _resourceParams);

     StartupCommand[] sc = _resource.initialize(); assertTrue(sc.length == 1);
     assertTrue("aaaaa-bbbbb-ccccc".equals(sc[0].getGuid()));
     assertTrue("VyosRouterTestDevice".equals(sc[0].getName()));
     assertTrue("TestZone".equals(sc[0].getDataCenter())); }
     */

/*
     @Test // implement public & private interfaces, source nat, guest network
     public void implementGuestNetwork() throws ConfigurationException, ExecutionException {
         if (_context.containsKey("enable_console_output") && _context.get("enable_console_output").equals("true")) {
             System.out.println("\nTEST: implementGuestNetwork");
             System.out.println("---------------------------------------------------");
         }

         _resource.configure("VyosRouterResource", _resourceParams);

         IpAddressTO ip = new IpAddressTO(Long.valueOf("1"), "192.168.99.102", true, false, true, "untagged", null, null, null, 100, false);
         IpAddressTO[] ips = new IpAddressTO[1]; ips[0] = ip; IpAssocCommand cmd = new IpAssocCommand(ips);
         cmd.setAccessDetail(NetworkElementCommand.GUEST_NETWORK_GATEWAY, "10.3.96.1");
         cmd.setAccessDetail(NetworkElementCommand.GUEST_NETWORK_CIDR, "10.3.96.0/24");
         cmd.setAccessDetail(NetworkElementCommand.GUEST_VLAN_TAG, "3954");
         cmd.setAccessDetail(NetworkElementCommand.FIREWALL_EGRESS_DEFAULT, "deny");

         IpAssocAnswer answer = (IpAssocAnswer)_resource.executeRequest(cmd);
         assertTrue(answer.getResult());

         if (_context.containsKey("enable_console_output") && _context.get("enable_console_output").equals("true")) {
             System.out.println("\nFINISHED TEST: implementGuestNetwork");
             System.out.println("---------------------------------------------------");
         }
     }
*/

/*
     @Test // remove public & private interface details, source nat, guest network
     public void shutdownGuestNetwork() throws ConfigurationException, ExecutionException
     {
         if (_context.containsKey("enable_console_output") && _context.get("enable_console_output").equals("true")) {
             System.out.println("\nTEST: shutdownGuestNetwork");
             System.out.println("---------------------------------------------------");
         }

         _context.put("has_public_interface", "true");
         _context.put("has_private_interface", "true");
         _context.put("has_src_nat_rule", "true");
         _context.put("has_isolation_fw_rule", "true");
         _resource.setMockContext(_context);
         _resource.configure("VyosRouterResource", _resourceParams);

         IpAddressTO ip = new IpAddressTO(Long.valueOf("1"), "192.168.99.102", false, false, true, "untagged", null, null, null, 100, false);
         IpAddressTO[] ips = new IpAddressTO[1]; ips[0] = ip; IpAssocCommand cmd = new IpAssocCommand(ips);
         cmd.setAccessDetail(NetworkElementCommand.GUEST_NETWORK_GATEWAY, "10.3.96.1");
         cmd.setAccessDetail(NetworkElementCommand.GUEST_NETWORK_CIDR, "10.3.96.0/24");
         cmd.setAccessDetail(NetworkElementCommand.GUEST_VLAN_TAG, "3954");
         cmd.setAccessDetail(NetworkElementCommand.FIREWALL_EGRESS_DEFAULT, "deny");

         IpAssocAnswer answer = (IpAssocAnswer)_resource.executeRequest(cmd);
         assertTrue(answer.getResult());
         if (_context.containsKey("enable_console_output") && _context.get("enable_console_output").equals("true")) {
             System.out.println("\nFINISHED TEST: shutdownGuestNetwork");
             System.out.println("---------------------------------------------------");
         }
     }
*/

/*
     @Test public void addIngressFirewallRule() throws ConfigurationException, Exception {
         if (_context.containsKey("enable_console_output") && _context.get("enable_console_output").equals("true")) {
             System.out.println("\nTEST: addIngressFirewallRule");
             System.out.println("---------------------------------------------------");
         } //_context.put("use_test_router", "true");
         _context.put("has_public_interface", "true");
         _context.put("has_private_interface", "true");
         _context.put("has_src_nat_rule", "true");
         _context.put("has_isolation_fw_rule", "true");
         _context.put("has_service_tcp_80", "true");
         _resource.setMockContext(_context);
         _resource.configure("VyosRouterResource", _resourceParams);

         long vlanId = 3954;
         List<FirewallRuleTO> rules = new ArrayList<FirewallRuleTO>();
         List<String> cidrList = new ArrayList<String>();
         cidrList.add("0.0.0.0/0");
         //FirewallRuleTO active = new FirewallRuleTO(8, null, "192.168.99.103", "tcp", 80, 80, false, false, FirewallRule.Purpose.Firewall, cidrList, null, null);
         FirewallRuleTO active = new FirewallRuleTO(8, null, "192.168.99.102", "tcp", 22, 22, false, false, FirewallRule.Purpose.Firewall, cidrList, null, null);
         rules.add(active);

         SetFirewallRulesCommand cmd = new SetFirewallRulesCommand(rules);
         cmd.setAccessDetail(NetworkElementCommand.GUEST_VLAN_TAG, Long.toString(vlanId));
         cmd.setAccessDetail(NetworkElementCommand.GUEST_NETWORK_CIDR, "10.3.96.0/24");

         Answer answer = _resource.executeRequest(cmd);
         assertTrue(answer.getResult());
         if (_context.containsKey("enable_console_output") && _context.get("enable_console_output").equals("true")) {
             System.out.println("\nFINISHED TEST: addIngressFirewallRule");
             System.out.println("---------------------------------------------------");
         }
     }
*/

/*
     @Test public void removeIngressFirewallRule() throws ConfigurationException, Exception {
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
         _resource.configure("VyosRouterResource", _resourceParams);

         long vlanId = 3954; List<FirewallRuleTO> rules = new ArrayList<FirewallRuleTO>();
         //FirewallRuleTO revoked = new FirewallRuleTO(8, null, "192.168.99.103", "tcp", 80, 80, true, false, FirewallRule.Purpose.Firewall, null, null, null);
         FirewallRuleTO revoked = new FirewallRuleTO(8, null, "192.168.99.102", "tcp", 22, 22, true, false, FirewallRule.Purpose.Firewall, null, null, null);
         rules.add(revoked);

         SetFirewallRulesCommand cmd = new SetFirewallRulesCommand(rules);
         cmd.setAccessDetail(NetworkElementCommand.GUEST_VLAN_TAG, Long.toString(vlanId));
         cmd.setAccessDetail(NetworkElementCommand.GUEST_NETWORK_CIDR, "10.3.96.0/24");

         Answer answer = _resource.executeRequest(cmd);
         assertTrue(answer.getResult());
         if (_context.containsKey("enable_console_output") && _context.get("enable_console_output").equals("true")) {
             System.out.println("\nFINISHED TEST: removeIngressFirewallRule");
             System.out.println("---------------------------------------------------");
         }
     }
*/
/*
     @Test public void addEgressFirewallRule() throws ConfigurationException, Exception {
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
         _resource.configure("VyosRouterResource", _resourceParams);

         long vlanId = 3954; List<FirewallRuleTO> rules = new ArrayList<FirewallRuleTO>();
         List<String> cidrList = new ArrayList<String>();
         cidrList.add("0.0.0.0/0");
         FirewallRuleVO activeVO = new FirewallRuleVO(null, null, 80, 80, "tcp", 1, 1, 1, Purpose.Firewall, cidrList, null, null, null, FirewallRule.TrafficType.Egress);
         FirewallRuleTO active = new FirewallRuleTO(activeVO, Long.toString(vlanId), null, Purpose.Firewall, FirewallRule.TrafficType.Egress);
         rules.add(active);

         SetFirewallRulesCommand cmd = new SetFirewallRulesCommand(rules);
         cmd.setAccessDetail(NetworkElementCommand.GUEST_VLAN_TAG, Long.toString(vlanId));
         cmd.setAccessDetail(NetworkElementCommand.GUEST_NETWORK_CIDR, "10.3.96.0/24");

         Answer answer = _resource.executeRequest(cmd);
         assertTrue(answer.getResult());
         if (_context.containsKey("enable_console_output") && _context.get("enable_console_output").equals("true")) {
             System.out.println("\nFINISHED TEST: addEgressFirewallRule");
             System.out.println("---------------------------------------------------");
         }
     }
*/
/*
     @Test public void removeEgressFirewallRule() throws ConfigurationException, Exception {
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
         _resource.configure("VyosRouterResource", _resourceParams);

         long vlanId = 3954; List<FirewallRuleTO> rules = new ArrayList<FirewallRuleTO>();
         FirewallRuleVO revokedVO = new FirewallRuleVO(null, null, 80, 80, "tcp", 1, 1, 1, Purpose.Firewall, null, null, null, null, FirewallRule.TrafficType.Egress);
         revokedVO.setState(State.Revoke); FirewallRuleTO revoked = new FirewallRuleTO(revokedVO, Long.toString(vlanId), null, Purpose.Firewall, FirewallRule.TrafficType.Egress); rules.add(revoked);

         SetFirewallRulesCommand cmd = new SetFirewallRulesCommand(rules);
         cmd.setAccessDetail(NetworkElementCommand.GUEST_VLAN_TAG, Long.toString(vlanId));
         cmd.setAccessDetail(NetworkElementCommand.GUEST_NETWORK_CIDR, "10.3.96.0/24");

         Answer answer = _resource.executeRequest(cmd);
         assertTrue(answer.getResult());
         if (_context.containsKey("enable_console_output") && _context.get("enable_console_output").equals("true")) {
             System.out.println("\nFINISHED TEST: removeEgressFirewallRule");
             System.out.println("---------------------------------------------------");
         }
     }
*/
/*
     @Test public void addStaticNatRule() throws ConfigurationException, Exception {
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
         _resource.configure("VyosRouterResource", _resourceParams);

         System.out.println("Starting Execute");
         long vlanId = 3954;
         List<StaticNatRuleTO> rules = new ArrayList<StaticNatRuleTO>();
         //StaticNatRuleTO active = new StaticNatRuleTO(9, "192.168.99.103", null, null, "10.3.97.158", null, null, null, false, false);
         StaticNatRuleTO active = new StaticNatRuleTO(9, "192.168.99.103", null, null, "10.3.96.3", null, null, null, false, false); rules.add(active);

         SetStaticNatRulesCommand cmd = new SetStaticNatRulesCommand(rules, null);
         cmd.setAccessDetail(NetworkElementCommand.GUEST_VLAN_TAG, Long.toString(vlanId));
         cmd.setAccessDetail(NetworkElementCommand.GUEST_NETWORK_CIDR, "10.3.96.0/24");

         Answer answer = _resource.executeRequest(cmd);
         System.out.println("Finished Execute");
         assertTrue(answer.getResult());
         if (_context.containsKey("enable_console_output") && _context.get("enable_console_output").equals("true")) {
             System.out.println("\nFINISHED TEST: addStaticNatRule");
             System.out.println("---------------------------------------------------");
         }
     }
*/

    @Test
    public void removeStaticNatRule() throws ConfigurationException, Exception {
        if (_context.containsKey("enable_console_output")
                && _context.get("enable_console_output").equals("true")) {
            System.out.println("\nTEST: removeStaticNatRule");
            System.out.println(
                    "---------------------------------------------------");
        }
        _context.put("has_public_interface", "true");
        _context.put("has_private_interface", "true");
        _context.put("has_src_nat_rule", "true");
        _context.put("has_isolation_fw_rule", "true");
        _context.put("has_service_tcp_80", "true");
        _context.put("has_stc_nat_rule", "true");
        _resource.setMockContext(_context);
        _resource.configure("VyosRouterResource", _resourceParams);

        long vlanId = 3954;
        List<StaticNatRuleTO> rules = new ArrayList<StaticNatRuleTO>();
        //StaticNatRuleTO revoked = new StaticNatRuleTO(9, "192.168.99.103", null, null, "10.3.97.158", null, null, null, true, false);
        StaticNatRuleTO revoked = new StaticNatRuleTO(9, "192.168.99.103", null, null, "10.3.96.3", null, null, null, true, false);
        rules.add(revoked);

        SetStaticNatRulesCommand cmd = new SetStaticNatRulesCommand(rules, null);
        cmd.setAccessDetail(NetworkElementCommand.GUEST_VLAN_TAG, Long.toString(vlanId));
        cmd.setAccessDetail(NetworkElementCommand.GUEST_NETWORK_CIDR, "10.3.96.0/24");

        Answer answer = _resource.executeRequest(cmd);
        assertTrue(answer.getResult());
        if (_context.containsKey("enable_console_output") && _context.get("enable_console_output").equals("true")) {
            System.out.println("\nFINISHED TEST: removeStaticNatRule");
            System.out.println(
                    "---------------------------------------------------");
        }
    }
/*
    @Test
    public void addPortForwardingRule() throws ConfigurationException, Exception {
        if (_context.containsKey("enable_console_output") && _context.get("enable_console_output").equals("true")) {
            System.out.println("\nTEST: addPortForwardingRule");
            System.out.println(
                    "---------------------------------------------------");
        }
        _context.put("has_public_interface", "true");
        _context.put("has_private_interface", "true");
        _context.put("has_src_nat_rule", "true");
        _context.put("has_isolation_fw_rule", "true");
        _context.put("has_service_tcp_80", "true");
        _resource.setMockContext(_context);
        _resource.configure("VyosRouterResource", _resourceParams);

        long vlanId = 3954;
        List<PortForwardingRuleTO> rules = new ArrayList<PortForwardingRuleTO>();
        // PortForwardingRuleTO active = new PortForwardingRuleTO(10,
        // "192.168.99.103", 80, 80, "10.3.97.158", 8080, 8080, "tcp", false,
        // false);
        PortForwardingRuleTO active = new PortForwardingRuleTO(10, "192.168.99.102", 22, 22, "10.3.96.2", 22, 22, "tcp", false, false);
        rules.add(active);

        SetPortForwardingRulesCommand cmd = new SetPortForwardingRulesCommand(rules);
        cmd.setAccessDetail(NetworkElementCommand.GUEST_VLAN_TAG, Long.toString(vlanId));
        cmd.setAccessDetail(NetworkElementCommand.GUEST_NETWORK_CIDR, "10.3.96.0/24");
        Answer answer = _resource.executeRequest(cmd);
        assertTrue(answer.getResult());
        if (_context.containsKey("enable_console_output") && _context.get("enable_console_output").equals("true")) {
            System.out.println("\nFINISHED TEST: addPortForwardingRule");
            System.out.println(
                    "---------------------------------------------------");
        }
    }
*/

    @Test
    public void removePortForwardingRule()
            throws ConfigurationException, Exception {
        if (_context.containsKey("enable_console_output") && _context.get("enable_console_output").equals("true")) {
            System.out.println("\nTEST: removePortForwardingRule");
            System.out.println(
                    "---------------------------------------------------");
        }
        _context.put("has_public_interface", "true");
        _context.put("has_private_interface", "true");
        _context.put("has_src_nat_rule", "true");
        _context.put("has_isolation_fw_rule", "true");
        _context.put("has_service_tcp_80", "true");
        _context.put("has_dst_nat_rule", "true");
        _resource.setMockContext(_context);
        _resource.configure("VyosRouterResource", _resourceParams);

        long vlanId = 3954;
        List<PortForwardingRuleTO> rules = new ArrayList<PortForwardingRuleTO>();
        // PortForwardingRuleTO revoked = new PortForwardingRuleTO(10,
        // "192.168.99.103", 80, 80, "10.3.97.158", 8080, 8080, "tcp", true,
        // false);
        PortForwardingRuleTO revoked = new PortForwardingRuleTO(10, "192.168.99.102", 22, 22, "10.3.96.2", 22, 22, "tcp", true, false);
        rules.add(revoked);

        SetPortForwardingRulesCommand cmd = new SetPortForwardingRulesCommand(
                rules);
        cmd.setAccessDetail(NetworkElementCommand.GUEST_VLAN_TAG,
                Long.toString(vlanId));
        cmd.setAccessDetail(NetworkElementCommand.GUEST_NETWORK_CIDR,
                "10.3.96.0/24");

        Answer answer = _resource.executeRequest(cmd);
        assertTrue(answer.getResult());
        if (_context.containsKey("enable_console_output")
                && _context.get("enable_console_output").equals("true")) {
            System.out.println("\nFINISHED TEST: removePortForwardingRule");
            System.out.println(
                    "---------------------------------------------------");
        }
    }



}