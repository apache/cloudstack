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
package com.cloud.network;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.cloud.agent.api.routing.LoadBalancerConfigCommand;
import com.cloud.agent.api.to.LoadBalancerTO;
import com.cloud.agent.api.to.PortForwardingRuleTO;
import com.cloud.agent.api.to.LoadBalancerTO.DestinationTO;
import com.cloud.utils.net.NetUtils;


/**
 * @author chiradeep
 *
 */
public class HAProxyConfigurator implements LoadBalancerConfigurator {
	private static String [] globalSection = {"global",
			"\tlog 127.0.0.1:3914   local0 info",
			"\tmaxconn 4096",
			"\tchroot /var/lib/haproxy",
			"\tuser haproxy",
			"\tgroup haproxy",
			"\tdaemon"
	};

	private static String [] defaultsSection = {"defaults",
	        "\tlog     global",
	        "\tmode    tcp",
	        "\toption  dontlognull",
	        "\tretries 3",
	        "\toption redispatch",
	        "\toption forwardfor",
	        "\tstats enable",
	        "\tstats uri     /admin?stats",
	        "\tstats realm   Haproxy\\ Statistics",
	        "\tstats auth    admin1:AdMiN123",
	        "\toption forceclose",
	        "\ttimeout connect    5000",
	        "\ttimeout client     50000",
	        "\ttimeout server     50000"
	};
	
	private static String [] defaultListen = {"listen  vmops 0.0.0.0:9",
        "\toption transparent"
	};

	@Override
	public String[] generateConfiguration(List<PortForwardingRuleTO> fwRules) {
		//Group the rules by publicip:publicport
		Map<String, List<PortForwardingRuleTO>> pools = new HashMap<String, List<PortForwardingRuleTO>>();
		
		for(PortForwardingRuleTO rule:fwRules) {
			StringBuilder sb = new StringBuilder();
			String poolName = sb.append(rule.getSrcIp().replace(".", "_")).append('-').append(rule.getSrcPortRange()[0]).toString();
			if (!rule.revoked()) {	
				List<PortForwardingRuleTO> fwList = pools.get(poolName);
				if (fwList == null) {
					fwList = new ArrayList<PortForwardingRuleTO>();
					pools.put(poolName, fwList);
				}
				fwList.add(rule);
			}
		}
		
		List<String> result = new ArrayList<String>();
		
		result.addAll(Arrays.asList(globalSection));
		result.add(getBlankLine());
		result.addAll(Arrays.asList(defaultsSection));
		result.add(getBlankLine());
		
		if (pools.isEmpty()){
			//haproxy cannot handle empty listen / frontend or backend, so add a dummy listener 
			//on port 9
			result.addAll(Arrays.asList(defaultListen));
		}
		result.add(getBlankLine());
		
		for (Map.Entry<String, List<PortForwardingRuleTO>> e : pools.entrySet()){
		    List<String> poolRules = getRulesForPool(e.getKey(), e.getValue());
		    result.addAll(poolRules);
		}
		
		return result.toArray(new String[result.size()]);
	}

	private List<String> getRulesForPool(String poolName, List<PortForwardingRuleTO> fwRules) {
		PortForwardingRuleTO firstRule = fwRules.get(0);
		String publicIP = firstRule.getSrcIp();
		String publicPort = Integer.toString(firstRule.getSrcPortRange()[0]);
// FIXEME:		String algorithm = firstRule.getAlgorithm();
		
		List<String> result = new ArrayList<String>();
		//add line like this: "listen  65_37_141_30-80 65.37.141.30:80"
		StringBuilder sb = new StringBuilder();
		sb.append("listen ").append(poolName).append(" ")
		  .append(publicIP).append(":").append(publicPort);
		result.add(sb.toString());
		sb = new StringBuilder();
//FIXME		sb.append("\t").append("balance ").append(algorithm);
		result.add(sb.toString());
		if (publicPort.equals(NetUtils.HTTP_PORT)) {
			sb = new StringBuilder();
			sb.append("\t").append("mode http");
			result.add(sb.toString());
			sb = new StringBuilder();
			sb.append("\t").append("option httpclose");
			result.add(sb.toString());
		}
		int i=0;
		for (PortForwardingRuleTO rule: fwRules) {
			//add line like this: "server  65_37_141_30-80_3 10.1.1.4:80 check"
			if (rule.revoked()) {
                continue;
            }
			sb = new StringBuilder();
			sb.append("\t").append("server ").append(poolName)
			   .append("_").append(Integer.toString(i++)).append(" ")
			   .append(rule.getDstIp()).append(":").append(rule.getDstPortRange()[0])
			   .append(" check");
			result.add(sb.toString());
		}
		result.add(getBlankLine());
		return result;
	}
	
	private List<String> getRulesForPool(LoadBalancerTO lbTO) {
		StringBuilder sb = new StringBuilder();
		String poolName  =  sb.append(lbTO.getSrcIp().replace(".", "_")).append('-').append(lbTO.getSrcPort()).toString();
		String publicIP = lbTO.getSrcIp();
		String publicPort = Integer.toString(lbTO.getSrcPort());
		String algorithm = lbTO.getAlgorithm();
		
		List<String> result = new ArrayList<String>();
		//add line like this: "listen  65_37_141_30-80 65.37.141.30:80"
		sb = new StringBuilder();
		sb.append("listen ").append(poolName).append(" ")
		  .append(publicIP).append(":").append(publicPort);
		result.add(sb.toString());
		sb = new StringBuilder();
		sb.append("\t").append("balance ").append(algorithm);
		result.add(sb.toString());
		if (publicPort.equals(NetUtils.HTTP_PORT)) {
			sb = new StringBuilder();
			sb.append("\t").append("mode http");
			result.add(sb.toString());
			sb = new StringBuilder();
			sb.append("\t").append("option httpclose");
			result.add(sb.toString());
		}
		int i=0;
		for (DestinationTO dest: lbTO.getDestinations()) {
			//add line like this: "server  65_37_141_30-80_3 10.1.1.4:80 check"
			if (dest.isRevoked()) {
                continue;
            }
			sb = new StringBuilder();
			sb.append("\t").append("server ").append(poolName)
			   .append("_").append(Integer.toString(i++)).append(" ")
			   .append(dest.getDestIp()).append(":").append(dest.getDestPort())
			   .append(" check");
			result.add(sb.toString());
		}
		result.add(getBlankLine());
		return result;
	}

	private String getBlankLine() {
		return new String("\t ");
	}
	
	@Override
	public String[][] generateFwRules(List<PortForwardingRuleTO> fwRules) {
		String [][] result = new String [2][];
		Set<String> toAdd = new HashSet<String>();
		Set<String> toRemove = new HashSet<String>();
		
		for (int i = 0; i < fwRules.size(); i++) {
			PortForwardingRuleTO rule = fwRules.get(i);
			
			String vlanNetmask = rule.getVlanNetmask();
			
			StringBuilder sb = new StringBuilder();
			sb.append(rule.getSrcIp()).append(":");
			sb.append(rule.getSrcPortRange()[0]).append(":");
			sb.append(vlanNetmask);
			String lbRuleEntry = sb.toString();
			if (!rule.revoked()) {	
				toAdd.add(lbRuleEntry);
			} else {
				toRemove.add(lbRuleEntry);
			}
		}
		toRemove.removeAll(toAdd);
		result[ADD] = toAdd.toArray(new String[toAdd.size()]);
		result[REMOVE] = toRemove.toArray(new String[toRemove.size()]); 

		return result;
	}

	@Override
	public String[] generateConfiguration(LoadBalancerConfigCommand lbCmd) {
		List<String> result = new ArrayList<String>();
		
		result.addAll(Arrays.asList(globalSection));
		result.add(getBlankLine());
		result.addAll(Arrays.asList(defaultsSection));
		result.add(getBlankLine());
		
		if (lbCmd.getLoadBalancers().length == 0){
			//haproxy cannot handle empty listen / frontend or backend, so add a dummy listener 
			//on port 9
			result.addAll(Arrays.asList(defaultListen));
		}
		result.add(getBlankLine());
		
		for (LoadBalancerTO lbTO: lbCmd.getLoadBalancers()){
		    List<String> poolRules = getRulesForPool(lbTO);
		    result.addAll(poolRules);
		}
		
		return result.toArray(new String[result.size()]);
	}

	@Override
	public String[][] generateFwRules(LoadBalancerConfigCommand lbCmd) {
		String [][] result = new String [2][];
		Set<String> toAdd = new HashSet<String>();
		Set<String> toRemove = new HashSet<String>();
		
		for (LoadBalancerTO lbTO: lbCmd.getLoadBalancers()) {
			
			StringBuilder sb = new StringBuilder();
			sb.append(lbTO.getSrcIp()).append(":");
			sb.append(lbTO.getSrcPort()).append(":");
			String lbRuleEntry = sb.toString();
			if (!lbTO.isRevoked()) {	
				toAdd.add(lbRuleEntry);
			} else {
				toRemove.add(lbRuleEntry);
			}
		}
		toRemove.removeAll(toAdd);
		result[ADD] = toAdd.toArray(new String[toAdd.size()]);
		result[REMOVE] = toRemove.toArray(new String[toRemove.size()]); 

		return result;
	}
}
