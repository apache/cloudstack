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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.api.response.UserDataResponse;
import org.apache.log4j.Logger;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.acl.SecurityChecker.AccessType;
import org.apache.cloudstack.api.ACL;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandResourceType;
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

@APICommand(name = "updateVirtualMachine", description = "Updates properties of an Instance. The Instance has to be stopped and restarted for the " +
        "new properties to take effect. UpdateVirtualMachine does not first check whether the Instance is stopped. " +
        "Therefore, stop the Instance manually before issuing this call.", responseObject = UserVmResponse.class, responseView = ResponseView.Restricted, entityType = {VirtualMachine.class},
    requestHasSensitiveInfo = false, responseHasSensitiveInfo = true)
public class UpdateVMCmd extends BaseCustomIdCmd implements SecurityGroupAction, UserCmd {
    public static final Logger s_logger = Logger.getLogger(UpdateVMCmd.class.getName());
    private static final String s_name = "updatevirtualmachineresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.DISPLAY_NAME, type = CommandType.STRING, description = "User generated name")
    private String displayName;

    @Parameter(name = ApiConstants.GROUP, type = CommandType.STRING, description = "Group of the Instance")
    private String group;

    @Parameter(name = ApiConstants.HA_ENABLE, type = CommandType.BOOLEAN, description = "True if high-availability is enabled for the Instance, false otherwise")
    private Boolean haEnable;

    @ACL(accessType = AccessType.OperateEntry)
    @Parameter(name=ApiConstants.ID, type=CommandType.UUID, entityType=UserVmResponse.class,
            required=true, description = "The ID of the Instance")
    private Long id;

    @Parameter(name = ApiConstants.OS_TYPE_ID,
               type = CommandType.UUID,
               entityType = GuestOSResponse.class,
               description = "The ID of the OS type that best represents this Instance.")
    private Long osTypeId;

    @Parameter(name = ApiConstants.USER_DATA,
               type = CommandType.STRING,
               description = "An optional binary data that can be sent to the Instance upon a successful deployment. " +
                       "This binary data must be base64 encoded before adding it to the request. " +
                       "Using HTTP GET (via querystring), you can send up to 4KB of data after base64 encoding. " +
                       "Using HTTP POST(via POST body), you can send up to 1MB of data after base64 encoding." +
                       "You also need to change vm.userdata.max.length value",
               length = 1048576,
               since = "4.16.0")
    private String userData;

    @Parameter(name = ApiConstants.USER_DATA_ID, type = CommandType.UUID, entityType = UserDataResponse.class, description = "The ID of the userdata", since = "4.18")
    private Long userdataId;

    @Parameter(name = ApiConstants.USER_DATA_DETAILS, type = CommandType.MAP, description = "Used to specify the parameters values for the variables in userdata.", since = "4.18")
    private Map userdataDetails;

    @Parameter(name = ApiConstants.DISPLAY_VM, type = CommandType.BOOLEAN, description = "An optional field, whether to the display the Instance to the end User or not.", authorized = {RoleType.Admin})
    private Boolean displayVm;

    @Parameter(name = ApiConstants.IS_DYNAMICALLY_SCALABLE,
               type = CommandType.BOOLEAN,
               description = "True if Instance contains XS/VMWare tools in order to support dynamic scaling of Instance cpu/memory. This can be updated only when dynamic scaling is enabled on Template, service offering and the corresponding global setting")
    protected Boolean isDynamicallyScalable;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, description = "New host name of the Instance. The Instance has to be stopped/started for this update to take affect", since = "4.4")
    private String name;

    @Parameter(name = ApiConstants.INSTANCE_NAME, type = CommandType.STRING, description = "Instance name of the User Instance", since = "4.4", authorized = {RoleType.Admin})
    private String instanceName;

    @Parameter(name = ApiConstants.DETAILS, type = CommandType.MAP, description = "Details in key/value pairs. 'extraconfig' is not allowed to be passed in details.")
    protected Map<String, String> details;

    @ACL
    @Parameter(name = ApiConstants.SECURITY_GROUP_IDS,
               type = CommandType.LIST,
               collectionType = CommandType.UUID,
               entityType = SecurityGroupResponse.class,
               description = "List of security group ids to be applied on the Instance.")
    private List<Long> securityGroupIdList;

    @ACL
    @Parameter(name = ApiConstants.SECURITY_GROUP_NAMES,
               type = CommandType.LIST,
               collectionType = CommandType.STRING,
               entityType = SecurityGroupResponse.class,
               description = "Comma separated list of security groups names that going to be applied to the Instance. " +
                       "Should be passed only when Instance is created from a zone with Basic Network support. " +
                       "Mutually exclusive with securitygroupids parameter"
            )
    private List<String> securityGroupNameList;

    @Parameter(name = ApiConstants.CLEAN_UP_DETAILS,
            type = CommandType.BOOLEAN,
            description = "Optional boolean field, which indicates if details should be cleaned up or not (if set to true, details removed for this resource, details field ignored; if false or not set, no action)")
    private Boolean cleanupDetails;

    @Parameter(name = ApiConstants.DHCP_OPTIONS_NETWORK_LIST, type = CommandType.MAP, description = "DHCP options which are passed to the Instance on start up"
            + " Example: dhcpoptionsnetworklist[0].dhcp:114=url&dhcpoptionsetworklist[0].networkid=networkid&dhcpoptionsetworklist[0].dhcp:66=www.test.com")
    private Map dhcpOptionsNetworkList;

    @Parameter(name = ApiConstants.EXTRA_CONFIG, type = CommandType.STRING, since = "4.12", description = "An optional URL encoded string that can be passed to the Instance upon successful deployment", authorized = { RoleType.Admin }, length = 5120)
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

    public Long getUserdataId() {
        return userdataId;
    }

    public Map<String, String> getUserdataDetails() {
        Map<String, String> userdataDetailsMap = new HashMap<String, String>();
        if (userdataDetails != null && userdataDetails.size() != 0) {
            Collection parameterCollection = userdataDetails.values();
            Iterator iter = parameterCollection.iterator();
            while (iter.hasNext()) {
                HashMap<String, String> value = (HashMap<String, String>)iter.next();
                for (Map.Entry<String,String> entry: value.entrySet()) {
                    userdataDetailsMap.put(entry.getKey(),entry.getValue());
                }
            }
        }
        return userdataDetailsMap;
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
                    throw new IllegalArgumentException("No networkid specified when providing extra DHCP options.");
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
        CallContext.current().setEventDetails("Instance Id: " + this._uuidMgr.getUuid(VirtualMachine.class, getId()));
        UserVm result = null;
        try {
            result = _userVmService.updateVirtualMachine(this);
        } catch (CloudRuntimeException e) {
            throw new CloudRuntimeException(String.format("Failed to update Instance, due to: %s", e.getLocalizedMessage()), e);
        }
        if (result != null) {
            UserVmResponse response = _responseGenerator.createUserVmResponse(getResponseView(), "virtualmachine", result).get(0);
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to update Instance");
        }
    }

    @Override
    public void checkUuid() {
        if (getCustomId() != null) {
            _uuidMgr.checkUuid(getCustomId(), UserVm.class);

        }
    }

    @Override
    public Long getApiResourceId() {
        return id;
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.VirtualMachine;
    }
}
