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
package org.apache.cloudstack.api.command.admin.domain;

import java.util.ArrayList;
import java.util.List;

import com.cloud.server.ResourceIcon;
import com.cloud.server.ResourceTag;
import org.apache.cloudstack.api.response.ResourceIconResponse;
import org.apache.log4j.Logger;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.ListResponse;

import com.cloud.domain.Domain;
import com.cloud.utils.Pair;

@APICommand(name = "listDomainChildren", description = "Lists all children domains belonging to a specified domain", responseObject = DomainResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class ListDomainChildrenCmd extends BaseListCmd {
    public static final Logger s_logger = Logger.getLogger(ListDomainChildrenCmd.class.getName());


    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = DomainResponse.class, description = "list children domain by parent domain ID.")
    private Long id;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, description = "list children domains by name")
    private String domainName;

    @Parameter(name = ApiConstants.IS_RECURSIVE,
               type = CommandType.BOOLEAN,
               description = "to return the entire tree, use the value \"true\". To return the first level children, use the value \"false\".")
    private Boolean recursive;

    @Parameter(name = ApiConstants.LIST_ALL,
               type = CommandType.BOOLEAN,
               description = "If set to false, list only resources belonging to the command's caller; if set to true - list resources that the caller is authorized to see. Default value is false")
    private Boolean listAll;

    @Parameter(name = ApiConstants.SHOW_RESOURCE_ICON, type = CommandType.BOOLEAN,
            description = "flag to display the resource icon for domains")
    private Boolean showIcon;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public String getDomainName() {
        return domainName;
    }

    public boolean listAll() {
        return listAll == null ? false : listAll;
    }

    public boolean isRecursive() {
        return recursive == null ? false : recursive;
    }

    public Boolean getShowIcon() {
        return showIcon != null ? showIcon : false;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() {
        Pair<List<? extends Domain>, Integer> result = _domainService.searchForDomainChildren(this);
        ListResponse<DomainResponse> response = new ListResponse<DomainResponse>();
        List<DomainResponse> domainResponses = new ArrayList<DomainResponse>();
        for (Domain domain : result.first()) {
            DomainResponse domainResponse = _responseGenerator.createDomainResponse(domain);
            domainResponse.setObjectName("domain");
            domainResponses.add(domainResponse);
        }

        response.setResponses(domainResponses, result.second());
        response.setResponseName(getCommandName());
        if (response != null && response.getCount() > 0 && getShowIcon()) {
            updateDomainResponse(response.getResponses());
        }
        this.setResponseObject(response);
    }

    private void updateDomainResponse(List<DomainResponse> response) {
        for (DomainResponse domainResponse : response) {
            ResourceIcon resourceIcon = resourceIconManager.getByResourceTypeAndUuid(ResourceTag.ResourceObjectType.Domain, domainResponse.getId());
            if (resourceIcon == null) {
                continue;
            }
            ResourceIconResponse iconResponse = _responseGenerator.createResourceIconResponse(resourceIcon);
            domainResponse.setResourceIconResponse(iconResponse);
        }
    }
}
