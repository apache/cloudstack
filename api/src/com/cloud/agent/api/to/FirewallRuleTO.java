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

import com.cloud.network.IpAddress;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.FirewallRule.State;

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
    String srcIp;
    String protocol;
    int[] srcPortRange;
    boolean revoked;
    boolean alreadyAdded;
    boolean isOneToOneNat;
    String vlanNetmask;    // FIXME: Get rid of this!

    protected FirewallRuleTO() {
    }
    
    public FirewallRuleTO(long id, String srcIp, String protocol, int srcPortStart, int srcPortEnd, boolean revoked, boolean alreadyAdded, boolean isOneToOneNat) {
        this.srcIp = srcIp;
        this.protocol = protocol;
        this.srcPortRange = new int[] {srcPortStart, srcPortEnd};
        this.revoked = revoked;
        this.alreadyAdded = alreadyAdded;
        this.isOneToOneNat = isOneToOneNat;
    }
    
    public FirewallRuleTO(FirewallRule rule, String srcIp) {
        this(rule.getId(), srcIp, rule.getProtocol(), rule.getSourcePortStart(), rule.getSourcePortEnd(), rule.getState()==State.Revoke, rule.getState()==State.Active, rule.isOneToOneNat());
    }
    
    public long getId() {
        return id;
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

    public boolean revoked() {
        return revoked;
    }
    
    public String getVlanNetmask() {
        return vlanNetmask;
    }
    
    public boolean isAlreadyAdded() {
        return alreadyAdded;
    }
    
    public boolean isOneToOneNat() {
        return isOneToOneNat;
    }
}
