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
package com.cloud.network.rules;

import java.util.List;

import com.cloud.acl.ControlledEntity;
import com.cloud.network.rules.FirewallRule.FirewallRuleType;

public interface FirewallRule extends ControlledEntity {
    enum Purpose {
        Firewall,
        PortForwarding,
        LoadBalancing,
        Vpn,
        StaticNat,
    }
    
    enum FirewallRuleType {
    	System, // The pre-defined rules created by admin, in the system wide
    	User; // the rules created by user, to a specific ip
    }
    
    enum State {
        Staged, // Rule been created but has never got through network rule conflict detection.  Rules in this state can not be sent to network elements.
        Add,    // Add means the rule has been created and has gone through network rule conflict detection.
        Active, // Rule has been sent to the network elements and reported to be active.
        Revoke  // Revoke means this rule has been revoked. If this rule has been sent to the network elements, the rule will be deleted from database.
    }
    
    /**
     * @return database id.
     */
    long getId();
    
    /**
     * @return external id.
     */
    String getXid();
    
    /**
     * @return first port of the source port range.
     */
    Integer getSourcePortStart();
    
    /**
     * @return last port of the source prot range.  If this is null, that means only one port is mapped.
     */
    Integer getSourcePortEnd();

    /**
     * @return protocol to open these ports for.
     */
    String getProtocol();
    
    Purpose getPurpose();
    
    State getState();
    
    long getNetworkId();
    
    long getSourceIpAddressId();

    Integer getIcmpCode();

    Integer getIcmpType();
    
    List<String> getSourceCidrList();
    
    Long getRelated();

	FirewallRuleType getType();

}
