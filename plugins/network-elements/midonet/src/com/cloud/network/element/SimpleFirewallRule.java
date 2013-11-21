/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.cloud.network.element;

import java.util.ArrayList;
import java.util.List;
// Used for translation between MidoNet firewall rules and
// CloudStack firewall rules

import org.midonet.client.dto.DtoRule;
import org.midonet.client.resource.Rule;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import com.cloud.agent.api.to.FirewallRuleTO;
import com.cloud.agent.api.to.NetworkACLTO;
import com.cloud.agent.api.to.PortForwardingRuleTO;

public class SimpleFirewallRule {

    public List<String> sourceCidrs;
    public String protocol;
    public String dstIp;
    public int dstPortStart = 0;
    public int dstPortEnd = 0;
    public int icmpType = 0;
    public int icmpCode = 0;

    private static BiMap<Integer, String> protocolNumberToString;
    static {
        protocolNumberToString = HashBiMap.create();
        protocolNumberToString.put(1, "icmp");
        protocolNumberToString.put(6, "tcp");
        protocolNumberToString.put(17, "udp");
        protocolNumberToString.put(0, "none");
    }

    public SimpleFirewallRule(FirewallRuleTO rule) {
        // Destination IP (confusingly called SourceIP in FirewallRule attributes)
        dstIp = rule.getSrcIp();
        protocol = rule.getProtocol();

        if ("icmp".equals(protocol)) {
            icmpType = rule.getIcmpType();
            icmpCode = rule.getIcmpCode();
        } else {
            int[] portNumbers = rule.getSrcPortRange();

            // if port start and end are not set, they
            // should be 0,0, and that's already the case
            if (portNumbers != null && portNumbers.length == 2) {
                dstPortStart = portNumbers[0];
                dstPortEnd = portNumbers[1];
            }
        }

        sourceCidrs = rule.getSourceCidrList();

        // If no CIDRs specified, it is an "all sources" rule
        if (sourceCidrs == null || sourceCidrs.isEmpty()) {
            sourceCidrs = new ArrayList<String>();
            sourceCidrs.add("0.0.0.0/0");
        }
    }

    public SimpleFirewallRule(NetworkACLTO rule) {
        dstIp = "null";
        protocol = rule.getProtocol();

        if ("icmp".equals(protocol)) {
            icmpType = rule.getIcmpType();
            icmpCode = rule.getIcmpCode();
        } else {
            int[] portNumbers = rule.getSrcPortRange();

            // if port start and end are not set, they
            // should be 0,0, and that's already the case
            if (portNumbers != null && portNumbers.length == 2) {
                dstPortStart = portNumbers[0];
                dstPortEnd = portNumbers[1];
            }
        }

        sourceCidrs = rule.getSourceCidrList();

        // If no CIDRs specified, it is an "all sources" rule
        if (sourceCidrs == null || sourceCidrs.isEmpty()) {
            sourceCidrs = new ArrayList<String>();
            sourceCidrs.add("0.0.0.0/0");
        }
    }

    public SimpleFirewallRule(PortForwardingRuleTO rule) {
        dstIp = rule.getSrcIp();
        protocol = rule.getProtocol();

        int[] srcPortNumbers = rule.getSrcPortRange();
        int[] dstPortNumbers = rule.getDstPortRange();

        // if port start and end are not set, they
        // should be 0,0, and that's already the case
        if (srcPortNumbers != null && srcPortNumbers.length == 2 && dstPortNumbers != null && dstPortNumbers.length == 2) {
            dstPortStart = dstPortNumbers[0];
            dstPortEnd = srcPortNumbers[0];
        }

        sourceCidrs = new ArrayList<String>();
        sourceCidrs.add(rule.getDstIp());
    }

    public SimpleFirewallRule(Rule rule) {

        String sourceIP = rule.getNwSrcAddress();
        int sourceLength = rule.getNwSrcLength();

        sourceCidrs = new ArrayList<String>();
        /*
         * Only one IP in the CIDR list
         * Port Forwarding Rules don't have sourceCidrs, but they do have
         * targets. Use those instead if they exist.
         */
        DtoRule.DtoNatTarget[] targets = rule.getNatTargets();
        if (targets != null) {
            sourceCidrs.add(targets[0].addressFrom);
        } else {
            sourceCidrs.add(String.format("%s/%d", sourceIP, sourceLength));
        }

        int protoNum = rule.getNwProto();
        protocol = SimpleFirewallRule.protocolNumberToString(protoNum);

        dstIp = rule.getNwDstAddress();

        if ("icmp".equals(protocol)) {
            if (rule.getTpSrc() != null && rule.getTpDst() != null) {
                icmpType = rule.getTpSrc().start;
                icmpCode = rule.getTpDst().start;
            } else {
                icmpType = -1;
                icmpCode = -1;
            }

        } else {
            /*
             * If this is port forwarding, we want to take the start
             * port for the public port range, and the start port for
             * the private port range to uniquely identify this rule.
             */
            if (targets != null) {
                dstPortStart = targets[0].portFrom;
            } else {
                dstPortStart = rule.getTpDst().start;
            }
            dstPortEnd = rule.getTpDst().end;
        }

        // cidr, protocol, dstIp, dstPortStart, dstPortEnd, icmpType, icmpCode);
    }

    public static String protocolNumberToString(int protocolNumber) {
        return protocolNumberToString.get(protocolNumber);
    }

    public static int stringToProtocolNumber(String protoString) {
        return protocolNumberToString.inverse().get(protoString);
    }

    public int getFieldOne() {
        if (protocol.equals("icmp")) {
            return icmpType;

        } else {
            return dstPortStart;
        }
    }

    public int getFieldTwo() {
        if (protocol.equals("icmp")) {
            return icmpCode;
        } else {
            return dstPortEnd;
        }
    }

    public String[] toStringArray() {
        List<String> stringRules = new ArrayList<String>();

        // Create a rule string per source CIDR, since each MidoNet
        // rule is for one CIDR
        for (String sourceCidr : sourceCidrs) {

            // Follows the rule String format defined in SetFirewallRulesCommand.java::generateFirewallRules()
            int field1 = getFieldOne();
            int field2 = getFieldTwo();

            String stringRule = String.format("%s:%s:%d:%d:%s:", dstIp, protocol, field1, field2, sourceCidr);
            stringRules.add(stringRule);
        }
        String[] stringArray = new String[stringRules.size()];
        return stringRules.toArray(stringArray);
    }
}
