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

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import com.cloud.server.ResourceIcon;
import com.cloud.server.ResourceTag;
import org.apache.cloudstack.api.response.ResourceIconResponse;
import org.apache.log4j.Logger;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiConstants.DomainDetails;
import org.apache.cloudstack.api.BaseListAccountResourcesCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.TaggedResources;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.ProjectResponse;

import com.cloud.exception.InvalidParameterValueException;

@APICommand(name = "listProjects",
            description = "Lists projects and provides detailed information for listed projects",
            responseObject = ProjectResponse.class,
            since = "3.0.0",
            requestHasSensitiveInfo = false,
            responseHasSensitiveInfo = false)
public class ListProjectsCmd extends BaseListAccountResourcesCmd {
    public static final Logger s_logger = Logger.getLogger(ListProjectsCmd.class.getName());

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = ProjectResponse.class, description = "list projects by project ID")
    private Long id;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, description = "list projects by name")
    private String name;

    @Parameter(name = ApiConstants.DISPLAY_TEXT, type = CommandType.STRING, description = "list projects by display text")
    private String displayText;

    @Parameter(name = ApiConstants.STATE, type = CommandType.STRING, description = "list projects by state")
    private String state;

    @Parameter(name = ApiConstants.TAGS, type = CommandType.MAP, description = "List projects by tags (key/value pairs)")
    private Map tags;

    @Parameter(name = ApiConstants.USERNAME, type = CommandType.STRING, description = "List projects by username")
    private String username;

    @Parameter(name = ApiConstants.DETAILS,
               type = CommandType.LIST,
               collectionType = CommandType.STRING,
               description = "comma separated list of project details requested, value can be a list of [ all, resource, min]")
    private List<String> viewDetails;

    @Parameter(name = ApiConstants.SHOW_RESOURCE_ICON, type = CommandType.BOOLEAN,
            description = "flag to display the resource icon for projects")
    private Boolean showIcon;

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

    public String getUsername() {
        return username;
    }

    public Map<String, String> getTags() {
        return TaggedResources.parseKeyValueMap(tags, false);
    }

    public EnumSet<DomainDetails> getDetails() throws InvalidParameterValueException {
        EnumSet<DomainDetails> dv;
        if (viewDetails == null || viewDetails.size() <= 0) {
            dv = EnumSet.of(DomainDetails.all);
        } else {
            try {
                ArrayList<DomainDetails> dc = new ArrayList<DomainDetails>();
                for (String detail : viewDetails) {
                    dc.add(DomainDetails.valueOf(detail));
                }
                dv = EnumSet.copyOf(dc);
            } catch (IllegalArgumentException e) {
                throw new InvalidParameterValueException("The details parameter contains a non permitted value. The allowed values are " +
                    EnumSet.allOf(DomainDetails.class));
            }
        }
        return dv;
    }

    public Boolean getShowIcon() {
        return showIcon != null ? showIcon : false;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() {
        ListResponse<ProjectResponse> response = _queryService.listProjects(this);
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
        if (response != null && response.getCount() > 0 && getShowIcon()) {
            updateProjectResponse(response.getResponses());
        }
    }

    private void updateProjectResponse(List<ProjectResponse> response) {
        for (ProjectResponse projectResponse : response) {
            ResourceIcon resourceIcon = resourceIconManager.getByResourceTypeAndUuid(ResourceTag.ResourceObjectType.Project, projectResponse.getId());
            if (resourceIcon == null) {
                continue;
            }
            ResourceIconResponse iconResponse = _responseGenerator.createResourceIconResponse(resourceIcon);
            projectResponse.setResourceIconResponse(iconResponse);
        }
    }
}
