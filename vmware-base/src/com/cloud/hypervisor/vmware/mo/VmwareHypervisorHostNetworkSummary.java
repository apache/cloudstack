/**
 *  Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
 */

package com.cloud.hypervisor.vmware.mo;

public class VmwareHypervisorHostNetworkSummary {
	private String hostIp;
	private String hostNetmask;
	private String hostMacAddress;

	public VmwareHypervisorHostNetworkSummary() {
	}
	
	public String getHostIp() {
		return hostIp;
	}
	
	public void setHostIp(String hostIp) {
		this.hostIp = hostIp;
	}
	
	public String getHostNetmask() {
		return hostNetmask;
	}
	
	public void setHostNetmask(String hostNetmask) {
		this.hostNetmask = hostNetmask;
	}
	
	public String getHostMacAddress() {
		return hostMacAddress;
	}
	
	public void setHostMacAddress(String hostMacAddress) {
		this.hostMacAddress = hostMacAddress;
	}
}
