/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.api.command.user.vnf;

import org.apache.cloudstack.api.*;
import org.apache.cloudstack.api.response.*;
import javax.inject.Inject;

@APICommand(name="attachVnfTemplate", responseObject=SuccessResponse.class,
        description="Bind an existing VM as the VNF for the network")
public class AttachVnfTemplateCmd extends BaseAsyncCmd {
    @Parameter(name="networkid", type=CommandType.UUID, entityType=NetworkResponse.class, required=true) private Long networkId;
    @Parameter(name="vmid", type=CommandType.UUID, entityType=UserVmResponse.class, required=true) private Long vmId;

    @Inject private org.apache.cloudstack.vnf.VnfNetworkService vnfSvc;
    @Override public void execute() {
        vnfSvc.attachVnfVm(networkId, vmId, getEntityOwnerId());
        setResponseObject(new SuccessResponse(getCommandName()));
    }
    @Override public String getCommandName(){return "attachvnftemplateresponse";}
    @Override public long getEntityOwnerId(){return CallContext.current().getCallingAccountId();}
}
