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
import org.apache.cloudstack.api.response.UserVmResponse;

import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.PortForwardingRule;
import com.cloud.user.Account;
import com.cloud.utils.net.Ip;

import java.util.List;

@APICommand(name = "updatePortForwardingRule",
            responseObject = FirewallRuleResponse.class,
        description = "Updates a port forwarding rule. Only the private port and the virtual machine can be updated.", entityType = {PortForwardingRule.class},
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class UpdatePortForwardingRuleCmd extends BaseAsyncCustomIdCmd {

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = FirewallRuleResponse.class, required = true, description = "the ID of the port forwarding rule", since = "4.4")
    private Long id;

    @Parameter(name=ApiConstants.PRIVATE_START_PORT, type=CommandType.INTEGER, description="the private start port of the port forwarding rule")
    private Integer privatePort;


    @Parameter(name=ApiConstants.PRIVATE_END_PORT, type=CommandType.INTEGER, description="the private end port of the port forwarding rule")
    private Integer privateEndPort;

    @Parameter(name = ApiConstants.VIRTUAL_MACHINE_ID,
               type = CommandType.UUID,
               entityType = UserVmResponse.class,
               description = "the ID of the virtual machine for the port forwarding rule")
    private Long virtualMachineId;

    @Parameter(name = ApiConstants.VM_GUEST_IP, type = CommandType.STRING, required = false, description = "VM guest nic Secondary ip address for the port forwarding rule", since = "4.5")
    private String vmGuestIp;

    @Parameter(name = ApiConstants.FOR_DISPLAY, type = CommandType.BOOLEAN, description = "an optional field, whether to the display the rule to the end user or not", since = "4.4", authorized = {RoleType.Admin})
    private Boolean display;

    @Parameter(name = ApiConstants.CIDR_LIST,
            type = CommandType.LIST,
            collectionType = CommandType.STRING,
            description = " the source CIDR list to allow traffic from; all other CIDRs will be blocked. " +
                    "Multiple entries must be separated by a single comma character (,). This param will be used only for VPC tiers. By default, all CIDRs are allowed.")
    private List<String> sourceCidrList;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public Integer getPrivatePort() {
        return privatePort;
    }

    public Integer getPrivateEndPort() {
        return privateEndPort;
    }

    public Long getVirtualMachineId() {
        return virtualMachineId;
    }

    public Ip getVmGuestIp() {
        if (vmGuestIp == null) {
            return null;
        }
        return new Ip(vmGuestIp);
    }

    public Boolean getDisplay() {
        return display;
    }

    public List<String> getSourceCidrList() {
        return sourceCidrList;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////



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
        PortForwardingRule rule = _rulesService.updatePortForwardingRule(this);
        FirewallRuleResponse fwResponse = new FirewallRuleResponse();
        if (rule != null) {
            fwResponse = _responseGenerator.createPortForwardingRuleResponse(rule);
            setResponseObject(fwResponse);
        }
        fwResponse.setResponseName(getCommandName());
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
