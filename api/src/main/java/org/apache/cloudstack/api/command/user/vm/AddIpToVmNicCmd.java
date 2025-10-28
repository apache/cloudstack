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

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCreateCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.NicResponse;
import org.apache.cloudstack.api.response.NicSecondaryIpResponse;
import org.apache.cloudstack.context.CallContext;

import com.cloud.dc.DataCenter;
import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network;
import com.cloud.network.Network.IpAddresses;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.Nic;
import com.cloud.vm.NicSecondaryIp;
import com.cloud.vm.VirtualMachine;

@APICommand(name = "addIpToNic", description = "Assigns secondary IP to NIC", responseObject = NicSecondaryIpResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class AddIpToVmNicCmd extends BaseAsyncCreateCmd {
    private static final String s_name = "addiptovmnicresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @Parameter(name = ApiConstants.NIC_ID, type = CommandType.UUID, entityType = NicResponse.class, required = true, description = "the ID of the nic to which you want to assign private IP")
    private Long nicId;

    @Parameter(name = ApiConstants.IP_ADDRESS, type = CommandType.STRING, required = false, description = "Secondary IP Address")
    private String ipAddr;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getEntityTable() {
        return "nic_secondary_ips";
    }

    private long getNetworkId() {
        Nic nic = _entityMgr.findById(Nic.class, nicId);
        if (nic == null) {
            throw new InvalidParameterValueException("Can't find network id for specified nic");
        }
        return nic.getNetworkId();
    }

    public long getNicId() {
        return nicId;
    }

    private boolean isZoneSGEnabled() {
        Network ntwk = _entityMgr.findById(Network.class, getNetworkId());
        DataCenter dc = _entityMgr.findById(DataCenter.class, ntwk.getDataCenterId());
        return dc.isSecurityGroupEnabled() || _ntwkModel.isSecurityGroupSupportedForZone(dc.getId());
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_NIC_SECONDARY_IP_ASSIGN;
    }

    @Override
    public String getEventDescription() {
        return "associating ip to nic id=" + this._uuidMgr.getUuid(Nic.class, getNicId()) + " belonging to network id=" + this._uuidMgr.getUuid(Network.class, getNetworkId());
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

    @Override
    public void execute() throws ResourceUnavailableException, ResourceAllocationException, ConcurrentOperationException, InsufficientCapacityException {

        CallContext.current().setEventDetails("Nic Id: " + this._uuidMgr.getUuid(Nic.class, getNicId()));
        NicSecondaryIp result = _entityMgr.findById(NicSecondaryIp.class, getEntityId());

        if (result != null) {
            CallContext.current().setEventDetails("secondary Ip Id: " + getEntityUuid());
            boolean success = false;
            success = _networkService.configureNicSecondaryIp(result, isZoneSGEnabled());

            if (success == false) {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to set security group rules for the secondary ip");
            }

            NicSecondaryIpResponse response = _responseGenerator.createSecondaryIPToNicResponse(result);
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to assign secondary ip to nic");
        }
    }

    @Override
    public Long getSyncObjId() {
        return getNetworkId();
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.IpAddress;
    }

    @Override
    public long getEntityOwnerId() {
        Nic nic = _entityMgr.findById(Nic.class, nicId);
        if (nic == null) {
            throw new InvalidParameterValueException("Can't find nic for id specified");
        }
        long vmId = nic.getInstanceId();
        VirtualMachine vm = _entityMgr.findById(VirtualMachine.class, vmId);

        return vm.getAccountId();
    }

    @Override
    public void create() throws ResourceAllocationException {
        NicSecondaryIp result;

        IpAddresses requestedIpPair = new IpAddresses(ipAddr, null);
        if (!NetUtils.isIpv4(ipAddr)) {
            requestedIpPair = new IpAddresses(null, ipAddr);
        }

        try {
            result = _networkService.allocateSecondaryGuestIP(getNicId(), requestedIpPair);
            if (result != null) {
                setEntityId(result.getId());
                setEntityUuid(result.getUuid());
            }
        } catch (InsufficientAddressCapacityException e) {
            throw new InvalidParameterValueException("Allocating guest ip for nic failed : " + e.getMessage());
        }

        if (result == null) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to assign secondary ip to nic");
        }
    }
}
