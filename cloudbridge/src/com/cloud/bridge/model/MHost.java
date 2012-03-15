/*
 * Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cloud.bridge.model;

import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Kelven Yang
 */
public class MHost implements Serializable {
	private static final long serialVersionUID = 4848254624679753930L;

	private Long id;
	
	private String hostKey;
	private String host;
	private String version;
	private Date lastHeartbeatTime;

	private Set<SHost> localSHosts = new HashSet<SHost>();
	private Set<MHostMount> mounts = new HashSet<MHostMount>();

	public MHost() {
	}
	
	public Long getId() {
		return id;
	}
	
	private void setId(Long id) {
		this.id = id;
	}
	
	public String getHostKey() {
		return hostKey;
	}
	
	public void setHostKey(String hostKey) {
		this.hostKey = hostKey;
	}
	
	public String getHost() {
		return host;
	}
	
	public void setHost(String host) {
		this.host = host;
	}
	
	public String getVersion() {
		return version;
	}
	
	public void setVersion(String version) {
		this.version = version;
	}
	
	public Date getLastHeartbeatTime() {
		return lastHeartbeatTime;
	}
	
	public void setLastHeartbeatTime(Date lastHeartbeatTime) {
		this.lastHeartbeatTime = lastHeartbeatTime;
	}
	
	public Set<SHost> getLocalSHosts() {
		return localSHosts;
	}

	public void setLocalSHosts(Set<SHost> localSHosts) {
		this.localSHosts = localSHosts;
	}

	public Set<MHostMount> getMounts() {
		return mounts;
	}

	public void setMounts(Set<MHostMount> mounts) {
		this.mounts = mounts;
	}
	
	@Override
	public boolean equals(Object other) {
		if(this == other)
			return true;
		
		if(!(other instanceof MHost))
			return false;

		return hostKey == ((MHost)other).getHostKey();
	}
	
	@Override
	public int hashCode() {
		return hostKey.hashCode();
	}
}
