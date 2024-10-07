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
package org.apache.cloudstack.backup.backroll;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;

import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.backup.BackupOffering;
import org.apache.cloudstack.backup.Backup.Metric;
import org.apache.cloudstack.backup.backroll.model.BackrollBackup;
import org.apache.cloudstack.backup.backroll.model.BackrollBackupMetrics;
import org.apache.cloudstack.backup.backroll.model.BackrollOffering;
import org.apache.cloudstack.backup.backroll.model.BackrollTaskStatus;
import org.apache.cloudstack.backup.backroll.model.BackrollVmBackup;
import org.apache.cloudstack.backup.backroll.model.response.BackrollTaskRequestResponse;
import org.apache.cloudstack.backup.backroll.model.response.TaskState;
import org.apache.cloudstack.backup.backroll.model.response.TaskStateResponse;
import org.apache.cloudstack.backup.backroll.model.response.api.LoginApiResponse;
import org.apache.cloudstack.backup.backroll.model.response.archive.BackrollArchiveResponse;
import org.apache.cloudstack.backup.backroll.model.response.archive.BackrollBackupsFromVMResponse;
import org.apache.cloudstack.backup.backroll.model.response.backup.BackrollBackupStatusResponse;
import org.apache.cloudstack.backup.backroll.model.response.backup.BackrollBackupStatusSuccessResponse;
import org.apache.cloudstack.backup.backroll.model.response.metrics.backup.BackrollBackupMetricsResponse;
import org.apache.cloudstack.backup.backroll.model.response.metrics.virtualMachine.BackrollVmMetricsResponse;
import org.apache.cloudstack.backup.backroll.model.response.metrics.virtualMachine.CacheStats;
import org.apache.cloudstack.backup.backroll.model.response.metrics.virtualMachineBackups.BackupInfos;
import org.apache.cloudstack.backup.backroll.model.response.metrics.virtualMachineBackups.VirtualMachineBackupsResponse;
import org.apache.cloudstack.backup.backroll.model.response.policy.BackrollBackupPolicyResponse;
import org.apache.cloudstack.backup.backroll.model.response.policy.BackupPoliciesResponse;
import org.apache.cloudstack.utils.security.SSLUtils;

import org.apache.commons.lang3.StringUtils;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.ParseException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.joda.time.DateTime;

import org.json.JSONException;
import org.json.JSONObject;

import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.nio.TrustAllManager;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class BackrollClient {

    private Logger LOG = LogManager.getLogger(BackrollClient.class);

    private int restoreTimeout;

    private final URI apiURI;

    private final HttpClient httpClient;

    private String backrollToken = null;
    private String appname = null;
    private String password = null;

    public BackrollClient(final String url, final String appname, final String password,
            final boolean validateCertificate, final int timeout,
            final int restoreTimeout)
            throws URISyntaxException, NoSuchAlgorithmException, KeyManagementException {
        this.apiURI = new URI(url);
        this.restoreTimeout = restoreTimeout;
        this.appname = appname;
        this.password = password;

        final RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(timeout * 1000)
                .setConnectionRequestTimeout(timeout * 1000)
                .setSocketTimeout(timeout * 1000)
                .build();

        if (!validateCertificate) {
            final SSLContext sslcontext = SSLUtils.getSSLContext();
            sslcontext.init(null, new X509TrustManager[] { new TrustAllManager() }, new SecureRandom());
            final SSLConnectionSocketFactory factory = new SSLConnectionSocketFactory(sslcontext,
                    NoopHostnameVerifier.INSTANCE);
            this.httpClient = HttpClientBuilder.create()
                    .setDefaultRequestConfig(config)
                    .setSSLSocketFactory(factory)
                    .build();
        } else {
            this.httpClient = HttpClientBuilder.create()
                    .setDefaultRequestConfig(config)
                    .build();
        }

        if (StringUtils.isEmpty(backrollToken) || !isAuthenticated()) {
            login(appname, password);
        }
    }

    public String startBackupJob(final String jobId) {
        LOG.info("Trying to start backup for Backroll job: {}", jobId);

        try {
            loginIfAuthenticationFailed();
            return this.<BackrollTaskRequestResponse>parse(okBody(
                    post(String.format("/tasks/singlebackup/%s", jobId), null))).location
                    .replace("/api/v1/status/", "");
        } catch (final Exception e) {
            LOG.error("Failed to start Backroll backup job due to: {}", e.getMessage());
        }
        return null;
    }

    public String getBackupOfferingUrl() {
        LOG.info("Trying to list backroll backup policies");

        loginIfAuthenticationFailed();

        try {
            return this.<BackrollTaskRequestResponse>parse(okBody(get("/backup_policies"))).location.replace("/api/v1",
                    "");
        } catch (final Exception e) {
            LOG.error("Failed to list Backroll jobs due to: {}", e.getMessage());
        }
        return null;
    }

    public List<BackupOffering> getBackupOfferings(String idTask) {
        LOG.info("Trying to list backroll backup policies");

        loginIfAuthenticationFailed();

        try {
            BackupPoliciesResponse backupPoliciesResponse = waitGet(idTask);

            final List<BackupOffering> policies = new ArrayList<>();
            for (final BackrollBackupPolicyResponse policy : backupPoliciesResponse.backupPolicies) {
                policies.add(new BackrollOffering(policy.name, policy.id));
            }
            return policies;
        } catch (final IOException | InterruptedException e) {
            LOG.error("Failed to list Backroll jobs due to: {}", e.getMessage());
        }
        return new ArrayList<BackupOffering>();
    }

    public boolean restoreVMFromBackup(final String vmId, final String backupName) {
        LOG.info("Start restore backup with backroll with backup {} for vm {}", backupName, vmId);

        loginIfAuthenticationFailed();

        try {
            JSONObject jsonBody = new JSONObject();
            try {
                jsonBody.put("virtual_machine_id", vmId);
                jsonBody.put("backup_name", backupName);
                jsonBody.put("storage", "");
                jsonBody.put("mode", "single");

            } catch (JSONException e) {
                LOG.error("Backroll Error: {}", e.getMessage());
            }

            String urlToRequest = this
                    .<BackrollTaskRequestResponse>parse(okBody(
                            post(String.format("/tasks/restore/%s", vmId), jsonBody))).location
                    .replace("/api/v1", "");

            TaskStateResponse response = waitGet(urlToRequest);
            LOG.debug("RESTORE {}", response.state);
            return response.state.equals(TaskState.SUCCESS);
        } catch (final NotOkBodyException e) {
            return false;
        } catch (final IOException | InterruptedException e) {
            LOG.error("Ouch! Failed to restore VM with Backroll due to: {}", e.getMessage());
            throw new CloudRuntimeException("Ouch! Failed to restore VM with Backroll due to: {}" + e.getMessage());
        }
    }

    public BackrollTaskStatus checkBackupTaskStatus(String taskId) {
        LOG.info("Trying to get backup status for Backroll task: {}", taskId);

        loginIfAuthenticationFailed();

        try {
            BackrollTaskStatus status = new BackrollTaskStatus();

            String body = okBody(get("/status/" + taskId));

            if (body.contains(TaskState.FAILURE) || body.contains(TaskState.PENDING)) {
                BackrollBackupStatusResponse backupStatusRequestResponse = parse(body);
                status.setState(backupStatusRequestResponse.state);
            } else {
                BackrollBackupStatusSuccessResponse backupStatusSuccessRequestResponse = parse(body);
                status.setState(backupStatusSuccessRequestResponse.state);
                status.setInfo(backupStatusSuccessRequestResponse.info);
            }

            return status;

        } catch (final NotOkBodyException e) {
            // throw new CloudRuntimeException("Failed to retrieve backups status for this
            // VM via Backroll");
            LOG.error("Failed to retrieve backups status for this VM via Backroll");

        } catch (final IOException e) {
            LOG.error("Failed to check backups status due to: {}", e.getMessage());
        }
        return null;
    }

    public boolean deleteBackup(final String vmId, final String backupName) {
        LOG.info("Trying to delete backup {} for vm {} using Backroll", vmId, backupName);

        loginIfAuthenticationFailed();

        try {
            String urlToRequest = this.<BackrollTaskRequestResponse>parse(okBody(delete(
                    String.format("/virtualmachines/%s/backups/%s", vmId, backupName)))).location
                    .replace("/api/v1", "");

            BackrollBackupsFromVMResponse backrollBackupsFromVMResponse = waitGet(urlToRequest);
            LOG.debug(backrollBackupsFromVMResponse.state);
            return backrollBackupsFromVMResponse.state.equals(TaskState.SUCCESS);
        } catch (final NotOkBodyException e) {
            return false;
        } catch (final Exception e) {
            LOG.error("Failed to delete backup using Backroll due to: {}", e.getMessage());
        }
        return false;
    }

    public Metric getVirtualMachineMetrics(final String vmId) {
        LOG.info("Trying to retrieve virtual machine metric from Backroll for vm {}", vmId);

        loginIfAuthenticationFailed();

        Metric metric = new Metric(0L, 0L);

        try {

            String urlToRequest = this.<BackrollTaskRequestResponse>parse(okBody(
                    get(String.format("/virtualmachines/%s/repository", vmId)))).location.replace("/api/v1", "");
            LOG.debug(urlToRequest);

            BackrollVmMetricsResponse vmMetricsResponse = waitGet(urlToRequest);

            if (vmMetricsResponse != null) {
                if (vmMetricsResponse.state.equals(TaskState.SUCCESS)) {
                    LOG.debug("SUCCESS ok");
                    if (vmMetricsResponse.infos != null) {
                        if (vmMetricsResponse.infos.cache != null) {
                            if (vmMetricsResponse.infos.cache.stats != null) {
                                CacheStats stats = vmMetricsResponse.infos.cache.stats;
                                long size = Long.parseLong(stats.totalSize);
                                return new Metric(size, size);
                            }
                        }
                    }
                }
            }
        } catch (final Exception e) {
            LOG.error("Failed to retrieve virtual machine metrics with Backroll due to: {}", e.getMessage());
        }

        return metric;
    }

    public BackrollBackupMetrics getBackupMetrics(String vmId, String backupId) {
        LOG.info("Trying to get backup metrics for VM: {}, and backup: {}", vmId, backupId);

        loginIfAuthenticationFailed();

        try {

            String urlToRequest = this.<BackrollTaskRequestResponse>parse(okBody(get(
                    String.format("/virtualmachines/%s/backups/%s", vmId, backupId)))).location.replace("/api/v1", "");
            LOG.debug(urlToRequest);

            BackrollBackupMetricsResponse metrics = waitGet(urlToRequest);
            if (metrics.info != null) {
                return new BackrollBackupMetrics(Long.parseLong(metrics.info.originalSize),
                        Long.parseLong(metrics.info.deduplicatedSize));
            }
        } catch (final NotOkBodyException e) {
            throw new CloudRuntimeException("Failed to retrieve backups status for this VM via Backroll");
        } catch (final IOException | InterruptedException e) {
            LOG.error("Failed to check backups status due to: {}", e.getMessage());
        }
        return null;
    }

    public List<BackrollVmBackup> getAllBackupsfromVirtualMachine(String vmId) {
        LOG.info("Trying to retrieve all backups for vm {}", vmId);

        List<BackrollVmBackup> backups = new ArrayList<BackrollVmBackup>();

        try {

            String urlToRequest = this
                    .<BackrollTaskRequestResponse>parse(okBody(
                            get(String.format("/virtualmachines/%s/backups", vmId)))).location
                    .replace("/api/v1", "");
            LOG.debug(urlToRequest);

            VirtualMachineBackupsResponse response = waitGet(urlToRequest);

            if (response.state.equals(TaskState.SUCCESS)) {
                if (response.info.archives.size() > 0) {
                    for (BackupInfos infos : response.info.archives) {
                        var dateStart = new DateTime(infos.start);
                        backups.add(new BackrollVmBackup(infos.id, infos.name, dateStart.toDate()));
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return backups;
    }

    private HttpResponse post(final String path, final JSONObject json) throws IOException {
        String xml = null;
        StringEntity params = null;
        if (json != null) {
            LOG.debug("JSON {}", json.toString());
            params = new StringEntity(json.toString(), ContentType.APPLICATION_JSON);
        }

        String url = apiURI.toString() + path;
        final HttpPost request = new HttpPost(url);

        if (params != null) {
            request.setEntity(params);
        }

        request.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + backrollToken);
        request.setHeader("Content-type", "application/json");

        final HttpResponse response = httpClient.execute(request);

        LOG.debug("Response received in POST request with body {} is: {} for URL {}.", xml, response.toString(), url);

        return response;
    }

    protected HttpResponse get(final String path) throws IOException {
        String url = apiURI.toString() + path;
        LOG.debug("Backroll URL {}", url);
        final HttpGet request = new HttpGet(url);
        request.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + backrollToken);
        request.setHeader("Content-type", "application/json");
        final HttpResponse response = httpClient.execute(request);
        LOG.debug("Response received in GET request is: {} for URL: {}.", response.toString(), url);
        return response;
    }

    protected HttpResponse delete(final String path) throws IOException {
        String url = apiURI.toString() + path;
        final HttpDelete request = new HttpDelete(url);
        request.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + backrollToken);
        request.setHeader("Content-type", "application/json");
        final HttpResponse response = httpClient.execute(request);
        LOG.debug("Response received in GET request is: {} for URL: {}.", response.toString(), url);
        return response;
    }

    private boolean isResponseAuthorized(final HttpResponse response) {
        return response.getStatusLine().getStatusCode() == HttpStatus.SC_OK
                || response.getStatusLine().getStatusCode() == HttpStatus.SC_ACCEPTED;
    }

    private class NotOkBodyException extends Exception {
    }

    private String okBody(final HttpResponse response)
            throws ParseException, IOException, NotOkBodyException {
        switch (response.getStatusLine().getStatusCode()) {
            case HttpStatus.SC_OK:
            case HttpStatus.SC_ACCEPTED:
                HttpEntity bodyEntity = response.getEntity();
                try {
                    return EntityUtils.toString(bodyEntity);
                } finally {
                    EntityUtils.consumeQuietly(bodyEntity);
                }
            default:
                throw new NotOkBodyException();
        }
    }

    private <T> T parse(final String json)
            throws JsonMappingException, JsonProcessingException {
        return new ObjectMapper().readValue(json, new TypeReference<T>() {
        });
    }

    private <T> T waitGet(String url)
            throws IOException, InterruptedException {
        // int threshold = 30; // 5 minutes
        int maxAttempts = 12; // 2 minutes

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                String body = okBody(get(url));
                if (!body.contains(TaskState.PENDING)) {
                    return parse(body);
                }
            } catch (final NotOkBodyException e) {
                throw new CloudRuntimeException("An error occured with Backroll");
            }
            TimeUnit.SECONDS.sleep(10);
        }

        return null;
    }

    private boolean isAuthenticated() {
        boolean result = false;
        try {
            final HttpResponse response = post("/auth", null);
            result = isResponseAuthorized(response);
            EntityUtils.consumeQuietly(response.getEntity());
        } catch (IOException e) {
            LOG.error("Failed to authenticate to Backroll due to: {}", e.getMessage());
        }
        return result;
    }

    private void loginIfAuthenticationFailed() {
        if (!isAuthenticated()) {
            login(appname, password);
        }
    }

    private void login(final String appname, final String appsecret) {
        LOG.debug("Backroll client -  start login");
        final HttpPost request = new HttpPost(apiURI.toString() + "/login");

        request.addHeader("content-type", "application/json");

        JSONObject jsonBody = new JSONObject();
        StringEntity params;

        try {
            jsonBody.put("app_id", appname);
            jsonBody.put("app_secret", appsecret);
            params = new StringEntity(jsonBody.toString());
            request.setEntity(params);

            final HttpResponse response = httpClient.execute(request);
            try {
                LoginApiResponse loginResponse = parse(okBody(response));
                backrollToken = loginResponse.accessToken;
                LOG.debug("Backroll client -  Token : {}", backrollToken);

                if (StringUtils.isEmpty(loginResponse.accessToken)) {
                    throw new CloudRuntimeException("Backroll token is not available to perform API requests");
                }
            } catch (final NotOkBodyException e) {
                if (response.getStatusLine().getStatusCode() != HttpStatus.SC_CREATED) {
                    throw new CloudRuntimeException(
                            "Failed to create and authenticate Backroll client, please check the settings.");
                } else {
                    throw new ServerApiException(ApiErrorCode.UNAUTHORIZED,
                            "Backroll API call unauthorized, please ask your administrator to fix integration issues.");
                }
            }
        } catch (final IOException e) {
            throw new CloudRuntimeException("Failed to authenticate Backroll API service due to:" + e.getMessage());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        LOG.debug("Backroll client -  end login");
    }

    private List<BackrollBackup> getBackrollBackups(final String vmId) {
        try {
            LOG.info("start to list Backroll backups for vm {}", vmId);
            String urlToRequest = this
                    .<BackrollTaskRequestResponse>parse(okBody(get("/virtualmachines/" + vmId + "/backups"))).location
                    .replace("/api/v1", "");
            LOG.debug(urlToRequest);
            BackrollBackupsFromVMResponse backrollBackupsFromVMResponse = waitGet(urlToRequest);

            final List<BackrollBackup> backups = new ArrayList<>();
            for (final BackrollArchiveResponse archive : backrollBackupsFromVMResponse.archives.archives) {
                backups.add(new BackrollBackup(archive.name));
                LOG.debug(archive.name);
            }
            return backups;
        } catch (final NotOkBodyException e) {
            throw new CloudRuntimeException("Failed to retrieve backups for this VM via Backroll");
        } catch (final IOException e) {
            LOG.error("Failed to list backup form vm with Backroll due to: {}", e);
        } catch (InterruptedException e) {
            LOG.error("Backroll Error: {}", e);
        }
        return new ArrayList<BackrollBackup>();
    }
}
