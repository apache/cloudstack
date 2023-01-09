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
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.UUID;

import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;

import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.backup.Backup;
import org.apache.cloudstack.backup.BackupOffering;
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
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.log4j.Logger;

import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.nio.TrustAllManager;
import com.cloud.utils.ssh.SshHelper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;
import org.apache.commons.lang3.StringUtils;

public class VeeamClient {
    private static final Logger LOG = Logger.getLogger(VeeamClient.class);
    private static final String FAILED_TO_DELETE = "Failed to delete";

    private final URI apiURI;

    private final HttpClient httpClient;
    private static final String RESTORE_VM_SUFFIX = "CS-RSTR-";
    private static final String SESSION_HEADER = "X-RestSvcSessionId";

    private String veeamServerIp;
    private String veeamServerUsername;
    private String veeamServerPassword;
    private String veeamSessionId = null;
    private int restoreTimeout;
    private final int veeamServerPort = 22;

    public VeeamClient(final String url, final String username, final String password, final boolean validateCertificate, final int timeout,
            final int restoreTimeout) throws URISyntaxException, NoSuchAlgorithmException, KeyManagementException {
        this.apiURI = new URI(url);
        this.restoreTimeout = restoreTimeout;

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
                    .setDefaultRequestConfig(config)
                    .setSSLSocketFactory(factory)
                    .build();
        } else {
            this.httpClient = HttpClientBuilder.create()
                    .setDefaultRequestConfig(config)
                    .build();
        }

        authenticate(username, password);
        setVeeamSshCredentials(this.apiURI.getHost(), username, password);
    }

    protected void setVeeamSshCredentials(String hostIp, String username, String password) {
        this.veeamServerIp = hostIp;
        this.veeamServerUsername = username;
        this.veeamServerPassword = password;
    }

    private void authenticate(final String username, final String password) {
        // https://helpcenter.veeam.com/docs/backup/rest/http_authentication.html?ver=95u4
        final HttpPost request = new HttpPost(apiURI.toString() + "/sessionMngr/?v=v1_4");
        request.setHeader(HttpHeaders.AUTHORIZATION, "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes()));
        try {
            final HttpResponse response = httpClient.execute(request);
            checkAuthFailure(response);
            veeamSessionId = response.getFirstHeader(SESSION_HEADER).getValue();
            if (StringUtils.isEmpty(veeamSessionId)) {
                throw new CloudRuntimeException("Veeam Session ID is not available to perform API requests");
            }
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_CREATED) {
                throw new CloudRuntimeException("Failed to create and authenticate Veeam API client, please check the settings.");
            }
        } catch (final IOException e) {
            throw new CloudRuntimeException("Failed to authenticate Veeam API service due to:" + e.getMessage());
        }
    }

    private void checkAuthFailure(final HttpResponse response) {
        if (response != null && response.getStatusLine().getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
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
            LOG.debug(String.format("HTTP request failed, status code is [%s], response is: [%s].", response.getStatusLine().getStatusCode(), response.toString()));
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Got invalid API status code returned by the Veeam server");
        }
    }

    private void checkResponseTimeOut(final Exception e) {
        if (e instanceof ConnectTimeoutException || e instanceof SocketTimeoutException) {
            throw new ServerApiException(ApiErrorCode.RESOURCE_UNAVAILABLE_ERROR, "Veeam API operation timed out, please try again.");
        }
    }

    protected HttpResponse get(final String path) throws IOException {
        String url = apiURI.toString() + path;
        final HttpGet request = new HttpGet(url);
        request.setHeader(SESSION_HEADER, veeamSessionId);
        final HttpResponse response = httpClient.execute(request);
        checkAuthFailure(response);

        LOG.debug(String.format("Response received in GET request is: [%s] for URL: [%s].", response.toString(), url));
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

        String url = apiURI.toString() + path;
        final HttpPost request = new HttpPost(url);
        request.setHeader(SESSION_HEADER, veeamSessionId);
        request.setHeader("Content-type", "application/xml");
        if (StringUtils.isNotBlank(xml)) {
            request.setEntity(new StringEntity(xml));
        }

        final HttpResponse response = httpClient.execute(request);
        checkAuthFailure(response);

        LOG.debug(String.format("Response received in POST request with body [%s] is: [%s] for URL [%s].", xml, response.toString(), url));
        return response;
    }

    private HttpResponse delete(final String path) throws IOException {
        String url = apiURI.toString() + path;
        final HttpDelete request = new HttpDelete(url);
        request.setHeader(SESSION_HEADER, veeamSessionId);
        final HttpResponse response = httpClient.execute(request);
        checkAuthFailure(response);

        LOG.debug(String.format("Response received in DELETE request is: [%s] for URL [%s].", response.toString(), url));
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

    protected RestoreSession parseRestoreSessionResponse(HttpResponse response) throws IOException {
        checkResponseOK(response);
        final ObjectMapper objectMapper = new XmlMapper();
        return objectMapper.readValue(response.getEntity().getContent(), RestoreSession.class);
    }

    private boolean checkTaskStatus(final HttpResponse response) throws IOException {
        final Task task = parseTaskResponse(response);
        for (int i = 0; i < 120; i++) {
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
                            return checkIfRestoreSessionFinished(type, path);
                        }
                    }
                    return true;
                }
                throw new CloudRuntimeException("Failed to assign VM to backup offering due to: " + polledTask.getResult().getMessage());
            }
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                LOG.debug("Failed to sleep while polling for Veeam task status due to: ", e);
            }
        }
        return false;
    }

    protected boolean checkIfRestoreSessionFinished(String type, String path) throws IOException {
        for (int j = 0; j < this.restoreTimeout; j++) {
            HttpResponse relatedResponse = get(path);
            RestoreSession session = parseRestoreSessionResponse(relatedResponse);
            if (session.getResult().equals("Success")) {
                return true;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
                LOG.trace(String.format("Ignoring InterruptedException [%s] when waiting for restore session finishes.", ignored.getMessage()));
            }
        }
        throw new CloudRuntimeException("Related job type: " + type + " was not successful");
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

    public Ref listBackupRepository(final String backupServerId, final String backupName) {
        LOG.debug(String.format("Trying to list backup repository for backup job [name: %s] in server [id: %s].", backupName, backupServerId));
        try {
            String repositoryName = getRepositoryNameFromJob(backupName);
            final HttpResponse response = get(String.format("/backupServers/%s/repositories", backupServerId));
            checkResponseOK(response);
            final ObjectMapper objectMapper = new XmlMapper();
            final EntityReferences references = objectMapper.readValue(response.getEntity().getContent(), EntityReferences.class);
            for (final Ref ref : references.getRefs()) {
                if (ref.getType().equals("RepositoryReference") && ref.getName().equals(repositoryName)) {
                    return ref;
                }
            }
        } catch (final IOException e) {
            LOG.error(String.format("Failed to list Veeam backup repository used by backup job [name: %s] due to: [%s].", backupName, e.getMessage()), e);
            checkResponseTimeOut(e);
        }
        return null;
    }

    protected String getRepositoryNameFromJob(String backupName) {
        final List<String> cmds = Arrays.asList(
                String.format("$Job = Get-VBRJob -name \"%s\"", backupName),
                "$Job.GetBackupTargetRepository() ^| select Name ^| Format-List"
        );
        Pair<Boolean, String> result = executePowerShellCommands(cmds);
        if (result == null || !result.first()) {
            throw new CloudRuntimeException(String.format("Failed to get Repository Name from Job [name: %s].", backupName));
        }

        for (String block : result.second().split("\n\n")) {
           if (block.matches("Name(\\s)+:(.)*")) {
               return block.split(":")[1].trim();
           }
        }
        throw new CloudRuntimeException(String.format("Can't find any repository name for Job [name: %s].", backupName));
    }

    public void listAllBackups() {
        LOG.debug("Trying to list Veeam backups");
        try {
            final HttpResponse response = get("/backups");
            checkResponseOK(response);
            final ObjectMapper objectMapper = new XmlMapper();
            final EntityReferences entityReferences = objectMapper.readValue(response.getEntity().getContent(), EntityReferences.class);
            for (final Ref ref : entityReferences.getRefs()) {
                LOG.debug("Veeam Backup found, name: " + ref.getName() + ", uid: " + ref.getUid() + ", type: " + ref.getType());
            }
        } catch (final IOException e) {
            LOG.error("Failed to list Veeam backups due to:", e);
            checkResponseTimeOut(e);
        }
    }

    public List<BackupOffering> listJobs() {
        LOG.debug("Trying to list backup policies that are Veeam jobs");
        try {
            final HttpResponse response = get("/jobs");
            checkResponseOK(response);
            final ObjectMapper objectMapper = new XmlMapper();
            final EntityReferences entityReferences = objectMapper.readValue(response.getEntity().getContent(), EntityReferences.class);
            final List<BackupOffering> policies = new ArrayList<>();
            if (entityReferences == null || entityReferences.getRefs() == null) {
                return policies;
            }
            for (final Ref ref : entityReferences.getRefs()) {
                policies.add(new VeeamBackupOffering(ref.getName(), ref.getUid()));
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
            final Ref repositoryRef = listBackupRepository(parentJob.getBackupServerId(), parentJob.getName());
            if (repositoryRef == null) {
                throw new CloudRuntimeException(String.format("Failed to clone backup job because couldn't find any "
                        + "repository associated with backup job [id: %s, uid: %s, backupServerId: %s, name: %s].",
                        parentJob.getId(), parentJob.getUid(), parentJob.getBackupServerId(), parentJob.getName()));
            }
            final BackupJobCloneInfo cloneInfo = new BackupJobCloneInfo();
            cloneInfo.setJobName(clonedJobName);
            cloneInfo.setFolderName(clonedJobName);
            cloneInfo.setRepositoryUid(repositoryRef.getUid());
            final JobCloneSpec cloneSpec = new JobCloneSpec(cloneInfo);
            final HttpResponse response = post(String.format("/jobs/%s?action=clone", parentJob.getId()), cloneSpec);
            return checkTaskStatus(response);
        } catch (final Exception e) {
            LOG.warn("Exception caught while trying to clone Veeam job:", e);
        }
        return false;
    }

    public boolean addVMToVeeamJob(final String jobId, final String vmwareInstanceName, final String vmwareDcName) {
        LOG.debug("Trying to add VM to backup offering that is Veeam job: " + jobId);
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
        throw new CloudRuntimeException("Failed to add VM to backup offering likely due to timeout, please check Veeam tasks");
    }

    public boolean removeVMFromVeeamJob(final String jobId, final String vmwareInstanceName, final String vmwareDcName) {
        LOG.debug("Trying to remove VM from backup offering that is a Veeam job: " + jobId);
        try {
            final String hierarchyId = findDCHierarchy(vmwareDcName);
            final String veeamVmRefId = lookupVM(hierarchyId, vmwareInstanceName);
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
            LOG.warn(vmwareInstanceName + " VM was not found to be attached to Veaam job (backup offering): " + jobId);
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

    /**
     * Generate a single command to be passed through SSH
     */
    protected String transformPowerShellCommandList(List<String> cmds) {
        StringJoiner joiner = new StringJoiner(";");
        joiner.add("PowerShell Add-PSSnapin VeeamPSSnapin");
        for (String cmd : cmds) {
            joiner.add(cmd);
        }
        return joiner.toString();
    }

    /**
     * Execute a list of commands in a single call on PowerShell through SSH
     */
    protected Pair<Boolean, String> executePowerShellCommands(List<String> cmds) {
        try {
            String commands = transformPowerShellCommandList(cmds);
            Pair<Boolean, String> response = SshHelper.sshExecute(veeamServerIp, veeamServerPort,
                    veeamServerUsername, null, veeamServerPassword,
                    commands, 120000, 120000, 3600000);

            if (response == null || !response.first()) {
                LOG.error(String.format("Veeam PowerShell commands [%s] failed due to: [%s].", commands, response != null ? response.second() : "no PowerShell output returned"));
            } else {
                LOG.debug(String.format("Veeam response for PowerShell commands [%s] is: [%s].", commands, response.second()));
            }

            return response;
        } catch (Exception e) {
            throw new CloudRuntimeException("Error while executing PowerShell commands due to: " + e.getMessage());
        }
    }

    public boolean setJobSchedule(final String jobName) {
        Pair<Boolean, String> result = executePowerShellCommands(Arrays.asList(
                String.format("$job = Get-VBRJob -Name \"%s\"", jobName),
                "if ($job) { Set-VBRJobSchedule -Job $job -Daily -At \"11:00\" -DailyKind Weekdays }"
        ));
        return result.first() && !result.second().isEmpty() && !result.second().contains(FAILED_TO_DELETE);
    }

    public boolean deleteJobAndBackup(final String jobName) {
        Pair<Boolean, String> result = executePowerShellCommands(Arrays.asList(
                String.format("$job = Get-VBRJob -Name \"%s\"", jobName),
                "if ($job) { Remove-VBRJob -Job $job -Confirm:$false }",
                String.format("$backup = Get-VBRBackup -Name \"%s\"", jobName),
                "if ($backup) { Remove-VBRBackup -Backup $backup -FromDisk -Confirm:$false }",
                "$repo = Get-VBRBackupRepository",
                "Sync-VBRBackupRepository -Repository $repo"
        ));
        return result.first() && !result.second().contains(FAILED_TO_DELETE);
    }

    public boolean deleteBackup(final String restorePointId) {
        LOG.debug(String.format("Trying to delete restore point [name: %s].", restorePointId));
        Pair<Boolean, String> result = executePowerShellCommands(Arrays.asList(
                String.format("$restorePoint = Get-VBRRestorePoint ^| Where-Object { $_.Id -eq '%s' }", restorePointId),
                "if ($restorePoint) { Remove-VBRRestorePoint -Oib $restorePoint -Confirm:$false",
                    "$repo = Get-VBRBackupRepository",
                    "Sync-VBRBackupRepository -Repository $repo",
                "} else { ",
                    " Write-Output \"Failed to delete\"",
                    " Exit 1",
                "}"
        ));
        return result.first() && !result.second().contains(FAILED_TO_DELETE);
    }

    public Map<String, Backup.Metric> getBackupMetrics() {
        final String separator = "=====";
        final List<String> cmds = Arrays.asList(
            "$backups = Get-VBRBackup",
            "foreach ($backup in $backups) {" +
               "$backup.JobName;" +
               "$storageGroups = $backup.GetStorageGroups();" +
               "foreach ($group in $storageGroups) {" +
                    "$usedSize = 0;" +
                    "$dataSize = 0;" +
                    "$sizePerStorage = $group.GetStorages().Stats.BackupSize;" +
                    "$dataPerStorage = $group.GetStorages().Stats.DataSize;" +
                    "foreach ($size in $sizePerStorage) {" +
                        "$usedSize += $size;" +
                    "}" +
                    "foreach ($size in $dataPerStorage) {" +
                        "$dataSize += $size;" +
                    "}" +
                    "$usedSize;" +
                    "$dataSize;" +
               "}" +
               "echo \"" + separator + "\"" +
            "}"
        );
        Pair<Boolean, String> response = executePowerShellCommands(cmds);
        final Map<String, Backup.Metric> sizes = new HashMap<>();
        for (final String block : response.second().split(separator + "\r\n")) {
            final String[] parts = block.split("\r\n");
            if (parts.length != 3) {
                continue;
            }
            final String backupName = parts[0];
            if (backupName != null && backupName.contains(BACKUP_IDENTIFIER)) {
                final String[] names = backupName.split(BACKUP_IDENTIFIER);
                sizes.put(names[names.length - 1], new Backup.Metric(Long.valueOf(parts[1]), Long.valueOf(parts[2])));
            }
        }
        return sizes;
    }

    private Backup.RestorePoint getRestorePointFromBlock(String[] parts) {
        LOG.debug(String.format("Processing block of restore points: [%s].", StringUtils.join(parts, ", ")));
        String id = null;
        String created = null;
        String type = null;
        for (String part : parts) {
            if (part.matches("Id(\\s)+:(.)*")) {
                String[] split = part.split(":");
                id = split[1].trim();
            } else if (part.matches("CreationTime(\\s)+:(.)*")) {
                String [] split = part.split(":", 2);
                created = split[1].trim();
            } else if (part.matches("Type(\\s)+:(.)*")) {
                String [] split = part.split(":");
                type = split[1].trim();
            }
        }
        return new Backup.RestorePoint(id, created, type);
    }

    public List<Backup.RestorePoint> listRestorePoints(String backupName, String vmInternalName) {
        final List<String> cmds = Arrays.asList(
                String.format("$backup = Get-VBRBackup -Name \"%s\"", backupName),
                String.format("if ($backup) { $restore = (Get-VBRRestorePoint -Backup:$backup -Name \"%s\" ^| Where-Object {$_.IsConsistent -eq $true})", vmInternalName),
                "if ($restore) { $restore ^| Format-List } }"
        );
        Pair<Boolean, String> response = executePowerShellCommands(cmds);
        final List<Backup.RestorePoint> restorePoints = new ArrayList<>();
        if (response == null || !response.first()) {
            return restorePoints;
        }

        for (final String block : response.second().split("\r\n\r\n")) {
            if (block.isEmpty()) {
                continue;
            }
            LOG.debug(String.format("Found restore points from [backupName: %s, vmInternalName: %s] which is: [%s].", backupName, vmInternalName, block));
            final String[] parts = block.split("\r\n");
            restorePoints.add(getRestorePointFromBlock(parts));
        }
        return restorePoints;
    }

    public Pair<Boolean, String> restoreVMToDifferentLocation(String restorePointId, String hostIp, String dataStoreUuid) {
        final String restoreLocation = RESTORE_VM_SUFFIX + UUID.randomUUID().toString();
        final String datastoreId = dataStoreUuid.replace("-","");
        final List<String> cmds = Arrays.asList(
                "$points = Get-VBRRestorePoint",
                String.format("foreach($point in $points) { if ($point.Id -eq '%s') { break; } }", restorePointId),
                String.format("$server = Get-VBRServer -Name \"%s\"", hostIp),
                String.format("$ds = Find-VBRViDatastore -Server:$server -Name \"%s\"", datastoreId),
                String.format("$job = Start-VBRRestoreVM -RestorePoint:$point -Server:$server -Datastore:$ds -VMName \"%s\" -RunAsync", restoreLocation),
                "while (-not (Get-VBRRestoreSession -Id $job.Id).IsCompleted) { Start-Sleep -Seconds 10 }"
        );
        Pair<Boolean, String> result = executePowerShellCommands(cmds);
        if (result == null || !result.first()) {
            throw new CloudRuntimeException("Failed to restore VM to location " + restoreLocation);
        }
        return new Pair<>(result.first(), restoreLocation);
    }
}
