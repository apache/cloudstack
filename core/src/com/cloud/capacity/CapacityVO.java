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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="op_host_capacity")
public class CapacityVO {
    public static final short CAPACITY_TYPE_MEMORY = 0;
    public static final short CAPACITY_TYPE_CPU = 1;
    public static final short CAPACITY_TYPE_STORAGE = 2;
    public static final short CAPACITY_TYPE_STORAGE_ALLOCATED = 3;
    public static final short CAPACITY_TYPE_PUBLIC_IP = 4;
    public static final short CAPACITY_TYPE_PRIVATE_IP = 5;
    public static final short CAPACITY_TYPE_SECONDARY_STORAGE = 6;
    

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id")
    private Long id = null;

    @Column(name="host_id")
    private Long hostOrPoolId;

    @Column(name="data_center_id")
    private long dataCenterId;

    @Column(name="pod_id")
    private Long podId;

    @Column(name="used_capacity")
    private long usedCapacity;

    @Column(name="total_capacity")
    private long totalCapacity;

    @Column(name="capacity_type")
    private short capacityType;

    public CapacityVO() {}

    public CapacityVO(Long hostId, long dataCenterId, Long podId, long usedCapacity, long totalCapacity, short capacityType) {
        this.hostOrPoolId = hostId;
        this.dataCenterId = dataCenterId;
        this.podId = podId;
        this.usedCapacity = usedCapacity;
        this.totalCapacity = totalCapacity;
        this.capacityType = capacityType;
    }

    public Long getId() {
        return id;
    }
    
    public Long getHostOrPoolId() {
        return hostOrPoolId;
    }
    
    public void setHostId(Long hostId) {
        this.hostOrPoolId = hostId;
    }
    public long getDataCenterId() {
        return dataCenterId;
    }
    public void setDataCenterId(long dataCenterId) {
        this.dataCenterId = dataCenterId;
    }
    
    public Long getPodId() {
        return podId;
    }
    public void setPodId(long podId) {
        this.podId = new Long(podId);
    }
    public long getUsedCapacity() {
        return usedCapacity;
    }
    public void setUsedCapacity(long usedCapacity) {
        this.usedCapacity = usedCapacity;
    }
    public long getTotalCapacity() {
        return totalCapacity;
    }
    public void setTotalCapacity(long totalCapacity) {
        this.totalCapacity = totalCapacity;
    }
    public short getCapacityType() {
        return capacityType;
    }
    public void setCapacityType(short capacityType) {
        this.capacityType = capacityType;
    }
}
