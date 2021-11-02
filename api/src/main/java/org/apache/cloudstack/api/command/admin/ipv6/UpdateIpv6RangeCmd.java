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
package org.apache.cloudstack.api.command.admin.ipv6;

import com.cloud.network.Ipv6Address;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.Ipv6RangeResponse;
import org.apache.log4j.Logger;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.user.Account;
import com.cloud.utils.net.NetUtils;

@APICommand(name = UpdateIpv6RangeCmd.APINAME, description = "Updates a VLAN IP range.", responseObject =
        Ipv6RangeResponse.class, since = "4.16.0",
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false,
        authorized = {RoleType.Admin})
public class UpdateIpv6RangeCmd extends BaseCmd {

    public static final String APINAME = "updateIpv6Range";
    public static final Logger s_logger = Logger.getLogger(UpdateIpv6RangeCmd.class.getName());

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = Ipv6RangeResponse.class, required = true,
            description = "the UUID of the IPv6 range")
    private Long id;

    @Parameter(name = ApiConstants.IP6_GATEWAY, type = CommandType.STRING, required = true, description = "the gateway of the IPv6 network")
    private String ip6Gateway;

    @Parameter(name = ApiConstants.IP6_CIDR, type = CommandType.STRING, required = true, description = "the CIDR of IPv6 network, must be at least /64")
    private String ip6Cidr;

    @Parameter(name = ApiConstants.ROUTER_IPV6, type = CommandType.STRING, required = false, description = "the Outbound IPv6 address of virtual router")
    private String routerIpv6;

    @Parameter(name = ApiConstants.ROUTER_IPV6_GATEWAY, type = CommandType.STRING, required = true, description = "the gateway of Outbound IPv6 network of virtual router")
    private String routerIpv6Gateway;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////
    public Long getId() {
        return id;
    }

    public String getIp6Gateway() {
        if (ip6Gateway == null) {
            return null;
        }
        return NetUtils.standardizeIp6Address(ip6Gateway);
    }

    public String getIp6Cidr() {
        if (ip6Cidr == null) {
            return null;
        }
        return NetUtils.standardizeIp6Cidr(ip6Cidr);
    }

    public String getRouterIpv6() {
        if (routerIpv6 == null) {
            return null;
        }
        return NetUtils.standardizeIp6Address(routerIpv6);
    }

    public String getRouterIpv6Gateway() {
        if (routerIpv6Gateway == null) {
            return null;
        }
        return NetUtils.standardizeIp6Address(routerIpv6Gateway);
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() throws ResourceUnavailableException, ResourceAllocationException {
        try {
            Ipv6Address result = ipv6Service.updateIpv6Range(this);
            if (result != null) {
                Ipv6RangeResponse response = ipv6Service.createIpv6RangeResponse(result);
                response.setResponseName(getCommandName());
                this.setResponseObject(response);
            } else {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to Update ipv6 range");
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