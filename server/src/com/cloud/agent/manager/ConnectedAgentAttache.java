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

import java.nio.channels.ClosedChannelException;

import org.apache.log4j.Logger;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Command;
import com.cloud.agent.transport.Request;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.host.Status;
import com.cloud.utils.nio.Link;

/**
 * ConnectedAgentAttache implements an direct connection to this management server.
 */
public class ConnectedAgentAttache extends AgentAttache {
    private static final Logger s_logger = Logger.getLogger(ConnectedAgentAttache.class);

    protected Link _link;

    public ConnectedAgentAttache(AgentManagerImpl agentMgr, final long id, final Link link, boolean maintenance) {
        super(agentMgr, id, maintenance);
        _link = link;
    }

    @Override
    public synchronized void send(Request req) throws AgentUnavailableException {
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
            s_logger.debug("Processing Disconnect.");
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
    public boolean equals(Object obj) {
        try {
            ConnectedAgentAttache that = (ConnectedAgentAttache) obj;
            return super.equals(obj) && this._link == that._link && this._link != null;
        } catch (ClassCastException e) {
            assert false : "Who's sending an " + obj.getClass().getSimpleName() + " to " + this.getClass().getSimpleName() + ".equals()? ";
        return false;
        }
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            assert _link == null : "Duh...Says you....Forgot to call disconnect()!";
            synchronized (this) {
                if (_link != null) {
                    s_logger.warn("Lost attache " + _id);
                    disconnect(Status.Alert);
                }
            }
        } finally {
            super.finalize();
        }
    }

    @Override
    public void updatePassword(Command newPassword) {
        throw new IllegalStateException("Should not have come here ");
    }
}
