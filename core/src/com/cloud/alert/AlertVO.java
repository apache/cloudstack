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

package com.cloud.alert;

import java.util.Date;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import com.cloud.api.Identity;
import com.cloud.utils.db.GenericDao;

@Entity
@Table(name="alert")
public class AlertVO implements Alert, Identity {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id")
    private long id;

    @Column(name="type")
    private short type;
    
    @Column(name="cluster_id")
    private Long clusterId = null;
    
    @Column(name="pod_id")
    private Long podId = null;

    @Column(name="data_center_id")
    private long dataCenterId = 0;

    @Column(name="subject", length=999)
    private String subject;

    @Column(name="sent_count")
    private int sentCount = 0;

    @Column(name=GenericDao.CREATED_COLUMN)
    private Date createdDate;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="last_sent", updatable=true, nullable=true)
    private Date lastSent;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="resolved", updatable=true, nullable=true)
    private Date resolved;
    
    @Column(name="uuid")
    private String uuid;

    public AlertVO() {
    	this.uuid = UUID.randomUUID().toString();
    }
    public AlertVO(Long id) {
        this.id = id;
    	this.uuid = UUID.randomUUID().toString();
    }

    @Override
    public long getId() {
        return id;
    }
    @Override
    public short getType() {
        return type;
    }

    public void setType(short type) {
        this.type = type;
    }

    @Override
    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public Long getClusterId() {
		return clusterId;
	}
	public void setClusterId(Long clusterId) {
		this.clusterId = clusterId;
	}
	@Override
    public Long getPodId() {
        return podId;
    }

    public void setPodId(Long podId) {
        this.podId = podId;
    }

    @Override
    public long getDataCenterId() {
        return dataCenterId;
    }

    public void setDataCenterId(long dataCenterId) {
        this.dataCenterId = dataCenterId;
    }

    @Override
    public int getSentCount() {
        return sentCount;
    }

    public void setSentCount(int sentCount) {
        this.sentCount = sentCount;
    }

    @Override
    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    @Override
    public Date getLastSent() {
        return lastSent;
    }

    public void setLastSent(Date lastSent) {
        this.lastSent = lastSent;
    }

    @Override
    public Date getResolved() {
        return resolved;
    }

    public void setResolved(Date resolved) {
        this.resolved = resolved;
    }
    
    @Override
    public String getUuid() {
    	return this.uuid;
    }
    
    public void setUuid(String uuid) {
    	this.uuid = uuid;
    }
}
