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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.cloud.utils.db.GenericDao;

@Entity
@Table(name="domain")
public class DomainVO {
    public static final long ROOT_DOMAIN = 1L;
    
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id")
    private Long id = null;

    @Column(name="parent")
    private Long parent = null;

    @Column(name="name")
    private String name = null;

    @Column(name="owner")
    private Long owner = null;
    
    @Column(name="path")
    private String path = null;
    
    @Column(name="level")
    private Integer level = null;

    @Column(name=GenericDao.REMOVED_COLUMN)
    private Date removed;

    @Column(name="child_count")
    private int childCount = 0;

    @Column(name="next_child_seq")
    private long nextChildSeq = 1L;

    public DomainVO() {}

    public DomainVO(String name, Long owner, Long parentId) {
    	this.parent = parentId;
        this.name = name;
        this.owner = owner;
        this.path ="";
        this.level = 0;
    }

    public Long getId() {
        return id;
    }

    public Long getParent() {
        return parent;
    }
    
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

    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }

    public Long getOwner() {
        return owner;
    }
    
    public Date getRemoved() {
        return removed;
    }
    
    public String getPath() {
    	return path;
    }
    
    public void setPath(String path) {
    	this.path = path;
    }
    
    public Integer getLevel() {
    	return level; 
    }
    
    public void setLevel(Integer level) {
    	this.level = level;
    }
    
    public int getChildCount() {
    	return childCount; 
    }
    
    public void setChildCount(int count) {
    	childCount = count;
    }
    
    public long getNextChildSeq() {
    	return nextChildSeq;
    }
    
    public void setNextChildSeq(long seq) {
    	nextChildSeq = seq;
    }
}

