package com.cloud.baremetal.networkservice;

import com.cloud.api.ApiConstants;
import com.cloud.api.response.BaseResponse;
import com.cloud.serializer.Param;
import com.cloud.utils.IdentityProxy;
import com.google.gson.annotations.SerializedName;

public class BaremetalPxeResponse extends BaseResponse {
    @SerializedName(ApiConstants.ID) @Param(description="device id of ")
    private IdentityProxy id = new IdentityProxy("external_pxe_devices");
    
    @SerializedName(ApiConstants.PHYSICAL_NETWORK_ID) @Param(description="the physical network to which this external dhcp device belongs to")
    private IdentityProxy physicalNetworkId = new IdentityProxy("physical_network");
    
    @SerializedName(ApiConstants.PROVIDER) @Param(description="name of the provider")
    private IdentityProxy providerId = new IdentityProxy("physical_network_service_providers");
    
    @SerializedName(ApiConstants.POD_ID) @Param(description="pod id where the device is in")
    private IdentityProxy podId = new IdentityProxy("host_pod_ref");

    public long getId() {
        return id.getValue();
    }

    public void setId(long id) {
        this.id.setValue(id);
    }

    public long getPhysicalNetworkId() {
        return physicalNetworkId.getValue();
    }

    public void setPhysicalNetworkId(long physicalNetworkId) {
        this.physicalNetworkId.setValue(physicalNetworkId);
    }

    public long getProviderId() {
        return providerId.getValue();
    }

    public void setProviderId(long providerId) {
        this.providerId.setValue(providerId);
    }

    public long getPodId() {
        return podId.getValue();
    }

    public void setPodId(long podId) {
        this.podId.setValue(podId);
    }
}
