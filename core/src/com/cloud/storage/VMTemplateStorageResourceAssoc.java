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

/**
 * @author chiradeep
 *
 */
public interface VMTemplateStorageResourceAssoc {
	public static enum Status  {UNKNOWN, DOWNLOAD_ERROR, NOT_DOWNLOADED, DOWNLOAD_IN_PROGRESS, DOWNLOADED, ABANDONED}

	public String getInstallPath();

	public long getTemplateId();

	public void setTemplateId(long templateId);

	public int getDownloadPercent();

	public void setDownloadPercent(int downloadPercent);

	public void setDownloadState(Status downloadState);

	public Long getId();

	public Date getCreated();

	public Date getLastUpdated();

	public void setLastUpdated(Date date);

	public void setInstallPath(String installPath);

	public Status getDownloadState();

	public void setLocalDownloadPath(String localPath);

	public String getLocalDownloadPath();

	public void setErrorString(String errorString);

	public String getErrorString();

	public void setJobId(String jobId);

	public String getJobId();;

}
