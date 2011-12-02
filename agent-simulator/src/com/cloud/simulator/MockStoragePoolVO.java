package com.cloud.simulator;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.cloud.storage.Storage.StoragePoolType;

@Entity
@Table(name="mockstoragepool")

public class MockStoragePoolVO {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id")
    private long id;
    
    @Column(name="guid")
    private String uuid;
    
    @Column(name="mount_point")
    private String mountPoint;
    
    @Column(name="capacity")
    private long capacity;
    
    @Column(name="hostguid")
    private String hostGuid;
    
    @Column(name="pool_type")
    @Enumerated(value=EnumType.STRING)
    private StoragePoolType poolType;
    
    public MockStoragePoolVO() {
        
    }
    
    public String getHostGuid() {
        return this.hostGuid;
    }
    
    public void setHostGuid(String hostGuid) {
        this.hostGuid = hostGuid;
    }
    
    public long getId() {
        return this.id;
    }
    
    public StoragePoolType getPoolType() {
        return this.poolType;
    }
    
    public void setStorageType(StoragePoolType poolType) {
        this.poolType = poolType;
    }
    
    public String getUuid() {
        return this.uuid;
    }
    
    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
    
    public String getMountPoint() {
        return this.mountPoint;
    }
    
    public void setMountPoint(String mountPoint) {
        this.mountPoint = mountPoint;
    }
    
    public long getCapacity() {
        return this.capacity;
    }
    
    public void setCapacity(long capacity) {
        this.capacity = capacity;
    }
}
