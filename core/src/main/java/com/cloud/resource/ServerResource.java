//
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
//

package com.cloud.resource;

import com.cloud.agent.IAgentControl;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.PingCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.host.Host;
import com.cloud.utils.component.Manager;

/**
 * ServerResource is a generic container to execute commands sent
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
    StartupCommand[] initialize();

    /**
     * @param id id of the server to put in the PingCommand
     * @return PingCommand
     */
    PingCommand getCurrentStatus(long id);

    /**
     * Execute the request coming from the computing server.
     * @param cmd Command to execute.
     * @return Answer
     */
    Answer executeRequest(Command cmd);

    /**
     * disconnected() is called when the connection is down between the
     * agent and the management server.  If there are any cleanups, this
     * is the time to do it.
     */
    void disconnected();

    /**
     * This is added to allow calling agent control service from within the resource
     * @return
     */
    IAgentControl getAgentControl();

    void setAgentControl(IAgentControl agentControl);

}
