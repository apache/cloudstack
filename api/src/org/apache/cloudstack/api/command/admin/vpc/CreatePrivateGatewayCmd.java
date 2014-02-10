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
package org.apache.cloudstack.api.command.admin.vpc;

import org.apache.log4j.Logger;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandJobType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.BaseAsyncCreateCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.NetworkACLResponse;
import org.apache.cloudstack.api.response.NetworkOfferingResponse;
import org.apache.cloudstack.api.response.PhysicalNetworkResponse;
import org.apache.cloudstack.api.response.PrivateGatewayResponse;
import org.apache.cloudstack.api.response.VpcResponse;

import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.vpc.PrivateGateway;
import com.cloud.network.vpc.Vpc;
import com.cloud.user.Account;

@APICommand(name = "createPrivateGateway", description = "Creates a private gateway", responseObject = PrivateGatewayResponse.class)
public class CreatePrivateGatewayCmd extends BaseAsyncCreateCmd {
    public static final Logger s_logger = Logger.getLogger(CreatePrivateGatewayCmd.class.getName());

    private static final String s_name = "createprivategatewayresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.PHYSICAL_NETWORK_ID,
               type = CommandType.UUID,
               entityType = PhysicalNetworkResponse.class,
               description = "the Physical Network ID the network belongs to")
    private Long physicalNetworkId;

    @Parameter(name = ApiConstants.GATEWAY, type = CommandType.STRING, required = true, description = "the gateway of the Private gateway")
    private String gateway;

    @Parameter(name = ApiConstants.NETMASK, type = CommandType.STRING, required = true, description = "the netmask of the Private gateway")
    private String netmask;

    @Parameter(name = ApiConstants.IP_ADDRESS, type = CommandType.STRING, required = true, description = "the IP address of the Private gateaway")
    private String ipAddress;

    @Parameter(name = ApiConstants.VLAN, type = CommandType.STRING, required = true, description = "the network implementation uri for the private gateway")
    private String broadcastUri;

    @Parameter(name = ApiConstants.NETWORK_OFFERING_ID,
               type = CommandType.UUID,
               required = false,
               entityType = NetworkOfferingResponse.class,
               description = "the uuid of the network offering to use for the private gateways network connection")
    private Long networkOfferingId;

    @Parameter(name = ApiConstants.VPC_ID, type = CommandType.UUID, entityType = VpcResponse.class, required = true, description = "the VPC network belongs to")
    private Long vpcId;

    @Parameter(name = ApiConstants.SOURCE_NAT_SUPPORTED,
               type = CommandType.BOOLEAN,
               required = false,
               description = "source NAT supported value. Default value false. If 'true' source NAT is enabled on the private gateway"
                   + " 'false': sourcenat is not supported")
    private Boolean isSourceNat;

    @Parameter(name = ApiConstants.ACL_ID, type = CommandType.UUID, entityType = NetworkACLResponse.class, required = false, description = "the ID of the network ACL")
    private Long aclId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getGateway() {
        return gateway;
    }

    public String getBroadcastUri() {
        return broadcastUri;
    }

    public String getNetmask() {
        return netmask;
    }

    public String getStartIp() {
        return ipAddress;
    }

    public Long getPhysicalNetworkId() {
        return physicalNetworkId;
    }

    private Long getNetworkOfferingId() {
        return networkOfferingId;
    }

    public Long getVpcId() {
        return vpcId;
    }

    public Boolean getIsSourceNat() {
        if (isSourceNat == null) {
            return false;
        }
        return isSourceNat;
    }

    public Long getAclId() {
        return aclId;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////
    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public void create() throws ResourceAllocationException {
        PrivateGateway result = null;
        try {
            result =
                _vpcService.createVpcPrivateGateway(getVpcId(), getPhysicalNetworkId(), getBroadcastUri(), getStartIp(), getGateway(), getNetmask(), getEntityOwnerId(),
                    getNetworkOfferingId(), getIsSourceNat(), getAclId());
        } catch (InsufficientCapacityException ex) {
            s_logger.info(ex);
            s_logger.trace(ex);
            throw new ServerApiException(ApiErrorCode.INSUFFICIENT_CAPACITY_ERROR, ex.getMessage());
        } catch (ConcurrentOperationException ex) {
            s_logger.warn("Exception: ", ex);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, ex.getMessage());
        }

        if (result != null) {
            setEntityId(result.getId());
            setEntityUuid(result.getUuid());
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to create private gateway");
        }
    }

    @Override
    public void execute() throws InsufficientCapacityException, ConcurrentOperationException, ResourceAllocationException, ResourceUnavailableException {
        PrivateGateway result = _vpcService.applyVpcPrivateGateway(getEntityId(), true);
        if (result != null) {
            PrivateGatewayResponse response = _responseGenerator.createPrivateGatewayResponse(result);
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to create private gateway");
        }
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_PRIVATE_GATEWAY_CREATE;
    }

    @Override
    public String getEventDescription() {
        return "creating private gateway";
    }

    @Override
    public String getSyncObjType() {
        return BaseAsyncCmd.vpcSyncObject;
    }

    @Override
    public Long getSyncObjId() {
        Vpc vpc = _entityMgr.findById(Vpc.class, vpcId);
        if (vpc == null) {
            throw new InvalidParameterValueException("Invalid id is specified for the vpc");
        }
        return vpc.getId();
    }

    @Override
    public ApiCommandJobType getInstanceType() {
        return ApiCommandJobType.PrivateGateway;
    }
}
