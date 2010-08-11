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

import com.cloud.agent.api.AgentControlAnswer;
import com.cloud.agent.api.AgentControlCommand;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.StartupCommand;
import com.cloud.host.HostVO;
import com.cloud.host.Status;

/**
 * Listener is a multipurpose interface for hooking into the AgentManager.
 * There are several types of events that the AgentManager forwards
 * to the listener.
 * 
 *   1. Agent Connect & Disconnect
 *   2. Commands sent by the agent.
 *   3. Answers sent by the agent.
 */
public interface Listener {

	/**
	 * If the Listener is passed in the send(), this method will
	 * be called to process the answers.
	 * 
	 * @param agentId id of the agent
	 * @param seq sequence number return by the send() method.
	 * @param answers answers to the commands.
	 * @return true if processed.  false if not.
	 */
    boolean processAnswer(long agentId, long seq, Answer[] answers);

    /**
     * This method is called by the AgentManager when an agent sent
     * a command to the server.  In order to process these commands,
     * the Listener must be registered for host commands.
     * 
     * @param agentId id of the agent.
     * @param seq sequence number of the command sent.
     * @param commands commands that were sent.
     * @return true if you processed the commands.  false if not.
     */
    boolean processCommand(long agentId, long seq, Command[] commands);
    
    /**
     * process control command sent from agent under its management
     * @param agentId
     * @param cmd
     * @return
     */
    AgentControlAnswer processControlCommand(long agentId, AgentControlCommand cmd);

    /**
     * This method is called by AgentManager when an agent made a
     * connection to this server if the listener has
     * been registered for host events.
     * @param agentId id of the agent
     * @param cmd command sent by the agent to the server on startup.
     */
    boolean processConnect(HostVO host, StartupCommand cmd);
    
    /**
     * This method is called by AgentManager when an agent disconnects
     * from this server if the listener has been registered for host events.
     * 
     * If the Listener is passed to the send() method, this method is
     * also called by AgentManager if the agent disconnected.
     * 
     * @param agentId id of the agent
     * @param state the current state of the agent.
     */
    boolean processDisconnect(long agentId, Status state);
    
    /**
     * If ths Listener is passed to the send() method, this method
     * is called by AgentManager after processing an answer
     * from the agent.  Returning true means you're expecting more
     * answers from the agent using the same sequence number.
     * 
     * @return true if expecting more answers using the same sequence number.
     */
    boolean isRecurring();

    /**
     * If the Listener is passed to the send() method, this method is
     * called to determine how long to wait for the reply.  The value
     * is in seconds.  -1 indicates to wait forever.  0 indicates to
     * use the default timeout.  If the timeout is
     * reached, processTimeout on this same Listener is called.
     * 
     * @return timeout in seconds before processTimeout is called.
     */
    int getTimeout();

    /**
     * If the Listener is passed to the send() method, this method is
     * called by the AgentManager to process a command timeout.
     * @param agentId id of the agent
     * @param seq sequence number returned by the send().
     * @return true if processed; false if not.
     */
    boolean processTimeout(long agentId, long seq);
}
