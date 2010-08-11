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
package com.cloud.agent.api.routing;

import com.cloud.network.FirewallRuleVO;

public class SetFirewallRuleCommand extends RoutingCommand {
    FirewallRuleVO rule;
    String routerName;
    String routerIpAddress;
    String oldPrivateIP = null;
    String oldPrivatePort = null;
    
    protected SetFirewallRuleCommand() {
    }
    
    public SetFirewallRuleCommand(String routerName, String routerIpAddress, FirewallRuleVO rule, String oldPrivateIP, String oldPrivatePort) {
    	this.routerName = routerName;
    	this.routerIpAddress = routerIpAddress;
    	this.rule = rule;
    	this.oldPrivateIP = oldPrivateIP;
    	this.oldPrivatePort = oldPrivatePort;
    }
    
    public SetFirewallRuleCommand(String routerName, String routerIpAddress, FirewallRuleVO rule) {
    	this.routerName = routerName;
    	this.routerIpAddress = routerIpAddress;
    	this.rule = rule;
    }
    
    @Override
    public boolean executeInSequence() {
        return false;
    }
    
    public FirewallRuleVO getRule() {
        return rule;
    }
    
    public String getPrivateIpAddress() {
        return rule.getPrivateIpAddress();
    }
    
    public String getPublicIpAddress() {
        return rule.getPublicIpAddress();
    }
    
    public String getVlanNetmask() {
    	return rule.getVlanNetmask();
    }
    
    public String getPublicPort() {
        return rule.getPublicPort();
    }
    
    public String getPrivatePort() {
        return rule.getPrivatePort();
    }
    
    public String getRouterName() {
    	return routerName;
    }
    
    public String getRouterIpAddress() {
        return routerIpAddress;
    }
    
    public boolean isEnable() {
        return rule.isEnabled();
    }
    
    public String getProtocol() {
        return rule.getProtocol();
    }
    
    public String getOldPrivateIP() {
    	return this.oldPrivateIP;
    }
    
    public String getOldPrivatePort() {
    	return this.oldPrivatePort;
    }
    
}
