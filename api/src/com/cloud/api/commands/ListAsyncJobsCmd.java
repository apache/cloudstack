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
import com.cloud.api.BaseListAccountResourcesCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.AsyncJobResponse;
import com.cloud.api.response.ListResponse;
import com.cloud.async.AsyncJob;

@Implementation(description="Lists all pending asynchronous jobs for the account.", responseObject=AsyncJobResponse.class)
public class ListAsyncJobsCmd extends BaseListAccountResourcesCmd {
    private static final String s_name = "listasyncjobsresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.START_DATE, type=CommandType.TZDATE, description="the start date of the async job")
    private Date startDate;


    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

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
    public void execute(){
        List<? extends AsyncJob> result = _mgr.searchForAsyncJobs(this);
        ListResponse<AsyncJobResponse> response = new ListResponse<AsyncJobResponse>();
        List<AsyncJobResponse> jobResponses = new ArrayList<AsyncJobResponse>();
        for (AsyncJob job : result) {
            jobResponses.add(_responseGenerator.createAsyncJobResponse(job));
        }

        response.setResponses(jobResponses);
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
    }
}
