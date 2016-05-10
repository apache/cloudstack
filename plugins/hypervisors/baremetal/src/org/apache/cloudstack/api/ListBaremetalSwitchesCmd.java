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

import java.util.List;

import javax.inject.Inject;

import org.apache.log4j.Logger;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.response.ListResponse;

import com.cloud.baremetal.manager.BaremetalVlanManager;
import com.cloud.baremetal.networkservice.BaremetalSwitchResponse;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;

/**
 * @author fridvin
 */
@APICommand(name = "listBaremetalSwitches", description = "list baremetal switches", responseObject = BaremetalSwitchResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false, authorized = {RoleType.Admin})
public class ListBaremetalSwitchesCmd extends BaseListCmd {
    private static final String s_name = "listbaremetalswitchesresponse";
    private static final Logger s_logger = Logger.getLogger(ListBaremetalSwitchesCmd.class);

    @Inject
    BaremetalVlanManager vlanMgr;

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException,
            NetworkRuleConflictException {
        try {
            ListResponse<BaremetalSwitchResponse> response = new ListResponse<>();
            List<BaremetalSwitchResponse> switchResponses = vlanMgr.listSwitches();
            response.setResponses(switchResponses);
            response.setResponseName(getCommandName());
            response.setObjectName("baremetalswitches");
            setResponseObject(response);
        } catch (Exception e) {
            s_logger.debug("Exception happened while executing ListBaremetalSwitchesCmd", e);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, e.getMessage());
        }
    }

    @Override
    public String getCommandName() {
        return s_name;
    }
}
