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

import java.util.Map;

import org.apache.log4j.Logger;

import org.apache.cloudstack.acl.SecurityChecker.AccessType;
import org.apache.cloudstack.api.ACL;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject.ResponseView;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.user.UserCmd;
import org.apache.cloudstack.api.response.ServiceOfferingResponse;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.context.CallContext;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.offering.ServiceOffering;
import com.cloud.user.Account;
import com.cloud.uservm.UserVm;
import com.cloud.vm.VirtualMachine;

@Deprecated(since = "4.18")
@APICommand(name = "changeServiceForVirtualMachine", responseObject=UserVmResponse.class, description="(This API is deprecated, use scaleVirtualMachine API)" +
        "Changes the service offering for a virtual machine. The virtual machine must be in a \"Stopped\" state for " +
        "this command to take effect.", responseView = ResponseView.Restricted, entityType = {VirtualMachine.class},
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = true)
public class UpgradeVMCmd extends BaseCmd implements UserCmd {
    public static final Logger s_logger = Logger.getLogger(UpgradeVMCmd.class.getName());
    private static final String s_name = "changeserviceforvirtualmachineresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @ACL(accessType = AccessType.OperateEntry)
    @Parameter(name=ApiConstants.ID, type=CommandType.UUID, entityType=UserVmResponse.class,
            required=true, description="The ID of the virtual machine")
    private Long id;

    @Parameter(name=ApiConstants.SERVICE_OFFERING_ID, type=CommandType.UUID, entityType=ServiceOfferingResponse.class,
            required=true, description="the service offering ID to apply to the virtual machine")
    protected Long serviceOfferingId;

    @Parameter(name = ApiConstants.DETAILS, type = CommandType.MAP, description = "name value pairs of custom parameters for cpuspeed, memory and cpunumber. example details[i].name=value")
    private Map<String, String> details;

    @Parameter(name = ApiConstants.MIN_IOPS, type = CommandType.LONG, required = false, description = "New minimum number of IOPS for the custom disk offering", since = "4.17")
    private Long minIops;

    @Parameter(name = ApiConstants.MAX_IOPS, type = CommandType.LONG, required = false, description = "New maximum number of IOPS for the custom disk offering", since = "4.17")
    private Long maxIops;

    @Parameter(name = ApiConstants.AUTO_MIGRATE, type = CommandType.BOOLEAN, required = false, description = "Flag for automatic migration of the root volume " +
            "with new compute offering whenever migration is required to apply the offering", since = "4.17")
    private Boolean autoMigrate;

    @Parameter(name = ApiConstants.SHRINK_OK, type = CommandType.BOOLEAN, required = false, description = "Verify OK to Shrink", since = "4.17")
    private Boolean shrinkOk;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public Long getServiceOfferingId() {
        return serviceOfferingId;
    }

    public Map<String, String> getDetails() {
        Map<String, String> customparameterMap = convertDetailsToMap(details);

        if (shrinkOk != null) customparameterMap.put(ApiConstants.SHRINK_OK, String.valueOf(isShrinkOk()));
        if (autoMigrate != null) customparameterMap.put(ApiConstants.AUTO_MIGRATE, String.valueOf(getAutoMigrate()));
        if (getMinIops() != null) customparameterMap.put(ApiConstants.MIN_IOPS, String.valueOf(getMinIops()));
        if (getMaxIops() != null) customparameterMap.put(ApiConstants.MAX_IOPS, String.valueOf(getMaxIops()));

        return customparameterMap;
    }

    public Long getMinIops() {
        return minIops;
    }

    public Long getMaxIops() {
        return maxIops;
    }

    public boolean getAutoMigrate() {
        return autoMigrate == null ? true : autoMigrate;
    }

    public boolean isShrinkOk() { return shrinkOk == null ? true : shrinkOk; }

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
    public long getEntityOwnerId() {
        UserVm userVm = _entityMgr.findById(UserVm.class, getId());
        if (userVm != null) {
            return userVm.getAccountId();
        }

        return Account.ACCOUNT_ID_SYSTEM; // no account info given, parent this command to SYSTEM so ERROR events are tracked
    }

    @Override
    public void execute() throws ResourceAllocationException {
        CallContext.current().setEventDetails("Vm Id: " + this._uuidMgr.getUuid(VirtualMachine.class, getId()));

        ServiceOffering serviceOffering = _entityMgr.findById(ServiceOffering.class, serviceOfferingId);
        if (serviceOffering == null) {
            throw new InvalidParameterValueException("Unable to find service offering: " + serviceOfferingId);
        }

        UserVm result = _userVmService.upgradeVirtualMachine(this);

        if (result != null){
            UserVmResponse response = _responseGenerator.createUserVmResponse(getResponseView(), "virtualmachine", result).get(0);
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to upgrade vm");
        }
    }
}
