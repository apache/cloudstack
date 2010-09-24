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
import com.cloud.api.BaseCmd.Manager;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ResponseObject;
import com.cloud.api.response.ResourceLimitResponse;
import com.cloud.configuration.ResourceLimitVO;
import com.cloud.user.Account;

@Implementation(method="updateResourceLimit", manager=Manager.AccountManager)
public class UpdateResourceLimitCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(UpdateResourceLimitCmd.class.getName());

    private static final String s_name = "updateresourcelimitresponse";


    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name="account", type=CommandType.STRING)
    private String accountName;

    @Parameter(name="domainid", type=CommandType.LONG)
    private Long domainId;

    @Parameter(name="max", type=CommandType.LONG)
    private Long max;

    @Parameter(name="resourcetype", type=CommandType.INTEGER, required=true)
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

    public Long getMax() {
        return max;
    }

    public Integer getResourceType() {
        return resourceType;
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
        ResourceLimitVO limit = (ResourceLimitVO) getResponseObject();

        ResourceLimitResponse response = new ResourceLimitResponse();
        if (limit.getDomainId() != null) {
            response.setDomainId(limit.getDomainId());
            response.setDomainName(ApiDBUtils.findDomainById(limit.getDomainId()).getName());
        }

        if (limit.getAccountId() != null) {
            Account accountTemp = ApiDBUtils.findAccountById(limit.getAccountId());
            if (accountTemp != null) {
                response.setAccountName(accountTemp.getAccountName());
                response.setDomainId(accountTemp.getDomainId());
                response.setDomainName(ApiDBUtils.findDomainById(accountTemp.getDomainId()).getName());
            }
        }
        response.setResourceType(limit.getType().ordinal());
        response.setMax(limit.getMax());

        response.setResponseName(getName());
        return response;
    }
}
