//
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
//

package com.cloud.api.commands;

import com.cloud.api.response.NuageVspDeviceResponse;
import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.NuageVspDeviceVO;
import com.cloud.network.manager.NuageVspManager;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.PhysicalNetworkResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.log4j.Logger;

import javax.inject.Inject;

@APICommand(name = "updateNuageVspDevice", responseObject = NuageVspDeviceResponse.class, description = "Update a Nuage VSP device", since = "4.6")
public class UpdateNuageVspDeviceCmd extends BaseAsyncCmd {
    private static final Logger s_logger = Logger.getLogger(UpdateNuageVspDeviceCmd.class);
    private static final String s_name = "updatenuagevspdeviceresponse";

    @Inject
    NuageVspManager _nuageVspManager;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.PHYSICAL_NETWORK_ID, type = BaseCmd.CommandType.UUID, entityType = PhysicalNetworkResponse.class,
            required = true, description = "the ID of the physical network in to which Nuage VSP is added")
    private Long physicalNetworkId;

    @Parameter(name = VspConstants.NUAGE_VSP_API_PORT, type = CommandType.INTEGER, description = "the port to communicate to Nuage VSD")
    private Integer port;

    @Parameter(name = ApiConstants.HOST_NAME, type = CommandType.STRING, description = "the hostname of the Nuage VSD")
    private String hostName;

    @Parameter(name = ApiConstants.USERNAME, type = CommandType.STRING, description = "the user name of the CMS user in Nuage VSD")
    private String userName;

    @Parameter(name = ApiConstants.PASSWORD, type = CommandType.STRING, description = "the password of CMS user in Nuage VSD")
    private String password;

    @Parameter(name = VspConstants.NUAGE_VSP_API_VERSION, type = CommandType.STRING, description = "the version of the API to use to communicate to Nuage VSD")
    private String apiVersion;

    @Parameter(name = VspConstants.NUAGE_VSP_API_RETRY_COUNT, type = CommandType.INTEGER, description = "the number of retries on failure to communicate to Nuage VSD")
    private Integer apiRetryCount;

    @Parameter(name = VspConstants.NUAGE_VSP_API_RETRY_INTERVAL, type = CommandType.LONG, description = "the time to wait after failure before retrying to communicate to Nuage VSD")
    private Long apiRetryInterval;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getPhysicalNetworkId() {
        return physicalNetworkId;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public String getUserName() {
        return userName;
    }

    public String getPassword() {
        return password;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    public Integer getApiRetryCount() {
        return apiRetryCount;
    }

    public void setApiRetryCount(Integer apiRetryCount) {
        this.apiRetryCount = apiRetryCount;
    }

    public Long getApiRetryInterval() {
        return apiRetryInterval;
    }

    public void setApiRetryInterval(Long apiRetryInterval) {
        this.apiRetryInterval = apiRetryInterval;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException {
        try {
            NuageVspDeviceVO nuageVspDeviceVO = _nuageVspManager.updateNuageVspDevice(this);
            if (nuageVspDeviceVO != null) {
                NuageVspDeviceResponse response = _nuageVspManager.createNuageVspDeviceResponse(nuageVspDeviceVO);
                response.setObjectName("nuagevspdevice");
                response.setResponseName(getCommandName());
                this.setResponseObject(response);
            } else {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to add Nuage VSP device due to internal error.");
            }
        } catch (InvalidParameterValueException invalidParamExcp) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, invalidParamExcp.getMessage());
        } catch (CloudRuntimeException runtimeExcp) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, runtimeExcp.getMessage());
        }
    }

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccount().getId();
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_EXTERNAL_VSP_VSD_UPDATE;
    }

    @Override
    public String getEventDescription() {
        return "Updating a Nuage VSD";
    }
}