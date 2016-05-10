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

import javax.inject.Inject;

import org.apache.log4j.Logger;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.context.CallContext;

import com.cloud.baremetal.manager.BaremetalVlanManager;
import com.cloud.baremetal.networkservice.BaremetalSwitchResponse;
import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;

/**
 * @author fridvin
 */
@APICommand(name = "addBaremetalSwitch", description = "adds baremetal switch", responseObject = BaremetalSwitchResponse.class,
        requestHasSensitiveInfo = true, responseHasSensitiveInfo = false, authorized = {RoleType.Admin})
public class AddBaremetalSwitchCmd extends BaseAsyncCmd {
    private static final String s_name = "addbaremetalswitchresponse";
    public static final Logger s_logger = Logger.getLogger(AddBaremetalSwitchCmd.class);

    @Inject
    private BaremetalVlanManager vlanMgr;

    @Parameter(name = ApiConstants.IP_ADDRESS, type = CommandType.STRING, required = true, description = "the IP address of the baremetal switch")
    private String ip;

    @Parameter(name = ApiConstants.USERNAME, type = CommandType.STRING, required = true, description = "the username for the baremetal switch")
    private String username;

    @Parameter(name = ApiConstants.PASSWORD, type = CommandType.STRING, required = true, description = "the password for the baremetal switch")
    private String password;

    @Parameter(name = ApiConstants.TYPE, type = CommandType.STRING, required = true, description = "type of baremetal switch")
    private String type;

    public String getIp() {
        return ip;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getType() {
        return type;
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_BAREMETAL_SWITCH_ADD;
    }

    @Override
    public String getEventDescription() {
        return "Adding baremetal switch";
    }

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException,
            NetworkRuleConflictException {
        BaremetalSwitchResponse rsp = vlanMgr.addSwitch(this);
        if (null != rsp) {
            setResponseObject(rsp);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to add baremetal switch");
        }
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
