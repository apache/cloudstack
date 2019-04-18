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

import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.network.Network;
import com.cloud.user.Account;
import com.cloud.uservm.UserVm;
import com.cloud.utils.net.Dhcp;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.VirtualMachine;
import org.apache.cloudstack.acl.SecurityChecker.AccessType;
import org.apache.cloudstack.api.ACL;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiConstants.VMDetails;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject.ResponseView;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.NetworkResponse;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

@APICommand(name = "addNicToVirtualMachine", description = "Adds VM to specified network by creating a NIC", responseObject = UserVmResponse.class, responseView = ResponseView.Restricted, entityType = {VirtualMachine.class},
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = true)
public class AddNicToVMCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(AddNicToVMCmd.class);
    private static final String s_name = "addnictovirtualmachineresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @ACL(accessType = AccessType.OperateEntry)
    @Parameter(name=ApiConstants.VIRTUAL_MACHINE_ID, type=CommandType.UUID, entityType=UserVmResponse.class,
            required=true, description="Virtual Machine ID")
    private Long vmId;

    @Parameter(name = ApiConstants.NETWORK_ID, type = CommandType.UUID, entityType = NetworkResponse.class, required = true, description = "Network ID")
    private Long netId;

    @Parameter(name = ApiConstants.IP_ADDRESS, type = CommandType.STRING, description = "IP Address for the new network")
    private String ipaddr;

    @Parameter(name = ApiConstants.MTU, type = CommandType.INTEGER, description = "MTU size")
    private int mtu;

    @Parameter(name = ApiConstants.MAC_ADDRESS, type = CommandType.STRING, description = "Mac Address for the new network")
    private String macaddr;

    @Parameter(name = ApiConstants.DHCP_OPTIONS, type = CommandType.MAP, description = "DHCP options which are passed to the nic"
            + " Example: dhcpoptions[0].dhcp:114=url&dhcpoptions[0].dhcp:66=www.test.com")
    private Map dhcpOptions;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getVmId() {
        return vmId;
    }

    public Long getNetworkId() {
        return netId;
    }

    public String getIpAddress() {
        return ipaddr;
    }

    public int getMtu() {
        return mtu;
    }

    public String getMacAddress() {
        if (macaddr == null) {
            return null;
        }
        if(!NetUtils.isValidMac(macaddr)) {
            throw new InvalidParameterValueException("Mac address is not valid: " + macaddr);
        } else if(!NetUtils.isUnicastMac(macaddr)) {
            throw new InvalidParameterValueException("Mac address is not unicast: " + macaddr);
        }
        return NetUtils.standardizeMacAddress(macaddr);
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    public static String getResultObjectName() {
        return "virtualmachine";
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_NIC_CREATE;
    }

    @Override
    public String getEventDescription() {
        return  "Adding network " + this._uuidMgr.getUuid(Network.class, getNetworkId()) + " to user vm: " + this._uuidMgr.getUuid(VirtualMachine.class, getVmId());
    }

    @Override
    public long getEntityOwnerId() {
        UserVm vm = _responseGenerator.findUserVmById(getVmId());
        if (vm == null) {
             return Account.ACCOUNT_ID_SYSTEM; // bad id given, parent this command to SYSTEM so ERROR events are tracked
        }
        return vm.getAccountId();
    }

    public Map<Integer, String> getDhcpOptionsMap() {
        Map<Integer, String> dhcpOptionsMap = new HashMap<>();
        if (dhcpOptions != null && !dhcpOptions.isEmpty()) {

            Collection<Map<String, String>> paramsCollection = this.dhcpOptions.values();
            for(Map<String, String> dhcpNetworkOptions : paramsCollection) {
                for (String key : dhcpNetworkOptions.keySet()) {
                    if (key.startsWith(ApiConstants.DHCP_PREFIX)) {
                        int dhcpOptionValue = Integer.parseInt(key.replaceFirst(ApiConstants.DHCP_PREFIX, ""));
                        dhcpOptionsMap.put(dhcpOptionValue, dhcpNetworkOptions.get(key));
                    } else {
                        Dhcp.DhcpOptionCode dhcpOptionEnum = Dhcp.DhcpOptionCode.valueOfString(key);
                        dhcpOptionsMap.put(dhcpOptionEnum.getCode(), dhcpNetworkOptions.get(key));
                    }
                }

            }
        }

        return dhcpOptionsMap;
    }

    @Override
    public void execute() {
        CallContext.current().setEventDetails("Vm Id: " + this._uuidMgr.getUuid(VirtualMachine.class, getVmId()) + " Network Id: " + this._uuidMgr.getUuid(Network.class, getNetworkId()));
        UserVm result = _userVmService.addNicToVirtualMachine(this);
        ArrayList<VMDetails> dc = new ArrayList<VMDetails>();
        dc.add(VMDetails.valueOf("nics"));
        EnumSet<VMDetails> details = EnumSet.copyOf(dc);
        if (result != null){
            UserVmResponse response = _responseGenerator.createUserVmResponse(ResponseView.Restricted, "virtualmachine", details, result).get(0);
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to add NIC to vm. Refer to server logs for details.");
        }
    }
}
