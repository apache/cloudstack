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
package com.cloud.agent.api;


public class OvsCreateGreTunnelAnswer extends Answer {
    String hostIp;
    String remoteIp;
    String bridge;
    String key;
    long from;
    long to;
    int port;

    public OvsCreateGreTunnelAnswer(Command cmd, boolean success, String details) {
        super(cmd, success, details);
    }

    public OvsCreateGreTunnelAnswer(Command cmd, boolean success,
            String details, String hostIp, String bridge) {
        super(cmd, success, details);
        OvsCreateGreTunnelCommand c = (OvsCreateGreTunnelCommand)cmd;
        this.hostIp = hostIp;
        this.bridge = bridge;
        this.remoteIp = c.getRemoteIp();
        this.key = c.getKey();
        this.port = -1;
        this.from = c.getFrom();
        this.to = c.getTo();
    }

    public OvsCreateGreTunnelAnswer(Command cmd, boolean success,
            String details, String hostIp, String bridge, int port) {
        this(cmd, success, details, hostIp, bridge);
        this.port = port;
    }

    public String getHostIp() {
        return hostIp;
    }

    public String getRemoteIp() {
        return remoteIp;
    }

    public String getBridge() {
        return bridge;
    }

    public String getKey() {
        return key;
    }

    public long getFrom() {
        return from;
    }

    public long getTo() {
        return to;
    }

    public int getPort() {
        return port;
    }
}
