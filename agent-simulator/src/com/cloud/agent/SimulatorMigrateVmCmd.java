/**
 *  Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
 */

package com.cloud.agent;

public class SimulatorMigrateVmCmd extends SimulatorCmd {

	private static final long serialVersionUID = 1L;

    private String destIp;
	
	private String vmName;
	private long ramSize;
	private int cpuCount;
	private int utilization;
	
	public SimulatorMigrateVmCmd(String testCase) {
		super(testCase);
	}

	public String getDestIp() {
		return destIp;
	}

	public void setDestIp(String destIp) {
		this.destIp = destIp;
	}
	
	public String getVmName() {
		return vmName;
	}

	public void setVmName(String vmName) {
		this.vmName = vmName;
	}

	public long getRamSize() {
		return ramSize;
	}

	public void setRamSize(long ramSize) {
		this.ramSize = ramSize;
	}

	public int getCpuCount() {
		return cpuCount;
	}

	public void setCpuCount(int cpuCount) {
		this.cpuCount = cpuCount;
	}

	public int getUtilization() {
		return utilization;
	}

	public void setUtilization(int utilization) {
		this.utilization = utilization;
	}
	
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("SimulatorMigrateVmCmd {").append("vm: ").append(getVmName());
		sb.append(", destIp: ").append(getDestIp()).append(", ramSize: ").append(getRamSize());
		sb.append(", cpuCount: ").append(getCpuCount()).append(", utilization: ").append(getUtilization());
		sb.append("}");
		
		return sb.toString();
	}
}
