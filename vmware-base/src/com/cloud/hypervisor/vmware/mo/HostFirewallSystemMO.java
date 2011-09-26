/**
 *  Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
 */

package com.cloud.hypervisor.vmware.mo;

import org.apache.log4j.Logger;

import com.cloud.hypervisor.vmware.util.VmwareContext;
import com.vmware.vim25.HostFirewallDefaultPolicy;
import com.vmware.vim25.HostFirewallInfo;
import com.vmware.vim25.ManagedObjectReference;

public class HostFirewallSystemMO extends BaseMO {
    private static final Logger s_logger = Logger.getLogger(HostFirewallSystemMO.class);

	public HostFirewallSystemMO(VmwareContext context, ManagedObjectReference morFirewallSystem) {
		super(context, morFirewallSystem);
	}
	
	public HostFirewallSystemMO(VmwareContext context, String morType, String morValue) {
		super(context, morType, morValue);
	}
	
	public HostFirewallInfo getFirewallInfo() throws Exception {
		return (HostFirewallInfo)_context.getServiceUtil().getDynamicProperty(_mor, "firewallInfo");
	}
	
	public void updateDefaultPolicy(HostFirewallDefaultPolicy policy) throws Exception {
		_context.getService().updateDefaultPolicy(_mor, policy);
	}
	
	public void enableRuleset(String rulesetName) throws Exception {
		_context.getService().enableRuleset(_mor, rulesetName);
	}
	
	public void disableRuleset(String rulesetName) throws Exception {
		_context.getService().disableRuleset(_mor, rulesetName);
	}
	
	public void refreshFirewall() throws Exception {
		_context.getService().refreshFirewall(_mor);
	}
}
