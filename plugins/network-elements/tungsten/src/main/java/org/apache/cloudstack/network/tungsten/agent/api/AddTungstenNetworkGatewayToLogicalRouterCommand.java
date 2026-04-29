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
package org.apache.cloudstack.network.tungsten.agent.api;

import java.util.Objects;

public class AddTungstenNetworkGatewayToLogicalRouterCommand extends TungstenCommand {
    private final String networkUuid;
    private final String logicalRouterUuid;
    private final String ipAddress;

    public AddTungstenNetworkGatewayToLogicalRouterCommand(String networkUuid, String logicalRouterUuid, String ipAddress) {
        this.networkUuid = networkUuid;
        this.logicalRouterUuid = logicalRouterUuid;
        this.ipAddress = ipAddress;
    }

    public String getNetworkUuid() {
        return networkUuid;
    }

    public String getLogicalRouterUuid() {
        return logicalRouterUuid;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        AddTungstenNetworkGatewayToLogicalRouterCommand that = (AddTungstenNetworkGatewayToLogicalRouterCommand) o;
        return Objects.equals(networkUuid, that.networkUuid) && Objects.equals(logicalRouterUuid, that.logicalRouterUuid) && Objects.equals(ipAddress, that.ipAddress);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), networkUuid, logicalRouterUuid, ipAddress);
    }
}
