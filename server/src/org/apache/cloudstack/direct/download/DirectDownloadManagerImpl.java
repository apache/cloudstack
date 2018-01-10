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

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.VMTemplateStoragePoolVO;
import com.cloud.storage.VMTemplateStorageResourceAssoc;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VMTemplatePoolDao;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.exception.CloudRuntimeException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.inject.Inject;

import org.apache.cloudstack.agent.directdownload.DirectDownloadCommand;
import org.apache.cloudstack.agent.directdownload.DirectDownloadAnswer;
import org.apache.cloudstack.agent.directdownload.DirectDownloadCommand.DownloadProtocol;
import org.apache.cloudstack.agent.directdownload.HttpDirectDownloadCommand;
import org.apache.cloudstack.agent.directdownload.MetalinkDirectDownloadCommand;
import org.apache.cloudstack.agent.directdownload.NfsDirectDownloadCommand;
import org.apache.cloudstack.agent.directdownload.HttpsDirectDownloadCommand;
import org.apache.cloudstack.agent.directdownload.SetupDirectDownloadCertificate;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStore;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.to.PrimaryDataStoreTO;
import org.apache.commons.collections.MapUtils;
import org.apache.log4j.Logger;

public class DirectDownloadManagerImpl extends ManagerBase implements DirectDownloadManager {

    private static final Logger s_logger = Logger.getLogger(DirectDownloadManagerImpl.class);
    protected static final String httpHeaderDetailKey = "HTTP_HEADER";
    protected static final String BEGIN_CERT = "-----BEGIN CERTIFICATE-----";
    protected static final String END_CERT = "-----END CERTIFICATE-----";
    protected final static String LINE_SEPARATOR = "\n";

    @Inject
    VMTemplateDao vmTemplateDao;
    @Inject
    PrimaryDataStoreDao primaryDataStoreDao;
    @Inject
    HostDao hostDao;
    @Inject
    AgentManager agentManager;
    @Inject
    VMTemplatePoolDao vmTemplatePoolDao;
    @Inject
    DataStoreManager dataStoreManager;

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
        Answer answer = agentManager.easySend(hostId, cmd);
        if (answer == null || !answer.getResult()) {
            throw new CloudRuntimeException("Host " + hostId + " could not download template " +
                    templateId + " on pool " + poolId);
        }

        VMTemplateStoragePoolVO sPoolRef = vmTemplatePoolDao.findByPoolTemplate(poolId, templateId);
        if (sPoolRef == null) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Not found (templateId:" + templateId + " poolId: " + poolId + ") in template_spool_ref, persisting it");
            }
            DirectDownloadAnswer ans = (DirectDownloadAnswer) answer;
            sPoolRef = new VMTemplateStoragePoolVO(poolId, templateId);
            sPoolRef.setDownloadPercent(100);
            sPoolRef.setDownloadState(VMTemplateStorageResourceAssoc.Status.DOWNLOADED);
            sPoolRef.setState(ObjectInDataStoreStateMachine.State.Ready);
            sPoolRef.setTemplateSize(ans.getTemplateSize());
            sPoolRef.setLocalDownloadPath(ans.getInstallPath());
            sPoolRef.setInstallPath(ans.getInstallPath());
            vmTemplatePoolDao.persist(sPoolRef);
        }
    }

    /**
     * Return DirectDownloadCommand according to the protocol
     * @param protocol
     * @param url
     * @param templateId
     * @param destPool
     * @return
     */
    private DirectDownloadCommand getDirectDownloadCommandFromProtocol(DownloadProtocol protocol, String url, Long templateId, PrimaryDataStoreTO destPool,
                                                                       String checksum, Map<String, String> httpHeaders) {
        if (protocol.equals(DownloadProtocol.HTTP)) {
            return new HttpDirectDownloadCommand(url, templateId, destPool, checksum, httpHeaders);
        } else if (protocol.equals(DownloadProtocol.HTTPS)) {
            return new HttpsDirectDownloadCommand(url, templateId, destPool, checksum, httpHeaders);
        } else if (protocol.equals(DownloadProtocol.NFS)) {
            return new NfsDirectDownloadCommand(url, templateId, destPool, checksum);
        } else if (protocol.equals(DownloadProtocol.METALINK)) {
            return new MetalinkDirectDownloadCommand(url, templateId, destPool, checksum);
        } else {
            return null;
        }
    }

    @Override
    public boolean uploadCertificateToHosts(String certificateCer, String certificateName) {
        List<HostVO> hosts = hostDao.listAllHostsByType(Host.Type.Routing)
                .stream()
                .filter(x -> x.getStatus().equals(Status.Up) &&
                            x.getHypervisorType().equals(Hypervisor.HypervisorType.KVM))
                .collect(Collectors.toList());
        for (HostVO host : hosts) {
            if (!uploadCertificate(certificateCer, certificateName, host.getId())) {
                throw new CloudRuntimeException("Uploading certificate " + certificateName + " failed on host: " + host.getId());
            }
        }
        return true;
    }

    /**
     * Upload and import certificate to hostId on keystore
     */
    protected boolean uploadCertificate(String certificate, String certificateName, long hostId) {
        String cert = certificate.replaceAll("(.{64})", "$1\n");
        final String prettified_cert = BEGIN_CERT + LINE_SEPARATOR + cert + LINE_SEPARATOR + END_CERT;
        SetupDirectDownloadCertificate cmd = new SetupDirectDownloadCertificate(prettified_cert, certificateName);
        Answer answer = agentManager.easySend(hostId, cmd);
        if (answer == null || !answer.getResult()) {
            return false;
        }
        s_logger.info("Certificate " + certificateName + " successfully uploaded to host: " + hostId);
        return true;
    }
}
