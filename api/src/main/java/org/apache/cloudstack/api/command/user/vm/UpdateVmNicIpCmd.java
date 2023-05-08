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

import java.util.ArrayList;
import java.util.EnumSet;

import org.apache.log4j.Logger;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.ApiConstants.VMDetails;
import org.apache.cloudstack.api.ResponseObject.ResponseView;
import org.apache.cloudstack.api.response.NicResponse;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.context.CallContext;

import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network;
import com.cloud.uservm.UserVm;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.Nic;

@APICommand(name = "updateVmNicIp", description = "Update the default Ip of a VM Nic", responseObject = UserVmResponse.class)
public class UpdateVmNicIpCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(UpdateVmNicIpCmd.class.getName());

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @Parameter(name=ApiConstants.NIC_ID, type=CommandType.UUID, entityType = NicResponse.class, required = true,
            description="the ID of the nic to which you want to assign private IP")
            private Long nicId;

    @Parameter(name = ApiConstants.IP_ADDRESS, type = CommandType.STRING, required = false,
            description = "Secondary IP Address")
            private String ipAddr;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getEntityTable() {
        return "nic_secondary_ips";
    }

    public String getAccountName() {
        return CallContext.current().getCallingAccount().getAccountName();
    }

    public long getDomainId() {
        return CallContext.current().getCallingAccount().getDomainId();
    }

    private long getZoneId() {
        Network ntwk = _entityMgr.findById(Network.class, getNetworkId());
        if (ntwk == null) {
            throw new InvalidParameterValueException("Can't find zone id for specified");
        }
        return ntwk.getDataCenterId();
    }

    public Long getNetworkId() {
        Nic nic = _entityMgr.findById(Nic.class, nicId);
        if (nic == null) {
            throw new InvalidParameterValueException("Can't find network id for specified nic");
        }
        Long networkId = nic.getNetworkId();
        return networkId;
    }

    public Long getNicId() {
        return nicId;
    }

    public String getIpaddress () {
        if (ipAddr != null) {
            return ipAddr;
        } else {
            return null;
        }
    }

    public NetworkType getNetworkType() {
        Network ntwk = _entityMgr.findById(Network.class, getNetworkId());
        DataCenter dc = _entityMgr.findById(DataCenter.class, ntwk.getDataCenterId());
        return dc.getNetworkType();
    }

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccountId();
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_NET_IP_ASSIGN;
    }

    @Override
    public String getEventDescription() {
        return  "associating ip to nic id: " + this._uuidMgr.getUuid(Network.class, getNetworkId()) + " in zone " + this._uuidMgr.getUuid(DataCenter.class, getZoneId());
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////


    public static String getResultObjectName() {
        return "addressinfo";
    }

    @Override
    public void execute() throws ResourceUnavailableException, ResourceAllocationException,
    ConcurrentOperationException, InsufficientCapacityException {

        CallContext.current().setEventDetails("Nic Id: " + getNicId() );
        String ip;
        if ((ip = getIpaddress()) != null) {
            if (!NetUtils.isValidIp4(ip)) {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Invalid ip address " + ip);
            }
        }

        UserVm vm = _userVmService.updateNicIpForVirtualMachine(this);
        ArrayList<VMDetails> dc = new ArrayList<VMDetails>();
        dc.add(VMDetails.valueOf("nics"));
        EnumSet<VMDetails> details = EnumSet.copyOf(dc);
        if (vm != null){
            UserVmResponse response = _responseGenerator.createUserVmResponse(ResponseView.Restricted, "virtualmachine", details, vm).get(0);
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to update ip address on vm NIC. Refer to server logs for details.");
        }
    }

    @Override
    public String getSyncObjType() {
        return BaseAsyncCmd.networkSyncObject;
    }

    @Override
    public Long getSyncObjId() {
        return getNetworkId();
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.IpAddress;
    }

}
