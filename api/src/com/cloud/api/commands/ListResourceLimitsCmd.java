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
import com.cloud.api.response.ListResponse;
import com.cloud.api.response.ResourceLimitResponse;
import com.cloud.configuration.ResourceLimit;

@Implementation(description="Lists resource limits.", responseObject=ResourceLimitResponse.class)
public class ListResourceLimitsCmd extends BaseListCmd {
    public static final Logger s_logger = Logger.getLogger(ListResourceLimitsCmd.class.getName());

    private static final String s_name = "listresourcelimitsresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.ACCOUNT, type=CommandType.STRING, description="Lists resource limits by account. Must be used with the domainId parameter.")
    private String accountName;
    
    @IdentityMapper(entityTableName="projects")
    @Parameter(name=ApiConstants.PROJECT_ID, type=CommandType.LONG, description="Lists resource limits by project")
    private Long projectId;

    @IdentityMapper(entityTableName="domain")
    @Parameter(name=ApiConstants.DOMAIN_ID, type=CommandType.LONG, description="Lists resource limits by domain ID. If used with the account parameter, lists resource limits for a specified account in a specified domain.")
    private Long domainId;

    @Parameter(name=ApiConstants.ID, type=CommandType.LONG, description="Lists resource limits by ID.")
    private Long id;

    @Parameter(name=ApiConstants.RESOURCE_TYPE, type=CommandType.INTEGER, description="Type of resource to update. Values are 0, 1, 2, 3, and 4. 0 - Instance. Number of instances a user can create. " +
																						"1 - IP. Number of public IP addresses a user can own. " +
																						"2 - Volume. Number of disk volumes a user can create." +
																						"3 - Snapshot. Number of snapshots a user can create." +
																						"4 - Template. Number of templates that a user can register/create.")
	private Integer resourceType;
			
    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getAccountName() {
        return accountName;
    }

    public Long getDomainId() {
        return domainId;
    }

    public Long getId() {
        return id;
    }

    public Integer getResourceType() {
        return resourceType;
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
        List<? extends ResourceLimit> result = _resourceLimitService.searchForLimits(id, getAccountId(accountName, domainId, projectId, false), domainId, resourceType, this.getStartIndex(), this.getPageSizeVal());
        ListResponse<ResourceLimitResponse> response = new ListResponse<ResourceLimitResponse>();
        List<ResourceLimitResponse> limitResponses = new ArrayList<ResourceLimitResponse>();
        for (ResourceLimit limit : result) {
            ResourceLimitResponse resourceLimitResponse = _responseGenerator.createResourceLimitResponse(limit);
            resourceLimitResponse.setObjectName("resourcelimit");
            limitResponses.add(resourceLimitResponse);
        }

        response.setResponses(limitResponses);
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
    }
}
