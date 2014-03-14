package com.cloud.network.dao;

import org.apache.cloudstack.api.InternalIdentity;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Id;
import javax.persistence.Column;


@Entity
@Table(name = "op_router_monitoring_services")
public class OpRouterMonitorServiceVO implements InternalIdentity {

    @Id
    @Column(name="vm_id")
    Long id;

    @Column(name="router_name")
    private String name;

    @Column(name="last_alert_timestamp")
    private String lastAlertTimestamp;


    public OpRouterMonitorServiceVO() {}

    public OpRouterMonitorServiceVO(long vmId, String name, String lastAlertTimestamp) {
        this.id = vmId;
        this.name = name;
        this.lastAlertTimestamp = lastAlertTimestamp;
    }


    @Override
    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getLastAlertTimestamp() {
        return lastAlertTimestamp;
    }

    public void setLastAlertTimestamp (String timestamp) {
        this.lastAlertTimestamp = timestamp;
    }

}
