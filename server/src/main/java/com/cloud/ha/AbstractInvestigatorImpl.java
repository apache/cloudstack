// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.ha;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.naming.ConfigurationException;


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
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.db.QueryBuilder;
import com.cloud.utils.db.SearchCriteria.Op;

public abstract class AbstractInvestigatorImpl extends AdapterBase implements Investigator {

    @Inject
    private final HostDao _hostDao = null;
    @Inject
    private final AgentManager _agentMgr = null;
    @Inject
    private final ResourceManager _resourceMgr = null;

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {

        return true;
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
        QueryBuilder<HostVO> sc = QueryBuilder.create(HostVO.class);
        sc.and(sc.entity().getType(), Op.EQ, Type.Routing);
        sc.and(sc.entity().getPodId(), Op.EQ, podId);
        sc.and(sc.entity().getStatus(), Op.EQ, Status.Up);
        List<HostVO> hosts = sc.list();

        List<Long> hostIds = new ArrayList<Long>(hosts.size());
        for (HostVO h : hosts) {
            hostIds.add(h.getId());
        }

        if (excludeHostId != null) {
            hostIds.remove(excludeHostId);
        }

        return hostIds;
    }

    // Method only returns Status.Up, Status.Down and Status.Unknown
    protected Status testIpAddress(Long hostId, String testHostIp) {
        try {
            Answer pingTestAnswer = _agentMgr.send(hostId, new PingTestCommand(testHostIp));
            if (pingTestAnswer == null) {
                if (logger.isDebugEnabled()) {
                    logger.debug("host (" + testHostIp + ") returns Unknown (null) answer");
                }
                return Status.Unknown;
            }

            if (pingTestAnswer.getResult()) {
                if (logger.isDebugEnabled()) {
                    logger.debug("host (" + testHostIp + ") has been successfully pinged, returning that host is up");
                }
                // computing host is available, but could not reach agent, return false
                return Status.Up;
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("host (" + testHostIp + ") cannot be pinged, returning Unknown (I don't know) state");
                }
                return Status.Unknown;
            }
        } catch (AgentUnavailableException e) {
            if (logger.isDebugEnabled()) {
                logger.debug("host (" + testHostIp + "): " + e.getLocalizedMessage() + ", trapped AgentUnavailableException returning Unknown state");
            }
            return Status.Unknown;
        } catch (OperationTimedoutException e) {
            if (logger.isDebugEnabled()) {
                logger.debug("host (" + testHostIp + "): " + e.getLocalizedMessage() + ", trapped OperationTimedoutException returning Unknown state");
            }
            return Status.Unknown;
        }
    }
}
