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

import com.cloud.storage.template.TemplateUploader.UploadCompleteCallback;
import com.cloud.storage.template.TemplateUploader.Status;

public interface TemplateUploader extends Runnable{

	/**
	 * Callback used to notify completion of upload
	 * @author nitin
	 *
	 */
	public interface UploadCompleteCallback {
		void uploadComplete( Status status);

	}

	public static enum Status  {UNKNOWN, NOT_STARTED, IN_PROGRESS, ABORTED, UNRECOVERABLE_ERROR, RECOVERABLE_ERROR, UPLOAD_FINISHED, POST_UPLOAD_FINISHED}

	
	/**
	 * Initiate upload
	 * @param callback completion callback to be called after upload is complete
	 * @return bytes uploaded
	 */
	public long upload(UploadCompleteCallback callback);
	
	/**
	 * @return
	 */
	public boolean stopUpload();
	
	/**
	 * @return percent of file uploaded
	 */
	public int getUploadPercent();

	/**
	 * Get the status of the upload
	 * @return status of upload
	 */
	public TemplateUploader.Status getStatus();


	/**
	 * Get time taken to upload so far
	 * @return time in seconds taken to upload
	 */
	public long getUploadTime();

	/**
	 * Get bytes uploaded
	 * @return bytes uploaded so far
	 */
	public long getUploadedBytes();

	/**
	 * Get the error if any
	 * @return error string if any
	 */
	public String getUploadError();

	/** Get local path of the uploaded file
	 * @return local path of the file uploaded
	 */
	public String getUploadLocalPath();

	public void setStatus(TemplateUploader.Status status);

	public void setUploadError(String string);

	public void setResume(boolean resume);	

}
