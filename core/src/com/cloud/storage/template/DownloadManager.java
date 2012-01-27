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

import java.util.List;
import java.util.Map;

import com.cloud.agent.api.storage.DownloadAnswer;
import com.cloud.agent.api.storage.DownloadCommand;
import com.cloud.agent.api.storage.DownloadCommand.Proxy;
import com.cloud.storage.VMTemplateHostVO;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.resource.SecondaryStorageResource;
import com.cloud.utils.component.Manager;

/**
 * @author chiradeep
 *
 */
public interface DownloadManager extends Manager {

	/**
	 * Initiate download of a public template
	 * @param id unique id.
	 * @param url  the url from where to download from
	 * @param name the user-supplied name for the template
	 * @param format the file format of the template
     * @param hvm  whether the template is a hardware virtual machine
     * @param accountId the accountId of the iso owner (null if public iso)
	 * @param descr	description of the template
	 * @param cksum	checksum for the downloaded file (note: not of the final installed template)
	 * @param the name if the ISO file if the template being downloaded is an ISO
	 * @param user username used for authentication to the server
	 * @param password password used for authentication to the server
	 * @param maxDownloadSizeInBytes (optional) max download size for the template, in bytes. 
	 * @return job-id that can be used to interrogate the status of the download.
	 */
	public String downloadPublicTemplate(long id, String url, String name, ImageFormat format, boolean hvm, Long accountId, String descr, String cksum, String installPathPrefix, String userName, String passwd, long maxDownloadSizeInBytes, Proxy proxy);
	
	
	/**
	 * Get the status of a download job
	 * @param jobId job Id
	 * @return status of the download job
	 */
	public TemplateDownloader.Status getDownloadStatus(String jobId);
	
	/**
	 * Get the status of a download job
	 * @param jobId job Id
	 * @return status of the download job
	 */
	public VMTemplateHostVO.Status getDownloadStatus2(String jobId);

	/**
	 * Get the download percent of a download job
	 * @param jobId job Id
	 * @return
	 */
	public int getDownloadPct(String jobId);

	/**
	 * Get the download error if any
	 * @param jobId job Id
	 * @return
	 */
	public String getDownloadError(String jobId);

	/**
	 * Get the local path for the download
	 * @param jobId job Id
	 * @return
	public String getDownloadLocalPath(String jobId);
     */
	
	/** Handle download commands from the management server
	 * @param cmd cmd from server
	 * @return answer representing status of download.
	 */
	public DownloadAnswer handleDownloadCommand(SecondaryStorageResource resource, DownloadCommand cmd);
	
	
	/**
	/**
	 * @return list of template info for installed templates
	 */
	public Map<String, TemplateInfo> gatherTemplateInfo(String templateDir);
}