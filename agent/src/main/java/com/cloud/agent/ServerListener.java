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

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;

/**
 * There are several types of events that the Agent forwards.
 * <p>
 * 1. Commands sent by the Agent.
 * 2. Answers sent by the Management Server.
 */
public interface ServerListener {

    /**
     * @param seq     sequence number return by the send() method.
     * @param answers answers to the commands.
     * @return true if processed. false if not.
     */
    boolean processAnswers(long seq, Answer[] answers);

    /**
     * This method is called by the ServerHandler when Management Server sent
     * a command to the server.
     * In order to process these commands, the Server Attache Listener must be registered for commands.
     *
     * @param seq      sequence number of the command sent.
     * @param commands commands that were sent.
     * @return true if you processed the commands.  false if not.
     */
    boolean processCommands(long seq, Command[] commands);

    /**
     * This method is called by ServerHandler when an agent disconnects
     * from the Management Server if the listener has been registered for host events.
     * <p>
     * If the Listener is passed to the send() method, this method is
     * also called by ServerHandler if the agent disconnected.
     */
    boolean processDisconnect();

    /**
     * If this Listener is passed to the send() method, this method
     * is called by ServerHandler after processing an answer
     * from the agent.
     * Returning true means you're expecting more
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
    default int getTimeout() {
        return -1;
    }

    /**
     * If the Listener is passed to the send() method, this method is
     * called by the ServerHandler to process a command timeout.
     *
     * @param seq sequence number returned by send().
     * @return true if processed; false if not.
     */
    default boolean processTimeout(long seq) {
        return false;
    }
}
