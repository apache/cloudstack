/**
 * *  Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved
*
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

import java.util.Date;

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.BaseCmd;
import com.cloud.api.IdentityMapper;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.SuccessResponse;
import com.cloud.server.ManagementServerExt;
import com.cloud.user.Account;

@Implementation(description="Generates usage records. This will generate records only if there any records to be generated, i.e if the scheduled usage job was not run or failed", responseObject=SuccessResponse.class)
public class GenerateUsageRecordsCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(GenerateUsageRecordsCmd.class.getName());

    private static final String s_name = "generateusagerecordsresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @IdentityMapper(entityTableName="domain")
    @Parameter(name=ApiConstants.DOMAIN_ID, type=CommandType.LONG, description="List events for the specified domain.")
    private Long domainId;

    @Parameter(name=ApiConstants.END_DATE, type=CommandType.DATE, required=true, description="End date range for usage record query. Use yyyy-MM-dd as the date format, e.g. startDate=2009-06-03.")
    private Date endDate;

    @Parameter(name=ApiConstants.START_DATE, type=CommandType.DATE, required=true, description="Start date range for usage record query. Use yyyy-MM-dd as the date format, e.g. startDate=2009-06-01.")
    private Date startDate;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getDomainId() {
        return domainId;
    }

    public Date getEndDate() {
        return endDate;
    }

    public Date getStartDate() {
        return startDate;
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
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public void execute(){
        ManagementServerExt _mgrExt = (ManagementServerExt)_mgr;
        boolean result = _mgrExt.generateUsageRecords(this);
        if (result) {
            SuccessResponse response = new SuccessResponse(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to generate usage records");
        }
    }
}
