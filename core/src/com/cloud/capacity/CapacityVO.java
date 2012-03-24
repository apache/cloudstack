/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package com.cloud.capacity;

import javax.persistence.Transient;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import com.cloud.utils.db.GenericDao;

@Entity
@Table(name="op_host_capacity")
public class CapacityVO implements Capacity {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id")
    private long id;

    @Column(name="host_id")
    private Long hostOrPoolId;

    @Column(name="data_center_id")
    private Long dataCenterId;

    @Column(name="pod_id")
    private Long podId;

    @Column(name="cluster_id")
    private Long clusterId;

    @Column(name="used_capacity")
    private long usedCapacity;

    @Column(name="reserved_capacity")
    private long reservedCapacity;
    
    @Column(name="total_capacity")
    private long totalCapacity;

    @Column(name="capacity_type")
    private short capacityType;
    
    @Column(name="capacity_state")
    private CapacityState capacityState = CapacityState.Enabled;
    
    @Column(name=GenericDao.CREATED_COLUMN)
    protected Date created;    
    
    @Column(name="update_time", updatable=true, nullable=true)
    @Temporal(value=TemporalType.TIMESTAMP)
    protected Date updateTime;
    
    @Transient
    private Float usedPercentage;
    
    public CapacityVO() {}

    public CapacityVO(Long hostId, Long dataCenterId, Long podId, Long clusterId, long usedCapacity, long totalCapacity, short capacityType) {
        this.hostOrPoolId = hostId;
        this.dataCenterId = dataCenterId;
        this.podId = podId;
        this.clusterId = clusterId;
        this.usedCapacity = usedCapacity;
        this.totalCapacity = totalCapacity;
        this.capacityType = capacityType;
        this.updateTime = new Date();
    }
    
    public CapacityVO(Long dataCenterId, Long podId, Long clusterId, short capacityType, float usedPercentage) {        
        this.dataCenterId = dataCenterId;
        this.podId = podId;
        this.clusterId = clusterId;
        this.capacityType = capacityType;
        this.usedPercentage = usedPercentage;
    }

    @Override
    public long getId() {
        return id;
    }
    
    @Override
    public Long getHostOrPoolId() {
        return hostOrPoolId;
    }
    
    public void setHostId(Long hostId) {
        this.hostOrPoolId = hostId;
    }
    @Override
    public Long getDataCenterId() {
        return dataCenterId;
    }
    public void setDataCenterId(Long dataCenterId) {
        this.dataCenterId = dataCenterId;
    }
    
    @Override
    public Long getPodId() {
        return podId;
    }
    public void setPodId(long podId) {
        this.podId = new Long(podId);
    }
    
    @Override
    public Long getClusterId() {
        return clusterId;
    }
    public void setClusterId(long clusterId) {
        this.clusterId = new Long(clusterId);
    }
    
    @Override
    public long getUsedCapacity() {
        return usedCapacity;
    }
    public void setUsedCapacity(long usedCapacity) {
        this.usedCapacity = usedCapacity;
        this.setUpdateTime (new Date());
    }
    @Override
    public long getReservedCapacity() {
        return reservedCapacity;
    }
    public void setReservedCapacity(long reservedCapacity) {
        this.reservedCapacity = reservedCapacity;
        this.setUpdateTime (new Date());
    }
    @Override
    public long getTotalCapacity() {
        return totalCapacity;
    }
    public void setTotalCapacity(long totalCapacity) {
        this.totalCapacity = totalCapacity;
        this.setUpdateTime (new Date());
    }
    @Override
    public short getCapacityType() {
        return capacityType;
    }
    public void setCapacityType(short capacityType) {
        this.capacityType = capacityType;
    }

	public CapacityState getCapacityState() {
        return capacityState;
    }

    public void setCapacityState(CapacityState capacityState) {
        this.capacityState = capacityState;
    }

    public Date getCreated() {
		return created;
	}

	public Date getUpdateTime() {
		return updateTime;
	}

	public void setUpdateTime(Date updateTime) {
		this.updateTime = updateTime;
	}

	@Override
    public Float getUsedPercentage() {
        return usedPercentage;
    }

    public void setUsedPercentage(float usedPercentage) {
        this.usedPercentage = usedPercentage;
    }
}
