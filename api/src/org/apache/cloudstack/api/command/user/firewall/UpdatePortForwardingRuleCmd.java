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
package org.apache.cloudstack.api.command.user.firewall;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.BaseAsyncCustomIdCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.FirewallRuleResponse;
import org.apache.cloudstack.api.response.IPAddressResponse;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.log4j.Logger;

import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.PortForwardingRule;
import com.cloud.user.Account;

@APICommand(name = "updatePortForwardingRule",
            responseObject = FirewallRuleResponse.class,
        description = "Updates a port forwarding rule.  Only the private port and the virtual machine can be updated.", entityType = {PortForwardingRule.class},
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class UpdatePortForwardingRuleCmd extends BaseAsyncCustomIdCmd {
    public static final Logger s_logger = Logger.getLogger(UpdatePortForwardingRuleCmd.class.getName());
    private static final String s_name = "updateportforwardingruleresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = FirewallRuleResponse.class, required = true, description = "the ID of the port forwarding rule", since = "4.4")
    private Long id;

    @Parameter(name = ApiConstants.PRIVATE_IP, type = CommandType.STRING, description = "the private IP address of the port forwarding rule")
    private String privateIp;

    @Parameter(name = ApiConstants.PRIVATE_PORT, type = CommandType.STRING, description = "the private port of the port forwarding rule")
    private String privatePort;

    @Parameter(name = ApiConstants.PROTOCOL,
               type = CommandType.STRING,
               description = "the protocol for the port fowarding rule. Valid values are TCP or UDP.")
    private String protocol;

    @Parameter(name = ApiConstants.IP_ADDRESS_ID,
               type = CommandType.UUID,
               entityType = IPAddressResponse.class,
               description = "the IP address id of the port forwarding rule")
    private Long publicIpId;

    @Parameter(name = ApiConstants.PUBLIC_PORT, type = CommandType.STRING, description = "the public port of the port forwarding rule")
    private String publicPort;

    @Parameter(name = ApiConstants.VIRTUAL_MACHINE_ID,
               type = CommandType.UUID,
               entityType = UserVmResponse.class,
               description = "the ID of the virtual machine for the port forwarding rule")
    private Long virtualMachineId;

    @Parameter(name = ApiConstants.FOR_DISPLAY, type = CommandType.BOOLEAN, description = "an optional field, whether to the display the rule to the end user or not", since = "4.4", authorized = {RoleType.Admin})
    private Boolean display;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getPrivateIp() {
        return privateIp;
    }

    public String getPrivatePort() {
        return privatePort;
    }

    public String getProtocol() {
        return protocol;
    }

    public String getPublicPort() {
        return publicPort;
    }

    public Long getVirtualMachineId() {
        return virtualMachineId;
    }

    public Boolean getDisplay() {
        return display;
    }
    public Long getId() {
        return id;
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
        PortForwardingRule rule = _entityMgr.findById(PortForwardingRule.class, getId());
        if (rule != null) {
            return rule.getAccountId();
        }

        // bad address given, parent this command to SYSTEM so ERROR events are tracked
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_NET_RULE_MODIFY;
    }

    @Override
    public String getEventDescription() {
        return "updating port forwarding rule";
    }

    @Override
    public void checkUuid() {
        if (getCustomId() != null) {
            _uuidMgr.checkUuid(getCustomId(), FirewallRule.class);
        }
    }

    @Override
    public void execute() {
        PortForwardingRule rule = _rulesService.updatePortForwardingRule(id, getCustomId(), getDisplay());
        FirewallRuleResponse fwResponse = new FirewallRuleResponse();
        if (rule != null) {
            fwResponse = _responseGenerator.createPortForwardingRuleResponse(rule);
            setResponseObject(fwResponse);
        }
        fwResponse.setResponseName(getCommandName());
//FIXME:        PortForwardingRule result = _mgr.updatePortForwardingRule(this);
//        if (result != null) {
//            FirewallRuleResponse response = _responseGenerator.createFirewallRuleResponse(result);
//            response.setResponseName(getName());
//            this.setResponseObject(response);
//        } else {
//            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to update port forwarding rule");
//        }
    }

    @Override
    public String getSyncObjType() {
        return BaseAsyncCmd.networkSyncObject;
    }

    @Override
    public Long getSyncObjId() {
        PortForwardingRule rule = _entityMgr.findById(PortForwardingRule.class, getId());
        if (rule != null) {
            return rule.getNetworkId();
        } else {
            throw new InvalidParameterValueException("Unable to find the rule by id");
        }
    }
}
