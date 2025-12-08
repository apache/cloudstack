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

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.SuccessResponse;

import com.cloud.event.EventTypes;
import com.cloud.server.ResourceTag;
import com.cloud.server.ResourceTag.ResourceObjectType;

@APICommand(name = "deleteTags", description = "Deleting resource tag(s)", responseObject = SuccessResponse.class, since = "4.0.0", entityType = {ResourceTag.class},
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class DeleteTagsCmd extends BaseAsyncCmd {


    // ///////////////////////////////////////////////////
    // ////////////// API parameters /////////////////////
    // ///////////////////////////////////////////////////

    @Parameter(name = ApiConstants.TAGS, type = CommandType.MAP, description = "Delete tags matching key/value pairs")
    private Map tag;

    @Parameter(name = ApiConstants.RESOURCE_TYPE, type = CommandType.STRING, required = true, description = "Delete tag by resource type")
    private String resourceType;

    @Parameter(name = ApiConstants.RESOURCE_IDS,
               type = CommandType.LIST,
               required = true,
               collectionType = CommandType.STRING,
               description = "Delete tags for resource id(s)")
    private List<String> resourceIds;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public ResourceObjectType getResourceType() {
        return resourceManagerUtil.getResourceType(resourceType);
    }

    public Map<String, String> getTags() {
        Map<String, String> tagsMap = null;
        if (tag != null && !tag.isEmpty()) {
            tagsMap = new HashMap<String, String>();
            Collection<?> servicesCollection = tag.values();
            Iterator<?> iter = servicesCollection.iterator();
            while (iter.hasNext()) {
                HashMap<String, String> services = (HashMap<String, String>)iter.next();
                String key = services.get("key");
                String value = services.get("value");
                tagsMap.put(key, value);
            }
        }
        return tagsMap;
    }

    public List<String> getResourceIds() {
        return resourceIds;
    }

    // ///////////////////////////////////////////////////
    // ///////////// API Implementation///////////////////
    // ///////////////////////////////////////////////////

    @Override
    public long getEntityOwnerId() {
        //FIXME - validate the owner here
        return 1;
    }

    @Override
    public void execute() {
        boolean success = _taggedResourceService.deleteTags(getResourceIds(), getResourceType(), getTags());

        if (success) {
            SuccessResponse response = new SuccessResponse(getCommandName());
            setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to delete tags");
        }
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_TAGS_DELETE;
    }

    @Override
    public String getEventDescription() {
        return "Deleting tags";
    }
}
