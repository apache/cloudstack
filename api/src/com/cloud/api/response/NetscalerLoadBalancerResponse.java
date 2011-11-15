package com.cloud.api.response;

import com.cloud.api.ApiConstants;
import com.cloud.api.IdentityProxy;
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

    @SerializedName(ApiConstants.LOAD_BALANCER_DEVICE_DEDICATED) @Param(description="device capacity")
    private Boolean dedicatedLoadBalancer;

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
}
