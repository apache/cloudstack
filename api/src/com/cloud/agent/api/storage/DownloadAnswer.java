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
package com.cloud.agent.api.storage;

import java.io.File;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.storage.VMTemplateStorageResourceAssoc;
import com.cloud.storage.VMTemplateStorageResourceAssoc.Status;

public class DownloadAnswer extends Answer  {
	private String jobId;
	private int downloadPct;
	private String errorString;
	private VMTemplateStorageResourceAssoc.Status downloadStatus;
	private String downloadPath;
	private String installPath;
	private long templateSize = 0L;
	private long templatePhySicalSize = 0L;
	private String checkSum;
	
	public String getCheckSum() {
		return checkSum;
	}
	
	public int getDownloadPct() {
		return downloadPct;
	}
	public String getErrorString() {
		return errorString;
	}
	
	public String getDownloadStatusString() {
		return downloadStatus.toString();
	}
	
	public VMTemplateStorageResourceAssoc.Status getDownloadStatus() {
		return downloadStatus;
	}
	
	public String getDownloadPath() {
		return downloadPath;
	}
	protected DownloadAnswer() {
		
	}
	
	public String getJobId() {
		return jobId;
	}
	public void setJobId(String jobId) {
		this.jobId = jobId;
	}
	
    public DownloadAnswer(String errorString, Status status) {
        super();
        this.downloadPct = 0;
        this.errorString = errorString;
        this.downloadStatus = status;
        this.details = errorString;
    }
	
	public DownloadAnswer(String jobId, int downloadPct, String errorString,
			Status downloadStatus, String fileSystemPath, String installPath, long templateSize, long templatePhySicalSize, String checkSum) {
		super();
		this.jobId = jobId;
		this.downloadPct = downloadPct;
		this.errorString = errorString;
		this.details = errorString;
		this.downloadStatus = downloadStatus;
		this.downloadPath = fileSystemPath;
		this.installPath = fixPath(installPath);
		this.templateSize = templateSize;
		this.templatePhySicalSize = templatePhySicalSize;
		this.checkSum = checkSum;
	}
	
   public DownloadAnswer(String jobId, int downloadPct, Command command,
            Status downloadStatus, String fileSystemPath, String installPath) {
        super(command);
        this.jobId = jobId;
        this.downloadPct = downloadPct;
        this.downloadStatus = downloadStatus;
        this.downloadPath = fileSystemPath;
        this.installPath = installPath;
    }
		
	private static String fixPath(String path){
		if (path == null) {
            return path;
        }
		if (path.startsWith(File.separator)) {
			path=path.substring(File.separator.length());
		}
		if (path.endsWith(File.separator)) {
			path=path.substring(0, path.length()-File.separator.length());
		}
		return path;
	}
	
	public void setDownloadStatus(VMTemplateStorageResourceAssoc.Status downloadStatus) {
		this.downloadStatus = downloadStatus;
	}
	
	public String getInstallPath() {
		return installPath;
	}
	public void setInstallPath(String installPath) {
		this.installPath = fixPath(installPath);
	}

	public void setTemplateSize(long templateSize) {
		this.templateSize = templateSize;
	}
	
	public Long getTemplateSize() {
		return templateSize;
	}
    public void setTemplatePhySicalSize(long templatePhySicalSize) {
        this.templatePhySicalSize = templatePhySicalSize;
    }
    public long getTemplatePhySicalSize() {
        return templatePhySicalSize;
    }
	
}
