/**
 *  Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
 */

package com.cloud.simulator;

import com.cloud.vm.VirtualMachine.State;

// As storage is mapped from storage device, can virtually treat that VM here does
// not need any local storage resource, therefore we don't have attribute here for storage
public interface MockVm {
	
	
	public String getName();
	
	public State getState();
	
	public void setState(State state);
	
	public void setHostId(long hostId);
	public long getMemory();
	
	public int getCpu();
	public String getType();
	public int getVncPort();
	
	public void setName(String name);
	public void setMemory(long memory);
	public void setCpu(int cpu);
	public void setType(String type);
	public void setVncPort(int vncPort);
	public long getId();
}

