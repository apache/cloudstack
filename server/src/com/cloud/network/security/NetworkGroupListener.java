/**
 *  Copyright (C) 2010 Cloud.com.  All rights reserved.
 *
 * This software is licensed under the GNU General Public License v3 or later. 
 *
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later
version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.cloud.network.security;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;

import com.cloud.agent.AgentManager;
import com.cloud.agent.Listener;
import com.cloud.agent.api.AgentControlAnswer;
import com.cloud.agent.api.AgentControlCommand;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.NetworkIngressRuleAnswer;
import com.cloud.agent.api.PingRoutingWithNwGroupsCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.network.security.NetworkGroupWorkVO.Step;
import com.cloud.network.security.dao.NetworkGroupWorkDao;

/**
 * Listens for answers to ingress rules modification commands
 *
 */
public class NetworkGroupListener implements Listener {
    public static final Logger s_logger = Logger.getLogger(NetworkGroupListener.class.getName());

	NetworkGroupManagerImpl _networkGroupManager;
    AgentManager _agentMgr;
    NetworkGroupWorkDao _workDao;
    

	public NetworkGroupListener(NetworkGroupManagerImpl networkGroupManager,
			AgentManager agentMgr, NetworkGroupWorkDao workDao) {
		super();
		_networkGroupManager = networkGroupManager;
		_agentMgr = agentMgr;
		_workDao = workDao;
	}


	@Override
	public int getTimeout() {
    	return -1;
	}


	@Override
	public boolean isRecurring() {
		return false;
	}


	@Override
	public boolean processAnswer(long agentId, long seq, Answer[] answers) {
		Set<Long> affectedVms = new HashSet<Long>();
		int commandNum = 0;
		for (Answer ans: answers) {
			if (ans instanceof NetworkIngressRuleAnswer) {
				NetworkIngressRuleAnswer ruleAnswer = (NetworkIngressRuleAnswer) ans;
				if (ans.getResult()) {
					s_logger.debug("Successfully programmed rule " + ruleAnswer.toString() + " into host " + agentId);
					_workDao.updateStep(ruleAnswer.getVmId(), ruleAnswer.getLogSequenceNumber(), Step.Done);

				} else {
					_workDao.updateStep(ruleAnswer.getVmId(), ruleAnswer.getLogSequenceNumber(), Step.Error);
					s_logger.debug("Failed to program rule " + ruleAnswer.toString() + " into host " + agentId);
					affectedVms.add(ruleAnswer.getVmId());
				}
				commandNum++;
			}
		}
		_networkGroupManager.scheduleRulesetUpdateToHosts(affectedVms, false, new Long(10*1000l));

        return true;
	}

	@Override
	public boolean processCommand(long agentId, long seq, Command[] commands) {
		boolean processed = false;
        for (Command cmd : commands) {
            if (cmd instanceof PingRoutingWithNwGroupsCommand) {
            	PingRoutingWithNwGroupsCommand ping = (PingRoutingWithNwGroupsCommand)cmd;
                if (ping.getNewGroupStates().size() > 0) {
                    _networkGroupManager.fullSync(agentId, ping.getNewGroupStates());
                }
                processed = true;
            }
        }
        return processed;
	}


	@Override
	public boolean processConnect(HostVO host, StartupCommand cmd) {
		
		return true;
	}


	@Override
	public AgentControlAnswer processControlCommand(long agentId,
			AgentControlCommand cmd) {
		return null;
	}


	@Override
	public boolean processDisconnect(long agentId, Status state) {
		return true;
	}


	@Override
	public boolean processTimeout(long agentId, long seq) {
		return true;
	}

}
