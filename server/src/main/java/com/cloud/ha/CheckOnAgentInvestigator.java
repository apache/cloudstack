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
package com.cloud.ha;

import javax.inject.Inject;


import com.cloud.agent.AgentManager;
import com.cloud.agent.api.CheckVirtualMachineAnswer;
import com.cloud.agent.api.CheckVirtualMachineCommand;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.host.Host;
import com.cloud.host.Status;
import com.cloud.utils.component.AdapterBase;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachine.PowerState;

public class CheckOnAgentInvestigator extends AdapterBase implements Investigator {
    @Inject
    AgentManager _agentMgr;

    protected CheckOnAgentInvestigator() {
    }

    @Override
    public Status isAgentAlive(Host agent) {
        return null;
    }

    @Override
    public boolean isVmAlive(VirtualMachine vm, Host host) throws UnknownVM {
        CheckVirtualMachineCommand cmd = new CheckVirtualMachineCommand(vm.getInstanceName());
        try {
            CheckVirtualMachineAnswer answer = (CheckVirtualMachineAnswer)_agentMgr.send(vm.getHostId(), cmd);
            if (!answer.getResult()) {
                logger.debug("Unable to get vm state on " + vm.toString());
                throw new UnknownVM();
            }

            logger.debug("Agent responded with state " + answer.getState().toString());
            return answer.getState() == PowerState.PowerOn;
        } catch (AgentUnavailableException e) {
            logger.debug("Unable to reach the agent for " + vm.toString() + ": " + e.getMessage());
            throw new UnknownVM();
        } catch (OperationTimedoutException e) {
            logger.debug("Operation timed out for " + vm.toString() + ": " + e.getMessage());
            throw new UnknownVM();
        }
    }
}
