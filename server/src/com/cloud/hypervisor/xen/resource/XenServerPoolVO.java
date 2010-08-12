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
package com.cloud.hypervisor.xen.resource;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="xen_server_pool")
public class XenServerPoolVO {
    
    @Id
    @Column(name="pool_uuid", updatable=false, nullable=false)
    String poolUuid;
    
    @Column(name="pod_id", updatable=false, nullable=false)
    long podId;
    
    @Column(name="name")
    String name;
    
    public XenServerPoolVO(String poolUuid, long podId) {
        this.poolUuid = poolUuid;
        this.podId = podId;
    }
    
    protected XenServerPoolVO() {
    }
    
    public long getPodId() {
        return podId;
    }
    
    public String getPoolUuid() {
        return poolUuid;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
}
