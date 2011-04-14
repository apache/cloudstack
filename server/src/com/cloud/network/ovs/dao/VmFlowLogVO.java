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

package com.cloud.network.ovs.dao;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.cloud.utils.db.GenericDao;

@Entity
@Table(name="ovs_vm_flow_log")
public class VmFlowLogVO {
	@Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id")
    private Long id;
    
    @Column(name="instance_id", updatable=false, nullable=false)
    private Long instanceId;    // vm_instance id
    
    @Column(name=GenericDao.CREATED_COLUMN)
    private Date created;
    
    @Column(name="logsequence")
    long logsequence;
    
    @Column(name="vm_name", updatable=false, nullable=false, length=255)
	protected String name = null;
    
    protected VmFlowLogVO() {
    	
    }

	public VmFlowLogVO(Long instanceId, String name) {
		super();
		this.instanceId = instanceId;
		this.name = name;
	}

	public Long getId() {
		return id;
	}

	public Long getInstanceId() {
		return instanceId;
	}

	public Date getCreated() {
		return created;
	}

	public long getLogsequence() {
		return logsequence;
	}
    
	public void incrLogsequence() {
		logsequence++;
	}
	
	public String getName() {
		return name;
	}
}
