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

package com.cloud.async;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="sync_queue_item")
public class SyncQueueItemVO {

	@Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id")
    private Long id = null;
	
    @Column(name="queue_id")
	private Long queueId;
	
    @Column(name="content_type")
	private String contentType;
	
    @Column(name="content_id")
	private Long contentId;
    
    @Column(name="queue_proc_msid")
    private Long lastProcessMsid;

    @Column(name="queue_proc_number")
    private Long lastProcessNumber;
    
	@Column(name="created")
	private Date created;
    
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getQueueId() {
		return queueId;
	}

	public void setQueueId(Long queueId) {
		this.queueId = queueId;
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public Long getContentId() {
		return contentId;
	}

	public void setContentId(Long contentId) {
		this.contentId = contentId;
	}

	public Long getLastProcessMsid() {
		return lastProcessMsid;
	}

	public void setLastProcessMsid(Long lastProcessMsid) {
		this.lastProcessMsid = lastProcessMsid;
	}
	
    public Long getLastProcessNumber() {
		return lastProcessNumber;
	}

	public void setLastProcessNumber(Long lastProcessNumber) {
		this.lastProcessNumber = lastProcessNumber;
	}

	public Date getCreated() {
		return created;
	}

	public void setCreated(Date created) {
		this.created = created;
	}
	
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("SyncQueueItemVO {id:").append(getId()).append(", queueId: ").append(getQueueId());
		sb.append(", contentType: ").append(getContentType());
		sb.append(", contentId: ").append(getContentId());
		sb.append(", lastProcessMsid: ").append(getLastProcessMsid());
		sb.append(", lastprocessNumber: ").append(getLastProcessNumber());
		sb.append(", created: ").append(getCreated());
		sb.append("}");
		return sb.toString();
	}
}
