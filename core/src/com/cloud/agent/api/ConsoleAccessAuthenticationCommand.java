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

package com.cloud.agent.api;

public class ConsoleAccessAuthenticationCommand extends AgentControlCommand {

    private String _host;
    private String _port;
    private String _vmId;
    private String _sid;
    private String _ticket;

    private boolean _isReauthenticating;

    public ConsoleAccessAuthenticationCommand() {
        _isReauthenticating = false;
    }

    public ConsoleAccessAuthenticationCommand(String host, String port, String vmId, String sid, String ticket) {
        _host = host;
        _port = port;
        _vmId = vmId;
        _sid = sid;
        _ticket = ticket;
    }

    public String getHost() {
        return _host;
    }

    public String getPort() {
        return _port;
    }

    public String getVmId() {
        return _vmId;
    }

    public String getSid() {
        return _sid;
    }

    public String getTicket() {
        return _ticket;
    }

    public boolean isReauthenticating() {
        return _isReauthenticating;
    }

    public void setReauthenticating(boolean value) {
        _isReauthenticating = value;
    }
}
