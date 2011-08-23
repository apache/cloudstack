/**
 *  Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
 */

package com.cloud.hypervisor.vmware.mo;

import org.apache.log4j.Logger;

import com.cloud.hypervisor.vmware.util.VmwareContext;
import com.vmware.vim25.HostPortGroupSpec;
import com.vmware.vim25.HostVirtualSwitchSpec;
import com.vmware.vim25.ManagedObjectReference;

public class HostNetworkSystemMO extends BaseMO {
    private static final Logger s_logger = Logger.getLogger(HostNetworkSystemMO.class);
    
	public HostNetworkSystemMO(VmwareContext context, ManagedObjectReference morNetworkSystem) {
		super(context, morNetworkSystem);
	}
	
	public HostNetworkSystemMO(VmwareContext context, String morType, String morValue) {
		super(context, morType, morValue);
	}
	
	public void addPortGroup(HostPortGroupSpec spec) throws Exception {
		_context.getService().addPortGroup(_mor, spec);
	}
	
	public void updatePortGroup(String portGroupName, HostPortGroupSpec spec) throws Exception {
		_context.getService().updatePortGroup(_mor, portGroupName, spec);
	}
	
	public void removePortGroup(String portGroupName) throws Exception {
		_context.getService().removePortGroup(_mor, portGroupName);
	}
	
	public void addVirtualSwitch(String vSwitchName, HostVirtualSwitchSpec spec) throws Exception {
		_context.getService().addVirtualSwitch(_mor, vSwitchName, spec);
	}
	
	public void updateVirtualSwitch(String vSwitchName, HostVirtualSwitchSpec spec) throws Exception {
		_context.getService().updateVirtualSwitch(_mor, vSwitchName, spec);
	}
	
	public void removeVirtualSwitch(String vSwitchName) throws Exception {
		_context.getService().removeVirtualSwitch(_mor, vSwitchName);
	}
	
	public void refresh() throws Exception {
		_context.getService().refreshNetworkSystem(_mor);
	}
}

