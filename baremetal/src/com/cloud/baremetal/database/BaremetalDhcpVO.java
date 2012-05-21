package com.cloud.baremetal.database;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="external_dhcp_devices")
public class BaremetalDhcpVO {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;
    
    @Column(name="uuid")
    private String uuid;
    
    @Column(name = "host_id")
    private long hostId;
    
    @Column(name = "pod_id")
    private long podId;

    @Column(name = "physical_network_id")
    private long physicalNetworkId;
    
    @Column(name = "nsp_id")
    private long networkServiceProviderId ;
    
    @Column(name = "device_type")
    private String deviceType;

    public BaremetalDhcpVO() {
        super();
        this.uuid = UUID.randomUUID().toString();
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public long getHostId() {
        return hostId;
    }

    public void setHostId(long hostId) {
        this.hostId = hostId;
    }

    public long getPhysicalNetworkId() {
        return physicalNetworkId;
    }

    public void setPhysicalNetworkId(long physicalNetworkId) {
        this.physicalNetworkId = physicalNetworkId;
    }
    
    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public long getNetworkServiceProviderId() {
        return networkServiceProviderId;
    }

    public void setNetworkServiceProviderId(long networkServiceProviderId) {
        this.networkServiceProviderId = networkServiceProviderId;
    }

    public long getPodId() {
        return podId;
    }

    public void setPodId(long podId) {
        this.podId = podId;
    }
}
