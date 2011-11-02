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

import java.util.Date;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.SecondaryTable;
import javax.persistence.Table;

import com.cloud.api.Identity;
import com.cloud.utils.db.GenericDao;

@Entity
@Table(name="instance_group")
@SecondaryTable(name="account",
        pkJoinColumns={@PrimaryKeyJoinColumn(name="account_id", referencedColumnName="id")})
public class InstanceGroupVO implements InstanceGroup, Identity {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id")
    private long id;
    
    @Column(name="name")
    String name;
    
    @Column(name="account_id")
    private long accountId;
    
    @Column(name="domain_id", table="account", insertable=false, updatable=false)
    private long domainId;
    
    @Column(name=GenericDao.REMOVED_COLUMN)
    private Date removed;

    @Column(name=GenericDao.CREATED_COLUMN)
    private Date created;

    @Column(name="uuid")
    private String uuid;
    
    public InstanceGroupVO(String name, long accountId) {
        this.name = name;
        this.accountId = accountId;
        this.uuid = UUID.randomUUID().toString();
    }
    
    protected InstanceGroupVO() {
        super();
    }
    
    @Override
    public long getId() {
    	return id;
    }
    
    @Override
    public String getName() {
    	return name; 
    }
    
    @Override
    public long getAccountId() {
        return accountId;
    }
    
    public long getDomainId() {
        return domainId;
    }
    
    public Date getRemoved() {
        return removed;
    }
    
	public Date getCreated() {
		return created;
	}
    
    public void setName(String name) {
    	this.name = name;
    }

    @Override
    public String getUuid() {
    	return this.uuid;
    }
    
    public void setUuid(String uuid) {
    	this.uuid = uuid;
    }
}
