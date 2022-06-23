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

import java.util.List;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseAsyncCustomIdCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.NetworkACLItemResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.log4j.Logger;

import com.cloud.event.EventTypes;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.vpc.NetworkACLItem;
import com.cloud.user.Account;

@APICommand(name = "updateNetworkACLItem", description = "Updates ACL item with specified ID", responseObject = NetworkACLItemResponse.class, requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class UpdateNetworkACLItemCmd extends BaseAsyncCustomIdCmd {
    public static final Logger s_logger = Logger.getLogger(UpdateNetworkACLItemCmd.class.getName());

    private static final String s_name = "createnetworkaclresponse";

    // ///////////////////////////////////////////////////
    // ////////////// API parameters /////////////////////
    // ///////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = NetworkACLItemResponse.class, required = true, description = "the ID of the network ACL item")
    private Long id;

    @Parameter(name = ApiConstants.PROTOCOL, type = CommandType.STRING, description = "the protocol for the ACL rule. Valid values are TCP/UDP/ICMP/ALL or valid protocol number")
    private String protocol;

    @Parameter(name = ApiConstants.START_PORT, type = CommandType.INTEGER, description = "the starting port of ACL")
    private Integer publicStartPort;

    @Parameter(name = ApiConstants.END_PORT, type = CommandType.INTEGER, description = "the ending port of ACL")
    private Integer publicEndPort;

    @Parameter(name = ApiConstants.CIDR_LIST, type = CommandType.LIST, collectionType = CommandType.STRING, description = "the cidr list to allow traffic from/to. Multiple entries must be separated by a single comma character (,).")
    private List<String> cidrlist;

    @Parameter(name = ApiConstants.ICMP_TYPE, type = CommandType.INTEGER, description = "type of the ICMP message being sent")
    private Integer icmpType;

    @Parameter(name = ApiConstants.ICMP_CODE, type = CommandType.INTEGER, description = "error code for this ICMP message")
    private Integer icmpCode;

    @Parameter(name = ApiConstants.TRAFFIC_TYPE, type = CommandType.STRING, description = "the traffic type for the ACL, can be Ingress or Egress, defaulted to Ingress if not specified")
    private String trafficType;

    @Parameter(name = ApiConstants.NUMBER, type = CommandType.INTEGER, description = "The network of the vm the ACL will be created for")
    private Integer number;

    @Parameter(name = ApiConstants.ACTION, type = CommandType.STRING, description = "scl entry action, allow or deny")
    private String action;

    @Parameter(name = ApiConstants.FOR_DISPLAY, type = CommandType.BOOLEAN, description = "an optional field, whether to the display the rule to the end user or not", since = "4.4", authorized = {
            RoleType.Admin})
    private Boolean display;

    @Parameter(name = ApiConstants.ACL_REASON, type = CommandType.STRING, description = "A description indicating why the ACL rule is required.")
    private String reason;

    @Parameter(name = ApiConstants.ACL_RULE_PARTIAL_UPGRADE, type = CommandType.BOOLEAN, required = false, description = "Indicates if the ACL rule is to be updated partially (merging the parameters sent with current configuration) or completely (disconsidering all of the current configurations). The default value is 'true'.")
    private boolean partialUpgrade = true;

    // ///////////////////////////////////////////////////
    // ///////////////// Accessors ///////////////////////
    // ///////////////////////////////////////////////////

    @Override
    public boolean isDisplay() {
        if (display != null) {
            return display;
        } else {
            return true;
        }
    }

    public Long getId() {
        return id;
    }

    public String getProtocol() {
        if (protocol != null) {
            return protocol.trim();
        } else {
            return null;
        }
    }

    public List<String> getSourceCidrList() {
        return cidrlist;
    }

    public NetworkACLItem.TrafficType getTrafficType() {
        if (trafficType != null) {
            for (NetworkACLItem.TrafficType type : NetworkACLItem.TrafficType.values()) {
                if (type.toString().equalsIgnoreCase(trafficType)) {
                    return type;
                }
            }
        }
        return null;
    }

    // ///////////////////////////////////////////////////
    // ///////////// API Implementation///////////////////
    // ///////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    public String getAction() {
        return action;
    }

    public Integer getNumber() {
        return number;
    }

    public Integer getSourcePortStart() {
        return publicStartPort;
    }

    public Integer getSourcePortEnd() {
        return publicEndPort;
    }

    @Override
    public long getEntityOwnerId() {
        Account caller = CallContext.current().getCallingAccount();
        return caller.getAccountId();
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_NETWORK_ACL_ITEM_UPDATE;
    }

    @Override
    public String getEventDescription() {
        return "Updating network ACL item";
    }

    public Integer getIcmpCode() {
        return icmpCode;
    }

    public Integer getIcmpType() {
        return icmpType;
    }

    public String getReason() {
        return reason;
    }

    @Override
    public void execute() throws ResourceUnavailableException {
        CallContext.current().setEventDetails("Rule Id: " + getId());
        NetworkACLItem aclItem = _networkACLService.updateNetworkACLItem(this);
        NetworkACLItemResponse aclResponse = _responseGenerator.createNetworkACLItemResponse(aclItem);
        setResponseObject(aclResponse);
        aclResponse.setResponseName(getCommandName());
    }

    @Override
    public void checkUuid() {
        if (this.getCustomId() != null) {
            _uuidMgr.checkUuid(this.getCustomId(), NetworkACLItem.class);
        }
    }

    public boolean isPartialUpgrade() {
        return partialUpgrade;
    }

    @Override
    public Long getApiResourceId() {
        return id;
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.NetworkAclItem;
    }
}
