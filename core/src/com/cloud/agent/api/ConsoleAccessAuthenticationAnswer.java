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

public class ConsoleAccessAuthenticationAnswer extends AgentControlAnswer {

    private Boolean _success;

    private Boolean _isReauthenticating;
    private String _host;
    private Integer _port;

    private String _tunnelUrl;
    private String _tunnelSession;

    public ConsoleAccessAuthenticationAnswer() {
        _success = false;
        _isReauthenticating = false;
        _port = 0;
    }

    public ConsoleAccessAuthenticationAnswer(Command cmd, Boolean success) {
        super(cmd);
        _success = success;
    }

    public Boolean succeeded() {
        return _success;
    }

    public void setSuccess(Boolean value) {
        _success = value;
    }

    public Boolean isReauthenticating() {
        return _isReauthenticating;
    }

    public void setReauthenticating(Boolean value) {
        _isReauthenticating = value;
    }

    public String getHost() {
        return _host;
    }

    public void setHost(String host) {
        _host = host;
    }

    public Integer getPort() {
        return _port;
    }

    public void setPort(Integer port) {
        _port = port;
    }

    public String getTunnelUrl() {
        return _tunnelUrl;
    }

    public void setTunnelUrl(String tunnelUrl) {
        _tunnelUrl = tunnelUrl;
    }

    public String getTunnelSession() {
        return _tunnelSession;
    }

    public void setTunnelSession(String tunnelSession) {
        _tunnelSession = tunnelSession;
    }
}
