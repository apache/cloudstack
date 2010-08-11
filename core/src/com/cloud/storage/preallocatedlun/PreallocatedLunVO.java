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
package com.cloud.storage.preallocatedlun;

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
@Table(name="ext_lun_alloc")
public class PreallocatedLunVO {
    
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id")
    private long id;
    
    private String portal;
    
    @Column(name="target_iqn")
    private String targetIqn;
    
    private int lun;
    
    @Column(name="data_center_id")
    private long dataCenterId;
    
    private long size;
    
    @Column(name="taken", nullable=true)
    @Temporal(value=TemporalType.TIMESTAMP)
    private Date taken;

    @Column(name="volume_id")
    private Long volumeId;
    
    public PreallocatedLunVO(long dataCenterId, String portal, String targetIqn, int lun, long size) {
        this.portal = portal;
        this.targetIqn = targetIqn;
        this.lun = lun;
        this.size = size;
        this.taken = null;
        this.volumeId = null;
        this.dataCenterId = dataCenterId;
    }
    
    public long getDataCenterId() {
        return dataCenterId;
    }

    public long getId() {
        return id;
    }
    
    public Date getTaken() {
        return taken;
    }
    
    public Long getVolumeId() {
        return volumeId;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getPortal() {
        return portal;
    }

    public void setPortal(String portal) {
        this.portal = portal;
    }

    public String getTargetIqn() {
        return targetIqn;
    }

    public void setTargetIqn(String targetIqn) {
        this.targetIqn = targetIqn;
    }

    public int getLun() {
        return lun;
    }

    public void setLun(int lun) {
        this.lun = lun;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public void setTaken(Date taken) {
        this.taken = taken;
    }

    public void setVolumeId(Long instanceId) {
        this.volumeId = instanceId;
    }
    
    protected PreallocatedLunVO() {
    }
}
