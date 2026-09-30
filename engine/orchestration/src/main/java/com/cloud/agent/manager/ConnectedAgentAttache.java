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

import java.nio.channels.ClosedChannelException;


import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CancelCommand;
import com.cloud.agent.api.UnsupportedAnswer;
import com.cloud.agent.transport.Request;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.host.Status;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.utils.nio.Link;

/**
 * ConnectedAgentAttache implements a direct connection to this management server.
 */
public class ConnectedAgentAttache extends AgentAttache {

    protected Link _link;
    private static final int CANCEL_COMMAND_WAIT_SECONDS = 60;

    public ConnectedAgentAttache(final AgentManagerImpl agentMgr, final long id, final String uuid, final String name, final Hypervisor.HypervisorType hypervisorType, final Link link, final boolean maintenance) {
        super(agentMgr, id, uuid, name, hypervisorType, maintenance);
        _link = link;
    }

    @Override
    public synchronized void send(final Request req) throws AgentUnavailableException {
        try {
            _link.send(req.toBytes());
        } catch (ClosedChannelException e) {
            throw new AgentUnavailableException("Channel is closed", _id);
        }
    }

    @Override
    public synchronized boolean isClosed() {
        return _link == null;
    }

    /**
     * The command is executing on a remote agent, so only the agent can say whether its backend work
     * can be stopped; ask it. An agent that does not understand the question (an older one, or a
     * resource with nothing to stop) answers unsupported, which is a no.
     */
    @Override
    protected boolean isExecutionCancellable(final long seq) {
        return askAgentToCancel(seq, true);
    }

    @Override
    protected boolean cancelRunning(final long seq) {
        return askAgentToCancel(seq, false);
    }

    private boolean askAgentToCancel(final long seq, final boolean checkOnly) {
        if (_agentMgr == null) {
            return false;
        }
        final CancelCommand cmd = new CancelCommand(seq, "Job cancelled", checkOnly);
        cmd.setWait(CANCEL_COMMAND_WAIT_SECONDS);
        try {
            final Answer answer = _agentMgr.send(_id, cmd);
            if (answer == null || answer instanceof UnsupportedAnswer) {
                logger.debug(LOG_SEQ_FORMATTED_STRING, seq, "Agent does not support cancellation, treating as not cancellable");
                return false;
            }
            if (!answer.getResult()) {
                logger.debug(LOG_SEQ_FORMATTED_STRING, seq, (checkOnly ? "Not cancellable: " : "Not cancelled: ") + answer.getDetails());
            }
            return answer.getResult();
        } catch (final AgentUnavailableException | OperationTimedoutException e) {
            logger.warn(LOG_SEQ_FORMATTED_STRING, seq, "Unable to ask the agent about cancellation: " + e.getMessage());
            return false;
        }
    }

    @Override
    public void disconnect(final Status state) {
        synchronized (this) {
            logger.debug("Processing disconnect [id: {}, uuid: {}, name: {}]", _id, _uuid, _name);

            if (_link != null) {
                logger.debug("Disconnecting from {}, Socket Address: {}", _link.getIpAddress(), _link.getSocketAddress());
                _link.close();
                _link.terminated();
            }
            _link = null;
        }
        cancelAllCommands(state, true);
        _requests.clear();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((_link == null) ? 0 : _link.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        // Return false straight away.
        if (obj == null) {
            return false;
        }
        // No need to handle a ClassCastException. If the classes are different, then equals can return false straight ahead.
        if (this.getClass() != obj.getClass()) {
            return false;
        }
        // This should not be part of the equals() method, but I'm keeping it because it is expected behaviour based
        // on the previous implementation. The link attribute of the other object should be checked here as well
        // to verify if it's not null whilst the this is null.
        if (_link == null) {
            return false;
        }
        ConnectedAgentAttache that = (ConnectedAgentAttache)obj;
        return super.equals(obj) && _link == that._link;
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            assert _link == null : "Duh...Says you....Forgot to call disconnect()!";
            synchronized (this) {
                if (_link != null) {
                    logger.warn("Lost attache {} ({})", _id, _name);
                    disconnect(Status.Alert);
                }
            }
        } finally {
            super.finalize();
        }
    }

}
