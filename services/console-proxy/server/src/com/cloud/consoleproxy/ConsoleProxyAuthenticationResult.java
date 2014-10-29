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

// duplicated class
public class ConsoleProxyAuthenticationResult {
    private boolean success;
    private boolean isReauthentication;
    private String host;
    private int port;
    private String tunnelUrl;
    private String tunnelSession;

    public ConsoleProxyAuthenticationResult() {
        success = false;
        isReauthentication = false;
        port = 0;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public boolean isReauthentication() {
        return isReauthentication;
    }

    public void setReauthentication(boolean isReauthentication) {
        this.isReauthentication = isReauthentication;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getTunnelUrl() {
        return tunnelUrl;
    }

    public void setTunnelUrl(String tunnelUrl) {
        this.tunnelUrl = tunnelUrl;
    }

    public String getTunnelSession() {
        return tunnelSession;
    }

    public void setTunnelSession(String tunnelSession) {
        this.tunnelSession = tunnelSession;
    }
}
