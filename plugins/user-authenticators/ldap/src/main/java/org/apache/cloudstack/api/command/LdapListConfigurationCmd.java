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
package org.apache.cloudstack.api.command;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.response.DomainResponse;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.LdapConfigurationResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.ldap.LdapConfigurationVO;
import org.apache.cloudstack.ldap.LdapManager;

import com.cloud.user.Account;
import com.cloud.utils.Pair;

@APICommand(name = "listLdapConfigurations", responseObject = LdapConfigurationResponse.class, description = "Lists all LDAP configurations", since = "4.2.0",
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class LdapListConfigurationCmd extends BaseListCmd {

    private static final String s_name = "ldapconfigurationresponse";

    @Inject
    private LdapManager _ldapManager;

    @Parameter(name = ApiConstants. HOST_NAME, type = CommandType.STRING, required = false, description = "Hostname")
    private String hostname;

    @Parameter(name = ApiConstants.PORT, type = CommandType.INTEGER, required = false, description = "Port")
    private int port;

    @Parameter(name = ApiConstants.DOMAIN_ID, type = CommandType.UUID, required = false, entityType = DomainResponse.class, description = "linked domain")
    private Long domainId;

    @Parameter(name = ApiConstants.LIST_ALL, type = CommandType.BOOLEAN, description = "If set to true, "
    + " and no domainid specified, list all LDAP configurations irrespective of the linked domain", since = "4.13.2")
    private Boolean listAll;

    public LdapListConfigurationCmd() {
        super();
    }

    public LdapListConfigurationCmd(final LdapManager ldapManager) {
        super();
        _ldapManager = ldapManager;
    }

    private List<LdapConfigurationResponse> createLdapConfigurationResponses(final List<? extends LdapConfigurationVO> configurations) {
        final List<LdapConfigurationResponse> responses = new ArrayList<LdapConfigurationResponse>();
        for (final LdapConfigurationVO resource : configurations) {
            final LdapConfigurationResponse configurationResponse = _ldapManager.createLdapConfigurationResponse(resource);
            configurationResponse.setObjectName("LdapConfiguration");
            responses.add(configurationResponse);
        }
        return responses;
    }

    @Override
    public void execute() {
        final Pair<List<? extends LdapConfigurationVO>, Integer> result = _ldapManager.listConfigurations(this);
        final List<LdapConfigurationResponse> responses = createLdapConfigurationResponses(result.first());
        final ListResponse<LdapConfigurationResponse> response = new ListResponse<LdapConfigurationResponse>();
        response.setResponses(responses, result.second());
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    public String getHostname() {
        return hostname;
    }

    public int getPort() {
        return port;
    }

    public Long getDomainId() {
        return domainId;
    }

    public void setHostname(final String hostname) {
        this.hostname = hostname;
    }

    public void setPort(final int port) {
        this.port = port;
    }

    public void setDomainId(final Long domainId) {
        this.domainId = domainId;
    }

    public boolean listAll() {
        return listAll != null && listAll;
    }
}
