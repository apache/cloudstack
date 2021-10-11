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

package org.apache.cloudstack.ca.provider;

import java.math.BigInteger;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;

import javax.net.ssl.X509TrustManager;

import org.apache.log4j.Logger;

import com.cloud.certificate.dao.CrlDao;
import org.apache.commons.lang3.StringUtils;

public final class RootCACustomTrustManager implements X509TrustManager {
    private static final Logger LOG = Logger.getLogger(RootCACustomTrustManager.class);

    private String clientAddress = "Unknown";
    private boolean authStrictness = true;
    private boolean allowExpiredCertificate = true;
    private CrlDao crlDao;
    private X509Certificate caCertificate;
    private Map<String, X509Certificate> activeCertMap;

    public RootCACustomTrustManager(final String clientAddress, final boolean authStrictness, final boolean allowExpiredCertificate, final Map<String, X509Certificate> activeCertMap, final X509Certificate caCertificate, final CrlDao crlDao) {
        if (StringUtils.isNotEmpty(clientAddress)) {
            this.clientAddress = clientAddress.replace("/", "").split(":")[0];
        }
        this.authStrictness = authStrictness;
        this.allowExpiredCertificate = allowExpiredCertificate;
        this.activeCertMap = activeCertMap;
        this.caCertificate = caCertificate;
        this.crlDao = crlDao;
    }

    private void printCertificateChain(final X509Certificate[] certificates, final String s) throws CertificateException {
        if (certificates == null) {
            return;
        }
        final StringBuilder builder = new StringBuilder();
        builder.append("A client/agent attempting connection from address=").append(clientAddress).append(" has presented these certificate(s):");
        int counter = 1;
        for (final X509Certificate certificate: certificates) {
            builder.append("\nCertificate [").append(counter++).append("] :");
            builder.append(String.format("\n Serial: %x", certificate.getSerialNumber()));
            builder.append("\n  Not Before:" + certificate.getNotBefore());
            builder.append("\n  Not After:" + certificate.getNotAfter());
            builder.append("\n  Signature Algorithm:" + certificate.getSigAlgName());
            builder.append("\n  Version:" + certificate.getVersion());
            builder.append("\n  Subject DN:" + certificate.getSubjectDN());
            builder.append("\n  Issuer DN:" + certificate.getIssuerDN());
            builder.append("\n  Alternative Names:" + certificate.getSubjectAlternativeNames());
        }
        LOG.debug(builder.toString());
    }

    @Override
    public void checkClientTrusted(final X509Certificate[] certificates, final String s) throws CertificateException {
        if (LOG.isDebugEnabled()) {
            printCertificateChain(certificates, s);
        }

        final X509Certificate primaryClientCertificate = (certificates != null && certificates.length > 0 && certificates[0] != null) ? certificates[0] : null;
        String exceptionMsg = "";

        if (authStrictness && primaryClientCertificate == null) {
            throw new CertificateException("In strict auth mode, certificate(s) are expected from client:" + clientAddress);
        } else if (primaryClientCertificate == null) {
            LOG.info("No certificate was received from client, but continuing since strict auth mode is disabled");
            return;
        }

        // Revocation check
        final BigInteger serialNumber = primaryClientCertificate.getSerialNumber();
        if (serialNumber == null || crlDao.findBySerial(serialNumber) != null) {
            final String errorMsg = String.format("Client is using revoked certificate of serial=%x, subject=%s from address=%s",
                    primaryClientCertificate.getSerialNumber(), primaryClientCertificate.getSubjectDN(), clientAddress);
            LOG.error(errorMsg);
            exceptionMsg = (StringUtils.isEmpty(exceptionMsg)) ? errorMsg : (exceptionMsg + ". " + errorMsg);
        }

        // Validity check
        try {
            primaryClientCertificate.checkValidity();
        } catch (final CertificateExpiredException | CertificateNotYetValidException e) {
            final String errorMsg = String.format("Client certificate has expired with serial=%x, subject=%s from address=%s",
                    primaryClientCertificate.getSerialNumber(), primaryClientCertificate.getSubjectDN(), clientAddress);
            LOG.error(errorMsg);
            if (!allowExpiredCertificate) {
                throw new CertificateException(errorMsg);
            }
        }

        // Ownership check
        boolean certMatchesOwnership = false;
        if (primaryClientCertificate.getSubjectAlternativeNames() != null) {
            for (final List<?> list : primaryClientCertificate.getSubjectAlternativeNames()) {
                if (list != null && list.size() == 2 && list.get(1) instanceof String) {
                    final String alternativeName = (String) list.get(1);
                    if (clientAddress.equals(alternativeName)) {
                        certMatchesOwnership = true;
                    }
                }
            }
        }
        if (!certMatchesOwnership) {
            final String errorMsg = "Certificate ownership verification failed for client: " + clientAddress;
            LOG.error(errorMsg);
            exceptionMsg = (StringUtils.isEmpty(exceptionMsg)) ? errorMsg : (exceptionMsg + ". " + errorMsg);
        }
        if (authStrictness && StringUtils.isNotEmpty(exceptionMsg)) {
            throw new CertificateException(exceptionMsg);
        }
        if (LOG.isDebugEnabled()) {
            if (authStrictness) {
                LOG.debug("Client/agent connection from ip=" + clientAddress + " has been validated and trusted.");
            } else {
                LOG.debug("Client/agent connection from ip=" + clientAddress + " accepted without certificate validation.");
            }
        }

        if (primaryClientCertificate != null && activeCertMap != null && StringUtils.isNotEmpty(clientAddress)) {
            activeCertMap.put(clientAddress, primaryClientCertificate);
        }
    }

    @Override
    public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[]{caCertificate};
    }
}
