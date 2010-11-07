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

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.ApiResponseHelper;
import com.cloud.api.BaseCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.DomainResponse;
import com.cloud.domain.DomainVO;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;

@Implementation(method="createDomain", description="Creates a domain")
public class CreateDomainCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(CreateDomainCmd.class.getName());

    private static final String s_name = "createdomainresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.NAME, type=CommandType.STRING, required=true, description="creates domain with this name")
    private String domainName;

    @Parameter(name=ApiConstants.PARENT_DOMAIN_ID, type=CommandType.LONG, description="assigns new domain a parent domain by domain ID of the parent.  If no parent domain is specied, the ROOT domain is assumed.")
    private Long parentDomainId;


    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getDomainName() {
        return domainName;
    }

    public Long getParentDomainId() {
        return parentDomainId;
    }


    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getName() {
        return s_name;
    }

    @Override @SuppressWarnings("unchecked")
    public DomainResponse getResponse() {
        DomainVO domain = (DomainVO)getResponseObject();
        if (domain != null) {
            DomainResponse response = ApiResponseHelper.createDomainResponse(domain);
            response.setResponseName(getName());
            return response;
        } else {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to create domain");
        }
    }
    
    @Override
    public Object execute() throws ServerApiException, InvalidParameterValueException, PermissionDeniedException, InsufficientAddressCapacityException, InsufficientCapacityException, ConcurrentOperationException{
        DomainVO domain = _mgr.createDomain(this);
        return domain;
        //set response object here; change return type to void
    }
}
