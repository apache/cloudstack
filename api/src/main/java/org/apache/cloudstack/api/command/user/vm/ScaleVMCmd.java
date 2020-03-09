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

import com.cloud.offering.ServiceOffering;
import org.apache.log4j.Logger;

import org.apache.cloudstack.acl.SecurityChecker.AccessType;
import org.apache.cloudstack.api.ACL;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject.ResponseView;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.user.UserCmd;
import org.apache.cloudstack.api.response.ServiceOfferingResponse;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.api.response.UserVmResponse;

import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.ManagementServerException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.exception.VirtualMachineMigrationException;
import com.cloud.user.Account;
import com.cloud.uservm.UserVm;
import com.cloud.vm.VirtualMachine;


@APICommand(name = "scaleVirtualMachine", description = "Scales the virtual machine to a new service offering.", responseObject = SuccessResponse.class, responseView = ResponseView.Restricted, entityType = {VirtualMachine.class},
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class ScaleVMCmd extends BaseAsyncCmd implements UserCmd {
    public static final Logger s_logger = Logger.getLogger(ScaleVMCmd.class.getName());
    private static final String s_name = "scalevirtualmachineresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @ACL(accessType = AccessType.OperateEntry)
    @Parameter(name=ApiConstants.ID, type=CommandType.UUID, entityType=UserVmResponse.class,
            required=true, description="The ID of the virtual machine")
    private Long id;

    @Parameter(name=ApiConstants.SERVICE_OFFERING_ID, type=CommandType.UUID, entityType=ServiceOfferingResponse.class,
            required=true, description="the ID of the service offering for the virtual machine")
    private Long serviceOfferingId;

    @Parameter(name = ApiConstants.DETAILS, type = BaseCmd.CommandType.MAP, description = "name value pairs of custom parameters for cpu,memory and cpunumber. example details[i].name=value")
    private Map<String, String> details;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public Long getServiceOfferingId() {
        return serviceOfferingId;
    }

    //instead of reading a map directly we are using collections.
    //it is because details.values() cannot be cast to a map.
    //it gives a exception
    public Map<String, String> getDetails() {
        Map<String, String> customparameterMap = new HashMap<String, String>();
        if (details != null && details.size() != 0) {
            Collection parameterCollection = details.values();
            Iterator iter = parameterCollection.iterator();
            while (iter.hasNext()) {
                HashMap<String, String> value = (HashMap<String, String>)iter.next();
                for (String key : value.keySet()) {
                    customparameterMap.put(key, value.get(key));
                }
            }
        }
        return customparameterMap;
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
    public long getEntityOwnerId() {
        UserVm userVm = _entityMgr.findById(UserVm.class, getId());
        if (userVm != null) {
            return userVm.getAccountId();
        }

        return Account.ACCOUNT_ID_SYSTEM; // no account info given, parent this command to SYSTEM so ERROR events are tracked
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_VM_UPGRADE;
    }

    @Override
    public String getEventDescription() {
        return  "upgrading vm: " + this._uuidMgr.getUuid(VirtualMachine.class, getId()) + " to service offering: " + this._uuidMgr.getUuid(ServiceOffering.class, getServiceOfferingId());
    }

    @Override
    public void execute() {
        UserVm result;
        try {
            result = _userVmService.upgradeVirtualMachine(this);
        } catch (ResourceUnavailableException ex) {
            s_logger.warn("Exception: ", ex);
            throw new ServerApiException(ApiErrorCode.RESOURCE_UNAVAILABLE_ERROR, ex.getMessage());
        } catch (ConcurrentOperationException ex) {
            s_logger.warn("Exception: ", ex);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, ex.getMessage());
        } catch (ManagementServerException ex) {
            s_logger.warn("Exception: ", ex);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, ex.getMessage());
        } catch (VirtualMachineMigrationException ex) {
            s_logger.warn("Exception: ", ex);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, ex.getMessage());
        }
        if (result != null){
            List<UserVmResponse> responseList = _responseGenerator.createUserVmResponse(getResponseView(), "virtualmachine", result);
            UserVmResponse response = responseList.get(0);
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to scale vm");
        }
    }
}
