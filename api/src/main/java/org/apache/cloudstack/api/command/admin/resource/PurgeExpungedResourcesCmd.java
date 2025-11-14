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

package org.apache.cloudstack.api.command.admin.resource;


import java.util.Date;

import javax.inject.Inject;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.PurgeExpungedResourcesResponse;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.resource.ResourceCleanupService;

import com.cloud.event.EventTypes;

@APICommand(name = "purgeExpungedResources",
        description = "Purge expunged resources",
        responseObject = SuccessResponse.class,
        responseView = ResponseObject.ResponseView.Full,
        requestHasSensitiveInfo = false,
        responseHasSensitiveInfo = false,
        authorized = {RoleType.Admin},
        since = "4.20")
public class PurgeExpungedResourcesCmd extends BaseAsyncCmd {

    @Inject
    ResourceCleanupService resourceCleanupService;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.RESOURCE_TYPE, type = BaseCmd.CommandType.STRING,
            description = "The type of the resource which need to be purged. Supported types: " +
                    "VirtualMachine")
    private String resourceType;

    @Parameter(name = ApiConstants.BATCH_SIZE, type = CommandType.LONG,
            description = "The size of batch used during purging")
    private Long batchSize;

    @Parameter(name = ApiConstants.START_DATE,
            type = CommandType.DATE,
            description = "The start date range of the expunged resources used for purging " +
                    "(use format \"yyyy-MM-dd\" or \"yyyy-MM-dd HH:mm:ss\")")
    private Date startDate;

    @Parameter(name = ApiConstants.END_DATE,
            type = CommandType.DATE,
            description = "The end date range of the expunged resources used for purging " +
                    "(use format \"yyyy-MM-dd\" or \"yyyy-MM-dd HH:mm:ss\")")
    private Date endDate;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////


    public String getResourceType() {
        return resourceType;
    }

    public Long getBatchSize() {
        return batchSize;
    }

    public Date getStartDate() {
        return startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccount().getId();
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_PURGE_EXPUNGED_RESOURCES;
    }

    @Override
    public String getEventDescription() {
        return "Purging expunged resources";
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() {
        try {
            long result = resourceCleanupService.purgeExpungedResources(this);
            PurgeExpungedResourcesResponse response = new PurgeExpungedResourcesResponse();
            response.setResourceCount(result);
            response.setObjectName(getCommandName().toLowerCase());
            setResponseObject(response);
        } catch (Exception e) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, e.getLocalizedMessage());
        }
    }
}
