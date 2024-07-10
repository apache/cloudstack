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

package org.apache.cloudstack.ca;

import java.io.IOException;
import java.math.BigInteger;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.framework.ca.CAProvider;
import org.apache.cloudstack.framework.ca.CAService;
import org.apache.cloudstack.framework.ca.Certificate;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;

import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.host.Host;
import com.cloud.utils.component.PluggableService;

public interface CAManager extends CAService, Configurable, PluggableService {

    ConfigKey<String> CAProviderPlugin = new ConfigKey<>("Advanced", String.class,
            "ca.framework.provider.plugin",
            "root",
            "The CA provider plugin that is used for secure CloudStack management server-agent communication for encryption and authentication. Restart management server(s) when changed.", true);

    ConfigKey<Integer> CertKeySize = new ConfigKey<>("Advanced", Integer.class,
                                    "ca.framework.cert.keysize",
                                    "2048",
                                    "The key size to be used for random certificate keypair generation.", true);

    ConfigKey<String> CertSignatureAlgorithm = new ConfigKey<>(String.class,
    "ca.framework.cert.signature.algorithm", "Advanced",
            "SHA256withRSA",
            "The default signature algorithm to use for certificate generation.", true, ConfigKey.Scope.Global, null, null, null, null, null, ConfigKey.Kind.Select, "SHA256withRSA");


    ConfigKey<Integer> CertValidityPeriod = new ConfigKey<>("Advanced", Integer.class,
                                            "ca.framework.cert.validity.period",
                                            "365",
                                            "The validity period of a client certificate in number of days. Set the value to be more than the expiry alert period.", true);

    ConfigKey<Boolean> AutomaticCertRenewal = new ConfigKey<>("Advanced", Boolean.class,
            "ca.framework.cert.automatic.renewal",
            "true",
            "Enable automatic renewal and provisioning of certificate to agents as supported by the configured CA plugin.", true, ConfigKey.Scope.Cluster);

    ConfigKey<Boolean> AllowHostIPInSysVMAgentCert = new ConfigKey<>("Advanced", Boolean.class,
            "ca.framework.cert.systemvm.allow.host.ip",
            "false",
            "Allow hypervisor host's IP to be a part of a system VM's agent cert", true, ConfigKey.Scope.Zone);

    ConfigKey<Long> CABackgroundJobDelay = new ConfigKey<>("Advanced", Long.class,
            "ca.framework.background.task.delay",
            "3600",
            "The CA framework background task delay in seconds. Background task runs expiry checks and renews certificate if auto-renewal is enabled.", true);

    ConfigKey<Integer> CertExpiryAlertPeriod = new ConfigKey<>("Advanced", Integer.class,
                                                    "ca.framework.cert.expiry.alert.period",
                                                    "15",
                                                    "The number of days before expiry of a client certificate, the validations are checked. Admins are alerted when auto-renewal is not allowed, otherwise auto-renewal is attempted.", true, ConfigKey.Scope.Cluster);


    ConfigKey<String> CertManagementCustomSubjectAlternativeName = new ConfigKey<>("Advanced", String.class,
            "ca.framework.cert.management.custom.san",
            "cloudstack.internal",
            "The custom Subject Alternative Name that will be added to the management server certificate. " +
                    "The actual implementation will depend on the configured CA provider.",
            false);

    /**
     * Returns a list of available CA provider plugins
     * @return returns list of CAProvider
     */
    List<CAProvider> getCaProviders();

    /**
     * Returns a map of active agents/hosts certificates
     * @return returns a non-null map
     */
    Map<String, X509Certificate> getActiveCertificatesMap();

    /**
     * Checks whether the configured CA plugin can provision/create certificates
     * @return returns certificate creation capability
     */
    boolean canProvisionCertificates();

    /**
     * Returns PEM-encoded chained CA certificate
     * @param caProvider
     * @return returns CA certificate chain string
     */
    String getCaCertificate(final String caProvider) throws IOException;

    /**
     * Issues client Certificate
     * @param csr
     * @param ipAddresses
     * @param domainNames
     * @param validityDays
     * @param provider
     * @return returns Certificate
     */
    Certificate issueCertificate(final String csr, final List<String> domainNames, final List<String> ipAddresses, final Integer validityDays, final String provider);

    /**
     * Revokes certificate from provided serial and CN
     * @param certSerial
     * @param certCn
     * @return returns success/failure as boolean
     */
    boolean revokeCertificate(final BigInteger certSerial, final String certCn, final String provider);

    /**
     * Provisions certificate for given active and connected agent host
     * @param host
     * @param provider
     * @return returns success/failure as boolean
     */
    boolean provisionCertificate(final Host host, final Boolean reconnect, final String provider);

    /**
     * Setups up a new keystore and generates CSR for a host
     * @param host
     * @param sshAccessDetails when provided, VirtualRoutingResource uses router proxy to execute commands via SSH in systemvms
     * @return
     * @throws AgentUnavailableException
     * @throws OperationTimedoutException
     */
    String generateKeyStoreAndCsr(final Host host, final Map<String, String> sshAccessDetails) throws AgentUnavailableException, OperationTimedoutException;

    /**
     * Deploys a Certificate payload to a provided host
     * @param host
     * @param certificate
     * @param reconnect when true the host/agent is reconnected on successful deployment of the certificate
     * @param sshAccessDetails when provided, VirtualRoutingResource uses router proxy to execute commands via SSH in systemvms
     * @return
     * @throws AgentUnavailableException
     * @throws OperationTimedoutException
     */
    boolean deployCertificate(final Host host, final Certificate certificate, final Boolean reconnect, final Map<String, String> sshAccessDetails) throws AgentUnavailableException, OperationTimedoutException;

    /**
     * Removes the host from an internal active client/certificate map
     * @param host
     */
    void purgeHostCertificate(final Host host);

    /**
     * Sends a CA cert event alert to admins with a subject and a message
     * @param host
     * @param subject
     * @param message
     */
    void sendAlert(final Host host, final String subject, final String message);

}
