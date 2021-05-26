//Licensed to the Apache Software Foundation (ASF) under one
//or more contributor license agreements.  See the NOTICE file
//distributed with this work for additional information
//regarding copyright ownership.  The ASF licenses this file
//to you under the Apache License, Version 2.0 (the
//"License"); you may not use this file except in compliance
//with the License.  You may obtain a copy of the License at
//
//http://www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing,
//software distributed under the License is distributed on an
//"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
//KIND, either express or implied.  See the License for the
//specific language governing permissions and limitations
//under the License.
package org.apache.cloudstack.api.command.user.vpn;

import javax.inject.Inject;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.CertificateResponse;
import org.apache.cloudstack.pki.PkiDetail;
import org.apache.cloudstack.pki.PkiManager;

import com.cloud.domain.Domain;
import com.cloud.exception.RemoteAccessVpnException;
import com.cloud.user.Account;
import com.cloud.user.DomainService;
import com.cloud.utils.exception.CloudRuntimeException;

/**
* @author Khosrow Moossavi
* @since 4.12.0.0
*/
@APICommand(
        name = ListRemoteAccessVpnCaCertificatesCmd.APINAME,
        description = "Lists the CA public certificate(s) as support by the configured/provided CA plugin",
        responseObject = CertificateResponse.class,
        requestHasSensitiveInfo = false,
        responseHasSensitiveInfo = false,
        since = "4.12.0.0",
        authorized = {
                RoleType.Admin,
                RoleType.ResourceAdmin,
                RoleType.DomainAdmin,
                RoleType.User
        }
)
public class ListRemoteAccessVpnCaCertificatesCmd extends BaseCmd {
    public static final String APINAME = "listVpnCaCertificate";

    @Inject private DomainService domainService;
    @Inject private PkiManager pkiManager;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.DOMAIN, type = CommandType.STRING, description = "Name of the CA service provider, otherwise the default configured provider plugin will be used")
    private String domain;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getDomain() {
        return domain;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() {
        final PkiDetail certificate;

        try {
            Domain domain = domainService.getDomain(getDomain());
            certificate = pkiManager.getCertificate(domain);
        } catch (final RemoteAccessVpnException e) {
            throw new CloudRuntimeException("Failed to get CA certificates for given domain");
        }

        final CertificateResponse certificateResponse = new CertificateResponse("cacertificates");
        certificateResponse.setCertificate(certificate.getIssuingCa());
        certificateResponse.setResponseName(getCommandName());
        setResponseObject(certificateResponse);
    }

    @Override
    public String getCommandName() {
        return APINAME.toLowerCase() + BaseCmd.RESPONSE_SUFFIX;
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_TYPE_NORMAL;
    }
}
