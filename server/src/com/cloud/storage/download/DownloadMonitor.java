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

package com.cloud.storage.download;

import java.util.Map;

import com.cloud.exception.StorageUnavailableException;
import com.cloud.host.HostVO;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.template.TemplateInfo;
import com.cloud.utils.component.Manager;

/**
 * Monitor download progress of all templates across all servers
 * @author chiradeep
 *
 */
public interface DownloadMonitor extends Manager{
	
	public boolean downloadTemplateToStorage(VMTemplateVO template, Long zoneId);
	
	public void cancelAllDownloads(Long templateId);

	public void handleTemplateSync(HostVO host);

	public boolean copyTemplate(VMTemplateVO template, HostVO sourceServer, HostVO destServer)
			throws StorageUnavailableException;

	/*When new host added, take a look at if there are templates needed to be downloaded for the same hypervisor as the host*/
    void handleSysTemplateDownload(HostVO hostId);

    void handleTemplateSync(Long dcId);

    void addSystemVMTemplatesToHost(HostVO host, Map<String, TemplateInfo> templateInfos);

}