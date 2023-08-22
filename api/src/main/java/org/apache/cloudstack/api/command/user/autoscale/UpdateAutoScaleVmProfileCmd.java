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


import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.acl.SecurityChecker.AccessType;
import org.apache.cloudstack.api.ACL;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCustomIdCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.AutoScaleVmProfileResponse;
import org.apache.cloudstack.api.response.ServiceOfferingResponse;
import org.apache.cloudstack.api.response.TemplateResponse;
import org.apache.cloudstack.api.response.UserDataResponse;
import org.apache.cloudstack.api.response.UserResponse;
import org.apache.cloudstack.context.CallContext;

import com.cloud.event.EventTypes;
import com.cloud.network.as.AutoScaleVmProfile;
import com.cloud.user.Account;

@APICommand(name = "updateAutoScaleVmProfile", description = "Updates an existing autoscale vm profile.", responseObject = AutoScaleVmProfileResponse.class, entityType = {AutoScaleVmProfile.class},
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class UpdateAutoScaleVmProfileCmd extends BaseAsyncCustomIdCmd {


    // ///////////////////////////////////////////////////
    // ////////////// API parameters /////////////////////
    // ///////////////////////////////////////////////////

    @ACL(accessType = AccessType.OperateEntry)
    @Parameter(name = ApiConstants.ID,
               type = CommandType.UUID,
               entityType = AutoScaleVmProfileResponse.class,
               required = true,
               description = "the ID of the autoscale vm profile")
    private Long id;

    @Parameter(name = ApiConstants.SERVICE_OFFERING_ID,
            type = CommandType.UUID,
            entityType = ServiceOfferingResponse.class,
            description = "the service offering of the auto deployed virtual machine",
            since = "4.18.0")
    private Long serviceOfferingId;

    @Parameter(name = ApiConstants.TEMPLATE_ID,
               type = CommandType.UUID,
               entityType = TemplateResponse.class,
               description = "the template of the auto deployed virtual machine")
    private Long templateId;

    @Parameter(name = ApiConstants.AUTOSCALE_EXPUNGE_VM_GRACE_PERIOD,
               type = CommandType.INTEGER,
               description = "the time allowed for existing connections to get closed before a vm is destroyed")
    private Integer expungeVmGracePeriod;

    @Parameter(name = ApiConstants.COUNTERPARAM_LIST,
               type = CommandType.MAP,
               description = "counterparam list. Example: counterparam[0].name=snmpcommunity&counterparam[0].value=public&counterparam[1].name=snmpport&counterparam[1].value=161")
    private Map counterParamList;

    @Parameter(name = ApiConstants.OTHER_DEPLOY_PARAMS,
            type = CommandType.MAP,
            description = "parameters other than zoneId/serviceOfferringId/templateId of the auto deployed virtual machine. \n"
                    + "Example: otherdeployparams[0].name=serviceofferingid&otherdeployparams[0].value=a7fb50f6-01d9-11ed-8bc1-77f8f0228926&otherdeployparams[1].name=rootdisksize&otherdeployparams[1].value=10 .\n"
                    + "Possible parameters are \"rootdisksize\", \"diskofferingid\",\"size\", \"securitygroupids\", \"overridediskofferingid\", \"keypairs\", \"affinitygroupids'\" and \"networkids\".",
            since = "4.18.0")
    private Map<String, HashMap<String, String>> otherDeployParams;

    @Parameter(name = ApiConstants.USER_DATA,
            type = CommandType.STRING,
            description = "an optional binary data that can be sent to the virtual machine upon a successful deployment. " +
                    "This binary data must be base64 encoded before adding it to the request. " +
                    "Using HTTP GET (via querystring), you can send up to 4KB of data after base64 encoding. " +
                    "Using HTTP POST(via POST body), you can send up to 1MB of data after base64 encoding." +
                    "You also need to change vm.userdata.max.length value",
            length = 1048576,
            since = "4.18.0")
    private String userData;

    @Parameter(name = ApiConstants.USER_DATA_ID, type = CommandType.UUID, entityType = UserDataResponse.class, description = "the ID of the userdata",
            since = "4.18.1")
    private Long userDataId;

    @Parameter(name = ApiConstants.USER_DATA_DETAILS, type = CommandType.MAP, description = "used to specify the parameters values for the variables in userdata.",
            since = "4.18.1")
    private Map userDataDetails;

    @Parameter(name = ApiConstants.AUTOSCALE_USER_ID,
               type = CommandType.UUID,
               entityType = UserResponse.class,
               description = "the ID of the user used to launch and destroy the VMs")
    private Long autoscaleUserId;

    @Parameter(name = ApiConstants.FOR_DISPLAY, type = CommandType.BOOLEAN, description = "an optional field, whether to the display the profile to the end user or not", since = "4.4", authorized = {RoleType.Admin})
    private Boolean display;

    // ///////////////////////////////////////////////////
    // ///////////// API Implementation///////////////////
    // ///////////////////////////////////////////////////

    @Override
    public void execute() {
        CallContext.current().setEventDetails("AutoScale Policy Id: " + getId());
        AutoScaleVmProfile result = _autoScaleService.updateAutoScaleVmProfile(this);
        if (result != null) {
            AutoScaleVmProfileResponse response = _responseGenerator.createAutoScaleVmProfileResponse(result);
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to update autoscale vm profile");
        }
    }

    // ///////////////////////////////////////////////////
    // ///////////////// Accessors ///////////////////////
    // ///////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public Long getServiceOfferingId() {
        return serviceOfferingId;
    }

    public Long getTemplateId() {
        return templateId;
    }

    public Map<String, HashMap<String, String>> getOtherDeployParams() {
        return otherDeployParams;
    }

    public Map getCounterParamList() {
        return counterParamList;
    }

    public String getUserData() {
        return userData;
    }

    public Long getUserDataId() {
        return userDataId;
    }

    public Map<String, String> getUserDataDetails() {
        return convertDetailsToMap(userDataDetails);
    }

    public Long getAutoscaleUserId() {
        return autoscaleUserId;
    }

    public Integer getExpungeVmGracePeriod() {
        return expungeVmGracePeriod;
    }

    public Boolean getDisplay() {
        return display;
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_AUTOSCALEVMPROFILE_UPDATE;
    }

    @Override
    public String getEventDescription() {
        return "Updating AutoScale Vm Profile. Vm Profile Id: " + getId();
    }

    @Override
    public long getEntityOwnerId() {
        AutoScaleVmProfile vmProfile = _entityMgr.findById(AutoScaleVmProfile.class, getId());
        if (vmProfile != null) {
            return vmProfile.getAccountId();
        }
        return Account.ACCOUNT_ID_SYSTEM; // no account info given, parent this command to SYSTEM so ERROR events are
        // tracked
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.AutoScaleVmProfile;
    }

    @Override
    public void checkUuid() {
        if (getCustomId() != null) {
            _uuidMgr.checkUuid(getCustomId(), AutoScaleVmProfile.class);
        }
    }
}
