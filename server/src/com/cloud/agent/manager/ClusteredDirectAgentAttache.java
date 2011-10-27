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
package com.cloud.agent.manager;

import com.cloud.agent.AgentManager;
import com.cloud.agent.transport.Request;
import com.cloud.agent.transport.Response;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.UnsupportedVersionException;
import com.cloud.resource.ServerResource;
import com.cloud.utils.exception.CloudRuntimeException;

public class ClusteredDirectAgentAttache extends DirectAgentAttache implements Routable {
    private final ClusteredAgentManagerImpl _mgr;
    private final long _nodeId;

    public ClusteredDirectAgentAttache(AgentManagerImpl agentMgr, long id, long mgmtId, ServerResource resource, boolean maintenance, ClusteredAgentManagerImpl mgr) {
        super(agentMgr, id, resource, maintenance, mgr);
        _mgr = mgr;
        _nodeId = mgmtId;
    }

    @Override
    public void routeToAgent(byte[] data) throws AgentUnavailableException {
        Request req;
        try {
            req = Request.parse(data);
        } catch (ClassNotFoundException e) {
            throw new CloudRuntimeException("Unable to rout to an agent ", e);
        } catch (UnsupportedVersionException e) {
            throw new CloudRuntimeException("Unable to rout to an agent ", e);
        }

        if (req instanceof Response) {
            super.process(((Response) req).getAnswers());
        } else {
            super.send(req);
        }
    }

    @Override
    public boolean processAnswers(long seq, Response response) {
        long mgmtId = response.getManagementServerId();
        if (mgmtId != -1 && mgmtId != _nodeId) {
            _mgr.routeToPeer(Long.toString(mgmtId), response.getBytes());
            if (response.executeInSequence()) {
                sendNext(response.getSequence());
            }
            return true;
        } else {
            return super.processAnswers(seq, response);
        }
    }
    
}
