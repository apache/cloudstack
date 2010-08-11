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
package com.cloud.dc;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Table(name="op_dc_link_local_ip_address_alloc")
public class DataCenterLinkLocalIpAddressVO {

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id")
    long id;
    
    @Column(name="ip_address", updatable=false, nullable=false)
    String ipAddress;
    
    @Column(name="taken")
    @Temporal(value=TemporalType.TIMESTAMP)
    private Date takenAt;
    
    @Column(name="data_center_id", updatable=false, nullable=false)
    private long dataCenterId;
    
    @Column(name="pod_id", updatable=false, nullable=false)
    private long podId;
    
    @Column(name="instance_id")
    private Long instanceId;
    
    protected DataCenterLinkLocalIpAddressVO() {
    }
    
    public DataCenterLinkLocalIpAddressVO(String ipAddress, long dataCenterId, long podId) {
        this.ipAddress = ipAddress;
        this.dataCenterId = dataCenterId;
        this.podId = podId;
    }
    
    public Long getId() {
        return id;
    }
    
    public Long getInstanceId() {
    	return instanceId;
    }
    
    public void setInstanceId(Long instanceId) {
    	this.instanceId = instanceId;
    }

    public long getPodId() {
        return podId;
    }

    public void setTakenAt(Date takenDate) {
        this.takenAt = takenDate;
    }

    public String getIpAddress() {
        return ipAddress;
    }
    
    public long getDataCenterId() {
        return dataCenterId;
    }

    public Date getTakenAt() {
        return takenAt;
    }
}
