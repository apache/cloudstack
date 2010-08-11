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

import com.cloud.utils.db.GenericDaoBase;

/**
 * Join table for storage hosts and templates
 * @author chiradeep
 *
 */
@Entity
@Table(name="template_host_ref")
public class VMTemplateHostVO implements VMTemplateStorageResourceAssoc {
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	Long id;
	
	@Column(name="host_id")
	private long hostId;
	
	@Column(name="template_id")
	private long templateId;
	
	@Column(name=GenericDaoBase.CREATED_COLUMN)
	private Date created = null;
	
	@Column(name="last_updated")
	@Temporal(value=TemporalType.TIMESTAMP)
	private Date lastUpdated = null;
	
	@Column (name="download_pct")
	private int downloadPercent;
	
	@Column (name="size")
	private long size;
	
	@Column (name="download_state")
	@Enumerated(EnumType.STRING)
	private Status downloadState;
	
	@Column (name="local_path")
	private String localDownloadPath;
	
	@Column (name="error_str")
	private String errorString;
	
	@Column (name="job_id")
	private String jobId;
	
	@Column (name="pool_id")
	private Long poolId;
	
	@Column (name="install_path")
    private String installPath;
	
	@Column (name="url")
	private String downloadUrl;
	
	@Column(name="is_copy")
	private boolean isCopy = false;
    
    @Column(name="destroyed")
    boolean destroyed = false;
    
	public String getInstallPath() {
		return installPath;
	}

	public long getHostId() {
		return hostId;
	}

	public void setHostId(long hostId) {
		this.hostId = hostId;
	}

	public long getTemplateId() {
		return templateId;
	}

	public void setTemplateId(long templateId) {
		this.templateId = templateId;
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

	public Long getId() {
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

	public VMTemplateHostVO(long hostId, long templateId) {
		super();
		this.hostId = hostId;
		this.templateId = templateId;
	}

	public VMTemplateHostVO(long hostId, long templateId, Date lastUpdated,
			int downloadPercent, Status downloadState,
			String localDownloadPath, String errorString, String jobId,
			String installPath, String downloadUrl) {
		super();
		this.hostId = hostId;
		this.templateId = templateId;
		this.lastUpdated = lastUpdated;
		this.downloadPercent = downloadPercent;
		this.downloadState = downloadState;
		this.localDownloadPath = localDownloadPath;
		this.errorString = errorString;
		this.jobId = jobId;
		this.installPath = installPath;
		this.setDownloadUrl(downloadUrl);
	}

	protected VMTemplateHostVO() {
		
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

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof VMTemplateHostVO) {
		   VMTemplateHostVO other = (VMTemplateHostVO)obj;
		   if (poolId == null && other.getPoolId() == null) {
			   return (this.templateId==other.getTemplateId() && this.hostId==other.getHostId());
		   } else if (poolId == null && other.getPoolId() != null) {
			   return false;
		   } else if (poolId != null && other.getPoolId() == null) {
			   return false;
		   } else {
			   return (this.templateId==other.getTemplateId() && this.hostId==other.getHostId() && poolId.longValue() == other.getPoolId().longValue());
		   }
		}
		return false;
	}

	@Override
	public int hashCode() {
		Long tid = new Long(templateId);
		Long hid = new Long(hostId);
		return tid.hashCode()+hid.hashCode() + ((poolId != null)?poolId.hashCode():0);
	}

	public void setPoolId(Long poolId) {
		this.poolId = poolId;
	}

	public Long getPoolId() {
		return poolId;
	}

    public void setSize(long size) {
        this.size = size;
    }

    public long getSize() {
        return size;
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

	public void setCopy(boolean isCopy) {
		this.isCopy = isCopy;
	}

	public boolean isCopy() {
		return isCopy;
	}
}
