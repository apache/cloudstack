// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.region;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;

import com.cloud.region.Region;
import com.cloud.utils.db.GenericDao;

@Entity
@Table(name="region")
public class RegionVO implements Region{

    @Id
    @Column(name="id")
    private long id;
    
    @Column(name="name")
    private String name;

    @Column(name="end_point")
    private String endPoint;
    
    @Column(name="status")
    @Enumerated(value=EnumType.STRING)
    private Region.State status;
    
    @Column(name=GenericDao.REMOVED_COLUMN)
    private Date removed;

    public RegionVO() {
    }
    
    public RegionVO(long id, String name, String endPoint) {
    	this.id = id;
    	this.name = name;
    	this.endPoint = endPoint;
    	this.status = Region.State.Down;
    }

	public long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Region.State getStatus() {
		return status;
	}

	public void setStatus(Region.State status) {
		this.status = status;
	}

	public Date getRemoved() {
		return removed;
	}

	public String getEndPoint() {
		return endPoint;
	}

	public void setEndPoint(String endPoint) {
		this.endPoint = endPoint;
	}

    
}
