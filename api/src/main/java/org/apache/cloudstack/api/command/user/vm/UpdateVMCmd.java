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

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.acl.SecurityChecker.AccessType;
import org.apache.cloudstack.api.ACL;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCustomIdCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject.ResponseView;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.user.UserCmd;
import org.apache.cloudstack.api.response.GuestOSResponse;
import org.apache.cloudstack.api.response.SecurityGroupResponse;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.context.CallContext;

import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.user.Account;
import com.cloud.uservm.UserVm;
import com.cloud.utils.net.Dhcp;
import com.cloud.vm.VirtualMachine;

@APICommand(name = "updateVirtualMachine", description="Updates properties of a virtual machine. The VM has to be stopped and restarted for the " +
        "new properties to take effect. UpdateVirtualMachine does not first check whether the VM is stopped. " +
        "Therefore, stop the VM manually before issuing this call.", responseObject = UserVmResponse.class, responseView = ResponseView.Restricted, entityType = {VirtualMachine.class},
    requestHasSensitiveInfo = false, responseHasSensitiveInfo = true)
public class UpdateVMCmd extends BaseCustomIdCmd implements SecurityGroupAction, UserCmd {
    public static final Logger s_logger = Logger.getLogger(UpdateVMCmd.class.getName());
    private static final String s_name = "updatevirtualmachineresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.DISPLAY_NAME, type = CommandType.STRING, description = "user generated name")
    private String displayName;

    @Parameter(name = ApiConstants.GROUP, type = CommandType.STRING, description = "group of the virtual machine")
    private String group;

    @Parameter(name = ApiConstants.HA_ENABLE, type = CommandType.BOOLEAN, description = "true if high-availability is enabled for the virtual machine, false otherwise")
    private Boolean haEnable;

    @ACL(accessType = AccessType.OperateEntry)
    @Parameter(name=ApiConstants.ID, type=CommandType.UUID, entityType=UserVmResponse.class,
            required=true, description="The ID of the virtual machine")
    private Long id;

    @Parameter(name = ApiConstants.OS_TYPE_ID,
               type = CommandType.UUID,
               entityType = GuestOSResponse.class,
               description = "the ID of the OS type that best represents this VM.")
    private Long osTypeId;

    @Parameter(name = ApiConstants.USER_DATA,
               type = CommandType.STRING,
               description = "an optional binary data that can be sent to the virtual machine upon a successful deployment. This binary data must be base64 encoded before adding it to the request. Using HTTP GET (via querystring), you can send up to 2KB of data after base64 encoding. Using HTTP POST(via POST body), you can send up to 32K of data after base64 encoding.",
               length = 32768)
    private String userData;

    @Parameter(name = ApiConstants.DISPLAY_VM, type = CommandType.BOOLEAN, description = "an optional field, whether to the display the vm to the end user or not.", authorized = {RoleType.Admin})
    private Boolean displayVm;

    @Parameter(name = ApiConstants.IS_DYNAMICALLY_SCALABLE,
               type = CommandType.BOOLEAN,
               description = "true if VM contains XS/VMWare tools inorder to support dynamic scaling of VM cpu/memory")
    protected Boolean isDynamicallyScalable;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, description = "new host name of the vm. The VM has to be stopped/started for this update to take affect", since = "4.4")
    private String name;

    @Parameter(name = ApiConstants.INSTANCE_NAME, type = CommandType.STRING, description = "instance name of the user vm", since = "4.4", authorized = {RoleType.Admin})
    private String instanceName;

    @Parameter(name = ApiConstants.DETAILS, type = CommandType.MAP, description = "Details in key/value pairs.")
    protected Map<String, String> details;

    @ACL
    @Parameter(name = ApiConstants.SECURITY_GROUP_IDS,
               type = CommandType.LIST,
               collectionType = CommandType.UUID,
               entityType = SecurityGroupResponse.class,
               description = "list of security group ids to be applied on the virtual machine.")
    private List<Long> securityGroupIdList;

    @ACL
    @Parameter(name = ApiConstants.SECURITY_GROUP_NAMES,
               type = CommandType.LIST,
               collectionType = CommandType.STRING,
               entityType = SecurityGroupResponse.class,
               description = "comma separated list of security groups names that going to be applied to the virtual machine. " +
                       "Should be passed only when vm is created from a zone with Basic Network support. " +
                       "Mutually exclusive with securitygroupids parameter"
            )
    private List<String> securityGroupNameList;

    @Parameter(name = ApiConstants.CLEAN_UP_DETAILS,
            type = CommandType.BOOLEAN,
            description = "optional boolean field, which indicates if details should be cleaned up or not (if set to true, details removed for this resource, details field ignored; if false or not set, no action)")
    private Boolean cleanupDetails;

    @Parameter(name = ApiConstants.DHCP_OPTIONS_NETWORK_LIST, type = CommandType.MAP, description = "DHCP options which are passed to the VM on start up"
            + " Example: dhcpoptionsnetworklist[0].dhcp:114=url&dhcpoptionsetworklist[0].networkid=networkid&dhcpoptionsetworklist[0].dhcp:66=www.test.com")
    private Map dhcpOptionsNetworkList;

    @Parameter(name = ApiConstants.EXTRA_CONFIG, type = CommandType.STRING, since = "4.12", description = "an optional URL encoded string that can be passed to the virtual machine upon successful deployment", authorized = { RoleType.Admin }, length = 5120)
    private String extraConfig;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getDisplayName() {
        return displayName;
    }

    public String getGroup() {
        return group;
    }

    public Boolean getHaEnable() {
        return haEnable;
    }

    public Long getId() {
        return id;
    }

    public String getUserData() {
        return userData;
    }

    public Boolean getDisplayVm() {
        return displayVm;
    }

    public Boolean isDynamicallyScalable() {
        return isDynamicallyScalable;
    }

    public String getHostName() {
        return name;
    }

    public String getInstanceName() {
        return instanceName;
    }

    public Map<String, String> getDetails() {
        if (this.details == null || this.details.isEmpty()) {
            return null;
        }

        Collection<String> paramsCollection = this.details.values();
        return (Map<String, String>) (paramsCollection.toArray())[0];
    }

    public List<Long> getSecurityGroupIdList() {
        return securityGroupIdList;
    }

    public List<String> getSecurityGroupNameList() {
        return securityGroupNameList;
    }

    public boolean isCleanupDetails(){
        return cleanupDetails == null ? false : cleanupDetails.booleanValue();
    }

    public Map<String, Map<Integer, String>> getDhcpOptionsMap() {
        Map<String, Map<Integer, String>> dhcpOptionsMap = new HashMap<>();
        if (dhcpOptionsNetworkList != null && !dhcpOptionsNetworkList.isEmpty()) {

            Collection<Map<String, String>> paramsCollection = this.dhcpOptionsNetworkList.values();
            for(Map<String, String> dhcpNetworkOptions : paramsCollection) {
                String networkId = dhcpNetworkOptions.get(ApiConstants.NETWORK_ID);

                if(networkId == null) {
                    throw new IllegalArgumentException("No networkid specified when providing extra dhcp options.");
                }

                Map<Integer, String> dhcpOptionsForNetwork = new HashMap<>();
                dhcpOptionsMap.put(networkId, dhcpOptionsForNetwork);

                for (String key : dhcpNetworkOptions.keySet()) {
                    if (key.startsWith(ApiConstants.DHCP_PREFIX)) {
                        int dhcpOptionValue = Integer.parseInt(key.replaceFirst(ApiConstants.DHCP_PREFIX, ""));
                        dhcpOptionsForNetwork.put(dhcpOptionValue, dhcpNetworkOptions.get(key));
                    } else if (!key.equals(ApiConstants.NETWORK_ID)) {
                        Dhcp.DhcpOptionCode dhcpOptionEnum = Dhcp.DhcpOptionCode.valueOfString(key);
                        dhcpOptionsForNetwork.put(dhcpOptionEnum.getCode(), dhcpNetworkOptions.get(key));
                    }
                }

            }
        }

        return dhcpOptionsMap;
    }

    public String getExtraConfig() {
        return extraConfig;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    public Long getOsTypeId() {
        return osTypeId;
    }

    @Override
    public String getCommandName() {
        return s_name;
    }

    public static String getResultObjectName() {
        return "virtualmachine";
    }

    @Override
    public long getEntityOwnerId() {
        UserVm userVm = _entityMgr.findById(UserVm.class, getId());
        if (userVm != null) {
            return userVm.getAccountId();
        }

        return Account.ACCOUNT_ID_SYSTEM; // no account info given, parent this command to SYSTEM so ERROR events are tracked
    }

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException {
        CallContext.current().setEventDetails("Vm Id: " + this._uuidMgr.getUuid(VirtualMachine.class, getId()));
        UserVm result = _userVmService.updateVirtualMachine(this);
        if (result != null){
            UserVmResponse response = _responseGenerator.createUserVmResponse(getResponseView(), "virtualmachine", result).get(0);
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to update vm");
        }
    }

    @Override
    public void checkUuid() {
        if (getCustomId() != null) {
            _uuidMgr.checkUuid(getCustomId(), UserVm.class);

        }
    }
}
