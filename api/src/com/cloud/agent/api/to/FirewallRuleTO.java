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
package com.cloud.agent.api.to;

import java.util.ArrayList;
import java.util.List;

import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.FirewallRule.State;
import com.cloud.utils.net.NetUtils;

/**
 * FirewallRuleTO transfers a port range for an ip to be opened.
 *   
 * There are essentially three states transferred with each state.
 *   1. revoked - the rule has been revoked.  A rule in this state may be
 *      sent multiple times to the destination.  If the rule is not on 
 *      the destination, the answer to a revoke rule should be successful.
 *   2. alreadyAdded - the rule has been successfully added before.  Rules
 *      in this state are sent for completeness and optimization.
 *   3. neither - the rule is to be added but it might have been added before.
 *      If the rule already exists on the destination, the destination should
 *      reply the rule is successfully applied.
 *      
 * As for the information carried, it is fairly straightforward:
 *   - srcIp: ip to be open the ports for.
 *   - srcPortRange: port range to open.
 *   - protocol: protocol to open for.  Usually tcp and udp.
 *   - id: a unique id if the destination can use it to uniquly identify the rules.
 *
 */
public class FirewallRuleTO {
    long id;
    String srcVlanTag;
    String srcIp;
    String protocol;
    int[] srcPortRange;
    boolean revoked;
    boolean alreadyAdded;
    private List<String> sourceCidrList;
    FirewallRule.Purpose purpose;
    private Integer icmpType;
    private Integer icmpCode;
    

    protected FirewallRuleTO() {
    }
    
    public FirewallRuleTO(long id, String srcIp, String protocol, Integer srcPortStart, Integer srcPortEnd, boolean revoked, boolean alreadyAdded, FirewallRule.Purpose purpose, List<String> sourceCidr,Integer icmpType,Integer icmpCode) {
       this(id,null,srcIp,protocol,srcPortStart,srcPortEnd,revoked,alreadyAdded,purpose,sourceCidr,icmpType,icmpCode);
    } 
    public FirewallRuleTO(long id,String srcVlanTag, String srcIp, String protocol, Integer srcPortStart, Integer srcPortEnd, boolean revoked, boolean alreadyAdded, FirewallRule.Purpose purpose, List<String> sourceCidr,Integer icmpType,Integer icmpCode) {
        this.srcVlanTag = srcVlanTag;
        this.srcIp = srcIp;
        this.protocol = protocol;
        
        if (srcPortStart != null) {
            List<Integer> portRange = new ArrayList<Integer>();
            portRange.add(srcPortStart);
            if (srcPortEnd != null) {
                portRange.add(srcPortEnd);
            }
            
            srcPortRange = new int[portRange.size()];
            int i = 0;
            for (Integer port : portRange) {
                srcPortRange[i] = port.intValue();
                i ++;
            }   
        } 
        
        this.revoked = revoked;
        this.alreadyAdded = alreadyAdded;
        this.purpose = purpose;
        this.sourceCidrList = sourceCidr;
        this.icmpType = icmpType;
        this.icmpCode = icmpCode;
    }
    public FirewallRuleTO(FirewallRule rule, String srcVlanTag, String srcIp) {
        this(rule.getId(),srcVlanTag, srcIp, rule.getProtocol(), rule.getSourcePortStart(), rule.getSourcePortEnd(), rule.getState()==State.Revoke, rule.getState()==State.Active, rule.getPurpose(),rule.getSourceCidrList(),rule.getIcmpType(),rule.getIcmpCode());
    }
    
    public FirewallRuleTO(FirewallRule rule, String srcIp) {
        this(rule.getId(),null, srcIp, rule.getProtocol(), rule.getSourcePortStart(), rule.getSourcePortEnd(), rule.getState()==State.Revoke, rule.getState()==State.Active, rule.getPurpose(),rule.getSourceCidrList(),rule.getIcmpType(),rule.getIcmpCode());
    }
    
    public long getId() {
        return id;
    }

    public String getSrcVlanTag() {
    	return srcVlanTag;
    }
    
    public String getSrcIp() {
        return srcIp;
    }

    public String getProtocol() {
        return protocol;
    }

    public int[] getSrcPortRange() {
        return srcPortRange;
    }
    
    public Integer getIcmpType(){
    	return icmpType;
    }
    
    public Integer getIcmpCode(){
    	return icmpCode;  
    }
    
    public String getStringSrcPortRange() {
    	if (srcPortRange == null || srcPortRange.length < 2)
    		return "0:0";
    	else
    		return NetUtils.portRangeToString(srcPortRange);
    }

    public boolean revoked() {
        return revoked;
    }
    
    public List<String> getSourceCidrList() {
        return sourceCidrList;
    }
    
    public boolean isAlreadyAdded() {
        return alreadyAdded;
    }

    public FirewallRule.Purpose getPurpose() {
        return purpose;
    }
    
}
