package com.cloud.agent.api.baremetal;

import com.cloud.agent.api.Command;

public class PrepareLinMinPxeServerCommand extends PreparePxeServerCommand {

	public PrepareLinMinPxeServerCommand(String ip, String mac, String netMask, String gateway, String dns, String template, String vmName, String hostName) {
		super(ip, mac, netMask, gateway, dns, template, vmName, hostName);
	}
}
