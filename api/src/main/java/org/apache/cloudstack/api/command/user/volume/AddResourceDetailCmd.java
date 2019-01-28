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
package org.apache.cloudstack.api.command.user.volume;

import java.util.Map;

import org.apache.log4j.Logger;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.TaggedResources;
import org.apache.cloudstack.api.response.SuccessResponse;

import com.cloud.event.EventTypes;
import com.cloud.server.ResourceTag;

@APICommand(name = "addResourceDetail", description = "Adds detail for the Resource.", responseObject = SuccessResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class AddResourceDetailCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(AddResourceDetailCmd.class.getName());
    private static final String s_name = "addresourcedetailresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.DETAILS, type = CommandType.MAP, required = true, description = "Map of (key/value pairs)")
    private Map details;

    @Parameter(name = ApiConstants.RESOURCE_TYPE, type = CommandType.STRING, required = true, description = "type of the resource")
    private String resourceType;

    @Parameter(name = ApiConstants.RESOURCE_ID, type = CommandType.STRING, required = true, collectionType = CommandType.STRING, description = "resource id to create the details for")
    private String resourceId;

    @Parameter(name = ApiConstants.FOR_DISPLAY, type = CommandType.BOOLEAN, description = "pass false if you want this detail to be disabled for the regular user. True by default", since = "4.4")
    private Boolean display;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Map getDetails() {
        return TaggedResources.parseKeyValueMap(details, true);
    }

    public ResourceTag.ResourceObjectType getResourceType() {
        return _taggedResourceService.getResourceType(resourceType);
    }

    public String getResourceId() {
        return resourceId;
    }

    public boolean forDisplay() {
        if (display != null) {
            return display;
        } else {
            return true;
        }
    }

/////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public long getEntityOwnerId() {
        //FIXME - validate the owner here
        return 1;
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_RESOURCE_DETAILS_CREATE;
    }

    @Override
    public String getEventDescription() {
        return "adding details to the resource ";
    }

    @Override
    public void execute() {
        _resourceMetaDataService.addResourceMetaData(getResourceId(), getResourceType(), getDetails(), forDisplay());
        setResponseObject(new SuccessResponse(getCommandName()));
    }
}
