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
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.SecureRandom;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;
import javax.naming.ConfigurationException;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import com.trilead.ssh2.Connection;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import com.cloud.host.HostVO;
import com.cloud.utils.PasswordGenerator;
import com.cloud.utils.ssh.SSHCmdHelper;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.dao.VMInstanceDao;
import org.apache.cloudstack.utils.security.KeyStoreUtils;
import org.apache.commons.lang3.math.NumberUtils;

import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.admin.ca.IssueCertificateCmd;
import org.apache.cloudstack.api.command.admin.ca.ListCAProvidersCmd;
import org.apache.cloudstack.api.command.admin.ca.ListCaCertificateCmd;
import org.apache.cloudstack.api.command.admin.ca.ProvisionCertificateCmd;
import org.apache.cloudstack.api.command.admin.ca.RevokeCertificateCmd;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.framework.ca.CAProvider;
import org.apache.cloudstack.framework.ca.Certificate;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.managed.context.ManagedContextRunnable;
import org.apache.cloudstack.poll.BackgroundPollManager;
import org.apache.cloudstack.poll.BackgroundPollTask;
import org.apache.cloudstack.utils.identity.ManagementServerNode;
import org.apache.cloudstack.utils.security.CertUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.routing.NetworkElementCommand;
import com.cloud.alert.AlertManager;
import com.cloud.certificate.CrlVO;
import com.cloud.certificate.dao.CrlDao;
import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.host.Host;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.commons.lang3.StringUtils;

public class CAManagerImpl extends ManagerBase implements CAManager {

    @Inject
    private CrlDao crlDao;
    @Inject
    private HostDao hostDao;
    @Inject
    private VMInstanceDao vmInstanceDao;
    @Inject
    private NetworkOrchestrationService networkOrchestrationService;
    @Inject
    private ConfigurationDao configDao;
    @Inject
    private AgentManager agentManager;
    @Inject
    private BackgroundPollManager backgroundPollManager;
    @Inject
    private AlertManager alertManager;

    private static CAProvider configuredCaProvider;
    private static Map<String, CAProvider> caProviderMap = new HashMap<>();
    private static Map<String, Date> alertMap = new ConcurrentHashMap<>();
    private static Map<String, X509Certificate> activeCertMap = new ConcurrentHashMap<>();

    private List<CAProvider> caProviders;

    private CAProvider getConfiguredCaProvider() {
        if (configuredCaProvider != null) {
            return configuredCaProvider;
        }
        if (caProviderMap.containsKey(CAProviderPlugin.value()) && caProviderMap.get(CAProviderPlugin.value()) != null) {
            configuredCaProvider = caProviderMap.get(CAProviderPlugin.value());
            return configuredCaProvider;
        }
        throw new CloudRuntimeException("Failed to find default configured CA provider plugin");
    }

    private CAProvider getCAProvider(final String provider) {
        if (StringUtils.isEmpty(provider)) {
            return getConfiguredCaProvider();
        }
        final String caProviderName = provider.toLowerCase();
        if (!caProviderMap.containsKey(caProviderName)) {
            throw new CloudRuntimeException(String.format("CA provider plugin '%s' not found", caProviderName));
        }
        final CAProvider caProvider = caProviderMap.get(caProviderName);
        if (caProvider == null) {
            throw new CloudRuntimeException(String.format("CA provider plugin '%s' returned is null", caProviderName));
        }
        return caProvider;
    }

    ///////////////////////////////////////////////////////////
    /////////////// CA Manager API Handlers ///////////////////
    ///////////////////////////////////////////////////////////

    @Override
    public List<CAProvider> getCaProviders() {
        return caProviders;
    }

    @Override
    public Map<String, X509Certificate> getActiveCertificatesMap() {
        return activeCertMap;
    }

    @Override
    public boolean canProvisionCertificates() {
        return getConfiguredCaProvider().canProvisionCertificates();
    }

    @Override
    public String getCaCertificate(final String caProvider) throws IOException {
        final CAProvider provider = getCAProvider(caProvider);
        return CertUtils.x509CertificatesToPem(provider.getCaCertificate());
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_CA_CERTIFICATE_ISSUE, eventDescription = "issuing certificate", async = true)
    public Certificate issueCertificate(final String csr, final List<String> domainNames, final List<String> ipAddresses, final Integer validityDuration, final String caProvider) {
        CallContext.current().setEventDetails("domain(s): " + domainNames + " addresses: " + ipAddresses);
        final CAProvider provider = getCAProvider(caProvider);
        Integer validity = CAManager.CertValidityPeriod.value();
        if (validityDuration != null) {
            validity = validityDuration;
        }
        if (StringUtils.isEmpty(csr)) {
            if (domainNames == null || domainNames.isEmpty()) {
                throw new ServerApiException(ApiErrorCode.PARAM_ERROR, "No domains or CSR provided");
            }
            return provider.issueCertificate(domainNames, ipAddresses, validity);
        }
        return provider.issueCertificate(csr, domainNames, ipAddresses, validity);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_CA_CERTIFICATE_REVOKE, eventDescription = "revoking certificate", async = true)
    public boolean revokeCertificate(final BigInteger certSerial, final String certCn, final String caProvider) {
        CallContext.current().setEventDetails("cert serial: " + certSerial);
        final CrlVO crl = crlDao.revokeCertificate(certSerial, certCn);
        if (crl != null && crl.getCertSerial().equals(certSerial)) {
            final CAProvider provider = getCAProvider(caProvider);
            return provider.revokeCertificate(certSerial, certCn);
        }
        return false;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_CA_CERTIFICATE_PROVISION, eventDescription = "provisioning certificate for host", async = true)
    public boolean provisionCertificate(final Host host, final Boolean reconnect, final String caProvider, final boolean forced) {
        if (host == null) {
            throw new CloudRuntimeException("Unable to find valid host to renew certificate for");
        }
        CallContext.current().setEventDetails("Host ID: " + host.getUuid());
        CallContext.current().putContextParameter(Host.class, host.getUuid());

        if (forced) {
            return provisionCertificateForced(host, reconnect, caProvider);
        }

        String csr = null;

        try {
            if (host.getType() != Host.Type.ConsoleProxy && host.getType() != Host.Type.SecondaryStorageVM) {
                csr = generateKeyStoreAndCsr(host, null);
                if (StringUtils.isEmpty(csr)) {
                    return false;
                }
            }
            final Certificate certificate = issueCertificate(csr, Arrays.asList(host.getName(), host.getPrivateIpAddress()), Arrays.asList(host.getPrivateIpAddress(), host.getPublicIpAddress(), host.getStorageIpAddress()), CAManager.CertValidityPeriod.value(), caProvider);
            return deployCertificate(host, certificate, reconnect, null);
        } catch (final AgentUnavailableException | OperationTimedoutException e) {
            logger.error("Host/agent is not available or operation timed out, failed to setup keystore and generate CSR for host/agent {}, due to: ", host, e);
            throw new CloudRuntimeException(String.format("Failed to generate keystore and get CSR from the host/agent %s", host));
        }
    }

    private boolean provisionCertificateForced(Host host, Boolean reconnect, String caProvider) {
        if (host.getType() == Host.Type.Routing && host.getHypervisorType() == com.cloud.hypervisor.Hypervisor.HypervisorType.KVM) {
            return provisionKvmHostViaSsh(host);
        } else if (host.getType() == Host.Type.ConsoleProxy || host.getType() == Host.Type.SecondaryStorageVM) {
            return provisionSystemVmViaSsh(host, reconnect);
        }
        throw new CloudRuntimeException("Forced certificate provisioning is only supported for KVM hosts and SystemVMs.");
    }

    @Override
    public void provisionCertificateViaSsh(final Connection sshConnection, final String agentIp, final String agentHostname) {
        Integer validityPeriod = CAManager.CertValidityPeriod.value();
        if (validityPeriod < 1) {
            validityPeriod = 1;
        }

        String keystorePassword = PasswordGenerator.generateRandomPassword(16);

        // 1. Setup Keystore and Generate CSR
        final SSHCmdHelper.SSHCmdResult keystoreSetupResult = SSHCmdHelper.sshExecuteCmdWithResult(sshConnection,
                String.format("sudo /usr/share/cloudstack-common/scripts/util/%s " +
                              "/etc/cloudstack/agent/agent.properties " +
                              "/etc/cloudstack/agent/%s " +
                              "%s %d " +
                              "/etc/cloudstack/agent/%s",
                        KeyStoreUtils.KS_SETUP_SCRIPT,
                        KeyStoreUtils.KS_FILENAME,
                        keystorePassword,
                        validityPeriod,
                        KeyStoreUtils.CSR_FILENAME));

        if (!keystoreSetupResult.isSuccess()) {
            throw new CloudRuntimeException("Failed to setup keystore and generate CSR via SSH on host: " + agentIp);
        }

        // 2. Issue Certificate based on returned CSR
        final String csr = keystoreSetupResult.getStdOut();
        final Certificate certificate = issueCertificate(csr, Arrays.asList(agentHostname, agentIp),
                Collections.singletonList(agentIp), null, null);

        if (certificate == null || certificate.getClientCertificate() == null) {
            throw new CloudRuntimeException("Failed to issue certificates for host: " + agentIp);
        }

        // 3. Import Certificate into agent keystore
        final SetupCertificateCommand certificateCommand = new SetupCertificateCommand(certificate);
        final SSHCmdHelper.SSHCmdResult setupCertResult = SSHCmdHelper.sshExecuteCmdWithResult(sshConnection,
                String.format("sudo /usr/share/cloudstack-common/scripts/util/%s " +
                              "/etc/cloudstack/agent/agent.properties %s " +
                              "/etc/cloudstack/agent/%s %s " +
                              "/etc/cloudstack/agent/%s \"%s\" " +
                              "/etc/cloudstack/agent/%s \"%s\" " +
                              "/etc/cloudstack/agent/%s \"%s\"",
                        KeyStoreUtils.KS_IMPORT_SCRIPT,
                        keystorePassword,
                        KeyStoreUtils.KS_FILENAME,
                        KeyStoreUtils.SSH_MODE,
                        KeyStoreUtils.CERT_FILENAME,
                        certificateCommand.getEncodedCertificate(),
                        KeyStoreUtils.CACERT_FILENAME,
                        certificateCommand.getEncodedCaCertificates(),
                        KeyStoreUtils.PKEY_FILENAME,
                        certificateCommand.getEncodedPrivateKey()));

        if (!setupCertResult.isSuccess()) {
            throw new CloudRuntimeException("Failed to import certificates into agent keystore via SSH on host: " + agentIp);
        }
    }

    private boolean provisionKvmHostViaSsh(Host host) {
        final HostVO hostVO = (HostVO) host;
        hostDao.loadDetails(hostVO);
        String username = hostVO.getDetail(ApiConstants.USERNAME);
        String password = hostVO.getDetail(ApiConstants.PASSWORD);
        String hostIp = host.getPrivateIpAddress();

        int port = AgentManager.KVMHostDiscoverySshPort.valueIn(host.getClusterId());
        if (hostVO.getDetail(Host.HOST_SSH_PORT) != null) {
            port = NumberUtils.toInt(hostVO.getDetail(Host.HOST_SSH_PORT), port);
        }

        Connection sshConnection = null;
        try {
            sshConnection = new Connection(hostIp, port);
            sshConnection.connect(null, 60000, 60000);

            String privateKey = configDao.getValue("ssh.privatekey");
            if (!SSHCmdHelper.acquireAuthorizedConnectionWithPublicKey(sshConnection, username, privateKey)) {
                if (StringUtils.isEmpty(password) || !sshConnection.authenticateWithPassword(username, password)) {
                    throw new CloudRuntimeException("Failed to authenticate to host via SSH for forced provisioning: " + hostIp);
                }
            }

            provisionCertificateViaSsh(sshConnection, hostIp, host.getName());

            SSHCmdHelper.sshExecuteCmd(sshConnection, "sudo service cloudstack-agent restart");
            return true;
        } catch (Exception e) {
            logger.error("Error during forced SSH provisioning for KVM host " + host.getUuid(), e);
            return false;
        } finally {
            if (sshConnection != null) {
                sshConnection.close();
            }
        }
    }

    private boolean provisionSystemVmViaSsh(Host host, Boolean reconnect) {
        VMInstanceVO vm = vmInstanceDao.findVMByInstanceName(host.getName());
        if (vm == null) {
            throw new CloudRuntimeException("Cannot find underlying VM for host: " + host.getName());
        }

        final Map<String, String> sshAccessDetails = networkOrchestrationService.getSystemVMAccessDetails(vm);
        final Map<String, String> ipAddressDetails = new HashMap<>(sshAccessDetails);
        ipAddressDetails.remove(NetworkElementCommand.ROUTER_NAME);

        try {
            final Host hypervisorHost = hostDao.findById(vm.getHostId());
            if (hypervisorHost == null) {
                throw new CloudRuntimeException("Cannot find hypervisor host for system VM: " + host.getName());
            }

            final Certificate certificate = issueCertificate(null, Arrays.asList(vm.getHostName(), vm.getInstanceName()),
                    new ArrayList<>(ipAddressDetails.values()), CertValidityPeriod.value(), null);
            return deployCertificate(hypervisorHost, certificate, reconnect, sshAccessDetails);
        } catch (Exception e) {
            logger.error("Failed to provision system VM " + host.getName() + " via hypervisor SSH proxy. Ensure the hypervisor host is connected.", e);
            return false;
        }
    }

    @Override
    public String generateKeyStoreAndCsr(final Host host, final Map<String, String> sshAccessDetails) throws AgentUnavailableException, OperationTimedoutException {
        final SetupKeyStoreCommand cmd = new SetupKeyStoreCommand(CertValidityPeriod.value());
        if (sshAccessDetails != null && !sshAccessDetails.isEmpty()) {
            cmd.setAccessDetail(sshAccessDetails);
        }
        CallContext.current().setEventDetails("generating keystore and CSR for Host with ID: " + host.getUuid());
        final SetupKeystoreAnswer answer = (SetupKeystoreAnswer)agentManager.send(host.getId(), cmd);
        return answer.getCsr();
    }

    private boolean isValidSystemVMType(Host.Type type) {
        return Host.Type.SecondaryStorageVM.equals(type) ||
                Host.Type.ConsoleProxy.equals(type);
    }

    @Override
    public boolean deployCertificate(final Host host, final Certificate certificate, final Boolean reconnect, final Map<String, String> sshAccessDetails)
            throws AgentUnavailableException, OperationTimedoutException {
        final SetupCertificateCommand cmd = new SetupCertificateCommand(certificate);
        if (sshAccessDetails != null && !sshAccessDetails.isEmpty()) {
            cmd.setAccessDetail(sshAccessDetails);
        }
        CallContext.current().setEventDetails("deploying certificate for Host with ID: " + host.getUuid());
        final SetupCertificateAnswer answer = (SetupCertificateAnswer)agentManager.send(host.getId(), cmd);
        if (answer.getResult()) {
            CallContext.current().setEventDetails("successfully deployed certificate for Host with ID: " + host.getUuid());
        } else {
            CallContext.current().setEventDetails("failed to deploy certificate for Host with ID: " + host.getUuid());
        }

        if (answer.getResult()) {
            getActiveCertificatesMap().put(host.getPrivateIpAddress(), certificate.getClientCertificate());
            if (sshAccessDetails == null && reconnect != null && reconnect) {
                logger.info("Successfully setup certificate on host, reconnecting with agent [{}] with address={}", host, host.getPublicIpAddress());
                try {
                    agentManager.reconnect(host.getId());
                } catch (AgentUnavailableException | CloudRuntimeException e) {
                    logger.debug("Error when reconnecting to host: {}", host, e);
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public void purgeHostCertificate(final Host host) {
        if (host == null) {
            return;
        }
        final String privateAddress = host.getPrivateIpAddress();
        final String publicAddress = host.getPublicIpAddress();
        final Map<String, X509Certificate> activeCertsMap = getActiveCertificatesMap();
        if (StringUtils.isNotEmpty(privateAddress) && activeCertsMap.containsKey(privateAddress)) {
            activeCertsMap.remove(privateAddress);
        }
        if (StringUtils.isNotEmpty(publicAddress) && activeCertsMap.containsKey(publicAddress)) {
            activeCertsMap.remove(publicAddress);
        }
    }

    @Override
    public void sendAlert(final Host host, final String subject, final String message) {
        if (host == null) {
            return;
        }
        alertManager.sendAlert(AlertManager.AlertType.ALERT_TYPE_CA_CERT, host.getDataCenterId(), host.getPodId(), subject, message);
    }

    @Override
    public SSLEngine createSSLEngine(final SSLContext sslContext, final String remoteAddress) throws GeneralSecurityException, IOException {
        if (sslContext == null) {
            throw new CloudRuntimeException("SSLContext provided to create SSLEngine is null, aborting");
        }
        if (StringUtils.isEmpty(remoteAddress)) {
            throw new CloudRuntimeException("Remote client address connecting to mgmt server cannot be empty/null");
        }
        return getConfiguredCaProvider().createSSLEngine(sslContext, remoteAddress, getActiveCertificatesMap());
    }

    @Override
    public KeyStore getManagementKeyStore() throws KeyStoreException {
        return getConfiguredCaProvider().getManagementKeyStore();
    }

    @Override
    public char[] getKeyStorePassphrase() {
        return getConfiguredCaProvider().getKeyStorePassphrase();
    }

    ////////////////////////////////////////////////////
    /////////////// CA Manager Setup ///////////////////
    ////////////////////////////////////////////////////

    public static final class CABackgroundTask extends ManagedContextRunnable implements BackgroundPollTask {
        private CAManager caManager;
        private HostDao hostDao;

        public CABackgroundTask(final CAManager caManager, final HostDao hostDao) {
            this.caManager = caManager;
            this.hostDao = hostDao;
        }

        @Override
        protected void runInContext() {
            try {
                if (logger.isTraceEnabled()) {
                    logger.trace("CA background task is running...");
                }
                final DateTime now = DateTime.now(DateTimeZone.UTC);
                final Map<String, X509Certificate> certsMap = caManager.getActiveCertificatesMap();
                for (final Iterator<Map.Entry<String, X509Certificate>> it = certsMap.entrySet().iterator(); it.hasNext();) {
                    final Map.Entry<String, X509Certificate> entry = it.next();
                    if (entry == null) {
                        continue;
                    }
                    final String hostIp = entry.getKey();
                    final X509Certificate certificate = entry.getValue();
                    if (certificate == null) {
                        it.remove();
                        continue;
                    }
                    final Host host = hostDao.findByIp(hostIp);
                    if (host == null || host.getManagementServerId() == null || host.getManagementServerId() != ManagementServerNode.getManagementServerId() || host.getStatus() != Status.Up) {
                        if (host == null || (host.getManagementServerId() != null && host.getManagementServerId() != ManagementServerNode.getManagementServerId())) {
                            it.remove();
                        }
                        continue;
                    }

                    final String hostDescription = String.format("host id=%d, uuid=%s, name=%s, ip=%s, zone id=%d", host.getId(), host.getUuid(), host.getName(), hostIp, host.getDataCenterId());

                    try {
                        certificate.checkValidity(now.plusDays(CertExpiryAlertPeriod.valueIn(host.getClusterId())).toDate());
                    } catch (final CertificateExpiredException | CertificateNotYetValidException e) {
                        logger.warn("Certificate is going to expire for " + hostDescription, e);
                        if (AutomaticCertRenewal.valueIn(host.getClusterId())) {
                            try {
                                logger.debug("Attempting certificate auto-renewal for " + hostDescription, e);
                                boolean result = caManager.provisionCertificate(host, false, null, false);
                                if (result) {
                                    logger.debug("Succeeded in auto-renewing certificate for " + hostDescription, e);
                                } else {
                                    logger.debug("Failed in auto-renewing certificate for " + hostDescription, e);
                                }
                            } catch (final Throwable ex) {
                                logger.warn("Failed to auto-renew certificate for " + hostDescription + ", with error=", ex);
                                caManager.sendAlert(host, "Certificate auto-renewal failed for " + hostDescription,
                                        String.format("Certificate is going to expire for %s. Auto-renewal failed to renew the certificate, please renew it manually. It is not valid after %s.",
                                                hostDescription, certificate.getNotAfter()));
                            }
                        } else {
                            if (alertMap.containsKey(hostIp)) {
                                final Date lastSentDate = alertMap.get(hostIp);
                                if (now.minusDays(1).toDate().before(lastSentDate)) {
                                    continue;
                                }
                            }
                            caManager.sendAlert(host, "Certificate expiring soon for " + hostDescription,
                                    String.format("Certificate is going to expire for %s. Please renew it, it is not valid after %s.", hostDescription, certificate.getNotAfter()));
                            alertMap.put(hostIp, new Date());
                        }
                    }
                }
            } catch (final Throwable t) {
                logger.error("Error trying to run CA background task", t);
            }
        }

        @Override
        public Long getDelay() {
            return CABackgroundJobDelay.value() * 1000L;
        }
    }

    public void setCaProviders(final List<CAProvider> caProviders) {
        this.caProviders = caProviders;
        initializeCaProviderMap();
    }

    private void initializeCaProviderMap() {
        if (caProviderMap != null && caProviderMap.size() != caProviders.size()) {
            for (final CAProvider caProvider : caProviders) {
                caProviderMap.put(caProvider.getProviderName().toLowerCase(), caProvider);
            }
        }
    }

    @Override
    public boolean start() {
        super.start();
        initializeCaProviderMap();
        if (caProviderMap.containsKey(CAProviderPlugin.value())) {
            configuredCaProvider = caProviderMap.get(CAProviderPlugin.value());
        }
        if (configuredCaProvider == null) {
            logger.error("Failed to find valid configured CA provider, please check!");
            return false;
        }
        if (CaInjectDefaultTruststore.value()) {
            injectCaCertIntoDefaultTruststore();
        }
        return true;
    }

    private void injectCaCertIntoDefaultTruststore() {
        try {
            final List<X509Certificate> caCerts = configuredCaProvider.getCaCertificate();
            if (caCerts == null || caCerts.isEmpty()) {
                logger.debug("No CA certificates found from the configured provider, skipping JVM truststore injection");
                return;
            }

            final KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            trustStore.load(null, null);

            // Copy existing default trusted certs
            final TrustManagerFactory defaultTmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            defaultTmf.init((KeyStore) null);
            final X509TrustManager defaultTm = (X509TrustManager) defaultTmf.getTrustManagers()[0];
            int aliasIndex = 0;
            for (final X509Certificate cert : defaultTm.getAcceptedIssuers()) {
                trustStore.setCertificateEntry("default-ca-" + aliasIndex++, cert);
            }

            // Add CA provider's certificates
            int count = 0;
            for (final X509Certificate caCert : caCerts) {
                final String alias = "cloudstack-ca-" + count;
                trustStore.setCertificateEntry(alias, caCert);
                count++;
                logger.info("Injected CA certificate into JVM default truststore: subject={}, alias={}",
                    caCert.getSubjectX500Principal().getName(), alias);
            }

            // Reinitialize default SSLContext with the updated truststore
            final TrustManagerFactory updatedTmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            updatedTmf.init(trustStore);
            final SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, updatedTmf.getTrustManagers(), new SecureRandom());
            SSLContext.setDefault(sslContext);
            logger.info("Successfully injected {} CA certificate(s) into JVM default truststore", count);
        } catch (final GeneralSecurityException | IOException e) {
            logger.error("Failed to inject CA certificate into JVM default truststore", e);
        }
    }

    @Override
    public boolean configure(final String name, final Map<String, Object> params) throws ConfigurationException {
        backgroundPollManager.submitTask(new CABackgroundTask(this, hostDao));
        return true;
    }

    //////////////////////////////////////////////////////////
    /////////////// CA Manager Descriptors ///////////////////
    //////////////////////////////////////////////////////////

    @Override
    public List<Class<?>> getCommands() {
        final List<Class<?>> cmdList = new ArrayList<Class<?>>();
        cmdList.add(ListCAProvidersCmd.class);
        cmdList.add(ListCaCertificateCmd.class);
        cmdList.add(IssueCertificateCmd.class);
        cmdList.add(ProvisionCertificateCmd.class);
        cmdList.add(RevokeCertificateCmd.class);
        return cmdList;
    }

    @Override
    public String getConfigComponentName() {
        return CAManager.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[] {CAProviderPlugin, CertKeySize, CertSignatureAlgorithm, CertValidityPeriod,
                AutomaticCertRenewal, AllowHostIPInSysVMAgentCert, CABackgroundJobDelay, CertExpiryAlertPeriod,
                CertManagementCustomSubjectAlternativeName, CaInjectDefaultTruststore
        };
    }

    @Override
    public boolean isManagementCertificate(java.security.cert.Certificate certificate) throws CertificateParsingException {
        return getConfiguredCaProvider().isManagementCertificate(certificate);
    }
}
