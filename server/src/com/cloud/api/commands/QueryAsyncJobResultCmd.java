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

import java.util.Date;

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.ApiSerializerHelper;
import com.cloud.api.BaseCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ResponseObject;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.AsyncJobResponse;
import com.cloud.async.AsyncJobResult;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;

@Implementation(description="Retrieves the current status of asynchronous job.", responseObject=AsyncJobResponse.class)
public class QueryAsyncJobResultCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(QueryAsyncJobResultCmd.class.getName());

    private static final String s_name = "queryasyncjobresultresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.JOB_ID, type=CommandType.LONG, required=true, description="the ID of the asychronous job")
    private Long id;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
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
        AsyncJobResult result = _asyncMgr.queryAsyncJobResult(this);
        AsyncJobResponse response = new AsyncJobResponse();
        response.setId(result.getJobId());
        response.setJobStatus(result.getJobStatus());
        response.setJobProcStatus(result.getProcessStatus());
        response.setJobResultCode(result.getResultCode());
        response.setJobResult((ResponseObject)ApiSerializerHelper.fromSerializedString(result.getResult()));

        Object resultObject = result.getResultObject();
        if (resultObject != null) {
            Class<?> clz = resultObject.getClass();
            if(clz.isPrimitive() || clz.getSuperclass() == Number.class || clz == String.class || clz == Date.class) {
                response.setJobResultType("text");
            } else {
                response.setJobResultType("object");
            }
        }

        response.setResponseName(getName());
        this.setResponseObject(response);
    }
}
