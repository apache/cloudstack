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

import com.cloud.server.ResourceTag;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.api.response.VolumeResponse;
import org.apache.cloudstack.context.CallContext;

import org.apache.log4j.Logger;

import com.cloud.async.AsyncJob;
import com.cloud.event.EventTypes;
import com.cloud.storage.Volume;
import com.cloud.user.Account;

import java.util.*;

@APICommand(name = "addResourceDetail", description="Adds detail for the Resource.", responseObject=SuccessResponse.class)
public class AddResourceDetailCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(AddResourceDetailCmd.class.getName());
    private static final String s_name = "addResourceDetailresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.DETAILS, type = CommandType.MAP, required=true, description = "Map of (key/value pairs)")
    private Map details;

    @Parameter(name=ApiConstants.RESOURCE_TYPE, type=CommandType.STRING, required=true, description="type of the resource")
    private String resourceType;

    @Parameter(name=ApiConstants.RESOURCE_ID, type=CommandType.STRING, required=true,
            collectionType=CommandType.STRING, description="resource id to create the details for")
    private String resourceId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Map getDetails() {
        Map<String, String> detailsMap = null;
        if (!details.isEmpty()) {
            detailsMap = new HashMap<String, String>();
            Collection<?> servicesCollection = details.values();
            Iterator<?> iter = servicesCollection.iterator();
            while (iter.hasNext()) {
                HashMap<String, String> services = (HashMap<String, String>) iter.next();
                String key = services.get("key");
                String value = services.get("value");
                detailsMap.put(key, value);
            }
        }
        return detailsMap;
    }

    public ResourceTag.TaggedResourceType getResourceType() {
        return _taggedResourceService.getResourceType(resourceType);
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
        return  "adding details to the resource ";
    }

    @Override
    public void execute(){
        _resourceMetaDataService.addResourceMetaData(getResourceId(), getResourceType(), getDetails());
        this.setResponseObject(new SuccessResponse(getCommandName()));
    }
}
