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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


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
	public String[] generateConfiguration(List<FirewallRuleVO> fwRules) {
		//Group the rules by publicip:publicport
		Map<String, List<FirewallRuleVO>> pools = new HashMap<String, List<FirewallRuleVO>>();
		
		for(FirewallRuleVO rule:fwRules) {
			StringBuilder sb = new StringBuilder();
			String poolName = sb.append(rule.getPublicIpAddress().replace(".", "_")).append('-').append(rule.getPublicPort()).toString();
			if (rule.isEnabled() && !rule.isForwarding()) {	
				List<FirewallRuleVO> fwList = pools.get(poolName);
				if (fwList == null) {
					fwList = new ArrayList<FirewallRuleVO>();
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
		
		for (Map.Entry<String, List<FirewallRuleVO>> e : pools.entrySet()){
		    List<String> poolRules = getRulesForPool(e.getKey(), e.getValue());
		    result.addAll(poolRules);
		}
		
		return result.toArray(new String[result.size()]);
	}

	private List<String> getRulesForPool(String poolName, List<FirewallRuleVO> fwRules) {
		FirewallRuleVO firstRule = fwRules.get(0);
		String publicIP = firstRule.getPublicIpAddress();
		String publicPort = firstRule.getPublicPort();
		String algorithm = firstRule.getAlgorithm();
		
		List<String> result = new ArrayList<String>();
		//add line like this: "listen  65_37_141_30-80 65.37.141.30:80"
		StringBuilder sb = new StringBuilder();
		sb.append("listen ").append(poolName).append(" ")
		  .append(publicIP).append(":").append(publicPort);
		result.add(sb.toString());
		sb = new StringBuilder();
		sb.append("\t").append("balance ").append(algorithm);
		result.add(sb.toString());
		int i=0;
		for (FirewallRuleVO rule: fwRules) {
			//add line like this: "server  65_37_141_30-80_3 10.1.1.4:80 check"
			if (!rule.isEnabled())
				continue;
			sb = new StringBuilder();
			sb.append("\t").append("server ").append(poolName)
			   .append("_").append(Integer.toString(i++)).append(" ")
			   .append(rule.getPrivateIpAddress()).append(":").append(rule.getPrivatePort())
			   .append(" check");
			result.add(sb.toString());
		}
		result.add(getBlankLine());
		return result;
	}

	private String getBlankLine() {
		return new String("\t ");
	}
	
	public static void main(String [] args) {
/*		FirewallRulesDao dao = new FirewallRulesDaoImpl();
		List<FirewallRuleVO> rules = dao.listIPForwarding();
		
		HAProxyConfigurator hapc = new HAProxyConfigurator();
		String [] result = hapc.generateConfiguration(rules);
		for (int i=0; i < result.length; i++) {
			System.out.println(result[i]);
		}*/
		
	}

	@Override
	public String[][] generateFwRules(List<FirewallRuleVO> fwRules) {
		String [][] result = new String [2][];
		Set<String> toAdd = new HashSet<String>();
		Set<String> toRemove = new HashSet<String>();
		
		for (int i = 0; i < fwRules.size(); i++) {
			FirewallRuleVO rule = fwRules.get(i);
			if (rule.isForwarding()) 
				continue;
			
			String vlanNetmask = rule.getVlanNetmask();
			
			StringBuilder sb = new StringBuilder();
			sb.append(rule.getPublicIpAddress()).append(":");
			sb.append(rule.getPublicPort()).append(":");
			sb.append(vlanNetmask);
			String lbRuleEntry = sb.toString();
			if (rule.isEnabled()) {	
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
