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
package com.cloud.capacity;

import java.util.List;

import org.apache.log4j.Logger;

import com.cloud.agent.Listener;
import com.cloud.agent.api.AgentControlAnswer;
import com.cloud.agent.api.AgentControlCommand;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StartupRoutingCommand;
import com.cloud.capacity.dao.CapacityDao;
import com.cloud.exception.ConnectionException;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.utils.component.Inject;
import com.cloud.utils.db.SearchCriteria;


public class ComputeCapacityListener implements Listener {
    private static final Logger s_logger = Logger.getLogger(ComputeCapacityListener.class);
    CapacityDao _capacityDao;   
    CapacityManager _capacityMgr;
    float _cpuOverProvisioningFactor = 1.0f;


    public ComputeCapacityListener(CapacityDao _capacityDao,
    		CapacityManager _capacityMgr,
            float _overProvisioningFactor) {
        super();
        this._capacityDao = _capacityDao;
        this._capacityMgr = _capacityMgr;
        this._cpuOverProvisioningFactor = _overProvisioningFactor;
    }


    @Override
    public boolean processAnswers(long agentId, long seq, Answer[] answers) {
        return false;
    }


    @Override
    public boolean processCommands(long agentId, long seq, Command[] commands) {
        return false;
    }


    @Override
    public AgentControlAnswer processControlCommand(long agentId,
            AgentControlCommand cmd) {

        return null;
    }


    @Override
    public void processConnect(HostVO server, StartupCommand startup, boolean forRebalance) throws ConnectionException {
        if (!(startup instanceof StartupRoutingCommand)) {
            return;
        }
        _capacityMgr.updateCapacityForHost(server);        
    }




    @Override
    public boolean processDisconnect(long agentId, Status state) {
        return false;
    }


    @Override
    public boolean isRecurring() {
        return false;
    }


    @Override
    public int getTimeout() {
        return 0;
    }


    @Override
    public boolean processTimeout(long agentId, long seq) {
        return false;
    }
    
}
