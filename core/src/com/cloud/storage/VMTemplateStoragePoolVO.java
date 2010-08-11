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
 * Join table for storage pools and templates
 * @author chiradeep
 *
 */
@Entity
@Table(name="template_spool_ref")
public class VMTemplateStoragePoolVO implements VMTemplateStorageResourceAssoc{
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	Long id;
	
	@Column(name="pool_id")
	private long poolId;
	
	@Column(name="template_id") long templateId;
	
	@Column(name=GenericDaoBase.CREATED_COLUMN) Date created = null;
	
	@Column(name="last_updated")
	@Temporal(value=TemporalType.TIMESTAMP) Date lastUpdated = null;
	
	@Column (name="download_pct") int downloadPercent;
	
	@Column (name="download_state")
	@Enumerated(EnumType.STRING) Status downloadState;
	
	@Column (name="local_path") String localDownloadPath;
	
	@Column (name="error_str") String errorString;
	
	@Column (name="job_id") String jobId;
	
	@Column (name="install_path") String installPath;
	
	@Column (name="template_size") long templateSize;
	
	@Column (name="marked_for_gc") boolean markedForGC;
    
	public String getInstallPath() {
		return installPath;
	}
	
	public long getTemplateSize() {
		return templateSize;
	}

	public long getPoolId() {
		return poolId;
	}

	public void setpoolId(long poolId) {
		this.poolId = poolId;
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

	public VMTemplateStoragePoolVO(long poolId, long templateId) {
		super();
		this.poolId = poolId;
		this.templateId = templateId;
		this.downloadState = Status.NOT_DOWNLOADED;
		this.markedForGC = false;
	}

	public VMTemplateStoragePoolVO(long poolId, long templateId, Date lastUpdated,
			int downloadPercent, Status downloadState,
			String localDownloadPath, String errorString, String jobId,
			String installPath, long templateSize) {
		super();
		this.poolId = poolId;
		this.templateId = templateId;
		this.lastUpdated = lastUpdated;
		this.downloadPercent = downloadPercent;
		this.downloadState = downloadState;
		this.localDownloadPath = localDownloadPath;
		this.errorString = errorString;
		this.jobId = jobId;
		this.installPath = installPath;
		this.templateSize = templateSize;
	}

	protected VMTemplateStoragePoolVO() {
		
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
	
	public void setTemplateSize(long templateSize) {
		this.templateSize = templateSize;
	}
	
	public boolean getMarkedForGC() {
		return markedForGC;
	}
	
	public void setMarkedForGC(boolean markedForGC) {
		this.markedForGC = markedForGC;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof VMTemplateStoragePoolVO) {
		   VMTemplateStoragePoolVO other = (VMTemplateStoragePoolVO)obj;
		   return (this.templateId==other.getTemplateId() && this.poolId==other.getPoolId());
		}
		return false;
	}

	@Override
	public int hashCode() {
		Long tid = new Long(templateId);
		Long hid = new Long(poolId);
		return tid.hashCode()+hid.hashCode();
	}
	

}
