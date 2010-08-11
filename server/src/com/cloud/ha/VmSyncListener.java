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

import java.util.List;

import org.apache.log4j.Logger;

import com.cloud.agent.AgentManager;
import com.cloud.agent.Listener;
import com.cloud.agent.api.AgentControlAnswer;
import com.cloud.agent.api.AgentControlCommand;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.PingRoutingCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StartupRoutingCommand;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;

public class VmSyncListener implements Listener {
    private static final Logger s_logger = Logger.getLogger(VmSyncListener.class);
    
    long _hostId;
    HostDao _dao;
    HighAvailabilityManagerImpl _haMgr;
    AgentManager _agentMgr;
    
    public VmSyncListener(HighAvailabilityManagerImpl mgr, AgentManager agentMgr) {
        _haMgr = mgr;
        _agentMgr = agentMgr;
        
    }
    public VmSyncListener(HostDao dao, long hostId) {
        _dao = dao;
        _hostId = hostId;
    }
    
    @Override
    public boolean isRecurring() {
        return false;
    }

    @Override
    public boolean processAnswer(long agentId, long seq, Answer[] answers) {
        for (final Answer answer : answers) {
            if (!answer.getResult()) {
                s_logger.warn("Cleanup failed due to " + answer.getDetails());
            } else {
                    if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Cleanup succeeded. Details " + answer.getDetails());
                }
            }
        }
        return true;
    }
    
    @Override
    public boolean processTimeout(long agentId, long seq) {
    	return true;
    }
    
    @Override
    public int getTimeout() {
    	return -1;
    }

    @Override
    public boolean processCommand(long agentId, long seq, Command[] req) {
        boolean processed = false;
        for (Command cmd : req) {
            if (cmd instanceof PingRoutingCommand) {
                PingRoutingCommand ping = (PingRoutingCommand)cmd;
                if (ping.getNewStates().size() > 0) {
                    List<Command> commands = _haMgr.deltaSync(agentId, ping.getNewStates());
                    if (commands.size() > 0) {
                        try {
                            _agentMgr.send(agentId, commands.toArray(new Command[commands.size()]), false, this);
                        } catch (final AgentUnavailableException e) {
                            s_logger.warn("Agent is now unavailable", e);
                        }
                    }
                }
                processed = true;
            }
        }
        return processed;
    }

    @Override
    public AgentControlAnswer processControlCommand(long agentId, AgentControlCommand cmd) {
    	return null;
    }
    
    @Override
    public boolean processDisconnect(long agentId, Status state) {
        return true;
    }
    
    @Override
    public boolean processConnect(HostVO agent, StartupCommand cmd) {
        if (!(cmd instanceof StartupRoutingCommand)) {
            return true;
        }
        
        long agentId = agent.getId();
        
        StartupRoutingCommand startup = (StartupRoutingCommand)cmd;
        
        List<Command> commands = _haMgr.fullSync(agentId, startup.getVmStates());
        s_logger.debug("Sending clean commands to the agent");

        if (commands.size() > 0) {
            final Command[] cmds = commands.toArray(new Command[commands.size()]);
            try {
                _agentMgr.send(agentId, cmds, false, this);
            } catch (final AgentUnavailableException e) {
                s_logger.warn("Agent is unavailable now", e);
            }
        }
        
        return true;
    }
}
