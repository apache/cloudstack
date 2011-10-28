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
import java.util.Map;


import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.PingTestCommand;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.host.Host.Type;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.resource.ResourceManager;
import com.cloud.utils.component.Inject;
import com.cloud.utils.db.SearchCriteria2;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.db.SearchCriteriaService;

public abstract class AbstractInvestigatorImpl implements Investigator {
    private static final Logger s_logger = Logger.getLogger(AbstractInvestigatorImpl.class);

    private String _name = null;
    @Inject private HostDao _hostDao = null;
    @Inject private AgentManager _agentMgr = null;
    @Inject private ResourceManager _resourceMgr = null;


    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        _name = name;

        return true;
    }

    @Override
    public String getName() {
        return _name;
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }
    
    // Host.status is up and Host.type is routing
    protected List<Long> findHostByPod(long podId, Long excludeHostId) {
    	SearchCriteriaService<HostVO, Long> sc = SearchCriteria2.create(HostVO.class, Long.class);
        sc.addAnd(sc.getEntity().getType(), Op.EQ, Type.Routing);
        sc.addAnd(sc.getEntity().getPodId(), Op.EQ, podId);
        sc.addAnd(sc.getEntity().getStatus(), Op.EQ, Status.Up);
        List<Long> hostIds = sc.list();
        
        if (excludeHostId != null){
            hostIds.remove(excludeHostId);
        }
        
        return hostIds;
    }

    protected Status testIpAddress(Long hostId, String testHostIp) {
        try {
            Answer pingTestAnswer = _agentMgr.send(hostId, new PingTestCommand(testHostIp));
            if(pingTestAnswer == null) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("host (" + testHostIp + ") returns null answer");
                }
            	return null;
            }
            
            if (pingTestAnswer.getResult()) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("host (" + testHostIp + ") has been successfully pinged, returning that host is up");
                }
                // computing host is available, but could not reach agent, return false
                return Status.Up;
            } else {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("host (" + testHostIp + ") cannot be pinged, returning null ('I don't know')");
                }
                return null;
            }
        } catch (AgentUnavailableException e) {
            return null;
        } catch (OperationTimedoutException e) {
            return null;
        }
    }
}
