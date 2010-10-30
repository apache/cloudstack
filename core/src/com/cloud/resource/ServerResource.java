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
package com.cloud.resource;

import com.cloud.agent.IAgentControl;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.PingCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.host.Host;
import com.cloud.utils.component.Manager;

/**
 *
 * ServerResource is a generic container to execute commands sent
 * to the agent.
 */
public interface ServerResource extends Manager {
    /**
     * @return Host.Type type of the computing server we have.
     */
    Host.Type getType();
    
    /**
     * Generate a startup command containing information regarding the resource.
     * @return StartupCommand ready to be sent to the management server.
     */
    public StartupCommand[] initialize();
        
    /**
     * @param id id of the server to put in the PingCommand
     * @return PingCommand
     */
    public PingCommand getCurrentStatus(long id);
    
    /**
     * Execute the request coming from the computing server.
     * @param cmd Command to execute.
     * @return Answer
     */
    public Answer executeRequest(Command cmd);
    
//    public void revertRequest(Command cmd);
    
    /**
     * disconnected() is called when the connection is down between the
     * agent and the management server.  If there are any cleanups, this
     * is the time to do it.
     */
    public void disconnected();

    /**
     * This is added to allow calling agent control service from within the resource
     * @return
     */
    public IAgentControl getAgentControl();
    
    public void setAgentControl(IAgentControl agentControl);
}
