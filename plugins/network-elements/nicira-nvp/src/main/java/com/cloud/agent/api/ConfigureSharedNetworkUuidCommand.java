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

public class ConfigureSharedNetworkUuidCommand extends Command {

    private String logicalRouterUuid;
    private String logicalSwitchUuid;
    private String portIpAddress;
    private String ownerName;
    private long networkId;

    public ConfigureSharedNetworkUuidCommand(final String logicalRouterUuid, final String logicalSwitchUuid,
            final String portIpAddress, final String ownerName, final long networkId) {
        super();
        this.logicalRouterUuid = logicalRouterUuid;
        this.logicalSwitchUuid = logicalSwitchUuid;
        this.portIpAddress = portIpAddress;
        this.ownerName = ownerName;
        this.networkId = networkId;
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }

    public String getLogicalRouterUuid() {
        return logicalRouterUuid;
    }

    public void setLogicalRouterUuid(String logicalRouterUuid) {
        this.logicalRouterUuid = logicalRouterUuid;
    }

    public String getLogicalSwitchUuid() {
        return logicalSwitchUuid;
    }

    public void setLogicalSwitchUuid(String logicalSwitchUuid) {
        this.logicalSwitchUuid = logicalSwitchUuid;
    }

    public String getPortIpAddress() {
        return portIpAddress;
    }

    public void setPortIpAddress(String portIpAddress) {
        this.portIpAddress = portIpAddress;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public long getNetworkId() {
        return networkId;
    }

    public void setNetworkId(long networkId) {
        this.networkId = networkId;
    }

}
