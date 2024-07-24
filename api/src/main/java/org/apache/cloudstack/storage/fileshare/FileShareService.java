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
package org.apache.cloudstack.storage.fileshare;

import java.util.List;
import java.util.Map;

import javax.naming.ConfigurationException;

import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.command.user.storage.fileshare.ChangeFileShareDiskOfferingCmd;
import org.apache.cloudstack.api.command.user.storage.fileshare.ChangeFileShareServiceOfferingCmd;
import org.apache.cloudstack.api.command.user.storage.fileshare.CreateFileShareCmd;
import org.apache.cloudstack.api.command.user.storage.fileshare.ResizeFileShareCmd;
import org.apache.cloudstack.api.command.user.storage.fileshare.UpdateFileShareCmd;

import com.cloud.utils.component.PluggableService;
import org.apache.cloudstack.api.command.user.storage.fileshare.ListFileSharesCmd;
import org.apache.cloudstack.api.response.FileShareResponse;
import org.apache.cloudstack.api.response.ListResponse;

public interface FileShareService extends PluggableService {

    boolean configure(String name, Map<String, Object> params) throws ConfigurationException;

    List<Class<?>> getCommands();

    List<FileShareProvider> getFileShareProviders();

    void setFileShareProviders(List<FileShareProvider> fileShareProviders);

    FileShareProvider getFileShareProvider(String fileShareProviderName);

    FileShare allocFileShare(CreateFileShareCmd cmd);

    FileShare deployFileShare(Long fileShareId, Long networkId, Long diskOfferingId, Long size);

    FileShare startFileShare(Long fileShareId);

    FileShare stopFileShare(Long fileShareId);

    FileShare restartFileShare(Long fileShareId, boolean cleanup);

    ListResponse<FileShareResponse> searchForFileShares(ResponseObject.ResponseView respView, ListFileSharesCmd cmd);

    FileShare updateFileShare(UpdateFileShareCmd cmd);

    FileShare resizeFileShare(ResizeFileShareCmd cmd);

    FileShare changeFileShareDiskOffering(ChangeFileShareDiskOfferingCmd cmd);

    FileShare changeFileShareServiceOffering(ChangeFileShareServiceOfferingCmd cmd);

    FileShare destroyFileShare(Long fileShareId);

    FileShare recoverFileShare(Long fileShareId);

    void deleteFileShare(Long fileShareId);
}
