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
import java.util.EnumSet;
import java.util.List;

import org.apache.log4j.Logger;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiConstants.DomainDetails;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject.ResponseView;
import org.apache.cloudstack.api.command.user.UserCmd;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.ListResponse;

import com.cloud.domain.Domain;
import com.cloud.exception.InvalidParameterValueException;

@APICommand(name = "listDomains", description = "Lists domains and provides detailed information for listed domains", responseObject = DomainResponse.class, responseView = ResponseView.Restricted, entityType = {Domain.class},
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class ListDomainsCmd extends BaseListCmd implements UserCmd {
    public static final Logger s_logger = Logger.getLogger(ListDomainsCmd.class.getName());

    private static final String s_name = "listdomainsresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = DomainResponse.class, description = "List domain by domain ID.")
    private Long id;

    @Parameter(name = ApiConstants.LEVEL, type = CommandType.INTEGER, description = "List domains by domain level.")
    private Integer level;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, description = "List domain by domain name.")
    private String domainName;

    @Parameter(name = ApiConstants.LIST_ALL,
               type = CommandType.BOOLEAN,
               description = "If set to false, list only resources belonging to the command's caller; if set to true - list resources that the caller is authorized to see. Default value is false")
    private Boolean listAll;

    @Parameter(name = ApiConstants.DETAILS,
               type = CommandType.LIST,
               collectionType = CommandType.STRING,
               description = "comma separated list of domain details requested, value can be a list of [ all, resource, min]")
    private List<String> viewDetails;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public Integer getLevel() {
        return level;
    }

    public String getDomainName() {
        return domainName;
    }

    public boolean listAll() {
        return listAll == null ? false : listAll;
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

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public void execute() {
        ListResponse<DomainResponse> response = _queryService.searchForDomains(this);
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
    }
}
