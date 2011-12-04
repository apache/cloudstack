package com.cloud.simulator;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="mocksecstorage")

public class MockSecStorageVO {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id")
    private long id;
    
    @Column(name="url")
    private String url;
    
    @Column(name="capacity")
    private long capacity;
    
    @Column(name="mount_point")
    private String mountPoint;
    
    
    public MockSecStorageVO() {
        
    }
    
    public long getId() {
        return this.id;
    }
    
    public String getMountPoint() {
        return this.mountPoint;
    }
    
    public void setMountPoint(String mountPoint) {
        this.mountPoint = mountPoint;
    }
    
    public String getUrl() {
        return url;
    }
    
    public void setUrl(String url) {
        this.url = url;
    }
    
    public long getCapacity() {
        return this.capacity;
    }
    
    public void setCapacity(long capacity) {
        this.capacity = capacity;
    }
}
