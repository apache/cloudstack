/**
 *  Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
 */

package com.cloud.hypervisor.vmware.mo;

import com.vmware.vim25.ManagedObjectReference;

public class NetworkDetails {

	private String _name;
	private ManagedObjectReference _morNetwork;
	private ManagedObjectReference[] _morVMsOnNetwork;
	private String _gcTag;
	
	public NetworkDetails(String name, ManagedObjectReference morNetwork, ManagedObjectReference[] morVMsOnNetwork, String gcTag) {
		_name = name;
		_morNetwork = morNetwork;
		_morVMsOnNetwork = morVMsOnNetwork;
		_gcTag = gcTag;
	}
	
	public String getName() {
		return _name;
	}
	
	public ManagedObjectReference getNetworkMor() {
		return _morNetwork;
	}
	
	public ManagedObjectReference[] getVMMorsOnNetwork() {
		return _morVMsOnNetwork;
	}
	
	public String getGCTag() {
		return _gcTag;
	}
}
