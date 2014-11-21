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
package com.cloud.agent.manager;

import com.cloud.agent.transport.Request;
import com.cloud.agent.transport.Response;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.UnsupportedVersionException;
import com.cloud.resource.ServerResource;
import com.cloud.utils.exception.CloudRuntimeException;

public class ClusteredDirectAgentAttache extends DirectAgentAttache implements Routable {
    private final long _nodeId;

    public ClusteredDirectAgentAttache(ClusteredAgentManagerImpl agentMgr, long id, String name, long mgmtId, ServerResource resource, boolean maintenance) {
        super(agentMgr, id, name, resource, maintenance);
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
            super.process(((Response)req).getAnswers());
        } else {
            super.send(req);
        }
    }

    @Override
    public boolean processAnswers(long seq, Response response) {
        long mgmtId = response.getManagementServerId();
        if (mgmtId != -1 && mgmtId != _nodeId) {
            ((ClusteredAgentManagerImpl)_agentMgr).routeToPeer(Long.toString(mgmtId), response.getBytes());
            if (response.executeInSequence()) {
                sendNext(response.getSequence());
            }
            return true;
        } else {
            return super.processAnswers(seq, response);
        }
    }

}
