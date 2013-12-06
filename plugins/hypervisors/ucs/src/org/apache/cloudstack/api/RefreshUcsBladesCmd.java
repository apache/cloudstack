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
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.UcsBladeResponse;
import org.apache.cloudstack.api.response.UcsManagerResponse;
import org.apache.log4j.Logger;

import javax.inject.Inject;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Date: 10/3/13
 * Time: 2:18 PM
 * To change this template use File | Settings | File Templates.
 */
@APICommand(name="refreshUcsBlades", description="refresh ucs blades to sync with UCS manager", responseObject=UcsBladeResponse.class)
public class RefreshUcsBladesCmd  extends BaseListCmd  {
    private static Logger s_logger = Logger.getLogger(RefreshUcsBladesCmd.class);

    @Inject
    private UcsManager mgr;

    @Parameter(name=ApiConstants.UCS_MANAGER_ID, type= BaseCmd.CommandType.UUID, description="ucs manager id", entityType=UcsManagerResponse.class, required=true)
    private Long ucsManagerId;

    public UcsManager getMgr() {
        return mgr;
    }

    public void setMgr(UcsManager mgr) {
        this.mgr = mgr;
    }

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        try {
            ListResponse<UcsBladeResponse> response = mgr.refreshBlades(ucsManagerId);
            response.setResponseName(getCommandName());
            response.setObjectName("ucsblade");
            this.setResponseObject(response);
        } catch (Exception e) {
            s_logger.warn("unhandled exception:", e);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, e.getMessage());
        }
    }

    @Override
    public String getCommandName() {
        return "refreshucsbladesresponse";
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }
}
