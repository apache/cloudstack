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

import com.cloud.api.ApiDBUtils;
import com.cloud.api.BaseCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ResponseObject;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.DomainResponse;
import com.cloud.domain.DomainVO;

@Implementation(method="createDomain")
public class CreateDomainCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(CreateDomainCmd.class.getName());

    private static final String s_name = "createdomainresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name="name", type=CommandType.STRING, required=true)
    private String domainName;

    @Parameter(name="parentdomainid", type=CommandType.LONG)
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

    @Override
    public ResponseObject getResponse() {
        DomainResponse response = new DomainResponse();
        DomainVO responseObject = (DomainVO)getResponseObject();
        if (responseObject != null) {
            response.setId(responseObject.getId());
            response.setDomainName(responseObject.getName());
            response.setLevel(responseObject.getLevel());
            response.setParentDomainId(responseObject.getParent());
            response.setParentDomainName(ApiDBUtils.findDomainById(responseObject.getParent()).getName());
        } else {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to create domain");
        }

        response.setResponseName(getName());
        return response;
    }
}
