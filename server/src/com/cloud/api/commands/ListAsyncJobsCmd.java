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

import com.cloud.api.BaseListCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.ApiResponseSerializer;
import com.cloud.api.response.AsyncJobResponse;
import com.cloud.api.response.ListResponse;
import com.cloud.async.AsyncJobVO;

@Implementation(method="searchForAsyncJobs")
public class ListAsyncJobsCmd extends BaseListCmd {
    private static final String s_name = "listasyncjobsresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name="account", type=CommandType.STRING)
    private String accountName;

    @Parameter(name="domainid", type=CommandType.LONG)
    private Long domainId;

    @Parameter(name="startdate", type=CommandType.TZDATE)
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

    @Override @SuppressWarnings("unchecked")
    public String getResponse() {
        List<AsyncJobVO> jobs = (List<AsyncJobVO>)getResponseObject();

        ListResponse response = new ListResponse();
        List<AsyncJobResponse> jobResponses = new ArrayList<AsyncJobResponse>();
        for (AsyncJobVO job : jobs) {
            AsyncJobResponse jobResponse = new AsyncJobResponse();
            jobResponse.setAccountId(job.getAccountId());
            jobResponse.setCmd(job.getCmd());
            jobResponse.setCreated(job.getCreated());
            jobResponse.setId(job.getId());
            jobResponse.setJobInstanceId(job.getInstanceId());
            jobResponse.setJobInstanceType(job.getInstanceType());
            jobResponse.setJobProcStatus(job.getProcessStatus());
            jobResponse.setJobResult(job.getResult());
            jobResponse.setJobResultCode(job.getResultCode());
            jobResponse.setJobStatus(job.getStatus());
            jobResponse.setUserId(job.getUserId());

            jobResponse.setResponseName("asyncjobs");
            jobResponses.add(jobResponse);
        }

        response.setResponses(jobResponses);
        response.setResponseName(getName());
        return ApiResponseSerializer.toSerializedString(response);
    }
}
