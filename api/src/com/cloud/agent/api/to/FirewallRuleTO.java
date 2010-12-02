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

public class FirewallRuleTO {
    String srcIp;
    String protocol;
    int[] srcPortRange;
    boolean revoked;
    String vlanNetmask;    // FIXME: Get rid of this!

    protected FirewallRuleTO() {
    }
    
    public FirewallRuleTO(String srcIp, String protocol, int srcPortStart, int srcPortEnd, boolean revoked) {
        this.srcIp = srcIp;
        this.protocol = protocol;
        this.srcPortRange = new int[] {srcPortStart, srcPortEnd};
        this.revoked = revoked;
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
}
