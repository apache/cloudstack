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

public class SetTungstenNetworkGatewayCommand extends TungstenCommand {
    private final String projectFqn;
    private final String routerName;
    private final long vnId;
    private final String vnUuid;
    private final String vnGatewayIp;

    public SetTungstenNetworkGatewayCommand(final String projectFqn, final String routerName, final long vnId,
        final String vnUuid, final String vnGatewayIp) {
        this.projectFqn = projectFqn;
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

    public String getProjectFqn() {
        return projectFqn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        SetTungstenNetworkGatewayCommand that = (SetTungstenNetworkGatewayCommand) o;
        return vnId == that.vnId && Objects.equals(projectFqn, that.projectFqn) && Objects.equals(routerName, that.routerName) && Objects.equals(vnUuid, that.vnUuid) && Objects.equals(vnGatewayIp, that.vnGatewayIp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), projectFqn, routerName, vnId, vnUuid, vnGatewayIp);
    }
}
