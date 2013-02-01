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
package org.apache.cloudstack.storage.test;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.storage.HostEndpointRpcServer;
import org.apache.cloudstack.storage.HypervisorHostEndPoint;
import org.apache.log4j.Logger;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.utils.component.ComponentContext;


public class MockHostEndpointRpcServerDirectCallResource implements HostEndpointRpcServer {
    private static final Logger s_logger = Logger.getLogger(MockHostEndpointRpcServerDirectCallResource.class);
    private ScheduledExecutorService executor;
    @Inject
    AgentManager agentMgr;
    public MockHostEndpointRpcServerDirectCallResource() {
        executor = Executors.newScheduledThreadPool(10);
    }
    
    public void sendCommandAsync(HypervisorHostEndPoint host, final Command command, final AsyncCompletionCallback<Answer> callback) {
       // new MockRpcCallBack(host.getHostId(), command, callback);
        MockRpcCallBack run = ComponentContext.inject(MockRpcCallBack.class);
        run.setCallback(callback);
        run.setCmd(command);
        run.setHostId(host.getId());
        executor.schedule(run, 10, TimeUnit.SECONDS);
    }

    @Override
    public Answer sendCommand(HypervisorHostEndPoint host, Command command) {
        Answer answer;
        try {
            answer = agentMgr.send(host.getId(), command);
            return answer;
        } catch (AgentUnavailableException e) {
           return null;
        } catch (OperationTimedoutException e) {
           return null;
        }
    }
}
