/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
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

package com.cloud.agent.mockvm;

import com.cloud.vm.VirtualMachine.State;

// As storage is mapped from storage device, can virtually treat that VM here does
// not need any local storage resource, therefore we don't have attribute here for storage
public class MockVm {
	
	private String vmName;
	private State state = State.Stopped;
	
	private long ramSize;			// unit of Mbytes
	private int cpuCount;
	private int utilization;		// in percentage
	private int vncPort;			// 0-based allocation, real port number needs to be applied with base
	
	public MockVm() {
	}
	
	public MockVm(String vmName, State state, long ramSize, int cpuCount, int utilization, int vncPort) {
		this.vmName = vmName;
		this.state = state;
		this.ramSize = ramSize;
		this.cpuCount = cpuCount;
		this.utilization = utilization;
		this.vncPort = vncPort;
	}
	
	public String getName() {
		return vmName;
	}
	
	public State getState() {
		return state;
	}
	
	public void setState(State state) {
		this.state = state;
	}
	
	public long getRamSize() {
		return ramSize;
	}
	
	public int getCpuCount() {
		return cpuCount;
	}
	
	public int getUtilization() {
		return utilization;
	}
	
	public int getVncPort() {
		return vncPort;
	}
	public static void main(String[] args) {
		long i = 10;
		Long l = null;
		if (i == l) {
			System.out.print("fdfd");
		}
	}
}

