package com.cloud.simulator;

public interface MockHost {
    public long getCpuSpeed();
    public long getCpuCount();
    
   
    public long getMemorySize();
    
    public String getCapabilities();
    
    public long getId();
    
    public String getName();
    
    public String getGuid();
    
    
    public String getVersion();
    
    public Long getDataCenterId();
    
    public Long getPodId();
    
    public Long getClusterId();
    
    public String getPrivateIpAddress();
    
    public String getPrivateNetMask();
    
    public String getPrivateMacAddress();
    
    
    public String getPublicIpAddress();
    
    public String getPublicNetMask();
    
    public String getPublicMacAddress();
    
    public String getStorageIpAddress();
    
    public String getStorageNetMask();
    
    public String getStorageMacAddress();
    
}
