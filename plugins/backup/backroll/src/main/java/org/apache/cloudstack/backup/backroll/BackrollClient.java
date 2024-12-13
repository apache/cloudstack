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
import org.apache.cloudstack.backup.backroll.BackrollService.NotOkBodyException;
import org.apache.cloudstack.backup.backroll.model.BackrollBackup;
import org.apache.cloudstack.backup.backroll.model.BackrollBackupMetrics;
import org.apache.cloudstack.backup.backroll.model.BackrollOffering;
import org.apache.cloudstack.backup.backroll.model.BackrollTaskStatus;
import org.apache.cloudstack.backup.backroll.model.BackrollVmBackup;
import org.apache.cloudstack.backup.backroll.model.response.BackrollTaskRequestResponse;
import org.apache.cloudstack.backup.backroll.model.response.TaskState;
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
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
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

    protected Logger logger = LogManager.getLogger(BackrollClient.class);

    private final URI apiURI;

    private String backrollToken = null;
    private String appname = null;
    private String password = null;
    private boolean validateCertificate = false;
    private RequestConfig config = null;
    private BackrollService backrollService;

    private int restoreTimeout;

    public BackrollClient(final String url, final String appname, final String password,
            final boolean validateCertificate, final int timeout,
            final int restoreTimeout, final BackrollService backrollService)
            throws URISyntaxException, NoSuchAlgorithmException, KeyManagementException {
        this.apiURI = new URI(url);
        this.restoreTimeout = restoreTimeout;
        this.appname = appname;
        this.password = password;
        this.backrollService = backrollService;

        this.config = RequestConfig.custom()
            .setConnectTimeout(timeout * 1000)
            .setConnectionRequestTimeout(timeout * 1000)
            .setSocketTimeout(timeout * 1000)
            .build();
    }

    private CloseableHttpClient createHttpClient() throws KeyManagementException, NoSuchAlgorithmException {
        if(!validateCertificate) {
            SSLContext sslContext = SSLUtils.getSSLContext();
            sslContext.init(null, new X509TrustManager[] { new TrustAllManager() }, new SecureRandom());
            final SSLConnectionSocketFactory factory = new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE);
            return HttpClientBuilder.create()
                .setDefaultRequestConfig(config)
                .setSSLSocketFactory(factory)
                .build();
        } else {
            return HttpClientBuilder.create()
                .setDefaultRequestConfig(config)
                .build();
        }
    }

    private void closeConnection(CloseableHttpResponse closeableHttpResponse) throws IOException {
        closeableHttpResponse.close();
    }

    private String triggerBackrollTaskForVM(final String vmId, JSONObject jsonBody, final String task) throws InterruptedException, KeyManagementException, ParseException, NoSuchAlgorithmException, IOException, NotOkBodyException {
            CloseableHttpResponse response = backrollService.post(apiURI,String.format("/tasks/%s/%s",task, vmId) ,jsonBody);
            String result = backrollService.okBody(response);
            BackrollTaskRequestResponse requestResponse = new ObjectMapper().readValue(result, BackrollTaskRequestResponse.class);
            response.close();
            String urlToRequest = requestResponse.location.replace("/api/v1", "");
            return urlToRequest;
    }

    public boolean restoreVMFromBackup(final String vmId, final String backupName) throws KeyManagementException, ParseException, NoSuchAlgorithmException, IOException {
        logger.info("Start restore backup with backroll with backup {} for vm {}", backupName, vmId);

        loginIfAuthenticationFailed();
        boolean isRestoreOk = false;

        try {
            JSONObject jsonBody = new JSONObject();
            try {
                jsonBody.put("virtual_machine_id", vmId);
                jsonBody.put("backup_name", backupName);
                jsonBody.put("storage", "");
                jsonBody.put("mode", "single");

            } catch (JSONException e) {
                logger.error("Backroll Error: {}", e.getMessage());
            }

            String urlToRequest = triggerBackrollTaskForVM(vmId, jsonBody, "restore");
            String result = backrollService.waitGet(apiURI,urlToRequest);

            if(result.contains("SUCCESS")) {
                logger.debug("RESTORE SUCCESS");
                isRestoreOk = true;
            }

        } catch (final NotOkBodyException e) {
            return false;
        } catch (final IOException | InterruptedException e) {
            logger.error("Ouch! Failed to restore VM with Backroll due to: {}", e.getMessage());
            throw new CloudRuntimeException("Ouch! Failed to restore VM with Backroll due to: {}" + e.getMessage());
        }
        return isRestoreOk;
    }

    public void triggerTaskStatus(String urlToRequest)
        throws IOException, InterruptedException, KeyManagementException, ParseException, NoSuchAlgorithmException{
            backrollService.waitGet(apiURI,urlToRequest);
    }

    public String startBackupJob(final String vmId) throws KeyManagementException, NoSuchAlgorithmException, IOException {
        logger.info("Trying to start backup for Backroll job: {}", vmId);
        loginIfAuthenticationFailed();

        try {
            String urlToRequest = triggerBackrollTaskForVM(vmId, null, "singlebackup");
            return urlToRequest;
        } catch (final Exception e) {
            logger.error("Failed to start Backroll backup job due to: {}", e.getMessage());
        }
        return null;
    }

    public String getBackupOfferingUrl() throws KeyManagementException, NoSuchAlgorithmException, IOException {
        logger.info("Trying to list backroll backup policies");

        loginIfAuthenticationFailed();
        String url = "";

        try {
            CloseableHttpResponse response = backrollService.get(apiURI,"/backup_policies");
            String result = backrollService.okBody(response);
            logger.info("BackrollClient:getBackupOfferingUrl:result:  " + result);
            //BackrollTaskRequestResponse requestResponse = parse(result);
            BackrollTaskRequestResponse requestResponse = new ObjectMapper().readValue(result, BackrollTaskRequestResponse.class);
            logger.info("BackrollClient:getBackupOfferingUrl:Apres PArse:  " + requestResponse.location);
            response.close();
            url = requestResponse.location.replace("/api/v1", "");
        } catch (final Exception e) {
            logger.info("Failed to list Backroll jobs due to: {}", e.getMessage());
            logger.error("Failed to list Backroll jobs due to: {}", e.getMessage());
        }
        return StringUtils.isEmpty(url) ? null : url;
    }

    public List<BackupOffering> getBackupOfferings(String idTask) throws KeyManagementException, ParseException, NoSuchAlgorithmException, IOException {
        logger.info("Trying to list backroll backup policies");

        loginIfAuthenticationFailed();
        final List<BackupOffering> policies = new ArrayList<>();

        try {
            String results = backrollService.waitGet(apiURI,idTask);
            BackupPoliciesResponse backupPoliciesResponse = new ObjectMapper().readValue(results, BackupPoliciesResponse.class);

            for (final BackrollBackupPolicyResponse policy : backupPoliciesResponse.backupPolicies) {
                policies.add(new BackrollOffering(policy.name, policy.id));
            }
        } catch (final IOException | InterruptedException e) {
            logger.error("Failed to list Backroll jobs due to: {}", e.getMessage());
        }
        return policies;
    }

    public BackrollTaskStatus checkBackupTaskStatus(String taskId) throws KeyManagementException, ParseException, NoSuchAlgorithmException, IOException {
        logger.info("Trying to get backup status for Backroll task: {}", taskId);

        loginIfAuthenticationFailed();

        BackrollTaskStatus status = new BackrollTaskStatus();

        try {

            String body = backrollService.okBody(backrollService.get(apiURI,"/status/" + taskId));

            if (body.contains(TaskState.FAILURE) || body.contains(TaskState.PENDING)) {
                BackrollBackupStatusResponse backupStatusRequestResponse = new ObjectMapper().readValue(body, BackrollBackupStatusResponse.class);
                status.setState(backupStatusRequestResponse.state);
            } else {
                BackrollBackupStatusSuccessResponse backupStatusSuccessRequestResponse = new ObjectMapper().readValue(body, BackrollBackupStatusSuccessResponse.class);
                status.setState(backupStatusSuccessRequestResponse.state);
                status.setInfo(backupStatusSuccessRequestResponse.info);
            }

        } catch (final NotOkBodyException e) {
            // throw new CloudRuntimeException("Failed to retrieve backups status for this
            // VM via Backroll");
            logger.error("Failed to retrieve backups status for this VM via Backroll");

        } catch (final IOException e) {
            logger.error("Failed to check backups status due to: {}", e.getMessage());
        }
        return StringUtils.isEmpty(status.getState()) ? null : status;
    }

    public boolean deleteBackup(final String vmId, final String backupName) throws KeyManagementException, NoSuchAlgorithmException, IOException {
        logger.info("Trying to delete backup {} for vm {} using Backroll", vmId, backupName);

        loginIfAuthenticationFailed();

        boolean isBackupDeleted = false;

        try {

            CloseableHttpResponse response = backrollService.delete(apiURI,String.format("/virtualmachines/%s/backups/%s", vmId, backupName));
            String result = backrollService.okBody(response);
            BackrollTaskRequestResponse requestResponse = new ObjectMapper().readValue(result, BackrollTaskRequestResponse.class);
            response.close();
            String urlToRequest = requestResponse.location.replace("/api/v1", "");

            result = backrollService.waitGet(apiURI,urlToRequest);
            BackrollBackupsFromVMResponse backrollBackupsFromVMResponse = new ObjectMapper().readValue(result, BackrollBackupsFromVMResponse.class);
            logger.debug(backrollBackupsFromVMResponse.state);
            isBackupDeleted = backrollBackupsFromVMResponse.state.equals(TaskState.SUCCESS);
        } catch (final NotOkBodyException e) {
            isBackupDeleted = false;
        } catch (final Exception e) {
            isBackupDeleted = false;
            logger.error("Failed to delete backup using Backroll due to: {}", e.getMessage());
        }
        return isBackupDeleted;
    }

    public Metric getVirtualMachineMetrics(final String vmId) throws KeyManagementException, NoSuchAlgorithmException, IOException {
        logger.info("Trying to retrieve virtual machine metric from Backroll for vm {}", vmId);

        loginIfAuthenticationFailed();

        Metric metric = new Metric(0L, 0L);

        try {

            CloseableHttpResponse response = backrollService.get(apiURI,String.format("/virtualmachines/%s/repository", vmId));
            String result = backrollService.okBody(response);
            BackrollTaskRequestResponse requestResponse = new ObjectMapper().readValue(result, BackrollTaskRequestResponse.class);
            response.close();
            String urlToRequest = requestResponse.location.replace("/api/v1", "");

            result = backrollService.waitGet(apiURI,urlToRequest);
            BackrollVmMetricsResponse vmMetricsResponse =new ObjectMapper().readValue(result, BackrollVmMetricsResponse.class);

            if (vmMetricsResponse != null && vmMetricsResponse.state.equals(TaskState.SUCCESS)) {
                logger.debug("SUCCESS ok");
                CacheStats stats = null;
                try {
                    stats = vmMetricsResponse.infos.cache.stats;
                } catch (NullPointerException e) {
                }
                if (stats != null) {
                    long size = Long.parseLong(stats.totalSize);
                    metric = new Metric(size, size);
                }
            }
        } catch (final Exception e) {
            logger.error("Failed to retrieve virtual machine metrics with Backroll due to: {}", e.getMessage());
        }

        return metric;
    }

    public BackrollBackupMetrics getBackupMetrics(String vmId, String backupId) throws KeyManagementException, NoSuchAlgorithmException, IOException {
        logger.info("Trying to get backup metrics for VM: {}, and backup: {}", vmId, backupId);

        loginIfAuthenticationFailed();

        BackrollBackupMetrics metrics = null;

        try {

            CloseableHttpResponse response = backrollService.get(apiURI,String.format("/virtualmachines/%s/backups/%s", vmId, backupId));
            String result = backrollService.okBody(response);
            BackrollTaskRequestResponse requestResponse = new ObjectMapper().readValue(result, BackrollTaskRequestResponse.class);
            response.close();
            String urlToRequest = requestResponse.location.replace("/api/v1", "");

            logger.debug(urlToRequest);

            result = backrollService.waitGet(apiURI,urlToRequest);
            BackrollBackupMetricsResponse metricsResponse = new ObjectMapper().readValue(result, BackrollBackupMetricsResponse.class);
            if (metricsResponse.info != null) {
                metrics = new BackrollBackupMetrics(Long.parseLong(metricsResponse.info.originalSize),
                        Long.parseLong(metricsResponse.info.deduplicatedSize));
            }
        } catch (final NotOkBodyException e) {
            throw new CloudRuntimeException("Failed to retrieve backups status for this VM via Backroll");
        } catch (final IOException | InterruptedException e) {
            logger.error("Failed to check backups status due to: {}", e.getMessage());
        }
        return metrics;
    }

    public List<BackrollVmBackup> getAllBackupsfromVirtualMachine(String vmId) {
        //logger.info("Trying to retrieve all backups for vm {}", vmId);

        List<BackrollVmBackup> backups = new ArrayList<BackrollVmBackup>();

        try {

            CloseableHttpResponse response = backrollService.get(apiURI, String.format("/virtualmachines/%s/backups", vmId));
            String result = backrollService.okBody(response);
            BackrollTaskRequestResponse requestResponse = new ObjectMapper().readValue(result, BackrollTaskRequestResponse.class);
            response.close();
            String urlToRequest = requestResponse.location.replace("/api/v1", "");

            //logger.debug(urlToRequest);

            result = backrollService.waitGet(apiURI,urlToRequest);
            VirtualMachineBackupsResponse virtualMachineBackupsResponse = new ObjectMapper().readValue(result, VirtualMachineBackupsResponse.class);

            if (virtualMachineBackupsResponse.state.equals(TaskState.SUCCESS)) {
                if (virtualMachineBackupsResponse.info.archives.size() > 0) {
                    for (BackupInfos infos : virtualMachineBackupsResponse.info.archives) {
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

    private boolean isResponseAuthorized(final HttpResponse response) {
        return response.getStatusLine().getStatusCode() == HttpStatus.SC_OK
                || response.getStatusLine().getStatusCode() == HttpStatus.SC_ACCEPTED;
    }

    private boolean isAuthenticated() throws KeyManagementException, NoSuchAlgorithmException {
        boolean result = false;

        if(StringUtils.isEmpty(backrollToken)) {
            return result;
        }

        try {
            CloseableHttpResponse response = backrollService.post(apiURI,"/auth", null);
            result = isResponseAuthorized(response);
            EntityUtils.consumeQuietly(response.getEntity());
            closeConnection(response);
        } catch (IOException e) {
            logger.error("Failed to authenticate to Backroll due to: {}", e.getMessage());
        }
        return result;
    }

    private void loginIfAuthenticationFailed() throws KeyManagementException, NoSuchAlgorithmException, IOException {
        if (!isAuthenticated()) {
            login(appname, password);
        }
    }


    private void login(final String appname, final String appsecret) throws IOException, KeyManagementException, NoSuchAlgorithmException {
        logger.debug("Backroll client -  start login");

        CloseableHttpClient httpClient = createHttpClient();
        CloseableHttpResponse response = null;

        final HttpPost request = new HttpPost(apiURI.toString() + "/login");
        request.addHeader("content-type", "application/json");

        JSONObject jsonBody = new JSONObject();
        StringEntity params;

        try {
            jsonBody.put("app_id", appname);
            jsonBody.put("app_secret", appsecret);
            params = new StringEntity(jsonBody.toString());
            request.setEntity(params);

            response = httpClient.execute(request);
            try {
                String toto = backrollService.okBody(response);
                ObjectMapper objectMapper = new ObjectMapper();
                logger.info("BACKROLL:     " + toto);
                LoginApiResponse loginResponse = objectMapper.readValue(toto, LoginApiResponse.class);
                logger.info("ok");
                backrollToken = loginResponse.accessToken;
                logger.debug("Backroll client -  Token : {}", backrollToken);

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
        finally {
            closeConnection(response);
        }
        logger.debug("Backroll client -  end login");
    }

    private List<BackrollBackup> getBackrollBackups(final String vmId) throws KeyManagementException, ParseException, NoSuchAlgorithmException {
        try {
            logger.info("start to list Backroll backups for vm {}", vmId);

            CloseableHttpResponse response = backrollService.get(apiURI,"/virtualmachines/" + vmId + "/backups");
            String result = backrollService.okBody(response);
            BackrollTaskRequestResponse requestResponse = new ObjectMapper().readValue(result, BackrollTaskRequestResponse.class);
            response.close();
            String urlToRequest = requestResponse.location.replace("/api/v1", "");

            logger.debug(urlToRequest);
            result = backrollService.waitGet(apiURI,urlToRequest);
            BackrollBackupsFromVMResponse backrollBackupsFromVMResponse = new ObjectMapper().readValue(result, BackrollBackupsFromVMResponse.class);

            final List<BackrollBackup> backups = new ArrayList<>();
            for (final BackrollArchiveResponse archive : backrollBackupsFromVMResponse.archives.archives) {
                backups.add(new BackrollBackup(archive.name));
                logger.debug(archive.name);
            }
            return backups;
        } catch (final NotOkBodyException e) {
            throw new CloudRuntimeException("Failed to retrieve backups for this VM via Backroll");
        } catch (final IOException e) {
            logger.error("Failed to list backup form vm with Backroll due to: {}", e);
        } catch (InterruptedException e) {
            logger.error("Backroll Error: {}", e);
        }
        return new ArrayList<BackrollBackup>();
    }
}