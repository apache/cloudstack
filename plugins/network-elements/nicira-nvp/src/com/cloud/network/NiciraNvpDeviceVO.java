package com.cloud.network;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="external_nicira_nvp_devices")
public class NiciraNvpDeviceVO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="id")
    private long id;
    
    @Column(name="uuid")
    private String uuid;
    
    @Column(name="host_id")
    private long hostId;
    
    @Column(name="physical_network_id")
    private long physicalNetworkId;
    
    @Column(name="provider_name")
    private String providerName;
    
    @Column(name="device_name")
    private String deviceName;

    
    public NiciraNvpDeviceVO() {
        this.uuid = UUID.randomUUID().toString();
    }

    public NiciraNvpDeviceVO(long hostId, long physicalNetworkId,
            String providerName, String deviceName) {
        super();
        this.hostId = hostId;
        this.physicalNetworkId = physicalNetworkId;
        this.providerName = providerName;
        this.deviceName = deviceName;
        this.uuid = UUID.randomUUID().toString();
    }

    public long getId() {
        return id;
    }
    
    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public long getPhysicalNetworkId() {
        return physicalNetworkId;
    }

    public long getHostId() {
        return hostId;
    }

    public String getProviderName() {
        return providerName;
    }

    public String getDeviceName() {
        return deviceName;
    }
    
}
