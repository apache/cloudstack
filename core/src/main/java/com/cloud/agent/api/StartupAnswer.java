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

import java.util.HashMap;
import java.util.Map;

public class StartupAnswer extends Answer {
    long hostId;
    String hostName;
    String hostUuid;
    int pingInterval;

    Integer agentHostStatusCheckDelaySec;
    private Map<String, String> params;

    protected StartupAnswer() {
        params = new HashMap<>();
    }

    public StartupAnswer(StartupCommand cmd, long hostId, String hostUuid, String hostName, int pingInterval) {
        super(cmd);
        this.hostId = hostId;
        this.hostUuid = hostUuid;
        this.hostName = hostName;
        this.pingInterval = pingInterval;
        params = new HashMap<>();
    }

    public StartupAnswer(StartupCommand cmd, String details) {
        super(cmd, false, details);
        params = new HashMap<>();
    }

    public long getHostId() {
        return hostId;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public String getHostName() {
        return hostName;
    }

    public int getPingInterval() {
        return pingInterval;
    }

    public Map<String, String> getParams() {
        return params;
    }

    public void setParams(Map<String, String> params) {
        this.params = params;
    }

    public Integer getAgentHostStatusCheckDelaySec() {
        return agentHostStatusCheckDelaySec;
    }

    public void setAgentHostStatusCheckDelaySec(Integer agentHostStatusCheckDelaySec) {
        this.agentHostStatusCheckDelaySec = agentHostStatusCheckDelaySec;
    }
}
