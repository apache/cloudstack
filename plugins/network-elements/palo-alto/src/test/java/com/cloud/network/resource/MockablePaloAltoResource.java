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

import java.util.HashMap;
import java.util.Map;

import com.cloud.utils.exception.ExecutionException;
// http client handling
// for prettyFormat()

public class MockablePaloAltoResource extends PaloAltoResource {
    private HashMap<String, String> context;

    public void setMockContext(HashMap<String, String> context) {
        this.context = context;
    }

    /* Fake the calls to the Palo Alto API */
    @Override
    protected String request(PaloAltoMethod method, Map<String, String> params) throws ExecutionException {
        if (method != PaloAltoMethod.GET && method != PaloAltoMethod.POST) {
            throw new ExecutionException("Invalid http method used to access the Palo Alto API.");
        }

        String response = "";

        // 'keygen' request
        if (params.containsKey("type") && params.get("type").equals("keygen")) {
            response = "<response status = 'success'><result><key>LUFRPT14MW5xOEo1R09KVlBZNnpnemh0VHRBOWl6TGM9bXcwM3JHUGVhRlNiY0dCR0srNERUQT09</key></result></response>";
        }

        // 'config' requests
        if (params.containsKey("type") && params.get("type").equals("config") && params.containsKey("action")) {
            // action = 'get'
            if (params.get("action").equals("get")) {
                // get interface for type
                // | public_using_ethernet
                if (params.get("xpath").equals("/config/devices/entry/network/interface/ethernet/entry[@name='ethernet1/1']")) {
                    if (context.containsKey("public_using_ethernet") && context.get("public_using_ethernet").equals("true")) {
                        context.put("public_interface_type", "ethernet");
                        response =
                            "<response status=\"success\" code=\"19\"><result total-count=\"1\" count=\"1\"><entry name=\"ethernet1/1\" admin=\"admin\" time=\"2013/06/18 "
                                +
                                "13:33:56\"><layer3 admin=\"admin\" time=\"2013/06/18 13:33:56\"><ipv6><neighbor-discovery><router-advertisement><enable>no</enable>"
                                +
                                "<min-interval>200</min-interval><max-interval>600</max-interval><hop-limit>64</hop-limit><reachable-time>unspecified</reachable-time>"
                                +
                                "<retransmission-timer>unspecified</retransmission-timer><lifetime>1800</lifetime><managed-flag>no</managed-flag>"
                                +
                                "<other-flag>no</other-flag><enable-consistency-check>no</enable-consistency-check><link-mtu>unspecified</link-mtu></router-advertisement>"
                                +
                                "<enable-dad>no</enable-dad><reachable-time>30</reachable-time><ns-interval>1</ns-interval><dad-attempts>1</dad-attempts></neighbor-discovery>"+
                                "<enabled>no</enabled><interface-id>EUI-64</interface-id></ipv6><untagged-sub-interface>no</untagged-sub-interface>"
                                +
                                "<units admin=\"admin\" time=\"2013/06/18 13:33:56\"><entry name=\"ethernet1/1.9999\" admin=\"admin\" time=\"2013/06/18 13:33:56\">"
                                +
                                "<ipv6><neighbor-discovery><router-advertisement><enable>no</enable><min-interval>200</min-interval><max-interval>600</max-interval>"
                                +
                                "<hop-limit>64</hop-limit><reachable-time>unspecified</reachable-time><retransmission-timer>unspecified</retransmission-timer><lifetime>"
                                +
                                "1800</lifetime><managed-flag>no</managed-flag><other-flag>no</other-flag><enable-consistency-check>no</enable-consistency-check>" +
                                "<link-mtu>unspecified</link-mtu></router-advertisement><enable-dad>no</enable-dad><reachable-time>30</reachable-time><ns-interval>" +
                                "1</ns-interval><dad-attempts>1</dad-attempts></neighbor-discovery><enabled>no</enabled><interface-id>EUI-64</interface-id></ipv6>" +
                                "<ip admin=\"admin\" time=\"2013/06/18 13:33:56\"><entry name=\"192.168.80.254/24\"/></ip><adjust-tcp-mss>no</adjust-tcp-mss>" +
                                "<tag>3033</tag></entry></units></layer3><link-speed>auto</link-speed><link-duplex>auto</link-duplex><link-state>auto</link-state>" +
                                "</entry></result></response>";
                    } else {
                        response = "<response status=\"success\" code=\"19\"><result/></response>";
                    }
                } // | private_using_ethernet
                if (params.get("xpath").equals("/config/devices/entry/network/interface/ethernet/entry[@name='ethernet1/2']")) {
                    if (context.containsKey("private_using_ethernet") && context.get("private_using_ethernet").equals("true")) {
                        context.put("private_interface_type", "ethernet");
                        response =
                            "<response status=\"success\" code=\"19\"><result total-count=\"1\" count=\"1\"><entry name=\"ethernet1/2\" admin=\"admin\" "
                                + "time=\"2013/06/18 13:33:57\"><layer3 admin=\"admin\" time=\"2013/06/18 13:33:57\"><ipv6><neighbor-discovery><router-advertisement>"
                                + "<enable>no</enable><min-interval>200</min-interval><max-interval>600</max-interval><hop-limit>64</hop-limit>"
                                + "<reachable-time>unspecified</reachable-time><retransmission-timer>unspecified</retransmission-timer><lifetime>1800</lifetime>"
                                + "<managed-flag>no</managed-flag><other-flag>no</other-flag><enable-consistency-check>no</enable-consistency-check>"
                                + "<link-mtu>unspecified</link-mtu></router-advertisement><enable-dad>no</enable-dad><reachable-time>30</reachable-time>"
                                + "<ns-interval>1</ns-interval><dad-attempts>1</dad-attempts></neighbor-discovery><enabled>no</enabled><interface-id>EUI-64</interface-id>"
                                + "</ipv6><untagged-sub-interface>no</untagged-sub-interface><units admin=\"admin\" time=\"2013/06/18 13:33:57\"/></layer3>"
                                + "<link-speed>auto</link-speed><link-duplex>auto</link-duplex><link-state>auto</link-state></entry></result></response>";
                    } else {
                        response = "<response status=\"success\" code=\"19\"><result/></response>";
                    }
                }

                // get management profile | has_management_profile
                if (params.get("xpath").equals("/config/devices/entry/network/profiles/interface-management-profile/entry[@name='Ping']")) {
                    if (context.containsKey("has_management_profile") && context.get("has_management_profile").equals("true")) {
                        response =
                            "<response status=\"success\" code=\"19\"><result total-count=\"1\" count=\"1\"><entry name=\"Ping\"><ping>yes</ping></entry></result></response>";
                    } else {
                        response = "<response status=\"success\" code=\"19\"><result/></response>";
                    }
                }

                // get public interface IP | has_public_interface
                if (params.get("xpath").equals(
                    "/config/devices/entry/network/interface/ethernet/entry[@name='ethernet1/1']"
                        + "/layer3/units/entry[@name='ethernet1/1.9999']/ip/entry[@name='192.168.80.102/32']")) {
                    if (context.containsKey("has_public_interface") && context.get("has_public_interface").equals("true")) {
                        response =
                            "<response status=\"success\" code=\"19\"><result total-count=\"1\" count=\"1\">"
                                + "<entry name=\"192.168.80.102/32\" admin=\"admin\" time=\"2013/07/05 13:02:37\"/></result></response>";
                    } else {
                        response = "<response status=\"success\" code=\"19\"><result/></response>";
                    }
                }

                // get private interface | has_private_interface
                if (params.get("xpath")
                    .equals("/config/devices/entry/network/interface/ethernet/entry[@name='ethernet1/2']/layer3/units/entry[@name='ethernet1/2.3954']")) {
                    if (context.containsKey("has_private_interface") && context.get("has_private_interface").equals("true")) {
                        response =
                            "<response status=\"success\" code=\"19\"><result total-count=\"1\" count=\"1\">"
                                + "<entry name=\"ethernet1/2.3954\" admin=\"admin\" time=\"2013/07/05 13:02:36\"><tag admin=\"admin\" time=\"2013/07/05 13:02:36\">3954</tag>"
                                + "<ip><entry name=\"10.5.80.1/20\"/></ip><interface-management-profile>Ping</interface-management-profile></entry></result></response>";
                    } else {
                        response = "<response status=\"success\" code=\"19\"><result/></response>";
                    }
                }

                // get private interface ip
                if (params.get("xpath").equals(
                    "/config/devices/entry/network/interface/ethernet/entry" + "[@name='ethernet1/2']/layer3/units/entry[@name='ethernet1/2.3954']/ip/entry")) {
                    response = "<response status=\"success\" code=\"19\"><result total-count=\"1\" count=\"1\"><entry name=\"10.3.96.1/20\"/></result></response>";
                }

                // get source nat | has_src_nat_rule
                if (params.get("xpath").equals("/config/devices/entry/vsys/entry[@name='vsys1']/rulebase/nat/rules/entry[@name='src_nat.3954']")) {
                    if (context.containsKey("has_src_nat_rule") && context.get("has_src_nat_rule").equals("true")) {
                        response =
                            "<response status=\"success\" code=\"19\"><result total-count=\"1\" count=\"1\">"
                                + "<entry name=\"src_nat.3954\" admin=\"admin\" time=\"2013/07/05 13:02:38\"><to admin=\"admin\" time=\"2013/07/05 13:02:38\">"
                                + "<member admin=\"admin\" time=\"2013/07/05 13:02:38\">untrust</member></to><from><member>trust</member></from><source>"
                                + "<member>10.5.80.1/20</member></source><destination><member>any</member></destination><service>any</service>"
                                + "<nat-type>ipv4</nat-type><to-interface>ethernet1/1.9999</to-interface><source-translation><dynamic-ip-and-port>"
                                + "<interface-address><ip>192.168.80.102/32</ip><interface>ethernet1/1.9999</interface></interface-address></dynamic-ip-and-port>"
                                + "</source-translation></entry></result></response>";
                    } else {
                        response = "<response status=\"success\" code=\"19\"><result/></response>";
                    }
                }

                // get isolation firewall rule | has_isolation_fw_rule
                if (params.get("xpath").equals("/config/devices/entry/vsys/entry[@name='vsys1']/rulebase/security/rules/entry[@name='isolate_3954']")) {
                    if (context.containsKey("has_isolation_fw_rule") && context.get("has_isolation_fw_rule").equals("true")) {
                        response =
                            "<response status=\"success\" code=\"19\"><result total-count=\"1\" count=\"1\">"
                                + "<entry name=\"isolate_3954\" admin=\"admin\" time=\"2013/07/05 13:02:38\"><from admin=\"admin\" time=\"2013/07/05 13:02:38\">"
                                + "<member admin=\"admin\" time=\"2013/07/05 13:02:38\">trust</member></from><to><member>trust</member></to><source>"
                                + "<member>10.5.80.0/20</member></source><destination><member>10.5.80.1</member></destination><application><member>any</member></application>"
                                + "<service><member>any</member></service><action>deny</action><negate-source>no</negate-source><negate-destination>yes</negate-destination>"
                                + "</entry></result></response>";
                    } else {
                        response = "<response status=\"success\" code=\"19\"><result/></response>";
                    }
                }

                // get service | has_service
                if (params.get("xpath").equals("/config/devices/entry/vsys/entry[@name='vsys1']/service/entry[@name='cs_tcp_80']")) {
                    if (context.containsKey("has_service_tcp_80") && context.get("has_service_tcp_80").equals("true")) {
                        response =
                            "<response status=\"success\" code=\"19\"><result total-count=\"1\" count=\"1\"><entry name=\"cs_tcp_80\">"
                                + "<protocol><tcp><port>80</port></tcp></protocol></entry></result></response>";
                    } else {
                        response = "<response status=\"success\" code=\"19\"><result/></response>";
                    }
                }

                // get egress firewall rule | has_egress_fw_rule | policy_0_3954
                if (params.get("xpath").equals("/config/devices/entry/vsys/entry[@name='vsys1']/rulebase/security/rules/entry[@name='policy_0_3954']")) {
                    if (context.containsKey("has_egress_fw_rule") && context.get("has_egress_fw_rule").equals("true")) {
                        response =
                            "<response status=\"success\" code=\"19\"><result total-count=\"1\" count=\"1\">"
                                + "<entry name=\"policy_0_3954\" admin=\"admin\" time=\"2013/07/03 12:43:30\"><from admin=\"admin\" time=\"2013/07/03 12:43:30\">"
                                + "<member admin=\"admin\" time=\"2013/07/03 12:43:30\">trust</member></from><to><member>untrust</member></to><source><member>10.3.96.1/20</member>"
                                + "</source><destination><member>any</member></destination><application><member>any</member></application><service><member>cs_tcp_80</member>"
                                + "</service><action>allow</action><negate-source>no</negate-source><negate-destination>no</negate-destination></entry></result></response>";
                    } else {
                        response = "<response status=\"success\" code=\"19\"><result/></response>";
                    }
                }

                // get ingress firewall rule | has_ingress_fw_rule | policy_8
                if (params.get("xpath").equals("/config/devices/entry/vsys/entry[@name='vsys1']/rulebase/security/rules/entry[@name='policy_8']")) {
                    if (context.containsKey("has_ingress_fw_rule") && context.get("has_ingress_fw_rule").equals("true")) {
                        response =
                            "<response status=\"success\" code=\"19\"><result total-count=\"1\" count=\"1\">"
                                + "<entry name=\"policy_8\" admin=\"admin\" time=\"2013/07/03 13:26:27\"><from admin=\"admin\" time=\"2013/07/03 13:26:27\">"
                                + "<member admin=\"admin\" time=\"2013/07/03 13:26:27\">untrust</member></from><to><member>trust</member></to><source><member>any</member>"
                                + "</source><destination><member>192.168.80.103</member></destination><application><member>any</member></application><service>"
                                + "<member>cs_tcp_80</member></service><action>allow</action><negate-source>no</negate-source><negate-destination>no</negate-destination>"
                                + "</entry></result></response>";
                    } else {
                        response = "<response status=\"success\" code=\"19\"><result/></response>";
                    }
                }

                // get default egress rule | policy_0_3954
                if (params.get("xpath").equals(
                    "/config/devices/entry/vsys/entry[@name='vsys1']/rulebase/security/rules/entry[contains(@name, 'policy') and contains(@name, '3954')]")) {
                    response = "<response status=\"success\" code=\"19\"><result/></response>";
                }

                // get destination nat rule (port forwarding) | has_dst_nat_rule
                if (params.get("xpath").equals("/config/devices/entry/vsys/entry[@name='vsys1']/rulebase/nat/rules/entry[@name='dst_nat.192-168-80-103_9']")) {
                    if (context.containsKey("has_dst_nat_rule") && context.get("has_dst_nat_rule").equals("true")) {
                        response =
                            "<response status=\"success\" code=\"19\"><result total-count=\"1\" count=\"1\">"
                                + "<entry name=\"dst_nat.192-168-80-103_9\" admin=\"admin\" time=\"2013/07/03 13:40:50\"><to admin=\"admin\" time=\"2013/07/03 13:40:50\">"
                                + "<member admin=\"admin\" time=\"2013/07/03 13:40:50\">untrust</member></to><from><member>untrust</member></from><source><member>any</member>"
                                + "</source><destination><member>192.168.80.103</member></destination><service>cs_tcp_80</service><nat-type>ipv4</nat-type>"
                                + "<to-interface>ethernet1/1.9999</to-interface><destination-translation><translated-address>10.3.97.158</translated-address>"
                                + "<translated-port>8080</translated-port></destination-translation></entry></result></response>";
                    } else {
                        response = "<response status=\"success\" code=\"19\"><result/></response>";
                    }
                }

                // get destination nat rules (returns all dst nat rules per ip)
                if (params.get("xpath").equals("/config/devices/entry/vsys/entry[@name='vsys1']/rulebase/nat/rules/entry[destination/member[text()='192.168.80.103']]")) {
                    if (context.containsKey("has_dst_nat_rule") && context.get("has_dst_nat_rule").equals("true")) {
                        response =
                            "<response status=\"success\" code=\"19\"><result total-count=\"1\" count=\"1\">"
                                + "<entry name=\"dst_nat.192-168-80-103_9\" admin=\"admin\" time=\"2013/07/03 13:40:50\"><to admin=\"admin\" time=\"2013/07/03 13:40:50\">"
                                + "<member admin=\"admin\" time=\"2013/07/03 13:40:50\">untrust</member></to><from><member>untrust</member></from><source><member>any</member>"
                                + "</source><destination><member>192.168.80.103</member></destination><service>cs_tcp_80</service><nat-type>ipv4</nat-type>"
                                + "<to-interface>ethernet1/1.9999</to-interface><destination-translation><translated-address>10.3.97.158</translated-address>"
                                + "<translated-port>8080</translated-port></destination-translation></entry></result></response>";
                    } else {
                        response = "<response status=\"success\" code=\"19\"><result/></response>";
                    }
                }

                // get static nat rule | has_stc_nat_rule
                if (params.get("xpath").equals("/config/devices/entry/vsys/entry[@name='vsys1']/rulebase/nat/rules/entry[@name='stc_nat.192-168-80-103_0']")) {
                    if (context.containsKey("has_stc_nat_rule") && context.get("has_stc_nat_rule").equals("true")) {
                        response =
                            "<response status=\"success\" code=\"19\"><result total-count=\"1\" count=\"1\">"
                                + "<entry name=\"stc_nat.192-168-80-103_0\" admin=\"admin\" time=\"2013/07/03 14:02:23\"><to admin=\"admin\" time=\"2013/07/03 14:02:23\">"
                                + "<member admin=\"admin\" time=\"2013/07/03 14:02:23\">untrust</member></to><from><member>untrust</member></from><source><member>any</member>"
                                + "</source><destination><member>192.168.80.103</member></destination><service>any</service><nat-type>ipv4</nat-type>"
                                + "<to-interface>ethernet1/1.9999</to-interface><destination-translation><translated-address>10.3.97.158</translated-address>"
                                + "</destination-translation></entry></result></response>";
                    } else {
                        response = "<response status=\"success\" code=\"19\"><result/></response>";
                    }
                }

            }

            // action = 'set'
            if (params.get("action").equals("set")) {
                // set management profile
                if (params.get("xpath").equals("/config/devices/entry/network/profiles/interface-management-profile/entry[@name='Ping']")) {
                    response = "<response status=\"success\" code=\"20\"><msg>command succeeded</msg></response>";
                    context.put("has_management_profile", "true");
                }

                // add private interface
                if (params.get("xpath")
                    .equals("/config/devices/entry/network/interface/ethernet/entry[@name='ethernet1/2']/layer3/units/entry[@name='ethernet1/2.3954']")) {
                    response = "<response status=\"success\" code=\"20\"><msg>command succeeded</msg></response>";
                    context.put("has_private_interface", "true");
                }

                // add public ip to public interface
                if (params.get("xpath").equals(
                    "/config/devices/entry/network/interface/ethernet/entry[@name='ethernet1/1']/layer3/units/entry[@name='ethernet1/1.9999']/ip")) {
                    response = "<response status=\"success\" code=\"20\"><msg>command succeeded</msg></response>";
                    context.put("has_public_interface", "true");
                }

                // add private interface to zone
                if (params.get("xpath").equals("/config/devices/entry/vsys/entry[@name='vsys1']/zone/entry[@name='trust']/network/layer3")) {
                    response = "<response status=\"success\" code=\"20\"><msg>command succeeded</msg></response>";
                }

                // add public interface to zone
                if (params.get("xpath").equals("/config/devices/entry/vsys/entry[@name='vsys1']/zone/entry[@name='untrust']/network/layer3")) {
                    response = "<response status=\"success\" code=\"20\"><msg>command succeeded</msg></response>";
                }

                // set virtual router (public | private)
                if (params.get("xpath").equals("/config/devices/entry/network/virtual-router/entry[@name='default']/interface")) {
                    response = "<response status=\"success\" code=\"20\"><msg>command succeeded</msg></response>";
                }

                // add interface to network (public | private)
                if (params.get("xpath").equals("/config/devices/entry/vsys/entry[@name='vsys1']/import/network/interface")) {
                    response = "<response status=\"success\" code=\"20\"><msg>command succeeded</msg></response>";
                }

                // add src nat rule
                if (params.get("xpath").equals("/config/devices/entry/vsys/entry[@name='vsys1']/rulebase/nat/rules/entry[@name='src_nat.3954']")) {
                    response = "<response status=\"success\" code=\"20\"><msg>command succeeded</msg></response>";
                    context.put("has_src_nat_rule", "true");
                }

                // add isolation firewall rule
                if (params.get("xpath").equals("/config/devices/entry/vsys/entry[@name='vsys1']/rulebase/security/rules/entry[@name='isolate_3954']")) {
                    response = "<response status=\"success\" code=\"20\"><msg>command succeeded</msg></response>";
                    context.put("has_isolation_fw_rule", "true");
                }

                // add egress firewall rule
                if (params.get("xpath").equals("/config/devices/entry/vsys/entry[@name='vsys1']/rulebase/security/rules/entry[@name='policy_0_3954']")) {
                    response = "<response status=\"success\" code=\"20\"><msg>command succeeded</msg></response>";
                    context.put("has_egress_fw_rule", "true");
                }

                // add ingress firewall rule
                if (params.get("xpath").equals("/config/devices/entry/vsys/entry[@name='vsys1']/rulebase/security/rules/entry[@name='policy_8']")) {
                    response = "<response status=\"success\" code=\"20\"><msg>command succeeded</msg></response>";
                    context.put("has_ingress_fw_rule", "true");
                }

                // add destination nat rule (port forwarding)
                if (params.get("xpath").equals("/config/devices/entry/vsys/entry[@name='vsys1']/rulebase/nat/rules/entry[@name='dst_nat.192-168-80-103_9']")) {
                    response = "<response status=\"success\" code=\"20\"><msg>command succeeded</msg></response>";
                    context.put("has_dst_nat_rule", "true");
                }

                // add static nat rule
                if (params.get("xpath").equals("/config/devices/entry/vsys/entry[@name='vsys1']/rulebase/nat/rules/entry[@name='stc_nat.192-168-80-103_0']")) {
                    response = "<response status=\"success\" code=\"20\"><msg>command succeeded</msg></response>";
                    context.put("has_stc_nat_rule", "true");
                }

                // add tcp 80 service
                if (params.get("xpath").equals("/config/devices/entry/vsys/entry[@name='vsys1']/service/entry[@name='cs_tcp_80']")) {
                    response = "<response status=\"success\" code=\"20\"><msg>command succeeded</msg></response>";
                    context.put("has_service_tcp_80", "true");
                }
            }

            // action = 'delete'
            if (params.get("action").equals("delete")) {
                // remove egress firewall rule
                if (params.get("xpath").equals("/config/devices/entry/vsys/entry[@name='vsys1']/rulebase/security/rules/entry[@name='policy_0_3954']")) {
                    response = "<response status=\"success\" code=\"20\"><msg>command succeeded</msg></response>";
                    context.remove("has_egress_fw_rule");
                }

                // remove ingress firewall rule
                if (params.get("xpath").equals("/config/devices/entry/vsys/entry[@name='vsys1']/rulebase/security/rules/entry[@name='policy_8']")) {
                    response = "<response status=\"success\" code=\"20\"><msg>command succeeded</msg></response>";
                    context.remove("has_ingress_fw_rule");
                }

                // remove destination nat rule
                if (params.get("xpath").equals("/config/devices/entry/vsys/entry[@name='vsys1']/rulebase/nat/rules/entry[@name='dst_nat.192-168-80-103_9']")) {
                    response = "<response status=\"success\" code=\"20\"><msg>command succeeded</msg></response>";
                    context.remove("has_dst_nat_rule");
                }

                // remove static nat rule
                if (params.get("xpath").equals("/config/devices/entry/vsys/entry[@name='vsys1']/rulebase/nat/rules/entry[@name='stc_nat.192-168-80-103_0']")) {
                    response = "<response status=\"success\" code=\"20\"><msg>command succeeded</msg></response>";
                    context.remove("has_dst_nat_rule");
                }

                // remove public ip from interface (dst_nat | stc_nat)
                if (params.get("xpath").equals(
                    "/config/devices/entry/network/interface/ethernet/entry[@name='ethernet1/1']/layer3/units/entry"
                        + "[@name='ethernet1/1.9999']/ip/entry[@name='192.168.80.103/32']")) {
                    response = "<response status=\"success\" code=\"20\"><msg>command succeeded</msg></response>";
                }

                // remove isolation firewall rule
                if (params.get("xpath").equals("/config/devices/entry/vsys/entry[@name='vsys1']/rulebase/security/rules/entry[@name='isolate_3954']")) {
                    response = "<response status=\"success\" code=\"20\"><msg>command succeeded</msg></response>";
                    context.remove("has_isolation_fw_rule");
                }

                // remove source nat rule
                if (params.get("xpath").equals("/config/devices/entry/vsys/entry[@name='vsys1']/rulebase/nat/rules/entry[@name='src_nat.3954']")) {
                    response = "<response status=\"success\" code=\"20\"><msg>command succeeded</msg></response>";
                    context.remove("has_src_nat_rule");
                }

                // remove public source nat ip
                if (params.get("xpath").equals(
                    "/config/devices/entry/network/interface/ethernet/entry[@name='ethernet1/1']/layer3/units/entry"
                        + "[@name='ethernet1/1.9999']/ip/entry[@name='192.168.80.102/32']")) {
                    response = "<response status=\"success\" code=\"20\"><msg>command succeeded</msg></response>";
                    context.remove("has_public_interface");
                }

                // remove private interface from the zone
                if (params.get("xpath").equals(
                    "/config/devices/entry/vsys/entry[@name='vsys1']/zone/entry[@name='trust']/network/layer3/member[text()='ethernet1/2.3954']")) {
                    response = "<response status=\"success\" code=\"20\"><msg>command succeeded</msg></response>";
                }

                // remove private interface from the virtual router
                if (params.get("xpath").equals("/config/devices/entry/network/virtual-router/entry[@name='default']/interface/member[text()='ethernet1/2.3954']")) {
                    response = "<response status=\"success\" code=\"20\"><msg>command succeeded</msg></response>";
                }

                // remove private interface from network
                if (params.get("xpath").equals("/config/devices/entry/vsys/entry[@name='vsys1']/import/network/interface/member[text()='ethernet1/2.3954']")) {
                    response = "<response status=\"success\" code=\"20\"><msg>command succeeded</msg></response>";
                }

                // remove private interface
                if (params.get("xpath")
                    .equals("/config/devices/entry/network/interface/ethernet/entry[@name='ethernet1/2']/layer3/units/entry[@name='ethernet1/2.3954']")) {
                    response = "<response status=\"success\" code=\"20\"><msg>command succeeded</msg></response>";
                    context.remove("has_private_interface");
                }

            }
        } // end 'config'

        // 'op' requests
        if (params.containsKey("type") && params.get("type").equals("op")) {
            // check if there are pending changes
            if (params.get("cmd").equals("<check><pending-changes></pending-changes></check>")) {
                if (context.containsKey("firewall_has_pending_changes") && context.get("firewall_has_pending_changes").equals("true")) {
                    response = "<response status=\"success\"><result>yes</result></response>";
                } else {
                    response = "<response status=\"success\"><result>no</result></response>";
                }
            }

            // add a config lock
            if (params.get("cmd").equals("<request><config-lock><add></add></config-lock></request>")) {
                response =
                    "<response status=\"success\"><result>Successfully acquired lock. Other administrators will not be able to modify configuration "
                        + "for scope shared until lock is released</result></response>";
            }

            // check job status
            if (params.get("cmd").equals("<show><jobs><id>1</id></jobs></show>")) {
                if (context.containsKey("simulate_commit_failure") && context.get("simulate_commit_failure").equals("true")) {
                    response =
                        "<response status=\"success\"><result><job><tenq>2013/07/10 11:11:49</tenq><id>1</id><user>admin</user><type>Commit</type>"
                            + "<status>FIN</status><stoppable>no</stoppable><result>FAIL</result><tfin>11:11:54</tfin><progress>11:11:54</progress><details>"
                            + "<line>Bad config</line><line>Commit failed</line></details><warnings></warnings></job></result></response>";
                } else {
                    response =
                        "<response status=\"success\"><result><job><tenq>2013/07/02 14:49:49</tenq><id>1</id><user>admin</user>"
                            + "<type>Commit</type><status>FIN</status><stoppable>no</stoppable><result>OK</result><tfin>14:50:02</tfin><progress>14:50:02</progress>"
                            + "<details><line>Configuration committed successfully</line></details><warnings></warnings></job></result></response>";
                }
            }

            // load from running config
            if (params.get("cmd").equals("<load><config><from>running-config.xml</from></config></load>")) {
                response = "<response status=\"success\"><result><msg><line>Config loaded from running-config.xml</line></msg></result></response>";
            }

            // remove config lock
            if (params.get("cmd").equals("<request><config-lock><remove></remove></config-lock></request>")) {
                response = "<response status=\"success\"><result>Config lock released for scope shared</result></response>";
            }
        } // end 'op'

        // 'commit' requests
        if (params.containsKey("type") && params.get("type").equals("commit")) {
            // cmd = '<commit></commit>'
            if (params.get("cmd").equals("<commit></commit>")) {
                response = "<response status=\"success\" code=\"19\"><result><msg><line>Commit job enqueued with jobid 1</line></msg><job>1</job></result></response>";
            }
        } // end 'commit'

        // print out the details into the console
        if (context.containsKey("enable_console_output") && context.get("enable_console_output") == "true") {
            if (params.containsKey("xpath")) {
                System.out.println("XPATH(" + params.get("action") + "): " + params.get("xpath"));
            }
            if (params.containsKey("type") && params.get("type").equals("op")) {
                System.out.println("OP CMD: " + params.get("cmd"));
            }
            System.out.println(response + "\n");
        }

        return response;
    }
}
