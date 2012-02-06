package com.cloud.api.response;

import com.cloud.api.ApiConstants;
import com.cloud.api.IdentityProxy;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class StorageNetworkIpRangeResponse extends BaseResponse {
    @SerializedName(ApiConstants.VLAN) @Param(description="the uuid of storage network IP range.")
    private String uuid;
    
    @SerializedName(ApiConstants.VLAN) @Param(description="the ID or VID of the VLAN.")
    private Integer vlan;
    
    @SerializedName(ApiConstants.POD_ID) @Param(description="the Pod uuid for the storage network IP range")
    private String podUuid;

    @SerializedName(ApiConstants.START_IP) @Param(description="the start ip of the storage network IP range")
    private String startIp;

    @SerializedName(ApiConstants.END_IP) @Param(description="the end ip of the storage network IP range")
    private String endIp;
    
    @SerializedName(ApiConstants.NETWORK_ID) @Param(description="the network uuid of storage network IP range")
    private String networkUuid;
    
    @SerializedName(ApiConstants.ZONE_ID) @Param(description="the Zone uuid of the storage network IP range")
    private String zoneUuid;
    
    @SerializedName(ApiConstants.NETMASK) @Param(description="the netmask of the storage network IP range")
    private String netmask;
     
	public void setUuid(String uuId) {
		this.uuid = uuid;
	}
	
    public void setZoneUuid(String zoneUuid) {
        this.zoneUuid = zoneUuid;
    }
    
    public void setVlan(Integer vlan) {
        this.vlan = vlan;
    }
    
    public void setPodUuid(String podUuid) {
        this.podUuid = podUuid;
    }
       
    public void setStartIp(String startIp) {
        this.startIp = startIp;
    }

    public void setEndIp(String endIp) {
        this.endIp = endIp;
    }

    public void setNetworkUuid(String networkUuid) {
        this.networkUuid = networkUuid;
    }
    
    public void setNetmask(String netmask) {
    	this.netmask = netmask;
    }
}
