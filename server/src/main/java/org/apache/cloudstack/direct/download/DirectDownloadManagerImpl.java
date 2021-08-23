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
package org.apache.cloudstack.direct.download;

import static com.cloud.storage.Storage.ImageFormat;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.agent.directdownload.DirectDownloadAnswer;
import org.apache.cloudstack.agent.directdownload.DirectDownloadCommand;
import org.apache.cloudstack.agent.directdownload.DirectDownloadCommand.DownloadProtocol;
import org.apache.cloudstack.agent.directdownload.HttpDirectDownloadCommand;
import org.apache.cloudstack.agent.directdownload.HttpsDirectDownloadCommand;
import org.apache.cloudstack.agent.directdownload.MetalinkDirectDownloadCommand;
import org.apache.cloudstack.agent.directdownload.NfsDirectDownloadCommand;
import org.apache.cloudstack.agent.directdownload.RevokeDirectDownloadCertificateCommand;
import org.apache.cloudstack.agent.directdownload.SetupDirectDownloadCertificateCommand;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeService;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.managed.context.ManagedContextRunnable;
import org.apache.cloudstack.poll.BackgroundPollManager;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.to.PrimaryDataStoreTO;
import org.apache.cloudstack.storage.to.TemplateObjectTO;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.event.ActionEventUtils;
import com.cloud.event.EventTypes;
import com.cloud.event.EventVO;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.ScopeType;
import com.cloud.storage.Storage;
import com.cloud.storage.StorageManager;
import com.cloud.storage.VMTemplateStoragePoolVO;
import com.cloud.storage.VMTemplateStorageResourceAssoc;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VMTemplatePoolDao;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.security.CertificateHelper;

import sun.security.x509.X509CertImpl;

public class DirectDownloadManagerImpl extends ManagerBase implements DirectDownloadManager {

    private static final Logger s_logger = Logger.getLogger(DirectDownloadManagerImpl.class);
    protected static final String httpHeaderDetailKey = "HTTP_HEADER";
    protected static final String BEGIN_CERT = "-----BEGIN CERTIFICATE-----";
    protected static final String END_CERT = "-----END CERTIFICATE-----";
    protected final static String LINE_SEPARATOR = "\n";

    @Inject
    private VMTemplateDao vmTemplateDao;
    @Inject
    private PrimaryDataStoreDao primaryDataStoreDao;
    @Inject
    private HostDao hostDao;
    @Inject
    private AgentManager agentManager;
    @Inject
    private VMTemplatePoolDao vmTemplatePoolDao;
    @Inject
    private DataStoreManager dataStoreManager;
    @Inject
    private DirectDownloadCertificateDao directDownloadCertificateDao;
    @Inject
    private DirectDownloadCertificateHostMapDao directDownloadCertificateHostMapDao;
    @Inject
    private BackgroundPollManager backgroundPollManager;
    @Inject
    private DataCenterDao dataCenterDao;
    @Inject
    private ConfigurationDao configDao;
    @Inject
    private TemplateDataFactory tmplFactory;
    @Inject
    private VolumeService volService;

    protected ScheduledExecutorService executorService;

    @Override
    public List<Class<?>> getCommands() {
        final List<Class<?>> cmdList = new ArrayList<Class<?>>();
        return cmdList;
    }

    /**
     * Return protocol to use from provided URL
     * @param url
     * @return
     */
    public static DownloadProtocol getProtocolFromUrl(String url) {
        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            throw new CloudRuntimeException("URI is incorrect: " + url);
        }
        if ((uri != null) && (uri.getScheme() != null)) {
            if (uri.getPath().endsWith(".metalink")) {
                return DownloadProtocol.METALINK;
            } else if (uri.getScheme().equalsIgnoreCase("http")) {
                return DownloadProtocol.HTTP;
            } else if (uri.getScheme().equalsIgnoreCase("https")) {
                return DownloadProtocol.HTTPS;
            } else if (uri.getScheme().equalsIgnoreCase("nfs")) {
                return DownloadProtocol.NFS;
            } else {
                throw new CloudRuntimeException("Scheme is not supported " + url);
            }
        } else {
            throw new CloudRuntimeException("URI is incorrect: " + url);
        }
    }

    /**
     * Return HTTP headers from template details
     * @param templateDetails
     * @return
     */
    protected Map<String, String> getHeadersFromDetails(Map<String, String> templateDetails) {
        if (MapUtils.isEmpty(templateDetails)) {
            return new HashMap<>();
        }
        Map<String, String> headers = new HashMap<>();
        for (String key : templateDetails.keySet()) {
            if (key.startsWith(httpHeaderDetailKey)) {
                String header = key.split(":")[1];
                String value = templateDetails.get(key);
                headers.put(header, value);
            }
        }
        return headers;
    }

    /**
     * Get running host IDs within the same hypervisor, cluster and datacenter than hostId. ID hostId is not included on the returned list
     */
    protected List<Long> getRunningHostIdsInTheSameCluster(Long clusterId, long dataCenterId, HypervisorType hypervisorType, long hostId) {
        List<Long> list = hostDao.listByDataCenterIdAndHypervisorType(dataCenterId, hypervisorType)
                .stream()
                .filter(x -> x.getHypervisorType().equals(hypervisorType) && x.getStatus().equals(Status.Up) &&
                        x.getType().equals(Host.Type.Routing) && x.getClusterId().equals(clusterId) &&
                        x.getId() != hostId)
                .map(x -> x.getId())
                .collect(Collectors.toList());
        Collections.shuffle(list);
        return list;
    }

    /**
     * Create host IDs array having hostId as the first element
     */
    protected Long[] createHostIdsList(List<Long> hostIds, long hostId) {
        if (CollectionUtils.isEmpty(hostIds)) {
            return Collections.singletonList(hostId).toArray(new Long[1]);
        }
        Long[] ids = new Long[hostIds.size() + 1];
        ids[0] = hostId;
        int i = 1;
        for (Long id : hostIds) {
            ids[i] = id;
            i++;
        }
        return ids;
    }

    /**
     * Get alternative hosts to retry downloading a template. The planner have previously selected a host and a storage pool
     * @return array of host ids which can access the storage pool
     */
    protected Long[] getHostsToRetryOn(Host host, StoragePoolVO storagePool) {
        List<Long> clusterHostIds = new ArrayList<>();
        if (storagePool.getPoolType() != Storage.StoragePoolType.Filesystem || storagePool.getScope() != ScopeType.HOST) {
            clusterHostIds = getRunningHostIdsInTheSameCluster(host.getClusterId(), host.getDataCenterId(), host.getHypervisorType(), host.getId());
        }
        return createHostIdsList(clusterHostIds, host.getId());
    }

    @Override
    public void downloadTemplate(long templateId, long poolId, long hostId) {
        VMTemplateVO template = vmTemplateDao.findById(templateId);
        StoragePoolVO pool = primaryDataStoreDao.findById(poolId);
        HostVO host = hostDao.findById(hostId);
        if (pool == null) {
            throw new CloudRuntimeException("Storage pool " + poolId + " could not be found");
        }
        if (template == null) {
            throw new CloudRuntimeException("Template " + templateId + " could not be found");
        }
        if (host == null) {
            throw new CloudRuntimeException("Host " + hostId + " could not be found");
        }
        if (!template.isDirectDownload()) {
            throw new CloudRuntimeException("Template " + templateId + " is not marked for direct download");
        }
        Map<String, String> details = template.getDetails();
        String url = template.getUrl();
        String checksum = template.getChecksum();
        Map<String, String> headers = getHeadersFromDetails(details);
        DataStore store = dataStoreManager.getDataStore(poolId, DataStoreRole.Primary);
        if (store == null) {
            throw new CloudRuntimeException("Data store " + poolId + " could not be found");
        }
        PrimaryDataStore primaryDataStore = (PrimaryDataStore) store;
        PrimaryDataStoreTO to = (PrimaryDataStoreTO) primaryDataStore.getTO();

        DownloadProtocol protocol = getProtocolFromUrl(url);
        DirectDownloadCommand cmd = getDirectDownloadCommandFromProtocol(protocol, url, templateId, to, checksum, headers);
        cmd.setTemplateSize(template.getSize());
        cmd.setFormat(template.getFormat());

        if (tmplFactory.getTemplate(templateId, store) != null) {
            cmd.setDestData((TemplateObjectTO) tmplFactory.getTemplate(templateId, store).getTO());
        }

        int cmdTimeOut = StorageManager.PRIMARY_STORAGE_DOWNLOAD_WAIT.value();
        cmd.setWait(cmdTimeOut);

        Answer answer = sendDirectDownloadCommand(cmd, template, poolId, host);

        VMTemplateStoragePoolVO sPoolRef = vmTemplatePoolDao.findByPoolTemplate(poolId, templateId, null);
        if (sPoolRef == null) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Not found (templateId:" + templateId + " poolId: " + poolId + ") in template_spool_ref, persisting it");
            }
            DirectDownloadAnswer ans = (DirectDownloadAnswer) answer;
            sPoolRef = new VMTemplateStoragePoolVO(poolId, templateId, null);
            sPoolRef.setDownloadPercent(100);
            sPoolRef.setDownloadState(VMTemplateStorageResourceAssoc.Status.DOWNLOADED);
            sPoolRef.setState(ObjectInDataStoreStateMachine.State.Ready);
            sPoolRef.setTemplateSize(ans.getTemplateSize());
            sPoolRef.setLocalDownloadPath(ans.getInstallPath());
            sPoolRef.setInstallPath(ans.getInstallPath());
            vmTemplatePoolDao.persist(sPoolRef);
        } else {
            // For managed storage, update after template downloaded and copied to the disk
            DirectDownloadAnswer ans = (DirectDownloadAnswer) answer;
            sPoolRef.setDownloadPercent(100);
            sPoolRef.setDownloadState(VMTemplateStorageResourceAssoc.Status.DOWNLOADED);
            sPoolRef.setState(ObjectInDataStoreStateMachine.State.Ready);
            sPoolRef.setTemplateSize(ans.getTemplateSize());
            sPoolRef.setLocalDownloadPath(ans.getInstallPath());
            sPoolRef.setInstallPath(ans.getInstallPath());
            vmTemplatePoolDao.update(sPoolRef.getId(), sPoolRef);
        }
    }

    /**
     * Send direct download command for downloading template with ID templateId on storage pool with ID poolId.<br/>
     * At first, cmd is sent to host, in case of failure it will retry on other hosts before failing
     * @param cmd direct download command
     * @param template template
     * @param poolId pool id
     * @param host first host to which send the command
     * @return download answer from any host which could handle cmd
     */
    private Answer sendDirectDownloadCommand(DirectDownloadCommand cmd, VMTemplateVO template, long poolId, HostVO host) {
        boolean downloaded = false;
        int retry = 3;

        StoragePoolVO storagePoolVO = primaryDataStoreDao.findById(poolId);
        // TODO: Move the host retry attempts to upper layer
        Long[] hostsToRetry = getHostsToRetryOn(host, storagePoolVO);
        int hostIndex = 0;
        Answer answer = null;
        Long hostToSendDownloadCmd = hostsToRetry[hostIndex];
        boolean continueRetrying = true;
        while (!downloaded && retry > 0 && continueRetrying) {
            PrimaryDataStore primaryDataStore = null;
            TemplateInfo templateOnPrimary = null;

            try {
                if (hostToSendDownloadCmd != host.getId() && storagePoolVO.isManaged()) {
                    primaryDataStore = (PrimaryDataStore) dataStoreManager.getPrimaryDataStore(poolId);
                    templateOnPrimary = primaryDataStore.getTemplate(template.getId(), null);
                    if (templateOnPrimary != null) {
                        volService.grantAccess(templateOnPrimary, host, primaryDataStore);
                    }
                }

                s_logger.debug("Sending Direct download command to host " + hostToSendDownloadCmd);
                answer = agentManager.easySend(hostToSendDownloadCmd, cmd);
                if (answer != null) {
                    DirectDownloadAnswer ans = (DirectDownloadAnswer)answer;
                    downloaded = answer.getResult();
                    continueRetrying = ans.isRetryOnOtherHosts();
                }
                hostToSendDownloadCmd = hostsToRetry[(hostIndex + 1) % hostsToRetry.length];
            } finally {
                if (templateOnPrimary != null) {
                    volService.revokeAccess(templateOnPrimary, host, primaryDataStore);
                }
            }

            retry --;
        }
        if (!downloaded) {
            logUsageEvent(template, poolId);
            throw new CloudRuntimeException("Template " + template.getId() + " could not be downloaded on pool " + poolId + ", failing after trying on several hosts");
        }
        return answer;
    }

    /**
     * Log and persist event for direct download failure
     */
    private void logUsageEvent(VMTemplateVO template, long poolId) {
        String event = EventTypes.EVENT_TEMPLATE_DIRECT_DOWNLOAD_FAILURE;
        if (template.getFormat() == ImageFormat.ISO) {
            event = EventTypes.EVENT_ISO_DIRECT_DOWNLOAD_FAILURE;
        }
        String description = "Direct Download for template Id: " + template.getId() + " on pool Id: " + poolId + " failed";
        s_logger.error(description);
        ActionEventUtils.onCompletedActionEvent(CallContext.current().getCallingUserId(), template.getAccountId(), EventVO.LEVEL_INFO, event, description, 0);
    }

    /**
     * Return DirectDownloadCommand according to the protocol
     */
    private DirectDownloadCommand getDirectDownloadCommandFromProtocol(DownloadProtocol protocol, String url, Long templateId, PrimaryDataStoreTO destPool,
                                                                       String checksum, Map<String, String> httpHeaders) {
        int connectTimeout = DirectDownloadConnectTimeout.value();
        int soTimeout = DirectDownloadSocketTimeout.value();
        int connectionRequestTimeout = DirectDownloadConnectionRequestTimeout.value();
        if (protocol.equals(DownloadProtocol.HTTP)) {
            return new HttpDirectDownloadCommand(url, templateId, destPool, checksum, httpHeaders, connectTimeout, soTimeout);
        } else if (protocol.equals(DownloadProtocol.HTTPS)) {
            return new HttpsDirectDownloadCommand(url, templateId, destPool, checksum, httpHeaders, connectTimeout, soTimeout, connectionRequestTimeout);
        } else if (protocol.equals(DownloadProtocol.NFS)) {
            return new NfsDirectDownloadCommand(url, templateId, destPool, checksum, httpHeaders);
        } else if (protocol.equals(DownloadProtocol.METALINK)) {
            return new MetalinkDirectDownloadCommand(url, templateId, destPool, checksum, httpHeaders, connectTimeout, soTimeout);
        } else {
            return null;
        }
    }

    /**
     * Return the list of running hosts to which upload certificates for Direct Download
     */
    private List<HostVO> getRunningHostsToUploadCertificate(Long zoneId, HypervisorType hypervisorType) {
        return hostDao.listAllHostsUpByZoneAndHypervisor(zoneId, hypervisorType);
    }

    /**
     * Return pretified PEM certificate
     */
    protected String getPretifiedCertificate(String certificateCer) {
        String cert = certificateCer.replaceAll("(.{64})", "$1\n");
        if (!cert.startsWith(BEGIN_CERT) && !cert.endsWith(END_CERT)) {
            cert = BEGIN_CERT + LINE_SEPARATOR + cert + LINE_SEPARATOR + END_CERT;
        }
        return cert;
    }

    /**
     * Generate and return certificate from the string
     * @throws CloudRuntimeException if the certificate is not well formed
     */
    private Certificate getCertificateFromString(String certificatePem) {
        try {
            return CertificateHelper.buildCertificate(certificatePem);
        } catch (CertificateException e) {
            e.printStackTrace();
            throw new CloudRuntimeException("Cannot parse the certificate provided, please provide a PEM certificate. Error: " + e.getMessage());
        }
    }

    /**
     * Perform sanity of string parsed certificate
     */
    protected void certificateSanity(String certificatePem) {
        Certificate certificate = getCertificateFromString(certificatePem);

        if (certificate instanceof X509CertImpl) {
            X509CertImpl x509Cert = (X509CertImpl) certificate;
            try {
                x509Cert.checkValidity();
            } catch (CertificateExpiredException | CertificateNotYetValidException e) {
                String msg = "Certificate is invalid. Please provide a valid certificate. Error: " + e.getMessage();
                s_logger.error(msg);
                throw new CloudRuntimeException(msg);
            }
            if (x509Cert.getSubjectDN() != null) {
                s_logger.debug("Valid certificate for domain name: " + x509Cert.getSubjectDN().getName());
            }
        }
    }

    @Override
    public boolean uploadCertificateToHosts(String certificateCer, String alias, String hypervisor, Long zoneId, Long hostId) {
        if (alias != null && (alias.equalsIgnoreCase("cloud") || alias.startsWith("cloudca"))) {
            throw new CloudRuntimeException("Please provide a different alias name for the certificate");
        }

        List<HostVO> hosts;
        DirectDownloadCertificateVO certificateVO;
        HypervisorType hypervisorType = HypervisorType.getType(hypervisor);

        if (hostId == null) {
            hosts = getRunningHostsToUploadCertificate(zoneId, hypervisorType);

            String certificatePem = getPretifiedCertificate(certificateCer);
            certificateSanity(certificatePem);

            certificateVO = directDownloadCertificateDao.findByAlias(alias, hypervisorType, zoneId);
            if (certificateVO != null) {
                throw new CloudRuntimeException("Certificate alias " + alias + " has been already created");
            }
            certificateVO = new DirectDownloadCertificateVO(alias, certificatePem, hypervisorType, zoneId);
            directDownloadCertificateDao.persist(certificateVO);
        } else {
            HostVO host = hostDao.findById(hostId);
            hosts = Collections.singletonList(host);
            certificateVO = directDownloadCertificateDao.findByAlias(alias, hypervisorType, zoneId);
            if (certificateVO == null) {
                s_logger.info("Certificate must be uploaded on zone " + zoneId);
                return false;
            }
        }

        s_logger.info("Attempting to upload certificate: " + alias + " to " + hosts.size() + " hosts on zone " + zoneId);
        int hostCount = 0;
        if (CollectionUtils.isNotEmpty(hosts)) {
            for (HostVO host : hosts) {
                if (!uploadCertificate(certificateVO.getId(), host.getId())) {
                    String msg = "Could not upload certificate " + alias + " on host: " + host.getName() + " (" + host.getUuid() + ")";
                    s_logger.error(msg);
                    throw new CloudRuntimeException(msg);
                }
                hostCount++;
            }
        }
        s_logger.info("Certificate was successfully uploaded to " + hostCount + " hosts");
        return true;
    }

    /**
     * Upload and import certificate to hostId on keystore
     */
    public boolean uploadCertificate(long certificateId, long hostId) {
        DirectDownloadCertificateVO certificateVO = directDownloadCertificateDao.findById(certificateId);
        if (certificateVO == null) {
            throw new CloudRuntimeException("Could not find certificate with id " + certificateId + " to upload to host: " + hostId);
        }

        String certificate = certificateVO.getCertificate();
        String alias = certificateVO.getAlias();

        s_logger.debug("Uploading certificate: " + certificateVO.getAlias() + " to host " + hostId);
        SetupDirectDownloadCertificateCommand cmd = new SetupDirectDownloadCertificateCommand(certificate, alias);
        Answer answer = agentManager.easySend(hostId, cmd);
        if (answer == null || !answer.getResult()) {
            String msg = "Certificate " + alias + " could not be added to host " + hostId;
            if (answer != null) {
                msg += " due to: " + answer.getDetails();
            }
            s_logger.error(msg);
            return false;
        }

        s_logger.info("Certificate " + alias + " successfully uploaded to host: " + hostId);
        DirectDownloadCertificateHostMapVO map = directDownloadCertificateHostMapDao.findByCertificateAndHost(certificateId, hostId);
        if (map != null) {
            map.setRevoked(false);
            directDownloadCertificateHostMapDao.update(map.getId(), map);
        } else {
            DirectDownloadCertificateHostMapVO mapVO = new DirectDownloadCertificateHostMapVO(certificateId, hostId);
            directDownloadCertificateHostMapDao.persist(mapVO);
        }

        return true;
    }

    @Override
    public boolean syncCertificatesToHost(long hostId, long zoneId) {
        List<DirectDownloadCertificateVO> zoneCertificates = directDownloadCertificateDao.listByZone(zoneId);
        if (CollectionUtils.isEmpty(zoneCertificates)) {
            if (s_logger.isTraceEnabled()) {
                s_logger.trace("No certificates to sync on host: " + hostId);
            }
            return true;
        }

        boolean syncCertificatesResult = true;
        int certificatesSyncCount = 0;
        s_logger.debug("Syncing certificates on host: " + hostId);
        for (DirectDownloadCertificateVO certificateVO : zoneCertificates) {
            DirectDownloadCertificateHostMapVO mapping = directDownloadCertificateHostMapDao.findByCertificateAndHost(certificateVO.getId(), hostId);
            if (mapping == null) {
                s_logger.debug("Syncing certificate " + certificateVO.getId() + " (" + certificateVO.getAlias() + ") on host: " + hostId + ", uploading it");
                if (!uploadCertificate(certificateVO.getId(), hostId)) {
                    String msg = "Could not sync certificate " + certificateVO.getId() + " (" + certificateVO.getAlias() + ") on host: " + hostId + ", upload failed";
                    s_logger.error(msg);
                    syncCertificatesResult = false;
                } else {
                    certificatesSyncCount++;
                }
            } else {
                s_logger.debug("Certificate " + certificateVO.getId() + " (" + certificateVO.getAlias() + ") already synced on host: " + hostId);
            }
        }

        s_logger.debug("Synced " + certificatesSyncCount + " out of " + zoneCertificates.size() + " certificates on host: " + hostId);
        return syncCertificatesResult;
    }

    @Override
    public boolean revokeCertificateAlias(String certificateAlias, String hypervisor, Long zoneId, Long hostId) {
        HypervisorType hypervisorType = HypervisorType.getType(hypervisor);
        DirectDownloadCertificateVO certificateVO = directDownloadCertificateDao.findByAlias(certificateAlias, hypervisorType, zoneId);
        if (certificateVO == null) {
            throw new CloudRuntimeException("Certificate alias " + certificateAlias + " does not exist");
        }

        List<DirectDownloadCertificateHostMapVO> maps = null;
        if (hostId == null) {
             maps = directDownloadCertificateHostMapDao.listByCertificateId(certificateVO.getId());
        } else {
            DirectDownloadCertificateHostMapVO hostMap = directDownloadCertificateHostMapDao.findByCertificateAndHost(certificateVO.getId(), hostId);
            if (hostMap == null) {
                s_logger.info("Certificate " + certificateAlias + " cannot be revoked from host " + hostId + " as it is not available on the host");
                return false;
            }
            maps = Collections.singletonList(hostMap);
        }

        s_logger.info("Attempting to revoke certificate alias: " + certificateAlias + " from " + maps.size() + " hosts");
        if (CollectionUtils.isNotEmpty(maps)) {
            for (DirectDownloadCertificateHostMapVO map : maps) {
                Long mappingHostId = map.getHostId();
                if (!revokeCertificateAliasFromHost(certificateAlias, mappingHostId)) {
                    String msg = "Could not revoke certificate from host: " + mappingHostId;
                    s_logger.error(msg);
                    throw new CloudRuntimeException(msg);
                }
                s_logger.info("Certificate " + certificateAlias + " revoked from host " + mappingHostId);
                map.setRevoked(true);
                directDownloadCertificateHostMapDao.update(map.getId(), map);
            }
        }
        return true;
    }

    protected boolean revokeCertificateAliasFromHost(String alias, Long hostId) {
        RevokeDirectDownloadCertificateCommand cmd = new RevokeDirectDownloadCertificateCommand(alias);
        try {
            Answer answer = agentManager.send(hostId, cmd);
            return answer != null && answer.getResult();
        } catch (AgentUnavailableException | OperationTimedoutException e) {
            s_logger.error("Error revoking certificate " + alias + " from host " + hostId, e);
        }
        return false;
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        executorService = new ScheduledThreadPoolExecutor(1, new NamedThreadFactory("DirectDownloadCertificateMonitor"));
        return true;
    }

    @Override
    public boolean stop() {
        executorService.shutdownNow();
        return true;
    }

    @Override
    public boolean start() {
        if (DirectDownloadCertificateUploadInterval.value() > 0L) {
            executorService.scheduleWithFixedDelay(
                    new DirectDownloadCertificateUploadBackgroundTask(this, hostDao, dataCenterDao,
                            directDownloadCertificateDao, directDownloadCertificateHostMapDao),
                    60L, DirectDownloadCertificateUploadInterval.value(), TimeUnit.HOURS);
        }
        return true;
    }

    @Override
    public String getConfigComponentName() {
        return DirectDownloadManager.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[]{
                DirectDownloadCertificateUploadInterval,
                DirectDownloadConnectTimeout,
                DirectDownloadSocketTimeout,
                DirectDownloadConnectionRequestTimeout
        };
    }

    public static final class DirectDownloadCertificateUploadBackgroundTask extends ManagedContextRunnable {

        private DirectDownloadManager directDownloadManager;
        private HostDao hostDao;
        private DirectDownloadCertificateDao directDownloadCertificateDao;
        private DirectDownloadCertificateHostMapDao directDownloadCertificateHostMapDao;
        private DataCenterDao dataCenterDao;

        public DirectDownloadCertificateUploadBackgroundTask(
                final DirectDownloadManager manager,
                final HostDao hostDao,
                final DataCenterDao dataCenterDao,
                final DirectDownloadCertificateDao directDownloadCertificateDao,
                final DirectDownloadCertificateHostMapDao directDownloadCertificateHostMapDao) {
            this.directDownloadManager = manager;
            this.hostDao = hostDao;
            this.dataCenterDao = dataCenterDao;
            this.directDownloadCertificateDao = directDownloadCertificateDao;
            this.directDownloadCertificateHostMapDao = directDownloadCertificateHostMapDao;
        }

        @Override
        protected void runInContext() {
            try {
                if (s_logger.isTraceEnabled()) {
                    s_logger.trace("Direct Download Manager background task is running...");
                }
                final DateTime now = DateTime.now(DateTimeZone.UTC);
                List<DataCenterVO> enabledZones = dataCenterDao.listEnabledZones();
                for (DataCenterVO zone : enabledZones) {
                    List<DirectDownloadCertificateVO> zoneCertificates = directDownloadCertificateDao.listByZone(zone.getId());
                    if (CollectionUtils.isNotEmpty(zoneCertificates)) {
                        for (DirectDownloadCertificateVO certificateVO : zoneCertificates) {
                            List<HostVO> hostsToUpload = hostDao.listAllHostsUpByZoneAndHypervisor(certificateVO.getZoneId(), certificateVO.getHypervisorType());
                            if (CollectionUtils.isNotEmpty(hostsToUpload)) {
                                for (HostVO hostVO : hostsToUpload) {
                                    DirectDownloadCertificateHostMapVO mapping = directDownloadCertificateHostMapDao.findByCertificateAndHost(certificateVO.getId(), hostVO.getId());
                                    if (mapping == null) {
                                        s_logger.debug("Certificate " + certificateVO.getId() +
                                                " (" + certificateVO.getAlias() + ") was not uploaded to host: " + hostVO.getId() +
                                                " uploading it");
                                        boolean result = directDownloadManager.uploadCertificate(certificateVO.getId(), hostVO.getId());
                                        s_logger.debug("Certificate " + certificateVO.getAlias() + " " +
                                                (result ? "uploaded" : "could not be uploaded") +
                                                " to host " + hostVO.getId());
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (final Throwable t) {
                s_logger.error("Error trying to run Direct Download background task", t);
            }
        }
    }
}
