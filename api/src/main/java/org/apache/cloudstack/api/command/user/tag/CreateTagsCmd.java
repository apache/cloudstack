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

package org.apache.cloudstack.api.command.user.tag;

import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.TaggedResources;
import org.apache.cloudstack.api.response.SuccessResponse;

import com.cloud.event.EventTypes;
import com.cloud.server.ResourceTag;
import com.cloud.server.ResourceTag.ResourceObjectType;

@APICommand(name = "createTags", description = "Creates resource tag(s)", responseObject = SuccessResponse.class, since = "4.0.0", entityType = {ResourceTag.class},
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class CreateTagsCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(CreateTagsCmd.class.getName());

    private static final String s_name = "createtagsresponse";

    // ///////////////////////////////////////////////////
    // ////////////// API parameters /////////////////////
    // ///////////////////////////////////////////////////

    @Parameter(name = ApiConstants.TAGS, type = CommandType.MAP, required = true, description = "Map of tags (key/value pairs)")
    private Map tag;

    @Parameter(name = ApiConstants.RESOURCE_TYPE, type = CommandType.STRING, required = true, description = "type of the resource")
    private String resourceType;

    @Parameter(name = ApiConstants.RESOURCE_IDS,
               type = CommandType.LIST,
               required = true,
               collectionType = CommandType.STRING,
               description = "list of resources to create the tags for")
    private List<String> resourceIds;

    @Parameter(name = ApiConstants.CUSTOMER, type = CommandType.STRING, description = "identifies client specific tag. "
        + "When the value is not null, the tag can't be used by cloudStack code internally")
    private String customer;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public ResourceObjectType getResourceType() {
        return _taggedResourceService.getResourceType(resourceType);
    }

    public Map<String, String> getTags() {
        return TaggedResources.parseKeyValueMap(tag, true);
    }

    public List<String> getResourceIds() {
        return resourceIds;
    }

    public String getCustomer() {
        return customer;
    }

    // ///////////////////////////////////////////////////
    // ///////////// API Implementation///////////////////
    // ///////////////////////////////////////////////////

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
    public void execute() {
        List<ResourceTag> tags = _taggedResourceService.createTags(getResourceIds(), getResourceType(), getTags(), getCustomer());

        if (tags != null && !tags.isEmpty()) {
            SuccessResponse response = new SuccessResponse(getCommandName());
            setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to create tags");
        }
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_TAGS_CREATE;
    }

    @Override
    public String getEventDescription() {
        return "creating tags";
    }
}
