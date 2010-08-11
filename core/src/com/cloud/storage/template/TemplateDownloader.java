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
package com.cloud.storage.template;

/**
 * Download a template file given a URL
 * @author Chiradeep
 *
 */
public interface TemplateDownloader extends Runnable{
	

	/**
	 * Callback used to notify completion of download
	 * @author chiradeep
	 *
	 */
	public interface DownloadCompleteCallback {
		void downloadComplete( Status status);

	}

	public static enum Status  {UNKNOWN, NOT_STARTED, IN_PROGRESS, ABORTED, UNRECOVERABLE_ERROR, RECOVERABLE_ERROR, DOWNLOAD_FINISHED, POST_DOWNLOAD_FINISHED}

	public static long DEFAULT_MAX_TEMPLATE_SIZE_IN_BYTES = 50L*1024L*1024L*1024L;
	
	/**
	 * Initiate download, resuming a previous one if required
	 * @param resume resume if necessary
	 * @param callback completion callback to be called after download is complete
	 * @return bytes downloaded
	 */
	public long download(boolean resume, DownloadCompleteCallback callback);
	
	/**
	 * @return
	 */
	public boolean stopDownload();
	
	/**
	 * @return percent of file downloaded
	 */
	public int getDownloadPercent();

	/**
	 * Get the status of the download
	 * @return status of download
	 */
	public TemplateDownloader.Status getStatus();


	/**
	 * Get time taken to download so far
	 * @return time in seconds taken to download
	 */
	public long getDownloadTime();

	/**
	 * Get bytes downloaded
	 * @return bytes downloaded so far
	 */
	public long getDownloadedBytes();

	/**
	 * Get the error if any
	 * @return error string if any
	 */
	public String getDownloadError();

	/** Get local path of the downloaded file
	 * @return local path of the file downloaded
	 */
	public String getDownloadLocalPath();

	public void setStatus(TemplateDownloader.Status status);

	public void setDownloadError(String string);

	public void setResume(boolean resume);
	
	public boolean isInited();
	
	public long getMaxTemplateSizeInBytes();

}
