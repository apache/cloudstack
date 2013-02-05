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
package org.apache.cloudstack.api.command.user.project;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListAccountResourcesCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.ProjectResponse;
import org.apache.log4j.Logger;

import com.cloud.exception.InvalidParameterValueException;

@APICommand(name = "listProjects", description="Lists projects and provides detailed information for listed projects", responseObject=ProjectResponse.class, since="3.0.0")
public class ListProjectsCmd extends BaseListAccountResourcesCmd {
    public static final Logger s_logger = Logger.getLogger(ListProjectsCmd.class.getName());
    private static final String s_name = "listprojectsresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.ID, type=CommandType.UUID, entityType=ProjectResponse.class,
            description="list projects by project ID")
    private Long id;

    @Parameter(name=ApiConstants.NAME, type=CommandType.STRING, description="list projects by name")
    private String name;

    @Parameter(name=ApiConstants.DISPLAY_TEXT, type=CommandType.STRING, description="list projects by display text")
    private String displayText;

    @Parameter(name=ApiConstants.STATE, type=CommandType.STRING, description="list projects by state")
    private String state;

    @Parameter(name = ApiConstants.TAGS, type = CommandType.MAP, description = "List projects by tags (key/value pairs)")
    private Map tags;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDisplayText() {
        return displayText;
    }


    public String getState() {
        return state;
    }

    @Override
    public String getCommandName() {
        return s_name;
    }

    public Map<String, String> getTags() {
        Map<String, String> tagsMap = null;
        if (tags != null && !tags.isEmpty()) {
            tagsMap = new HashMap<String, String>();
            Collection<?> servicesCollection = tags.values();
            Iterator<?> iter = servicesCollection.iterator();
            while (iter.hasNext()) {
                HashMap<String, String> services = (HashMap<String, String>) iter.next();
                String key = services.get("key");
                String value = services.get("value");
                if (value == null) {
                    throw new InvalidParameterValueException("No value is passed in for key " + key);
                }
                tagsMap.put(key, value);
            }
        }
        return tagsMap;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute(){
        ListResponse<ProjectResponse> response = _queryService.listProjects(this);
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
    }
}
