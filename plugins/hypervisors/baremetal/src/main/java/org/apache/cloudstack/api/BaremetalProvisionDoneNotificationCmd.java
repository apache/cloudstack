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

import com.cloud.baremetal.manager.BaremetalManager;
import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.context.CallContext;

import javax.inject.Inject;
import org.apache.log4j.Logger;

/**
 * Created by frank on 9/17/14.
 */
@APICommand(name = "notifyBaremetalProvisionDone", description = "Notify provision has been done on a host. This api is for baremetal virtual router service, not for end user", responseObject = SuccessResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class BaremetalProvisionDoneNotificationCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(BaremetalProvisionDoneNotificationCmd.class);
    private static final String s_name = "baremetalprovisiondone";

    @Inject
    private BaremetalManager bmMgr;

    @Parameter(name="mac", required = true, description = "mac of the nic used for provision")
    private String mac;

    @Override
    public String getEventType() {
        return EventTypes.EVENT_BAREMETAL_PROVISION_DONE;
    }

    @Override
    public String getEventDescription() {
        return "notify management server that baremetal provision has been done on a host";
    }

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        try {
            bmMgr.notifyProvisionDone(this);
            this.setResponseObject(new SuccessResponse(getCommandName()));
        } catch (Exception e) {
            s_logger.warn(String.format("unable to notify baremetal provision done[mac:%s]", mac), e);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, e.getMessage());
        }
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccount().getId();
    }
}
