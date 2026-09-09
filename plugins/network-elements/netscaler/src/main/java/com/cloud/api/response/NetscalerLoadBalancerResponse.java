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
package com.cloud.api.response;

import java.util.List;

import com.google.gson.annotations.SerializedName;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;

import com.cloud.network.dao.ExternalLoadBalancerDeviceVO;
import com.cloud.serializer.Param;

@EntityReference(value = ExternalLoadBalancerDeviceVO.class)
@SuppressWarnings("unused")
public class NetscalerLoadBalancerResponse extends BaseResponse {

    @SerializedName(ApiConstants.LOAD_BALANCER_DEVICE_ID)
    @Param(description = "Device ID of the netscaler load balancer")
    private String id;

    @SerializedName(ApiConstants.PHYSICAL_NETWORK_ID)
    @Param(description = "The physical Network to which this netscaler device belongs to")
    private String physicalNetworkId;

    @SerializedName(ApiConstants.PROVIDER)
    @Param(description = "Name of the provider")
    private String providerName;

    @SerializedName(ApiConstants.LOAD_BALANCER_DEVICE_NAME)
    @Param(description = "Device name")
    private String deviceName;

    @SerializedName(ApiConstants.LOAD_BALANCER_DEVICE_STATE)
    @Param(description = "Device state")
    private String deviceState;

    @SerializedName(ApiConstants.LOAD_BALANCER_DEVICE_CAPACITY)
    @Param(description = "Device capacity")
    private Long deviceCapacity;

    @SerializedName(ApiConstants.LOAD_BALANCER_DEVICE_DEDICATED)
    @Param(description = "True if device is dedicated for an account")
    private Boolean dedicatedLoadBalancer;

    @SerializedName(ApiConstants.PUBLIC_INTERFACE)
    @Param(description = "The public interface of the load balancer")
    private String publicInterface;

    @SerializedName(ApiConstants.PRIVATE_INTERFACE)
    @Param(description = "The private interface of the load balancer")
    private String privateInterface;

    @SerializedName(ApiConstants.IP_ADDRESS)
    @Param(description = "The management IP address of the external load balancer")
    private String ipAddress;

    @SerializedName(ApiConstants.GSLB_PROVIDER)
    @Param(description = "True if NetScaler device is provisioned to be a GSLB service provider")
    private Boolean isGslbProvider;

    @SerializedName(ApiConstants.EXCLUSIVE_GSLB_PROVIDER)
    @Param(description = "True if NetScaler device is provisioned exclusively to be a GSLB service provider")
    private Boolean isExclusiveGslbProvider;

    @SerializedName(ApiConstants.GSLB_PROVIDER_PUBLIC_IP)
    @Param(description = "Public IP of the NetScaler representing GSLB site")
    private String gslbSitePublicIp;

    @SerializedName(ApiConstants.GSLB_PROVIDER_PRIVATE_IP)
    @Param(description = "Private IP of the NetScaler representing GSLB site")
    private String gslbSitePrivateIp;

    @SerializedName(ApiConstants.POD_IDS)
    @Param(description = "Used when NetScaler device is provider of EIP service."
        + " This parameter represents the list of pod's, for which there exists a policy based route on datacenter L3 router to "
        + "route pod's subnet IP to a NetScaler device.")
    private List<Long> podIds;

    public void setId(String lbDeviceId) {
        this.id = lbDeviceId;
    }

    public void setPhysicalNetworkId(String physicalNetworkId) {
        this.physicalNetworkId = physicalNetworkId;
    }

    public void setProvider(String provider) {
        this.providerName = provider;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public void setDeviceCapacity(long deviceCapacity) {
        this.deviceCapacity = deviceCapacity;
    }

    public void setDedicatedLoadBalancer(boolean isDedicated) {
        this.dedicatedLoadBalancer = isDedicated;
    }

    public void setDeviceState(String deviceState) {
        this.deviceState = deviceState;
    }

    public void setPublicInterface(String publicInterface) {
        this.publicInterface = publicInterface;
    }

    public void setPrivateInterface(String privateInterface) {
        this.privateInterface = privateInterface;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public void setAssociatedPods(List<Long> pods) {
        this.podIds = pods;
    }

    public void setGslbProvider(boolean isGslbProvider) {
        this.isGslbProvider = isGslbProvider;
    }

    public void setExclusiveGslbProvider(boolean isExclusiveGslbProvider) {
        this.isExclusiveGslbProvider = isExclusiveGslbProvider;
    }

    public void setGslbSitePublicIp(String publicIP) {
        this.gslbSitePublicIp = publicIP;
    }

    public void setGslbSitePrivateIp(String privateIp) {
        this.gslbSitePrivateIp = privateIp;
    }
}
