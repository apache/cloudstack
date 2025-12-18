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

import com.cloud.baremetal.manager.BaremetalVlanManager;
import com.cloud.baremetal.networkservice.BaremetalRctResponse;
import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.context.CallContext;

import javax.inject.Inject;

/**
 * Created by frank on 5/8/14.
 */
@APICommand(name = "addBaremetalRct", description = "adds baremetal rack configuration text", responseObject = BaremetalRctResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false, authorized = {RoleType.Admin})
public class AddBaremetalRctCmd extends BaseAsyncCmd {

    @Inject
    private BaremetalVlanManager vlanMgr;

    @Parameter(name=ApiConstants.BAREMETAL_RCT_URL, required = true, description = "http url to baremetal RCT configuration")
    private String rctUrl;

    public String getRctUrl() {
        return rctUrl;
    }

    public void setRctUrl(String rctUrl) {
        this.rctUrl = rctUrl;
    }

    public String getEventType() {
        return EventTypes.EVENT_BAREMETAL_RCT_ADD;
    }

    @Override
    public String getEventDescription() {
        return "Adding baremetal rct configuration";
    }

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        try {
            BaremetalRctResponse rsp = vlanMgr.addRct(this);
            this.setResponseObject(rsp);
        } catch (Exception e) {
            logger.warn(String.format("unable to add baremetal RCT[%s]", getRctUrl()), e);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, e.getMessage());
        }
    }

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccount().getId();
    }
}
