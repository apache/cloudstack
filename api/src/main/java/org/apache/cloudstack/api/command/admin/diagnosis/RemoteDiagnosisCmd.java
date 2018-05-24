/*
 * // Licensed to the Apache Software Foundation (ASF) under one
 * // or more contributor license agreements.  See the NOTICE file
 * // distributed with this work for additional information
 * // regarding copyright ownership.  The ASF licenses this file
 * // to you under the Apache License, Version 2.0 (the
 * // "License"); you may not use this file except in compliance
 * // with the License.  You may obtain a copy of the License at
 * //
 * //   http://www.apache.org/licenses/LICENSE-2.0
 * //
 * // Unless required by applicable law or agreed to in writing,
 * // software distributed under the License is distributed on an
 * // "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * // KIND, either express or implied.  See the License for the
 * // specific language governing permissions and limitations
 * // under the License.
 */

package org.apache.cloudstack.api.command.admin.diagnosis;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.user.Account;
import com.cloud.vm.VirtualMachine;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.RemoteDiagnosisResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.diangosis.RemoteDiagnosisService;

import javax.inject.Inject;

@APICommand(name = RemoteDiagnosisCmd.APINAME,
        since = "4.11",
        description = "Execute network utility command (ping/arping/tracert from a remote host",
        responseHasSensitiveInfo = false,
        requestHasSensitiveInfo = false,
        responseObject = RemoteDiagnosisResponse.class,
        entityType = VirtualMachine.class,
        authorized = RoleType.Admin)
public class RemoteDiagnosisCmd extends BaseCmd {

    public static final String APINAME = "remoteDiganosis";

    @Inject
    private RemoteDiagnosisService diagnosisService;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @Parameter(name = ApiConstants.ID,
            description = "The ID of the System VM instance",
            required = true,
            type = CommandType.UUID,
            entityType = RemoteDiagnosisResponse.class)
    private Long id;

    @Parameter(name = ApiConstants.IP_ADDRESS,
            description = "Destination IP address to ping",
            required = true,
            type = CommandType.STRING)
    private String ipaddress;

    @Parameter(name = ApiConstants.DIAGNOSIS_TYPE,
            description = "The type of command to be executed inside the System VM instance, e.g. ping, tracert or arping",
            type = CommandType.STRING)
    private String diagnosisType;
    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////
    public Long getId() {
        return id;
    }

    public String getIpaddress() {
        return ipaddress;
    }

    public String getDiagnosisType() {
        return diagnosisType;
    }
    /////////////////////////////////////////////////////
    /////////////////// Implementation //////////////////
    /////////////////////////////////////////////////////
    @Override
    public String getCommandName() {
        return APINAME.toLowerCase() + BaseCmd.RESPONSE_SUFFIX;
    }

    @Override
    public long getEntityOwnerId() {
        Account account = CallContext.current().getCallingAccount();
        if (account != null){
            return account.getId();
        }
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        RemoteDiagnosisResponse diagnosisResponse = diagnosisService.pingAddress(this);
        diagnosisResponse.setObjectName("diagnosis");
        diagnosisResponse.setResponseName(getCommandName());
        this.setResponseObject(diagnosisResponse);
    }
}
