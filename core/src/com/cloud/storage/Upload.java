package com.cloud.storage;

public interface Upload {

	public static enum Status  {UNKNOWN, ABANDONED, UPLOADED, NOT_UPLOADED, UPLOAD_ERROR, UPLOAD_IN_PROGRESS, NOT_COPIED, COPY_IN_PROGRESS, COPY_ERROR, COPY_COMPLETE}
	public static enum Type  {VOLUME, TEMPLATE, ISO}
}
