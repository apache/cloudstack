//Licensed to the Apache Software Foundation (ASF) under one
//or more contributor license agreements.  See the NOTICE file
//distributed with this work for additional information
//regarding copyright ownership.  The ASF licenses this file
//to you under the Apache License, Version 2.0 (the
//"License"); you may not use this file except in compliance
//with the License.  You may obtain a copy of the License at
//
//http://www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing,
//software distributed under the License is distributed on an
//"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
//KIND, either express or implied.  See the License for the
//specific language governing permissions and limitations
//under the License.

package org.apache.cloudstack.api.response;

import com.google.gson.annotations.SerializedName;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;

import com.cloud.serializer.Param;

public class AcquirePodIpCmdResponse extends BaseResponse {

    @SerializedName(ApiConstants.IP_ADDRESS)
    @Param(description = "Allocated IP address")
    private String ipAddress;

    @SerializedName(ApiConstants.POD_ID)
    @Param(description = "the ID of the pod the  IP address belongs to")
    private Long podId;

    @SerializedName(ApiConstants.GATEWAY)
    @Param(description = "Gateway for Pod ")
    private String gateway;

    @SerializedName(ApiConstants.CIDR)
    @Param(description = "CIDR of the Pod")
    private String cidrAddress;

    @SerializedName(ApiConstants.NIC_ID)
    @Param(description = "the ID of the nic")
    private Long instanceId;

    @SerializedName(ApiConstants.HOST_MAC)
    @Param(description = "MAC address of the pod the  IP")
    private Long macAddress;

    @SerializedName(ApiConstants.ID)
    @Param(description = "the ID of the pod the  IP address")
    private long id;

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public void setInstanceId(Long instanceId) {
        this.instanceId = instanceId;
    }

    public void setPodId(long podId) {
        this.podId = podId;
    }

    public void setMacAddress(long macAddress) {
        this.macAddress = macAddress;
    }

    public void setGateway(String gateway) {
        this.gateway = gateway;
    }

    public void setCidrAddress(String cidrAddress) {
        this.cidrAddress = cidrAddress;
    }

    public long getId() {
        return id;
    }

    public Long getInstanceId() {
        return instanceId;
    }

    public long getPodId() {
        return podId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public long getMacAddress() {
        return macAddress;
    }

    public String getCidrAddress() {
        return cidrAddress;
    }

    public String getGateway() {
        return gateway;
    }

    public void setId(long id) {
        this.id = id;
    }

}
