/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package com.cloud.api.commands;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.BaseListCmd;
import com.cloud.api.IdentityMapper;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.DomainResponse;
import com.cloud.api.response.ListResponse;
import com.cloud.domain.Domain;

@Implementation(description="Lists all children domains belonging to a specified domain", responseObject=DomainResponse.class)
public class ListDomainChildrenCmd extends BaseListCmd {
	public static final Logger s_logger = Logger.getLogger(ListDomainChildrenCmd.class.getName());

    private static final String s_name = "listdomainchildrenresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @IdentityMapper(entityTableName="domain")
    @Parameter(name=ApiConstants.ID, type=CommandType.LONG, description="list children domain by parent domain ID.")
    private Long id;

    @Parameter(name=ApiConstants.NAME, type=CommandType.STRING, description="list children domains by name")
    private String domainName;
    
    @Parameter(name=ApiConstants.IS_RECURSIVE, type=CommandType.BOOLEAN, description="to return the entire tree, use the value \"true\". To return the first level children, use the value \"false\".")
    private Boolean recursive;
    
    @Parameter(name=ApiConstants.LIST_ALL, type=CommandType.BOOLEAN, description="If set to false, list only resources belonging to the command's caller; if set to true - list resources that the caller is authorized to see. Default value is false")
    private Boolean listAll;

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

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }
    
    @Override
    public void execute(){
        List<? extends Domain> result = _domainService.searchForDomainChildren(this);
        ListResponse<DomainResponse> response = new ListResponse<DomainResponse>();
        List<DomainResponse> domainResponses = new ArrayList<DomainResponse>();
        for (Domain domain : result) {
            DomainResponse domainResponse = _responseGenerator.createDomainResponse(domain);
            domainResponse.setObjectName("domain");
            domainResponses.add(domainResponse);
        }

        response.setResponses(domainResponses);
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
    }
}
