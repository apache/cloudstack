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

import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.command.user.storage.fileshare.ChangeFileShareDiskOfferingCmd;
import org.apache.cloudstack.api.command.user.storage.fileshare.ChangeFileShareServiceOfferingCmd;
import org.apache.cloudstack.api.command.user.storage.fileshare.CreateFileShareCmd;
import org.apache.cloudstack.api.command.user.storage.fileshare.DestroyFileShareCmd;
import org.apache.cloudstack.api.command.user.storage.fileshare.UpdateFileShareCmd;

import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import org.apache.cloudstack.api.command.user.storage.fileshare.ListFileSharesCmd;
import org.apache.cloudstack.api.response.FileShareResponse;
import org.apache.cloudstack.api.response.ListResponse;

public interface FileShareService {

    List<FileShareProvider> getFileShareProviders();

    boolean stateTransitTo(FileShare fileShare, FileShare.Event event);

    void setFileShareProviders(List<FileShareProvider> fileShareProviders);

    FileShareProvider getFileShareProvider(String fileShareProviderName);

    FileShare allocFileShare(CreateFileShareCmd cmd);

    FileShare deployFileShare(CreateFileShareCmd cmd) throws ResourceUnavailableException, InsufficientCapacityException, ResourceAllocationException, OperationTimedoutException;

    FileShare startFileShare(Long fileShareId) throws OperationTimedoutException, ResourceUnavailableException, InsufficientCapacityException, ResourceAllocationException;

    FileShare stopFileShare(Long fileShareId, Boolean forced);

    FileShare restartFileShare(Long fileShareId, boolean cleanup) throws OperationTimedoutException, ResourceUnavailableException, InsufficientCapacityException, ResourceAllocationException;

    ListResponse<FileShareResponse> searchForFileShares(ResponseObject.ResponseView respView, ListFileSharesCmd cmd);

    FileShare updateFileShare(UpdateFileShareCmd cmd);

    FileShare changeFileShareDiskOffering(ChangeFileShareDiskOfferingCmd cmd) throws ResourceAllocationException;

    FileShare changeFileShareServiceOffering(ChangeFileShareServiceOfferingCmd cmd) throws OperationTimedoutException, ResourceUnavailableException, InsufficientCapacityException, ResourceAllocationException;

    Boolean destroyFileShare(DestroyFileShareCmd cmd);

    FileShare recoverFileShare(Long fileShareId);

    void deleteFileShare(Long fileShareId);
}
