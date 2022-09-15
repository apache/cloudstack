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
package org.apache.cloudstack.api.command.user.autoscale;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCreateCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.AutoScaleVmProfileResponse;
import org.apache.cloudstack.api.response.ServiceOfferingResponse;
import org.apache.cloudstack.api.response.TemplateResponse;
import org.apache.cloudstack.api.response.UserResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.cloudstack.context.CallContext;

import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.network.as.AutoScaleVmProfile;
import com.cloud.user.Account;
import com.cloud.user.User;

@APICommand(name = "createAutoScaleVmProfile",
            description = "Creates a profile that contains information about the virtual machine which will be provisioned automatically by autoscale feature.",
        responseObject = AutoScaleVmProfileResponse.class, entityType = {AutoScaleVmProfile.class},
            requestHasSensitiveInfo = false,
            responseHasSensitiveInfo = false)
@SuppressWarnings("rawtypes")
public class CreateAutoScaleVmProfileCmd extends BaseAsyncCreateCmd {
    public static final Logger s_logger = Logger.getLogger(CreateAutoScaleVmProfileCmd.class.getName());

    private static final String s_name = "autoscalevmprofileresponse";

    // ///////////////////////////////////////////////////
    // ////////////// API parameters /////////////////////
    // ///////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ZONE_ID,
               type = CommandType.UUID,
               entityType = ZoneResponse.class,
               required = true,
               description = "availability zone for the auto deployed virtual machine")
    private Long zoneId;

    @Parameter(name = ApiConstants.SERVICE_OFFERING_ID,
               type = CommandType.UUID,
               entityType = ServiceOfferingResponse.class,
               required = true,
               description = "the service offering of the auto deployed virtual machine")
    private Long serviceOfferingId;

    @Parameter(name = ApiConstants.TEMPLATE_ID,
               type = CommandType.UUID,
               entityType = TemplateResponse.class,
               required = true,
               description = "the template of the auto deployed virtual machine")
    private Long templateId;

    @Parameter(name = ApiConstants.OTHER_DEPLOY_PARAMS,
               type = CommandType.STRING,
               description = "parameters other than zoneId/serviceOfferringId/templateId of the auto deployed virtual machine")
    private String otherDeployParams;

    @Parameter(name = ApiConstants.AUTOSCALE_VM_DESTROY_TIME,
               type = CommandType.INTEGER,
               description = "the time allowed for existing connections to get closed before a vm is destroyed")
    private Integer destroyVmGraceperiod;

    @Parameter(name = ApiConstants.COUNTERPARAM_LIST,
               type = CommandType.MAP,
               description = "counterparam list. Example: counterparam[0].name=snmpcommunity&counterparam[0].value=public&counterparam[1].name=snmpport&counterparam[1].value=161")
    private Map counterParamList;

    @Parameter(name = ApiConstants.AUTOSCALE_USER_ID,
               type = CommandType.UUID,
               entityType = UserResponse.class,
               description = "the ID of the user used to launch and destroy the VMs")
    private Long autoscaleUserId;

    @Parameter(name = ApiConstants.FOR_DISPLAY, type = CommandType.BOOLEAN, description = "an optional field, whether to the display the profile to the end user or not", since = "4.4", authorized = {RoleType.Admin})
    private Boolean display;

    private Map<String, String> otherDeployParamMap;

    // ///////////////////////////////////////////////////
    // ///////////////// Accessors ///////////////////////
    // ///////////////////////////////////////////////////

    private Long domainId;
    private Long accountId;

    public Long getDomainId() {
        if (domainId == null) {
            getAccountId();
        }
        return domainId;
    }

    public Long getZoneId() {
        return zoneId;
    }

    public Long getServiceOfferingId() {
        return serviceOfferingId;
    }

    public Long getTemplateId() {
        return templateId;
    }

    @Deprecated
    public Boolean getDisplay() {
        return display;
    }

    @Override
    public boolean isDisplay() {
        if(display == null)
            return true;
        else
            return display;
    }

    public Map getCounterParamList() {
        return counterParamList;
    }

    public String getOtherDeployParams() {
        return otherDeployParams;
    }

    public long getAutoscaleUserId() {
        if (autoscaleUserId != null) {
            return autoscaleUserId;
        } else {
            return CallContext.current().getCallingUserId();
        }
    }

    public Integer getDestroyVmGraceperiod() {
        return destroyVmGraceperiod;
    }

    public long getAccountId() {
        if (accountId != null) {
            return accountId;
        }
        Account account = null;
        if (autoscaleUserId != null) {
            User user = _entityMgr.findById(User.class, autoscaleUserId);
            account = _entityMgr.findById(Account.class, user.getAccountId());
        } else {
            account = CallContext.current().getCallingAccount();
        }
        accountId = account.getAccountId();
        domainId = account.getDomainId();
        return accountId;
    }

    private void createOtherDeployParamMap() {
        if (otherDeployParamMap == null) {
            otherDeployParamMap = new HashMap<String, String>();
        }
        if (otherDeployParams == null)
            return;
        String[] keyValues = otherDeployParams.split("&"); // hostid=123, hypervisor=xenserver
        for (String keyValue : keyValues) { // keyValue == "hostid=123"
            String[] keyAndValue = keyValue.split("="); // keyValue = hostid, 123
            if (keyAndValue.length != 2) {
                throw new InvalidParameterValueException("Invalid parameter in otherDeployParam : " + keyValue);
            }
            String paramName = keyAndValue[0]; // hostid
            String paramValue = keyAndValue[1]; // 123
            otherDeployParamMap.put(paramName, paramValue);
        }
    }

    public HashMap<String, String> getDeployParamMap() {
        createOtherDeployParamMap();
        HashMap<String, String> deployParams = new HashMap<String, String>(otherDeployParamMap);
        deployParams.put("command", "deployVirtualMachine");
        deployParams.put("zoneId", zoneId.toString());
        deployParams.put("serviceOfferingId", serviceOfferingId.toString());
        deployParams.put("templateId", templateId.toString());
        return deployParams;
    }

    public String getOtherDeployParam(String param) {
        if (param == null) {
            return null;
        }
        createOtherDeployParamMap();
        return otherDeployParamMap.get(param);
    }

    // ///////////////////////////////////////////////////
    // ///////////// API Implementation///////////////////
    // ///////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    public static String getResultObjectName() {
        return "autoscalevmprofile";
    }

    @Override
    public long getEntityOwnerId() {
        return getAccountId();
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_AUTOSCALEVMPROFILE_CREATE;
    }

    @Override
    public String getEventDescription() {
        return "creating AutoScale Vm Profile";
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.AutoScaleVmProfile;
    }

    @Override
    public void execute() {
        AutoScaleVmProfile result = _entityMgr.findById(AutoScaleVmProfile.class, getEntityId());
        AutoScaleVmProfileResponse response = _responseGenerator.createAutoScaleVmProfileResponse(result);
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }

    @Override
    public void create() throws ResourceAllocationException {

        AutoScaleVmProfile result = _autoScaleService.createAutoScaleVmProfile(this);
        if (result != null) {
            setEntityId(result.getId());
            setEntityUuid(result.getUuid());
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to create Autoscale Vm Profile");
        }
    }
}
