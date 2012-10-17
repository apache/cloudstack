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

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.BaseCmd;
import com.cloud.api.IdentityMapper;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.FindDomainResponse;
import com.cloud.domain.Domain;
import com.cloud.exception.InvalidParameterValueException;

@Implementation(description="Find account by ID", responseObject=FindDomainResponse.class)
public class FindDomainCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(FindDomainCmd.class.getName());

    private static final String s_name = "finddomainresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @IdentityMapper(entityTableName="domain")
    @Parameter(name = ApiConstants.ID, type = CommandType.LONG, description = "Id of the domain")
    private Long id;

    @Parameter(name = ApiConstants.DOMAIN, type = CommandType.STRING, description = "Path of the domain")
    private String domain;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

	public Long getId() {
		return id;
	}

	public String getDomain() {
		return domain;
	}
	
    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

	@Override
    public String getCommandName() {
        return s_name;
    }

	@Override
	public long getEntityOwnerId() {
		return 0;
	}
	
    @Override
    public void execute(){
    	Domain result = null;
    	if(getId() != null){
    		result = _domainService.getDomain(getId());	
    	} else if (getDomain() != null){
    		result = _domainService.findDomainByPath(getDomain());
    	}
        
        if(result != null){
        	FindDomainResponse response = _responseGenerator.createFindDomainResponse(result);
        	response.setResponseName(getCommandName());
        	this.setResponseObject(response);
        } else {
            throw new InvalidParameterValueException("Domain with specified Id does not exist");
        }
    }
}
