package com.cloud.simulator;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.cloud.storage.VMTemplateStorageResourceAssoc;
import com.cloud.storage.VMTemplateStorageResourceAssoc.Status;


@Entity
@Table(name="mockvolume")

public class MockVolumeVO {
    public enum MockVolumeType {
        VOLUME,
        TEMPLATE,
        ISO,
        SNAPSHOT;
    }
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id")
    private long id;
    
    @Column(name="name")
    private String name;
    
    @Column(name="size")
    private long size;
    
    @Column(name="path")
    private String path;
    
    @Column(name="pool_id")
    private long poolId;
    
    @Column(name="type")
    @Enumerated(value=EnumType.STRING)
    private MockVolumeType type;
    
    @Column(name="status")
    @Enumerated(value=EnumType.STRING)
    private VMTemplateStorageResourceAssoc.Status status;
    
    public long getId() {
        return id;
    }
    public String getName() {
        return this.name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public long getSize() {
        return this.size;
    }
    
    public void setSize(long size) {
        this.size = size;
    }
    
    public String getPath() {
        return this.path;
    }
    
    public void setPath(String path) {
        this.path = path;
    }
    
    public long getPoolId() {
        return this.poolId;
    }
    
    public void setPoolId(long poolId) {
        this.poolId = poolId;
    }
    
    public MockVolumeType getType() {
        return this.type;
    }
    
    public void setType(MockVolumeType type) {
        this.type = type;
    }
    
    public Status getStatus() {
        return this.status;
    }
    
    public void setStatus(Status status) {
        this.status = status;
    }
    
}
