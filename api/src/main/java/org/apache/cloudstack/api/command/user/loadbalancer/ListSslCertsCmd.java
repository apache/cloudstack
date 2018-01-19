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
package org.apache.cloudstack.api.command.user.loadbalancer;

import java.util.List;

import javax.inject.Inject;

import org.apache.log4j.Logger;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.AccountResponse;
import org.apache.cloudstack.api.response.FirewallRuleResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.ProjectResponse;
import org.apache.cloudstack.api.response.SslCertResponse;
import org.apache.cloudstack.context.CallContext;

import org.apache.cloudstack.network.tls.CertService;
import com.cloud.utils.exception.CloudRuntimeException;

@APICommand(name = "listSslCerts", description = "Lists SSL certificates", responseObject = SslCertResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class ListSslCertsCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(DeleteSslCertCmd.class.getName());

    private static final String s_name = "listsslcertsresponse";

    @Inject
    CertService _certService;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.CERTIFICATE_ID, type = CommandType.UUID, entityType = SslCertResponse.class, required = false, description = "ID of SSL certificate")
    private Long certId;

    @Parameter(name = ApiConstants.ACCOUNT_ID, type = CommandType.UUID, entityType = AccountResponse.class, required = false, description = "Account ID")
    private Long accountId;

    @Parameter(name = ApiConstants.LBID, type = CommandType.UUID, entityType = FirewallRuleResponse.class, required = false, description = "Load balancer rule ID")
    private Long lbId;

    @Parameter(name = ApiConstants.PROJECT_ID, type = CommandType.UUID, entityType = ProjectResponse.class, required = false, description = "Project that owns the SSL certificate")
    private Long projectId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getCertId() {
        return certId;
    }

    public Long getAccountId() {
        return accountId;
    }

    public Long getLbId() {
        return lbId;
    }

    public Long getProjectId() {
        return projectId;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() {

        try {
            List<SslCertResponse> certResponseList = _certService.listSslCerts(this);
            ListResponse<SslCertResponse> response = new ListResponse<SslCertResponse>();

            response.setResponses(certResponseList);
            response.setResponseName(getCommandName());
            this.setResponseObject(response);

        } catch (Exception e) {
            throw new CloudRuntimeException(e);
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
