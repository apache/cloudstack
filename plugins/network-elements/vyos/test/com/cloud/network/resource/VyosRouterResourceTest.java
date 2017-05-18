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


public class VyosRouterResourceTest {
    // configuration data
    private String _testName = "VyosRouterTestDevice";
    private String _testZoneId = "TestZone";
    private String _testIp = "192.168.2.91";
    private String _testUsername = "vyos";
    private String _testPassword = "password";
    private String _testPublicInterface = "eth0";
    private String _testPrivateInterface = "eth0";

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
        _resourceParams.put("guid", "aaaaa-bbbbb-ccccc");

        _context = new HashMap<String, String>(); // global context
        _context.put("name", _testName);
        _context.put("zone_id", _testZoneId);
        _context.put("ip", _testIp);
        _context.put("username", _testUsername);
        _context.put("password", _testPassword);
        _context.put("public_interface", _testPublicInterface);
        _context.put("private_interface", _testPrivateInterface);


        // --
        _context.put("enable_console_output", "true"); // CHANGE TO "true" TO
                                                       // ENABLE CONSOLE LOGGING
                                                       // OF TESTS

        // Set this to true to run the tests against an actual Vyos Router
        // instead of the mock router.
        _context.put("use_test_router", "false");

        _resource.setMockContext(_context);
    }

     @Test(expected = ConfigurationException.class)
     public void resourceConfigureFailure() throws ConfigurationException {
         if (_context.containsKey("enable_console_output") && _context.get("enable_console_output").equals("true")) {
             System.out.println("\nTEST: resourceConfigureFailure");
             System.out.println("---------------------------------------------------");
         }
         _resource.configure("VyosRouterResource", new HashMap<String, Object>());
     }

     @Test
     public void resourceConfigure() throws ConfigurationException {
         if (_context.containsKey("enable_console_output") && _context.get("enable_console_output").equals("true")) {
             System.out.println("\nTEST: resourceConfigure");
             System.out.println("---------------------------------------------------");
         }
         _resource.configure("VyosRouterResource", _resourceParams);
     }

    @Test
    public void testInitialize() throws ConfigurationException {
        if (_context.containsKey("enable_console_output")
                && _context.get("enable_console_output").equals("true")) {
            System.out.println("\nTEST: testInitialize");
            System.out.println(
                    "---------------------------------------------------");
        }
        _resource.configure("VyosRouterResource", _resourceParams);

        StartupCommand[] sc = _resource.initialize();
        assertTrue(sc.length == 1);
        assertTrue("aaaaa-bbbbb-ccccc".equals(sc[0].getGuid()));
        assertTrue("VyosRouterTestDevice".equals(sc[0].getName()));
        assertTrue("TestZone".equals(sc[0].getDataCenter()));
    }

    // implement a fully functional network including public ip, private ip, ingress firewall, egress firewall, source nat, destination nat, and static nat.
    public void implementEndToEnd() throws ConfigurationException, ExecutionException {

        if (_context.containsKey("enable_console_output") && _context.get("enable_console_output").equals("true")) {
            System.out.println("\nTEST: implementEndToEnd");
            System.out.println("---------------------------------------------------");
            System.out.println("---------------------------------------------------");
            System.out.println("---------------------------------------------------");
        }

        //This method only works as a test against an actual Vyos Router. Skip it if we are running in sim mode.
        if (_context.containsKey("use_test_router") && _context.get("use_test_router").equals("true")) {

            //implementGuestNetwork
            implementGuestNetwork(true);

            //addIngressFirewallRule
            addIngressFirewallRule(true);

            //addEgressFirewallRule
            addEgressFirewallRule(true);

            //addStaticNateRule
            addStaticNatRule(true);

            //addPortForwardingRule
            addPortForwardingRule(true);

        } else if (_context.containsKey("enable_console_output") && _context.get("enable_console_output").equals("true")) {
            System.out.println("Skipping test because use_test_router is false. This test can only be run against an actual Vyos Router");

        }

        if (_context.containsKey("enable_console_output") && _context.get("enable_console_output").equals("true")) {
            System.out.println("\nFinished TEST: implementEndToEnd");
            System.out.println("---------------------------------------------------");
            System.out.println("---------------------------------------------------");
            System.out.println("---------------------------------------------------");
        }


    }


    @Test // setup and tear down a fully functional network including public ip, private ip, ingress firewall, egress firewall, source nat, destination nat, and static nat.
    public void testEndToEnd() throws ConfigurationException, ExecutionException {
        if (_context.containsKey("enable_console_output") && _context.get("enable_console_output").equals("true")) {
            System.out.println("\nTEST: TearDownEndToEnd");
            System.out.println("---------------------------------------------------");
            System.out.println("---------------------------------------------------");
            System.out.println("---------------------------------------------------");
        }

      //This method only works as a test against an actual Vyos Router. Skip it if we are running in sim mode.
        if (_context.containsKey("use_test_router") && _context.get("use_test_router").equals("true")) {
            //Run implementEndToEnd to setup the initial state of the vyos Router. This should be idempotent so if implement has already been called it should be ok to run it again.
            implementEndToEnd();

            //removePortForwardingRule
            removePortForwardingRule(true);

            //removeStaticNateRule
            removeStaticNatRule(true);

            //removeEgressFirewallRule
            removeEgressFirewallRule(true);

            //removeIngressFirewallRule
            removeIngressFirewallRule(true);

            //shutdownGuestNetwork
            shutdownGuestNetwork(true);

        } else if (_context.containsKey("enable_console_output") && _context.get("enable_console_output").equals("true")) {
            System.out.println("Skipping test because use_test_router is false. This test can only be run against an actual Vyos Router");

        }

        if (_context.containsKey("enable_console_output") && _context.get("enable_console_output").equals("true")) {
            System.out.println("\nFinished TEST: TearDownEndToEnd");
            System.out.println("---------------------------------------------------");
            System.out.println("---------------------------------------------------");
            System.out.println("---------------------------------------------------");
        }

    }


    @Test // implement public & private interfaces, source nat, guest network
    public void implementGuestNetwork() throws ConfigurationException, ExecutionException {
        //if we are using
        implementGuestNetwork(false);
    }

    public void implementGuestNetwork(boolean usingTestRouter) throws ConfigurationException, ExecutionException {
        //If we are using a test router then this method should only be executed via a direct call from  the implementEndToEnd/tearDownEndToEnd method.
        //Skip it when called directly by JUnit.
        if (_context.containsKey("use_test_router") && _context.get("use_test_router").equals("true") && usingTestRouter == false) {
            return;
        }

        if (_context.containsKey("enable_console_output") && _context.get("enable_console_output").equals("true")) {
            System.out.println("\nTEST: implementGuestNetwork");
            System.out.println("---------------------------------------------------");
        }

        _context.put("currentTest", "implementGuestNetwork");
        _context.put("vyosState", "initial");
        _resource.setMockContext(_context);
        _resource.configure("VyosRouterResource", _resourceParams);

        IpAddressTO ip = new IpAddressTO(Long.valueOf("1"), "192.168.2.102", true, false, true, "2", null, null, null, 100, false);
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

    @Test // remove public & private interface details, source nat, guest network
    public void shutdownGuestNetwork() throws ConfigurationException, ExecutionException {
        shutdownGuestNetwork(false);
    }

    public void shutdownGuestNetwork(boolean usingTestRouter) throws ConfigurationException, ExecutionException {

        //If we are using a test router then this method should only be executed via a direct call from the implementEndToEnd/tearDownEndToEnd method.
        //Skip it when called directly by JUnit.
        if (_context.containsKey("use_test_router") && _context.get("use_test_router").equals("true") && usingTestRouter == false) {
            return;
        }


        if (_context.containsKey("enable_console_output") && _context.get("enable_console_output").equals("true")) {
            System.out.println("\nTEST: shutdownGuestNetwork");
            System.out.println("---------------------------------------------------");
        }

        _context.put("currentTest", "shutdownGuestNetwork");
        _context.put("vyosState", "withImplementedGuestNetwork");
        _resource.setMockContext(_context);
        _resource.configure("VyosRouterResource", _resourceParams);

        IpAddressTO ip = new IpAddressTO(Long.valueOf("1"), "192.168.2.102", false, false, true, "2", null, null, null, 100, false);
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

    @Test
    public void addIngressFirewallRule() throws ConfigurationException {
        addIngressFirewallRule(false);
    }

    public void addIngressFirewallRule(boolean usingTestRouter) throws ConfigurationException {
        //If we are using a test router then this method should only be executed via a direct call from  the implementEndToEnd/tearDownEndToEnd method.
        //Skip it when called directly by JUnit.
        if (_context.containsKey("use_test_router") && _context.get("use_test_router").equals("true") && usingTestRouter == false) {
            return;
        }
        if (_context.containsKey("enable_console_output") && _context.get("enable_console_output").equals("true")) {
            System.out.println("\nTEST: addIngressFirewallRule");
            System.out.println("---------------------------------------------------");
        }

        _context.put("currentTest", "addIngressFirewallRule");
        _context.put("vyosState", "withImplementedGuestNetwork");
        _resource.setMockContext(_context);
        _resource.configure("VyosRouterResource", _resourceParams);

        String privateSubnet="10.3.96.0/24";
        _context.put("privateSubnet", privateSubnet);

        long vlanId = 3954;
        _context.put("guestVlanTag", String.valueOf(vlanId));
        _resource.setMockContext(_context);

        List<FirewallRuleTO> rules = new ArrayList<FirewallRuleTO>();
        List<String> cidrList = new ArrayList<String>();
        cidrList.add("0.0.0.0/0");
        FirewallRuleTO active = new FirewallRuleTO(8, "2", "192.168.2.102", "tcp", 22, 22, false, false, FirewallRule.Purpose.Firewall, cidrList, null, null);
        rules.add(active);

        SetFirewallRulesCommand cmd = new SetFirewallRulesCommand(rules);
        cmd.setAccessDetail(NetworkElementCommand.GUEST_VLAN_TAG, Long.toString(vlanId));
        cmd.setAccessDetail(NetworkElementCommand.GUEST_NETWORK_CIDR, "10.3.96.0/24");
        cmd.setAccessDetail("PUBLIC_VLAN_TAG", "2");

        Answer answer = _resource.executeRequest(cmd);
        assertTrue(answer.getResult());
        if (_context.containsKey("enable_console_output") && _context.get("enable_console_output").equals("true")) {
            System.out.println("\nFINISHED TEST: addIngressFirewallRule");
            System.out.println("---------------------------------------------------");
        }
    }

    @Test public void removeIngressFirewallRule() throws ConfigurationException {
        removeIngressFirewallRule(false);
    }
    public void removeIngressFirewallRule(boolean usingTestRouter) throws ConfigurationException {
        //If we are using a test router then this method should only be executed via a direct call from  the implementEndToEnd/tearDownEndToEnd method.
        //Skip it when called directly by JUnit.
        if (_context.containsKey("use_test_router") && _context.get("use_test_router").equals("true") && usingTestRouter == false) {
            return;
        }

        if (_context.containsKey("enable_console_output") && _context.get("enable_console_output").equals("true")) {
            System.out.println("\nTEST: removeIngressFirewallRule");
            System.out.println("---------------------------------------------------");
        }
        _context.put("currentTest", "removeIngressFirewallRule");
        _context.put("vyosState", "withAllRules");
        _resource.setMockContext(_context);
        _resource.configure("VyosRouterResource", _resourceParams);
        String privateSubnet="10.3.96.0/24";
        _context.put("privateSubnet", privateSubnet);


        long vlanId = 3954;
        _context.put("guestVlanTag", String.valueOf(vlanId));
        _resource.setMockContext(_context);

        List<FirewallRuleTO> rules = new ArrayList<FirewallRuleTO>();
        FirewallRuleTO revoked = new FirewallRuleTO(8, null, "192.168.2.102", "tcp", 22, 22, true, false, FirewallRule.Purpose.Firewall, null, null, null);
        rules.add(revoked);
        SetFirewallRulesCommand cmd = new SetFirewallRulesCommand(rules);
        cmd.setAccessDetail(NetworkElementCommand.GUEST_VLAN_TAG, Long.toString(vlanId));
        cmd.setAccessDetail(NetworkElementCommand.GUEST_NETWORK_CIDR, "10.3.96.0/24");
        cmd.setAccessDetail("PUBLIC_VLAN_TAG", "2");

        Answer answer = _resource.executeRequest(cmd);
        assertTrue(answer.getResult());
        if (_context.containsKey("enable_console_output") && _context.get("enable_console_output").equals("true")) {
            System.out.println("\nFINISHED TEST: removeIngressFirewallRule");
            System.out.println("---------------------------------------------------");
        }
    }

    @Test
    public void addEgressFirewallRule() throws ConfigurationException {
        addEgressFirewallRule(false);
    }

    public void addEgressFirewallRule(boolean usingTestRouter) throws ConfigurationException {
        //If we are using a test router then this method should only be executed via a direct call from  the implementEndToEnd/tearDownEndToEnd method.
        //Skip it when called directly by JUnit.
        if (_context.containsKey("use_test_router") && _context.get("use_test_router").equals("true") && usingTestRouter == false) {
            return;
        }

        if (_context.containsKey("enable_console_output") && _context.get("enable_console_output").equals("true")) {
            System.out.println("\nTEST: addEgressFirewallRule");
            System.out.println("---------------------------------------------------");
        }
        _context.put("currentTest", "addEgressFirewallRule");
        _context.put("vyosState", "withImplementedGuestNetwork");
        String privateSubnet="10.3.96.0/24";
        _context.put("privateSubnet", privateSubnet);

        _resource.configure("VyosRouterResource", _resourceParams);

        long vlanId = 3954;
        _context.put("guestVlanTag", String.valueOf(vlanId));
        _resource.setMockContext(_context);

        List<FirewallRuleTO> rules = new ArrayList<FirewallRuleTO>();
        List<String> cidrList = new ArrayList<String>();
        cidrList.add("0.0.0.0/0");
        FirewallRuleVO activeVO = new FirewallRuleVO("333", null, 80, 80, "tcp", 1, 1, 1, Purpose.Firewall, cidrList, null, null, null, FirewallRule.TrafficType.Egress);
        FirewallRuleTO active = new FirewallRuleTO(activeVO, Long.toString(vlanId), null, Purpose.Firewall, FirewallRule.TrafficType.Egress);
        rules.add(active);

        SetFirewallRulesCommand cmd = new SetFirewallRulesCommand(rules);
        cmd.setAccessDetail(NetworkElementCommand.GUEST_VLAN_TAG, Long.toString(vlanId));
        cmd.setAccessDetail(NetworkElementCommand.GUEST_NETWORK_CIDR, "10.3.96.0/24");
        cmd.setAccessDetail("PUBLIC_VLAN_TAG", "2");

        Answer answer = _resource.executeRequest(cmd);
        assertTrue(answer.getResult());
        if (_context.containsKey("enable_console_output") && _context.get("enable_console_output").equals("true")) {
            System.out.println("\nFINISHED TEST: addEgressFirewallRule");
            System.out.println("---------------------------------------------------");
        }
    }

    @Test
    public void removeEgressFirewallRule() throws ConfigurationException {
        removeEgressFirewallRule(false);
    }

    public void removeEgressFirewallRule(boolean usingTestRouter) throws ConfigurationException {
        //If we are using a test router then this method should only be executed via a direct call from  the implementEndToEnd/tearDownEndToEnd method.
        //Skip it when called directly by JUnit.
        if (_context.containsKey("use_test_router") && _context.get("use_test_router").equals("true") && usingTestRouter == false) {
            return;
        }

        if (_context.containsKey("enable_console_output") && _context.get("enable_console_output").equals("true")) {
            System.out.println("\nTEST: removeEgressFirewallRule");
            System.out.println("---------------------------------------------------");
        }
        _context.put("currentTest", "removeEgressFirewallRule");
        _context.put("vyosState", "withAllRules");
        String privateSubnet="10.3.96.0/24";
        _context.put("privateSubnet", privateSubnet);

        _resource.configure("VyosRouterResource", _resourceParams);

        long vlanId = 3954;
        _context.put("guestVlanTag", String.valueOf(vlanId));
        _resource.setMockContext(_context);

        List<FirewallRuleTO> rules = new ArrayList<FirewallRuleTO>();
        FirewallRuleVO revokedVO = new FirewallRuleVO("333", null, 80, 80, "tcp", 1, 1, 1, Purpose.Firewall, null, null, null, null, FirewallRule.TrafficType.Egress);
        revokedVO.setState(State.Revoke); FirewallRuleTO revoked = new FirewallRuleTO(revokedVO, Long.toString(vlanId), null, Purpose.Firewall, FirewallRule.TrafficType.Egress); rules.add(revoked);

        SetFirewallRulesCommand cmd = new SetFirewallRulesCommand(rules);
        cmd.setAccessDetail(NetworkElementCommand.GUEST_VLAN_TAG, Long.toString(vlanId));
        cmd.setAccessDetail(NetworkElementCommand.GUEST_NETWORK_CIDR, "10.3.96.0/24");
        cmd.setAccessDetail("PUBLIC_VLAN_TAG", "2");

        Answer answer = _resource.executeRequest(cmd);
        assertTrue(answer.getResult());
        if (_context.containsKey("enable_console_output") && _context.get("enable_console_output").equals("true")) {
            System.out.println("\nFINISHED TEST: removeEgressFirewallRule");
            System.out.println("---------------------------------------------------");
        }
    }

    @Test
    public void addStaticNatRule() throws ConfigurationException {
        addStaticNatRule(false);
    }

    public void addStaticNatRule(boolean usingTestRouter) throws ConfigurationException {
        //If we are using a test router then this method should only be executed via a direct call from  the implementEndToEnd/tearDownEndToEnd method.
        //Skip it when called directly by JUnit.
        if (_context.containsKey("use_test_router") && _context.get("use_test_router").equals("true") && usingTestRouter == false) {
            return;
        }

        if (_context.containsKey("enable_console_output") && _context.get("enable_console_output").equals("true")) {
            System.out.println("\nTEST: addStaticNatRule");
            System.out.println("---------------------------------------------------");
        }

        _context.put("currentTest", "addStaticNatRule");
        _context.put("vyosState", "withImplementedGuestNetwork");
        _resource.setMockContext(_context);
        _resource.configure("VyosRouterResource", _resourceParams);


        long vlanId = 3954;
        List<StaticNatRuleTO> rules = new ArrayList<StaticNatRuleTO>();
        StaticNatRuleTO active = new StaticNatRuleTO(9, "2", "192.168.2.103", null, null, "10.3.96.3", null, null, null, false, false);
        rules.add(active);

        SetStaticNatRulesCommand cmd = new SetStaticNatRulesCommand(rules, null);
        cmd.setAccessDetail(NetworkElementCommand.GUEST_VLAN_TAG, Long.toString(vlanId));
        cmd.setAccessDetail(NetworkElementCommand.GUEST_NETWORK_CIDR, "10.3.96.0/24");
        cmd.setAccessDetail("PUBLIC_VLAN_TAG", "2");

        Answer answer = _resource.executeRequest(cmd);

        assertTrue(answer.getResult());
        if (_context.containsKey("enable_console_output") && _context.get("enable_console_output").equals("true")) {
            System.out.println("\nFINISHED TEST: addStaticNatRule");
            System.out.println("---------------------------------------------------");
        }
    }

   @Test
   public void removeStaticNatRule() throws ConfigurationException {
       removeStaticNatRule(false);
   }

   public void removeStaticNatRule(boolean usingTestRouter) throws ConfigurationException {
       //If we are using a test router then this method should only be executed via a direct call from  the implementEndToEnd/tearDownEndToEnd method.
       //Skip it when called directly by JUnit.
       if (_context.containsKey("use_test_router") && _context.get("use_test_router").equals("true") && usingTestRouter == false) {
           return;
       }

       if (_context.containsKey("enable_console_output")
               && _context.get("enable_console_output").equals("true")) {
           System.out.println("\nTEST: removeStaticNatRule");
           System.out.println(
                   "---------------------------------------------------");
       }
       _context.put("currentTest", "removeStaticNatRule");
       _context.put("vyosState", "withAllRules");
       _resource.setMockContext(_context);
       _resource.configure("VyosRouterResource", _resourceParams);

       long vlanId = 3954;
       List<StaticNatRuleTO> rules = new ArrayList<StaticNatRuleTO>();
       //StaticNatRuleTO revoked = new StaticNatRuleTO(9, "192.168.2.103", null, null, "10.3.96.3", null, null, null, true, false);
       StaticNatRuleTO revoked = new StaticNatRuleTO(9, "2", "192.168.2.103", null, null, "10.3.96.3", null, null, null, true, false);
       rules.add(revoked);

       SetStaticNatRulesCommand cmd = new SetStaticNatRulesCommand(rules, null);
       cmd.setAccessDetail(NetworkElementCommand.GUEST_VLAN_TAG, Long.toString(vlanId));
       cmd.setAccessDetail(NetworkElementCommand.GUEST_NETWORK_CIDR, "10.3.96.0/24");
       cmd.setAccessDetail("PUBLIC_VLAN_TAG", "2");

       Answer answer = _resource.executeRequest(cmd);
       assertTrue(answer.getResult());
       if (_context.containsKey("enable_console_output") && _context.get("enable_console_output").equals("true")) {
           System.out.println("\nFINISHED TEST: removeStaticNatRule");
           System.out.println("---------------------------------------------------");
       }
   }

   @Test
   public void addPortForwardingRule() throws ConfigurationException {
       addPortForwardingRule(false);
   }

   public void addPortForwardingRule(boolean usingTestRouter) throws ConfigurationException {
       //If we are using a test router then this method should only be executed via a direct call from  the implementEndToEnd/tearDownEndToEnd method.
       //Skip it when called directly by JUnit.
       if (_context.containsKey("use_test_router") && _context.get("use_test_router").equals("true") && usingTestRouter == false) {
           return;
       }

       if (_context.containsKey("enable_console_output") && _context.get("enable_console_output").equals("true")) {
           System.out.println("\nTEST: addPortForwardingRule");
           System.out.println(
                   "---------------------------------------------------");
       }
       _context.put("currentTest", "addPortForwardingRule");
       _context.put("vyosState", "withImplementedGuestNetwork");
       _resource.setMockContext(_context);
       _resource.configure("VyosRouterResource", _resourceParams);

       long vlanId = 3954;
       List<PortForwardingRuleTO> rules = new ArrayList<PortForwardingRuleTO>();
       PortForwardingRuleTO active = new PortForwardingRuleTO(10, "192.168.2.102", 22, 22, "10.3.96.2", 22, 22, "tcp", false, false);
       rules.add(active);

       SetPortForwardingRulesCommand cmd = new SetPortForwardingRulesCommand(rules);
       cmd.setAccessDetail(NetworkElementCommand.GUEST_VLAN_TAG, Long.toString(vlanId));
       cmd.setAccessDetail(NetworkElementCommand.GUEST_NETWORK_CIDR, "10.3.96.0/24");
       cmd.setAccessDetail("PUBLIC_VLAN_TAG", "2");

       Answer answer = _resource.executeRequest(cmd);
       assertTrue(answer.getResult());
       if (_context.containsKey("enable_console_output") && _context.get("enable_console_output").equals("true")) {
           System.out.println("\nFINISHED TEST: addPortForwardingRule");
           System.out.println("---------------------------------------------------");
       }
   }

   @Test
   public void removePortForwardingRule() throws ConfigurationException {
       removePortForwardingRule(false);
   }
   public void removePortForwardingRule(boolean usingTestRouter) throws ConfigurationException {
       //If we are using a test router then this method should only be executed via a direct call from  the implementEndToEnd/tearDownEndToEnd method.
       //Skip it when called directly by JUnit.
       if (_context.containsKey("use_test_router") && _context.get("use_test_router").equals("true") && usingTestRouter == false) {
           return;
       }

       if (_context.containsKey("enable_console_output") && _context.get("enable_console_output").equals("true")) {
           System.out.println("\nTEST: removePortForwardingRule");
           System.out.println("---------------------------------------------------");
       }
       _context.put("currentTest", "removePortForwardingRule");
       _context.put("vyosState", "withAllRules");
       _resource.configure("VyosRouterResource", _resourceParams);

       long vlanId = 3954;
       List<PortForwardingRuleTO> rules = new ArrayList<PortForwardingRuleTO>();
       PortForwardingRuleTO revoked = new PortForwardingRuleTO(10, "192.168.2.102", 22, 22, "10.3.96.2", 22, 22, "tcp", true, false);
       rules.add(revoked);

       SetPortForwardingRulesCommand cmd = new SetPortForwardingRulesCommand(rules);
       cmd.setAccessDetail(NetworkElementCommand.GUEST_VLAN_TAG, Long.toString(vlanId));
       cmd.setAccessDetail(NetworkElementCommand.GUEST_NETWORK_CIDR, "10.3.96.0/24");
       cmd.setAccessDetail("PUBLIC_VLAN_TAG", "2");

       Answer answer = _resource.executeRequest(cmd);
       assertTrue(answer.getResult());
       if (_context.containsKey("enable_console_output")
               && _context.get("enable_console_output").equals("true")) {
           System.out.println("\nFINISHED TEST: removePortForwardingRule");
            System.out.println("---------------------------------------------------");
        }
    }
}