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

import java.util.List;

import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.PortForwardingRule;
import com.cloud.utils.StringUtils;
import com.cloud.utils.net.NetUtils;

/**
 * PortForwardingRuleTO specifies one port forwarding rule.
 * 
 * See FirewallRuleTO for the stuff.
 *
 */
public class PortForwardingRuleTO extends FirewallRuleTO {
    String dstIp;
    int[] dstPortRange;
    List<String>  sourceCidrs;
    
    protected PortForwardingRuleTO() {
        super();
    }
    
    public PortForwardingRuleTO(PortForwardingRule rule, String srcIp) {
        super(rule, srcIp);
        this.dstIp = rule.getDestinationIpAddress().addr();
        this.dstPortRange = new int[] { rule.getDestinationPortStart(), rule.getDestinationPortEnd() };
        this.sourceCidrs = rule.getSourceCidrList();
    }
    
    protected PortForwardingRuleTO(long id, String srcIp, int srcPortStart, int srcPortEnd, String dstIp, int dstPortStart, int dstPortEnd, String protocol, boolean revoked, boolean brandNew) {
        super(id, srcIp, protocol, srcPortStart, srcPortEnd, revoked, brandNew, FirewallRule.Purpose.PortForwarding);
        this.dstIp = dstIp;
        this.dstPortRange = new int[] { dstPortStart, dstPortEnd };
    }

    public String getDstIp() {
        return dstIp;
    }

    public int[] getDstPortRange() {
        return dstPortRange;
    }

    public String getStringDstPortRange() {
        return NetUtils.portRangeToString(dstPortRange);
    }
    
    public List<String> getSourceCidrs(){
        return sourceCidrs;
    }
    
    public String getStringSourceCidrs(){
        return StringUtils.join(sourceCidrs, ",");
    }    
    
}
