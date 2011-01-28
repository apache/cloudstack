package com.cloud.storage;

import javax.persistence.Column;
import javax.persistence.Entity;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="storage_pool_work")
public class StoragePoolWorkVO {
    
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPoolId() {
        return poolId;
    }


    public void setPoolId(Long poolId) {
        this.poolId = poolId;
    }


    public boolean isStoppedForMaintenance() {
        return stoppedForMaintenance;
    }


    public void setStoppedForMaintenance(boolean stoppedForMaintenance) {
        this.stoppedForMaintenance = stoppedForMaintenance;
    }


    public boolean isStartedAfterMaintenance() {
        return startedAfterMaintenance;
    }

    public void setStartedAfterMaintenance(boolean startedAfterMaintenance) {
        this.startedAfterMaintenance = startedAfterMaintenance;
    }

    public Long getVmId() {
        return vmId;
    }

    public void setVmId(Long vmId) {
        this.vmId = vmId;
    }
    
    public Long getManagementServerId() {
        return managementServerId;
    }

    public void setManagementServerId(Long managementServerId) {
        this.managementServerId = managementServerId;
    }

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id")
    private Long id;
    
    @Column(name="pool_id")
    private Long poolId;

    @Column(name="vm_id")
    private Long vmId;
    
    @Column(name="stopped_for_maintenance")
    private boolean stoppedForMaintenance;
    
    @Column(name="started_after_maintenance")
    private boolean startedAfterMaintenance;

    @Column(name="mgmt_server_id")
    private Long managementServerId;
    

    public StoragePoolWorkVO(long vmId, long poolId, boolean stoppedForMaintenance, boolean startedAfterMaintenance, long mgmtServerId) {
        super();
        this.vmId = vmId;
        this.poolId = poolId;
        this.stoppedForMaintenance = stoppedForMaintenance;
        this.startedAfterMaintenance = startedAfterMaintenance;
        this.managementServerId = mgmtServerId;
    }
    
    public StoragePoolWorkVO() {
        
    }
}
