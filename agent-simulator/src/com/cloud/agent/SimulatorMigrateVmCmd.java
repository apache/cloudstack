/**
 * *  Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved
*
 *
 * This software is licensed under the GNU General Public License v3 or later.
 *
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
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
