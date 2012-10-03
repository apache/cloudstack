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

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

@Entity
@Table(name="shost")
public class SHostVO implements SHost{
	private static final long serialVersionUID = 213346565810468018L;
	
	public static final int STORAGE_HOST_TYPE_LOCAL = 0;
	public static final int STORAGE_HOST_TYPE_NFS = 1;

	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	@Column(name="ID")
	private Long id;
	
	@Column(name="Host")
	private String host;
	
	@Column(name="HostType")
	private int hostType;
	
	@Column(name="MHostID")
	private long mhostid;
	
	@Column(name="ExportRoot")
	private String exportRoot;
	
	@Column(name="UserOnHost")
	private String userOnHost;
	
	@Column(name="UserPassword")
	private String userPassword;
	
	@Transient
	private MHostVO mhost;
	
	@Transient
	private Set<SBucket> buckets = new HashSet<SBucket>();
	
	@Transient
	private Set<MHostMountVO> mounts = new HashSet<MHostMountVO>();
	
	public SHostVO() {
	}
	
	public Long getId() {
		return id;
	}

	private void setId(Long id) {
		this.id = id;
	}
	
	public String getHost() {
		return host;
	}
	
	public void setHost(String host) {
		this.host = host;
	}
	
	public int getHostType() {
		return hostType;
	}

	public void setHostType(int hostType) {
		this.hostType = hostType;
	}

	public long getMhostid() {
	    return mhostid;
	}

	public void setMhostid(long mhostid) {
	    this.mhostid = mhostid;
	}

	public String getExportRoot() {
		return exportRoot;
	}

	public void setExportRoot(String exportRoot) {
		this.exportRoot = exportRoot;
	}

	public String getUserOnHost() {
		return userOnHost;
	}
	
	public void setUserOnHost(String userOnHost) {
		this.userOnHost = userOnHost;
	}
	
	public String getUserPassword() {
		return userPassword;
	}
	
	public void setUserPassword(String userPassword) {
		this.userPassword = userPassword;
	}
	
	public MHostVO getMhost() {
		return mhost;
	}

	public void setMhost(MHostVO mhost) {
		this.mhost = mhost;
	}

	public Set<SBucket> getBuckets() {
		return buckets;
	}

	public void setBuckets(Set<SBucket> buckets) {
		this.buckets = buckets;
	}
	
	public Set<MHostMountVO> getMounts() {
		return mounts;
	}

	public void setMounts(Set<MHostMountVO> mounts) {
		this.mounts = mounts;
	}
}
