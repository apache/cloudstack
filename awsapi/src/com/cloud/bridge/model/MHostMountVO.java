// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.bridge.model;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

@Entity
@Table(name="mhost_mount")
public class MHostMountVO implements Serializable {
	private static final long serialVersionUID = -1119494563131099642L;
	
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	@Column(name="ID")
	private Long id;
	
	@Column(name="MHostID")
	private long mHostID;
	
	@Column(name="SHostID")
	private long sHostID;
	
	@Transient
	private MHostVO mhost;

	@Transient
	private SHostVO shost;
	
	@Column(name="MountPath")
	private String mountPath;
	
	@Column(name="LastMountTime")
	@Temporal(value=TemporalType.TIMESTAMP)
	private Date lastMountTime;
	
	public MHostMountVO() {
	}
	
	public Long getId() {
		return id;
	}
	
	private void setId(Long id) {
		this.id = id;
	}
	
	public long getmHostID() {
	    return mHostID;
	}

	public void setmHostID(long mHostID) {
	    this.mHostID = mHostID;
	}

	public long getsHostID() {
	    return sHostID;
	}

	public void setsHostID(long sHostID) {
	    this.sHostID = sHostID;
	}

	public MHostVO getMhost() {
		return mhost;
	}
	
	public void setMhost(MHostVO mhost) {
		this.mhost = mhost;
	}
	
	public SHostVO getShost() {
		return shost;
	}
	
	public void setShost(SHostVO shost) {
		this.shost = shost;
	}
	
	public String getMountPath() {
		return mountPath;
	}
	
	public void setMountPath(String mountPath) {
		this.mountPath = mountPath;
	}
	
	public Date getLastMountTime() {
		return lastMountTime;
	}
	
	public void setLastMountTime(Date lastMountTime) {
		this.lastMountTime = lastMountTime;
	}
	
	@Override
	public boolean equals(Object other) {
		if(this == other)
			return true;
		
		if(!(other instanceof MHostMountVO))
			return false;
		
		return getMhost().equals(((MHostMountVO)other).getMhost()) &&
			getShost().equals(((MHostMountVO)other).getShost());
	}
	
	@Override
	public int hashCode() {
		return getMhost().hashCode() ^ getShost().hashCode();
	}
}
