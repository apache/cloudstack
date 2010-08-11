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

package com.cloud.event;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SecondaryTable;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.PrimaryKeyJoinColumn;

import com.cloud.utils.db.GenericDao;

@Entity
@Table(name="event")
@SecondaryTable(name="account",
        pkJoinColumns={@PrimaryKeyJoinColumn(name="account_id", referencedColumnName="id")})
public class EventVO {
	@Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id")
	private Long id = (long)-1;

	@Column(name="type")
	private String type;
	
	@Enumerated(value=EnumType.STRING)
	@Column(name="state")
    private EventState state = EventState.Completed;

	@Column(name="description")
	private String description;

	@Column(name=GenericDao.CREATED_COLUMN)
	private Date createDate;

    @Column(name="user_id")
    private long userId;

	@Column(name="account_id")
	private long accountId;

    @Column(name="domain_id", table="account", insertable=false, updatable=false)
    private long domainId;

	@Column(name="account_name", table="account", insertable=false, updatable=false)
	private String accountName;

	@Column(name="removed", table="account", insertable=false, updatable=false)
	private Date removed;

	@Column(name="level")
	private String level = LEVEL_INFO;
	
	@Column(name="start_id")
    private long startId;

	@Column(name="parameters")
	private String parameters;

	@Transient
	private int totalSize;

	public static final String LEVEL_INFO = "INFO";
	public static final String LEVEL_WARN = "WARN";
	public static final String LEVEL_ERROR = "ERROR";
	
	public EventVO() {
	}
	
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public EventState getState() {
        return state;
    }

    public void setState(EventState state) {
        this.state = state;
    }

	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public Date getCreateDate() {
		return createDate;
	}
	public void setCreatedDate(Date createdDate) {
	    createDate = createdDate;
	}
	public long getUserId() {
		return userId;
	}
	public void setUserId(long userId) {
		this.userId = userId;
	}
    public long getAccountId() {
        return accountId;
    }
    public void setAccountId(long accountId) {
        this.accountId = accountId;
    }
    public long getDomainId() {
        return domainId;
    }
    public void setDomainId(long domainId) {
    	this.domainId = domainId;
    }
	public String getAccountName() {
		return accountName;
	}
	public void setAccountName(String accountName) {
		this.accountName = accountName;
	}
	public int getTotalSize() {
		return totalSize;
	}
	public void setTotalSize(int totalSize) {
		this.totalSize = totalSize;
	}
	public String getLevel() {
		return level;
	}
	public void setLevel(String level) {
		this.level = level;
	}
	public long getStartId() {
        return startId;
    }

    public void setStartId(long startId) {
        this.startId = startId;
    }

	public String getParameters() {
		return parameters;
	}
	public void setParameters(String parameters) {
		this.parameters = parameters;
	}
}
