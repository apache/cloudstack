package com.cloud.storage;

public interface Upload {

	public static enum Status  {UNKNOWN, ABANDONED, UPLOADED, NOT_UPLOADED, UPLOAD_ERROR, UPLOAD_IN_PROGRESS, NOT_COPIED, COPY_IN_PROGRESS, COPY_ERROR, COPY_COMPLETE, DOWNLOAD_URL_CREATED, DOWNLOAD_URL_NOT_CREATED, ERROR}
	public static enum Type    {VOLUME, TEMPLATE, ISO}
	public static enum Mode    {FTP_UPLOAD, HTTP_DOWNLOAD}
}
