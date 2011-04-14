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

package com.cloud.vm;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="user_vm_details")
public class UserVmDetailVO {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id")
    private long id;
    
    @Column(name="vm_id")
    private long vmId;
    
    @Column(name="name")
    private String name;
    
    @Column(name="value")
    private String value;
    
    public UserVmDetailVO() {}
    
    public UserVmDetailVO(long vmId, String name, String value) {
    	this.vmId = vmId;
    	this.name = name;
    	this.value = value;
    }

	public long getId() {
		return id;
	}

	public long getVmId() {
		return vmId;
	}

	public String getName() {
		return name;
	}

	public String getValue() {
		return value;
	}

	public void setId(long id) {
		this.id = id;
	}

	public void setVmId(long vmId) {
		this.vmId = vmId;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setValue(String value) {
		this.value = value;
	}
	
}
