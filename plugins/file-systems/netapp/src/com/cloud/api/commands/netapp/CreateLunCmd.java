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
package com.cloud.api.commands.netapp;

import java.rmi.ServerException;

import javax.inject.Inject;

import org.apache.log4j.Logger;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.netapp.NetappManager;
import com.cloud.server.api.response.netapp.CreateLunCmdResponse;

@APICommand(name = "createLunOnFiler", description = "Create a LUN from a pool", responseObject = CreateLunCmdResponse.class)
public class CreateLunCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(CreateLunCmd.class.getName());
    private static final String s_name = "createlunresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, required = true, description = "pool name.")
    private String poolName;

    @Parameter(name = ApiConstants.SIZE, type = CommandType.LONG, required = true, description = "LUN size.")
    private long size;

    public String getPoolName() {
        return poolName;
    }

    public long getLunSize() {
        return size;
    }

    @Inject
    NetappManager netappMgr;

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException,
        ResourceAllocationException {

        try {
            CreateLunCmdResponse response = new CreateLunCmdResponse();
            String returnVals[] = null;
            returnVals = netappMgr.createLunOnFiler(getPoolName(), getLunSize());
            response.setPath(returnVals[0]);
            response.setIqn(returnVals[1]);
            response.setIpAddress(returnVals[2]);
            response.setObjectName("lun");
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
        } catch (ServerException e) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, e.toString());
        } catch (InvalidParameterValueException e) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, e.toString());
        }

    }

    @Override
    public String getCommandName() {
        // TODO Auto-generated method stub
        return s_name;
    }

    @Override
    public long getEntityOwnerId() {
        // TODO Auto-generated method stub
        return 0;
    }

}
