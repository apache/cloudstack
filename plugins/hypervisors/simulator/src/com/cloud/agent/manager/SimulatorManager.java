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
package com.cloud.agent.manager;

import java.util.HashMap;
import java.util.Map;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.StoragePoolInfo;
import com.cloud.simulator.MockConfigurationVO;
import com.cloud.simulator.MockVMVO;
import com.cloud.simulator.dao.MockConfigurationDao;
import com.cloud.utils.Pair;
import com.cloud.utils.component.Manager;
import com.cloud.vm.VirtualMachine.PowerState;

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

    Long configureSimulator(Long zoneId, Long podId, Long clusterId, Long hostId, String command, String values, Integer count, String jsonResponse);

    public HashMap<String, Pair<Long, Long>> syncNetworkGroups(String hostGuid);

    Map<String, PowerState> getVmStates(String hostGuid);

    Map<String, MockVMVO> getVms(String hostGuid);

    MockConfigurationVO querySimulatorMock(Long id);

    boolean clearSimulatorMock(Long id);

    MockConfigurationDao getMockConfigurationDao();
}
