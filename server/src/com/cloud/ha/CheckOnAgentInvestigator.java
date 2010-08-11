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
package com.cloud.ha;

import javax.ejb.Local;

import org.apache.log4j.Logger;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.CheckVirtualMachineAnswer;
import com.cloud.agent.api.CheckVirtualMachineCommand;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.component.Inject;
import com.cloud.vm.State;
import com.cloud.vm.VMInstanceVO;

@Local(value=Investigator.class)
public class CheckOnAgentInvestigator extends AdapterBase implements Investigator {
    private final static Logger s_logger = Logger.getLogger(CheckOnAgentInvestigator.class);
	@Inject AgentManager _agentMgr;
	
	
	protected CheckOnAgentInvestigator() {
	}
	
	@Override
	public Status isAgentAlive(HostVO agent) {
		return null;
	}

	@Override
	public Boolean isVmAlive(VMInstanceVO vm, HostVO host) {
		CheckVirtualMachineCommand cmd = new CheckVirtualMachineCommand(vm.getInstanceName());
		try {
			CheckVirtualMachineAnswer answer = (CheckVirtualMachineAnswer)_agentMgr.send(vm.getHostId(), cmd, 10 * 1000);
			if (!answer.getResult()) {
				s_logger.debug("Unable to get vm state on " + vm.toString());
				return null;
			}

			s_logger.debug("Agent responded with state " + answer.getState().toString());
			return answer.getState() == State.Running;
		} catch (AgentUnavailableException e) {
			s_logger.debug("Unable to reach the agent for " + vm.toString() + ": " + e.getMessage());
			return null;
		} catch (OperationTimedoutException e) {
			s_logger.debug("Operation timed out for " + vm.toString() + ": " + e.getMessage());
			return null;
		}
	}
}
