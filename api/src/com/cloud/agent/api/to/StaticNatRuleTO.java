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

import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.FirewallRule.State;
import com.cloud.network.rules.StaticNatRule;

/**
 * StaticNatRuleTO specifies one static nat rule.
 * 
 * See FirewallRuleTO for the stuff.
 *
 */

public class StaticNatRuleTO extends FirewallRuleTO{
    String dstIp;
    
    protected StaticNatRuleTO() {
    }
    
    public StaticNatRuleTO(StaticNatRule rule, String srcVlanTag, String srcIp, String dstIp) {
        super(rule.getId(), srcVlanTag, srcIp, rule.getProtocol(), rule.getSourcePortStart(), rule.getSourcePortEnd(),rule.getState()==State.Revoke, rule.getState()==State.Active, rule.getPurpose(), null,0,0);
        this.dstIp = dstIp;
    }
    
    
    public StaticNatRuleTO(long id, String srcVlanTag, String srcIp, Integer srcPortStart, Integer srcPortEnd, String dstIp, Integer dstPortStart, Integer dstPortEnd, String protocol, boolean revoked, boolean alreadyAdded) {
        super(id, srcVlanTag, srcIp, protocol, srcPortStart, srcPortEnd, revoked, alreadyAdded, FirewallRule.Purpose.StaticNat, null,0,0);
        this.dstIp = dstIp;
    }

    public String getDstIp() {
        return dstIp;
    }
    
}
