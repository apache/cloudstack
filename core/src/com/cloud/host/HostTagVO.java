package com.cloud.host;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="host_tags")
public class HostTagVO {
	@Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id")
    private long id;
    
    @Column(name="host_id")
    private long hostId;
    
    @Column(name="tag")
    private String tag;
    
    protected HostTagVO() {
    }
    
    public HostTagVO(long hostId, String tag) {
        this.hostId = hostId;
        this.tag = tag;
    }

    public long getHostId() {
        return hostId;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public long getId() {
        return id;
    }
}
