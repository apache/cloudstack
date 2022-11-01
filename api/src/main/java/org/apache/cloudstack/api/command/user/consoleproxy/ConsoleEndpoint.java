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
package org.apache.cloudstack.api.command.user.consoleproxy;

public class ConsoleEndpoint {

    private boolean result;
    private String details;
    private String url;
    private String websocketToken;
    private String websocketPath;
    private String websocketHost;
    private String websocketPort;
    private String websocketExtra;

    public ConsoleEndpoint(boolean result, String url) {
        this.result = result;
        this.url = url;
    }

    public ConsoleEndpoint(boolean result, String url, String details) {
        this(result, url);
        this.details = details;
    }

    public boolean isResult() {
        return result;
    }

    public void setResult(boolean result) {
        this.result = result;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getWebsocketToken() {
        return websocketToken;
    }

    public void setWebsocketToken(String websocketToken) {
        this.websocketToken = websocketToken;
    }

    public String getWebsocketPath() {
        return websocketPath;
    }

    public void setWebsocketPath(String websocketPath) {
        this.websocketPath = websocketPath;
    }

    public String getWebsocketHost() {
        return websocketHost;
    }

    public void setWebsocketHost(String websocketHost) {
        this.websocketHost = websocketHost;
    }

    public String getWebsocketPort() {
        return websocketPort;
    }

    public void setWebsocketPort(String websocketPort) {
        this.websocketPort = websocketPort;
    }

    public String getWebsocketExtra() {
        return websocketExtra;
    }

    public void setWebsocketExtra(String websocketExtra) {
        this.websocketExtra = websocketExtra;
    }
}
