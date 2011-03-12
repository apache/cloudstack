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
package com.cloud.agent;

import com.cloud.agent.api.StartupCommand;
import com.cloud.exception.ConnectionException;
import com.cloud.utils.component.Adapter;

/**
 * HostCreator hooks into the AgentManager to be notified of agent connections before the
 * AgentManager knows about the agent that's connecting.
 */
public interface StartupCommandProcessor extends Adapter {

    /**
     * This method is called by AgentManager when an agent made a
     * connection to this server before the AgentManager knows about this agent
     * @param agentId id of the agent
     * @param cmd command sent by the agent to the server on startup.
     * @return true if handled by the creator
     * @throws ConnectionException if host has problems 
     */
    boolean processInitialConnect(StartupCommand[] cmd) throws ConnectionException;
    
    
}
