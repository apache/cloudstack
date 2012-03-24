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

 */

package com.cloud.agent.manager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.StoragePoolInfo;
import com.cloud.agent.mockvm.MockVm;
import com.cloud.simulator.MockVMVO;
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

	Map<String, MockVMVO> getVms(String hostGuid);
}