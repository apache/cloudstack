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

package org.apache.cloudstack.api.command.user.dns;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.acl.SecurityChecker;
import org.apache.cloudstack.api.ACL;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.DnsZoneNetworkMapResponse;
import org.apache.cloudstack.api.response.DnsZoneResponse;
import org.apache.cloudstack.api.response.NetworkResponse;
import org.apache.cloudstack.dns.DnsZone;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.user.Account;

@APICommand(name = "associateDnsZoneToNetwork",
        description = "Associates a DNS Zone with a Network for VM auto-registration",
        responseObject = DnsZoneNetworkMapResponse.class,
        requestHasSensitiveInfo = false,
        responseHasSensitiveInfo = false,
        since = "4.23.0",
        authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User})
public class AssociateDnsZoneToNetworkCmd extends BaseCmd {

    @Parameter(name = ApiConstants.DNS_ZONE_ID, type = CommandType.UUID, entityType = DnsZoneResponse.class,
            required = true, description = "The ID of the DNS zone")
    private Long dnsZoneId;

    @ACL(accessType = SecurityChecker.AccessType.OperateEntry)
    @Parameter(name = ApiConstants.NETWORK_ID, type = CommandType.UUID, entityType = NetworkResponse.class,
            required = true, description = "The ID of the network")
    private Long networkId;

    @Parameter(name = "subdomain", type = CommandType.STRING,
            description = "Optional subdomain to append (e.g., 'dev' creates vm1.dev.example.com)")
    private String subDomain;

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        try {
            DnsZoneNetworkMapResponse response = dnsProviderManager.associateZoneToNetwork(this);
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } catch (Exception e) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, e.getMessage());
        }
    }

    @Override
    public long getEntityOwnerId() {
        DnsZone zone = _entityMgr.findById(DnsZone.class, dnsZoneId);
        if (zone != null) {
            return zone.getAccountId();
        }
        return Account.ACCOUNT_ID_SYSTEM;
    }

    public Long getDnsZoneId() {
        return dnsZoneId;
    }

    public Long getNetworkId() {
        return networkId;
    }

    public String getSubDomain() {
        return subDomain;
    }
}
