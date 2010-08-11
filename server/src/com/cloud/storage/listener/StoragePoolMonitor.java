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
package com.cloud.storage.listener;

import java.util.List;

import org.apache.log4j.Logger;

import com.cloud.agent.Listener;
import com.cloud.agent.api.AgentControlAnswer;
import com.cloud.agent.api.AgentControlCommand;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StartupRoutingCommand;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.hypervisor.Hypervisor.Type;
import com.cloud.storage.StorageManager;
import com.cloud.storage.StoragePoolVO;
import com.cloud.storage.dao.StoragePoolDao;

public class StoragePoolMonitor implements Listener {
    private static final Logger s_logger = Logger.getLogger(StoragePoolMonitor.class);
	private final HostDao _hostDao;
	private final StorageManager _storageManager;
	private final StoragePoolDao _poolDao;
	
    public StoragePoolMonitor(StorageManager mgr, HostDao hostDao, StoragePoolDao poolDao) {
    	this._storageManager = mgr;
    	this._hostDao = hostDao;
    	this._poolDao = poolDao;
    }
    
    
    @Override
    public boolean isRecurring() {
        return false;
    }
    
    @Override
    public synchronized boolean processAnswer(long agentId, long seq, Answer[] resp) {
        return true;
    }
    
    @Override
    public synchronized boolean processDisconnect(long agentId, Status state) {
    
        return true;
    }
    
    @Override
    public boolean processConnect(HostVO host, StartupCommand cmd) {
    	if (cmd instanceof StartupRoutingCommand) {
    		StartupRoutingCommand scCmd = (StartupRoutingCommand)cmd;
    		if (scCmd.getHypervisorType() == Hypervisor.Type.XenServer || scCmd.getHypervisorType() ==  Hypervisor.Type.KVM) {
    			List<StoragePoolVO> pools = _poolDao.listBy(host.getDataCenterId(), host.getPodId(), host.getClusterId());
    			for (StoragePoolVO pool : pools) {
    				Long hostId = host.getId();
    				s_logger.debug("Host " + hostId + " connected, sending down storage pool information ...");
    				if(_storageManager.addPoolToHost(hostId, pool)){
    					_storageManager.createCapacityEntry(pool);
    				}
    			}
    		}
    	}
    	return true;
    }
    

    @Override
    public boolean processCommand(long agentId, long seq, Command[] req) {
        return false;
    }
   
    @Override
    public AgentControlAnswer processControlCommand(long agentId, AgentControlCommand cmd) {
    	return null;
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
