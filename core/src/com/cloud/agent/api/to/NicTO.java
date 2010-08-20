/**
 * 
 */
package com.cloud.agent.api.to;

public class NicTO extends NetworkTO {
    int deviceId;
    Integer controlPort;
    Integer networkRateMbps;
    Integer networkRateMulticastMbps;

    public NicTO() {
        super();
        controlPort = null;
    }
    
    public void setDeviceId(int deviceId) {
        this.deviceId = deviceId;
    }
    
    public int getDeviceId() {
        return deviceId;
    }
    
    public Integer getControlPort() {
        return controlPort;
    }
    
    public Integer getNetworkRateMbps() {
        return networkRateMbps;
    }
    
    public Integer getNetworkRateMulticastMbps() {
        return networkRateMulticastMbps;
    }
}
