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


public class RemoteAccessVpnCfgCommand extends RoutingCommand {
	
	String vpnAppliancePrivateIpAddress; //router private ip address typically
	boolean create;
    String vpnServerIp;
    String ipRange;
    String presharedKey;
    String localIp;
    
    protected RemoteAccessVpnCfgCommand() {
    	this.create = false;
    }
    
    public boolean isCreate() {
		return create;
	}

	@Override
    public boolean executeInSequence() {
        return true;
    }
    
    
	public RemoteAccessVpnCfgCommand(boolean create, String routerPrivateIp, String vpnServerAddress, String localIp, String ipRange, String ipsecPresharedKey) {
		this.vpnAppliancePrivateIpAddress = routerPrivateIp;
		this.vpnServerIp = vpnServerAddress;
		this.ipRange  = ipRange;
		this.presharedKey = ipsecPresharedKey; 
		this.localIp = localIp;
		this.create = create;
	}

	public String getVpnServerIp() {
		return vpnServerIp;
	}

	public void setVpnServerIp(String vpnServerIp) {
		this.vpnServerIp = vpnServerIp;
	}

	public String getIpRange() {
		return ipRange;
	}

	public void setIpRange(String ipRange) {
		this.ipRange = ipRange;
	}

	public String getPresharedKey() {
		return presharedKey;
	}

	public void setPresharedKey(String presharedKey) {
		this.presharedKey = presharedKey;
	}

	public String getLocalIp() {
		return localIp;
	}

	public String getVpnAppliancePrivateIpAddress() {
		return vpnAppliancePrivateIpAddress;
	}

	public String getRouterPrivateIpAddress() {
		return vpnAppliancePrivateIpAddress;
	}

}
