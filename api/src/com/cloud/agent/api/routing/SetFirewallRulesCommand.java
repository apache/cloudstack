/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package com.cloud.agent.api.routing;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.cloud.agent.api.to.FirewallRuleTO;
import com.cloud.agent.api.to.LoadBalancerTO;
import com.cloud.utils.StringUtils;

/**
 * SetFirewallRulesCommand is the transport for firewall rules.
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
		String [][] result = new String [2][];
		Set<String> toAdd = new HashSet<String>();

		
		for (FirewallRuleTO fwTO: rules) {
		/* example  :  172.16.92.44:tcp:80:80:0.0.0.0/0:,200.16.92.44:tcp:220:220:0.0.0.0/0:, 
		 *  each entry format      <ip>:protocol:srcport:destport:scidr:
		 *  reverted entry format  <ip>:reverted:0:0:0:
		 */
			if (fwTO.revoked() == true) 
			{
				StringBuilder sb = new StringBuilder();
				/* This entry is added just to make sure atleast there will one entry in the list to get the ipaddress */
				sb.append(fwTO.getSrcIp()).append(":reverted:0:0:0:"); 
				String fwRuleEntry = sb.toString();
				toAdd.add(fwRuleEntry);
				continue;
			}
			
			List<String> cidr;
			StringBuilder sb = new StringBuilder();
			sb.append(fwTO.getSrcIp()).append(":").append(fwTO.getProtocol()).append(":");
			if ("icmp".compareTo(fwTO.getProtocol()) == 0)
			{
				sb.append(fwTO.getIcmpType()).append(":").append(fwTO.getIcmpCode()).append(":");
				
			}else if (fwTO.getStringSrcPortRange() == null)
				sb.append("0:0").append(":");
			else
			    sb.append(fwTO.getStringSrcPortRange()).append(":");
			
			cidr = fwTO.getSourceCidrList();
			if (cidr == null || cidr.isEmpty())
			{
				sb.append("0.0.0.0/0");
			}else{
				Boolean firstEntry = true;
	            for (String tag : cidr) {
	            	if (!firstEntry) sb.append("-"); 
	        	   sb.append(tag);
	        	   firstEntry = false;
	            }
			}
			sb.append(":");
			String fwRuleEntry = sb.toString();
		
			toAdd.add(fwRuleEntry);
			
		}
		result[0] = toAdd.toArray(new String[toAdd.size()]);
		
		return result;
	}
}
