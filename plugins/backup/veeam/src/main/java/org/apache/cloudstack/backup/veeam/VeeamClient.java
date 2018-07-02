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

package org.apache.cloudstack.backup.veeam;

import static org.apache.cloudstack.backup.VeeamBackupProvider.BACKUP_IDENTIFIER;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;

import com.cloud.utils.Pair;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.backup.BackupPolicy;
import org.apache.cloudstack.backup.VMBackup;
import org.apache.cloudstack.backup.veeam.api.BackupJobCloneInfo;
import org.apache.cloudstack.backup.veeam.api.CreateObjectInJobSpec;
import org.apache.cloudstack.backup.veeam.api.EntityReferences;
import org.apache.cloudstack.backup.veeam.api.HierarchyItem;
import org.apache.cloudstack.backup.veeam.api.HierarchyItems;
import org.apache.cloudstack.backup.veeam.api.Job;
import org.apache.cloudstack.backup.veeam.api.JobCloneSpec;
import org.apache.cloudstack.backup.veeam.api.Link;
import org.apache.cloudstack.backup.veeam.api.ObjectInJob;
import org.apache.cloudstack.backup.veeam.api.ObjectsInJob;
import org.apache.cloudstack.backup.veeam.api.Ref;
import org.apache.cloudstack.backup.veeam.api.RestoreSession;
import org.apache.cloudstack.backup.veeam.api.Task;
import org.apache.cloudstack.utils.security.SSLUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CookieStore;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.AuthSchemes;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.log4j.Logger;

import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.nio.TrustAllManager;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;

import io.cloudsoft.winrm4j.winrm.WinRmTool;
import io.cloudsoft.winrm4j.winrm.WinRmToolResponse;

public class VeeamClient {
    private static final Logger LOG = Logger.getLogger(VeeamClient.class);

    private final URI apiURI;

    private final HttpClient httpClient;
    private final HttpClientContext httpContext = HttpClientContext.create();
    private final CookieStore httpCookieStore = new BasicCookieStore();
    private final WinRmTool winRmTool;

    public VeeamClient(final String url, final String username, final String password, final boolean validateCertificate, final int timeout) throws URISyntaxException, NoSuchAlgorithmException, KeyManagementException {
        this.apiURI = new URI(url);

        final CredentialsProvider provider = new BasicCredentialsProvider();
        provider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));
        final HttpHost adminHost = new HttpHost(this.apiURI.getHost(), this.apiURI.getPort(), this.apiURI.getScheme());
        final AuthCache authCache = new BasicAuthCache();
        authCache.put(adminHost, new BasicScheme());

        this.httpContext.setCredentialsProvider(provider);
        this.httpContext.setAuthCache(authCache);

        final RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(timeout * 1000)
                .setConnectionRequestTimeout(timeout * 1000)
                .setSocketTimeout(timeout * 1000)
                .build();

        if (!validateCertificate) {
            final SSLContext sslcontext = SSLUtils.getSSLContext();
            sslcontext.init(null, new X509TrustManager[]{new TrustAllManager()}, new SecureRandom());
            final SSLConnectionSocketFactory factory = new SSLConnectionSocketFactory(sslcontext, NoopHostnameVerifier.INSTANCE);
            this.httpClient = HttpClientBuilder.create()
                    .setDefaultCredentialsProvider(provider)
                    .setDefaultCookieStore(httpCookieStore)
                    .setDefaultRequestConfig(config)
                    .setSSLSocketFactory(factory)
                    .build();
        } else {
            this.httpClient = HttpClientBuilder.create()
                    .setDefaultCredentialsProvider(provider)
                    .setDefaultCookieStore(httpCookieStore)
                    .setDefaultRequestConfig(config)
                    .build();
        }

        try {
            final HttpResponse response = post("/sessionMngr/", null);
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_CREATED) {
                throw new CloudRuntimeException("Failed to create and authenticate Veeam API client, please check the settings.");
            }
        } catch (final IOException e) {
            throw new CloudRuntimeException("Failed to authenticate Veeam API service due to:" + e.getMessage());
        }

        final WinRmTool.Builder builder = WinRmTool.Builder.builder(apiURI.getHost(), username, password);
        builder.useHttps(true);
        builder.disableCertificateChecks(true);
        builder.setAuthenticationScheme(AuthSchemes.NTLM);
        builder.port(WinRmTool.DEFAULT_WINRM_HTTPS_PORT);
        winRmTool = builder.build();
        winRmTool.setOperationTimeout(timeout * 1000L);
        winRmTool.setRetriesForConnectionFailures(1);
    }

    private void checkAuthFailure(final HttpResponse response) {
        if (response != null && response.getStatusLine().getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
            final Credentials credentials = httpContext.getCredentialsProvider().getCredentials(AuthScope.ANY);
            LOG.error("Veeam API authentication failed, please check Veeam configuration. Admin auth principal=" + credentials.getUserPrincipal() + ", password=" + credentials.getPassword() + ", API url=" + apiURI.toString());
            throw new ServerApiException(ApiErrorCode.UNAUTHORIZED, "Veeam B&R API call unauthorized, please ask your administrator to fix integration issues.");
        }
    }

    private void checkResponseOK(final HttpResponse response) {
        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_NO_CONTENT) {
            LOG.debug("Requested Veeam resource does not exist");
            return;
        }
        if (!(response.getStatusLine().getStatusCode() == HttpStatus.SC_OK ||
                response.getStatusLine().getStatusCode() == HttpStatus.SC_ACCEPTED) &&
                response.getStatusLine().getStatusCode() != HttpStatus.SC_NO_CONTENT) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to get valid response from Veeam B&R API call, please ask your administrator to diagnose and fix issues.");
        }
    }

    private void checkResponseTimeOut(final Exception e) {
        if (e instanceof ConnectTimeoutException || e instanceof SocketTimeoutException) {
            throw new ServerApiException(ApiErrorCode.RESOURCE_UNAVAILABLE_ERROR, "Veeam API operation timed out, please try again.");
        }
    }

    private HttpResponse get(final String path) throws IOException {
        final HttpGet request = new HttpGet(apiURI.toString() + path);
        final HttpResponse response = httpClient.execute(request, httpContext);
        checkAuthFailure(response);
        return response;
    }

    private HttpResponse post(final String path, final Object obj) throws IOException {
        String xml = null;
        if (obj != null) {
            XmlMapper xmlMapper = new XmlMapper();
            xml = xmlMapper.writer()
                    .with(ToXmlGenerator.Feature.WRITE_XML_DECLARATION)
                    .writeValueAsString(obj);
            // Remove invalid/empty xmlns
            xml = xml.replace(" xmlns=\"\"", "");
        }

        final HttpPost request = new HttpPost(apiURI.toString() + path);
        request.setHeader("Content-type", "application/xml");
        if (StringUtils.isNotBlank(xml)) {
            request.setEntity(new StringEntity(xml));
        }

        final HttpResponse response = httpClient.execute(request, httpContext);
        checkAuthFailure(response);
        return response;
    }

    private HttpResponse delete(final String path) throws IOException {
        final HttpResponse response = httpClient.execute(new HttpDelete(apiURI.toString() + path), httpContext);
        checkAuthFailure(response);
        return response;
    }

    ///////////////////////////////////////////////////////////////////
    //////////////// Private Veeam Helper Methods /////////////////////
    ///////////////////////////////////////////////////////////////////

    private String findDCHierarchy(final String vmwareDcName) {
        LOG.debug("Trying to find hierarchy ID for vmware datacenter: " + vmwareDcName);

        try {
            final HttpResponse response = get("/hierarchyRoots");
            checkResponseOK(response);
            final ObjectMapper objectMapper = new XmlMapper();
            final EntityReferences references = objectMapper.readValue(response.getEntity().getContent(), EntityReferences.class);
            for (final Ref ref : references.getRefs()) {
                if (ref.getName().equals(vmwareDcName) && ref.getType().equals("HierarchyRootReference")) {
                    return ref.getUid();
                }
            }
        } catch (final IOException e) {
            LOG.error("Failed to list Veeam jobs due to:", e);
            checkResponseTimeOut(e);
        }
        throw new CloudRuntimeException("Failed to find hierarchy reference for VMware datacenter " + vmwareDcName + " in Veeam, please ask administrator to check Veeam B&R manager configuration");
    }

    private String lookupVM(final String hierarchyId, final String vmName) {
        LOG.debug("Trying to lookup VM from veeam hierarchy:" + hierarchyId + " for vm name:" + vmName);

        try {
            final HttpResponse response = get(String.format("/lookup?host=%s&type=Vm&name=%s", hierarchyId, vmName));
            checkResponseOK(response);
            final ObjectMapper objectMapper = new XmlMapper();
            final HierarchyItems items = objectMapper.readValue(response.getEntity().getContent(), HierarchyItems.class);
            if (items == null || items.getItems() == null || items.getItems().isEmpty()) {
                throw new CloudRuntimeException("Could not find VM " + vmName + " in Veeam, please ask administrator to check Veeam B&R manager");
            }
            for (final HierarchyItem item : items.getItems()) {
                if (item.getObjectName().equals(vmName) && item.getObjectType().equals("Vm")) {
                    return item.getObjectRef();
                }
            }
        } catch (final IOException e) {
            LOG.error("Failed to list Veeam jobs due to:", e);
            checkResponseTimeOut(e);
        }
        throw new CloudRuntimeException("Failed to lookup VM " + vmName + " in Veeam, please ask administrator to check Veeam B&R manager configuration");
    }

    private Task parseTaskResponse(HttpResponse response) throws IOException {
        checkResponseOK(response);
        final ObjectMapper objectMapper = new XmlMapper();
        return objectMapper.readValue(response.getEntity().getContent(), Task.class);
    }

    private RestoreSession parseRestoreSessionResponse(HttpResponse response) throws IOException {
        checkResponseOK(response);
        final ObjectMapper objectMapper = new XmlMapper();
        return objectMapper.readValue(response.getEntity().getContent(), RestoreSession.class);
    }

    private boolean checkTaskStatus(final HttpResponse response) throws IOException {
        final Task task = parseTaskResponse(response);
        for (int i = 0; i < 20; i++) {
            final HttpResponse taskResponse = get("/tasks/" + task.getTaskId());
            final Task polledTask = parseTaskResponse(taskResponse);
            if (polledTask.getState().equals("Finished")) {
                final HttpResponse taskDeleteResponse = delete("/tasks/" + task.getTaskId());
                if (taskDeleteResponse.getStatusLine().getStatusCode() != HttpStatus.SC_NO_CONTENT) {
                    LOG.warn("Operation failed for veeam task id=" + task.getTaskId());
                }
                if (polledTask.getResult().getSuccess().equals("true")) {
                    Pair<String, String> pair = getRelatedLinkPair(polledTask.getLink());
                    if (pair != null) {
                        String url = pair.first();
                        String type = pair.second();
                        String path = url.replace(apiURI.toString(), "");
                        if (type.equals("RestoreSession")) {
                            for (int j = 0; j < 60; j++) {
                                HttpResponse relatedResponse = get(path);
                                RestoreSession session = parseRestoreSessionResponse(relatedResponse);
                                if (session.getResult().equals("Success")) {
                                    return true;
                                }
                                try {
                                    Thread.sleep(5000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                            throw new CloudRuntimeException("Related job type: " + type + " was not successful");
                        }
                    }
                    return true;
                }
                throw new CloudRuntimeException("Failed to assign VM to backup policy due to: " + polledTask.getResult().getMessage());
            }
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                LOG.debug("Failed to sleep while polling for Veeam task status due to: ", e);
            }
        }
        return false;
    }

    private Pair<String, String> getRelatedLinkPair(List<Link> links) {
        for (Link link : links) {
            if (link.getRel().equals("Related")) {
                return new Pair<>(link.getHref(), link.getType());
            }
        }
        return null;
    }

    ////////////////////////////////////////////////////////
    //////////////// Public Veeam APIs /////////////////////
    ////////////////////////////////////////////////////////

    public Ref listBackupRepository(final String backupServerId) {
        LOG.debug("Trying to list backup repository for backup server id: " + backupServerId);
        try {
            final HttpResponse response = get(String.format("/backupServers/%s/repositories", backupServerId));
            checkResponseOK(response);
            final ObjectMapper objectMapper = new XmlMapper();
            final EntityReferences references = objectMapper.readValue(response.getEntity().getContent(), EntityReferences.class);
            for (final Ref ref : references.getRefs()) {
                if (ref.getType().equals("RepositoryReference")) {
                    return ref;
                }
            }
        } catch (final IOException e) {
            LOG.error("Failed to list Veeam jobs due to:", e);
            checkResponseTimeOut(e);
        }
        return null;
    }

    public List<VeeamBackup> listAllBackups() {
        LOG.debug("Trying to list Veeam backups");
        try {
            final HttpResponse response = get("/backups");
            checkResponseOK(response);
            final ObjectMapper objectMapper = new XmlMapper();
            final EntityReferences entityReferences = objectMapper.readValue(response.getEntity().getContent(), EntityReferences.class);
            final List<VeeamBackup> backups = new ArrayList<>();
            for (final Ref ref : entityReferences.getRefs()) {
                backups.add(new VeeamBackup(ref.getName(), ref.getUid()));
            }
            return backups;
        } catch (final IOException e) {
            LOG.error("Failed to list Veeam backups due to:", e);
            checkResponseTimeOut(e);
        }
        return new ArrayList<>();
    }

    public List<BackupPolicy> listJobs() {
        LOG.debug("Trying to list backup policies that are Veeam jobs");
        try {
            final HttpResponse response = get("/jobs");
            checkResponseOK(response);
            final ObjectMapper objectMapper = new XmlMapper();
            final EntityReferences entityReferences = objectMapper.readValue(response.getEntity().getContent(), EntityReferences.class);
            final List<BackupPolicy> policies = new ArrayList<>();
            for (final Ref ref : entityReferences.getRefs()) {
                policies.add(new VeeamBackupPolicy(ref.getName(), ref.getUid()));
            }
            return policies;
        } catch (final IOException e) {
            LOG.error("Failed to list Veeam jobs due to:", e);
            checkResponseTimeOut(e);
        }
        return new ArrayList<>();
    }

    public Job listJob(final String jobId) {
        LOG.debug("Trying to list veeam job id: " + jobId);
        try {
            final HttpResponse response = get(String.format("/jobs/%s?format=Entity",
                    jobId.replace("urn:veeam:Job:", "")));
            checkResponseOK(response);
            final ObjectMapper objectMapper = new XmlMapper();
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            return objectMapper.readValue(response.getEntity().getContent(), Job.class);
        } catch (final IOException e) {
            LOG.error("Failed to list Veeam jobs due to:", e);
            checkResponseTimeOut(e);
        } catch (final ServerApiException e) {
            LOG.error(e);
        }
        return null;
    }

    public boolean toggleJobSchedule(final String jobId) {
        LOG.debug("Trying to toggle schedule for Veeam job: " + jobId);
        try {
            final HttpResponse response = post(String.format("/jobs/%s?action=toggleScheduleEnabled", jobId), null);
            return checkTaskStatus(response);
        } catch (final IOException e) {
            LOG.error("Failed to toggle Veeam job schedule due to:", e);
            checkResponseTimeOut(e);
        }
        return false;
    }

    public boolean startBackupJob(final String jobId) {
        LOG.debug("Trying to start ad-hoc backup for Veeam job: " + jobId);
        try {
            final HttpResponse response = post(String.format("/jobs/%s?action=start", jobId), null);
            return checkTaskStatus(response);
        } catch (final IOException e) {
            LOG.error("Failed to list Veeam jobs due to:", e);
            checkResponseTimeOut(e);
        }
        return false;
    }

    public boolean cloneVeeamJob(final Job parentJob, final String clonedJobName) {
        LOG.debug("Trying to clone veeam job: " + parentJob.getUid() + " with backup uuid: " + clonedJobName);
        try {
            final Ref repositoryRef =  listBackupRepository(parentJob.getBackupServerId());
            final BackupJobCloneInfo cloneInfo = new BackupJobCloneInfo();
            cloneInfo.setJobName(clonedJobName);
            cloneInfo.setFolderName(clonedJobName);
            cloneInfo.setRepositoryUid(repositoryRef.getUid());
            final JobCloneSpec cloneSpec = new JobCloneSpec(cloneInfo);
            final HttpResponse response = post(String.format("/jobs/%s?action=clone", parentJob.getId()), cloneSpec);
            return checkTaskStatus(response);
        } catch (final IOException e) {
            LOG.error("Failed to list Veeam jobs due to:", e);
            checkResponseTimeOut(e);
        }
        return false;
    }

    public boolean addVMToVeeamJob(final String jobId, final String vmwareInstanceName, final String vmwareDcName) {
        LOG.debug("Trying to add VM to backup policy that is a veeam job: " + jobId);
        try {
            final String heirarchyId = findDCHierarchy(vmwareDcName);
            final String veeamVmRefId = lookupVM(heirarchyId, vmwareInstanceName);
            final CreateObjectInJobSpec vmToBackupJob = new CreateObjectInJobSpec();
            vmToBackupJob.setObjName(vmwareInstanceName);
            vmToBackupJob.setObjRef(veeamVmRefId);
            final HttpResponse response = post(String.format("/jobs/%s/includes", jobId), vmToBackupJob);
            return checkTaskStatus(response);
        } catch (final IOException e) {
            LOG.error("Failed to add VM to Veeam job due to:", e);
            checkResponseTimeOut(e);
        }
        throw new CloudRuntimeException("Failed to add VM to backup policy likely due to timeout, please check veeam tasks");
    }

    public boolean removeVMFromVeeamJob(final String jobId, final String vmwareInstanceName, final String vmwareDcName) {
        LOG.debug("Trying to remove VM from backup policy that is a veeam job: " + jobId);
        try {
            final String heirarchyId = findDCHierarchy(vmwareDcName);
            final String veeamVmRefId = lookupVM(heirarchyId, vmwareInstanceName);
            final HttpResponse response = get(String.format("/jobs/%s/includes", jobId));
            checkResponseOK(response);
            final ObjectMapper objectMapper = new XmlMapper();
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            final ObjectsInJob jobObjects = objectMapper.readValue(response.getEntity().getContent(), ObjectsInJob.class);
            if (jobObjects == null || jobObjects.getObjects() == null) {
                LOG.warn("No objects found in the Veeam job " + jobId);
                return false;
            }
            for (final ObjectInJob jobObject : jobObjects.getObjects()) {
                if (jobObject.getName().equals(vmwareInstanceName) && jobObject.getHierarchyObjRef().equals(veeamVmRefId)) {
                    final HttpResponse deleteResponse = delete(String.format("/jobs/%s/includes/%s", jobId, jobObject.getObjectInJobId()));
                    return checkTaskStatus(deleteResponse);
                }
            }
            LOG.warn(vmwareInstanceName + " VM was not found to be attached to veaam job (backup policy): " + jobId);
            return false;
        } catch (final IOException e) {
            LOG.error("Failed to list Veeam jobs due to:", e);
            checkResponseTimeOut(e);
        }
        return false;
    }

    public boolean restoreFullVM(final String vmwareInstanceName, final String restorePointId) {
        LOG.debug("Trying to restore full VM: " + vmwareInstanceName + " from backup");
        try {
            final HttpResponse response = post(String.format("/vmRestorePoints/%s?action=restore", restorePointId), null);
            return checkTaskStatus(response);
        } catch (final IOException e) {
            LOG.error("Failed to restore full VM due to: ", e);
            checkResponseTimeOut(e);
        }
        throw new CloudRuntimeException("Failed to restore full VM from backup");
    }

    /////////////////////////////////////////////////////////////////
    //////////////// Public Veeam PS based APIs /////////////////////
    /////////////////////////////////////////////////////////////////

    public boolean deleteJobAndBackup(final String jobName) {
        final WinRmToolResponse response = winRmTool.executePs(
            Collections.singletonList(
                String.format(
                    "Add-PSSnapin VeeamPSSnapin\n" +
                    "Get-VBRBackup -Name \"%s\" | Remove-VBRBackup -FromDisk -Confirm:$false\n" +
                    "Get-VBRJob -Name \"%s\" | Remove-VBRJob -Confirm:$false\n" +
                    "Get-VBRBackupRepository | Sync-VBRBackupRepository", jobName, jobName)
            ));
        return response.getStatusCode() == 0 && !response.getStdErr().contains("Failed to delete");
    }

    public Map<String, VMBackup.Metric> getBackupMetrics() {
        final List<String> psScript = Collections.singletonList(
            "Add-PSSnapin VeeamPSSnapin\n" +
            "$backups = Get-VBRBackup\n" +
            "foreach ($backup in $backups) {\n" +
                "$backup.JobName\n" +
                "$storageGroups = $backup.GetStorageGroups()\n" +
                "foreach ($group in $storageGroups)\n" +
                "{\n" +
                "    $usedSize = 0\n" +
                "    $dataSize = 0\n" +
                "    $sizePerStorage = $group.GetStorages().Stats.BackupSize\n" +
                "    $dataPerStorage = $group.GetStorages().Stats.DataSize\n" +
                "    foreach ($size in $sizePerStorage)\n" +
                "    {\n" +
                "        $usedSize += $size\n" +
                "    }\n" +
                "    foreach ($size in $dataPerStorage)\n" +
                "    {\n" +
                "        $dataSize += $size\n" +
                "    }\n" +
                "    $usedSize\n" +
                "    $dataSize\n" +
                "}\n" +
                "echo \";\"" +
            "}\n");
        final WinRmToolResponse response = winRmTool.executePs(psScript);
        final Map<String, VMBackup.Metric> sizes = new HashMap<>();
        for (final String block : response.getStdOut().split(";\r\n")) {
            final String[] parts = block.split("\r\n");
            if (parts.length != 3) {
                continue;
            }
            final String backupName = parts[0];
            if (backupName != null && backupName.contains(BACKUP_IDENTIFIER)) {
                final String[] names = backupName.split(BACKUP_IDENTIFIER);
                sizes.put(names[names.length - 1], new VMBackup.Metric(Long.valueOf(parts[1]), Long.valueOf(parts[2])));
            }
        }
        return sizes;
    }

    private VMBackup.RestorePoint getRestorePointFromBlock(String[] parts) {
        String id = null;
        String created = null;
        String type = null;
        for (String part : parts) {
            if (part.matches("Id(\\s)+:(.)*")) {
                String[] split = part.split(":");
                id = split[1];
            } else if (part.matches("CreationTime(\\s)+:(.)*")) {
                String [] split = part.split(":", 2);
                created = split[1].trim();
            } else if (part.matches("Type(\\s)+:(.)*")) {
                String [] split = part.split(":");
                type = split[1].trim();
            }
        }
        return new VMBackup.RestorePoint(id, created, type);
    }

    public List<VMBackup.RestorePoint> listRestorePoints(String backupName, String vmInternalName) {
        final List<String> psScript = Collections.singletonList(
                String.format(
                        "Add-PSSnapin VeeamPSSnapin\n" +
                                "$backup = Get-VBRBackup -Name \"%s\"\n" +
                                "Get-VBRRestorePoint -Backup:$backup -Name \"%s\"",
                        backupName, vmInternalName)
        );
        final WinRmToolResponse response = winRmTool.executePs(psScript);
        if (response == null) {
            throw new CloudRuntimeException("Failed to list restore points");
        }
        final List<VMBackup.RestorePoint> restorePoints = new ArrayList<>();
        for (final String block : response.getStdOut().split("\r\n\r\n")) {
            if (block.isEmpty()) {
                continue;
            }
            final String[] parts = block.split("\r\n");
            restorePoints.add(getRestorePointFromBlock(parts));
        }
        return restorePoints;
    }

    public Pair<Boolean, String> restoreVMToDifferentLocation(String restorePointId, String hostIp, String dataStoreUuid) {
        final String restoreLocation = "CS-RSTR-" + UUID.randomUUID().toString();
        final String datastoreId = dataStoreUuid.replace("-","");
        final List<String> psScript = Collections.singletonList(
                String.format(
                        "Add-PSSnapin VeeamPSSnapin\n" +
                                "$point = Get-VBRRestorePoint | where {$_.Id -eq \"%s\"}\n" +
                                "$server = Get-VBRServer -Name \"%s\"\n" +
                                "$ds = Find-VBRViDatastore -Server:$server -Name \"%s\"\n" +
                                "Start-VBRRestoreVM -RestorePoint:$point -Server:$server -Datastore:$ds -VMName \"%s\"",
                        restorePointId, hostIp, datastoreId, restoreLocation)
        );
        final WinRmToolResponse response = winRmTool.executePs(psScript);
        if (response == null) {
            throw new CloudRuntimeException("Failed to restore VM to location " + restoreLocation);
        }
        return new Pair<>(response.getStatusCode() == 0, restoreLocation);
    }
}
