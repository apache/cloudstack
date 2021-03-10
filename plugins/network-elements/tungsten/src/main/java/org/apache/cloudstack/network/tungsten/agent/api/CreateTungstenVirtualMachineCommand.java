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

public class CreateTungstenVirtualMachineCommand extends TungstenCommand {
    private final String projectFqn;
    private final String vnUuid;
    private final String vmUuid;
    private final String vmName;
    private final String nicUuid;
    private final long nicId;
    private final String ip;
    private final String mac;
    private final String vmType;
    private final String trafficType;
    private final String host;

    public CreateTungstenVirtualMachineCommand(final String projectFqn, final String vnUuid, final String vmUuid,
        final String vmName, final String nicUuid, final long nicId, final String ip, final String mac,
        final String vmType, final String trafficType, final String host) {
        this.projectFqn = projectFqn;
        this.vnUuid = vnUuid;
        this.vmUuid = vmUuid;
        this.vmName = vmName;
        this.nicUuid = nicUuid;
        this.nicId = nicId;
        this.ip = ip;
        this.mac = mac;
        this.vmType = vmType;
        this.trafficType = trafficType;
        this.host = host;
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
}