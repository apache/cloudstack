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
package com.cloud.agent;

import com.cloud.agent.api.AgentControlAnswer;
import com.cloud.agent.api.AgentControlCommand;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.StartupCommand;
import com.cloud.exception.ConnectionException;
import com.cloud.host.Host;
import com.cloud.host.Status;

/**
 * There are several types of events that the AgentManager forwards
 *
 *   1. Agent Connect & Disconnect
 *   2. Commands sent by the agent.
 *   3. Answers sent by the agent.
 */
public interface Listener {

    /**
     *
     * @param agentId id of the agent
     * @param seq sequence number return by the send() method.
     * @param answers answers to the commands.
     * @return true if processed.  false if not.
     */
    boolean processAnswers(long agentId, long seq, Answer[] answers);

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
    boolean processCommands(long agentId, long seq, Command[] commands);

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
     * @param cmd command sent by the agent to the server on startup.
     * @param forRebalance TODO
     * @param agentId id of the agent
     * @throws ConnectionException if host has problems and needs to put into maintenance state.
     */
    void processConnect(Host host, StartupCommand cmd, boolean forRebalance) throws ConnectionException;

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
     * If this Listener is passed to the send() method, this method
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
