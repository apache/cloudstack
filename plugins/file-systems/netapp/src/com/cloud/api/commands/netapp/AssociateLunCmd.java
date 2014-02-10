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

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.netapp.NetappManager;
import com.cloud.server.api.response.netapp.AssociateLunCmdResponse;

@APICommand(name = "associateLun", description = "Associate a LUN with a guest IQN", responseObject = AssociateLunCmdResponse.class)
public class AssociateLunCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(AssociateLunCmd.class.getName());
    private static final String s_name = "associatelunresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, required = true, description = "LUN name.")
    private String lunName;

    @Parameter(name = ApiConstants.IQN, type = CommandType.STRING, required = true, description = "Guest IQN to which the LUN associate.")
    private String guestIqn;

    ///////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getLunName() {
        return lunName;
    }

    public String getGuestIQN() {
        return guestIqn;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Inject
    NetappManager netappMgr;

    @Override
    public void execute() {

        try {
            AssociateLunCmdResponse response = new AssociateLunCmdResponse();
            String returnVals[] = null;
            returnVals = netappMgr.associateLun(getGuestIQN(), getLunName());
            response.setLun(returnVals[0]);
            response.setIpAddress(returnVals[2]);
            response.setTargetIQN(returnVals[1]);
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
    public long getEntityOwnerId() {
        // TODO Auto-generated method stub
        return 0;
    }

}
