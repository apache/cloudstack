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

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListProjectAndAccountResourcesCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.SSHKeyPairResponse;

import com.cloud.user.SSHKeyPair;
import com.cloud.utils.Pair;

@APICommand(name = "listSSHKeyPairs", description = "List registered keypairs", responseObject = SSHKeyPairResponse.class, entityType = {SSHKeyPair.class},
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class ListSSHKeyPairsCmd extends BaseListProjectAndAccountResourcesCmd {
    public static final Logger s_logger = Logger.getLogger(ListSSHKeyPairsCmd.class.getName());
    private static final String s_name = "listsshkeypairsresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = SSHKeyPairResponse.class, description = "the ID of the ssh keypair")
    private Long id;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, description = "A key pair name to look for")
    private String name;

    @Parameter(name = "fingerprint", type = CommandType.STRING, description = "A public key fingerprint to look for")
    private String fingerprint;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////
    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() {
        Pair<List<? extends SSHKeyPair>, Integer> resultList = _mgr.listSSHKeyPairs(this);
        List<SSHKeyPairResponse> responses = new ArrayList<SSHKeyPairResponse>();
        for (SSHKeyPair result : resultList.first()) {
            SSHKeyPairResponse r = _responseGenerator.createSSHKeyPairResponse(result, false);
            r.setObjectName("sshkeypair");
            responses.add(r);
        }

        ListResponse<SSHKeyPairResponse> response = new ListResponse<SSHKeyPairResponse>();
        response.setResponses(responses, resultList.second());
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }

    @Override
    public String getCommandName() {
        return s_name;
    }

}
