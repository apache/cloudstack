package com.cloud.bridge.service.core.s3;

import java.util.Calendar;

public class S3CopyObjectResponse  extends S3Response  {
	// -> 2 versions are important here: 
	// (1) copyVersion: the version of the object's copy
	// (2) putVersion: the version assigned to the copy after it is put
	protected String copyVersion;
	protected String putVersion;
	protected String ETag;
	protected Calendar lastModified;
	
	public S3CopyObjectResponse() {
		super();
		copyVersion = null;
		putVersion  = null;
	}

	public String getETag() {
		return ETag;
	}

	public void setETag(String eTag) {
		this.ETag = eTag;
	}

	public Calendar getLastModified() {
		return lastModified;
	}

	public void setLastModified(Calendar lastModified) {
		this.lastModified = lastModified;
	}
	
	public String getCopyVersion() {
		return copyVersion;
	}
	
	public void setCopyVersion(String version) {
		copyVersion = version;
	}

	public String getPutVersion() {
		return putVersion;
	}
	
	public void setPutVersion(String version) {
		putVersion = version;
	}
}