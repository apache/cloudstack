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
package com.cloud.storage;

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

import org.apache.cloudstack.api.InternalIdentity;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObjectInStore;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine;

import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.VMTemplateStorageResourceAssoc.Status;
import com.cloud.utils.db.GenericDaoBase;

/**
 * Join table for storage hosts and volumes
 *
 */
@Entity
@Table(name="volume_host_ref")
public class VolumeHostVO implements InternalIdentity, DataObjectInStore {
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	Long id;
	
	@Column(name="host_id")
	private long hostId;
	
	@Column(name="volume_id")
	private long volumeId;
	
	@Column(name="zone_id")
	private long zoneId;
	
	@Column(name=GenericDaoBase.CREATED_COLUMN)
	private Date created = null;
	
	@Column(name="last_updated")
	@Temporal(value=TemporalType.TIMESTAMP)
	private Date lastUpdated = null;
	
	@Column (name="download_pct")
	private int downloadPercent;
	
	@Column (name="size")
	private long size;
	
	@Column (name="physical_size")
	private long physicalSize;
	
	@Column (name="download_state")
	@Enumerated(EnumType.STRING)
	private Status downloadState;
	
    @Column(name="checksum")
    private String checksum;
	
	@Column (name="local_path")
	private String localDownloadPath;
	
	@Column (name="error_str")
	private String errorString;
	
	@Column (name="job_id")
	private String jobId;	
	
	@Column (name="install_path")
    private String installPath;
	
	@Column (name="url")
	private String downloadUrl;
	
	@Column(name="format")
    private Storage.ImageFormat format;
    
    @Column(name="destroyed")
    boolean destroyed = false;
    
    @Column(name="update_count", updatable = true, nullable=false)
    protected long updatedCount;
    
    @Column(name = "updated")
    @Temporal(value = TemporalType.TIMESTAMP)
    Date updated;
    
    @Column(name = "state")
    @Enumerated(EnumType.STRING)
    ObjectInDataStoreStateMachine.State state;
	
    public String getInstallPath() {
		return installPath;
	}

	public long getHostId() {
		return hostId;
	}

	public void setHostId(long hostId) {
		this.hostId = hostId;
	}

	
    public long getVolumeId() {
		return volumeId;
	}

	
    public void setVolumeId(long volumeId) {
		this.volumeId = volumeId;
	}

	
    public long getZoneId() {
		return zoneId;
	}

	public void setZoneId(long zoneId) {
		this.zoneId = zoneId;
	}

	public int getDownloadPercent() {
		return downloadPercent;
	}

	
    public void setDownloadPercent(int downloadPercent) {
		this.downloadPercent = downloadPercent;
	}

	
    public void setDownloadState(Status downloadState) {
		this.downloadState = downloadState;
	}

	
    public long getId() {
		return id;
	}

	
    public Date getCreated() {
		return created;
	}

	
    public Date getLastUpdated() {
		return lastUpdated;
	}
	
	
    public void setLastUpdated(Date date) {
	    lastUpdated = date;
	}
	
	
    public void setInstallPath(String installPath) {
	    this.installPath = installPath;
	}

	
    public Status getDownloadState() {
		return downloadState;
	}

	public String getChecksum() {
		return checksum;
	}

	public void setChecksum(String checksum) {
		this.checksum = checksum;
	}

	public VolumeHostVO(long hostId, long volumeId) {
		super();
		this.hostId = hostId;
		this.volumeId = volumeId;
		this.state = ObjectInDataStoreStateMachine.State.Allocated;
	}

	public VolumeHostVO(long hostId, long volumeId, long zoneId, Date lastUpdated,
			int downloadPercent, Status downloadState,
			String localDownloadPath, String errorString, String jobId,
			String installPath, String downloadUrl, String checksum, ImageFormat format) {
		//super();
		this.hostId = hostId;
		this.volumeId = volumeId;
		this.zoneId = zoneId;
		this.lastUpdated = lastUpdated;
		this.downloadPercent = downloadPercent;
		this.downloadState = downloadState;
		this.localDownloadPath = localDownloadPath;
		this.errorString = errorString;
		this.jobId = jobId;
		this.installPath = installPath;
		this.setDownloadUrl(downloadUrl);
		this.checksum = checksum;
		this.format = format;
	}

	protected VolumeHostVO() {
		
	}

	
    public void setLocalDownloadPath(String localPath) {
		this.localDownloadPath = localPath;
	}

	
    public String getLocalDownloadPath() {
		return localDownloadPath;
	}

	
    public void setErrorString(String errorString) {
		this.errorString = errorString;
	}

	
    public String getErrorString() {
		return errorString;
	}

	
    public void setJobId(String jobId) {
		this.jobId = jobId;
	}

	
    public String getJobId() {
		return jobId;
	}

	
	public boolean equals(Object obj) {
		if (obj instanceof VolumeHostVO) {
			VolumeHostVO other = (VolumeHostVO)obj;
			return (this.volumeId==other.getVolumeId() && this.hostId==other.getHostId());		   
		}
		return false;
	}

	
	public int hashCode() {
		Long tid = new Long(volumeId);
		Long hid = new Long(hostId);
		return tid.hashCode()+hid.hashCode();
	}

    public void setSize(long size) {
        this.size = size;
    }

    public long getSize() {
        return size;
    }
	
    
    public void setPhysicalSize(long physicalSize) {
        this.physicalSize = physicalSize;
    }

    public long getPhysicalSize() {
        return physicalSize;
    }

    public void setDestroyed(boolean destroyed) {
    	this.destroyed = destroyed;
    }

    public boolean getDestroyed() {
    	return destroyed;
    }

	public void setDownloadUrl(String downloadUrl) {
		this.downloadUrl = downloadUrl;
	}

	public String getDownloadUrl() {
		return downloadUrl;
	}	
	
    public Storage.ImageFormat getFormat() {
		return format;
	}

	public void setFormat(Storage.ImageFormat format) {
		this.format = format;
	}

	public long getVolumeSize() {
	    return -1;
	}
	
	
    public String toString() {
	    return new StringBuilder("VolumeHost[").append(id).append("-").append(volumeId).append("-").append(hostId).append(installPath).append("]").toString();
	}
    
    public long getUpdatedCount() {
        return this.updatedCount;
    }
    
    public void incrUpdatedCount() {
        this.updatedCount++;
    }

    public void decrUpdatedCount() {
        this.updatedCount--;
    }
    
    public Date getUpdated() {
        return updated;
    }

    @Override
    public ObjectInDataStoreStateMachine.State getState() {
        // TODO Auto-generated method stub
        return this.state;
    }

}
