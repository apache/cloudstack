/**
 *  Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package com.cloud.api.response;

import com.cloud.api.ApiConstants;
import com.cloud.utils.IdentityProxy;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class NetscalerLoadBalancerResponse extends BaseResponse {

    @SerializedName(ApiConstants.LOAD_BALANCER_DEVICE_ID) @Param(description="device id of the netscaler load balancer")
    private IdentityProxy id = new IdentityProxy("external_load_balancer_devices");

    @SerializedName(ApiConstants.PHYSICAL_NETWORK_ID) @Param(description="the physical network to which this netscaler device belongs to")
    private IdentityProxy physicalNetworkId = new IdentityProxy("physical_network");

    @SerializedName(ApiConstants.PROVIDER) @Param(description="name of the provider")
    private String providerName;
    
    @SerializedName(ApiConstants.LOAD_BALANCER_DEVICE_NAME) @Param(description="device name")
    private String deviceName; 
    
    @SerializedName(ApiConstants.LOAD_BALANCER_DEVICE_STATE) @Param(description="device state")
    private String deviceState;

    @SerializedName(ApiConstants.LOAD_BALANCER_DEVICE_CAPACITY) @Param(description="device capacity")
    private Long deviceCapacity;

    @SerializedName(ApiConstants.LOAD_BALANCER_DEVICE_DEDICATED) @Param(description="true if device is dedicated for an account")
    private Boolean dedicatedLoadBalancer;

    @SerializedName(ApiConstants.INLINE) @Param(description="true if device is inline with firewall device")
    private Boolean inlineLoadBalancer;

    @SerializedName(ApiConstants.PUBLIC_INTERFACE) @Param(description="the public interface of the load balancer")
    private String publicInterface;
    
    @SerializedName(ApiConstants.PRIVATE_INTERFACE) @Param(description="the private interface of the load balancer")
    private String privateInterface;

    @SerializedName(ApiConstants.IP_ADDRESS) @Param(description="the management IP address of the external load balancer")
    private String ipAddress;

    public void setId(long lbDeviceId) {
        this.id.setValue(lbDeviceId);
    }

    public void setPhysicalNetworkId(long physicalNetworkId) {
        this.physicalNetworkId.setValue(physicalNetworkId);
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

    public void setInlineMode(boolean inline) {
        this.inlineLoadBalancer = inline;
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
}
