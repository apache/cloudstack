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

import com.google.gson.annotations.SerializedName;

import org.apache.cloudstack.api.BaseResponse;

import com.cloud.serializer.Param;

/*
 * This is the generic response for all types of System VMs (SSVM, consoleproxy, domain routers(router, LB, DHCP))
 */
public class SystemVmInstanceResponse extends BaseResponse {
    @SerializedName("id")
    @Param(description = "the ID of the system VM")
    private String id;

    @SerializedName("systemvmtype")
    @Param(description = "the system VM type")
    private String systemVmType;

    @SerializedName("name")
    @Param(description = "the name of the system VM")
    private String name;

    @SerializedName("hostid")
    @Param(description = "the host ID for the system VM")
    private String hostId;

    @SerializedName("state")
    @Param(description = "the state of the system VM")
    private String state;

    @SerializedName("role")
    @Param(description = "the role of the system VM")
    private String role;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSystemVmType() {
        return systemVmType;
    }

    public void setSystemVmType(String systemVmType) {
        this.systemVmType = systemVmType;
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

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

}
