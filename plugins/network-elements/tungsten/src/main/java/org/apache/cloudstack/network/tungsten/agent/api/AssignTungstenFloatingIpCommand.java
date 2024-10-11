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

public class AssignTungstenFloatingIpCommand extends TungstenCommand {
    private final String networkUuid;
    private final String vmiUuid;
    private final String fipName;
    private final String name;
    private final String privateIp;

    public AssignTungstenFloatingIpCommand(final String networkUuid, final String vmiUuid, final String fipName,
        final String name, final String privateIp) {
        this.networkUuid = networkUuid;
        this.vmiUuid = vmiUuid;
        this.fipName = fipName;
        this.name = name;
        this.privateIp = privateIp;
    }

    public String getNetworkUuid() {
        return networkUuid;
    }

    public String getVmiUuid() {
        return vmiUuid;
    }

    public String getFipName() {
        return fipName;
    }

    public String getName() {
        return name;
    }

    public String getPrivateIp() {
        return privateIp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        AssignTungstenFloatingIpCommand that = (AssignTungstenFloatingIpCommand) o;
        return Objects.equals(networkUuid, that.networkUuid) && Objects.equals(vmiUuid, that.vmiUuid) && Objects.equals(fipName, that.fipName) && Objects.equals(name, that.name) && Objects.equals(privateIp, that.privateIp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), networkUuid, vmiUuid, fipName, name, privateIp);
    }
}
