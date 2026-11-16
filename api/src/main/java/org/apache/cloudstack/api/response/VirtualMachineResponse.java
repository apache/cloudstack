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
package org.apache.cloudstack.api.response;

import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;

import com.cloud.serializer.Param;
import com.cloud.vm.VirtualMachine;
import com.google.gson.annotations.SerializedName;

@EntityReference(value = VirtualMachine.class)
public class VirtualMachineResponse extends BaseResponse {
    @SerializedName("id")
    @Param(description = "the ID of the VM")
    private String id;

    @SerializedName("type")
    @Param(description = "the type of VM")
    private String type;

    @SerializedName("name")
    @Param(description = "the name of the VM")
    private String name;

    @SerializedName("clusterid")
    @Param(description = "the cluster ID for the VM")
    private String clusterId;

    @SerializedName("clustername")
    @Param(description = "the cluster name for the VM")
    private String clusterName;

    @SerializedName("hostid")
    @Param(description = "the host ID for the VM")
    private String hostId;

    @SerializedName("hostname")
    @Param(description = "the hostname for the VM")
    private String hostName;

    @Override
    public String getObjectId() {
        return this.getId();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getVmType() {
        return type;
    }

    public void setVmType(String type) {
        this.type = type;
    }

    public String getVmName() {
        return name;
    }

    public void setVmName(String name) {
        this.name = name;
    }

    public String getClusterId() {
        return clusterId;
    }

    public void setClusterId(String clusterId) {
        this.clusterId = clusterId;
    }

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHostId() {
        return hostId;
    }

    public void setHostId(String hostId) {
        this.hostId = hostId;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }
}
