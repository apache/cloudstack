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

public class SetTungstenNetworkGatewayCommand extends TungstenCommand {
    private final String projectUuid;
    private final String routerName;
    private final long vnId;
    private final String vnUuid;
    private final String vnGatewayIp;

    public SetTungstenNetworkGatewayCommand(String projectUuid, final String routerName, final long vnId,
        final String vnUuid, final String vnGatewayIp) {
        this.projectUuid = projectUuid;
        this.routerName = routerName;
        this.vnId = vnId;
        this.vnUuid = vnUuid;
        this.vnGatewayIp = vnGatewayIp;
    }

    public String getRouterName() {
        return routerName;
    }

    public long getVnId() {
        return vnId;
    }

    public String getVnUuid() {
        return vnUuid;
    }

    public String getVnGatewayIp() {
        return vnGatewayIp;
    }

    public String getProjectUuid() {
        return projectUuid;
    }
}
