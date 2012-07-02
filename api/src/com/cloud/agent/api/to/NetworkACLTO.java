// Copyright 2012 Citrix Systems, Inc. Licensed under the
package com.cloud.agent.api.to;

import java.util.ArrayList;
import java.util.List;

import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.FirewallRule.TrafficType;
import com.cloud.utils.net.NetUtils;


public class NetworkACLTO {
    long id;
    String vlanTag;
    String protocol;
    int[] portRange;
    boolean revoked;
    boolean alreadyAdded;
    private List<String> cidrList;
    private Integer icmpType;
    private Integer icmpCode;
    private FirewallRule.TrafficType trafficType;
    

    protected NetworkACLTO() {
    }
    

    public NetworkACLTO(long id,String vlanTag, String protocol, Integer portStart, Integer portEnd, boolean revoked,
            boolean alreadyAdded, List<String> cidrList, Integer icmpType,Integer icmpCode,TrafficType trafficType) {
        this.vlanTag = vlanTag;
        this.protocol = protocol;
        
        if (portStart != null) {
            List<Integer> range = new ArrayList<Integer>();
            range.add(portStart);
            if (portEnd != null) {
                range.add(portEnd);
            }
            
            portRange = new int[range.size()];
            int i = 0;
            for (Integer port : range) {
                portRange[i] = port.intValue();
                i ++;
            }   
        } 
        
        this.revoked = revoked;
        this.alreadyAdded = alreadyAdded;
        this.cidrList = cidrList;
        this.icmpType = icmpType;
        this.icmpCode = icmpCode;
        this.trafficType = trafficType;
    }

    public NetworkACLTO(FirewallRule rule, String vlanTag, FirewallRule.TrafficType  trafficType ) {
        this(rule.getId(), vlanTag, rule.getProtocol(), rule.getSourcePortStart(), rule.getSourcePortEnd(), 
                rule.getState() == FirewallRule.State.Revoke, rule.getState() == FirewallRule.State.Active,
                rule.getSourceCidrList() ,rule.getIcmpType(), rule.getIcmpCode(),trafficType);
    }
    
    public long getId() {
        return id;
    }

    public String getSrcVlanTag() {
    	return vlanTag;
    }

    public String getProtocol() {
        return protocol;
    }

    public int[] getSrcPortRange() {
        return portRange;
    }
    
    public Integer getIcmpType(){
    	return icmpType;
    }
    
    public Integer getIcmpCode(){
    	return icmpCode;  
    }
    
    public String getStringPortRange() {
    	if (portRange == null || portRange.length < 2)
    		return "0:0";
    	else
    		return NetUtils.portRangeToString(portRange);
    }

    public boolean revoked() {
        return revoked;
    }
    
    public List<String> getSourceCidrList() {
        return cidrList;
    }
    
    public boolean isAlreadyAdded() {
        return alreadyAdded;
    }

    public FirewallRule.TrafficType getTrafficType() {
        return trafficType;
    }
}
