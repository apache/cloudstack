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
@Table(name="op_dc_vnet_alloc")
public class DataCenterVnetVO {
    
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id")
    Long id;
    
    @Column(name="taken", nullable=true)
    @Temporal(value=TemporalType.TIMESTAMP)
    Date takenAt;

    @Column(name="vnet", updatable=false, nullable=false)
    protected String vnet;
    
    @Column(name="data_center_id", updatable=false, nullable=false)
    protected long dataCenterId;

    @Column(name="account_id")
    protected Long accountId;
    
    public Date getTakenAt() {
        return takenAt;
    }

    public void setTakenAt(Date taken) {
        this.takenAt = taken;
    }

    public DataCenterVnetVO(String vnet, long dcId) {
        this.vnet = vnet;
        this.dataCenterId = dcId;
        this.takenAt = null;
    }
    
    public Long getId() {
        return id;
    }
    
    public String getVnet() {
        return vnet;
    }
    
    public Long getAccountId() {
    	return accountId;
    }
    
    public void setAccountId(Long accountId) {
    	this.accountId = accountId;
    }
    
    public long getDataCenterId() {
        return dataCenterId;
    }
    
    protected DataCenterVnetVO() {
    }
}
