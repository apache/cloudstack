/**
 * 
 */
package com.cloud.agent.api.to;

public class NicTO extends NetworkTO {
    int deviceId;
    Integer networkRateMbps;
    Integer networkRateMulticastMbps;
    String bootParams;
    boolean defaultNic;

    public NicTO() {
        super();
    }
    
    public void setDeviceId(int deviceId) {
        this.deviceId = deviceId;
    }
    
    public int getDeviceId() {
        return deviceId;
    }
    
    public Integer getNetworkRateMbps() {
        return networkRateMbps;
    }
    
    public Integer getNetworkRateMulticastMbps() {
        return networkRateMulticastMbps;
    }
    
    public String getBootParams() {
        return bootParams;
    }
    
    public void setBootParams(String bootParams) {
        this.bootParams = bootParams;
    }
    
    public boolean isDefaultNic() {
        return defaultNic;
    }
    
    public void setDefaultNic(boolean defaultNic) {
        this.defaultNic = defaultNic;
    }
    
    @Override
    public String toString() {
        return new StringBuilder("[Nic:").append(type).append("-").append(ip).append("-").append(broadcastUri).append("]").toString();
    }
}
