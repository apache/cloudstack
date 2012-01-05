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

package com.cloud.network;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.cloud.agent.AgentManager;
import com.cloud.agent.Listener;
import com.cloud.agent.api.AgentControlAnswer;
import com.cloud.agent.api.AgentControlCommand;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.ModifySshKeysCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StartupRoutingCommand;
import com.cloud.agent.manager.Commands;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.ConnectionException;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.network.router.VirtualNetworkApplianceManager;



public class SshKeysDistriMonitor implements Listener {
	  private static final Logger s_logger = Logger.getLogger(SshKeysDistriMonitor.class);
	  	AgentManager _agentMgr;
		private final HostDao _hostDao;
		private ConfigurationDao _configDao;
	    public SshKeysDistriMonitor(AgentManager mgr, HostDao host, ConfigurationDao config) {
	    	this._agentMgr = mgr;
	    	_hostDao = host;
	    	_configDao = config;
	    }
	    
	    
	    @Override
	    public boolean isRecurring() {
	        return false;
	    }
	    
	    @Override
	    public synchronized boolean processAnswers(long agentId, long seq, Answer[] resp) {
	        return true;
	    }
	    
	    @Override
	    public synchronized boolean processDisconnect(long agentId, Status state) {
	    	if(s_logger.isTraceEnabled())
	    		s_logger.trace("Agent disconnected, agent id: " + agentId + ", state: " + state + ". Will notify waiters");
	    	
	    
	        return true;
	    }
	    
	    @Override
	    public void processConnect(HostVO host, StartupCommand cmd, boolean forRebalance) throws ConnectionException {
	    	if (cmd instanceof StartupRoutingCommand) {
	    		if (((StartupRoutingCommand) cmd).getHypervisorType() == HypervisorType.KVM ||
	    		    ((StartupRoutingCommand) cmd).getHypervisorType() == HypervisorType.XenServer) {
	    			/*TODO: Get the private/public keys here*/
	    			
	    			Map<String, String> configs = _configDao.getConfiguration("management-server", new HashMap<String, Object>());
	    			String pubKey = configs.get("ssh.publickey");
	    			String prvKey = configs.get("ssh.privatekey");
	    			
	    			try {
	    				ModifySshKeysCommand cmds = new ModifySshKeysCommand(pubKey, prvKey);
		    			Commands c = new Commands(cmds);
	    				_agentMgr.send(host.getId(), c, this);
	    			} catch (AgentUnavailableException e) {
	    				s_logger.debug("Failed to send keys to agent: " + host.getId());
	    			}
	    		}
	    	}
	    }

		@Override
		public int getTimeout() {
			// TODO Auto-generated method stub
			return 0;
		}


		@Override
		public boolean processCommands(long agentId, long seq, Command[] commands) {
			// TODO Auto-generated method stub
			return false;
		}


		@Override
		public AgentControlAnswer processControlCommand(long agentId,
				AgentControlCommand cmd) {
			// TODO Auto-generated method stub
			return null;
		}


		@Override
		public boolean processTimeout(long agentId, long seq) {
			// TODO Auto-generated method stub
			return false;
		}
}
