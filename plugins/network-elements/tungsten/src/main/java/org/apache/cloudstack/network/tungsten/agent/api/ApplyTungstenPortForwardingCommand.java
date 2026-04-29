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

public class ApplyTungstenPortForwardingCommand extends TungstenCommand {
    private final boolean isAdd;
    private final String publicNetworkUuid;
    private final String floatingIpPoolName;
    private final String floatingIpName;
    private final String vmiUuid;
    private final String protocol;
    private final int publicPort;
    private final int privatePort;

    public ApplyTungstenPortForwardingCommand(final boolean isAdd, final String publicNetworkUuid,
        final String floatingIpPoolName, final String floatingIpName, final String vmiUuid, final String protocol,
        final int publicPort, final int privatePort) {
        this.isAdd = isAdd;
        this.publicNetworkUuid = publicNetworkUuid;
        this.floatingIpPoolName = floatingIpPoolName;
        this.floatingIpName = floatingIpName;
        this.vmiUuid = vmiUuid;
        this.protocol = protocol;
        this.publicPort = publicPort;
        this.privatePort = privatePort;
    }

    public boolean isAdd() {
        return isAdd;
    }

    public String getPublicNetworkUuid() {
        return publicNetworkUuid;
    }

    public String getFloatingIpPoolName() {
        return floatingIpPoolName;
    }

    public String getFloatingIpName() {
        return floatingIpName;
    }

    public String getVmiUuid() {
        return vmiUuid;
    }

    public String getProtocol() {
        return protocol;
    }

    public int getPublicPort() {
        return publicPort;
    }

    public int getPrivatePort() {
        return privatePort;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ApplyTungstenPortForwardingCommand that = (ApplyTungstenPortForwardingCommand) o;
        return isAdd == that.isAdd && publicPort == that.publicPort && privatePort == that.privatePort && Objects.equals(publicNetworkUuid, that.publicNetworkUuid) && Objects.equals(floatingIpPoolName, that.floatingIpPoolName) && Objects.equals(floatingIpName, that.floatingIpName) && Objects.equals(vmiUuid, that.vmiUuid) && Objects.equals(protocol, that.protocol);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), isAdd, publicNetworkUuid, floatingIpPoolName, floatingIpName, vmiUuid, protocol, publicPort, privatePort);
    }
}
