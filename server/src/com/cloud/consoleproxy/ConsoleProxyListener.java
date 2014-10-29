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
package com.cloud.consoleproxy;

import com.cloud.agent.Listener;
import com.cloud.agent.api.AgentControlAnswer;
import com.cloud.agent.api.AgentControlCommand;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.ConsoleAccessAuthenticationCommand;
import com.cloud.agent.api.ConsoleProxyLoadReportCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StartupProxyCommand;
import com.cloud.host.Host;
import com.cloud.host.Status;

public class ConsoleProxyListener implements Listener {
    AgentHook _proxyMgr = null;

    public ConsoleProxyListener(AgentHook proxyMgr) {
        _proxyMgr = proxyMgr;
    }

    @Override
    public boolean isRecurring() {
        return true;
    }

    @Override
    public boolean processAnswers(long agentId, long seq, Answer[] answers) {
        return true;
    }

    @Override
    public boolean processCommands(long agentId, long seq, Command[] commands) {
        return false;
    }

    @Override
    public AgentControlAnswer processControlCommand(long agentId, AgentControlCommand cmd) {
        if (cmd instanceof ConsoleProxyLoadReportCommand) {
            _proxyMgr.onLoadReport((ConsoleProxyLoadReportCommand)cmd);

            // return dummy answer
            return new AgentControlAnswer(cmd);
        } else if (cmd instanceof ConsoleAccessAuthenticationCommand) {
            return _proxyMgr.onConsoleAccessAuthentication((ConsoleAccessAuthenticationCommand)cmd);
        }
        return null;
    }

    @Override
    public void processConnect(Host host, StartupCommand cmd, boolean forRebalance) {
        _proxyMgr.onAgentConnect(host, cmd);

        if (cmd instanceof StartupProxyCommand) {
            _proxyMgr.startAgentHttpHandlerInVM((StartupProxyCommand)cmd);
        }
    }

    @Override
    public boolean processDisconnect(long agentId, Status state) {
        _proxyMgr.onAgentDisconnect(agentId, state);
        return true;
    }

    @Override
    public boolean processTimeout(long agentId, long seq) {
        return true;
    }

    @Override
    public int getTimeout() {
        return -1;
    }

}
