/**
 *  Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
 */

package com.cloud.agent.manager;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.cloud.agent.api.Answer;

import com.cloud.agent.api.Command;

import com.cloud.agent.api.StoragePoolInfo;

import com.cloud.resource.AgentResourceBase;
import com.cloud.simulator.MockHost;
import com.cloud.utils.Pair;
import com.cloud.utils.component.Manager;
import com.cloud.vm.VirtualMachine.State;

public interface SimulatorManager extends Manager {
	public static final String Name = "simulator manager";
	
	public enum AgentType {
		Computing(0), // not used anymore
		Routing(1), 
		Storage(2);

		int value;

		AgentType(int value) {
			this.value = value;
		}

		public int value() {
			return value;
		}
	}

    MockVmManager getVmMgr();

    MockStorageManager getStorageMgr();

    MockAgentManager getAgentMgr();

    Answer simulate(Command cmd, String hostGuid);
    StoragePoolInfo getLocalStorage(String hostGuid);
    
    boolean configureSimulator(Long zoneId, Long podId, Long clusterId, Long hostId, String command, String values);
    public HashMap<String, Pair<Long, Long>> syncNetworkGroups(String hostGuid);

    Map<String, State> getVmStates(String hostGuid);

	Pair<MockHost, StoragePoolInfo> getHostInfo(String uuid);
}