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
package org.apache.cloudstack.api.command.admin.snapshot;

import com.cloud.storage.Snapshot;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.command.admin.AdminCmd;
import org.apache.cloudstack.api.command.user.snapshot.ListSnapshotsCmd;
import org.apache.cloudstack.api.response.ImageStoreResponse;
import org.apache.cloudstack.api.response.SnapshotResponse;
import org.apache.cloudstack.api.response.StoragePoolResponse;

@APICommand(name = "listSnapshots", description = "Lists all available snapshots for the account.", responseObject = SnapshotResponse.class, entityType = {
        Snapshot.class }, responseView = ResponseObject.ResponseView.Full, requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class ListSnapshotsCmdByAdmin extends ListSnapshotsCmd implements AdminCmd {
    @Parameter(name = ApiConstants.IMAGE_STORE_ID, type = CommandType.UUID, entityType = ImageStoreResponse.class,
               description = "ID of the image or image cache store", since = "4.19")
    private Long imageStoreId;

    @Parameter(name = ApiConstants.STORAGE_ID, type = CommandType.UUID, entityType = StoragePoolResponse.class,
               description = "ID of the storage pool", since = "4.19")
    private Long storagePoolId;

    @Override
    public Long getImageStoreId() {
        return imageStoreId;
    }

    @Override
    public Long getStoragePoolId() {
        return storagePoolId;
    }
}
