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

package com.cloud.agent.api;

import java.util.List;

import com.cloud.storage.VolumeVO;
import com.cloud.vm.ConsoleProxyVO;

public class StartConsoleProxyCommand extends AbstractStartCommand {

    private ConsoleProxyVO proxy;
    private int proxyCmdPort;
    private String vncPort;
    private String urlPort;
    private String mgmt_host;
    private int mgmt_port;
    private boolean sslEnabled;
    
	protected StartConsoleProxyCommand() {
	}
	
    public StartConsoleProxyCommand(int proxyCmdPort, ConsoleProxyVO proxy, String vmName, String storageHost, 
    		List<VolumeVO> vols, String vncPort, String urlPort, String mgmtHost, int mgmtPort, boolean sslEnabled) {
    	super(vmName, storageHost, vols);
    	this.proxyCmdPort = proxyCmdPort;
    	this.proxy = proxy;
    	this.vncPort = vncPort;
    	this.urlPort = urlPort;
    	this.mgmt_host = mgmtHost;
    	this.mgmt_port = mgmtPort;
    	this.sslEnabled = sslEnabled;
    }
	
	@Override
	public boolean executeInSequence() {
		//Temporary relaxing serialization
        return false;
	}
	
	public ConsoleProxyVO getProxy() {
		return proxy;
	}
	
	public int getProxyCmdPort() {
		return proxyCmdPort;
	}
	
	public String getVNCPort() {
		return this.vncPort;
	}
	
	public String getURLPort() {
		return this.urlPort;
	}
	
	public String getManagementHost() {
		return mgmt_host;
	}
	
	public int getManagementPort() {
		return mgmt_port;
	}
	
	public String getBootArgs() {
		String eth1Ip = (proxy.getPrivateIpAddress() == null)? "0.0.0.0":proxy.getPrivateIpAddress();
		String eth1NetMask = (proxy.getPrivateNetmask() == null) ? "0.0.0.0":proxy.getPrivateNetmask();
		String eth2Ip = (proxy.getPublicIpAddress() == null)?"0.0.0.0" : proxy.getPublicIpAddress();
		String eth2NetMask = (proxy.getPublicNetmask() == null) ? "0.0.0.0":proxy.getPublicNetmask();
		String gateWay = (proxy.getGateway() == null) ? "0.0.0.0" : proxy.getGateway();
		
		String basic = " eth0ip=" + proxy.getGuestIpAddress() + " eth0mask=" + proxy.getGuestNetmask() + " eth1ip="
        + eth1Ip + " eth1mask=" + eth1NetMask + " eth2ip="
        + eth2Ip + " eth2mask=" + eth2NetMask + " gateway=" + gateWay
		+ " dns1=" + proxy.getDns1() + " type=consoleproxy"+ " name=" + proxy.getName() + " template=domP";
		if (proxy.getDns2() != null) {
			basic = basic + " dns2=" + proxy.getDns2();
		}
		basic = basic + " host=" + mgmt_host + " port=" + mgmt_port;
		if(sslEnabled)
			basic = basic + " premium=true";
		if (proxy.getPrivateIpAddress() == null || proxy.getPublicIpAddress() == null) {
			basic = basic + " bootproto=dhcp";
		}
		return basic;
	}
}
