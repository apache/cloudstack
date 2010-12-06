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

import com.cloud.network.rules.PortForwardingRule;

/**
 * PortForwardingRuleTO specifies one port forwarding rule.
 * 
 * See FirewallRuleTO for the stuff.
 *
 */
public class PortForwardingRuleTO extends FirewallRuleTO {
    String dstIp;
    int[] dstPortRange;
    
    protected PortForwardingRuleTO() {
        super();
    }
    
    public PortForwardingRuleTO(PortForwardingRule rule) {
        super(rule);
        this.dstIp = rule.getDestinationIpAddress().addr();
        this.dstPortRange = new int[] { rule.getDestinationPortStart(), rule.getDestinationPortEnd() };
    }
    
    protected PortForwardingRuleTO(long id, String srcIp, int srcPortStart, int srcPortEnd, String dstIp, int dstPortStart, int dstPortEnd, String protocol, boolean revoked, boolean brandNew) {
        super(id, srcIp, protocol, srcPortStart, srcPortEnd, revoked, brandNew);
        this.dstIp = dstIp;
        this.dstPortRange = new int[] { dstPortStart, dstPortEnd };
    }

    public String getDstIp() {
        return dstIp;
    }

    public int[] getDstPortRange() {
        return dstPortRange;
    }
}
