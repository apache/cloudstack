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


import com.cloud.agent.transport.Request;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.host.Status;
import com.cloud.utils.nio.Link;

/**
 * ConnectedAgentAttache implements a direct connection to this management server.
 */
public class ConnectedAgentAttache extends AgentAttache {

    protected Link _link;

    public ConnectedAgentAttache(final AgentManagerImpl agentMgr, final long id, final String name, final Link link, final boolean maintenance) {
        super(agentMgr, id, name, maintenance);
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

    @Override
    public void disconnect(final Status state) {
        synchronized (this) {
            logger.debug("Processing Disconnect.");
            if (_link != null) {
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
