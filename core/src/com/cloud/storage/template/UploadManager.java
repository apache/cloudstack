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

import com.cloud.agent.api.storage.CreateEntityDownloadURLAnswer;
import com.cloud.agent.api.storage.CreateEntityDownloadURLCommand;
import com.cloud.agent.api.storage.DeleteEntityDownloadURLAnswer;
import com.cloud.agent.api.storage.DeleteEntityDownloadURLCommand;
import com.cloud.agent.api.storage.UploadAnswer;
import com.cloud.agent.api.storage.UploadCommand;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.Upload.Status;
import com.cloud.utils.component.Manager;

public interface UploadManager extends Manager {


	/**
	 * Get the status of a upload job
	 * @param jobId job Id
	 * @return status of the upload job
	 */
	public TemplateUploader.Status getUploadStatus(String jobId);
	
	/**
	 * Get the status of a upload job
	 * @param jobId job Id
	 * @return status of the upload job
	 */
	public Status getUploadStatus2(String jobId);

	/**
	 * Get the upload percent of a upload job
	 * @param jobId job Id
	 * @return
	 */
	public int getUploadPct(String jobId);

	/**
	 * Get the upload error if any
	 * @param jobId job Id
	 * @return
	 */
	public String getUploadError(String jobId);

	/**
	 * Get the local path for the upload
	 * @param jobId job Id
	 * @return
	public String getUploadLocalPath(String jobId);
     */
	
	/** Handle upload commands from the management server
	 * @param cmd cmd from server
	 * @return answer representing status of upload.
	 */
	public UploadAnswer handleUploadCommand(UploadCommand cmd);		
    
    public String getPublicTemplateRepo();


	String uploadPublicTemplate(long id, String url, String name,
			ImageFormat format, Long accountId, String descr,
			String cksum, String installPathPrefix, String user,
			String password, long maxTemplateSizeInBytes);
	

    CreateEntityDownloadURLAnswer handleCreateEntityURLCommand(CreateEntityDownloadURLCommand cmd);
    
    DeleteEntityDownloadURLAnswer handleDeleteEntityDownloadURLCommand(DeleteEntityDownloadURLCommand cmd);
	
}
