package com.cloud.agent.manager;

public class SimulatorInfo {
	private boolean enabled;
	private int timeout;
	private String hostUuid;
	
	public SimulatorInfo(boolean enabled, int timeout, String hostUuid) {
		this.enabled = enabled;
		this.timeout = timeout;
		this.hostUuid = hostUuid;
	}
	
	public SimulatorInfo() {
		this.enabled = true;
		this.timeout = -1;
		this.hostUuid = null;
	}
	
	public boolean isEnabled() {
		return this.enabled;
	}
	
	public int getTimeout() {
		return this.timeout;
	}
	
	public String getHostUuid() {
		return this.hostUuid;
	}
	
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
	
	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}
	
	public void setHostUuid(String hostUuid) {
		this.hostUuid = hostUuid;
	}
}
