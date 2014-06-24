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
import com.cloud.exception.ResourceInUseException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.netapp.NetappManager;
import com.cloud.server.api.response.netapp.DeleteVolumeOnFilerCmdResponse;

@APICommand(name = "destroyVolumeOnFiler", description = "Destroy a Volume", responseObject = DeleteVolumeOnFilerCmdResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class DestroyVolumeOnFilerCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(DestroyVolumeOnFilerCmd.class.getName());
    private static final String s_name = "destroyvolumeresponse";

    @Parameter(name = ApiConstants.AGGREGATE_NAME, type = CommandType.STRING, required = true, description = "aggregate name.")
    private String aggrName;

    @Parameter(name = ApiConstants.IP_ADDRESS, type = CommandType.STRING, required = true, description = "ip address.")
    private String ipAddr;

    @Parameter(name = ApiConstants.VOLUME_NAME, type = CommandType.STRING, required = true, description = "volume name.")
    private String volumeName;

    @Inject
    NetappManager netappMgr;

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException,
        ResourceAllocationException {
        try {
            netappMgr.destroyVolumeOnFiler(ipAddr, aggrName, volumeName);
            DeleteVolumeOnFilerCmdResponse response = new DeleteVolumeOnFilerCmdResponse();
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
        } catch (InvalidParameterValueException e) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, e.toString());
        } catch (ResourceInUseException e) {
            throw new ServerApiException(ApiErrorCode.RESOURCE_IN_USE_ERROR, e.toString());
        } catch (ServerException e) {
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