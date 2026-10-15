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
package org.apache.cloudstack.api.command.admin.network;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.NetworkResponse;
import org.apache.cloudstack.api.response.NicResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.commons.lang3.StringUtils;

import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network;
import com.cloud.user.Account;
import com.cloud.vm.Nic;
import com.cloud.vm.VirtualMachine;

@APICommand(name = "associateNetworkToNic", description = "Associates one or more additional networks with an existing NIC, delivering them as a multi-VLAN trunk",
        responseObject = NicResponse.class, requestHasSensitiveInfo = false, responseHasSensitiveInfo = false, since = "24.0.0",
        authorized = {RoleType.Admin})
public class AssociateNetworkToNicCmd extends BaseAsyncCmd {
    private static final String s_name = "associatenetworktonicresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.NIC_ID, type = CommandType.UUID, entityType = NicResponse.class, required = true,
            description = "the ID of the NIC to associate additional networks with")
    private Long nicId;

    @Parameter(name = ApiConstants.NETWORK_IDS, type = CommandType.LIST, collectionType = CommandType.UUID, entityType = NetworkResponse.class, required = true,
            description = "the IDs of the additional networks to associate with the NIC")
    private List<Long> networkIds;

    @Parameter(name = ApiConstants.IP_ADDRESSES, type = CommandType.MAP, required = false,
            description = "optional per-network IP addresses, keyed by network. Example: "
                    + "ipaddresses[0].networkid=UUID&ipaddresses[0].ipaddress=10.0.0.5&ipaddresses[0].ip6address=fd00::5")
    private Map ipAddresses;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getNicId() {
        return nicId;
    }

    public List<Long> getNetworkIds() {
        return networkIds;
    }

    public Map<Long, Network.IpAddresses> getIpAddressesMap() {
        Map<Long, Network.IpAddresses> result = new HashMap<>();
        if (ipAddresses == null || ipAddresses.isEmpty()) {
            return result;
        }
        Collection<Map<String, String>> entries = ipAddresses.values();
        for (Map<String, String> entry : entries) {
            String networkUuid = entry.get(ApiConstants.NETWORK_ID);
            if (networkUuid == null) {
                throw new InvalidParameterValueException(String.format("Each ipaddresses entry must specify %s", ApiConstants.NETWORK_ID));
            }
            Network network = _entityMgr.findByUuid(Network.class, networkUuid);
            if (network == null) {
                throw new InvalidParameterValueException(String.format("Unable to find network with id: %s", networkUuid));
            }
            String ip4Address = entry.get(ApiConstants.IP_ADDRESS);
            String ip6Address = entry.get(ApiConstants.IP6_ADDRESS);
            result.put(network.getId(), new Network.IpAddresses(ip4Address, ip6Address));
        }
        return result;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation ///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_NIC_NETWORK_ASSOCIATE;
    }

    @Override
    public String getEventDescription() {
        return "Associating networks " + StringUtils.join(getNetworkIds(), ",") + " with nic " + getResourceUuid(ApiConstants.NIC_ID);
    }

    private Nic getNic() {
        Nic nic = _entityMgr.findById(Nic.class, nicId);
        if (nic == null) {
            throw new InvalidParameterValueException("Can't find NIC for id specified");
        }
        return nic;
    }

    @Override
    public long getEntityOwnerId() {
        VirtualMachine vm = _entityMgr.findById(VirtualMachine.class, getNic().getInstanceId());
        if (vm == null) {
            return Account.ACCOUNT_ID_SYSTEM;
        }
        return vm.getAccountId();
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.VirtualMachine;
    }

    @Override
    public Long getApiResourceId() {
        return getNic().getInstanceId();
    }

    @Override
    public void execute() throws ConcurrentOperationException, ResourceUnavailableException, InsufficientCapacityException, InsufficientAddressCapacityException {
        CallContext.current().setEventDetails("Nic ID: " + getResourceUuid(ApiConstants.NIC_ID));
        Nic result = _networkService.associateNetworkToNic(this);
        if (result == null) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to associate network to NIC. Refer to server logs for details.");
        }
        NicResponse response = _responseGenerator.createNicResponse(result);
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }
}
