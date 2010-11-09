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
import java.util.Date;
import java.util.List;

import com.cloud.api.ApiConstants;
import com.cloud.api.ApiSerializerHelper;
import com.cloud.api.BaseListCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ResponseObject;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.AsyncJobResponse;
import com.cloud.api.response.ListResponse;
import com.cloud.async.AsyncJobVO;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;

@Implementation(description="Lists all pending asynchronous jobs for the account.")
public class ListAsyncJobsCmd extends BaseListCmd {
    private static final String s_name = "listasyncjobsresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.ACCOUNT, type=CommandType.STRING, description="the account associated with the async job. Must be used with the domainId parameter.")
    private String accountName;

    @Parameter(name=ApiConstants.DOMAIN_ID, type=CommandType.LONG, description="the domain ID associated with the async job.  If used with the account parameter, returns async jobs for the account in the specified domain.")
    private Long domainId;

    @Parameter(name=ApiConstants.START_DATE, type=CommandType.TZDATE, description="the start date of the async job")
    private Date startDate;


    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getAccountName() {
        return accountName;
    }

    public Long getDomainId() {
        return domainId;
    }

    public Date getStartDate() {
        return startDate;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////
    @Override
    public String getName() {
        return s_name;
    }

    @Override
    public void execute() throws ServerApiException, InvalidParameterValueException, PermissionDeniedException, InsufficientAddressCapacityException, InsufficientCapacityException, ConcurrentOperationException{
        List<AsyncJobVO> result = _mgr.searchForAsyncJobs(this);
        ListResponse<AsyncJobResponse> response = new ListResponse<AsyncJobResponse>();
        List<AsyncJobResponse> jobResponses = new ArrayList<AsyncJobResponse>();
        for (AsyncJobVO job : result) {
            AsyncJobResponse jobResponse = new AsyncJobResponse();
            jobResponse.setAccountId(job.getAccountId());
            jobResponse.setCmd(job.getCmd());
            jobResponse.setCreated(job.getCreated());
            jobResponse.setId(job.getId());
            jobResponse.setJobInstanceId(job.getInstanceId());
            jobResponse.setJobInstanceType(job.getInstanceType());
            jobResponse.setJobProcStatus(job.getProcessStatus());
            jobResponse.setJobResult((ResponseObject)ApiSerializerHelper.fromSerializedString(job.getResult()));
            jobResponse.setJobResultCode(job.getResultCode());
            jobResponse.setJobStatus(job.getStatus());
            jobResponse.setUserId(job.getUserId());

            jobResponse.setObjectName("asyncjobs");
            jobResponses.add(jobResponse);
        }

        response.setResponses(jobResponses);
        response.setResponseName(getName());
        this.setResponseObject(response);
    }
}
