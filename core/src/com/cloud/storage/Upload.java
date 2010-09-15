package com.cloud.storage;

public interface Upload {

	public static enum Status  {UNKNOWN, ABANDONED, UPLOADED, NOT_UPLOADED, UPLOAD_ERROR, UPLOAD_IN_PROGRESS}
	public static enum Type  {VOLUME, TEMPLATE, ISO}
}
