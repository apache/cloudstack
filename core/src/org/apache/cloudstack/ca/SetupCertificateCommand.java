//
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
//

package org.apache.cloudstack.ca;

import java.io.IOException;
import java.util.Map;

import org.apache.cloudstack.framework.ca.Certificate;
import org.apache.cloudstack.utils.security.CertUtils;
import org.apache.cloudstack.utils.security.KeyStoreUtils;

import com.cloud.agent.api.LogLevel;
import com.cloud.agent.api.routing.NetworkElementCommand;
import com.cloud.utils.exception.CloudRuntimeException;

public class SetupCertificateCommand extends NetworkElementCommand {
    @LogLevel(LogLevel.Log4jLevel.Off)
    private String certificate;
    @LogLevel(LogLevel.Log4jLevel.Off)
    private String privateKey = "";
    @LogLevel(LogLevel.Log4jLevel.Off)
    private String caCertificates;

    private boolean handleByAgent = true;

    public SetupCertificateCommand(final Certificate certificate) {
        super();
        if (certificate == null) {
            throw new CloudRuntimeException("A null certificate was provided to setup");
        }
        setWait(60);
        try {
            this.certificate = CertUtils.x509CertificateToPem(certificate.getClientCertificate());
            this.caCertificates = CertUtils.x509CertificatesToPem(certificate.getCaCertificates());
            if (certificate.getPrivateKey() != null) {
                this.privateKey = CertUtils.privateKeyToPem(certificate.getPrivateKey());
            }
        } catch (final IOException e) {
            throw new CloudRuntimeException("Failed to transform X509 cert to PEM format", e);
        }
    }

    @Override
    public void setAccessDetail(final Map<String, String> accessDetails) {
        handleByAgent = false;
        super.setAccessDetail(accessDetails);
    }

    @Override
    public void setAccessDetail(String name, String value) {
        handleByAgent = false;
        super.setAccessDetail(name, value);
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public String getCertificate() {
        return certificate;
    }

    public String getCaCertificates() {
        return caCertificates;
    }

    public String getEncodedPrivateKey() {
        return privateKey.replace("\n", KeyStoreUtils.CERT_NEWLINE_ENCODER).replace(" ", KeyStoreUtils.CERT_SPACE_ENCODER);
    }

    public String getEncodedCertificate() {
        return certificate.replace("\n", KeyStoreUtils.CERT_NEWLINE_ENCODER).replace(" ", KeyStoreUtils.CERT_SPACE_ENCODER);
    }

    public String getEncodedCaCertificates() {
        return caCertificates.replace("\n", KeyStoreUtils.CERT_NEWLINE_ENCODER).replace(" ", KeyStoreUtils.CERT_SPACE_ENCODER);
    }

    public boolean isHandleByAgent() {
        return handleByAgent;
    }
}
