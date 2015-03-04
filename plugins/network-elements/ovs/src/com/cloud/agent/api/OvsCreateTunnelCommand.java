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


public class OvsCreateTunnelCommand extends Command {
    Integer key;
    String remoteIp;
    String networkName;
    Long from;
    Long to;
    long networkId;

    String networkUuid;

    // for debug info
    String fromIp;

    @Override
    public boolean executeInSequence() {
        return true;
    }

    public OvsCreateTunnelCommand(String remoteIp, Integer key, Long from,
            Long to, long networkId, String fromIp, String networkName, String networkUuid) {
        this.remoteIp = remoteIp;
        this.key = key;
        this.from = from;
        this.to = to;
        this.networkId = networkId;
        this.fromIp = fromIp;
        this.networkName = networkName;
        this.networkUuid = networkUuid;
    }

    public Integer getKey() {
        return key;
    }

    public String getRemoteIp() {
        return remoteIp;
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

    public String getFromIp() {
        return fromIp;
    }

    public String getNetworkName() {
        return networkName;
    }


    public String getNetworkUuid() {
        return networkUuid;
    }

    public void setNetworkUuid(String networkUuid) {
        this.networkUuid = networkUuid;
    }
}
