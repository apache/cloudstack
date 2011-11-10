/**
 * Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved
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
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

@SuppressWarnings("unused")
public class TrafficTypeResponse extends BaseResponse {

    @SerializedName(ApiConstants.ID) @Param(description="id of the network provider")
    private Long id;

    @SerializedName(ApiConstants.TRAFFIC_TYPE) @Param(description="the trafficType to be added to the physical network")
    private String trafficType;
    
    @SerializedName(ApiConstants.PHYSICAL_NETWORK_ID) @Param(description="the physical network this belongs to")
    private Long physicalNetworkId;
    
    @SerializedName(ApiConstants.XEN_NETWORK_LABEL) @Param(description="The network name label of the physical device dedicated to this traffic on a XenServer host")
    private String xenNetworkLabel;

    @SerializedName(ApiConstants.KVM_NETWORK_LABEL) @Param(description="The network name label of the physical device dedicated to this traffic on a KVM host")
    private String kvmNetworkLabel;

    @SerializedName(ApiConstants.VMWARE_NETWORK_LABEL) @Param(description="The network name label of the physical device dedicated to this traffic on a VMware host")
    private String vmwareNetworkLabel;

    public void setPhysicalNetworkId(long physicalNetworkId) {
        this.physicalNetworkId = physicalNetworkId;
    }

    public long getphysicalNetworkId() {
        return physicalNetworkId;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getId() {
        return this.id;
    }
    
    public String getTrafficType() {
        return trafficType;
    }
    
    public void setTrafficType(String trafficType) {
        this.trafficType = trafficType;
    }
    
    public String getXenLabel() {
        return xenNetworkLabel;
    }

    public String getKvmLabel() {
        return kvmNetworkLabel;
    }
    
    public void setXenLabel(String xenLabel) {
        this.xenNetworkLabel = xenLabel;
    }

    public void setKvmLabel(String kvmLabel) {
        this.kvmNetworkLabel = kvmLabel;
    }

    public void setVmwareLabel(String vmwareNetworkLabel) {
        this.vmwareNetworkLabel = vmwareNetworkLabel;
    }    
    
    public String getVmwareLabel() {
        return vmwareNetworkLabel;
    }
}
