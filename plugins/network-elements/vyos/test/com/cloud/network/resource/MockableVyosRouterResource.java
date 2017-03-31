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

import java.util.ArrayList;
import java.util.HashMap;

import com.cloud.utils.exception.ExecutionException;
// http client handling
// for prettyFormat()

public class MockableVyosRouterResource extends VyosRouterResource {
    private HashMap<String, String> context;
    private int commandSetCount;

    public void setMockContext(HashMap<String, String> context) {
        this.context = context;
        buildMockVyosConfigs();
        buildExpectedVyosCommands();
        commandSetCount=0;
    }

    private HashMap<String, String> mockVyosConfigs;


    private HashMap<String, ArrayList<ArrayList<String>>> expectedVyosCommands;

    //Build a series of Vyos Configs representing the different router states.
    //These will used in read requests in place of actual responses from the Vyos router
    private void buildMockVyosConfigs() {
        mockVyosConfigs=new HashMap<String, String>();
        mockVyosConfigs.put("initial", "set firewall 'group'\nset firewall name eth0_Egress default-action 'accept'\nset firewall name eth0_Egress rule 1 action 'accept'\nset firewall name "
                + "eth0_Egress rule 1 description 'defaultFirewallRule'\nset firewall name eth0_Egress rule 1 state established 'enable'\nset firewall name eth0_Egress rule 1 state related "
                + "'enable'\nset firewall name eth0_Ingress default-action 'drop'\nset firewall name eth0_Ingress rule 1 action 'accept'\nset firewall name eth0_Ingress rule 1 description "
                + "'defaultFirewallRule'\nset firewall name eth0_Ingress rule 1 state established 'enable'\nset firewall name eth0_Ingress rule 1 state related 'enable'\nset firewall name "
                + "eth0_local default-action 'drop'\nset firewall name eth0_local rule 1 action 'accept'\nset firewall name eth0_local rule 1 description 'defaultFirewallRule'\nset firewall "
                + "name eth0_local rule 1 state established 'enable'\nset firewall name eth0_local rule 1 state related 'enable'\nset firewall name eth0_local rule 2 action 'accept'\nset "
                + "firewall name eth0_local rule 2 description 'allowSSHToRouter'\nset firewall name eth0_local rule 2 destination port '22'\nset firewall name eth0_local rule 2 protocol 'tcp'\n"
                + "set firewall name eth0_local rule 2 state new 'enable'\nset interfaces ethernet eth0 address '192.168.2.91/24'\nset interfaces ethernet eth0 duplex 'auto'\nset interfaces "
                + "ethernet eth0 firewall in name 'eth0_Ingress'\nset interfaces ethernet eth0 firewall local name 'eth0_local'\nset interfaces ethernet eth0 firewall out name 'eth0_Egress'\nset "
                + "interfaces ethernet eth0 smp-affinity 'auto'\nset interfaces ethernet eth0 speed 'auto'\nset interfaces ethernet 'eth1'\nset interfaces loopback 'lo'\nset nat 'destination'\nset "
                + "nat 'source'\nset service ssh port '22'\nset system config-management commit-revisions '20'\nset system gateway-address '192.168.2.1'\nset system host-name 'vyos'\nset system "
                + "login user vyos authentication encrypted-password '$6$nJEIsPOST9$BcvgGh7GkjVsne0BDRrD8JI4TYMiBla7mvgbTGh2Bq7w9xdHFVuDBxxtZn6ijkveVk1Mp6qQYCqnsZg1sRulI1'\nset system login "
                + "user vyos authentication plaintext-password ''\nset system login user vyos level 'admin'\nset system name-server '192.168.2.1'\nset system ntp server '0.pool.ntp.org'\nset "
                + "system ntp server '1.pool.ntp.org'\nset system ntp server '2.pool.ntp.org'\nset system syslog global facility all level 'notice'\nset system syslog global facility protocols "
                + "level 'debug'\nset system time-zone 'UTC'\n");

        mockVyosConfigs.put("withImplementedGuestNetwork", "set firewall 'group'\nset firewall name eth0_Egress default-action 'accept'\nset firewall name eth0_Egress rule 1 action 'accept'\nset "
                + "firewall name eth0_Egress rule 1 description 'defaultFirewallRule'\nset firewall name eth0_Egress rule 1 state established 'enable'\nset firewall name eth0_Egress rule 1 state "
                + "related 'enable'\nset firewall name eth0_Ingress default-action 'drop'\nset firewall name eth0_Ingress rule 1 action 'accept'\nset firewall name eth0_Ingress rule 1 description "
                + "'defaultFirewallRule'\nset firewall name eth0_Ingress rule 1 state established 'enable'\nset firewall name eth0_Ingress rule 1 state related 'enable'\nset firewall name "
                + "eth0_local default-action 'drop'\nset firewall name eth0_local rule 1 action 'accept'\nset firewall name eth0_local rule 1 description 'defaultFirewallRule'\nset firewall name "
                + "eth0_local rule 1 state established 'enable'\nset firewall name eth0_local rule 1 state related 'enable'\nset firewall name eth0_local rule 2 action 'accept'\nset firewall name "
                + "eth0_local rule 2 description 'allowSSHToRouter'\nset firewall name eth0_local rule 2 destination port '22'\nset firewall name eth0_local rule 2 protocol 'tcp'\nset firewall name "
                + "eth0_local rule 2 state new 'enable'\nset firewall name eth1_Egress default-action 'accept'\nset firewall name eth1_Egress rule 1 action 'accept'\nset firewall name eth1_Egress "
                + "rule 1 description '192.168.99.102'\nset firewall name eth1_Egress rule 1 state established 'enable'\nset firewall name eth1_Egress rule 1 state related 'enable'\nset firewall name "
                + "eth1_Ingress default-action 'drop'\nset firewall name eth1_Ingress rule 1 action 'accept'\nset firewall name eth1_Ingress rule 1 description '192.168.99.102'\nset firewall name "
                + "eth1_Ingress rule 1 state established 'enable'\nset firewall name eth1_Ingress rule 1 state related 'enable'\nset interfaces ethernet eth0 address '192.168.2.91/24'\nset interfaces "
                + "ethernet eth0 address '192.168.99.102/32'\nset interfaces ethernet eth0 duplex 'auto'\nset interfaces ethernet eth0 firewall in name 'eth0_Ingress'\nset interfaces ethernet eth0 "
                + "firewall local name 'eth0_local'\nset interfaces ethernet eth0 firewall out name 'eth0_Egress'\nset interfaces ethernet eth0 smp-affinity 'auto'\nset interfaces ethernet eth0 "
                + "speed 'auto'\nset interfaces ethernet eth1 vif 3954 address '10.3.96.1/24'\nset interfaces ethernet eth1 vif 3954 firewall in name 'eth1_Egress'\nset interfaces ethernet eth1 "
                + "vif 3954 firewall out name 'eth1_Ingress'\nset interfaces loopback 'lo'\nset nat 'destination'\nset nat source rule 9999 description '3954'\nset nat source rule 9999 "
                + "outbound-interface 'eth0'\nset nat source rule 9999 source address '10.3.96.1/24'\nset nat source rule 9999 translation address 'masquerade'\nset service ssh port '22'\nset "
                + "system config-management commit-revisions '20'\nset system gateway-address '192.168.2.1'\nset system host-name 'vyos'\nset system login user vyos authentication encrypted-password "
                + "'$6$nJEIsPOST9$BcvgGh7GkjVsne0BDRrD8JI4TYMiBla7mvgbTGh2Bq7w9xdHFVuDBxxtZn6ijkveVk1Mp6qQYCqnsZg1sRulI1'\nset system login user vyos authentication plaintext-password ''\nset system "
                + "login user vyos level 'admin'\nset system name-server '192.168.2.1'\nset system ntp server '0.pool.ntp.org'\nset system ntp server '1.pool.ntp.org'\nset system ntp server "
                + "'2.pool.ntp.org'\nset system syslog global facility all level 'notice'\nset system syslog global facility protocols level 'debug'\nset system time-zone 'UTC'\n");

        mockVyosConfigs.put("withAllRules", "set firewall group network-group eth0_Egress-2 network '10.3.96.0/24'\nset firewall group network-group 'eth0_Ingress-2'\nset firewall group network-group "
                + "eth1_Egress-2 network '10.3.96.0/24'\nset firewall group network-group 'eth1_Ingress-2'\nset firewall name eth0_Egress default-action 'accept'\nset firewall name eth0_Egress rule 1 "
                + "action 'accept'\nset firewall name eth0_Egress rule 1 description 'defaultFirewallRule'\nset firewall name eth0_Egress rule 1 state established 'enable'\nset firewall name "
                + "eth0_Egress rule 1 state related 'enable'\nset firewall name eth0_Egress rule 2 action 'accept'\nset firewall name eth0_Egress rule 2 description '0_3954'\nset firewall name "
                + "eth0_Egress rule 2 destination port '80'\nset firewall name eth0_Egress rule 2 protocol 'tcp'\nset firewall name eth0_Egress rule 2 source group network-group 'eth0_Egress-2'\nset "
                + "firewall name eth0_Egress rule 2 state new 'enable'\nset firewall name eth0_Ingress default-action 'drop'\nset firewall name eth0_Ingress rule 1 action 'accept'\nset firewall name "
                + "eth0_Ingress rule 1 description 'defaultFirewallRule'\nset firewall name eth0_Ingress rule 1 state established 'enable'\nset firewall name eth0_Ingress rule 1 state related "
                + "'enable'\nset firewall name eth0_Ingress rule 2 action 'accept'\nset firewall name eth0_Ingress rule 2 description '8'\nset firewall name eth0_Ingress rule 2 destination address "
                + "'10.3.96.0/24'\nset firewall name eth0_Ingress rule 2 destination port '22'\nset firewall name eth0_Ingress rule 2 protocol 'tcp'\nset firewall name eth0_Ingress rule 2 state new "
                + "'enable'\nset firewall name eth0_local default-action 'drop'\nset firewall name eth0_local rule 1 action 'accept'\nset firewall name eth0_local rule 1 description "
                + "'defaultFirewallRule'\nset firewall name eth0_local rule 1 state established 'enable'\nset firewall name eth0_local rule 1 state related 'enable'\nset firewall name eth0_local rule 2 "
                + "action 'accept'\nset firewall name eth0_local rule 2 description 'allowSSHToRouter'\nset firewall name eth0_local rule 2 destination port '22'\nset firewall name eth0_local rule 2 "
                + "protocol 'tcp'\nset firewall name eth0_local rule 2 state new 'enable'\nset firewall name eth1_Egress default-action 'accept'\nset firewall name eth1_Egress rule 1 action 'accept'\n"
                + "set firewall name eth1_Egress rule 1 description '192.168.99.102'\nset firewall name eth1_Egress rule 1 state established 'enable'\nset firewall name eth1_Egress rule 1 state "
                + "related 'enable'\nset firewall name eth1_Egress rule 2 action 'accept'\nset firewall name eth1_Egress rule 2 description '0_3954'\nset firewall name eth1_Egress rule 2 destination "
                + "port '80'\nset firewall name eth1_Egress rule 2 protocol 'tcp'\nset firewall name eth1_Egress rule 2 source group network-group 'eth1_Egress-2'\nset firewall name eth1_Egress rule 2 "
                + "state new 'enable'\nset firewall name eth1_Ingress default-action 'drop'\nset firewall name eth1_Ingress rule 1 action 'accept'\nset firewall name eth1_Ingress rule 1 description "
                + "'192.168.99.102'\nset firewall name eth1_Ingress rule 1 state established 'enable'\nset firewall name eth1_Ingress rule 1 state related 'enable'\nset firewall name eth1_Ingress "
                + "rule 2 action 'accept'\nset firewall name eth1_Ingress rule 2 description '8'\nset firewall name eth1_Ingress rule 2 destination address '10.3.96.0/24'\nset firewall name eth1_Ingress "
                + "rule 2 destination port '22'\nset firewall name eth1_Ingress rule 2 protocol 'tcp'\nset firewall name eth1_Ingress rule 2 state new 'enable'\nset interfaces ethernet eth0 address "
                + "'192.168.2.91/24'\nset interfaces ethernet eth0 address '192.168.99.102/32'\nset interfaces ethernet eth0 address '192.168.99.103/32'\nset interfaces ethernet eth0 duplex 'auto'\nset "
                + "interfaces ethernet eth0 firewall in name 'eth0_Ingress'\nset interfaces ethernet eth0 firewall local name 'eth0_local'\nset interfaces ethernet eth0 firewall out name "
                + "'eth0_Egress'\nset interfaces ethernet eth0 smp-affinity 'auto'\nset interfaces ethernet eth0 speed 'auto'\nset interfaces ethernet eth1 vif 3954 address '10.3.96.1/24'\nset "
                + "interfaces ethernet eth1 vif 3954 firewall in name 'eth1_Egress'\nset interfaces ethernet eth1 vif 3954 firewall out name 'eth1_Ingress'\nset interfaces loopback 'lo'\nset nat "
                + "destination rule 1 description '9'\nset nat destination rule 1 destination address '192.168.99.103'\nset nat destination rule 1 inbound-interface 'eth0'\nset nat destination rule 1 "
                + "translation address '10.3.96.3'\nset nat destination rule 2 description '10'\nset nat destination rule 2 destination address '192.168.99.102'\nset nat destination rule 2 destination "
                + "port '22'\nset nat destination rule 2 inbound-interface 'eth0'\nset nat destination rule 2 protocol 'tcp'\nset nat destination rule 2 translation address '10.3.96.2'\nset nat "
                + "destination rule 2 translation port '22'\nset nat source rule 1 description '9'\nset nat source rule 1 outbound-interface 'eth0'\nset nat source rule 1 source address '10.3.96.3'\nset "
                + "nat source rule 1 translation address '192.168.99.103'\nset nat source rule 9999 description '3954'\nset nat source rule 9999 outbound-interface 'eth0'\nset nat source rule 9999 source "
                + "address '10.3.96.1/24'\nset nat source rule 9999 translation address 'masquerade'\nset service ssh port '22'\nset system config-management commit-revisions '20'\nset system "
                + "gateway-address '192.168.2.1'\nset system host-name 'vyos'\nset system login user vyos authentication encrypted-password "
                + "'$6$nJEIsPOST9$BcvgGh7GkjVsne0BDRrD8JI4TYMiBla7mvgbTGh2Bq7w9xdHFVuDBxxtZn6ijkveVk1Mp6qQYCqnsZg1sRulI1'\nset system login user vyos authentication plaintext-password ''\nset system "
                + "login user vyos level 'admin'\nset system name-server '192.168.2.1'\nset system ntp server '0.pool.ntp.org'\nset system ntp server '1.pool.ntp.org'\nset system ntp server "
                + "'2.pool.ntp.org'\nset system syslog global facility all level 'notice'\nset system syslog global facility protocols level 'debug'\nset system time-zone 'UTC'\n");
    }

    //For each Unit test type build a multidimensional arraylist of the expected commands in the expected order that we should send them to the vyos router
    private void buildExpectedVyosCommands() {
        expectedVyosCommands= new HashMap<String, ArrayList<ArrayList<String>>>();
        //implementGuestNetwork
        ArrayList<ArrayList<String>> implementCommands=new ArrayList<ArrayList<String>>();

        //Command list #1
        ArrayList<String> curCommands=new ArrayList<String>();
        curCommands.add("set interfaces ethernet eth1 vif 3954 address 10.3.96.1/24");
        implementCommands.add(curCommands);

        //Command list #2
        curCommands=new ArrayList<String>();
        curCommands.add("set firewall name eth1_Ingress default-action 'drop'");
        curCommands.add("set firewall name eth1_Ingress rule 1 action 'accept'");
        curCommands.add("set firewall name eth1_Ingress rule 1 state established 'enable'");
        curCommands.add("set firewall name eth1_Ingress rule 1 state related 'enable'");
        curCommands.add("set firewall name eth1_Ingress rule 1 description '192.168.99.102'");
        curCommands.add("set interfaces ethernet eth1 vif 3954 firewall out name eth1_Ingress");
        implementCommands.add(curCommands);

        //Command list #3
        curCommands=new ArrayList<String>();
        curCommands.add("set firewall name eth1_Egress default-action 'accept'");
        curCommands.add("set firewall name eth1_Egress rule 1 action 'accept'");
        curCommands.add("set firewall name eth1_Egress rule 1 state established 'enable'");
        curCommands.add("set firewall name eth1_Egress rule 1 state related 'enable'");
        curCommands.add("set firewall name eth1_Egress rule 1 description '192.168.99.102'");
        curCommands.add("set interfaces ethernet eth1 vif 3954 firewall in name eth1_Egress");
        implementCommands.add(curCommands);

        //Command list #4
        curCommands=new ArrayList<String>();
        curCommands.add("set interfaces ethernet eth0 address 192.168.99.102/32");
        implementCommands.add(curCommands);

        //Command list #5
        curCommands=new ArrayList<String>();
        curCommands.add("set nat source rule 9999 outbound-interface 'eth0'");
        curCommands.add("set nat source rule 9999 source address '10.3.96.1/24'");
        curCommands.add("set nat source rule 9999 translation address masquerade");
        curCommands.add("set nat source rule 9999 description '3954'");
        implementCommands.add(curCommands);

        this.expectedVyosCommands.put("implementGuestNetwork", implementCommands);


        //addIngressFirewallRule
        ArrayList<ArrayList<String>> addIngressCommands=new ArrayList<ArrayList<String>>();

        //Command list #1
        curCommands=new ArrayList<String>();
        curCommands.add("set firewall name eth1_Ingress rule 2 action 'accept'");
        curCommands.add("set firewall name eth1_Ingress rule 2 protocol 'tcp'");
        curCommands.add("set firewall name eth1_Ingress rule 2 state new 'enable'");
        curCommands.add("set firewall name eth1_Ingress rule 2 description '8'");
        curCommands.add("set firewall name eth0_Ingress rule 2 action 'accept'");
        curCommands.add("set firewall name eth0_Ingress rule 2 protocol 'tcp'");
        curCommands.add("set firewall name eth0_Ingress rule 2 state new 'enable'");
        curCommands.add("set firewall name eth0_Ingress rule 2 description '8'");
        curCommands.add("set firewall name eth1_Ingress rule 2 destination port '22'");
        curCommands.add("set firewall name eth0_Ingress rule 2 destination port '22'");
        curCommands.add("set firewall group network-group 'eth1_Ingress-2'");
        curCommands.add("set firewall group network-group 'eth0_Ingress-2'");
        curCommands.add("set firewall name eth1_Ingress rule 2 destination address '10.3.96.0/24'");
        curCommands.add("set firewall name eth0_Ingress rule 2 destination address '10.3.96.0/24'");

        addIngressCommands.add(curCommands);
        expectedVyosCommands.put("addIngressFirewallRule", addIngressCommands);


        //addEgressFirewallRule
        ArrayList<ArrayList<String>> addEgressCommands=new ArrayList<ArrayList<String>>();

        //Command list #1
        curCommands=new ArrayList<String>();
        curCommands.add("set firewall name eth1_Egress rule 2 action 'accept'");
        curCommands.add("set firewall name eth1_Egress rule 2 protocol 'tcp'");
        curCommands.add("set firewall name eth1_Egress rule 2 state new 'enable'");
        curCommands.add("set firewall name eth1_Egress rule 2 description '0_3954'");
        curCommands.add("set firewall name eth0_Egress rule 2 action 'accept'");
        curCommands.add("set firewall name eth0_Egress rule 2 protocol 'tcp'");
        curCommands.add("set firewall name eth0_Egress rule 2 state new 'enable'");
        curCommands.add("set firewall name eth0_Egress rule 2 description '0_3954'");
        curCommands.add("set firewall name eth1_Egress rule 2 destination port '80'");
        curCommands.add("set firewall name eth0_Egress rule 2 destination port '80'");
        curCommands.add("set firewall group network-group 'eth1_Egress-2'");
        curCommands.add("set firewall group network-group 'eth0_Egress-2'");
        curCommands.add("set firewall group network-group eth1_Egress-2 network '10.3.96.0/24'");
        curCommands.add("set firewall group network-group eth0_Egress-2 network '10.3.96.0/24'");
        curCommands.add("set firewall name eth1_Egress rule 2 source group network-group 'eth1_Egress-2'");
        curCommands.add("set firewall name eth0_Egress rule 2 source group network-group 'eth0_Egress-2'");

        addEgressCommands.add(curCommands);
        expectedVyosCommands.put("addEgressFirewallRule", addEgressCommands);


        //addStaticNatRule
        ArrayList<ArrayList<String>> addStaticNatCommands=new ArrayList<ArrayList<String>>();

        //Command list #1
        curCommands=new ArrayList<String>();
        curCommands.add("set interfaces ethernet eth0 address 192.168.99.103/32");
        addStaticNatCommands.add(curCommands);

        //Command list #2
        curCommands=new ArrayList<String>();
        curCommands.add("set nat source rule 1 source address '10.3.96.3'");
        curCommands.add("set nat source rule 1 outbound-interface 'eth0'");
        curCommands.add("set nat source rule 1 translation address '192.168.99.103'");
        curCommands.add("set nat source rule 1 description '9'");
        addStaticNatCommands.add(curCommands);

        //Command list #3
        curCommands=new ArrayList<String>();
        curCommands.add("set nat destination rule 1 inbound-interface 'eth0'");
        curCommands.add("set nat destination rule 1 destination address '192.168.99.103'");
        curCommands.add("set nat destination rule 1 translation address '10.3.96.3'");
        curCommands.add("set nat destination rule 1 description '9'");
        addStaticNatCommands.add(curCommands);

        expectedVyosCommands.put("addStaticNatRule", addStaticNatCommands);


        //addPortForwardingRule
        ArrayList<ArrayList<String>> addPortForwardingCommands=new ArrayList<ArrayList<String>>();

        //Command list #1
        curCommands=new ArrayList<String>();
        curCommands.add("set nat destination rule 1 translation port '22'");
        curCommands.add("set nat destination rule 1 inbound-interface 'eth0'");
        curCommands.add("set nat destination rule 1 protocol 'tcp'");
        curCommands.add("set nat destination rule 1 translation address '10.3.96.2'");
        curCommands.add("set nat destination rule 1 description '10'");
        curCommands.add("set nat destination rule 1 destination address '192.168.99.102'");
        curCommands.add("set nat destination rule 1 destination port '22'");
        addPortForwardingCommands.add(curCommands);

        expectedVyosCommands.put("addPortForwardingRule", addPortForwardingCommands);


        //removePortForwardingRule
        ArrayList<ArrayList<String>> removePortForwardingCommands=new ArrayList<ArrayList<String>>();

        //Command list #1
        curCommands=new ArrayList<String>();
        curCommands.add("delete nat destination rule 2");
        removePortForwardingCommands.add(curCommands);

        expectedVyosCommands.put("removePortForwardingRule", removePortForwardingCommands);


        //removeStaticNatRule
        ArrayList<ArrayList<String>> removeStaticNatCommands=new ArrayList<ArrayList<String>>();

        //Command list #1
        curCommands=new ArrayList<String>();
        curCommands.add("delete nat destination rule 1");
        curCommands.add("delete nat source rule 1");
        removeStaticNatCommands.add(curCommands);

        //Command list #2
        curCommands=new ArrayList<String>();
        curCommands.add("delete interfaces ethernet eth0 address 192.168.99.103/32");
        removeStaticNatCommands.add(curCommands);

        expectedVyosCommands.put("removeStaticNatRule", removeStaticNatCommands);


        //removeEgressFirewallRule
        ArrayList<ArrayList<String>> removeEgressCommands=new ArrayList<ArrayList<String>>();

        //Command list #1
        curCommands=new ArrayList<String>();
        curCommands.add("delete interface ethernet eth1 vif 3954 firewall in");
        curCommands.add("delete interface ethernet eth0 firewall out");
        removeEgressCommands.add(curCommands);


        //Command list #2
        curCommands=new ArrayList<String>();
        curCommands.add("delete firewall name eth1_Egress rule 2");
        curCommands.add("delete firewall name eth0_Egress rule 2");
        removeEgressCommands.add(curCommands);


        //Command list #3
        curCommands=new ArrayList<String>();
        curCommands.add("delete firewall group network-group 'eth1_Egress-2'");
        curCommands.add("delete firewall group network-group 'eth0_Egress-2'");
        removeEgressCommands.add(curCommands);


        //Command list #4
        curCommands=new ArrayList<String>();
        curCommands.add("set interface ethernet eth1 vif 3954 firewall in name eth1_Egress");
        curCommands.add("set interface ethernet eth0 firewall out name eth0_Egress");
        removeEgressCommands.add(curCommands);

        expectedVyosCommands.put("removeEgressFirewallRule", removeEgressCommands);


        //removeIngressFirewallRule
        ArrayList<ArrayList<String>> removeIngressCommands=new ArrayList<ArrayList<String>>();

        //Command list #1
        curCommands=new ArrayList<String>();
        curCommands.add("delete interface ethernet eth1 vif 3954 firewall out");
        curCommands.add("delete interface ethernet eth0 firewall in");
        removeIngressCommands.add(curCommands);

        //Command list #2
        curCommands=new ArrayList<String>();
        curCommands.add("delete firewall name eth1_Ingress rule 2");
        curCommands.add("delete firewall name eth0_Ingress rule 2");
        removeIngressCommands.add(curCommands);

        //Command list #3
        curCommands=new ArrayList<String>();
        curCommands.add("delete firewall group network-group 'eth1_Ingress-2'");
        curCommands.add("delete firewall group network-group 'eth0_Ingress-2'");
        removeIngressCommands.add(curCommands);

        //Command list #4
        curCommands=new ArrayList<String>();
        curCommands.add("set interface ethernet eth1 vif 3954 firewall out name eth1_Ingress");
        curCommands.add("set interface ethernet eth0 firewall in name eth0_Ingress");
        removeIngressCommands.add(curCommands);

        expectedVyosCommands.put("removeIngressFirewallRule", removeIngressCommands);


        //shutdownGuestNetwork
        ArrayList<ArrayList<String>> shutdownCommands=new ArrayList<ArrayList<String>>();

        //Command list #1
        curCommands=new ArrayList<String>();
        curCommands.add("delete nat source rule 9999");
        shutdownCommands.add(curCommands);

        //Command list #2
        curCommands=new ArrayList<String>();
        curCommands.add("delete interfaces ethernet eth0 address 192.168.99.102/32");
        shutdownCommands.add(curCommands);

        //Command list #3
        curCommands=new ArrayList<String>();
        curCommands.add("delete interfaces ethernet eth1 vif 3954 firewall out");
        shutdownCommands.add(curCommands);

        //Command list #4
        curCommands=new ArrayList<String>();
        curCommands.add("delete firewall name eth1_Ingress");
        shutdownCommands.add(curCommands);

        //Command list #5
        curCommands=new ArrayList<String>();
        curCommands.add("delete interfaces ethernet eth1 vif 3954 firewall in");
        shutdownCommands.add(curCommands);

        //Command list #6
        curCommands=new ArrayList<String>();
        curCommands.add("delete firewall name eth1_Egress");
        shutdownCommands.add(curCommands);

        //Command list #7
        curCommands=new ArrayList<String>();
        curCommands.add("delete interfaces ethernet eth1 vif 3954");
        shutdownCommands.add(curCommands);

        expectedVyosCommands.put("shutdownGuestNetwork", shutdownCommands);


    }

    private boolean validateCorrectVyosCommands(ArrayList<String> vyosCommands) {
        //get the list of expected commands for the current unit test.
        ArrayList<String> expectedCommands=this.expectedVyosCommands.get(context.get("currentTest")).get(this.commandSetCount);
        if (expectedCommands.size() != vyosCommands.size() ) {
            return false;
        }

        for (int i=0; i<expectedCommands.size(); i++) {
            if (!expectedCommands.get(i).trim().equals(vyosCommands.get(i).trim())) {
                return false;
            }
        }
        return true;

    }

    private void printVyosCommandList(ArrayList<String> commands) {

        for (String curCommand : commands) {
            System.out.println(curCommand);
        }

    }

    /* Fake the calls to the Vyos Router */
    @Override
    protected String request(VyosRouterMethod method,
            VyosRouterCommandType commandType, ArrayList<String> commands)
            throws ExecutionException {
        if (method != VyosRouterMethod.SHELL
                && method != VyosRouterMethod.HTTPSTUB) {
            throw new ExecutionException(
                    "Invalid method used to access the Vyos Router.");
        }

        String response = "";

        // Allow the tests to be run against an actual vyos instance instead of
        // returning mock results.
        if (context.containsKey("use_test_router")
                && context.get("use_test_router").equals("true")) {
            // System.out.println("Testing against an actual Vyos instance.");
            response = super.request(method, commandType, commands);
            // System.out.println("response: "+response);
        } else {
            switch (commandType) {
                case READ :
                    //Get the proper vyos state for the current unit test.
                    response=this.mockVyosConfigs.get(context.get("vyosState"));
                    break;

                case WRITE :
                    int nextId=-1;
                    boolean isDescending=false;
                    ArrayList<String> vyosCommands=new ArrayList<String>();
                    for (String curCommand : commands) {
                        //Get Next available ID number for the current command set.
                        //For write operations the unique rule id number must be ascertained at write time to eliminate race conditions.
                        if (nextId == -1) {
                            if (curCommand.contains("{{ "+VyosNextIdNumber.ASCENDING+" }}") ) {
                                nextId=getVyosRuleNumber(curCommand, "", isDescending);
                            } else if (curCommand.contains("{{ "+VyosNextIdNumber.DESCENDING+" }}") ) {
                                isDescending=true;
                                nextId=getVyosRuleNumber(curCommand, "", isDescending);
                            }
                        }
                        if (nextId == 0) {
                            throw new ExecutionException("Could not determine a valid new Vyos rule number for the current Command: "+curCommand);
                        }

                        if (!isDescending && nextId != -1) {
                            curCommand=curCommand.replace("{{ "+VyosNextIdNumber.ASCENDING+" }}", String.valueOf(nextId));
                        } else if (isDescending && nextId != -1) {
                            curCommand=curCommand.replace("{{ "+VyosNextIdNumber.DESCENDING+" }}", String.valueOf(nextId));
                        }
                        vyosCommands.add(curCommand);

                    }
                    if (!validateCorrectVyosCommands(vyosCommands)) {
                        if (context.containsKey("enable_console_output") && context.get("enable_console_output").equals("true")) {
                            System.out.println("Expected Vyos Commands: ");
                            printVyosCommandList(this.expectedVyosCommands.get(context.get("currentTest")).get(this.commandSetCount));
                            System.out.println("Commands returned by the unit test: ");
                            printVyosCommandList(vyosCommands);
                        }
                        throw new ExecutionException("The current list of commands to execute do not match the expected commands. Current Unit Test: "+context.get("currentTest")+" Current Command Set: "+commandSetCount);
                    }
                    this.commandSetCount++;
                    break;

                case SAVE :

                    break;

                default :
                    throw new ExecutionException("ERROR command type not supported. Type: "+ commandType);

            }
           // System.out.println("Commands in current Request: ");
           // for (String curCommand : commands) {
           //     System.out.println(curCommand);
           // }

        }

        return response;
    }

/*
    @Override
    protected String getPrivateSubnet(long firewallRuleId) throws ExecutionException {
        if (context.containsKey("privateSubnet")) {
            return context.get("privateSubnet");
        } else {
            throw new ExecutionException("Error calling mock getPrivateSubnet method. No private subnet set in the context hashmap.");
        }

    }
*/

 /*
    @Override
    protected String getGuestVlanTag(long firewallRuleId) throws ExecutionException {
        if (context.containsKey("guestVlanTag")) {
            return context.get("guestVlanTag");
        } else {
            throw new ExecutionException("Error calling mock getGuestVlanTag method. No guestVlanTag set in the context hashmap.");
        }
    }
*/



}