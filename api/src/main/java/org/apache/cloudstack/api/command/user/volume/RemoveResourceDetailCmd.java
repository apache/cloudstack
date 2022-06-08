// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for Removeitional information
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
package org.apache.cloudstack.api.command.user.volume;

import org.apache.log4j.Logger;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.SuccessResponse;

import com.cloud.event.EventTypes;
import com.cloud.server.ResourceTag;

@APICommand(name = "removeResourceDetail", description = "Removes detail for the Resource.", responseObject = SuccessResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class RemoveResourceDetailCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(RemoveResourceDetailCmd.class.getName());
    private static final String s_name = "removeresourcedetailresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.KEY, type = CommandType.STRING, description = "Delete details matching key/value pairs")
    private String key;

    @Parameter(name = ApiConstants.RESOURCE_TYPE, type = CommandType.STRING, required = true, description = "Delete detail by resource type")
    private String resourceType;

    @Parameter(name = ApiConstants.RESOURCE_ID,
               type = CommandType.STRING,
               required = true,
               collectionType = CommandType.STRING,
               description = "Delete details for resource id")
    private String resourceId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public ResourceTag.ResourceObjectType getResourceType() {
        return resourceManagerUtil.getResourceType(resourceType);
    }

    public String getKey() {
        return key;
    }

    public String getResourceId() {
        return resourceId;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.Volume;
    }

    @Override
    public long getEntityOwnerId() {
        //FIXME - validate the owner here
        return 1;
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_RESOURCE_DETAILS_DELETE;
    }

    @Override
    public String getEventDescription() {
        return "Removing detail to the volume ";
    }

    @Override
    public void execute() {
        _resourceMetaDataService.deleteResourceMetaData(getResourceId(), getResourceType(), getKey());
        this.setResponseObject(new SuccessResponse(getCommandName()));
    }
}
