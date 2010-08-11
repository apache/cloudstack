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

import java.net.URISyntaxException;
import java.util.List;

import com.cloud.storage.VolumeVO;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NfsUtils;
import com.cloud.vm.SecondaryStorageVmVO;

public class StartSecStorageVmCommand extends AbstractStartCommand {

    private SecondaryStorageVmVO secStorageVm;
    private int proxyCmdPort;
    private String mgmt_host;
    private int mgmt_port;
    private boolean sslCopy;
    
	protected StartSecStorageVmCommand() {
	}
	
    public StartSecStorageVmCommand(int proxyCmdPort, SecondaryStorageVmVO secStorageVm, String vmName, String storageHost,
    		List<VolumeVO> vols, String mgmtHost, int mgmtPort, boolean sslCopy) {
    	super(vmName, storageHost, vols);
    	this.proxyCmdPort = proxyCmdPort;
    	this.secStorageVm = secStorageVm;

    	this.mgmt_host = mgmtHost;
    	this.mgmt_port = mgmtPort;
    	this.sslCopy = sslCopy;
    }
	
	@Override
	public boolean executeInSequence() {
        return true;
	}
	
	public SecondaryStorageVmVO getSecondaryStorageVmVO() {
		return secStorageVm;
	}
	
	public int getProxyCmdPort() {
		return proxyCmdPort;
	}
	
	
	public String getManagementHost() {
		return mgmt_host;
	}
	
	public int getManagementPort() {
		return mgmt_port;
	}
	
	public String getBootArgs() {
		String eth1Ip = (secStorageVm.getPrivateIpAddress() == null)? "0.0.0.0":secStorageVm.getPrivateIpAddress();
		String eth1NetMask = (secStorageVm.getPrivateNetmask() == null) ? "0.0.0.0":secStorageVm.getPrivateNetmask();
		String eth2Ip = (secStorageVm.getPublicIpAddress() == null)?"0.0.0.0" : secStorageVm.getPublicIpAddress();
		String eth2NetMask = (secStorageVm.getPublicNetmask() == null) ? "0.0.0.0":secStorageVm.getPublicNetmask();
		String gateWay = (secStorageVm.getGateway() == null) ? "0.0.0.0" : secStorageVm.getGateway();
		
		String basic = " eth0ip=" + secStorageVm.getGuestIpAddress() + " eth0mask=" + secStorageVm.getGuestNetmask() + " eth1ip="
        + eth1Ip + " eth1mask=" + eth1NetMask + " eth2ip="
        + eth2Ip + " eth2mask=" + eth2NetMask + " gateway=" + gateWay
		+ " dns1=" + secStorageVm.getDns1() + " type=secstorage" + " name=" + secStorageVm.getName() + " template=domP";
		if (secStorageVm.getDns2() != null) {
			basic = basic + " dns2=" + secStorageVm.getDns2();
		}
		basic = basic + " host=" + mgmt_host + " port=" + mgmt_port;
		String mountStr = null;
		try {
			mountStr = NfsUtils.url2Mount(secStorageVm.getNfsShare());
		} catch (URISyntaxException e1) {
			throw new CloudRuntimeException("NFS url malformed in database? url=" + secStorageVm.getNfsShare());
		}
		basic = basic + " mount.path=" + mountStr + " guid=" + secStorageVm.getGuid();
		basic = basic + " resource=com.cloud.storage.resource.NfsSecondaryStorageResource";
		basic = basic + " instance=SecStorage";
		basic = basic + " sslcopy=" + Boolean.toString(sslCopy);
		if (secStorageVm.getPrivateIpAddress() == null || secStorageVm.getPublicIpAddress() == null) {
			basic = basic + " bootproto=dhcp";
		}
		return basic;
	}
}
