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

import com.cloud.agent.api.to.NetworkACLTO;
import com.cloud.agent.api.to.NicTO;

public class SetNetworkACLCommand extends NetworkElementCommand {
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
                /* This entry is added just to make sure atleast there will one entry in the list to get the ipaddress */
                sb.append(aclTO.getTrafficType().toString()).append(":reverted:0:0:0:");
                final String aclRuleEntry = sb.toString();
                result[0][i++] = aclRuleEntry;
                continue;
            }

            List<String> cidr;
            final StringBuilder sb = new StringBuilder();
            sb.append(aclTO.getTrafficType().toString()).append(":").append(aclTO.getProtocol()).append(":");
            if ("icmp".compareTo(aclTO.getProtocol()) == 0) {
                sb.append(aclTO.getIcmpType()).append(":").append(aclTO.getIcmpCode()).append(":");
            } else {
                sb.append(aclTO.getStringPortRange()).append(":");
            }
            cidr = aclTO.getSourceCidrList();
            if (cidr == null || cidr.isEmpty()) {
                sb.append("0.0.0.0/0");
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
            sb.append(":").append(aclTO.getAction()).append(":");
            final String aclRuleEntry = sb.toString();
            result[0][i++] = aclRuleEntry;
        }

        return result;
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

    @Override
    public int getAnswersCount() {
        return rules.length;
    }
}
