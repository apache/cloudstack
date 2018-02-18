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

import com.cloud.agent.api.to.FirewallRuleTO;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * AccessDetails allow different components to put in information about
 * how to access the components inside the command.
 */
public class SetFirewallRulesCommand extends NetworkElementCommand {
    FirewallRuleTO[] rules;

    protected SetFirewallRulesCommand() {
    }

    public SetFirewallRulesCommand(List<FirewallRuleTO> rules) {
        this.rules = rules.toArray(new FirewallRuleTO[rules.size()]);
    }

    public FirewallRuleTO[] getRules() {
        return rules;
    }

    public String[][] generateFwRules() {
        String[][] result = new String[2][];
        Set<String> toAdd = new HashSet<String>();

        for (FirewallRuleTO fwTO : rules) {
            /* example  :  172.16.92.44:tcp:80:80:0.0.0.0/0:,200.16.92.44:tcp:220:220:0.0.0.0/0:,
             *  each entry format      <ip>:protocol:srcport:destport:scidr:
             *  reverted entry format  <ip>:reverted:0:0:0:
             */
            if (fwTO.revoked()) {
                StringBuilder sb = new StringBuilder();
                /* This entry is added just to make sure atleast there will one entry in the list to get the ipaddress */
                sb.append(fwTO.getSrcIp()).append(":reverted:0:0:0:0:").append(fwTO.getId()).append(":");
                String fwRuleEntry = sb.toString();
                toAdd.add(fwRuleEntry);
                continue;
            }

            List<String> sCidr, dCidr;
            StringBuilder sb = new StringBuilder();
            sb.append(fwTO.getSrcIp()).append(":").append(fwTO.getProtocol()).append(":");
            if ("icmp".compareTo(fwTO.getProtocol()) == 0) {
                sb.append(fwTO.getIcmpType()).append(":").append(fwTO.getIcmpCode()).append(":");

            } else if (fwTO.getStringSrcPortRange() == null)
                sb.append("0:0").append(":");
            else
                sb.append(fwTO.getStringSrcPortRange()).append(":");

            sCidr = fwTO.getSourceCidrList();
            dCidr = fwTO.getDestCidrList();
            if (sCidr == null || sCidr.isEmpty()) {
                sb.append("0.0.0.0/0");  //check if this is necessary because we are providing the source cidr by default???
            } else {
                boolean firstEntry = true;
                for (String tag : sCidr) {
                    if (!firstEntry)
                        sb.append("-");
                    sb.append(tag);
                    firstEntry = false;
                }
            }
            sb.append(":");

            if(dCidr == null || dCidr.isEmpty()){
                sb.append("");
            }
            else{
                boolean firstEntry = true;
                for(String cidr : dCidr){
                    if(!firstEntry)
                        sb.append("-");
                    sb.append(cidr);
                    firstEntry = false;
                }
            }
            sb.append(":");
            sb.append(fwTO.getId());
            sb.append(":");
            String fwRuleEntry = sb.toString();
            toAdd.add(fwRuleEntry);

        }
        result[0] = toAdd.toArray(new String[toAdd.size()]);

        return result;
    }

    @Override
    public int getAnswersCount() {
        return rules.length;
    }
}
