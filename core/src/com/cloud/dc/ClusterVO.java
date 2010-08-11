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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.cloud.utils.NumbersUtil;

@Entity
@Table(name="cluster")
public class ClusterVO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="id")
    long id;
    
    @Column(name="name")
    String name;
    
    @Column(name="data_center_id")
    long dataCenterId;
    
    @Column(name="pod_id")
    long podId;
    
    public ClusterVO() {
    }
    
    public ClusterVO(long dataCenterId, long podId, String name) {
        this.dataCenterId = dataCenterId;
        this.podId = podId;
        this.name = name;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public long getDataCenterId() {
        return dataCenterId;
    }

    public long getPodId() {
        return podId;
    }
    
    public ClusterVO(long clusterId) {
        this.id = clusterId;
    }

    @Override
    public int hashCode() {
        return NumbersUtil.hash(id);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ClusterVO)) {
            return false;
        }
        ClusterVO that = (ClusterVO)obj;
        return this.id == that.id;
    }
}
