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
package org.apache.cloudstack.api.command.admin.vlan;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.VlanIpRangeResponse;
import org.apache.log4j.Logger;

import com.cloud.dc.Vlan;
import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.user.Account;
import com.cloud.utils.net.NetUtils;


@APICommand(name = UpdateVlanIpRangeCmd.APINAME, description = "Updates a VLAN IP range.", responseObject =
        VlanIpRangeResponse.class, since = "4.13.0",
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class UpdateVlanIpRangeCmd extends BaseAsyncCmd {

    public static final String APINAME = "updateVlanIpRange";
    public static final Logger s_logger = Logger.getLogger(UpdateVlanIpRangeCmd.class.getName());

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////


    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = VlanIpRangeResponse.class, required = true,
            description = "the UUID of the VLAN IP range")
    private Long id;

    @Parameter(name = ApiConstants.END_IP, type = CommandType.STRING, description = "the ending IP address in the VLAN IP range")
    private String endIp;

    @Parameter(name = ApiConstants.GATEWAY, type = CommandType.STRING, description = "the gateway of the VLAN IP range")
    private String gateway;

    @Parameter(name = ApiConstants.NETMASK, type = CommandType.STRING, description = "the netmask of the VLAN IP range")
    private String netmask;

    @Parameter(name = ApiConstants.START_IP, type = CommandType.STRING, description = "the beginning IP address in the VLAN IP range")
    private String startIp;

    @Parameter(name = ApiConstants.START_IPV6, type = CommandType.STRING, description = "the beginning IPv6 address in the IPv6 network range")
    private String startIpv6;

    @Parameter(name = ApiConstants.END_IPV6, type = CommandType.STRING, description = "the ending IPv6 address in the IPv6 network range")
    private String endIpv6;

    @Parameter(name = ApiConstants.VLAN, type = CommandType.INTEGER, description = "Optional. the vlan the ip range sits on")
    private Integer vlan;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////
    public Long getId() {
        return id;
    }
    public String getGateway() {
        return gateway;
    }
    public String getEndIp() {
        return endIp;
    }
    public String getNetmask() {
        return netmask;
    }
    public String getStartIp() {
        return startIp;
    }

    public String getStartIpv6() {
        if (startIpv6 == null) {
            return null;
        }
        return NetUtils.standardizeIp6Address(startIpv6);
    }

    public String getEndIpv6() {
        if (endIpv6 == null) {
            return null;
        }
        return NetUtils.standardizeIp6Address(endIpv6);
    }

    public Integer getVlan() {
        return vlan;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getEventType() {
        return EventTypes.EVENT_VLAN_IP_RANGE_UPDATE;
    }

    @Override
    public String getEventDescription() {
        return "Update vlan ip range " + getId() + " [StartIp=" + getStartIp() + ", EndIp=" + getEndIp() + ", vlan=" + getVlan() + ", netmask=" + getNetmask() + ']';
    }


    @Override
    public void execute() throws ResourceUnavailableException, ResourceAllocationException {
        try {
            Vlan result = _configService.updateVlanAndPublicIpRange(this);
            if (result != null) {
                VlanIpRangeResponse response = _responseGenerator.createVlanIpRangeResponse(result);
                response.setResponseName(getCommandName());
                this.setResponseObject(response);
            } else {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to Update vlan ip range");
            }
        } catch (ConcurrentOperationException ex) {
            s_logger.warn("Exception: ", ex);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, ex.getMessage());
        }
    }

    @Override
    public String getCommandName() {
        return APINAME.toLowerCase() + BaseCmd.RESPONSE_SUFFIX;
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }
}
