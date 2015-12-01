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

public class ConfigureSharedNetworkVlanIdCommand extends Command {

    private String logicalSwitchUuid;
    private String l2GatewayServiceUuid;
    private long vlanId;
    private String ownerName;
    private long networkId;

    public ConfigureSharedNetworkVlanIdCommand(final String logicalSwitchUuid, final String l2GatewayServiceUuid,
            final long vlanId, final String ownerName, final long networkId) {
        this.logicalSwitchUuid = logicalSwitchUuid;
        this.l2GatewayServiceUuid = l2GatewayServiceUuid;
        this.vlanId = vlanId;
        this.ownerName = ownerName;
        this.networkId = networkId;
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }

    public String getLogicalSwitchUuid() {
        return logicalSwitchUuid;
    }

    public void setLogicalSwitchUuid(String logicalSwitchUuid) {
        this.logicalSwitchUuid = logicalSwitchUuid;
    }

    public String getL2GatewayServiceUuid() {
        return l2GatewayServiceUuid;
    }

    public void setL2GatewayServiceUuid(String l2GatewayServiceUuid) {
        this.l2GatewayServiceUuid = l2GatewayServiceUuid;
    }

    public long getVlanId() {
        return vlanId;
    }

    public void setVlanId(long vlanId) {
        this.vlanId = vlanId;
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
