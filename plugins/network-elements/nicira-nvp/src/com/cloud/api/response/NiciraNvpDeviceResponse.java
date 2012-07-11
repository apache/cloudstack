package com.cloud.api.response;

import com.cloud.api.ApiConstants;
import com.cloud.serializer.Param;
import com.cloud.utils.IdentityProxy;
import com.google.gson.annotations.SerializedName;

public class NiciraNvpDeviceResponse extends BaseResponse {
    @SerializedName(ApiConstants.NICIRA_NVP_DEVICE_ID) @Param(description="device id of the Nicire Nvp")
    private IdentityProxy id = new IdentityProxy("external_nicira_nvp_devices");
    
    @SerializedName(ApiConstants.PHYSICAL_NETWORK_ID) @Param(description="the physical network to which this Nirica Nvp belongs to")
    private IdentityProxy physicalNetworkId = new IdentityProxy("physical_network");
    
    @SerializedName(ApiConstants.PROVIDER) @Param(description="name of the provider")
    private String providerName;
    
    @SerializedName(ApiConstants.NICIRA_NVP_DEVICE_NAME) @Param(description="device name")
    private String deviceName;

    public void setId(long nvpDeviceId) {
        this.id.setValue(nvpDeviceId);
    }

    public void setPhysicalNetworkId(long physicalNetworkId) {
        this.physicalNetworkId.setValue(physicalNetworkId);
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }     
    
}
