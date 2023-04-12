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

package org.apache.cloudstack.backup.networker;

import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.nio.TrustAllManager;
import com.cloud.vm.VirtualMachine;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.backup.BackupOffering;
import org.apache.cloudstack.backup.BackupVO;
import org.apache.cloudstack.backup.networker.api.NetworkerBackup;
import org.apache.cloudstack.backup.networker.api.NetworkerBackups;
import org.apache.cloudstack.backup.networker.api.ProtectionPolicies;
import org.apache.cloudstack.backup.networker.api.ProtectionPolicy;
import org.apache.cloudstack.utils.security.SSLUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.log4j.Logger;

import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;

import static org.apache.cloudstack.backup.NetworkerBackupProvider.BACKUP_IDENTIFIER;

public class NetworkerClient {
    private static final Logger LOG = Logger.getLogger(NetworkerClient.class);
    private final URI apiURI;
    private final String apiName;
    private final String apiPassword;
    private final HttpClient httpClient;

    public NetworkerClient(final String url, final String username, final String password, final boolean validateCertificate, final int timeout) throws URISyntaxException, NoSuchAlgorithmException, KeyManagementException {

        apiName = username;
        apiPassword = password;

        this.apiURI = new URI(url);
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
    }

    private void authenticate(final String username, final String password) {

        final HttpGet request = new HttpGet(apiURI.toString());
        request.setHeader(HttpHeaders.AUTHORIZATION, "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes()));
        request.setHeader(HttpHeaders.ACCEPT, "application/json");
        request.setHeader(HttpHeaders.USER_AGENT, "CloudStack B&R");
        request.setHeader(HttpHeaders.CONNECTION, "keep-alive");
        try {
            final HttpResponse response = httpClient.execute(request);
            checkAuthFailure(response);
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                throw new CloudRuntimeException("Failed to create and authenticate EMC Networker API client, please check the settings.");
            }
        } catch (final IOException e) {
            throw new CloudRuntimeException("Failed to authenticate Networker API service due to:" + e.getMessage());
        }
    }

    private void checkAuthFailure(final HttpResponse response) {
        if (response != null && response.getStatusLine().getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
            throw new ServerApiException(ApiErrorCode.UNAUTHORIZED, "EMC Networker B&R API call unauthorized. Check username/password or contact your backup administrator.");
        }
    }

    private void checkResponseOK(final HttpResponse response) {
        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_NO_CONTENT) {
            LOG.debug("Requested EMC Networker resource does not exist");
            return;
        }
        if (!(response.getStatusLine().getStatusCode() == HttpStatus.SC_OK ||
                response.getStatusLine().getStatusCode() == HttpStatus.SC_ACCEPTED) &&
                response.getStatusLine().getStatusCode() != HttpStatus.SC_NO_CONTENT) {
            LOG.debug(String.format("HTTP request failed, status code is [%s], response is: [%s].", response.getStatusLine().getStatusCode(), response));
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Got invalid API status code returned by the EMC Networker server");
        }
    }

    private void checkResponseTimeOut(final Exception e) {
        if (e instanceof ConnectTimeoutException || e instanceof SocketTimeoutException) {
            throw new ServerApiException(ApiErrorCode.RESOURCE_UNAVAILABLE_ERROR, "EMC Networker API operation timed out, please try again.");
        }
    }

    private HttpResponse get(final String path) throws IOException {
        String url = apiURI.toString() + path;
        final HttpGet request = new HttpGet(url);
        request.setHeader(HttpHeaders.AUTHORIZATION, "Basic " + Base64.getEncoder().encodeToString((apiName + ":" + apiPassword).getBytes()));
        request.setHeader(HttpHeaders.ACCEPT, "application/json");
        request.setHeader(HttpHeaders.USER_AGENT, "CloudStack B&R");
        final HttpResponse response = httpClient.execute(request);
        checkAuthFailure(response);

        LOG.debug(String.format("Response received in GET request is: [%s] for URL: [%s].", response.toString(), url));
        return response;
    }

    public  String getBackupPolicyRetentionInterval(String externalId) {
        try {
            final HttpResponse response = get("/global/protectionpolicies/?q=comment:" + BACKUP_IDENTIFIER);
            checkResponseOK(response);
            final ObjectMapper jsonMapper = new ObjectMapper();
            jsonMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            final ProtectionPolicies protectionPolicies = jsonMapper.readValue(response.getEntity().getContent(), ProtectionPolicies.class);

            if (protectionPolicies == null || protectionPolicies.getProtectionPolicies() == null) {
                return null;
            }
            for (final ProtectionPolicy protectionPolicy : protectionPolicies.getProtectionPolicies()) {
                if ( protectionPolicy.getResourceId().getId().equals(externalId)) {
                    return protectionPolicy.getPolicyProtectionPeriod();
                }
            }
        } catch (final IOException e) {
            LOG.error("Failed to get Protection Policy Period from EMC Networker due to:", e);
            checkResponseTimeOut(e);
        }
        return null;
    }
    private HttpResponse delete(final String path) throws IOException {
        String url = apiURI.toString() + path;
        final HttpDelete request = new HttpDelete(url);
        request.setHeader(HttpHeaders.AUTHORIZATION, "Basic " + Base64.getEncoder().encodeToString((apiName + ":" + apiPassword).getBytes()));
        request.setHeader(HttpHeaders.USER_AGENT, "CloudStack B&R");
        final HttpResponse response = httpClient.execute(request);
        checkAuthFailure(response);

        LOG.debug(String.format("Response received in DELETE request is: [%s] for URL [%s].", response.toString(), url));
        return response;
    }

    public boolean deleteBackupForVM(String externalId) {
        try {
            final HttpResponse response = delete("/global/backups/" + externalId);
            checkResponseOK(response);
            return true;
        } catch (final IOException e) {
            LOG.error("Failed to delete backup from EMC Networker due to:", e);
            checkResponseTimeOut(e);
        }
        return false;
    }


    public BackupVO registerBackupForVm(VirtualMachine vm, Date backupJobStart, String saveTime) {
        LOG.debug("Querying EMC Networker about latest backup");

        NetworkerBackups networkerBackups;
        BackupVO backup = new BackupVO();

        SimpleDateFormat formatterDate = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat formatterTime = new SimpleDateFormat("HH:mm:ss");
        SimpleDateFormat formatterDateTime = new SimpleDateFormat("yyy-MM-dd'T'HH:mm:ss");

        String startDate = formatterDate.format(backupJobStart);
        String startTime = formatterTime.format(backupJobStart);
        String endDate = formatterDate.format(new Date());
        String endTime = formatterTime.format(new Date());

        final String searchRange = "['" + startDate + "T" + startTime + "'+TO+'" + endDate + "T" + endTime + "']";
        String backupJobCriteria;

        try {
            if ( saveTime != null ) {
                Instant instant = Instant.ofEpochSecond(Long.parseLong(saveTime));
                String completionTime = formatterDateTime.format(Date.from(instant));
                backupJobCriteria = "+and+saveTime:" + "'" + completionTime + "'";
            }
            else {
                backupJobCriteria = "+and+saveTime:" + searchRange;
            }
            final HttpResponse response = get("/global/backups/?q=name:" + vm.getName() + backupJobCriteria);
            checkResponseOK(response);
            final ObjectMapper jsonMapper = new ObjectMapper();
            jsonMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            networkerBackups = jsonMapper.readValue(response.getEntity().getContent(), NetworkerBackups.class);
            NetworkerBackup networkerLatestBackup = new NetworkerBackup();
            LOG.debug("Found backups " + networkerBackups.getBackups() + " With Criteria: " + backupJobCriteria);
            if ( networkerBackups.getBackups() == null || networkerBackups.getCount() == 0 ) {
                return null;
            }
            if (networkerBackups.getCount() == 1) {
                networkerLatestBackup = networkerBackups.getBackups().get(0);
            } else {
                for (final NetworkerBackup networkerBackup : networkerBackups.getBackups()) {
                    LOG.debug("Found Backup :" + networkerBackup.getName());
                }
            }

            backup.setVmId(vm.getId());
            backup.setExternalId(networkerLatestBackup.getId());
            backup.setType(networkerLatestBackup.getType());
            backup.setDate(networkerLatestBackup.getCreationTime());
            backup.setSize(networkerLatestBackup.getSize().getValue());
            backup.setProtectedSize(networkerLatestBackup.getSize().getValue());
            backup.setStatus(org.apache.cloudstack.backup.Backup.Status.BackedUp);
            backup.setBackupOfferingId(vm.getBackupOfferingId());
            backup.setAccountId(vm.getAccountId());
            backup.setDomainId(vm.getDomainId());
            backup.setZoneId(vm.getDataCenterId());
            return backup;
        } catch (final IOException e) {
            LOG.error("Failed to register backup from EMC Networker due to:", e);
            checkResponseTimeOut(e);
        }
        return null;
    }


    public NetworkerBackup getNetworkerBackupInfo(String backupId) {
        LOG.debug("Trying to get EMC Networker details for backup " + backupId);
        try {
            final HttpResponse response = get("/global/backups/?q=id:" + backupId);
            checkResponseOK(response);
            final ObjectMapper jsonMapper = new ObjectMapper();
            jsonMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            NetworkerBackups networkerBackups = jsonMapper.readValue(response.getEntity().getContent(), NetworkerBackups.class);
            NetworkerBackup networkerBackup = networkerBackups.getBackups().get(0);
            if ( networkerBackup.getShortId() == null ) {
                return null;
            }
            return networkerBackup;
        } catch (final IOException e) {
            LOG.error("Failed to list EMC Networker backups due to:", e);
            checkResponseTimeOut(e);
        }
        return null;
    }
    public ArrayList<String> getBackupsForVm(VirtualMachine vm) {
        SimpleDateFormat formatterDateTime = new SimpleDateFormat("yyy-MM-dd'T'HH:mm:ss");

        LOG.debug("Trying to list EMC Networker backups for VM " + vm.getName());
        try {
            final HttpResponse response = get("/global/backups/?q=name:" + vm.getName());
            checkResponseOK(response);
            final ObjectMapper jsonMapper = new ObjectMapper();
            jsonMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            NetworkerBackups networkerBackups = jsonMapper.readValue(response.getEntity().getContent(), NetworkerBackups.class);
            final ArrayList<String> backupsTaken = new ArrayList<>();
            if (networkerBackups == null || networkerBackups.getBackups() == null) {
                return backupsTaken;
            }
            for (final NetworkerBackup backup : networkerBackups.getBackups()) {
                LOG.debug("Found Backup " + backup.getId());
                // Backups that have expired on the EMC Networker but not removed yet will not be added
                try {
                    Date backupRetentionTime = formatterDateTime.parse(backup.getRetentionTime());
                    Date currentTime = new Date();
                    if (currentTime.compareTo(backupRetentionTime) < 0) {
                        backupsTaken.add(backup.getId());
                    }
                } catch (ParseException e) {
                    throw new RuntimeException("Failed to parse EMC Networker backup retention time:  "+ e);
                }
            }
            return backupsTaken;
        } catch (final IOException e) {
            LOG.error("Failed to list EMC Networker backups due to:", e);
            checkResponseTimeOut(e);
        }
        return new ArrayList<>();
    }
    public List<BackupOffering> listPolicies() {
        LOG.debug("Trying to list backup EMC Networker Policies we can use");
        try {
            final HttpResponse response = get("/global/protectionpolicies/?q=comment:" + BACKUP_IDENTIFIER);
            checkResponseOK(response);
            final ObjectMapper jsonMapper = new ObjectMapper();
            jsonMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            final ProtectionPolicies protectionPolicies = jsonMapper.readValue(response.getEntity().getContent(), ProtectionPolicies.class);

            final List<BackupOffering> policies = new ArrayList<>();

            if (protectionPolicies == null || protectionPolicies.getProtectionPolicies() == null) {
                return policies;
            }
            for (final ProtectionPolicy protectionPolicy : protectionPolicies.getProtectionPolicies()) {
                LOG.debug("Found Protection Policy:" + protectionPolicy.getName());
                policies.add(new NetworkerBackupOffering(protectionPolicy.getName(), protectionPolicy.getResourceId().getId()));
            }
            return policies;
        } catch (final IOException e) {
            LOG.error("Failed to list EMC Networker Protection Policies jobs due to:", e);
            checkResponseTimeOut(e);
        }
        return new ArrayList<>();
    }
}
