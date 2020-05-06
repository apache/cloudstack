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

import java.util.List;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.MigrationResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.log4j.Logger;

import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.utils.StringUtils;

@APICommand(name = MigrateSecondaryStorageDataCmd.APINAME,
        description = "migrates data objects from one secondary storage to destination image store(s)",
        responseObject = MigrationResponse.class,
        requestHasSensitiveInfo = false,
        responseHasSensitiveInfo = false,
        since = "4.14.0",
        authorized = {RoleType.Admin})
public class MigrateSecondaryStorageDataCmd extends BaseAsyncCmd {

    public static final Logger s_logger = Logger.getLogger(MigrateSecondaryStorageDataCmd.class.getName());

    public static final String APINAME = "migrateSecondaryStorageData";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.FROM,
            type = CommandType.UUID,
            entityType = MigrationResponse.class,
            description = "id of the image store from where the data is to be migrated",
    required = true)
    private Long id;

    @Parameter(name = ApiConstants.MIGRATE_TO,
    type = CommandType.LIST,
    collectionType = CommandType.UUID,
    entityType = MigrationResponse.class,
    description = "id of the destination secondary storage pool to which the templates are to be migrated to",
    required = true)
    private List<Long> migrateTo;

    @Parameter(name = ApiConstants.MIGRATION_TYPE,
    type = CommandType.STRING,
    description = "Balance: if you want data to be distributed evenly among the destination stores, " +
            "Complete: If you want to migrate the entire data from source image store to the destination store(s)",
    required = true)
    private String migrationType;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public List<Long> getMigrateTo() {
        return migrateTo;
    }

    public String getMigrationType() {
        return migrationType;
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_FILE_MIGRATE;
    }

    @Override
    public String getEventDescription() {
        return "Attempting to migrate files/data objects " + "from : " + this.getId() + " to: " + StringUtils.join(getMigrateTo(), ",");
    }

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        MigrationResponse response = _imageStoreService.migrateData(this);
        response.setObjectName("imagestore");
        this.setResponseObject(response);
    }

    @Override
    public String getCommandName() {
        return APINAME.toLowerCase() + BaseAsyncCmd.RESPONSE_SUFFIX;
    }

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccountId();
    }
}