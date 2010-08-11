/**
 *  Copyright (C) 2010 Cloud.com.  All rights reserved.
 *
 * This software is licensed under the GNU General Public License v3 or later. 
 *
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later
version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.cloud.network.security;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.cloud.utils.db.GenericDao;

/**
 * Records the intent to update a VM's ingress ruleset
 *
 */
@Entity
@Table(name="op_vm_ruleset_log")
public class VmRulesetLogVO {
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
    
    protected VmRulesetLogVO() {
    	
    }

	public VmRulesetLogVO(Long instanceId) {
		super();
		this.instanceId = instanceId;
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
    
}
