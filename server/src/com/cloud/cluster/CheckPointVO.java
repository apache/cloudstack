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
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.cloud.utils.db.GenericDao;

@Entity
@Table(name="stack_maid")
public class CheckPointVO {

	@Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id")
    private long id;

	@Column(name="msid")
	private long msid;
	
	@Column(name="thread_id")
	private long threadId;
	
	@Column(name="seq")
	private long seq;
	
	@Column(name="cleanup_delegate", length=128)
	private String delegate;
	
	@Column(name="cleanup_context", length=65535)
	private String context;
	
    @Column(name=GenericDao.CREATED_COLUMN)
	private Date created;
	
	public CheckPointVO() {
	}
	
	public CheckPointVO(long seq) {
	    this.seq = seq;
	}

	public long getId() {
		return id;
	}

	public long getMsid() {
		return msid;
	}

	public void setMsid(long msid) {
		this.msid = msid;
	}

	public long getThreadId() {
		return threadId;
	}

	public void setThreadId(long threadId) {
		this.threadId = threadId;
	}

	public long getSeq() {
		return seq;
	}

	public void setSeq(long seq) {
		this.seq = seq;
	}

	public String getDelegate() {
		return delegate;
	}

	public void setDelegate(String delegate) {
		this.delegate = delegate;
	}

	public String getContext() {
		return context;
	}

	public void setContext(String context) {
		this.context = context;
	}
	
	public Date getCreated() {
		return this.created;
	}
	
	public void setCreated(Date created) {
		this.created = created;
	}
	
	@Override
    public String toString() {
	    return new StringBuilder("Task[").append(id).append("-").append(context).append("-").append(delegate).append("]").toString();
	}
}
