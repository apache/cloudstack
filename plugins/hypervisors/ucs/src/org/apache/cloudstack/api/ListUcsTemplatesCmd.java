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

import com.cloud.exception.*;
import com.cloud.ucs.manager.UcsManager;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.UcsManagerResponse;
import org.apache.cloudstack.api.response.UcsProfileResponse;
import org.apache.cloudstack.api.response.UcsTemplateResponse;
import org.apache.log4j.Logger;

import javax.inject.Inject;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Date: 10/8/13
 * Time: 3:08 PM
 * To change this template use File | Settings | File Templates.
 */
@APICommand(name="listUcsTemplates", description="List templates in ucs manager", responseObject=UcsTemplateResponse.class)
public class ListUcsTemplatesCmd extends BaseListCmd  {
    public static final Logger s_logger = Logger.getLogger(ListUcsTemplatesCmd.class);

    @Inject
    UcsManager mgr;

    @Parameter(name=ApiConstants.UCS_MANAGER_ID, type= BaseCmd.CommandType.UUID,  entityType=UcsManagerResponse.class, description="the id for the ucs manager", required=true)
    private Long ucsManagerId;

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        try {
            ListResponse<UcsTemplateResponse> response = mgr.listUcsTemplates(this);
            response.setResponseName(getCommandName());
            response.setObjectName("ucstemplate");
            this.setResponseObject(response);
        } catch (Exception e) {
            s_logger.warn("Exception: ", e);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, e.getMessage());
        }
    }

    @Override
    public String getCommandName() {
        return "listucstemplatesresponse";
    }

    public Long getUcsManagerId() {
        return ucsManagerId;
    }

    public void setUcsManagerId(Long ucsManagerId) {
        this.ucsManagerId = ucsManagerId;
    }
}
