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

/**
 * @author Kelven Yang
 */
public class MHostMount implements Serializable {
	private static final long serialVersionUID = -1119494563131099642L;

	private Long id;
	
	private MHost mhost;
	private SHost shost;
	
	private String mountPath;
	private Date lastMountTime;
	
	public MHostMount() {
	}
	
	public Long getId() {
		return id;
	}
	
	private void setId(Long id) {
		this.id = id;
	}
	
	public MHost getMhost() {
		return mhost;
	}
	
	public void setMhost(MHost mhost) {
		this.mhost = mhost;
	}
	
	public SHost getShost() {
		return shost;
	}
	
	public void setShost(SHost shost) {
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
		
		if(!(other instanceof MHostMount))
			return false;
		
		return getMhost().equals(((MHostMount)other).getMhost()) &&
			getShost().equals(((MHostMount)other).getShost());
	}
	
	@Override
	public int hashCode() {
		return getMhost().hashCode() ^ getShost().hashCode();
	}
}
