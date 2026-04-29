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

package org.apache.cloudstack.api.command.admin.storage;

import com.cloud.event.EventTypes;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.ImageStoreResponse;
import org.apache.cloudstack.api.response.MigrationResponse;
import org.apache.cloudstack.api.response.SnapshotResponse;
import org.apache.cloudstack.api.response.TemplateResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.commons.collections.CollectionUtils;

import java.util.Collections;
import java.util.List;

@APICommand(name = "migrateResourceToAnotherSecondaryStorage",
            description = "migrates resources from one secondary storage to destination image store",
            responseObject = MigrationResponse.class,
            requestHasSensitiveInfo = false,
            responseHasSensitiveInfo = false,
            since = "4.19.0",
            authorized = {RoleType.Admin})
public class MigrateResourcesToAnotherSecondaryStorageCmd extends BaseAsyncCmd {


    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.SRC_POOL,
               type = CommandType.UUID,
               entityType = ImageStoreResponse.class,
               description = "id of the image store from where the data is to be migrated",
               required = true)
    private Long id;


    @Parameter(name = ApiConstants.DEST_POOL,
               type = CommandType.UUID,
               entityType = ImageStoreResponse.class,
               description = "id of the destination secondary storage pool to which the resources are to be migrated",
               required = true)
    private Long destStoreId;

    @Parameter(name = "templates",
               type = CommandType.LIST,
               collectionType = CommandType.UUID,
               entityType = TemplateResponse.class,
               description = "id(s) of the templates to be migrated",
               required = false)
    private List<Long> templateIdList;

    @Parameter(name = "snapshots",
               type = CommandType.LIST,
               collectionType = CommandType.UUID,
               entityType = SnapshotResponse.class,
               description = "id(s) of the snapshots to be migrated",
               required = false)
    private List<Long> snapshotIdList;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getDestStoreId() {
        return destStoreId;
    }

    public List<Long> getTemplateIdList() {
        if (CollectionUtils.isEmpty(templateIdList)) {
            return Collections.emptyList();
        }
        return templateIdList;
    }

    public List<Long> getSnapshotIdList() {
        if (CollectionUtils.isEmpty(snapshotIdList)) {
            return Collections.emptyList();
        }
        return snapshotIdList;
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_IMAGE_STORE_RESOURCES_MIGRATE;
    }

    @Override
    public String getEventDescription() {
        return "Attempting to migrate files/data objects to another image store";
    }

    @Override
    public void execute() {
        MigrationResponse response = _imageStoreService.migrateResources(this);
        response.setObjectName("imagestore");
        this.setResponseObject(response);
        CallContext.current().setEventDetails(response.getMessage());
    }

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccountId();
    }

    @Override
    public Long getApiResourceId() {
        return getId();
    }

    public Long getId() {
        return id;
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.ImageStore;
    }
}
