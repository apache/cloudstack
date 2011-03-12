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
import com.cloud.agent.api.StartupStorageCommand;
import com.cloud.capacity.dao.CapacityDao;
import com.cloud.exception.ConnectionException;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.utils.db.SearchCriteria;


public class ComputeCapacityListener implements Listener {
    private static final Logger s_logger = Logger.getLogger(ComputeCapacityListener.class);
    CapacityDao _capacityDao;
    float _cpuOverProvisioningFactor = 1.0f;


    public ComputeCapacityListener(CapacityDao _capacityDao,
            float _overProvisioningFactor) {
        super();
        this._capacityDao = _capacityDao;
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
    public void processConnect(HostVO server, StartupCommand startup) throws ConnectionException {
        if (!(startup instanceof StartupRoutingCommand)) {
            return;
        }


        SearchCriteria<CapacityVO> capacityCPU = _capacityDao
        .createSearchCriteria();
        capacityCPU.addAnd("hostOrPoolId", SearchCriteria.Op.EQ,
                server.getId());
        capacityCPU.addAnd("dataCenterId", SearchCriteria.Op.EQ,
                server.getDataCenterId());
        capacityCPU
        .addAnd("podId", SearchCriteria.Op.EQ, server.getPodId());
        capacityCPU.addAnd("capacityType", SearchCriteria.Op.EQ,
                CapacityVO.CAPACITY_TYPE_CPU);
        List<CapacityVO> capacityVOCpus = _capacityDao.search(capacityCPU,
                null);

        if (capacityVOCpus != null && !capacityVOCpus.isEmpty()) {
            CapacityVO CapacityVOCpu = capacityVOCpus.get(0);
            long newTotalCpu = (long) (server.getCpus().longValue()
                    * server.getSpeed().longValue() * _cpuOverProvisioningFactor);
            if ((CapacityVOCpu.getTotalCapacity() <= newTotalCpu)
                    || ((CapacityVOCpu.getUsedCapacity() + CapacityVOCpu
                            .getReservedCapacity()) <= newTotalCpu)) {
                CapacityVOCpu.setTotalCapacity(newTotalCpu);
            } else if ((CapacityVOCpu.getUsedCapacity()
                    + CapacityVOCpu.getReservedCapacity() > newTotalCpu)
                    && (CapacityVOCpu.getUsedCapacity() < newTotalCpu)) {
                CapacityVOCpu.setReservedCapacity(0);
                CapacityVOCpu.setTotalCapacity(newTotalCpu);
            } else {
                s_logger.debug("What? new cpu is :" + newTotalCpu
                        + ", old one is " + CapacityVOCpu.getUsedCapacity()
                        + "," + CapacityVOCpu.getReservedCapacity() + ","
                        + CapacityVOCpu.getTotalCapacity());
            }
            _capacityDao.update(CapacityVOCpu.getId(), CapacityVOCpu);
        } else {
            CapacityVO capacity = new CapacityVO(
                    server.getId(),
                    server.getDataCenterId(),
                    server.getPodId(), 
                    server.getClusterId(),
                    0L,
                    (long) (server.getCpus().longValue()
                            * server.getSpeed().longValue() * _cpuOverProvisioningFactor),
                            CapacityVO.CAPACITY_TYPE_CPU);
            _capacityDao.persist(capacity);
        }

        SearchCriteria<CapacityVO> capacityMem = _capacityDao
        .createSearchCriteria();
        capacityMem.addAnd("hostOrPoolId", SearchCriteria.Op.EQ,
                server.getId());
        capacityMem.addAnd("dataCenterId", SearchCriteria.Op.EQ,
                server.getDataCenterId());
        capacityMem
        .addAnd("podId", SearchCriteria.Op.EQ, server.getPodId());
        capacityMem.addAnd("capacityType", SearchCriteria.Op.EQ,
                CapacityVO.CAPACITY_TYPE_MEMORY);
        List<CapacityVO> capacityVOMems = _capacityDao.search(capacityMem,
                null);

        if (capacityVOMems != null && !capacityVOMems.isEmpty()) {
            CapacityVO CapacityVOMem = capacityVOMems.get(0);
            long newTotalMem = server.getTotalMemory();
            if (CapacityVOMem.getTotalCapacity() <= newTotalMem
                    || (CapacityVOMem.getUsedCapacity()
                            + CapacityVOMem.getReservedCapacity() <= newTotalMem)) {
                CapacityVOMem.setTotalCapacity(newTotalMem);
            } else if (CapacityVOMem.getUsedCapacity()
                    + CapacityVOMem.getReservedCapacity() > newTotalMem
                    && CapacityVOMem.getUsedCapacity() < newTotalMem) {
                CapacityVOMem.setReservedCapacity(0);
                CapacityVOMem.setTotalCapacity(newTotalMem);
            } else {
                s_logger.debug("What? new cpu is :" + newTotalMem
                        + ", old one is " + CapacityVOMem.getUsedCapacity()
                        + "," + CapacityVOMem.getReservedCapacity() + ","
                        + CapacityVOMem.getTotalCapacity());
            }
            _capacityDao.update(CapacityVOMem.getId(), CapacityVOMem);
        } else {
            CapacityVO capacity = new CapacityVO(server.getId(),
                    server.getDataCenterId(), server.getPodId(), server.getClusterId(), 0L,
                    server.getTotalMemory(),
                    CapacityVO.CAPACITY_TYPE_MEMORY);
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
