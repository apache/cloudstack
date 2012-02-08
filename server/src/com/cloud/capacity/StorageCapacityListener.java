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
import com.cloud.agent.api.StartupStorageCommand;
import com.cloud.capacity.dao.CapacityDao;
import com.cloud.capacity.dao.CapacityDaoImpl;
import com.cloud.exception.ConnectionException;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.storage.Storage;
import com.cloud.utils.db.SearchCriteria;


public class StorageCapacityListener implements Listener {
    
    CapacityDao _capacityDao;
    float _overProvisioningFactor = 1.0f;    


    public StorageCapacityListener(CapacityDao _capacityDao,
            float _overProvisioningFactor) {
        super();
        this._capacityDao = _capacityDao;
        this._overProvisioningFactor = _overProvisioningFactor;
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
        
        if (!(startup instanceof StartupStorageCommand)) {
            return;
        }
        SearchCriteria<CapacityVO> capacitySC = _capacityDao.createSearchCriteria();
        capacitySC.addAnd("hostOrPoolId", SearchCriteria.Op.EQ, server.getId());
        capacitySC.addAnd("dataCenterId", SearchCriteria.Op.EQ,
                server.getDataCenterId());
        capacitySC.addAnd("podId", SearchCriteria.Op.EQ, server.getPodId());
        List<CapacityVO> capacities = _capacityDao.search(capacitySC, null);


        StartupStorageCommand ssCmd = (StartupStorageCommand) startup;
        if (ssCmd.getResourceType() == Storage.StorageResourceType.STORAGE_HOST) {
            CapacityVO capacity = new CapacityVO(server.getId(),
                    server.getDataCenterId(), server.getPodId(), server.getClusterId(), 0L,
                    (long) (server.getTotalSize() * _overProvisioningFactor),
                    CapacityVO.CAPACITY_TYPE_STORAGE_ALLOCATED);
            _capacityDao.persist(capacity);
        }

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
