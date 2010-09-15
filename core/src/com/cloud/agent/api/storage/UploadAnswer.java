package com.cloud.agent.api.storage;

import java.io.File;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.storage.UploadVO;

public class UploadAnswer extends Answer {

	
	private String jobId;
	private int uploadPct;
	private String errorString;
	private UploadVO.Status uploadStatus;
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
	
	public UploadVO.Status getUploadStatus() {
		return uploadStatus;
	}
	
	public String getUploadPath() {
		return uploadPath;
	}
	protected UploadAnswer() {
		
	}
	
	public String getJobId() {
		return jobId;
	}
	public void setJobId(String jobId) {
		this.jobId = jobId;
	}
	
	public UploadAnswer(String jobId, int uploadPct, String errorString,
			UploadVO.Status uploadStatus, String fileSystemPath, String installPath, long templateSize) {
		super();
		this.jobId = jobId;
		this.uploadPct = uploadPct;
		this.errorString = errorString;
		this.uploadStatus = uploadStatus;
		this.uploadPath = fileSystemPath;
		this.installPath = fixPath(installPath);
		this.templateSize = templateSize;
	}

   public UploadAnswer(String jobId, int uploadPct, Command command,
		   UploadVO.Status uploadStatus, String fileSystemPath, String installPath) {
	    super(command);
        this.jobId = jobId;
        this.uploadPct = uploadPct;
        this.uploadStatus = uploadStatus;
        this.uploadPath = fileSystemPath;
        this.installPath = installPath;
    }
		
	private static String fixPath(String path){
		if (path == null)
			return path;
		if (path.startsWith(File.separator)) {
			path=path.substring(File.separator.length());
		}
		if (path.endsWith(File.separator)) {
			path=path.substring(0, path.length()-File.separator.length());
		}
		return path;
	}
	
	public void setUploadStatus(UploadVO.Status uploadStatus) {
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
