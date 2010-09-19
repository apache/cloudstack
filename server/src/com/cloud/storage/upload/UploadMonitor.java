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

package com.cloud.storage.upload;

import java.util.Map;

import com.cloud.async.AsyncJobManager;
import com.cloud.storage.VMTemplateHostVO;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.Upload.Type;
import com.cloud.storage.template.TemplateInfo;
import com.cloud.utils.component.Manager;

/**
 * Monitor upload progress of all entities.
 * @author nitin
 *
 */
public interface UploadMonitor extends Manager{		
	
	public void cancelAllUploads(Long templateId);

	public void extractTemplate(VMTemplateVO template, String url,
			VMTemplateHostVO tmpltHostRef,Long dataCenterId, long eventId, long asyncJobId, AsyncJobManager asyncMgr);

	void handleUploadTemplateSync(long sserverId,
			Map<String, TemplateInfo> templateInfo);

	void extractVolume(VolumeVO volume, String url, Long dataCenterId,
			String installPath, long eventId, long asyncJobId,
			AsyncJobManager asyncMgr);

    boolean isTypeUploadInProgress(Long typeId, Type type);

}