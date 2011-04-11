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
