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


public class OvsCreateTunnelAnswer extends Answer {
    Long from;
    Long to;
    long networkId;
    String inPortName;

    // for debug info
    String fromIp;
    String toIp;
    int key;
    String bridge;

    public OvsCreateTunnelAnswer(Command cmd, boolean success, String details,
            String bridge) {
        super(cmd, success, details);
        OvsCreateTunnelCommand c = (OvsCreateTunnelCommand)cmd;
        from = c.getFrom();
        to = c.getTo();
        networkId = c.getNetworkId();
        inPortName = "[]";
        fromIp = c.getFromIp();
        toIp = c.getRemoteIp();
        key = c.getKey();
        this.bridge = bridge;
    }

    public OvsCreateTunnelAnswer(Command cmd, boolean success, String details,
            String inPortName, String bridge) {
        this(cmd, success, details, bridge);
        this.inPortName = inPortName;
    }

    public Long getFrom() {
        return from;
    }

    public Long getTo() {
        return to;
    }

    public long getNetworkId() {
        return networkId;
    }

    public String getInPortName() {
        return inPortName;
    }

    public String getFromIp() {
        return fromIp;
    }

    public String getToIp() {
        return toIp;
    }

    public int getKey() {
        return key;
    }

    public String getBridge() {
        return bridge;
    }
}
