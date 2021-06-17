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

package org.apache.cloudstack.storage.datastore.client;

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

import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;

import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.storage.datastore.api.ProtectionDomain;
import org.apache.cloudstack.storage.datastore.api.Sdc;
import org.apache.cloudstack.storage.datastore.api.SdcMappingInfo;
import org.apache.cloudstack.storage.datastore.api.SnapshotDef;
import org.apache.cloudstack.storage.datastore.api.SnapshotDefs;
import org.apache.cloudstack.storage.datastore.api.SnapshotGroup;
import org.apache.cloudstack.storage.datastore.api.StoragePool;
import org.apache.cloudstack.storage.datastore.api.StoragePoolStatistics;
import org.apache.cloudstack.storage.datastore.api.VTree;
import org.apache.cloudstack.storage.datastore.api.VTreeMigrationInfo;
import org.apache.cloudstack.storage.datastore.api.Volume;
import org.apache.cloudstack.storage.datastore.api.VolumeStatistics;
import org.apache.cloudstack.utils.security.SSLUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.pool.PoolStats;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

import com.cloud.storage.Storage;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.nio.TrustAllManager;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

public class ScaleIOGatewayClientImpl implements ScaleIOGatewayClient {
    private static final Logger LOG = Logger.getLogger(ScaleIOGatewayClientImpl.class);

    private final URI apiURI;
    private final HttpClient httpClient;
    private final PoolingHttpClientConnectionManager connectionManager;
    private static final String MDM_CONNECTED_STATE = "Connected";

    private String username;
    private String password;
    private String sessionKey = null;

    // The session token is valid for 8 hours from the time it was created, unless there has been no activity for 10 minutes
    // Reference: https://cpsdocs.dellemc.com/bundle/PF_REST_API_RG/page/GUID-92430F19-9F44-42B6-B898-87D5307AE59B.html
    private static final long MAX_VALID_SESSION_TIME_IN_HRS = 8;
    private static final long MAX_VALID_SESSION_TIME_IN_MILLISECS = MAX_VALID_SESSION_TIME_IN_HRS * 60 * 60 * 1000;
    private static final long MAX_IDLE_TIME_IN_MINS = 10;
    private static final long MAX_IDLE_TIME_IN_MILLISECS = MAX_IDLE_TIME_IN_MINS * 60 * 1000;
    private static final long BUFFER_TIME_IN_MILLISECS = 30 * 1000; // keep 30 secs buffer before the expiration (to avoid any last-minute operations)

    private boolean authenticating = false;
    private long createTime = 0;
    private long lastUsedTime = 0;

    public ScaleIOGatewayClientImpl(final String url, final String username, final String password,
                                    final boolean validateCertificate, final int timeout, final int maxConnections)
            throws NoSuchAlgorithmException, KeyManagementException, URISyntaxException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(url), "Gateway client url cannot be null");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(username) && !Strings.isNullOrEmpty(password), "Gateway client credentials cannot be null");

        final RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(timeout * 1000)
                .setConnectionRequestTimeout(timeout * 1000)
                .setSocketTimeout(timeout * 1000)
                .build();

        SSLConnectionSocketFactory factory = SSLConnectionSocketFactory.getSocketFactory();
        if (!validateCertificate) {
            final SSLContext sslcontext = SSLUtils.getSSLContext();
            sslcontext.init(null, new X509TrustManager[]{new TrustAllManager()}, new SecureRandom());
            factory = new SSLConnectionSocketFactory(sslcontext, NoopHostnameVerifier.INSTANCE);
        }

        final Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory> create()
                .register("https", factory)
                .build();
        connectionManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
        connectionManager.setMaxTotal(maxConnections);
        connectionManager.setDefaultMaxPerRoute(maxConnections);

        this.httpClient = HttpClientBuilder.create()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(config)
                .setSSLSocketFactory(factory)
                .build();

        this.apiURI = new URI(url);
        this.username = username;
        this.password = password;

        authenticate();
        LOG.debug("API client for the PowerFlex gateway " + apiURI.getHost() + " is created successfully, with max connections: "
                + maxConnections + " and timeout: " + timeout + " secs");
    }

    /////////////////////////////////////////////////////////////
    //////////////// Private Helper Methods /////////////////////
    /////////////////////////////////////////////////////////////

    private synchronized void authenticate() {
        final HttpGet request = new HttpGet(apiURI.toString() + "/login");
        request.setHeader(HttpHeaders.AUTHORIZATION, "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes()));
        HttpResponse response = null;
        try {
            authenticating = true;
            LOG.debug("Authenticating gateway " + apiURI.getHost() + " with the request: " + request.toString());
            response = httpClient.execute(request);
            if (isNullResponse(response)) {
                LOG.warn("Invalid response received while authenticating, for the request: " + request.toString());
                throw new CloudRuntimeException("Failed to authenticate PowerFlex API Gateway due to invalid response from the Gateway " + apiURI.getHost());
            }

            LOG.debug("Received response: " + response.getStatusLine().getStatusCode() + " " + response.getStatusLine().getReasonPhrase()
                    + ", for the authenticate request: " + request.toString());
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                throw new CloudRuntimeException("PowerFlex Gateway " + apiURI.getHost() + " login failed, please check the provided settings");
            }

            String sessionKeyInResponse = EntityUtils.toString(response.getEntity());
            if (Strings.isNullOrEmpty(sessionKeyInResponse)) {
                throw new CloudRuntimeException("Failed to create a valid session for PowerFlex Gateway " + apiURI.getHost() + " to perform API requests");
            }

            LOG.info("PowerFlex API Gateway " + apiURI.getHost() + " authenticated successfully");
            this.sessionKey = sessionKeyInResponse.replace("\"", "");

            long now = System.currentTimeMillis();
            createTime = lastUsedTime = now;
        } catch (final IOException e) {
            LOG.error("Failed to authenticate PowerFlex API Gateway " + apiURI.getHost() + " due to: " + e.getMessage() + getConnectionManagerStats());
            throw new CloudRuntimeException("Failed to authenticate PowerFlex API Gateway " + apiURI.getHost() + " due to: " + e.getMessage());
        } finally {
            authenticating = false;
            if (response != null) {
                EntityUtils.consumeQuietly(response.getEntity());
            }
        }
    }

    private synchronized void renewClientSessionOnExpiry() {
        if (isSessionExpired()) {
            LOG.debug("Session expired for the PowerFlex API Gateway " + apiURI.getHost() + ", renewing");
            authenticate();
        }
    }

    private boolean isSessionExpired() {
        long now = System.currentTimeMillis() + BUFFER_TIME_IN_MILLISECS;
        if ((now - createTime) > MAX_VALID_SESSION_TIME_IN_MILLISECS) {
            LOG.debug("Session expired for the Gateway " + apiURI.getHost() + ", token is invalid after " + MAX_VALID_SESSION_TIME_IN_HRS
                    + " hours from the time it was created");
            return true;
        }

        if ((now - lastUsedTime) > MAX_IDLE_TIME_IN_MILLISECS) {
            LOG.debug("Session expired for the Gateway " + apiURI.getHost() + ", as there has been no activity for " + MAX_IDLE_TIME_IN_MINS + " mins");
            return true;
        }

        return false;
    }

    private boolean isNullResponse(final HttpResponse response) {
        if (response == null) {
            LOG.warn("Nil response");
            return true;
        }

        if (response.getStatusLine() == null) {
            LOG.warn("No status line in the response");
            return true;
        }

        return false;
    }

    private boolean checkAuthFailure(final HttpResponse response, final boolean renewAndRetryOnAuthFailure) {
        if (!isNullResponse(response) && response.getStatusLine().getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
            if (!renewAndRetryOnAuthFailure) {
                throw new ServerApiException(ApiErrorCode.UNAUTHORIZED, "PowerFlex Gateway API call unauthorized, please check the provided settings");
            }
            LOG.debug("PowerFlex Gateway API call unauthorized. Current token might be invalid, renew the session." + getConnectionManagerStats());
            return true;
        }
        return false;
    }

    private void checkResponseOK(final HttpResponse response) {
        if (isNullResponse(response)) {
            return;
        }

        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_NO_CONTENT) {
            LOG.warn("Requested resource does not exist");
            return;
        }

        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_BAD_REQUEST) {
            throw new ServerApiException(ApiErrorCode.MALFORMED_PARAMETER_ERROR, "Bad API request");
        }

        if (!(response.getStatusLine().getStatusCode() == HttpStatus.SC_OK ||
                response.getStatusLine().getStatusCode() == HttpStatus.SC_ACCEPTED)) {
            String responseBody = response.toString();
            try {
                responseBody = EntityUtils.toString(response.getEntity());
            } catch (IOException ignored) {
            }
            LOG.debug("HTTP request failed, status code: " + response.getStatusLine().getStatusCode() + ", response: "
                    + responseBody + getConnectionManagerStats());
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "API failed due to: " + responseBody);
        }
    }

    private void checkResponseTimeOut(final Exception e) {
        if (e instanceof ConnectTimeoutException || e instanceof SocketTimeoutException) {
            throw new ServerApiException(ApiErrorCode.RESOURCE_UNAVAILABLE_ERROR, "Gateway API operation timed out, please try again.");
        }
    }

    private <T> T get(final String path, final Class<T> type) {
        return get(path, type, true);
    }

    private <T> T get(final String path, final Class<T> type, final boolean renewAndRetryOnAuthFailure) {
        renewClientSessionOnExpiry();
        HttpResponse response = null;
        boolean responseConsumed = false;
        try {
            while (authenticating); // wait for authentication request (if any) to complete (and to pick the new session key)
            final HttpGet request = new HttpGet(apiURI.toString() + path);
            request.setHeader(HttpHeaders.AUTHORIZATION, "Basic " + Base64.getEncoder().encodeToString((this.username + ":" + this.sessionKey).getBytes()));
            LOG.debug("Sending GET request: " + request.toString());
            response = httpClient.execute(request);
            String responseStatus = (!isNullResponse(response)) ? (response.getStatusLine().getStatusCode() + " " + response.getStatusLine().getReasonPhrase()) : "nil";
            LOG.debug("Received response: " + responseStatus + ", for the sent GET request: " + request.toString());
            if (checkAuthFailure(response, renewAndRetryOnAuthFailure)) {
                EntityUtils.consumeQuietly(response.getEntity());
                responseConsumed = true;

                authenticate();
                return get(path, type, false);
            }
            return processResponse(response, type);
        }  catch (final IOException e) {
            LOG.error("Failed in GET method due to: " + e.getMessage() + getConnectionManagerStats(), e);
            checkResponseTimeOut(e);
        } finally {
            if (!responseConsumed && response != null) {
                EntityUtils.consumeQuietly(response.getEntity());
            }
        }
        return null;
    }

    private <T> T post(final String path, final Object obj, final Class<T> type) {
        return post(path, obj, type, true);
    }

    private <T> T post(final String path, final Object obj, final Class<T> type, final boolean renewAndRetryOnAuthFailure) {
        renewClientSessionOnExpiry();
        HttpResponse response = null;
        boolean responseConsumed = false;
        try {
            while (authenticating); // wait for authentication request (if any) to complete (and to pick the new session key)
            final HttpPost request = new HttpPost(apiURI.toString() + path);
            request.setHeader(HttpHeaders.AUTHORIZATION, "Basic " + Base64.getEncoder().encodeToString((this.username + ":" + this.sessionKey).getBytes()));
            request.setHeader("Content-type", "application/json");
            if (obj != null) {
                if (obj instanceof String) {
                    request.setEntity(new StringEntity((String) obj));
                } else {
                    JsonMapper mapper = new JsonMapper();
                    mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
                    String json = mapper.writer().writeValueAsString(obj);
                    request.setEntity(new StringEntity(json));
                }
            }
            LOG.debug("Sending POST request: " + request.toString());
            response = httpClient.execute(request);
            String responseStatus = (!isNullResponse(response)) ? (response.getStatusLine().getStatusCode() + " " + response.getStatusLine().getReasonPhrase()) : "nil";
            LOG.debug("Received response: " + responseStatus + ", for the sent POST request: " + request.toString());
            if (checkAuthFailure(response, renewAndRetryOnAuthFailure)) {
                EntityUtils.consumeQuietly(response.getEntity());
                responseConsumed = true;

                authenticate();
                return post(path, obj, type, false);
            }
            return processResponse(response, type);
        } catch (final IOException e) {
            LOG.error("Failed in POST method due to: " + e.getMessage() + getConnectionManagerStats(), e);
            checkResponseTimeOut(e);
        } finally {
            if (!responseConsumed && response != null) {
                EntityUtils.consumeQuietly(response.getEntity());
            }
        }
        return null;
    }

    private <T> T processResponse(HttpResponse response, final Class<T> type) throws IOException {
        if (isNullResponse(response)) {
            return null;
        }

        checkResponseOK(response);
        synchronized (this) {
            lastUsedTime = System.currentTimeMillis();
        }
        if (type == Boolean.class) {
            return (T) Boolean.TRUE;
        } else if (type == String.class) {
            return (T) EntityUtils.toString(response.getEntity());
        } else if (type == JsonNode.class) {
            return (T) new ObjectMapper().readTree(response.getEntity().getContent());
        } else {
            ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            return mapper.readValue(response.getEntity().getContent(), type);
        }
    }

    //////////////////////////////////////////////////
    //////////////// Volume APIs /////////////////////
    //////////////////////////////////////////////////

    @Override
    public Volume createVolume(final String name, final String storagePoolId,
                               final Integer sizeInGb, final Storage.ProvisioningType volumeType) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(name), "Volume name cannot be null");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(storagePoolId), "Storage pool id cannot be null");
        Preconditions.checkArgument(sizeInGb != null && sizeInGb > 0, "Size(GB) must be greater than 0");

        Volume newVolume = new Volume();
        newVolume.setName(name);
        newVolume.setStoragePoolId(storagePoolId);
        newVolume.setVolumeSizeInGb(sizeInGb);
        if (Storage.ProvisioningType.FAT.equals(volumeType)) {
            newVolume.setVolumeType(Volume.VolumeType.ThickProvisioned);
        } else {
            newVolume.setVolumeType(Volume.VolumeType.ThinProvisioned);
        }
        // The basic allocation granularity is 8GB. The volume size will be rounded up.
        Volume newVolumeObject = post("/types/Volume/instances", newVolume, Volume.class);
        return getVolume(newVolumeObject.getId());
    }

    @Override
    public List<Volume> listVolumes() {
        Volume[] volumes = get("/types/Volume/instances", Volume[].class);
        if (volumes != null) {
            return Arrays.asList(volumes);
        }
        return new ArrayList<>();
    }

    @Override
    public List<Volume> listSnapshotVolumes() {
        List<Volume> volumes = listVolumes();
        List<Volume> snapshotVolumes = new ArrayList<>();
        if (volumes != null && !volumes.isEmpty()) {
            for (Volume volume : volumes) {
                if (volume != null && volume.getVolumeType() == Volume.VolumeType.Snapshot) {
                    snapshotVolumes.add(volume);
                }
            }
        }
        return snapshotVolumes;
    }

    @Override
    public Volume getVolume(String volumeId) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(volumeId), "Volume id cannot be null");
        return get("/instances/Volume::" + volumeId, Volume.class);
    }

    @Override
    public Volume getVolumeByName(String name) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(name), "Volume name cannot be null");

        Volume searchVolume = new Volume();
        searchVolume.setName(name);
        String volumeId = post("/types/Volume/instances/action/queryIdByKey", searchVolume, String.class);
        if (!Strings.isNullOrEmpty(volumeId)) {
            return getVolume(volumeId.replace("\"", ""));
        }
        return null;
    }

    @Override
    public boolean renameVolume(final String volumeId, final String newName) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(volumeId), "Volume id cannot be null");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(newName), "New name for volume cannot be null");

        Boolean renameVolumeStatus = post(
                "/instances/Volume::" + volumeId + "/action/setVolumeName",
                String.format("{\"newName\":\"%s\"}", newName), Boolean.class);
        if (renameVolumeStatus != null) {
            return renameVolumeStatus;
        }
        return false;
    }

    @Override
    public Volume resizeVolume(final String volumeId, final Integer sizeInGB) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(volumeId), "Volume id cannot be null");
        Preconditions.checkArgument(sizeInGB != null && (sizeInGB > 0 && sizeInGB % 8 == 0),
                "Size(GB) must be greater than 0 and in granularity of 8");

        // Volume capacity can only be increased. sizeInGB must be a positive number in granularity of 8 GB.
        Boolean resizeVolumeStatus = post(
                "/instances/Volume::" + volumeId + "/action/setVolumeSize",
                String.format("{\"sizeInGB\":\"%s\"}", sizeInGB.toString()), Boolean.class);
        if (resizeVolumeStatus != null && resizeVolumeStatus.booleanValue()) {
            return getVolume(volumeId);
        }
        return null;
    }

    @Override
    public Volume cloneVolume(final String sourceVolumeId, final String destVolumeName) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(sourceVolumeId), "Source volume id cannot be null");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(destVolumeName), "Dest volume name cannot be null");

        Map<String, String> snapshotMap = new HashMap<>();
        snapshotMap.put(sourceVolumeId, destVolumeName);
        takeSnapshot(snapshotMap);
        return getVolumeByName(destVolumeName);
    }

    @Override
    public SnapshotGroup takeSnapshot(final Map<String, String> srcVolumeDestSnapshotMap) {
        Preconditions.checkArgument(srcVolumeDestSnapshotMap != null && !srcVolumeDestSnapshotMap.isEmpty(), "srcVolumeDestSnapshotMap cannot be null");

        final List<SnapshotDef> defs = new ArrayList<>();
        for (final String volumeId : srcVolumeDestSnapshotMap.keySet()) {
            final SnapshotDef snapshotDef = new SnapshotDef();
            snapshotDef.setVolumeId(volumeId);
            String snapshotName = srcVolumeDestSnapshotMap.get(volumeId);
            if (!Strings.isNullOrEmpty(snapshotName)) {
                snapshotDef.setSnapshotName(srcVolumeDestSnapshotMap.get(volumeId));
            }
            defs.add(snapshotDef);
        }
        final SnapshotDefs snapshotDefs = new SnapshotDefs();
        snapshotDefs.setSnapshotDefs(defs.toArray(new SnapshotDef[0]));
        return post("/instances/System/action/snapshotVolumes", snapshotDefs, SnapshotGroup.class);
    }

    @Override
    public boolean revertSnapshot(final String systemId, final Map<String, String> srcSnapshotDestVolumeMap) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(systemId), "System id cannot be null");
        Preconditions.checkArgument(srcSnapshotDestVolumeMap != null && !srcSnapshotDestVolumeMap.isEmpty(), "srcSnapshotDestVolumeMap cannot be null");

        //  Take group snapshot (needs additional storage pool capacity till revert operation) to keep the last state of all volumes ???
        //  and delete the group snapshot after revert operation
        //  If revert snapshot failed for any volume, use the group snapshot, to revert volumes to last state
        Map<String, String> srcVolumeDestSnapshotMap = new HashMap<>();
        List<String> originalVolumeIds = new ArrayList<>();
        for (final String sourceSnapshotVolumeId : srcSnapshotDestVolumeMap.keySet()) {
            String destVolumeId = srcSnapshotDestVolumeMap.get(sourceSnapshotVolumeId);
            srcVolumeDestSnapshotMap.put(destVolumeId, "");
            originalVolumeIds.add(destVolumeId);
        }
        SnapshotGroup snapshotGroup = takeSnapshot(srcVolumeDestSnapshotMap);
        if (snapshotGroup == null) {
            throw new CloudRuntimeException("Failed to snapshot the last vm state");
        }

        boolean revertSnapshotResult = true;
        int revertStatusIndex = -1;

        try {
            // non-atomic operation, try revert each volume
            for (final String sourceSnapshotVolumeId : srcSnapshotDestVolumeMap.keySet()) {
                String destVolumeId = srcSnapshotDestVolumeMap.get(sourceSnapshotVolumeId);
                boolean revertStatus = revertSnapshot(sourceSnapshotVolumeId, destVolumeId);
                if (!revertStatus) {
                    revertSnapshotResult = false;
                    LOG.warn("Failed to revert snapshot for volume id: " + sourceSnapshotVolumeId);
                    throw new CloudRuntimeException("Failed to revert snapshot for volume id: " + sourceSnapshotVolumeId);
                } else {
                    revertStatusIndex++;
                }
            }
        } catch (final Exception e) {
            LOG.error("Failed to revert vm snapshot due to: " + e.getMessage(), e);
            throw new CloudRuntimeException("Failed to revert vm snapshot due to: " + e.getMessage());
        } finally {
            if (!revertSnapshotResult) {
                //revert to volume with last state and delete the snapshot group, for already reverted volumes
                List<String> volumesWithLastState = snapshotGroup.getVolumeIds();
                for (int index = revertStatusIndex; index >= 0; index--) {
                    // Handling failure for revert again will become recursive ???
                    revertSnapshot(volumesWithLastState.get(index), originalVolumeIds.get(index));
                }
            }
            deleteSnapshotGroup(systemId, snapshotGroup.getSnapshotGroupId());
        }

        return revertSnapshotResult;
    }

    @Override
    public int deleteSnapshotGroup(final String systemId, final String snapshotGroupId) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(systemId), "System id cannot be null");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(snapshotGroupId), "Snapshot group id cannot be null");

        JsonNode node = post(
                "/instances/System::" + systemId + "/action/removeConsistencyGroupSnapshots",
                String.format("{\"snapGroupId\":\"%s\"}", snapshotGroupId), JsonNode.class);
        if (node != null) {
            JsonNode noOfVolumesNode = node.get("numberOfVolumes");
            return noOfVolumesNode.asInt();
        }
        return -1;
    }

    @Override
    public Volume takeSnapshot(final String volumeId, final String snapshotVolumeName) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(volumeId), "Volume id cannot be null");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(snapshotVolumeName), "Snapshot name cannot be null");

        final SnapshotDef[] snapshotDef = new SnapshotDef[1];
        snapshotDef[0] = new SnapshotDef();
        snapshotDef[0].setVolumeId(volumeId);
        snapshotDef[0].setSnapshotName(snapshotVolumeName);
        final SnapshotDefs snapshotDefs = new SnapshotDefs();
        snapshotDefs.setSnapshotDefs(snapshotDef);

        SnapshotGroup snapshotGroup = post("/instances/System/action/snapshotVolumes", snapshotDefs, SnapshotGroup.class);
        if (snapshotGroup != null) {
            List<String> volumeIds = snapshotGroup.getVolumeIds();
            if (volumeIds != null && !volumeIds.isEmpty()) {
                return getVolume(volumeIds.get(0));
            }
        }
        return null;
    }

    @Override
    public boolean revertSnapshot(final String sourceSnapshotVolumeId, final String destVolumeId) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(sourceSnapshotVolumeId), "Source snapshot volume id cannot be null");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(destVolumeId), "Destination volume id cannot be null");

        Volume sourceSnapshotVolume = getVolume(sourceSnapshotVolumeId);
        if (sourceSnapshotVolume == null) {
            throw new CloudRuntimeException("Source snapshot volume: " + sourceSnapshotVolumeId + " doesn't exists");
        }

        Volume destVolume = getVolume(destVolumeId);
        if (sourceSnapshotVolume == null) {
            throw new CloudRuntimeException("Destination volume: " + destVolumeId + " doesn't exists");
        }

        if (!sourceSnapshotVolume.getVtreeId().equals(destVolume.getVtreeId())) {
            throw new CloudRuntimeException("Unable to revert, source snapshot volume and destination volume doesn't belong to same volume tree");
        }

        Boolean overwriteVolumeContentStatus = post(
                "/instances/Volume::" + destVolumeId + "/action/overwriteVolumeContent",
                String.format("{\"srcVolumeId\":\"%s\",\"allowOnExtManagedVol\":\"TRUE\"}", sourceSnapshotVolumeId), Boolean.class);
        if (overwriteVolumeContentStatus != null) {
            return overwriteVolumeContentStatus;
        }
        return false;
    }

    @Override
    public boolean mapVolumeToSdc(final String volumeId, final String sdcId) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(volumeId), "Volume id cannot be null");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(sdcId), "Sdc Id cannot be null");

        if (isVolumeMappedToSdc(volumeId, sdcId)) {
            return true;
        }

        Boolean mapVolumeToSdcStatus = post(
                "/instances/Volume::" + volumeId + "/action/addMappedSdc",
                String.format("{\"sdcId\":\"%s\",\"allowMultipleMappings\":\"TRUE\"}", sdcId), Boolean.class);
        if (mapVolumeToSdcStatus != null) {
            return mapVolumeToSdcStatus;
        }
        return false;
    }

    @Override
    public boolean mapVolumeToSdcWithLimits(final String volumeId, final String sdcId, final Long iopsLimit, final Long bandwidthLimitInKbps) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(volumeId), "Volume id cannot be null");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(sdcId), "Sdc Id cannot be null");
        Preconditions.checkArgument(iopsLimit != null && (iopsLimit == 0 || iopsLimit > 10),
                "IOPS limit must be 0 (unlimited) or greater than 10");
        Preconditions.checkArgument(bandwidthLimitInKbps != null && (bandwidthLimitInKbps == 0 || (bandwidthLimitInKbps > 0 && bandwidthLimitInKbps % 1024 == 0)),
                "Bandwidth limit(Kbps) must be 0 (unlimited) or in granularity of 1024");

        if (mapVolumeToSdc(volumeId, sdcId)) {
            long iopsLimitVal = 0;
            if (iopsLimit != null && iopsLimit.longValue() > 0) {
                iopsLimitVal = iopsLimit.longValue();
            }

            long bandwidthLimitInKbpsVal = 0;
            if (bandwidthLimitInKbps != null && bandwidthLimitInKbps.longValue() > 0) {
                bandwidthLimitInKbpsVal = bandwidthLimitInKbps.longValue();
            }

            Boolean setVolumeSdcLimitsStatus = post(
                    "/instances/Volume::" + volumeId + "/action/setMappedSdcLimits",
                    String.format("{\"sdcId\":\"%s\",\"bandwidthLimitInKbps\":\"%d\",\"iopsLimit\":\"%d\"}", sdcId, bandwidthLimitInKbpsVal, iopsLimitVal), Boolean.class);
            if (setVolumeSdcLimitsStatus != null) {
                return setVolumeSdcLimitsStatus;
            }
        }
        return false;
    }

    @Override
    public boolean unmapVolumeFromSdc(final String volumeId, final String sdcId) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(volumeId), "Volume id cannot be null");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(sdcId), "Sdc Id cannot be null");

        if (isVolumeMappedToSdc(volumeId, sdcId)) {
            Boolean unmapVolumeFromSdcStatus = post(
                    "/instances/Volume::" + volumeId + "/action/removeMappedSdc",
                    String.format("{\"sdcId\":\"%s\",\"skipApplianceValidation\":\"TRUE\"}", sdcId), Boolean.class);
            if (unmapVolumeFromSdcStatus != null) {
                return unmapVolumeFromSdcStatus;
            }
        }
        return false;
    }

    @Override
    public boolean unmapVolumeFromAllSdcs(final String volumeId) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(volumeId), "Volume id cannot be null");

        Volume volume = getVolume(volumeId);
        if (volume == null) {
            return false;
        }

        List<SdcMappingInfo> mappedSdcList = volume.getMappedSdcList();
        if (mappedSdcList == null || mappedSdcList.isEmpty()) {
            return true;
        }

        Boolean unmapVolumeFromAllSdcsStatus = post(
                "/instances/Volume::" + volumeId + "/action/removeMappedSdc",
                "{\"allSdcs\": \"\"}", Boolean.class);
        if (unmapVolumeFromAllSdcsStatus != null) {
            return unmapVolumeFromAllSdcsStatus;
        }
        return false;
    }

    @Override
    public boolean isVolumeMappedToSdc(final String volumeId, final String sdcId) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(volumeId), "Volume id cannot be null");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(sdcId), "Sdc Id cannot be null");

        if (Strings.isNullOrEmpty(volumeId) || Strings.isNullOrEmpty(sdcId)) {
            return false;
        }

        Volume volume = getVolume(volumeId);
        if (volume == null) {
            return false;
        }

        List<SdcMappingInfo> mappedSdcList = volume.getMappedSdcList();
        if (mappedSdcList != null && !mappedSdcList.isEmpty()) {
            for (SdcMappingInfo mappedSdc : mappedSdcList) {
                if (sdcId.equalsIgnoreCase(mappedSdc.getSdcId())) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public boolean deleteVolume(final String volumeId) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(volumeId), "Volume id cannot be null");

        try {
            unmapVolumeFromAllSdcs(volumeId);
        } catch (Exception ignored) {}
        Boolean removeVolumeStatus = post(
                "/instances/Volume::" + volumeId + "/action/removeVolume",
                "{\"removeMode\":\"ONLY_ME\"}", Boolean.class);
        if (removeVolumeStatus != null) {
            return removeVolumeStatus;
        }
        return false;
    }

    @Override
    public boolean migrateVolume(final String srcVolumeId, final String destPoolId, final int timeoutInSecs) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(srcVolumeId), "src volume id cannot be null");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(destPoolId), "dest pool id cannot be null");
        Preconditions.checkArgument(timeoutInSecs > 0, "timeout must be greater than 0");

        try {
            Volume volume = getVolume(srcVolumeId);
            if (volume == null || Strings.isNullOrEmpty(volume.getVtreeId())) {
                LOG.warn("Couldn't find the volume(-tree), can not migrate the volume " + srcVolumeId);
                return false;
            }

            String srcPoolId = volume.getStoragePoolId();
            LOG.debug("Migrating the volume: " + srcVolumeId + " on the src pool: " + srcPoolId + " to the dest pool: " + destPoolId +
                    " in the same PowerFlex cluster");

            post("/instances/Volume::" + srcVolumeId + "/action/migrateVTree",
                    String.format("{\"destSPId\":\"%s\"}", destPoolId), Boolean.class);

            LOG.debug("Wait until the migration is complete for the volume: " + srcVolumeId);
            long migrationStartTime = System.currentTimeMillis();
            boolean status = waitForVolumeMigrationToComplete(volume.getVtreeId(), timeoutInSecs);

            // Check volume storage pool and migration status
            // volume, v-tree, snapshot ids remains same after the migration
            volume = getVolume(srcVolumeId);
            if (volume == null || volume.getStoragePoolId() == null) {
                LOG.warn("Couldn't get the volume: " + srcVolumeId + " details after migration");
                return status;
            } else {
                String volumeOnPoolId = volume.getStoragePoolId();
                // confirm whether the volume is on the dest storage pool or not
                if (status && destPoolId.equalsIgnoreCase(volumeOnPoolId)) {
                    LOG.debug("Migration success for the volume: " + srcVolumeId);
                    return true;
                } else {
                    try {
                        // Check and pause any migration activity on the volume
                        status = false;
                        VTreeMigrationInfo.MigrationStatus migrationStatus = getVolumeTreeMigrationStatus(volume.getVtreeId());
                        if (migrationStatus != null && migrationStatus != VTreeMigrationInfo.MigrationStatus.NotInMigration) {
                            long timeElapsedInSecs = (System.currentTimeMillis() - migrationStartTime) / 1000;
                            int timeRemainingInSecs = (int) (timeoutInSecs - timeElapsedInSecs);
                            if (timeRemainingInSecs > (timeoutInSecs / 2)) {
                                // Try to pause gracefully (continue the migration) if atleast half of the time is remaining
                                pauseVolumeMigration(srcVolumeId, false);
                                status = waitForVolumeMigrationToComplete(volume.getVtreeId(), timeRemainingInSecs);
                            }
                        }

                        if (!status) {
                            rollbackVolumeMigration(srcVolumeId);
                        }

                        return status;
                    } catch (Exception ex) {
                        LOG.warn("Exception on pause/rollback migration of the volume: " + srcVolumeId + " - " + ex.getLocalizedMessage());
                    }
                }
            }
        } catch (final Exception e) {
            LOG.error("Failed to migrate PowerFlex volume due to: " + e.getMessage(), e);
            throw new CloudRuntimeException("Failed to migrate PowerFlex volume due to: " + e.getMessage());
        }

        LOG.debug("Migration failed for the volume: " + srcVolumeId);
        return false;
    }

    private boolean waitForVolumeMigrationToComplete(final String volumeTreeId, int waitTimeoutInSecs) {
        LOG.debug("Waiting for the migration to complete for the volume-tree " + volumeTreeId);
        if (Strings.isNullOrEmpty(volumeTreeId)) {
            LOG.warn("Invalid volume-tree id, unable to check the migration status of the volume-tree " + volumeTreeId);
            return false;
        }

        int delayTimeInSecs = 3;
        while (waitTimeoutInSecs > 0) {
            try {
                // Wait and try after few secs (reduce no. of client API calls to check the migration status) and return after migration is complete
                Thread.sleep(delayTimeInSecs * 1000);

                VTreeMigrationInfo.MigrationStatus migrationStatus = getVolumeTreeMigrationStatus(volumeTreeId);
                if (migrationStatus != null && migrationStatus == VTreeMigrationInfo.MigrationStatus.NotInMigration) {
                    LOG.debug("Migration completed for the volume-tree " + volumeTreeId);
                    return true;
                }
            } catch (Exception ex) {
                LOG.warn("Exception while checking for migration status of the volume-tree: " + volumeTreeId + " - " + ex.getLocalizedMessage());
                // don't do anything
            } finally {
                waitTimeoutInSecs = waitTimeoutInSecs - delayTimeInSecs;
            }
        }

        LOG.debug("Unable to complete the migration for the volume-tree " + volumeTreeId);
        return false;
    }

    private VTreeMigrationInfo.MigrationStatus getVolumeTreeMigrationStatus(final String volumeTreeId) {
        if (Strings.isNullOrEmpty(volumeTreeId)) {
            LOG.warn("Invalid volume-tree id, unable to get the migration status of the volume-tree " + volumeTreeId);
            return null;
        }

        VTree volumeTree = get("/instances/VTree::" + volumeTreeId, VTree.class);
        if (volumeTree != null && volumeTree.getVTreeMigrationInfo() != null) {
            return volumeTree.getVTreeMigrationInfo().getMigrationStatus();
        }
        return null;
    }

    private boolean rollbackVolumeMigration(final String srcVolumeId) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(srcVolumeId), "src volume id cannot be null");

        Volume volume = getVolume(srcVolumeId);
        if (volume == null) {
            LOG.warn("Unable to rollback volume migration, couldn't get details for the volume: " + srcVolumeId);
            return false;
        }

        VTreeMigrationInfo.MigrationStatus migrationStatus = getVolumeTreeMigrationStatus(volume.getVtreeId());
        if (migrationStatus != null && migrationStatus == VTreeMigrationInfo.MigrationStatus.NotInMigration) {
            LOG.debug("Volume: " + srcVolumeId + " is not migrating, no need to rollback");
            return true;
        }

        pauseVolumeMigration(srcVolumeId, true); // Pause forcefully
        // Wait few secs for volume migration to change to Paused state
        boolean paused = false;
        int retryCount = 3;
        while (retryCount > 0) {
            try {
                Thread.sleep(3000); // Try after few secs
                migrationStatus = getVolumeTreeMigrationStatus(volume.getVtreeId()); // Get updated migration status
                if (migrationStatus != null && migrationStatus == VTreeMigrationInfo.MigrationStatus.Paused) {
                    LOG.debug("Migration for the volume: " + srcVolumeId + " paused");
                    paused = true;
                    break;
                }
            } catch (Exception ex) {
                LOG.warn("Exception while checking for migration pause status of the volume: " + srcVolumeId + " - " + ex.getLocalizedMessage());
                // don't do anything
            } finally {
                retryCount--;
            }
        }

        if (paused) {
            // Rollback migration to the src pool (should be quick)
            Boolean migrateVTreeStatus = post(
                    "/instances/Volume::" + srcVolumeId + "/action/migrateVTree",
                    String.format("{\"destSPId\":\"%s\"}", volume.getStoragePoolId()), Boolean.class);
            if (migrateVTreeStatus != null) {
                return migrateVTreeStatus;
            }
        } else {
            LOG.warn("Migration for the volume: " + srcVolumeId + " didn't pause, couldn't rollback");
        }
        return false;
    }

    private boolean pauseVolumeMigration(final String volumeId, final boolean forced) {
        if (Strings.isNullOrEmpty(volumeId)) {
            LOG.warn("Invalid Volume Id, Unable to pause migration of the volume " + volumeId);
            return false;
        }

        // When paused gracefully, all data currently being moved is allowed to complete the migration.
        // When paused forcefully, migration of unfinished data is aborted and data is left at the source, if possible.
        // Pausing forcefully carries a potential risk to data.
        Boolean pauseVTreeMigrationStatus = post(
                "/instances/Volume::" + volumeId + "/action/pauseVTreeMigration",
                String.format("{\"pauseType\":\"%s\"}", forced ? "Forcefully" : "Gracefully"), Boolean.class);
        if (pauseVTreeMigrationStatus != null) {
            return pauseVTreeMigrationStatus;
        }
        return false;
    }

    ///////////////////////////////////////////////////////
    //////////////// StoragePool APIs /////////////////////
    ///////////////////////////////////////////////////////

    @Override
    public List<StoragePool> listStoragePools() {
        StoragePool[] pools = get("/types/StoragePool/instances", StoragePool[].class);
        if (pools != null) {
            return Arrays.asList(pools);
        }
        return new ArrayList<>();
    }

    @Override
    public StoragePool getStoragePool(String poolId) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(poolId), "Storage pool id cannot be null");
        return get("/instances/StoragePool::" + poolId, StoragePool.class);
    }

    @Override
    public StoragePoolStatistics getStoragePoolStatistics(String poolId) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(poolId), "Storage pool id cannot be null");
        return get("/instances/StoragePool::" + poolId + "/relationships/Statistics", StoragePoolStatistics.class);
    }

    @Override
    public VolumeStatistics getVolumeStatistics(String volumeId) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(volumeId), "Volume id cannot be null");

        Volume volume = getVolume(volumeId);
        if (volume != null) {
            String volumeTreeId = volume.getVtreeId();
            if (!Strings.isNullOrEmpty(volumeTreeId)) {
                VolumeStatistics volumeStatistics = get("/instances/VTree::" + volumeTreeId + "/relationships/Statistics", VolumeStatistics.class);
                if (volumeStatistics != null) {
                    volumeStatistics.setAllocatedSizeInKb(volume.getSizeInKb());
                    return volumeStatistics;
                }
            }
        }

        return null;
    }

    @Override
    public String getSystemId(String protectionDomainId) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(protectionDomainId), "Protection domain id cannot be null");

        ProtectionDomain protectionDomain = get("/instances/ProtectionDomain::" + protectionDomainId, ProtectionDomain.class);
        if (protectionDomain != null) {
            return protectionDomain.getSystemId();
        }
        return null;
    }

    @Override
    public List<Volume> listVolumesInStoragePool(String poolId) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(poolId), "Storage pool id cannot be null");

        Volume[] volumes = get("/instances/StoragePool::" + poolId + "/relationships/Volume", Volume[].class);
        if (volumes != null) {
            return Arrays.asList(volumes);
        }
        return new ArrayList<>();
    }

    ///////////////////////////////////////////////
    //////////////// SDC APIs /////////////////////
    ///////////////////////////////////////////////

    @Override
    public List<Sdc> listSdcs() {
        Sdc[] sdcs = get("/types/Sdc/instances", Sdc[].class);
        if (sdcs != null) {
            return Arrays.asList(sdcs);
        }
        return new ArrayList<>();
    }

    @Override
    public Sdc getSdc(String sdcId) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(sdcId), "Sdc id cannot be null");
        return get("/instances/Sdc::" + sdcId, Sdc.class);
    }

    @Override
    public Sdc getSdcByIp(String ipAddress) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(ipAddress), "IP address cannot be null");

        String sdcId = post("/types/Sdc/instances/action/queryIdByKey", String.format("{\"ip\":\"%s\"}", ipAddress), String.class);
        if (!Strings.isNullOrEmpty(sdcId)) {
            return getSdc(sdcId.replace("\"", ""));
        }
        return null;
    }

    @Override
    public Sdc getConnectedSdcByIp(String ipAddress) {
        Sdc sdc = getSdcByIp(ipAddress);
        if (sdc != null && MDM_CONNECTED_STATE.equalsIgnoreCase(sdc.getMdmConnectionState())) {
            return sdc;
        }

        return null;
    }

    @Override
    public List<String> listConnectedSdcIps() {
        List<String> sdcIps = new ArrayList<>();
        List<Sdc> sdcs = listSdcs();
        if(sdcs != null) {
            for (Sdc sdc : sdcs) {
                if (MDM_CONNECTED_STATE.equalsIgnoreCase(sdc.getMdmConnectionState())) {
                    sdcIps.add(sdc.getSdcIp());
                }
            }
        }

        return sdcIps;
    }

    @Override
    public boolean isSdcConnected(String ipAddress) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(ipAddress), "IP address cannot be null");

        List<Sdc> sdcs = listSdcs();
        if(sdcs != null) {
            for (Sdc sdc : sdcs) {
                if (ipAddress.equalsIgnoreCase(sdc.getSdcIp()) && MDM_CONNECTED_STATE.equalsIgnoreCase(sdc.getMdmConnectionState())) {
                    return true;
                }
            }
        }

        return false;
    }

    private String getConnectionManagerStats() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n").append("Client Connection Manager Stats => ");
        if (connectionManager != null) {
            sb.append("MaxTotal: ").append(connectionManager.getMaxTotal()).append(", ");
            sb.append("DefaultMaxPerRoute: ").append(connectionManager.getDefaultMaxPerRoute());

            PoolStats poolStats = connectionManager.getTotalStats();
            if (poolStats != null) {
                sb.append(", ");
                sb.append("Available: ").append(poolStats.getAvailable()).append(", ");
                sb.append("Leased: ").append(poolStats.getLeased()).append(", ");
                sb.append("Max: ").append(poolStats.getMax()).append(", ");
                sb.append("Pending: ").append(poolStats.getPending());
            }
        } else {
            sb.append("NULL");
        }

        sb.append("\n");
        return sb.toString();
    }
}
