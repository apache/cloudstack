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
package org.apache.cloudstack.api.command.user.vpn;

import org.apache.cloudstack.api.ApiArgValidator;
import org.apache.log4j.Logger;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.BaseCmd.CommandType;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.ProjectResponse;
import org.apache.cloudstack.api.response.Site2SiteCustomerGatewayResponse;
import org.apache.cloudstack.context.CallContext;

import com.cloud.event.EventTypes;
import com.cloud.network.Site2SiteCustomerGateway;

@APICommand(name = "createVpnCustomerGateway", description = "Creates site to site vpn customer gateway", responseObject = Site2SiteCustomerGatewayResponse.class, entityType = {Site2SiteCustomerGateway.class},
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class CreateVpnCustomerGatewayCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(CreateVpnCustomerGatewayCmd.class.getName());

    private static final String s_name = "createvpncustomergatewayresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, required = false, description = "name of this customer gateway")
    private String name;

    @Parameter(name = ApiConstants.GATEWAY, type = CommandType.STRING, required = true, description = "public ip address id of the customer gateway")
    private String gatewayIp;

    @Parameter(name = ApiConstants.CIDR_LIST, type = CommandType.STRING, required = true, description = "guest cidr list of the customer gateway. Multiple entries must be separated by a single comma character (,).")
    private String peerCidrList;

    @Parameter(name = ApiConstants.IPSEC_PSK, type = CommandType.STRING, required = true, description = "IPsec Preshared-Key of the customer gateway. Cannot contain newline or double quotes.")
    private String ipsecPsk;

    @Parameter(name = ApiConstants.IKE_POLICY, type = CommandType.STRING, required = true, description = "IKE policy of the customer gateway")
    private String ikePolicy;

    @Parameter(name = ApiConstants.ESP_POLICY, type = CommandType.STRING, required = true, description = "ESP policy of the customer gateway")
    private String espPolicy;

    @Parameter(name = ApiConstants.IKE_LIFETIME,
               type = CommandType.LONG,
               required = false,
               description = "Lifetime of phase 1 VPN connection to the customer gateway, in seconds")
    private Long ikeLifetime;

    @Parameter(name = ApiConstants.ESP_LIFETIME,
               type = CommandType.LONG,
               required = false,
               description = "Lifetime of phase 2 VPN connection to the customer gateway, in seconds")
    private Long espLifetime;

    @Parameter(name = ApiConstants.DPD, type = CommandType.BOOLEAN, required = false, description = "If DPD is enabled for VPN connection")
    private Boolean dpd;

    @Parameter(name = ApiConstants.FORCE_ENCAP, type = CommandType.BOOLEAN, required = false, description = "Force Encapsulation for NAT traversal")
    private Boolean encap;

    @Parameter(name = ApiConstants.ACCOUNT, type = CommandType.STRING, description = "the account associated with the gateway. Must be used with the domainId parameter.")
    private String accountName;

    @Parameter(name = ApiConstants.DOMAIN_ID,
               type = CommandType.UUID,
               entityType = DomainResponse.class,
               description = "the domain ID associated with the gateway. If used with the account parameter returns the "
                   + "gateway associated with the account for the specified domain.")
    private Long domainId;

    @Parameter(name = ApiConstants.PROJECT_ID, type = CommandType.UUID, entityType = ProjectResponse.class,
            description = "create site-to-site VPN customer gateway for the project", since = "4.6")
    private Long projectId;

    @Parameter(name = ApiConstants.SPLIT_CONNECTIONS, type = CommandType.BOOLEAN, required = false, description = "For IKEv2, whether to split multiple right subnet cidrs into multiple connection statements.",
            since = "4.15.1")
    private Boolean splitConnections;

    @Parameter(name = ApiConstants.IKE_VERSION, type = CommandType.STRING, required = false, description = "Which IKE Version to use, one of ike (autoselect), ikev1, or ikev2. " +
            "Connections marked with 'ike' will use 'ikev2' when initiating, but accept any protocol version when responding. Defaults to ike", validations = {ApiArgValidator.NotNullOrEmpty}, since = "4.15.1")
    private String ikeVersion;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getName() {
        return name;
    }

    public String getIpsecPsk() {
        return ipsecPsk;
    }

    public String getGuestCidrList() {
        return peerCidrList;
    }

    public String getGatewayIp() {
        return gatewayIp;
    }

    public String getIkePolicy() {
        return ikePolicy;
    }

    public String getEspPolicy() {
        return espPolicy;
    }

    public Long getIkeLifetime() {
        return ikeLifetime;
    }

    public Long getEspLifetime() {
        return espLifetime;
    }

    public Boolean getDpd() {
        return dpd;
    }

    public Boolean getEncap() { return encap; }

    public String getAccountName() {
        return accountName;
    }

    public Long getDomainId() {
        return domainId;
    }

    public Long getProjectId() {
        return projectId;
    }

    public Boolean getSplitConnections() {
        return splitConnections;
    }

    public String getIkeVersion() {
        return ikeVersion;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public long getEntityOwnerId() {
        Long accountId = _accountService.finalyzeAccountId(accountName, domainId, projectId, true);
        if (accountId == null) {
            accountId = CallContext.current().getCallingAccount().getId();
        }
        return accountId;
    }

    @Override
    public String getEventDescription() {
        return "Create site-to-site VPN customer gateway for account " + getEntityOwnerId();
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_S2S_VPN_CUSTOMER_GATEWAY_CREATE;
    }

    @Override
    public void execute() {
        Site2SiteCustomerGateway result = _s2sVpnService.createCustomerGateway(this);
        if (result != null) {
            Site2SiteCustomerGatewayResponse response = _responseGenerator.createSite2SiteCustomerGatewayResponse(result);
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to create customer VPN gateway");
        }
    }
}
