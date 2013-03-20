// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
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
		return (HostFirewallInfo)_context.getVimClient().getDynamicProperty(_mor, "firewallInfo");
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
