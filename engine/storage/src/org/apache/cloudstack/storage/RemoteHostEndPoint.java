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

import org.apache.cloudstack.engine.subsystem.api.storage.EndPoint;
import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.log4j.Logger;

import com.cloud.agent.AgentManager;
import com.cloud.agent.Listener;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.manager.Commands;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.exception.CloudRuntimeException;

public class RemoteHostEndPoint implements EndPoint {
    private static final Logger s_logger = Logger.getLogger(RemoteHostEndPoint.class);
    private  long hostId;
    private  String hostAddress;
    @Inject
    AgentManager agentMgr;
    @Inject
    HostEndpointRpcServer rpcServer;
    private ScheduledExecutorService executor;

    protected RemoteHostEndPoint() {
    	executor = Executors.newScheduledThreadPool(10);
    }

    private void configure(long hostId, String hostAddress) {
        this.hostId = hostId;
        this.hostAddress = hostAddress;
    }

    public static RemoteHostEndPoint getHypervisorHostEndPoint(long hostId, String hostAddress) {
        RemoteHostEndPoint ep = ComponentContext.inject(RemoteHostEndPoint.class);
        ep.configure(hostId, hostAddress);
        return ep;
    }

    @Override
    public String getHostAddr() {
        return this.hostAddress;
    }

    public long getId() {
        return this.hostId;
    }

    @Override
    public Answer sendMessage(Command cmd) {
    	String errMsg = null;
    	try {
			return agentMgr.send(getId(), cmd);
		} catch (AgentUnavailableException e) {
			errMsg = e.toString();
			s_logger.debug("Failed to send command, due to Agent:" + getId() + ", " + e.toString());
		} catch (OperationTimedoutException e) {
			errMsg = e.toString();
			s_logger.debug("Failed to send command, due to Agent:" + getId() + ", " + e.toString());
		}
    	throw new CloudRuntimeException("Failed to send command, due to Agent:" + getId() + ", " + errMsg);
    }

    private class CmdRunner implements Runnable {
		final Command cmd;
		final AsyncCompletionCallback<Answer> callback;
		public CmdRunner(Command cmd, AsyncCompletionCallback<Answer> callback) {
			this.cmd = cmd;
			this.callback = callback;
		}
		@Override
		public void run() {
			Answer answer = sendMessage(cmd);
			callback.complete(answer);
		}

	}

    @Override
    public void sendMessageAsync(Command cmd, AsyncCompletionCallback<Answer> callback) {
    	executor.schedule(new CmdRunner(cmd, callback), 10, TimeUnit.SECONDS);
    }

    @Override
    public void sendMessageAsyncWithListener(Command cmd, Listener listener) {
    	try {
    		this.agentMgr.send(getId(), new Commands(cmd), listener);
    	} catch (AgentUnavailableException e) {
    		s_logger.debug("Failed to send command: " + e.toString());
    		throw new CloudRuntimeException("Failed to send command: " + e.toString());
    	}
    }
}
