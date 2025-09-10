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
package org.apache.cloudstack.network.tungsten.api.command;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.lb.LoadBalancingRule;
import com.cloud.network.lb.LoadBalancingRulesManager;
import com.cloud.user.Account;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.network.tungsten.api.response.TlsDataResponse;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;

@APICommand(name = GetLoadBalancerSslCertificateCmd.APINAME, description = "get load balancer certificate",
    responseObject = TlsDataResponse.class, requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class GetLoadBalancerSslCertificateCmd extends BaseCmd {
    public static final String APINAME = "getLoadBalancerSslCertificate";

    @Inject
    private LoadBalancingRulesManager lbMgr;

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = TlsDataResponse.class, required = true, description = "the ID of Lb")
    private Long id;

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException,
        ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        LoadBalancingRule.LbSslCert lbSslCert = lbMgr.getLbSslCert(id);
        if (lbSslCert != null) {
            TlsDataResponse tlsDataResponse = new TlsDataResponse();
            tlsDataResponse.setCrt(Base64.encodeBase64String(lbSslCert.getCert().getBytes()));
            tlsDataResponse.setKey(Base64.encodeBase64String(lbSslCert.getKey().getBytes()));
            tlsDataResponse.setChain(
                lbSslCert.getChain() != null ? Base64.encodeBase64String(lbSslCert.getChain().getBytes()) :
                    StringUtils.EMPTY);
            tlsDataResponse.setResponseName(getCommandName());
            tlsDataResponse.setObjectName("data");
            setResponseObject(tlsDataResponse);
        } else {
            throw new CloudRuntimeException("can not get tls data");
        }
    }

    @Override
    public String getCommandName() {
        return APINAME.toLowerCase() + BaseCmd.RESPONSE_SUFFIX;
    }

    @Override
    public long getEntityOwnerId() {
        Account account = CallContext.current().getCallingAccount();
        if (account != null) {
            return account.getId();
        }
        return Account.ACCOUNT_ID_SYSTEM;
    }
}
