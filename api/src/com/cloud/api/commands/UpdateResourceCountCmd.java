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
import com.cloud.api.BaseCmd;
import com.cloud.api.IdentityMapper;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.ListResponse;
import com.cloud.api.response.ResourceCountResponse;
import com.cloud.configuration.ResourceCount;
import com.cloud.user.Account;
import com.cloud.user.UserContext;


@Implementation(description="Recalculate and update resource count for an account or domain.", responseObject=ResourceCountResponse.class)
public class UpdateResourceCountCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(UpdateResourceCountCmd.class.getName());

    private static final String s_name = "updateresourcecountresponse";


    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.ACCOUNT, type=CommandType.STRING, description="Update resource count for a specified account. Must be used with the domainId parameter.")
    private String accountName;

    @IdentityMapper(entityTableName="domain")
    @Parameter(name=ApiConstants.DOMAIN_ID, type=CommandType.LONG, required=true, description="If account parameter specified then updates resource counts for a specified account in this domain else update resource counts for all accounts & child domains in specified domain.")
    private Long domainId;

    @Parameter(name=ApiConstants.RESOURCE_TYPE, type=CommandType.INTEGER, description=  "Type of resource to update. If specifies valid values are 0, 1, 2, 3, and 4. If not specified will update all resource counts" +
    																					"0 - Instance. Number of instances a user can create. " +
    																					"1 - IP. Number of public IP addresses a user can own. " +
    																					"2 - Volume. Number of disk volumes a user can create." +
    																					"3 - Snapshot. Number of snapshots a user can create." +
    																					"4 - Template. Number of templates that a user can register/create.")
    private Integer resourceType;
    
    @IdentityMapper(entityTableName="projects")
    @Parameter(name=ApiConstants.PROJECT_ID, type=CommandType.LONG, description="Update resource limits for project")
    private Long projectId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getAccountName() {
        return accountName;
    }

    public Long getDomainId() {
        return domainId;
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
    public long getEntityOwnerId() {
        Account account = UserContext.current().getCaller();
        if ((account == null) || isAdmin(account.getType())) {
            if ((domainId != null) && (accountName != null)) {
                Account userAccount = _responseGenerator.findAccountByNameDomain(accountName, domainId);
                if (userAccount != null) {
                    return userAccount.getId();
                }
            }
        }

        if (account != null) {
            return account.getId();
        }

        return Account.ACCOUNT_ID_SYSTEM; // no account info given, parent this command to SYSTEM so ERROR events are tracked
    }

    @Override
    public void execute(){
    	List<? extends ResourceCount> result = _resourceLimitService.recalculateResourceCount(finalyzeAccountId(accountName, domainId, projectId, true), getDomainId(), getResourceType());

        if ((result != null) && (result.size()>0)){
            ListResponse<ResourceCountResponse> response = new ListResponse<ResourceCountResponse>();
            List<ResourceCountResponse> countResponses = new ArrayList<ResourceCountResponse>();

            for (ResourceCount count : result) {
                ResourceCountResponse resourceCountResponse = _responseGenerator.createResourceCountResponse(count);
                resourceCountResponse.setObjectName("resourcecount");
                countResponses.add(resourceCountResponse);
            }

            response.setResponses(countResponses);
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to recalculate resource counts");
        }
    }
}