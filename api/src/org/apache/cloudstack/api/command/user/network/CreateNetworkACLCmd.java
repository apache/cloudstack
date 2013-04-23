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
package org.apache.cloudstack.api.command.user.network;

import java.util.ArrayList;
import java.util.List;

import com.cloud.network.vpc.NetworkACL;
import com.cloud.network.vpc.NetworkACLItem;
import com.cloud.network.vpc.NetworkACLItem.NetworkACLType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.BaseAsyncCreateCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.NetworkACLItemResponse;
import org.apache.cloudstack.api.response.NetworkACLResponse;
import org.apache.cloudstack.api.response.NetworkResponse;
import org.apache.log4j.Logger;

import com.cloud.async.AsyncJob;
import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network;
import com.cloud.network.vpc.Vpc;
import com.cloud.user.Account;
import com.cloud.user.UserContext;
import com.cloud.utils.net.NetUtils;

@APICommand(name = "createNetworkACL", description = "Creates a ACL rule the given network (the network has to belong to VPC)",
responseObject = NetworkACLItemResponse.class)
public class CreateNetworkACLCmd extends BaseAsyncCreateCmd {
    public static final Logger s_logger = Logger.getLogger(CreateNetworkACLCmd.class.getName());

    private static final String s_name = "createnetworkaclresponse";

    // ///////////////////////////////////////////////////
    // ////////////// API parameters /////////////////////
    // ///////////////////////////////////////////////////

    @Parameter(name = ApiConstants.PROTOCOL, type = CommandType.STRING, required = true, description =
            "the protocol for the ACL rule. Valid values are TCP/UDP/ICMP.")
    private String protocol;

    @Parameter(name = ApiConstants.START_PORT, type = CommandType.INTEGER, description = "the starting port of ACL")
    private Integer publicStartPort;

    @Parameter(name = ApiConstants.END_PORT, type = CommandType.INTEGER, description = "the ending port of ACL")
    private Integer publicEndPort;

    @Parameter(name = ApiConstants.CIDR_LIST, type = CommandType.LIST, collectionType = CommandType.STRING,
            description = "the cidr list to allow traffic from/to")
    private List<String> cidrlist;

    @Parameter(name = ApiConstants.ICMP_TYPE, type = CommandType.INTEGER, description = "type of the icmp message being sent")
    private Integer icmpType;

    @Parameter(name = ApiConstants.ICMP_CODE, type = CommandType.INTEGER, description = "error code for this icmp message")
    private Integer icmpCode;

    @Parameter(name=ApiConstants.NETWORK_ID, type=CommandType.UUID, entityType = NetworkResponse.class,
        description="The network of the vm the ACL will be created for")
    private Long networkId;

    @Parameter(name=ApiConstants.ACL_ID, type=CommandType.UUID, entityType = NetworkACLResponse.class,
            description="The network of the vm the ACL will be created for")
    private Long aclId;

    @Parameter(name=ApiConstants.TRAFFIC_TYPE, type=CommandType.STRING, description="the traffic type for the ACL," +
            "can be Ingress or Egress, defaulted to Ingress if not specified")
    private String trafficType;

    // ///////////////////////////////////////////////////
    // ///////////////// Accessors ///////////////////////
    // ///////////////////////////////////////////////////

    public Long getIpAddressId() {
        return null;
    }

    public String getProtocol() {
        return protocol.trim();
    }

    public List<String> getSourceCidrList() {
        if (cidrlist != null) {
            return cidrlist;
        } else {
            List<String> oneCidrList = new ArrayList<String>();
            oneCidrList.add(NetUtils.ALL_CIDRS);
            return oneCidrList;
        }
    }

    public long getVpcId() {
        Long vpcId = null;

        if(getACLId() != null){
            NetworkACL acl = _networkACLService.getNetworkACL(getACLId());
            if(acl == null){
                throw new InvalidParameterValueException("Invalid aclId is given");
            }
            vpcId = acl.getVpcId();
        } else if(getNetworkId() != null){
            Network network = _networkService.getNetwork(getNetworkId());
            if (network == null) {
                throw new InvalidParameterValueException("Invalid networkId is given");
            }
            vpcId = network.getVpcId();
        }

        if (vpcId == null) {
            throw new InvalidParameterValueException("Can create network ACL only for the ACL belonging to the VPC");
        }

        return vpcId;
    }

    public NetworkACLItem.TrafficType getTrafficType() {
        if (trafficType == null) {
            return NetworkACLItem.TrafficType.Ingress;
        }
        for (NetworkACLItem.TrafficType type : NetworkACLItem.TrafficType.values()) {
            if (type.toString().equalsIgnoreCase(trafficType)) {
                return type;
            }
        }
        throw new InvalidParameterValueException("Invalid traffic type " + trafficType);
    }

    // ///////////////////////////////////////////////////
    // ///////////// API Implementation///////////////////
    // ///////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    public void setSourceCidrList(List<String> cidrs){
        cidrlist = cidrs;
    }

    @Override
    public void execute() throws ResourceUnavailableException {
        UserContext callerContext = UserContext.current();
        boolean success = false;
        NetworkACLItem rule = _networkACLService.getNetworkACLItem(getEntityId());
        try {
            UserContext.current().setEventDetails("Rule Id: " + getEntityId());
            success = _networkACLService.applyNetworkACLtoNetworks(rule.getACLId(), callerContext.getCaller());

            // State is different after the rule is applied, so get new object here
            NetworkACLItemResponse aclResponse = new NetworkACLItemResponse();
            if (rule != null) {
                aclResponse = _responseGenerator.createNetworkACLItemResponse(rule);
                setResponseObject(aclResponse);
            }
            aclResponse.setResponseName(getCommandName());
        } finally {
            if (!success || rule == null) {
                _networkACLService.revokeNetworkACLItem(getEntityId(), true);
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to create network ACL");
            }
        }
    }

    public Long getSourceIpAddressId() {
        return null;
    }

    public Integer getSourcePortStart() {
        if (publicStartPort != null) {
            return publicStartPort.intValue();
        }
        return null;
    }

    public Integer getSourcePortEnd() {
        if (publicEndPort == null) {
            if (publicStartPort != null) {
                return publicStartPort.intValue();
            }
        } else {
            return publicEndPort.intValue();
        }

        return null;
    }

    public NetworkACLItem.State getState() {
        throw new UnsupportedOperationException("Should never call me to find the state");
    }

    public Long getNetworkId() {
        return networkId;
    }

    @Override
    public long getEntityOwnerId() {
        Vpc vpc = _vpcService.getVpc(getVpcId());
        if (vpc == null) {
            throw new InvalidParameterValueException("Invalid vpcId is given");
        }

        Account account = _accountService.getAccount(vpc.getAccountId());
        return account.getId();
    }

    public long getDomainId() {
        Vpc vpc = _vpcService.getVpc(getVpcId());
        return vpc.getDomainId();
    }

    @Override
    public void create() {
        if (getSourceCidrList() != null) {
            for (String cidr: getSourceCidrList()){
                if (!NetUtils.isValidCIDR(cidr)){
                    throw new ServerApiException(ApiErrorCode.PARAM_ERROR, "Source cidrs formatting error " + cidr);
                }
            }
        }

        try {
            NetworkACLItem result = _networkACLService.createNetworkACLItem(this);
            setEntityId(result.getId());
            setEntityUuid(result.getUuid());
        } catch (NetworkRuleConflictException ex) {
            s_logger.info("Network rule conflict: " + ex.getMessage());
            s_logger.trace("Network Rule Conflict: ", ex);
            throw new ServerApiException(ApiErrorCode.NETWORK_RULE_CONFLICT_ERROR, ex.getMessage());
        }
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_NETWORK_ACL_ITEM_CREATE;
    }

    @Override
    public String getEventDescription() {
        //Network network = _networkService.getNetwork(networkId);
        return ("Creating Network ACL Item for protocol:" + this.getProtocol());
    }

    public long getAccountId() {
        Vpc vpc = _vpcService.getVpc(getVpcId());
        return vpc.getAccountId();
    }

    @Override
    public String getSyncObjType() {
        return BaseAsyncCmd.networkSyncObject;
    }

    @Override
    public Long getSyncObjId() {
        return getNetworkId();
    }

    public Integer getIcmpCode() {
        if (icmpCode != null) {
            return icmpCode;
        } else if (protocol.equalsIgnoreCase(NetUtils.ICMP_PROTO)) {
            return -1;
        }
        return null;
    }

    public Integer getIcmpType() {
        if (icmpType != null) {
            return icmpType;
        } else if (protocol.equalsIgnoreCase(NetUtils.ICMP_PROTO)) {
                return -1;

        }
        return null;
    }

    public NetworkACLType getType() {
        return NetworkACLType.User;
    }

    @Override
    public AsyncJob.Type getInstanceType() {
        return AsyncJob.Type.FirewallRule;
    }

    public Long getACLId() {
        return aclId;
    }
}
