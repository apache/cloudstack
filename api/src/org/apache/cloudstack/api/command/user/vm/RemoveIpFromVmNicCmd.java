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
package org.apache.cloudstack.api.command.user.vm;

import org.apache.log4j.Logger;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.NicSecondaryIpResponse;
import org.apache.cloudstack.api.response.SuccessResponse;
import com.cloud.async.AsyncJob;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.event.EventTypes;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.network.Network;
import com.cloud.user.Account;
import com.cloud.user.UserContext;
import com.cloud.vm.Nic;
import com.cloud.vm.NicSecondaryIp;

@APICommand(name = "removeIpFromNic", description="Assigns secondary IP to NIC.", responseObject=SuccessResponse.class)
public class RemoveIpFromVmNicCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(RemoveIpFromVmNicCmd.class.getName());
    private static final String s_name = "removeipfromnicresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.ID, type=CommandType.UUID, required = true, entityType = NicSecondaryIpResponse.class,
            description="the ID of the secondary ip address to nic")
            private Long id;

    // unexposed parameter needed for events logging
    @Parameter(name=ApiConstants.ACCOUNT_ID, type=CommandType.UUID, expose=false)
    private Long ownerId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getEntityTable() {
        return "nic_secondary_ips";
    }

    public Long getIpAddressId() {
        return id;
    }

    public String getAccountName() {
        return UserContext.current().getCaller().getAccountName();
    }

    public long getDomainId() {
        return UserContext.current().getCaller().getDomainId();
    }

    @Override
    public long getEntityOwnerId() {
        Account caller = UserContext.current().getCaller();
        return caller.getAccountId();
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_NET_IP_ASSIGN;
    }

    public NicSecondaryIp getIpEntry() {
        NicSecondaryIp nicSecIp = _entityMgr.findById(NicSecondaryIp.class, getIpAddressId());
        return nicSecIp;
    }

    @Override
    public String getEventDescription() {
        return  ("Disassociating ip address with id=" + id);
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    public static String getResultObjectName() {
        return "addressinfo";
    }

    public Long getNetworkId() {
        NicSecondaryIp nicSecIp = _entityMgr.findById(NicSecondaryIp.class, getIpAddressId());
        if (nicSecIp != null) {
            Long networkId = nicSecIp.getNetworkId();
            return networkId;
        } else {
            return null;
        }
    }

    public NetworkType getNetworkType() {
        Network ntwk = _entityMgr.findById(Network.class, getNetworkId());
        if (ntwk != null) {
            DataCenter dc = _entityMgr.findById(DataCenter.class, ntwk.getDataCenterId());
            return dc.getNetworkType();
        }
        return null;
    }

    @Override
    public void execute() throws InvalidParameterValueException {
        UserContext.current().setEventDetails("Ip Id: " + id);
        NicSecondaryIp nicSecIp = getIpEntry();

        if (nicSecIp == null) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Invalid IP id is passed");
        }

        if (getNetworkType() == NetworkType.Basic) {
            //remove the security group rules for this secondary ip
            boolean success = false;
            success = _securityGroupService.securityGroupRulesForVmSecIp(nicSecIp.getNicId(), nicSecIp.getNetworkId(),nicSecIp.getIp4Address(), false);
            if (success == false) {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to set security group rules for the secondary ip");
            }
        }

        try {
            boolean result = _networkService.releaseSecondaryIpFromNic(id);
            if (result) {
                SuccessResponse response = new SuccessResponse(getCommandName());
                this.setResponseObject(response);
            } else {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to remove secondary  ip address for the nic");
            }
        } catch (InvalidParameterValueException e) {
            throw new InvalidParameterValueException("Removing guest ip from nic failed");
        }
    }

    @Override
    public String getSyncObjType() {
        return BaseAsyncCmd.networkSyncObject;
    }

    @Override
    public AsyncJob.Type getInstanceType() {
        return AsyncJob.Type.IpAddress;
    }

}
