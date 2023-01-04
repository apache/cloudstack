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
package org.apache.cloudstack.api.command.user.loadbalancer;

import org.apache.log4j.Logger;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.LBHealthCheckResponse;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.context.CallContext;

import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.network.rules.HealthCheckPolicy;
import com.cloud.network.rules.LoadBalancer;
import com.cloud.user.Account;

@APICommand(name = "deleteLBHealthCheckPolicy", description = "Deletes a load balancer health check policy.", responseObject = SuccessResponse.class, since = "4.2.0",
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class DeleteLBHealthCheckPolicyCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(DeleteLBHealthCheckPolicyCmd.class.getName());
    // ///////////////////////////////////////////////////
    // ////////////// API parameters /////////////////////
    // ///////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID,
               type = CommandType.UUID,
               entityType = LBHealthCheckResponse.class,
               required = true,
               description = "the ID of the load balancer health check policy")
    private Long id;

    // ///////////////////////////////////////////////////
    // ///////////////// Accessors ///////////////////////
    // ///////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    // ///////////////////////////////////////////////////
    // ///////////// API Implementation///////////////////
    // ///////////////////////////////////////////////////

    @Override
    public long getEntityOwnerId() {
        Account account = CallContext.current().getCallingAccount();
        if (account != null) {
            return account.getId();
        }

        return Account.ACCOUNT_ID_SYSTEM; // no account info given, parent this command to SYSTEM so ERROR events are tracked
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_LB_HEALTHCHECKPOLICY_DELETE;
    }

    @Override
    public String getEventDescription() {
        return "deleting load balancer health check policy: " + getId();
    }

    @Override
    public void execute() {
        CallContext.current().setEventDetails("Load balancer health check policy Id: " + getId());
        boolean result = _lbService.deleteLBHealthCheckPolicy(getId(), true);

        if (result) {
            SuccessResponse response = new SuccessResponse(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to delete load balancer health check policy");
        }
    }

    @Override
    public String getSyncObjType() {
        return BaseAsyncCmd.networkSyncObject;
    }

    @Override
    public Long getSyncObjId() {
        HealthCheckPolicy policy = _entityMgr.findById(HealthCheckPolicy.class, getId());
        if (policy == null) {
            throw new InvalidParameterValueException("Unable to find load balancer health check rule: " + id);
        }
        LoadBalancer lb = _lbService.findById(policy.getLoadBalancerId());
        if (lb == null) {
            throw new InvalidParameterValueException("Unable to find load balancer rule for health check rule: " + id);
        }
        return lb.getNetworkId();
    }
}
