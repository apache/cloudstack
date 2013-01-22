/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.storage;

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.EndPoint;
import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.log4j.Logger;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.utils.component.ComponentContext;

public class HypervisorHostEndPoint implements EndPoint {
    private static final Logger s_logger = Logger.getLogger(HypervisorHostEndPoint.class);
    private  long hostId;
    private  String hostAddress;
    @Inject
    AgentManager agentMgr;
    @Inject
    HostEndpointRpcServer rpcServer;

    protected HypervisorHostEndPoint() {
      
    }
    
    private void configure(long hostId, String hostAddress) {
        this.hostId = hostId;
        this.hostAddress = hostAddress;
    }
    
    public static HypervisorHostEndPoint getHypervisorHostEndPoint(long hostId, String hostAddress) {
        HypervisorHostEndPoint ep = ComponentContext.inject(HypervisorHostEndPoint.class);
        ep.configure(hostId, hostAddress);
        return ep;
    }
    
    public String getHostAddr() {
        return this.hostAddress;
    }
    
    public long getId() {
        return this.hostId;
    }

    @Override
    public Answer sendMessage(Command cmd) {
        return rpcServer.sendCommand(this, cmd);
    }
    
    @Override
    public void sendMessageAsync(Command cmd, AsyncCompletionCallback<Answer> callback) {
        rpcServer.sendCommandAsync(this, cmd, callback);
    }
}
