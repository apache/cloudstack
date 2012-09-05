package com.cloud.bridge.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name="account")
public class CloudStackAccountVO {
    
    @Column(name="uuid")
    private String uuid;

    @Column(name="default_zone_id")
    private Long defaultZoneId = null;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public Long getDefaultZoneId() {
        return defaultZoneId;
    }

    public void setDefaultZoneId(Long defaultZoneId) {
        this.defaultZoneId = defaultZoneId;
    }


}
