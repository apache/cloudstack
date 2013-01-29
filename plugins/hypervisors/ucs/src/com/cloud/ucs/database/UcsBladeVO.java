package com.cloud.ucs.database;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="ucs_blade")
public class UcsBladeVO {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id")
    private long id;
    
    @Column(name="uuid")
    private String uuid;
    
    @Column(name="ucs_manager_id")
    private long ucsManagerId;
    
    @Column(name="host_id")
    private Long hostId;
    
    @Column(name="dn")
    private String dn;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getUcsManagerId() {
        return ucsManagerId;
    }

    public void setUcsManagerId(long ucsManagerId) {
        this.ucsManagerId = ucsManagerId;
    }

    public Long getHostId() {
        return hostId;
    }

    public void setHostId(Long hostId) {
        this.hostId = hostId;
    }

    public String getDn() {
        return dn;
    }

    public void setDn(String dn) {
        this.dn = dn;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}
