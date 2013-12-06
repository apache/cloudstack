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
//
package org.apache.cloudstack.api;

import com.cloud.event.EventTypes;
import com.cloud.exception.*;
import com.cloud.ucs.manager.UcsManager;
import com.cloud.user.Account;
import org.apache.cloudstack.api.response.UcsBladeResponse;
import org.apache.cloudstack.api.response.UcsManagerResponse;
import org.apache.log4j.Logger;

import javax.inject.Inject;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Date: 10/8/13
 * Time: 3:17 PM
 * To change this template use File | Settings | File Templates.
 */
@APICommand(name="instantiateUcsTemplateAndAssocaciateToBlade", description="create a profile of template and associate to a blade", responseObject=UcsBladeResponse.class)
public class InstantiateUcsTemplateAndAssociateToBladeCmd extends BaseAsyncCmd{
    public static final Logger s_logger = Logger.getLogger(InstantiateUcsTemplateAndAssociateToBladeCmd.class);

    @Inject
    private UcsManager mgr;

    @Parameter(name=ApiConstants.UCS_MANAGER_ID, type= BaseCmd.CommandType.UUID, description="ucs manager id", entityType=UcsManagerResponse.class, required=true)
    private Long ucsManagerId;
    @Parameter(name=ApiConstants.UCS_TEMPLATE_DN, type= BaseCmd.CommandType.STRING, description="template dn", required=true)
    private String templateDn;
    @Parameter(name=ApiConstants.UCS_BLADE_ID, type= BaseCmd.CommandType.UUID, entityType=UcsBladeResponse.class, description="blade id", required=true)
    private Long bladeId;
    @Parameter(name=ApiConstants.UCS_PROFILE_NAME, type= BaseCmd.CommandType.STRING, description="profile name")
    private String profileName;

    @Override
    public String getEventType() {
        return EventTypes.EVENT_UCS_INSTANTIATE_TEMPLATE_AND_ASSOCIATE;
    }

    @Override
    public String getEventDescription() {
        return "create a profile off template and associate to a blade";
    }

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        try {
            UcsBladeResponse rsp = mgr.instantiateTemplateAndAssociateToBlade(this);
            rsp.setResponseName(getCommandName());
            this.setResponseObject(rsp);
        } catch (Exception e) {
            s_logger.warn("Exception: ", e);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, e.getMessage());
        }
    }

    @Override
    public String getCommandName() {
        return "instantiateucstemplateandassociatetobladeresponse";
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    public Long getUcsManagerId() {
        return ucsManagerId;
    }

    public void setUcsManagerId(Long ucsManagerId) {
        this.ucsManagerId = ucsManagerId;
    }

    public String getTemplateDn() {
        return templateDn;
    }

    public void setTemplateDn(String templateDn) {
        this.templateDn = templateDn;
    }

    public Long getBladeId() {
        return bladeId;
    }

    public void setBladeId(Long bladeId) {
        this.bladeId = bladeId;
    }

    public String getProfileName() {
        return profileName;
    }

    public void setProfileName(String profileName) {
        this.profileName = profileName;
    }
}
