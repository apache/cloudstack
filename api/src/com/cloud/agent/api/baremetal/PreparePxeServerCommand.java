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

package com.cloud.agent.api.baremetal;

import com.cloud.agent.api.Command;

public class PreparePxeServerCommand extends Command {

	String ip;
	String mac;
	String netMask;
	String gateway;
	String dns;
	String template;
	String vmName;
	String hostName;
	
	@Override
	public boolean executeInSequence() {
		return true;
	}
	
    public PreparePxeServerCommand() {
    }

	public PreparePxeServerCommand(String ip, String mac, String netMask, String gateway, String dns, String template, String vmName, String hostName) {
		this.ip = ip;
		this.mac = mac;
		this.netMask = netMask;
		this.gateway = gateway;
		this.dns = dns;
		this.template = template;
		this.vmName = vmName;
		this.hostName = hostName;
	}
	
	public String getIp() {
		return ip;
	}

	public String getMac() {
		return mac;
	}

	public String getNetMask() {
		return netMask;
	}

	public String getGateWay() {
		return gateway;
	}

	public String getDns() {
		return dns;
	}

	public String getTemplate() {
		return template;
	}
	
	public String getVmName() {
		return vmName;
	}
	
	public String getHostName() {
		return hostName;
	}

}
