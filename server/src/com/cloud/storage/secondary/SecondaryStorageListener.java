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
package com.cloud.storage.secondary;

import org.apache.log4j.Logger;

import com.cloud.agent.AgentManager;
import com.cloud.agent.Listener;
import com.cloud.agent.api.AgentControlAnswer;
import com.cloud.agent.api.AgentControlCommand;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StartupSecondaryStorageCommand;
import com.cloud.agent.api.StartupStorageCommand;
import com.cloud.cluster.ManagementServerNode;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.Status.Event;
import com.cloud.storage.Storage;

public class SecondaryStorageListener implements Listener {
    private final static Logger s_logger = Logger.getLogger(SecondaryStorageListener.class);
    
    SecondaryStorageVmManager _ssVmMgr = null;
    AgentManager _agentMgr = null;
    public SecondaryStorageListener(SecondaryStorageVmManager ssVmMgr, AgentManager agentMgr) {
        _ssVmMgr = ssVmMgr;
        _agentMgr = agentMgr;
    }
    
    @Override
    public boolean isRecurring() {
        return true;
    }

    @Override
    public boolean processAnswers(long agentId, long seq, Answer[] answers) {
    	boolean processed = false;
    	if(answers != null) {
    		for(int i = 0; i < answers.length; i++) {
    		}
    	}
    	
        return processed;
    }

    @Override
    public boolean processCommands(long agentId, long seq, Command[] commands) {
        return false;
    }
    
    @Override
    public AgentControlAnswer processControlCommand(long agentId, AgentControlCommand cmd) {
    	return null;
    }

    @Override
    public void processConnect(HostVO agent, StartupCommand cmd, boolean forRebalance) {
        if ((cmd instanceof StartupStorageCommand) ) {
            StartupStorageCommand scmd = (StartupStorageCommand)cmd;
            if (scmd.getResourceType() ==  Storage.StorageResourceType.SECONDARY_STORAGE ) {
                _ssVmMgr.generateSetupCommand(agent.getId());
                return;
            }
        } else if (cmd instanceof StartupSecondaryStorageCommand) {
            if(s_logger.isInfoEnabled()) {
                s_logger.info("Received a host startup notification " + cmd);
            }
            _ssVmMgr.onAgentConnect(agent.getDataCenterId(), cmd);
            _ssVmMgr.generateSetupCommand(agent.getId());
            _ssVmMgr.generateFirewallConfiguration(agent.getId());
            _ssVmMgr.generateVMSetupCommand(agent.getId());
            return;
        } 
        return;
    }
    
    @Override
    public boolean processDisconnect(long agentId, Status state) {
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
}
