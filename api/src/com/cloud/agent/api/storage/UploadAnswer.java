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
import com.cloud.storage.Upload;

public class UploadAnswer extends Answer {

	
	private String jobId;
	private int uploadPct;
	private String errorString;
	private Upload.Status uploadStatus;
	private String uploadPath;
	private String installPath;
	public Long templateSize = 0L;
	
	public int getUploadPct() {
		return uploadPct;
	}
	public String getErrorString() {
		return errorString;
	}
	
	public String getUploadStatusString() {
		return uploadStatus.toString();
	}
	
	public Upload.Status getUploadStatus() {
		return uploadStatus;
	}
	
	public String getUploadPath() {
		return uploadPath;
	}
	protected UploadAnswer() {
		
	}
		
	public void setErrorString(String errorString) {
        this.errorString = errorString;
    }
    public String getJobId() {
		return jobId;
	}
	public void setJobId(String jobId) {
		this.jobId = jobId;
	}
	
	public UploadAnswer(String jobId, int uploadPct, String errorString,
			Upload.Status uploadStatus, String fileSystemPath, String installPath, long templateSize) {
		super();
		this.jobId = jobId;
		this.uploadPct = uploadPct;
		this.errorString = errorString;
		this.details = errorString;
		this.uploadStatus = uploadStatus;
		this.uploadPath = fileSystemPath;
		this.installPath = fixPath(installPath);
		this.templateSize = templateSize;
	}

   public UploadAnswer(String jobId, int uploadPct, Command command,
		   Upload.Status uploadStatus, String fileSystemPath, String installPath) {
	    super(command);
        this.jobId = jobId;
        this.uploadPct = uploadPct;
        this.uploadStatus = uploadStatus;
        this.uploadPath = fileSystemPath;
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
	
	public void setUploadStatus(Upload.Status uploadStatus) {
		this.uploadStatus = uploadStatus;
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
	
}
