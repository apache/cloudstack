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

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.apache.log4j.Logger;

import org.apache.cloudstack.engine.subsystem.api.storage.EndPoint;
import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.managed.context.ManagedContextRunnable;

import com.cloud.agent.AgentManager;
import com.cloud.agent.Listener;
import com.cloud.agent.api.AgentControlAnswer;
import com.cloud.agent.api.AgentControlCommand;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.manager.Commands;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.ConnectionException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.host.Host;
import com.cloud.host.Status;
import com.cloud.hypervisor.HypervisorGuruManager;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.exception.CloudRuntimeException;

public class RemoteHostEndPoint implements EndPoint {
    private static final Logger s_logger = Logger.getLogger(RemoteHostEndPoint.class);
    private long hostId;
    private String hostAddress;
    private String publicAddress;
    @Inject
    AgentManager agentMgr;
    @Inject
    protected HypervisorGuruManager _hvGuruMgr;
    private ScheduledExecutorService executor;

    public RemoteHostEndPoint() {
        executor = Executors.newScheduledThreadPool(10, new NamedThreadFactory("RemoteHostEndPoint"));
    }

    private void configure(long hostId, String hostAddress, String publicAddress) {
        this.hostId = hostId;
        this.hostAddress = hostAddress;
        this.publicAddress = publicAddress;
    }

    public static RemoteHostEndPoint getHypervisorHostEndPoint(long hostId, String hostAddress, String publicAddress) {
        RemoteHostEndPoint ep = ComponentContext.inject(RemoteHostEndPoint.class);
        ep.configure(hostId, hostAddress, publicAddress);
        return ep;
    }

    @Override
    public String getHostAddr() {
        return this.hostAddress;
    }

    @Override
    public String getPublicAddr() {
        return this.publicAddress;
    }

    @Override
    public long getId() {
        return this.hostId;
    }

    @Override
    public Answer sendMessage(Command cmd) {
        String errMsg = null;
        try {
            long newHostId = _hvGuruMgr.getGuruProcessedCommandTargetHost(hostId, cmd);
            return agentMgr.send(newHostId, cmd);
        } catch (AgentUnavailableException e) {
            errMsg = e.toString();
            s_logger.debug("Failed to send command, due to Agent:" + getId() + ", " + e.toString());
        } catch (OperationTimedoutException e) {
            errMsg = e.toString();
            s_logger.debug("Failed to send command, due to Agent:" + getId() + ", " + e.toString());
        }
        throw new CloudRuntimeException("Failed to send command, due to Agent:" + getId() + ", " + errMsg);
    }

    private class CmdRunner extends ManagedContextRunnable implements Listener {
        final AsyncCompletionCallback<Answer> callback;
        Answer answer;

        public CmdRunner(AsyncCompletionCallback<Answer> callback) {
            this.callback = callback;
        }

        @Override
        public boolean processAnswers(long agentId, long seq, Answer[] answers) {
            this.answer = answers[0];
            executor.schedule(this, 10, TimeUnit.SECONDS);
            return true;
        }

        @Override
        public boolean processCommands(long agentId, long seq, Command[] commands) {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public AgentControlAnswer processControlCommand(long agentId, AgentControlCommand cmd) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void processConnect(Host host, StartupCommand cmd, boolean forRebalance) throws ConnectionException {
            // TODO Auto-generated method stub

        }

        @Override
        public boolean processDisconnect(long agentId, Status state) {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean isRecurring() {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public int getTimeout() {
            // TODO Auto-generated method stub
            return -1;
        }

        @Override
        public boolean processTimeout(long agentId, long seq) {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        protected void runInContext() {
            callback.complete(answer);
        }
    }

    @Override
    public void sendMessageAsync(Command cmd, AsyncCompletionCallback<Answer> callback) {
        try {
            long newHostId = _hvGuruMgr.getGuruProcessedCommandTargetHost(this.hostId, cmd);
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Sending command " + cmd.toString() + " to host: " + newHostId);
            }
            agentMgr.send(newHostId, new Commands(cmd), new CmdRunner(callback));
        } catch (AgentUnavailableException e) {
            throw new CloudRuntimeException("Unable to send message", e);
        }
    }
}
