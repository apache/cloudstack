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
package org.apache.cloudstack.api.command.user.nat;

import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.log4j.Logger;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.IPAddressResponse;
import org.apache.cloudstack.api.response.NetworkResponse;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.api.response.UserVmResponse;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.IpAddress;
import com.cloud.user.Account;
import com.cloud.uservm.UserVm;

@APICommand(name = "enableStaticNat", description = "Enables static NAT for given IP address", responseObject = SuccessResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class EnableStaticNatCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(EnableStaticNatCmd.class.getName());


    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.IP_ADDRESS_ID, type = CommandType.UUID, entityType = IPAddressResponse.class, required = true, description = "the public IP "
        + "address ID for which static NAT feature is being enabled")
    private Long ipAddressId;

    @Parameter(name = ApiConstants.VIRTUAL_MACHINE_ID, type = CommandType.UUID, entityType = UserVmResponse.class, required = true, description = "the ID of "
        + "the virtual machine for enabling static NAT feature")
    private Long virtualMachineId;

    @Parameter(name = ApiConstants.NETWORK_ID,
               type = CommandType.UUID,
               entityType = NetworkResponse.class,
               description = "The network of the VM the static NAT will be enabled for."
                   + " Required when public IP address is not associated with any guest network yet (VPC case)")
    private Long networkId;
    @Parameter(name = ApiConstants.VM_GUEST_IP,
               type = CommandType.STRING,
               required = false,
               description = "VM guest NIC secondary IP address for the port forwarding rule")
    private String vmSecondaryIp;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getIpAddressId() {
        return ipAddressId;
    }

    public Long getVirtualMachineId() {
        return virtualMachineId;
    }

    public String getVmSecondaryIp() {
        if (vmSecondaryIp == null) {
            return null;
        }
        return vmSecondaryIp;
    }

    public long getNetworkId() {
        IpAddress ip = _entityMgr.findById(IpAddress.class, getIpAddressId());
        Long ntwkId = null;

        if (ip.getAssociatedWithNetworkId() != null) {
            ntwkId = ip.getAssociatedWithNetworkId();
        } else {
            ntwkId = networkId;
        }

        // in case of portable public IP, network ID passed takes precedence
        if (ip.isPortable() && networkId != null) {
            ntwkId = networkId;
        }

        if (ntwkId == null) {
            throw new InvalidParameterValueException("Unable to enable static NAT for the ipAddress id=" + ipAddressId +
                " as IP is not associated with any network and no networkId is passed in");
        }
        return ntwkId;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public long getEntityOwnerId() {
        UserVm userVm = _entityMgr.findById(UserVm.class, getVirtualMachineId());
        if (userVm != null) {
            return userVm.getAccountId();
        }

        return Account.ACCOUNT_ID_SYSTEM; // no account info given, parent this command to SYSTEM so ERROR events are tracked
    }

    @Override
    public void execute() throws ResourceUnavailableException {
        try {
            boolean result = _rulesService.enableStaticNat(ipAddressId, virtualMachineId, getNetworkId(), getVmSecondaryIp());
            if (result) {
                SuccessResponse response = new SuccessResponse(getCommandName());
                this.setResponseObject(response);
            } else {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to enable static NAT");
            }
        } catch (NetworkRuleConflictException ex) {
            s_logger.info("Network rule conflict: " + ex.getMessage());
            s_logger.trace("Network Rule Conflict: ", ex);
            throw new ServerApiException(ApiErrorCode.NETWORK_RULE_CONFLICT_ERROR, ex.getMessage());
        }
    }

    @Override
    public Long getApiResourceId() {
        return getVirtualMachineId();
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.VirtualMachine;
    }
}
