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

package org.apache.cloudstack.api.command.admin.region;

import org.apache.log4j.Logger;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCreateCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.PortableIpRangeResponse;
import org.apache.cloudstack.api.response.RegionResponse;
import org.apache.cloudstack.region.PortableIpRange;

import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.user.Account;

@APICommand(name = "createPortableIpRange",
            responseObject = PortableIpRangeResponse.class,
            description = "adds a range of portable public IP's to a region",
            since = "4.2.0",
            requestHasSensitiveInfo = false,
            responseHasSensitiveInfo = false)
public class CreatePortableIpRangeCmd extends BaseAsyncCreateCmd {

    public static final Logger s_logger = Logger.getLogger(CreatePortableIpRangeCmd.class.getName());

    private static final String s_name = "createportableiprangeresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.REGION_ID, type = CommandType.INTEGER, entityType = RegionResponse.class, required = true, description = "Id of the Region")
    private Integer regionId;

    @Parameter(name = ApiConstants.START_IP, type = CommandType.STRING, required = true, description = "the beginning IP address in the portable IP range")
    private String startIp;

    @Parameter(name = ApiConstants.END_IP, type = CommandType.STRING, required = true, description = "the ending IP address in the portable IP range")
    private String endIp;

    @Parameter(name = ApiConstants.GATEWAY, type = CommandType.STRING, required = true, description = "the gateway for the portable IP range")
    private String gateway;

    @Parameter(name = ApiConstants.NETMASK, type = CommandType.STRING, required = true, description = "the netmask of the portable IP range")
    private String netmask;

    @Parameter(name = ApiConstants.VLAN, type = CommandType.STRING, description = "VLAN id, if not specified defaulted to untagged")
    private String vlan;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Integer getRegionId() {
        return regionId;
    }

    public String getStartIp() {
        return startIp;
    }

    public String getEndIp() {
        return endIp;
    }

    public String getVlan() {
        return vlan;
    }

    public String getGateway() {
        return gateway;
    }

    public String getNetmask() {
        return netmask;
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
    public void execute() {
        PortableIpRange portableIpRange = _entityMgr.findById(PortableIpRange.class, getEntityId());
        if (portableIpRange != null) {
            PortableIpRangeResponse response = _responseGenerator.createPortableIPRangeResponse(portableIpRange);
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
        }
    }

    @Override
    public void create() throws ResourceAllocationException {
        try {
            PortableIpRange portableIpRange = _configService.createPortableIpRange(this);
            if (portableIpRange != null) {
                this.setEntityId(portableIpRange.getId());
                this.setEntityUuid(portableIpRange.getUuid());
            } else {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to create portable public IP range");
            }
        } catch (ConcurrentOperationException ex) {
            s_logger.warn("Exception: ", ex);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, ex.getMessage());
        }
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_PORTABLE_IP_RANGE_CREATE;
    }

    @Override
    public String getEventDescription() {
        return "creating a portable public ip range in region: " + getRegionId();
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.PortableIpAddress;
    }
}
