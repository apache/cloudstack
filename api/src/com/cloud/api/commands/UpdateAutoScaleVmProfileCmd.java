//       Licensed to the Apache Software Foundation (ASF) under one
//       or more contributor license agreements.  See the NOTICE file
//       distributed with this work for additional information
//       regarding copyright ownership.  The ASF licenses this file
//       to you under the Apache License, Version 2.0 (the
//       "License"); you may not use this file except in compliance
//       with the License.  You may obtain a copy of the License at
//
//         http://www.apache.org/licenses/LICENSE-2.0
//
//       Unless required by applicable law or agreed to in writing,
//       software distributed under the License is distributed on an
//       "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
//       KIND, either express or implied.  See the License for the
//       specific language governing permissions and limitations
//       under the License.

package com.cloud.api.commands;

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.BaseAsyncCmd;
import com.cloud.api.BaseCmd;
import com.cloud.api.IdentityMapper;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.AutoScaleVmProfileResponse;
import com.cloud.async.AsyncJob;
import com.cloud.event.EventTypes;
import com.cloud.network.as.AutoScaleVmProfile;
import com.cloud.user.Account;
import com.cloud.user.UserContext;

@Implementation(description = "Updates an existing autoscale vm profile.", responseObject = AutoScaleVmProfileResponse.class)
public class UpdateAutoScaleVmProfileCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(UpdateAutoScaleVmProfileCmd.class.getName());

    private static final String s_name = "updateautoscalevmprofileresponse";

    // ///////////////////////////////////////////////////
    // ////////////// API parameters /////////////////////
    // ///////////////////////////////////////////////////

    @IdentityMapper(entityTableName = "autoscale_vmprofiles")
    @Parameter(name = ApiConstants.ID, type = CommandType.LONG, required = true, description = "the ID of the autoscale vm profile")
    private Long id;

    @IdentityMapper(entityTableName = "vm_template")
    @Parameter(name = ApiConstants.TEMPLATE_ID, type = CommandType.LONG, description = "the template of the auto deployed virtual machine")
    private Long templateId;

    @Parameter(name = ApiConstants.AUTOSCALE_VM_DESTROY_TIME, type = CommandType.INTEGER, description = "the time allowed for existing connections to get closed before a vm is destroyed")
    private Integer destroyVmGraceperiod;

    @Parameter(name = ApiConstants.SNMP_COMMUNITY, type = CommandType.STRING, description = "snmp community string to be used to contact a virtual machine deployed by this profile")
    private String snmpCommunity;

    @Parameter(name = ApiConstants.SNMP_PORT, type = CommandType.INTEGER, description = "port at which snmp agent is listening in a virtual machine deployed by this profile")
    private Integer snmpPort;

    @IdentityMapper(entityTableName = "user")
    @Parameter(name = ApiConstants.AUTOSCALE_USER_ID, type = CommandType.LONG, description = "the ID of the user used to launch and destroy the VMs")
    private Long autoscaleUserId;

    // ///////////////////////////////////////////////////
    // ///////////// API Implementation///////////////////
    // ///////////////////////////////////////////////////

    @Override
    public void execute() {
        UserContext.current().setEventDetails("AutoScale Policy Id: " + getId());
        AutoScaleVmProfile result = _autoScaleService.updateAutoScaleVmProfile(this);
        if (result != null) {
            AutoScaleVmProfileResponse response = _responseGenerator.createAutoScaleVmProfileResponse(result);
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to update autoscale vm profile");
        }
    }

    // ///////////////////////////////////////////////////
    // ///////////////// Accessors ///////////////////////
    // ///////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public Long getTemplateId() {
        return templateId;
    }

    public Integer getSnmpPort() {
        return snmpPort;
    }

    public String getSnmpCommunity() {
        return snmpCommunity;
    }

    public Long getAutoscaleUserId() {
        return autoscaleUserId;
    }

    public Integer getDestroyVmGraceperiod() {
        return destroyVmGraceperiod;
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
    public String getCommandName() {
        return s_name;
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
    public AsyncJob.Type getInstanceType() {
        return AsyncJob.Type.AutoScaleVmProfile;
    }
}
