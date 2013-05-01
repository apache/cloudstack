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
package com.cloud.agent.api;

import java.net.URI;

import com.cloud.utils.net.NetUtils;

public class PvlanSetupCommand extends Command {
	public enum Type {
		DHCP,
		VM,
		VM_IN_DHCP_HOST
	}
	private String op;
	private String bridge;
	private String primary;
	private String isolated;
	private String vmMac;
	private String dhcpMac;
	private String dhcpIp;
	private boolean strict;
	private Type type;

	protected PvlanSetupCommand() {}
	
	protected PvlanSetupCommand(Type type, String op, String bridge, URI uri)
	{
		this.type = type;
		this.op = op;
		this.bridge = bridge;
		this.primary = NetUtils.getPrimaryPvlanFromUri(uri);
		this.isolated = NetUtils.getIsolatedPvlanFromUri(uri);
		this.strict = true;
	}
	
	static public PvlanSetupCommand createDhcpSetup(String op, String bridge, URI uri, String dhcpMac, String dhcpIp)
	{
		PvlanSetupCommand cmd = new PvlanSetupCommand(Type.DHCP, op, bridge, uri);
		cmd.setDhcpMac(dhcpMac);
		cmd.setDhcpIp(dhcpIp);
		return cmd;
	}
	
	static public PvlanSetupCommand createVmSetup(String op, String bridge, URI uri, String vmMac)
	{
		PvlanSetupCommand cmd = new PvlanSetupCommand(Type.VM, op, bridge, uri);
		cmd.setVmMac(vmMac);
		return cmd;
	}
	
	static public PvlanSetupCommand createVmInDhcpHostSetup(String op, String bridge, URI uri, String dhcpMac, String vmMac)
	{
		PvlanSetupCommand cmd = new PvlanSetupCommand(Type.VM_IN_DHCP_HOST, op, bridge, uri);
		cmd.setDhcpMac(dhcpMac);
		cmd.setVmMac(vmMac);
		return cmd;
	}
	
	@Override
	public boolean executeInSequence() {
		return true;
	}

	public String getOp() {
		return op;
	}

	public String getBridge() {
		return bridge;
	}

	public String getPrimary() {
		return primary;
	}

	public String getIsolated() {
		return isolated;
	}

	public String getVmMac() {
		return vmMac;
	}

	protected void setVmMac(String vmMac) {
		this.vmMac = vmMac;
	}

	public String getDhcpMac() {
		return dhcpMac;
	}

	protected void setDhcpMac(String dhcpMac) {
		this.dhcpMac = dhcpMac;
	}

	public String getDhcpIp() {
		return dhcpIp;
	}

	protected void setDhcpIp(String dhcpIp) {
		this.dhcpIp = dhcpIp;
	}

	public Type getType() {
		return type;
	}

	public boolean isStrict() {
		return strict;
	}

	public void setStrict(boolean strict) {
		this.strict = strict;
	}
}
