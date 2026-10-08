//
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
//

package com.cloud.agent.api.routing;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;

import com.cloud.agent.api.to.NetworkACLTO;
import com.cloud.agent.api.to.NicTO;
import com.cloud.utils.net.NetUtils;

public class SetNetworkACLCommand extends NetworkElementCommand {
    public static final String RULE_DETAIL_SEPARATOR = ";";
    private static final int TCP_PROTO_NUMBER = 6;
    private static final int UDP_PROTO_NUMBER = 17;

    NetworkACLTO[] rules;
    NicTO nic;

    protected SetNetworkACLCommand() {
    }

    public SetNetworkACLCommand(final List<NetworkACLTO> rules, final NicTO nic) {
        this.rules = rules.toArray(new NetworkACLTO[rules.size()]);
        this.nic = nic;
    }

    public NetworkACLTO[] getRules() {
        return rules;
    }

    public String[][] generateFwRules() {
        final List<NetworkACLTO> aclList = Arrays.asList(rules);

        orderNetworkAclRulesByRuleNumber(aclList);

        final String[][] result = new String[2][aclList.size()];
        int i = 0;
        for (final NetworkACLTO aclTO : aclList) {
            /*  example  :  Ingress:tcp:80:80:0.0.0.0/0:ACCEPT:,Egress:tcp:220:220:0.0.0.0/0:DROP:,
             *  each entry format      Ingress/Egress:protocol:start port: end port:scidrs:action:
             *  reverted entry format  Ingress/Egress:reverted:0:0:0:
             */
            if (aclTO.revoked() == true) {
                final StringBuilder sb = new StringBuilder();
                /* This entry is added just to make sure at least there will one entry in the list to get the ipaddress */
                List<String> revertRuleItems = Arrays.asList("", "reverted", "0", "0", "0", "");
                sb.append(aclTO.getTrafficType().toString()).append(String.join(RULE_DETAIL_SEPARATOR, revertRuleItems));
                final String aclRuleEntry = sb.toString();
                result[0][i++] = aclRuleEntry;
                continue;
            }

            List<String> cidr;
            final StringBuilder sb = new StringBuilder();
            final String protocol = normalizeProtocol(aclTO.getProtocol(), aclTO.getSrcPortRange() != null);
            sb.append(aclTO.getTrafficType().toString()).append(RULE_DETAIL_SEPARATOR).append(protocol).append(RULE_DETAIL_SEPARATOR);
            if (NetUtils.ICMP_PROTO.equals(protocol)) {
                sb.append(icmpTypeOrCode(aclTO.getIcmpType())).append(RULE_DETAIL_SEPARATOR).append(icmpTypeOrCode(aclTO.getIcmpCode())).append(RULE_DETAIL_SEPARATOR);
            } else {
                sb.append(aclTO.getStringPortRange().replace(":", RULE_DETAIL_SEPARATOR)).append(RULE_DETAIL_SEPARATOR);
            }
            cidr = aclTO.getSourceCidrList();
            if (cidr == null || cidr.isEmpty()) {
                sb.append(String.format("%s,%s", NetUtils.ALL_IP4_CIDRS, NetUtils.ALL_IP6_CIDRS));
            } else {
                Boolean firstEntry = true;
                for (final String tag : cidr) {
                    if (!firstEntry) {
                        sb.append(",");
                    }
                    sb.append(tag);
                    firstEntry = false;
                }
            }
            sb.append(RULE_DETAIL_SEPARATOR).append(aclTO.getAction()).append(RULE_DETAIL_SEPARATOR);
            final String aclRuleEntry = sb.toString();
            result[0][i++] = aclRuleEntry;
        }

        return result;
    }

    /**
     * The VR applies ports only to a rule it is sent as tcp or udp, and an ICMP type and code only to one
     * sent as icmp; any other protocol goes to it as a bare number, which matches the whole protocol. So
     * send protocol number 1 as icmp, and 6 and 17 as tcp and udp when the rule has ports, whatever form
     * or case they were stored in. Without ports 6 and 17 stay bare numbers: that is what matches all of
     * TCP or UDP, where a tcp or udp rule without ports would match port 0 only.
     */
    protected static String normalizeProtocol(String protocol, boolean hasPorts) {
        if (protocol == null) {
            return null;
        }
        String p = protocol.trim().toLowerCase(Locale.ROOT);
        if (!StringUtils.isNumeric(p)) {
            return p;
        }
        int number;
        try {
            number = Integer.parseInt(p);
        } catch (NumberFormatException e) {
            return p;
        }
        if (number == NetUtils.ICMP_PROTO_NUMBER) {
            return NetUtils.ICMP_PROTO;
        }
        if (hasPorts && number == TCP_PROTO_NUMBER) {
            return NetUtils.TCP_PROTO;
        }
        if (hasPorts && number == UDP_PROTO_NUMBER) {
            return NetUtils.UDP_PROTO;
        }
        return String.valueOf(number);
    }

    private static int icmpTypeOrCode(Integer value) {
        // -1 is "any"; a rule stored as protocol 1 may carry no ICMP type or code at all
        return value == null ? -1 : value;
    }

    protected void orderNetworkAclRulesByRuleNumber(List<NetworkACLTO> aclList) {
        Collections.sort(aclList, new Comparator<NetworkACLTO>() {
            @Override
            public int compare(final NetworkACLTO acl1, final NetworkACLTO acl2) {
                return acl1.getNumber() > acl2.getNumber() ? 1 : -1;
            }
        });
    }

    public NicTO getNic() {
        return nic;
    }
}
