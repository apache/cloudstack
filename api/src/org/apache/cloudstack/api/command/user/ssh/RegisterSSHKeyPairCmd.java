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
package org.apache.cloudstack.api.command.user.ssh;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.ProjectResponse;
import org.apache.cloudstack.api.response.SSHKeyPairResponse;
import org.apache.cloudstack.context.CallContext;

import org.apache.log4j.Logger;

import com.cloud.user.SSHKeyPair;

@APICommand(name = "registerSSHKeyPair", description="Register a public key in a keypair under a certain name", responseObject=SSHKeyPairResponse.class)
public class RegisterSSHKeyPairCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(RegisterSSHKeyPairCmd.class.getName());
    private static final String s_name = "registersshkeypairresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.NAME, type=CommandType.STRING, required=true, description="Name of the keypair")
    private String name;

    @Parameter(name="publickey", type=CommandType.STRING, required=true, description="Public key material of the keypair", length=5120)
    private String publicKey;

    //Owner information
    @Parameter(name=ApiConstants.ACCOUNT, type=CommandType.STRING, description="an optional account for the ssh key. Must be used with domainId.")
    private String accountName;

    @Parameter(name=ApiConstants.DOMAIN_ID, type=CommandType.UUID, entityType = DomainResponse.class,
            description="an optional domainId for the ssh key. If the account parameter is used, domainId must also be used.")
    private Long domainId;

    @Parameter(name=ApiConstants.PROJECT_ID, type=CommandType.UUID, entityType = ProjectResponse.class,
            description="an optional project for the ssh key")
    private Long projectId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getName() {
        return name;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public String getAccountName() {
        return accountName;
    }

    public Long getDomainId() {
        return domainId;
    }

    public Long getProjectId() {
        return projectId;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public long getEntityOwnerId() {
        Long accountId = finalyzeAccountId(accountName, domainId, projectId, true);
        if (accountId == null) {
            return CallContext.current().getCallingAccount().getId();
        }

        return accountId;
    }

    @Override
    public void execute() {
        SSHKeyPair result = _mgr.registerSSHKeyPair(this);
        SSHKeyPairResponse response = new SSHKeyPairResponse(result.getName(), result.getFingerprint());
        response.setResponseName(getCommandName());
        response.setObjectName("keypair");
        this.setResponseObject(response);
    }

    @Override
    public String getCommandName() {
        return s_name;
    }

}
