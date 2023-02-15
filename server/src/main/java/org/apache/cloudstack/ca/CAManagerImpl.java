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
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.cloud.agent.AgentManager;
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
    public static final Logger LOG = Logger.getLogger(CAManagerImpl.class);

    @Inject
    private CrlDao crlDao;
    @Inject
    private HostDao hostDao;
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
    public boolean provisionCertificate(final Host host, final Boolean reconnect, final String caProvider) {
        if (host == null) {
            throw new CloudRuntimeException("Unable to find valid host to renew certificate for");
        }
        CallContext.current().setEventDetails("host id: " + host.getId());
        CallContext.current().putContextParameter(Host.class, host.getUuid());
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
            LOG.error("Host/agent is not available or operation timed out, failed to setup keystore and generate CSR for host/agent id=" + host.getId() + ", due to: ", e);
            throw new CloudRuntimeException("Failed to generate keystore and get CSR from the host/agent id=" + host.getId());
        }
    }

    @Override
    public String generateKeyStoreAndCsr(final Host host, final Map<String, String> sshAccessDetails) throws AgentUnavailableException, OperationTimedoutException {
        final SetupKeyStoreCommand cmd = new SetupKeyStoreCommand(CertValidityPeriod.value());
        if (sshAccessDetails != null && !sshAccessDetails.isEmpty()) {
            cmd.setAccessDetail(sshAccessDetails);
        }
        CallContext.current().setEventDetails("generating keystore and CSR for host id: " + host.getId());
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
        CallContext.current().setEventDetails("deploying certificate for host id: " + host.getId());
        final SetupCertificateAnswer answer = (SetupCertificateAnswer)agentManager.send(host.getId(), cmd);
        if (answer.getResult()) {
            CallContext.current().setEventDetails("successfully deployed certificate for host id: " + host.getId());
        } else {
            CallContext.current().setEventDetails("failed to deploy certificate for host id: " + host.getId());
        }

        if (answer.getResult()) {
            getActiveCertificatesMap().put(host.getPrivateIpAddress(), certificate.getClientCertificate());
            if (sshAccessDetails == null && reconnect != null && reconnect) {
                LOG.info(String.format("Successfully setup certificate on host, reconnecting with agent with id=%d, name=%s, address=%s", host.getId(), host.getName(), host.getPublicIpAddress()));
                try {
                    agentManager.reconnect(host.getId());
                } catch (AgentUnavailableException | CloudRuntimeException e) {
                    LOG.debug("Error when reconnecting to host: " + host.getUuid(), e);
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
                if (LOG.isTraceEnabled()) {
                    LOG.trace("CA background task is running...");
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
                        LOG.warn("Certificate is going to expire for " + hostDescription, e);
                        if (AutomaticCertRenewal.valueIn(host.getClusterId())) {
                            try {
                                LOG.debug("Attempting certificate auto-renewal for " + hostDescription, e);
                                boolean result = caManager.provisionCertificate(host, false, null);
                                if (result) {
                                    LOG.debug("Succeeded in auto-renewing certificate for " + hostDescription, e);
                                } else {
                                    LOG.debug("Failed in auto-renewing certificate for " + hostDescription, e);
                                }
                            } catch (final Throwable ex) {
                                LOG.warn("Failed to auto-renew certificate for " + hostDescription + ", with error=", ex);
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
                LOG.error("Error trying to run CA background task", t);
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
            LOG.error("Failed to find valid configured CA provider, please check!");
            return false;
        }
        return true;
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
        return new ConfigKey<?>[] {CAProviderPlugin, CertKeySize, CertSignatureAlgorithm, CertValidityPeriod, AutomaticCertRenewal, AllowHostIPInSysVMAgentCert, CABackgroundJobDelay, CertExpiryAlertPeriod};
    }
}
