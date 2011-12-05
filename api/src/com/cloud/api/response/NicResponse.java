/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
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

@SuppressWarnings("unused")
public class NicResponse extends BaseResponse {
    
    @SerializedName("id") @Param(description="the ID of the nic")
    private final IdentityProxy id = new IdentityProxy("nics");

    @SerializedName("networkid") @Param(description="the ID of the corresponding network")
    private final IdentityProxy networkId = new IdentityProxy("networks");
    
    @SerializedName(ApiConstants.NETMASK) @Param(description="the netmask of the nic")
    private String netmask;
    
    @SerializedName(ApiConstants.GATEWAY) @Param(description="the gateway of the nic")
    private String gateway;
    
    @SerializedName(ApiConstants.IP_ADDRESS) @Param(description="the ip address of the nic")
    private String ipaddress;
    
    @SerializedName("isolationuri") @Param(description="the isolation uri of the nic")
    private String isolationUri;
    
    @SerializedName("broadcasturi") @Param(description="the broadcast uri of the nic")
    private String broadcastUri;
    
    @SerializedName(ApiConstants.TRAFFIC_TYPE) @Param(description="the traffic type of the nic")
    private String trafficType;
    
    @SerializedName(ApiConstants.TYPE) @Param(description="the type of the nic")
    private String type;
    
    @SerializedName(ApiConstants.IS_DEFAULT) @Param(description="true if nic is default, false otherwise")
    private Boolean isDefault;

    @SerializedName("macaddress") @Param(description="true if nic is default, false otherwise")
    private String macAddress;
    
    public Long getId() {
        return id.getValue();
    }

    public void setId(Long id) {
        this.id.setValue(id);
    }

    public void setNetworkid(Long networkid) {
        this.networkId.setValue(networkid);
    }

    public void setNetmask(String netmask) {
        this.netmask = netmask;
    }

    public void setGateway(String gateway) {
        this.gateway = gateway;
    }

    public void setIpaddress(String ipaddress) {
        this.ipaddress = ipaddress;
    }

    public void setIsolationUri(String isolationUri) {
        this.isolationUri = isolationUri;
    }

    public void setBroadcastUri(String broadcastUri) {
        this.broadcastUri = broadcastUri;
    }

    public void setTrafficType(String trafficType) {
        this.trafficType = trafficType;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }

	public void setMacAddress(String macAddress) {
		this.macAddress = macAddress;
	}

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        NicResponse other = (NicResponse) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }
    
}
