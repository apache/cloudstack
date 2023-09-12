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

public class CreateTungstenVirtualMachineCommand extends TungstenCommand {
    private final String projectFqn;
    private final String vnUuid;
    private final String vmUuid;
    private final String vmName;
    private final String nicUuid;
    private final long nicId;
    private final String ip;
    private final String ipv6;
    private final String mac;
    private final String vmType;
    private final String trafficType;
    private final String host;
    private final String gateway;
    private final boolean isDefaultNic;

    public CreateTungstenVirtualMachineCommand(final String projectFqn, final String vnUuid, final String vmUuid,
        final String vmName, final String nicUuid, final long nicId, final String ip, final String ipv6, final String mac,
        final String vmType, final String trafficType, final String host, final String gateway, final boolean isDefaultNic) {
        this.projectFqn = projectFqn;
        this.vnUuid = vnUuid;
        this.vmUuid = vmUuid;
        this.vmName = vmName;
        this.nicUuid = nicUuid;
        this.nicId = nicId;
        this.ip = ip;
        this.ipv6 = ipv6;
        this.mac = mac;
        this.vmType = vmType;
        this.trafficType = trafficType;
        this.host = host;
        this.gateway = gateway;
        this.isDefaultNic = isDefaultNic;
    }

    public String getProjectFqn() {
        return projectFqn;
    }

    public String getVnUuid() {
        return vnUuid;
    }

    public String getVmUuid() {
        return vmUuid;
    }

    public String getVmName() {
        return vmName;
    }

    public String getNicUuid() {
        return nicUuid;
    }

    public long getNicId() {
        return nicId;
    }

    public String getIp() {
        return ip;
    }

    public String getIpv6() {
        return ipv6;
    }

    public String getMac() {
        return mac;
    }

    public String getVmType() {
        return vmType;
    }

    public String getTrafficType() {
        return trafficType;
    }

    public String getHost() {
        return host;
    }

    public String getGateway() {
        return gateway;
    }

    public boolean isDefaultNic() {
        return isDefaultNic;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        CreateTungstenVirtualMachineCommand that = (CreateTungstenVirtualMachineCommand) o;
        return nicId == that.nicId && isDefaultNic == that.isDefaultNic && Objects.equals(projectFqn, that.projectFqn) && Objects.equals(vnUuid, that.vnUuid) && Objects.equals(vmUuid, that.vmUuid) && Objects.equals(vmName, that.vmName) && Objects.equals(nicUuid, that.nicUuid) && Objects.equals(ip, that.ip) && Objects.equals(ipv6, that.ipv6) && Objects.equals(mac, that.mac) && Objects.equals(vmType, that.vmType) && Objects.equals(trafficType, that.trafficType) && Objects.equals(host, that.host) && Objects.equals(gateway, that.gateway);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), projectFqn, vnUuid, vmUuid, vmName, nicUuid, nicId, ip, ipv6, mac, vmType, trafficType, host, gateway, isDefaultNic);
    }
}
