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

package com.cloud.cluster;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import com.cloud.utils.db.GenericDao;

@Entity
@Table(name="mshost")
public class ManagementServerHostVO implements ManagementServerHost{

	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	@Column(name="id")
	private long id;

	@Column(name="msid", updatable=true, nullable=false)
	private long msid;
	
	@Column(name="runid", updatable=true, nullable=false)
	private long runid;

	@Column(name="name", updatable=true, nullable=true)
	private String name;
	
    @Column(name="state", updatable = true, nullable=false)
    @Enumerated(value=EnumType.STRING)
	private ManagementServerHost.State state;
	
	@Column(name="version", updatable=true, nullable=true)
	private String version;
	
	@Column(name="service_ip", updatable=true, nullable=false)
	private String serviceIP;
	
	@Column(name="service_port", updatable=true, nullable=false)
	private int servicePort;
	
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="last_update", updatable=true, nullable=true)
    private Date lastUpdateTime;
    
    @Column(name=GenericDao.REMOVED_COLUMN)
    private Date removed;
    
	@Column(name="alert_count", updatable=true, nullable=false)
	private int alertCount;

    public ManagementServerHostVO() {
    }
    
    public ManagementServerHostVO(long msid, long runid, String serviceIP, int servicePort, Date updateTime) {
    	this.msid = msid;
    	this.runid = runid;
    	this.serviceIP = serviceIP;
    	this.servicePort = servicePort;
    	this.lastUpdateTime = updateTime;
    }
    
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}
	
	public long getRunid() {
		return runid;
	}
	
	public void setRunid(long runid) {
		this.runid = runid;
	}

	@Override
	public long getMsid() {
		return msid;
	}

	public void setMsid(long msid) {
		this.msid = msid;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	@Override
	public ManagementServerHost.State getState() {
		return this.state;
	}
	
	public void setState(ManagementServerHost.State state) {
		this.state = state;
	}
	
	@Override
	public String getVersion() {
		return version;
	}
	
	public void setVersion(String version) {
		this.version = version;
	}
	
	public String getServiceIP() {
		return serviceIP;
	}

	public void setServiceIP(String serviceIP) {
		this.serviceIP = serviceIP;
	}

	public int getServicePort() {
		return servicePort;
	}

	public void setServicePort(int servicePort) {
		this.servicePort = servicePort;
	}

	public Date getLastUpdateTime() {
		return lastUpdateTime;
	}

	public void setLastUpdateTime(Date lastUpdateTime) {
		this.lastUpdateTime = lastUpdateTime;
	}
	
	public Date getRemoved() {
		return removed;
	}
	
	public void setRemoved(Date removedTime) {
		removed = removedTime;
	}
	
	public int getAlertCount() {
		return alertCount; 
	}
	
	public void setAlertCount(int count) {
		alertCount = count;
	}
}
