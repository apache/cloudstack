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
import com.cloud.api.IdentityProxy;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class SrxFirewallResponse extends BaseResponse {

    @SerializedName(ApiConstants.FIREWALL_DEVICE_ID) @Param(description="device id of the SRX firewall")
    private IdentityProxy id = new IdentityProxy("external_firewall_devices");

    @SerializedName(ApiConstants.PHYSICAL_NETWORK_ID) @Param(description="the physical network to which this SRX firewall belongs to")
    private IdentityProxy physicalNetworkId = new IdentityProxy("physical_network");

    @SerializedName(ApiConstants.PROVIDER) @Param(description="name of the provider")
    private String providerName;
    
    @SerializedName(ApiConstants.FIREWALL_DEVICE_NAME) @Param(description="device name")
    private String deviceName; 
    
    @SerializedName(ApiConstants.FIREWALL_DEVICE_STATE) @Param(description="device state")
    private String deviceState;

    @SerializedName(ApiConstants.FIREWALL_DEVICE_CAPACITY) @Param(description="device capacity")
    private Long deviceCapacity;

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

    public void setDeviceState(String deviceState) {
        this.deviceState = deviceState;
    }
}
