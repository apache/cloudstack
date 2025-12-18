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
package org.apache.cloudstack.storage.sharedfs;

import java.util.List;

import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.command.user.storage.sharedfs.ChangeSharedFSDiskOfferingCmd;
import org.apache.cloudstack.api.command.user.storage.sharedfs.ChangeSharedFSServiceOfferingCmd;
import org.apache.cloudstack.api.command.user.storage.sharedfs.CreateSharedFSCmd;
import org.apache.cloudstack.api.command.user.storage.sharedfs.DestroySharedFSCmd;

import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ManagementServerException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.exception.VirtualMachineMigrationException;

import org.apache.cloudstack.api.command.user.storage.sharedfs.ListSharedFSCmd;
import org.apache.cloudstack.api.command.user.storage.sharedfs.UpdateSharedFSCmd;
import org.apache.cloudstack.api.response.SharedFSResponse;
import org.apache.cloudstack.api.response.ListResponse;

public interface SharedFSService {

    List<SharedFSProvider> getSharedFSProviders();

    boolean stateTransitTo(SharedFS sharedFS, SharedFS.Event event);

    void setSharedFSProviders(List<SharedFSProvider> sharedFSProviders);

    SharedFSProvider getSharedFSProvider(String sharedFSProviderName);

    SharedFS allocSharedFS(CreateSharedFSCmd cmd);

    SharedFS deploySharedFS(CreateSharedFSCmd cmd) throws ResourceUnavailableException, InsufficientCapacityException, ResourceAllocationException, OperationTimedoutException;

    SharedFS startSharedFS(Long sharedFSId) throws OperationTimedoutException, ResourceUnavailableException, InsufficientCapacityException, ResourceAllocationException;

    SharedFS stopSharedFS(Long sharedFSId, Boolean forced);

    SharedFS restartSharedFS(Long sharedFSId, boolean cleanup) throws OperationTimedoutException, ResourceUnavailableException, InsufficientCapacityException, ResourceAllocationException;

    ListResponse<SharedFSResponse> searchForSharedFS(ResponseObject.ResponseView respView, ListSharedFSCmd cmd);

    SharedFS updateSharedFS(UpdateSharedFSCmd cmd);

    SharedFS changeSharedFSDiskOffering(ChangeSharedFSDiskOfferingCmd cmd) throws ResourceAllocationException;

    SharedFS changeSharedFSServiceOffering(ChangeSharedFSServiceOfferingCmd cmd) throws OperationTimedoutException, ResourceUnavailableException, InsufficientCapacityException, ManagementServerException, VirtualMachineMigrationException;

    Boolean destroySharedFS(DestroySharedFSCmd cmd);

    SharedFS recoverSharedFS(Long sharedFSId);

    void deleteSharedFS(Long sharedFSId);
}
