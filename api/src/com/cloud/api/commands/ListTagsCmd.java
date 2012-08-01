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

package com.cloud.api.commands;

import java.util.ArrayList;
import java.util.List;

import com.cloud.api.ApiConstants;
import com.cloud.api.BaseListProjectAndAccountResourcesCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.ListResponse;
import com.cloud.api.response.ResourceTagResponse;
import com.cloud.server.ResourceTag;

/**
 * @author Alena Prokharchyk
 */

@Implementation(description = "List resource tag(s)", responseObject = ResourceTagResponse.class, since = "Burbank")
public class ListTagsCmd extends BaseListProjectAndAccountResourcesCmd{
    private static final String s_name = "listtagsresponse";
    
    @Parameter(name=ApiConstants.RESOURCE_TYPE, type=CommandType.STRING, description="list by resource type")
    private String resourceType;
    
    @Parameter(name=ApiConstants.RESOURCE_ID, type=CommandType.STRING, description="list by resource id")
    private String resourceId;
    
    @Parameter(name=ApiConstants.KEY, type=CommandType.STRING, description="list by key")
    private String key;
    
    @Parameter(name=ApiConstants.VALUE, type=CommandType.STRING, description="list by value")
    private String value;
    
    @Parameter(name=ApiConstants.CUSTOMER, type=CommandType.STRING, description="list by customer name")
    private String customer;
    
    
    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() {
      
      List<? extends ResourceTag> tags = _taggedResourceService.listTags(this);
      ListResponse<ResourceTagResponse> response = new ListResponse<ResourceTagResponse>();
      List<ResourceTagResponse> tagResponses = new ArrayList<ResourceTagResponse>();
      for (ResourceTag tag : tags) {
          ResourceTagResponse tagResponse = _responseGenerator.createResourceTagResponse(tag, false);
          tagResponses.add(tagResponse);
      }
      response.setResponses(tagResponses);
      
      response.setResponseName(getCommandName());
      this.setResponseObject(response);
    }

    public String getResourceType() {
        return resourceType;
    }

    public String getResourceId() {
        return resourceId;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String getCommandName() {
        return s_name;
    }
    
    public String getCustomer() {
        return customer;
    }
}
