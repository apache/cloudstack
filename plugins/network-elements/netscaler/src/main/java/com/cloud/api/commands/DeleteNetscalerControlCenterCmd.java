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

package com.cloud.api.commands;

import javax.inject.Inject;
import javax.persistence.EntityExistsException;

import org.apache.log4j.Logger;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.context.CallContext;

import com.cloud.api.response.NetscalerControlCenterResponse;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.network.element.NetscalerLoadBalancerElementService;
import com.cloud.utils.exception.CloudRuntimeException;

@APICommand(name = "deleteNetscalerControlCenter", responseObject = SuccessResponse.class, description = "Delete Netscaler Control Center")
public class DeleteNetscalerControlCenterCmd extends BaseCmd {

    public static final Logger s_logger = Logger.getLogger(DeleteNetscalerControlCenterCmd.class.getName());
    private static final String s_name = "deleteNetscalerControlCenter";
    @Inject
    NetscalerLoadBalancerElementService _netsclarLbService;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID, type = CommandType.STRING, entityType = NetscalerControlCenterResponse.class, required = true, description = "Netscaler Control Center ID")
    private String ID;

    @Override
    public void execute() throws ServerApiException, ConcurrentOperationException, EntityExistsException {
        SuccessResponse response = new SuccessResponse();
        try {
            boolean result = _netsclarLbService.deleteNetscalerControlCenter(this);
            if (response != null && result) {
                response.setDisplayText("Netscaler Control Center Deleted Successfully");
                response.setSuccess(result);
                response.setResponseName(getCommandName());
                setResponseObject(response);
            }
        } catch (CloudRuntimeException runtimeExcp) {
            response.setDisplayText(runtimeExcp.getMessage());
            response.setSuccess(false);
            response.setResponseName(getCommandName());
            setResponseObject(response);
            return;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, e.getMessage());
        }
    }

    public String setId(String iD) {
        return ID = iD;
    }

    public String getId() {
        return ID;
    }

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccount().getId();
    }

}
