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

package com.cloud.domain;

import java.util.Date;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.apache.log4j.Logger;

import com.cloud.api.Identity;
import com.cloud.utils.db.GenericDao;

@Entity
@Table(name="domain")
public class DomainVO implements Domain, Identity {
	public static final Logger s_logger = Logger.getLogger(DomainVO.class.getName());
	
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id")
    private long id;

    @Column(name="parent")
    private Long parent = null;

    @Column(name="name")
    private String name = null;

    @Column(name="owner")
    private long accountId;
    
    @Column(name="path")
    private String path = null;
    
    @Column(name="level")
    private int level;

    @Column(name=GenericDao.REMOVED_COLUMN)
    private Date removed;

    @Column(name="child_count")
    private int childCount = 0;

    @Column(name="next_child_seq")
    private long nextChildSeq = 1L;
    
    @Column(name="state")
    private Domain.State state;
    
    @Column(name="network_domain")
    private String networkDomain;
    
    @Column(name="uuid")
    private String uuid;
    
    public DomainVO() {}
    
    public DomainVO(long id, String name, long owner, Long parentId, String networkDomain) {
        this(name, owner, parentId, networkDomain);
        this.id = id;
        this.uuid = UUID.randomUUID().toString();
    }

    public DomainVO(String name, long owner, Long parentId, String networkDomain) {
    	this.parent = parentId;
        this.name = name;
        this.accountId = owner;
        this.path ="";
        this.level = 0;
        this.state = Domain.State.Active;
        this.networkDomain = networkDomain;
        this.uuid = UUID.randomUUID().toString();
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public Long getParent() {
        return parent;
    }
    
    @Override
    public void setParent(Long parent) {
    	if(parent == null) {
    		this.parent = DomainVO.ROOT_DOMAIN;
    	} else {
    		if(parent.longValue() <= DomainVO.ROOT_DOMAIN)
    			this.parent = DomainVO.ROOT_DOMAIN;
    		else
    			this.parent = parent;
    	}
    }

    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public long getAccountId() {
        return accountId;
    }
    
    @Override
    public Date getRemoved() {
        return removed;
    }
    
    @Override
    public String getPath() {
    	return path;
    }
    
    @Override
    public void setPath(String path) {
    	this.path = path;
    }
    
    @Override
    public int getLevel() {
    	return level; 
    }
    
    public void setLevel(int level) {
    	this.level = level;
    }
    
    @Override
    public int getChildCount() {
    	return childCount; 
    }
    
    public void setChildCount(int count) {
    	childCount = count;
    }
    
    @Override
    public long getNextChildSeq() {
    	return nextChildSeq;
    }
    
    public void setNextChildSeq(long seq) {
    	nextChildSeq = seq;
    }
    
    @Override 
    public Domain.State getState() {
        return state;
    }
    
    @Override
    public void setState(Domain.State state) {
        this.state = state;
    }
    
    @Override
    public String toString() {
        return new StringBuilder("Domain:").append(id).append(path).toString();
    }

    @Override
    public String getNetworkDomain() {
        return networkDomain;
    }

    public void setNetworkDomain(String domainSuffix) {
        this.networkDomain = domainSuffix;
    }
    
    @Override
    public String getUuid() {
    	return this.uuid;
    }
    
    public void setUuid(String uuid) {
    	this.uuid = uuid;
    }
}

