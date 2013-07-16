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
package org.apache.cloudstack.api.commands;

import javax.inject.Inject;

import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.api.response.HostResponse;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.network.element.SspService;

import org.apache.log4j.Logger;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;

@APICommand(name="deleteStratosphereSsp", responseObject=SuccessResponse.class, description="Removes stratosphere ssp server")
public class DeleteSspCmd extends BaseCmd {
    private static final Logger s_logger = Logger.getLogger(AddSspCmd.class.getName());
    @Inject
    SspService _service;

    @Parameter(name=ApiConstants.HOST_ID, type=CommandType.UUID, entityType=HostResponse.class,
            required=true, description="the host ID of ssp server")
    private Long hostId;

    @Override
    public String getCommandName() {
        return getClass().getAnnotation(APICommand.class).name();
    }

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccount().getId();
    }

    @Override
    public void execute() throws ResourceUnavailableException,
    InsufficientCapacityException, ConcurrentOperationException,
    ResourceAllocationException, NetworkRuleConflictException {
        s_logger.trace("execute");
        SuccessResponse resp = new SuccessResponse();
        resp.setSuccess(_service.deleteSspHost(this));
        this.setResponseObject(resp);
    }

    public Long getHostId() {
        return hostId;
    }

    public void setHostId(Long hostId) {
        this.hostId = hostId;
    }
}
