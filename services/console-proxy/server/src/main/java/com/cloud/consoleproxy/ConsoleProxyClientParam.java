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

/**
 *
 * Data object to store parameter info needed by client to connect to its host
 */
public class ConsoleProxyClientParam {

    private String clientHostAddress;
    private int clientHostPort;
    private String clientHostPassword;
    private String clientTag;
    private String ticket;

    private String clientTunnelUrl;
    private String clientTunnelSession;
    private String locale;
    private String ajaxSessionId;

    private String hypervHost;
    private String username;
    private String password;
    private String websocketUrl;

    private String sourceIP;

    public ConsoleProxyClientParam() {
        clientHostPort = 0;
    }

    public String getClientHostAddress() {
        return clientHostAddress;
    }

    public void setClientHostAddress(String clientHostAddress) {
        this.clientHostAddress = clientHostAddress;
    }

    public int getClientHostPort() {
        return clientHostPort;
    }

    public void setClientHostPort(int clientHostPort) {
        this.clientHostPort = clientHostPort;
    }

    public String getClientHostPassword() {
        return clientHostPassword;
    }

    public void setClientHostPassword(String clientHostPassword) {
        this.clientHostPassword = clientHostPassword;
    }

    public String getClientTag() {
        return clientTag;
    }

    public void setClientTag(String clientTag) {
        this.clientTag = clientTag;
    }

    public String getTicket() {
        return ticket;
    }

    public void setTicket(String ticket) {
        this.ticket = ticket;
    }

    public String getClientTunnelUrl() {
        return clientTunnelUrl;
    }

    public void setClientTunnelUrl(String clientTunnelUrl) {
        this.clientTunnelUrl = clientTunnelUrl;
    }

    public String getClientTunnelSession() {
        return clientTunnelSession;
    }

    public void setClientTunnelSession(String clientTunnelSession) {
        this.clientTunnelSession = clientTunnelSession;
    }

    public String getAjaxSessionId() {
        return ajaxSessionId;
    }

    public void setAjaxSessionId(String ajaxSessionId) {
        this.ajaxSessionId = ajaxSessionId;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public String getClientMapKey() {
        if (clientTag != null && !clientTag.isEmpty())
            return clientTag;

        return clientHostAddress + ":" + clientHostPort;
    }

    public void setHypervHost(String hypervHost) {
        this.hypervHost = hypervHost;
    }

    public String getHypervHost() {
        return hypervHost;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPassword() {
        return password;
    }

    public String getSourceIP() {
        return sourceIP;
    }

    public void setSourceIP(String sourceIP) {
        this.sourceIP = sourceIP;
    }

    public String getWebsocketUrl() {
        return websocketUrl;
    }

    public void setWebsocketUrl(String websocketUrl) {
        this.websocketUrl = websocketUrl;
    }
}
