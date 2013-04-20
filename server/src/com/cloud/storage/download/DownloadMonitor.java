// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.storage.download;

import org.apache.cloudstack.engine.subsystem.api.storage.CreateCmdResult;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.framework.async.AsyncCompletionCallback;


import com.cloud.exception.StorageUnavailableException;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.utils.component.Manager;

/**
 * Monitor download progress of all templates across all servers
 *
 */
public interface DownloadMonitor extends Manager{

    // when ssvm is not available yet
    public void downloadBootstrapSysTemplateToStorage(VMTemplateVO template, DataStore store, AsyncCompletionCallback<CreateCmdResult> callback);

    public void downloadTemplateToStorage(VMTemplateVO template, DataStore store, AsyncCompletionCallback<CreateCmdResult> callback);

	public void cancelAllDownloads(Long templateId);

	public boolean copyTemplate(VMTemplateVO template, DataStore sourceStore, DataStore Store)
			throws StorageUnavailableException;

    //void addSystemVMTemplatesToHost(HostVO host, Map<String, TemplateProp> templateInfos);

	//public boolean downloadVolumeToStorage(VolumeVO volume, Long zoneId, String url, String checkSum, ImageFormat format);

	public void downloadVolumeToStorage(VolumeVO volume, DataStore store, String url, String checkSum, ImageFormat format, AsyncCompletionCallback<CreateCmdResult> callback);

}