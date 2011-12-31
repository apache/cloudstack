package com.cloud.api.response;

import com.cloud.api.ApiConstants;
import com.cloud.api.IdentityProxy;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class StorageNetworkIpRangeResponse extends BaseResponse {
    @SerializedName(ApiConstants.VLAN) @Param(description="the ID of storage network IP range.")
    private Long id;
    
    @SerializedName(ApiConstants.VLAN) @Param(description="the ID or VID of the VLAN.")
    private Integer vlan;
    
    @SerializedName(ApiConstants.POD_ID) @Param(description="the Pod ID for the VLAN IP range")
    private IdentityProxy podId = new IdentityProxy("host_pod_ref");

    @SerializedName(ApiConstants.START_IP) @Param(description="the start ip of the VLAN IP range")
    private String startIp;

    @SerializedName(ApiConstants.END_IP) @Param(description="the end ip of the VLAN IP range")
    private String endIp;
    
    @SerializedName(ApiConstants.NETWORK_ID) @Param(description="the network id of vlan range")
    private IdentityProxy networkId = new IdentityProxy("networks");
    
    @SerializedName(ApiConstants.ZONE_ID) @Param(description="the Zone ID of the VLAN IP range")
    private IdentityProxy zoneId = new IdentityProxy("data_center");
     
	public void setId(Long id) {
		this.id = id;
	}
	
    public void setZoneId(Long zoneId) {
        this.zoneId.setValue(zoneId);
    }
    
    public void setVlan(Integer vlan) {
        this.vlan = vlan;
    }
    
    public void setPodId(Long podId) {
        this.podId.setValue(podId);
    }
       
    public void setStartIp(String startIp) {
        this.startIp = startIp;
    }

    public void setEndIp(String endIp) {
        this.endIp = endIp;
    }

    public void setNetworkId(Long networkId) {
        this.networkId.setValue(networkId);
    }
}
