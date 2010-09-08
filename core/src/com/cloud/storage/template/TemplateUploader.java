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
	
	public boolean isInited();	
	

}
