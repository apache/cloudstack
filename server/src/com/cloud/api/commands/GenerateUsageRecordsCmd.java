/**
 *  Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
 */

package com.cloud.api.commands;

import java.util.Date;

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.BaseCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.SuccessResponse;
import com.cloud.server.ManagementServerExt;
import com.cloud.user.Account;

@Implementation(description="Generates usage records", responseObject=SuccessResponse.class)
public class GenerateUsageRecordsCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(GenerateUsageRecordsCmd.class.getName());

    private static final String s_name = "generateusagerecordsresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

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
