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

package org.apache.cloudstack.api.command.admin.ca;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.CertificateResponse;
import org.apache.cloudstack.ca.CAManager;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.framework.ca.Certificate;
import org.apache.cloudstack.utils.security.CertUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.cloud.event.EventTypes;

@APICommand(name = "issueCertificate",
        description = "Issues a client certificate using configured or provided CA plugin",
        responseObject = CertificateResponse.class,
        requestHasSensitiveInfo = true,
        responseHasSensitiveInfo = true,
        since = "4.11.0",
        authorized = {RoleType.Admin})
public class IssueCertificateCmd extends BaseAsyncCmd {
    private static final Logger LOG = Logger.getLogger(IssueCertificateCmd.class);


    @Inject
    private CAManager caManager;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.CSR, type = BaseCmd.CommandType.STRING, description = "The certificate signing request (in pem format), if CSR is not provided then configured/provided options are considered", length = 65535)
    private String csr;

    @Parameter(name = ApiConstants.DOMAIN, type = BaseCmd.CommandType.STRING, description = "Comma separated list of domains, the certificate should be issued for. When csr is not provided, the first domain is used as a subject/CN")
    private String domains;

    @Parameter(name = ApiConstants.IP_ADDRESS, type = BaseCmd.CommandType.STRING, description = "Comma separated list of IP addresses, the certificate should be issued for")
    private String addresses;

    @Parameter(name = ApiConstants.DURATION, type = CommandType.INTEGER, description = "Certificate validity duration in number of days, when not provided the default configured value will be used")
    private Integer validityDuration;

    @Parameter(name = ApiConstants.PROVIDER, type = BaseCmd.CommandType.STRING, description = "Name of the CA service provider, otherwise the default configured provider plugin will be used")
    private String provider;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getCsr() {
        return csr;
    }

    private List<String> processList(final String string) {
        final List<String> list = new ArrayList<>();
        if (StringUtils.isNotEmpty(string)) {
            for (final String address: string.split(",")) {
                list.add(address.trim());
            }
        }
        return list;
    }

    public List<String> getAddresses() {
        return processList(addresses);
    }

    public List<String> getDomains() {
        return processList(domains);
    }

    public Integer getValidityDuration() {
        return validityDuration;
    }

    public String getProvider() {
        return provider;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() {
        if (StringUtils.isEmpty(getCsr()) && getDomains().isEmpty()) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, "Please provide the domains or the CSR, none of them are provided");
        }
        final Certificate certificate = caManager.issueCertificate(getCsr(), getDomains(), getAddresses(), getValidityDuration(), getProvider());
        if (certificate == null) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to issue client certificate with given provider");
        }

        final CertificateResponse certificateResponse = new CertificateResponse();
        try {
            certificateResponse.setCertificate(CertUtils.x509CertificateToPem(certificate.getClientCertificate()));
            if (certificate.getPrivateKey() != null) {
                certificateResponse.setPrivateKey(CertUtils.privateKeyToPem(certificate.getPrivateKey()));
            }
            if (certificate.getCaCertificates() != null) {
                certificateResponse.setCaCertificate(CertUtils.x509CertificatesToPem(certificate.getCaCertificates()));
            }
        } catch (final IOException e) {
            LOG.error("Failed to generate and convert client certificate(s) to PEM due to error: ", e);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to process and return client certificate");
        }
        certificateResponse.setResponseName(getCommandName());
        setResponseObject(certificateResponse);
    }

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccount().getId();
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_CA_CERTIFICATE_ISSUE;
    }

    @Override
    public String getEventDescription() {
        return "issuing certificate for domain(s)=" + domains + ", ip(s)=" + addresses;
    }
}
