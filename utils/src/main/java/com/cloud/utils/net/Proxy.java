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

package com.cloud.utils.net;

import java.net.URI;

/**
 * Download Proxy
 */
public class Proxy {
    private String _host;
    private int _port;
    private String _userName;
    private String _password;

    public Proxy(String host, int port, String userName, String password) {
        this._host = host;
        this._port = port;
        this._userName = userName;
        this._password = password;
    }

    public Proxy(URI uri) {
        this._host = uri.getHost();
        this._port = uri.getPort() == -1 ? 3128 : uri.getPort();
        String userInfo = uri.getUserInfo();
        if (userInfo != null) {
            String[] tokens = userInfo.split(":");
            if (tokens.length == 1) {
                this._userName = userInfo;
                this._password = "";
            } else if (tokens.length == 2) {
                this._userName = tokens[0];
                this._password = tokens[1];
            }
        }
    }

    public String getHost() {
        return _host;
    }

    public int getPort() {
        return _port;
    }

    public String getUserName() {
        return _userName;
    }

    public String getPassword() {
        return _password;
    }
}
