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
package org.apache.cloudstack.storage.datastore.adapter.flasharray;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;

import org.apache.http.Header;
import org.apache.http.NameValuePair;
import org.apache.cloudstack.storage.datastore.adapter.ProviderAdapter;
import org.apache.cloudstack.storage.datastore.adapter.ProviderAdapterContext;
import org.apache.cloudstack.storage.datastore.adapter.ProviderAdapterDataObject;
import org.apache.cloudstack.storage.datastore.adapter.ProviderAdapterDiskOffering;
import org.apache.cloudstack.storage.datastore.adapter.ProviderSnapshot;
import org.apache.cloudstack.storage.datastore.adapter.ProviderVolume;
import org.apache.cloudstack.storage.datastore.adapter.ProviderVolumeNamer;
import org.apache.cloudstack.storage.datastore.adapter.ProviderVolumeStats;
import org.apache.cloudstack.storage.datastore.adapter.ProviderVolumeStorageStats;
import org.apache.cloudstack.storage.datastore.adapter.ProviderVolume.AddressType;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.ssl.SSLContextBuilder;

import com.cloud.utils.exception.CloudRuntimeException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Array API
 */
public class FlashArrayAdapter implements ProviderAdapter {
    protected Logger logger = LogManager.getLogger(getClass());

    public static final String HOSTGROUP = "hostgroup";
    public static final String STORAGE_POD = "pod";
    public static final String KEY_TTL = "keyttl";
    public static final String CONNECT_TIMEOUT_MS = "connectTimeoutMs";
    public static final String POST_COPY_WAIT_MS = "postCopyWaitMs";
    public static final String API_LOGIN_VERSION = "apiLoginVersion";
    public static final String API_VERSION = "apiVersion";

    private static final long KEY_TTL_DEFAULT = (1000 * 60 * 14);
    private static final long CONNECT_TIMEOUT_MS_DEFAULT = 600000;
    private static final long POST_COPY_WAIT_MS_DEFAULT = 5000;
    private static final String API_LOGIN_VERSION_DEFAULT = "1.19";
    private static final String API_VERSION_DEFAULT = "2.23";

    static final ObjectMapper mapper = new ObjectMapper();
    public String pod = null;
    public String hostgroup = null;
    private String username;
    private String password;
    private String accessToken;
    private String url;
    private long keyExpiration = -1;
    private long keyTtl = KEY_TTL_DEFAULT;
    private long connTimeout = CONNECT_TIMEOUT_MS_DEFAULT;
    private long postCopyWait = POST_COPY_WAIT_MS_DEFAULT;
    private CloseableHttpClient _client = null;
    private boolean skipTlsValidation;
    private String apiLoginVersion = API_LOGIN_VERSION_DEFAULT;
    private String apiVersion = API_VERSION_DEFAULT;

    private Map<String, String> connectionDetails = null;

    protected FlashArrayAdapter(String url, Map<String, String> details) {
        this.url = url;
        this.connectionDetails = details;
        login();
    }

    @Override
    public ProviderVolume create(ProviderAdapterContext context, ProviderAdapterDataObject dataObject,
            ProviderAdapterDiskOffering offering, long size) {
        FlashArrayVolume request = new FlashArrayVolume();
        request.setExternalName(
                pod + "::" + ProviderVolumeNamer.generateObjectName(context, dataObject));
        request.setPodName(pod);
        request.setAllocatedSizeBytes(roundUp512Boundary(size));
        FlashArrayList<FlashArrayVolume> list = POST("/volumes?names=" + request.getExternalName() + "&overwrite=false",
                request, new TypeReference<FlashArrayList<FlashArrayVolume>>() {
                });

        return (ProviderVolume) getFlashArrayItem(list);
    }

    /**
     * Volumes must be added to a host set to be visable to the hosts.
     * the Hostset should contain all the hosts that are membrers of the zone or
     * cluster (depending on Cloudstack Storage Pool configuration)
     */
    @Override
    public String attach(ProviderAdapterContext context, ProviderAdapterDataObject dataObject, String hostname) {

        // should not happen but double check for sanity
        if (dataObject.getType() == ProviderAdapterDataObject.Type.SNAPSHOT) {
            throw new RuntimeException("This storage provider does not support direct attachments of snapshots to hosts");
        }

        String volumeName = normalizeName(pod, dataObject.getExternalName());
        try {
            FlashArrayList<FlashArrayConnection> list = null;
            FlashArrayHost host = getHost(hostname);
            if (host != null) {
                list = POST("/connections?host_names=" + host.getName() + "&volume_names=" + volumeName, null,
                    new TypeReference<FlashArrayList<FlashArrayConnection>>() {
                });
            }

            if (list == null || list.getItems() == null || list.getItems().size() == 0) {
                throw new RuntimeException("Volume attach did not return lun information");
            }

            FlashArrayConnection connection = (FlashArrayConnection) this.getFlashArrayItem(list);
            if (connection.getLun() == null) {
                throw new RuntimeException("Volume attach missing lun field");
            }

            return "" + connection.getLun();

        } catch (Throwable e) {
            // the volume is already attached. happens in some scenarios where orchestration
            // creates the volume before copying to it
            if (e.toString().contains("Connection already exists")) {
                FlashArrayList<FlashArrayConnection> list = GET("/connections?volume_names=" + volumeName,
                        new TypeReference<FlashArrayList<FlashArrayConnection>>() {
                        });
                if (list != null && list.getItems() != null) {
                    for (FlashArrayConnection conn : list.getItems()) {
                        if (conn.getHost() != null && conn.getHost().getName() != null &&
                            (conn.getHost().getName().equals(hostname) || conn.getHost().getName().equals(hostname.substring(0, hostname.indexOf('.')))) &&
                            conn.getLun() != null) {
                            return "" + conn.getLun();
                        }
                    }
                    throw new RuntimeException("Volume lun is not found in existing connection");
                } else {
                    throw new RuntimeException("Volume lun is not found in existing connection");
                }
            } else {
                throw e;
            }
        }
    }

    @Override
    public void detach(ProviderAdapterContext context, ProviderAdapterDataObject dataObject, String hostname) {
        String volumeName = normalizeName(pod, dataObject.getExternalName());
        // hostname is always provided by cloudstack, but we will detach from hostgroup
        // if this pool is configured to use hostgroup for attachments
        if (hostgroup != null) {
            DELETE("/connections?host_group_names=" + hostgroup + "&volume_names=" + volumeName);
        }

        FlashArrayHost host = getHost(hostname);
        if (host != null) {
            DELETE("/connections?host_names=" + host.getName() + "&volume_names=" + volumeName);
        }
    }

    @Override
    public void delete(ProviderAdapterContext context, ProviderAdapterDataObject dataObject) {
        // first make sure we are disconnected
        removeVlunsAll(context, pod, dataObject.getExternalName());
        String fullName = normalizeName(pod, dataObject.getExternalName());

        FlashArrayVolume volume = new FlashArrayVolume();

        // rename as we delete so it doesn't conflict if the template or volume is ever recreated
        // pure keeps the volume(s) around in a Destroyed bucket for a period of time post delete
        String timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new java.util.Date());
        volume.setExternalName(fullName + "-" + timestamp);

        try {
            PATCH("/volumes?names=" + fullName, volume, new TypeReference<FlashArrayList<FlashArrayVolume>>() {
            });

            // now delete it with new name
            volume.setDestroyed(true);

            PATCH("/volumes?names=" + fullName + "-" + timestamp, volume, new TypeReference<FlashArrayList<FlashArrayVolume>>() {
            });
        } catch (CloudRuntimeException e) {
            if (e.toString().contains("Volume does not exist")) {
                return;
            } else {
                throw e;
            }
        }
    }

    @Override
    public ProviderVolume getVolume(ProviderAdapterContext context, ProviderAdapterDataObject dataObject) {
        String externalName = dataObject.getExternalName();
        // if its not set, look for the generated name for some edge cases
        if (externalName == null) {
            externalName = pod + "::" + ProviderVolumeNamer.generateObjectName(context, dataObject);
        }
        FlashArrayVolume volume = null;
        try {
            volume = getVolume(externalName);
            // if we didn't get an address back its likely an empty object
            if (volume != null && volume.getAddress() == null) {
                return null;
            } else if (volume == null) {
                return null;
            }

            return volume;
        } catch (Exception e) {
            // assume any exception is a not found. Flash returns 400's for most errors
            return null;
        }
    }

    @Override
    public ProviderVolume getVolumeByAddress(ProviderAdapterContext context, AddressType addressType, String address) {
        // public FlashArrayVolume getVolumeByWwn(String wwn) {
        if (address == null || addressType == null) {
            throw new RuntimeException("Invalid search criteria provided for getVolumeByAddress");
        }

        // only support WWN type addresses at this time.
        if (!ProviderVolume.AddressType.FIBERWWN.equals(addressType)) {
            throw new RuntimeException(
                    "Invalid volume address type [" + addressType + "] requested for volume search");
        }

        // convert WWN to serial to search on. strip out WWN type # + Flash OUI value
        String serial = address.substring(FlashArrayVolume.PURE_OUI.length() + 1).toUpperCase();
        String query = "serial='" + serial + "'";

        FlashArrayVolume volume = null;
        try {
            FlashArrayList<FlashArrayVolume> list = GET("/volumes?filter=" + query,
                    new TypeReference<FlashArrayList<FlashArrayVolume>>() {
                    });

            // if we didn't get an address back its likely an empty object
            if (list == null || list.getItems() == null || list.getItems().size() == 0) {
                return null;
            }

            volume = (FlashArrayVolume) this.getFlashArrayItem(list);
            if (volume != null && volume.getAddress() == null) {
                return null;
            }

            return volume;
        } catch (Exception e) {
            // assume any exception is a not found. Flash returns 400's for most errors
            return null;
        }
    }

    @Override
    public void resize(ProviderAdapterContext context, ProviderAdapterDataObject dataObject, long newSizeInBytes) {
        // public void resizeVolume(String volumeNamespace, String volumeName, long
        // newSizeInBytes) {
        FlashArrayVolume volume = new FlashArrayVolume();
        volume.setAllocatedSizeBytes(roundUp512Boundary(newSizeInBytes));
        PATCH("/volumes?names=" + dataObject.getExternalName(), volume, null);
    }

    /**
     * Take a snapshot and return a Volume representing that snapshot
     *
     * @param volumeName
     * @param snapshotName
     * @return
     */
    @Override
    public ProviderSnapshot snapshot(ProviderAdapterContext context, ProviderAdapterDataObject sourceDataObject,
            ProviderAdapterDataObject targetDataObject) {
        // public FlashArrayVolume snapshotVolume(String volumeNamespace, String
        // volumeName, String snapshotName) {
        FlashArrayList<FlashArrayVolume> list = POST(
                "/volume-snapshots?source_names=" + sourceDataObject.getExternalName(), null,
                new TypeReference<FlashArrayList<FlashArrayVolume>>() {
                });

        return (FlashArrayVolume) getFlashArrayItem(list);
    }

    /**
     * Replaces the base volume with the given snapshot. Note this can only be done
     * when the snapshot and volume
     * are
     *
     * @param name
     * @return
     */
    @Override
    public ProviderVolume revert(ProviderAdapterContext context, ProviderAdapterDataObject snapshotDataObject) {
        // public void promoteSnapshot(String namespace, String snapshotName) {
        if (snapshotDataObject == null || snapshotDataObject.getExternalName() == null) {
            throw new RuntimeException("Snapshot revert not possible as an external snapshot name was not provided");
        }

        FlashArrayVolume snapshot = this.getSnapshot(snapshotDataObject.getExternalName());
        if (snapshot.getSource() == null) {
            throw new CloudRuntimeException("Snapshot source was not available from the storage array");
        }

        String origVolumeName = snapshot.getSource().getName();

        // now "create" a new volume with the snapshot volume as its source (basically a
        // Flash array copy)
        // and overwrite to true (volume already exists, we are recreating it)
        FlashArrayVolume input = new FlashArrayVolume();
        input.setExternalName(origVolumeName);
        input.setAllocatedSizeBytes(roundUp512Boundary(snapshot.getAllocatedSizeInBytes()));
        input.setSource(new FlashArrayVolumeSource(snapshot.getExternalName()));
        POST("/volumes?names=" + origVolumeName + "&overwrite=true", input, null);

        return this.getVolume(origVolumeName);
    }

    @Override
    public ProviderSnapshot getSnapshot(ProviderAdapterContext context, ProviderAdapterDataObject dataObject) {
        FlashArrayList<FlashArrayVolume> list = GET(
                "/volume-snapshots?names=" + dataObject.getExternalName(),
                new TypeReference<FlashArrayList<FlashArrayVolume>>() {
                });
        return (FlashArrayVolume) getFlashArrayItem(list);
    }

    @Override
    public ProviderVolume copy(ProviderAdapterContext context, ProviderAdapterDataObject sourceDataObject,
            ProviderAdapterDataObject destDataObject) {
        // private ManagedVolume copy(ManagedVolume sourceVolume, String destNamespace,
        // String destName) {
        if (sourceDataObject == null || sourceDataObject.getExternalName() == null
                || sourceDataObject.getType() == null) {
            throw new RuntimeException("Provided volume has no external source information");
        }

        if (destDataObject == null) {
            throw new RuntimeException("Provided volume target information was not provided");
        }

        if (destDataObject.getExternalName() == null) {
            // this means its a new volume? so our external name will be the Cloudstack UUID
            destDataObject
                    .setExternalName(ProviderVolumeNamer.generateObjectName(context, destDataObject));
        }

        FlashArrayVolume currentVol;
        if (sourceDataObject.getType().equals(ProviderAdapterDataObject.Type.SNAPSHOT)) {
            currentVol = getSnapshot(sourceDataObject.getExternalName());
        } else {
            currentVol = (FlashArrayVolume) this
                    .getFlashArrayItem(GET("/volumes?names=" + sourceDataObject.getExternalName(),
                            new TypeReference<FlashArrayList<FlashArrayVolume>>() {
                            }));
        }

        if (currentVol == null) {
            throw new RuntimeException("Unable to find current volume to copy from");
        }

        // now "create" a new volume with the snapshot volume as its source (basically a
        // Flash array copy)
        // and overwrite to true (volume already exists, we are recreating it)
        FlashArrayVolume payload = new FlashArrayVolume();
        payload.setExternalName(normalizeName(pod, destDataObject.getExternalName()));
        payload.setPodName(pod);
        payload.setAllocatedSizeBytes(roundUp512Boundary(currentVol.getAllocatedSizeInBytes()));
        payload.setSource(new FlashArrayVolumeSource(sourceDataObject.getExternalName()));
        FlashArrayList<FlashArrayVolume> list = POST(
                "/volumes?names=" + payload.getExternalName() + "&overwrite=true", payload,
                new TypeReference<FlashArrayList<FlashArrayVolume>>() {
                });
        FlashArrayVolume outVolume = (FlashArrayVolume) getFlashArrayItem(list);
        pause(postCopyWait);
        return outVolume;
    }

    private void pause(long period) {
        try {
            Thread.sleep(period);
        } catch (InterruptedException e) {

        }
    }

    public boolean supportsSnapshotConnection() {
        return false;
    }

    @Override
    public void refresh(Map<String, String> details) {
        this.connectionDetails = details;
        this.refreshSession(true);
    }

    @Override
    public void validate() {
        login();

        if (this.getVolumeNamespace(pod) == null) {
            throw new RuntimeException(
                    "Pod [" + pod + "] not found in FlashArray at [" + url + "], please validate configuration");
        }
    }

    @Override
    public void disconnect() {
        return;
    }

    @Override
    public ProviderVolumeStorageStats getManagedStorageStats() {
        FlashArrayPod pod = getVolumeNamespace(this.pod);
        // just in case
        if (pod == null || pod.getFootprint() == 0) {
            return null;
        }
        Long capacityBytes = pod.getQuotaLimit();
        Long usedBytes = pod.getQuotaLimit() - (pod.getQuotaLimit() - pod.getFootprint());
        ProviderVolumeStorageStats stats = new ProviderVolumeStorageStats();
        stats.setCapacityInBytes(capacityBytes);
        stats.setActualUsedInBytes(usedBytes);
        return stats;
    }

    @Override
    public ProviderVolumeStats getVolumeStats(ProviderAdapterContext context, ProviderAdapterDataObject dataObject) {
        ProviderVolume vol = getVolume(dataObject.getExternalName());
        Long usedBytes = vol.getUsedBytes();
        Long allocatedSizeInBytes = vol.getAllocatedSizeInBytes();
        if (usedBytes == null || allocatedSizeInBytes == null) {
            return null;
        }
        ProviderVolumeStats stats = new ProviderVolumeStats();
        stats.setAllocatedInBytes(allocatedSizeInBytes);
        stats.setActualUsedInBytes(usedBytes);
        return stats;
    }

    @Override
    public boolean canAccessHost(ProviderAdapterContext context, String hostname) {
        if (hostname == null) {
            throw new RuntimeException("Unable to validate host access because a hostname was not provided");
        }

        FlashArrayHost host = getHost(hostname);
        if (host != null) {
            return true;
        }

        return false;
    }

    private FlashArrayHost getHost(String hostname) {
        FlashArrayList<FlashArrayHost> list = null;

        try {
            list = GET("/hosts?names=" + hostname,
                new TypeReference<FlashArrayList<FlashArrayHost>>() {
                });
        } catch (Exception e) {

        }

        if (list == null) {
            if (hostname.indexOf('.') > 0) {
                list = GET("/hosts?names=" + hostname.substring(0, (hostname.indexOf('.'))),
                    new TypeReference<FlashArrayList<FlashArrayHost>>() {
                });
            }
        }
        return (FlashArrayHost) getFlashArrayItem(list);
    }

    private String getAccessToken() {
        return accessToken;
    }

    private synchronized void refreshSession(boolean force) {
        try {
            if (force || keyExpiration < System.currentTimeMillis()) {
                // close client to force connection reset on appliance -- not doing this can
                // result in NotAuthorized error...guessing
                _client.close();
                ;
                _client = null;
                login();
                keyExpiration = System.currentTimeMillis() + keyTtl;
            }
        } catch (Exception e) {
            // retry frequently but not every request to avoid DDOS on storage API
            logger.warn(
                    "Failed to refresh FlashArray API key for " + username + "@" + url + ", will retry in 5 seconds",
                    e);
            keyExpiration = System.currentTimeMillis() + (5 * 1000);
        }
    }

    /**
     * Login to the array and get an access token
     */
    private void login() {
        username = connectionDetails.get(ProviderAdapter.API_USERNAME_KEY);
        password = connectionDetails.get(ProviderAdapter.API_PASSWORD_KEY);
        String urlStr = connectionDetails.get(ProviderAdapter.API_URL_KEY);

        URL urlFull;
        try {
            urlFull = new URL(urlStr);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Invalid URL format: " + urlStr, e);
        }
        ;

        int port = urlFull.getPort();
        if (port <= 0) {
            port = 443;
        }
        this.url = urlFull.getProtocol() + "://" + urlFull.getHost() + ":" + port + urlFull.getPath();

        Map<String, String> queryParms = new HashMap<String, String>();
        if (urlFull.getQuery() != null) {
            String[] queryToks = urlFull.getQuery().split("&");
            for (String tok : queryToks) {
                if (tok.endsWith("=")) {
                    continue;
                }
                int i = tok.indexOf("=");
                if (i > 0) {
                    queryParms.put(tok.substring(0, i), tok.substring(i + 1));
                }
            }
        }

        pod = connectionDetails.get(FlashArrayAdapter.STORAGE_POD);
        if (pod == null) {
            pod = queryParms.get(FlashArrayAdapter.STORAGE_POD);
            if (pod == null) {
                throw new RuntimeException(
                        FlashArrayAdapter.STORAGE_POD + " paramater/option required to configure this storage pool");
            }
        }

        apiLoginVersion = connectionDetails.get(FlashArrayAdapter.API_LOGIN_VERSION);
        if (apiLoginVersion == null) {
            apiLoginVersion = queryParms.get(FlashArrayAdapter.API_LOGIN_VERSION);
            if (apiLoginVersion == null) {
                apiLoginVersion = API_LOGIN_VERSION_DEFAULT;
            }
        }

        apiVersion = connectionDetails.get(FlashArrayAdapter.API_VERSION);
        if (apiVersion == null) {
            apiVersion = queryParms.get(FlashArrayAdapter.API_VERSION);
            if (apiVersion == null) {
                apiVersion = API_VERSION_DEFAULT;
            }
        }

        // retrieve for legacy purposes.  if set, we'll remove any connections to hostgroup we find and use the host
        hostgroup = connectionDetails.get(FlashArrayAdapter.HOSTGROUP);
        if (hostgroup == null) {
            hostgroup = queryParms.get(FlashArrayAdapter.HOSTGROUP);
        }

        String connTimeoutStr = connectionDetails.get(FlashArrayAdapter.CONNECT_TIMEOUT_MS);
        if (connTimeoutStr == null) {
            connTimeoutStr = queryParms.get(FlashArrayAdapter.CONNECT_TIMEOUT_MS);
        }
        if (connTimeoutStr == null) {
            connTimeout = CONNECT_TIMEOUT_MS_DEFAULT;
        } else {
            try {
                connTimeout = Integer.parseInt(connTimeoutStr);
            } catch (NumberFormatException e) {
                logger.warn("Connection timeout not formatted correctly, using default", e);
                connTimeout = CONNECT_TIMEOUT_MS_DEFAULT;
            }
        }

        String keyTtlString = connectionDetails.get(FlashArrayAdapter.KEY_TTL);
        if (keyTtlString == null) {
            keyTtlString = queryParms.get(FlashArrayAdapter.KEY_TTL);
        }
        if (keyTtlString == null) {
            keyTtl = KEY_TTL_DEFAULT;
        } else {
            try {
                keyTtl = Integer.parseInt(keyTtlString);
            } catch (NumberFormatException e) {
                logger.warn("Key TTL not formatted correctly, using default", e);
                keyTtl = KEY_TTL_DEFAULT;
            }
        }

        String copyWaitStr = connectionDetails.get(FlashArrayAdapter.POST_COPY_WAIT_MS);
        if (copyWaitStr == null) {
            copyWaitStr = queryParms.get(FlashArrayAdapter.POST_COPY_WAIT_MS);
        }
        if (copyWaitStr == null) {
            postCopyWait = POST_COPY_WAIT_MS_DEFAULT;
        } else {
            try {
                postCopyWait = Integer.parseInt(copyWaitStr);
            } catch (NumberFormatException e) {
                logger.warn("Key TTL not formatted correctly, using default", e);
                postCopyWait = KEY_TTL_DEFAULT;
            }
        }

        String skipTlsValidationStr = connectionDetails.get(ProviderAdapter.API_SKIP_TLS_VALIDATION_KEY);
        if (skipTlsValidationStr == null) {
            skipTlsValidationStr = queryParms.get(ProviderAdapter.API_SKIP_TLS_VALIDATION_KEY);
        }

        if (skipTlsValidationStr != null) {
            skipTlsValidation = Boolean.parseBoolean(skipTlsValidationStr);
        } else {
            skipTlsValidation = true;
        }

        CloseableHttpResponse response = null;
        try {
            HttpPost request = new HttpPost(url + "/" + apiLoginVersion + "/auth/apitoken");
            // request.addHeader("Content-Type", "application/json");
            // request.addHeader("Accept", "application/json");
            ArrayList<NameValuePair> postParms = new ArrayList<NameValuePair>();
            postParms.add(new BasicNameValuePair("username", username));
            postParms.add(new BasicNameValuePair("password", password));
            request.setEntity(new UrlEncodedFormEntity(postParms, "UTF-8"));
            CloseableHttpClient client = getClient();
            response = (CloseableHttpResponse) client.execute(request);

            int statusCode = response.getStatusLine().getStatusCode();
            FlashArrayApiToken apitoken = null;
            if (statusCode == 200 | statusCode == 201) {
                apitoken = mapper.readValue(response.getEntity().getContent(), FlashArrayApiToken.class);
                if (apitoken == null) {
                    throw new CloudRuntimeException(
                            "Authentication responded successfully but no api token was returned");
                }
            } else if (statusCode == 401 || statusCode == 403) {
                throw new CloudRuntimeException(
                        "Authentication or Authorization to FlashArray [" + url + "] with user [" + username
                                + "] failed, unable to retrieve session token");
            } else {
                throw new CloudRuntimeException(
                        "Unexpected HTTP response code from FlashArray [" + url + "] - [" + statusCode
                                + "] - " + response.getStatusLine().getReasonPhrase());
            }

            // now we need to get the access token
            request = new HttpPost(url + "/" + apiVersion + "/login");
            request.addHeader("api-token", apitoken.getApiToken());
            response = (CloseableHttpResponse) client.execute(request);

            statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200 | statusCode == 201) {
                Header[] headers = response.getHeaders("x-auth-token");
                if (headers == null || headers.length == 0) {
                    throw new CloudRuntimeException(
                            "Getting access token responded successfully but access token was not available");
                }
                accessToken = headers[0].getValue();
            } else if (statusCode == 401 || statusCode == 403) {
                throw new CloudRuntimeException(
                        "Authentication or Authorization to FlashArray [" + url + "] with user [" + username
                                + "] failed, unable to retrieve session token");
            } else {
                throw new CloudRuntimeException(
                        "Unexpected HTTP response code from FlashArray [" + url + "] - [" + statusCode
                                + "] - " + response.getStatusLine().getReasonPhrase());
            }

        } catch (UnsupportedEncodingException e) {
            throw new CloudRuntimeException("Error creating input for login, check username/password encoding");
        } catch (UnsupportedOperationException e) {
            throw new CloudRuntimeException("Error processing login response from FlashArray [" + url + "]", e);
        } catch (IOException e) {
            throw new CloudRuntimeException("Error sending login request to FlashArray [" + url + "]", e);
        } finally {
            try {
                if (response != null) {
                    response.close();
                }
            } catch (IOException e) {
                logger.debug("Error closing response from login attempt to FlashArray", e);
            }
        }
    }

    private void removeVlunsAll(ProviderAdapterContext context, String volumeNamespace, String volumeName) {
        volumeName = normalizeName(volumeNamespace, volumeName);
        FlashArrayList<FlashArrayConnection> list = null;

        try {
            list = GET("/connections?volume_names=" + volumeName,
                    new TypeReference<FlashArrayList<FlashArrayConnection>>() {
                    });
        } catch (CloudRuntimeException e) {
            // this means the volume being deleted no longer exists so no connections can be
            // searched
            if (e.toString().contains("Bad Request")) {
                return;
            }
        }

        if (list != null && list.getItems() != null) {
            for (FlashArrayConnection conn : list.getItems()) {
                if (hostgroup != null && conn.getHostGroup() != null && conn.getHostGroup().getName() != null) {
                    DELETE("/connections?host_group_names=" + conn.getHostGroup().getName() + "&volume_names="
                            + volumeName);
                    break;
                } else if (conn.getHost() != null && conn.getHost().getName() != null) {
                    DELETE("/connections?host_names=" + conn.getHost().getName() + "&volume_names=" + volumeName);
                }
            }
        }
    }

    private FlashArrayVolume getVolume(String volumeName) {
        FlashArrayList<FlashArrayVolume> list = GET("/volumes?names=" + volumeName,
                new TypeReference<FlashArrayList<FlashArrayVolume>>() {
                });
        return (FlashArrayVolume) getFlashArrayItem(list);
    }

    private FlashArrayPod getVolumeNamespace(String name) {
        FlashArrayList<FlashArrayPod> list = GET("/pods?names=" + name,
                new TypeReference<FlashArrayList<FlashArrayPod>>() {
                });
        return (FlashArrayPod) getFlashArrayItem(list);
    }

    private FlashArrayVolume getSnapshot(String snapshotName) {
        FlashArrayList<FlashArrayVolume> list = GET("/volume-snapshots?names=" + snapshotName,
                new TypeReference<FlashArrayList<FlashArrayVolume>>() {
                });
        return (FlashArrayVolume) getFlashArrayItem(list);
    }

    private Object getFlashArrayItem(FlashArrayList<?> list) {
        if (list.getItems() != null && list.getItems().size() > 0) {
            return list.getItems().get(0);
        } else {
            return null;
        }
    }

    private String normalizeName(String volumeNamespace, String volumeName) {
        if (!volumeName.contains("::")) {
            if (volumeNamespace != null) {
                volumeName = volumeNamespace + "::" + volumeName;
            }
        }
        return volumeName;
    }

    @SuppressWarnings("unchecked")
    private <T> T POST(String path, Object input, final TypeReference<T> type) {
        CloseableHttpResponse response = null;
        try {
            this.refreshSession(false);
            HttpPost request = new HttpPost(url + "/" + apiVersion + path);
            request.addHeader("Content-Type", "application/json");
            request.addHeader("Accept", "application/json");
            request.addHeader("X-auth-token", getAccessToken());
            if (input != null) {
                try {
                    String data = mapper.writeValueAsString(input);
                    request.setEntity(new StringEntity(data));
                } catch (UnsupportedEncodingException | JsonProcessingException e) {
                    throw new CloudRuntimeException(
                            "Error processing request payload to [" + url + "] for path [" + path + "]", e);
                }
            }

            CloseableHttpClient client = getClient();
            try {
                response = (CloseableHttpResponse) client
                        .execute(request);
            } catch (IOException e) {
                throw new CloudRuntimeException("Error sending request to FlashArray [" + url + path + "]", e);
            }

            final int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200 || statusCode == 201) {
                try {
                    if (type != null) {
                        Header header = response.getFirstHeader("Location");
                        if (type.getType().getTypeName().equals(String.class.getName())) {
                            if (header != null) {
                                return (T) header.getValue();
                            } else {
                                return null;
                            }
                        } else {
                            return mapper.readValue(response.getEntity().getContent(), type);
                        }
                    }
                    return null;
                } catch (UnsupportedOperationException | IOException e) {
                    throw new CloudRuntimeException("Error processing response from FlashArray [" + url + path + "]",
                            e);
                }
            } else if (statusCode == 400) {
                try {
                    Map<String, Object> payload = mapper.readValue(response.getEntity().getContent(),
                            new TypeReference<Map<String, Object>>() {
                            });
                    throw new CloudRuntimeException("Invalid request error 400: " + payload);
                } catch (UnsupportedOperationException | IOException e) {
                    throw new CloudRuntimeException(
                            "Error processing bad request response from FlashArray [" + url + path + "]", e);
                }
            } else if (statusCode == 401 || statusCode == 403) {
                throw new CloudRuntimeException(
                        "Authentication or Authorization to FlashArray [" + url + "] with user [" + username
                                + "] failed, unable to retrieve session token");
            } else {
                try {
                    Map<String, Object> payload = mapper.readValue(response.getEntity().getContent(),
                            new TypeReference<Map<String, Object>>() {
                            });
                    throw new CloudRuntimeException("Invalid request error " + statusCode + ": " + payload);
                } catch (UnsupportedOperationException | IOException e) {
                    throw new CloudRuntimeException(
                            "Unexpected HTTP response code from FlashArray on POST [" + url + path + "] - ["
                                    + statusCode + "] - " + response.getStatusLine().getReasonPhrase());
                }
            }
        } finally {
            if (response != null) {
                try {
                    response.close();
                } catch (IOException e) {
                    logger.debug("Unexpected failure closing response to FlashArray API", e);
                }
            }
        }
    }

    private <T> T PATCH(String path, Object input, final TypeReference<T> type) {
        CloseableHttpResponse response = null;
        try {
            this.refreshSession(false);
            HttpPatch request = new HttpPatch(url + "/" + apiVersion + path);
            request.addHeader("Content-Type", "application/json");
            request.addHeader("Accept", "application/json");
            request.addHeader("X-auth-token", getAccessToken());
            String data = mapper.writeValueAsString(input);
            request.setEntity(new StringEntity(data));

            CloseableHttpClient client = getClient();
            response = (CloseableHttpResponse) client.execute(request);

            final int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200 || statusCode == 201) {
                if (type != null)
                    return mapper.readValue(response.getEntity().getContent(), type);
                return null;
            } else if (statusCode == 400) {
                Map<String, Object> payload = mapper.readValue(response.getEntity().getContent(),
                        new TypeReference<Map<String, Object>>() {
                        });
                throw new CloudRuntimeException("Invalid request error 400: " + payload);
            } else if (statusCode == 401 || statusCode == 403) {
                throw new CloudRuntimeException(
                        "Authentication or Authorization to FlashArray [" + url + "] with user [" + username
                                + "] failed, unable to retrieve session token");
            } else {
                Map<String, Object> payload = mapper.readValue(response.getEntity().getContent(),
                        new TypeReference<Map<String, Object>>() {
                        });
                throw new CloudRuntimeException(
                        "Invalid request error from FlashArray on PUT [" + url + path + "]" + statusCode + ": "
                                + response.getStatusLine().getReasonPhrase() + " - " + payload);
            }
        } catch (UnsupportedEncodingException | JsonProcessingException e) {
            throw new CloudRuntimeException(
                    "Error processing request payload to [" + url + "] for path [" + path + "]", e);
        } catch (UnsupportedOperationException e) {
            throw new CloudRuntimeException("Error processing bad request response from FlashArray [" + url + "]",
                    e);
        } catch (IOException e) {
            throw new CloudRuntimeException("Error sending request to FlashArray [" + url + "]", e);

        } finally {
            if (response != null) {
                try {
                    response.close();
                } catch (IOException e) {
                    logger.debug("Unexpected failure closing response to FlashArray API", e);
                }
            }
        }

    }

    private <T> T GET(String path, final TypeReference<T> type) {
        CloseableHttpResponse response = null;
        try {
            this.refreshSession(false);
            HttpGet request = new HttpGet(url + "/" + apiVersion + path);
            request.addHeader("Content-Type", "application/json");
            request.addHeader("Accept", "application/json");
            request.addHeader("X-auth-token", getAccessToken());

            CloseableHttpClient client = getClient();
            response = (CloseableHttpResponse) client.execute(request);
            final int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200) {
                try {
                    return mapper.readValue(response.getEntity().getContent(), type);
                } catch (UnsupportedOperationException | IOException e) {
                    throw new CloudRuntimeException("Error processing response from FlashArray [" + url + "]", e);
                }
            } else if (statusCode == 401 || statusCode == 403) {
                throw new CloudRuntimeException(
                        "Authentication or Authorization to FlashArray [" + url + "] with user [" + username
                                + "] failed, unable to retrieve session token");
            } else {
                throw new CloudRuntimeException(
                        "Unexpected HTTP response code from FlashArray on GET [" + request.getURI() + "] - ["
                                + statusCode + "] - " + response.getStatusLine().getReasonPhrase());
            }
        } catch (IOException e) {
            throw new CloudRuntimeException("Error sending request to FlashArray [" + url + "]", e);
        } catch (UnsupportedOperationException e) {
            throw new CloudRuntimeException("Error processing response from FlashArray [" + url + "]", e);
        } finally {
            if (response != null) {
                try {
                    response.close();
                } catch (IOException e) {
                    logger.debug("Unexpected failure closing response to FlashArray API", e);
                }
            }
        }
    }

    private void DELETE(String path) {
        CloseableHttpResponse response = null;
        try {
            this.refreshSession(false);
            HttpDelete request = new HttpDelete(url + "/" + apiVersion + path);
            request.addHeader("Content-Type", "application/json");
            request.addHeader("Accept", "application/json");
            request.addHeader("X-auth-token", getAccessToken());

            CloseableHttpClient client = getClient();
            response = (CloseableHttpResponse) client.execute(request);
            final int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200 || statusCode == 404 || statusCode == 400) {
                // this means the volume was deleted successfully, or doesn't exist (effective
                // delete), or
                // the volume name is malformed or too long - meaning it never got created to
                // begin with (effective delete)
                return;
            } else if (statusCode == 401 || statusCode == 403) {
                throw new CloudRuntimeException(
                        "Authentication or Authorization to FlashArray [" + url + "] with user [" + username
                                + "] failed, unable to retrieve session token");
            } else if (statusCode == 409) {
                throw new CloudRuntimeException(
                        "The volume cannot be deleted at this time due to existing dependencies.  Validate that all snapshots associated with this volume have been deleted and try again.");
            } else {
                throw new CloudRuntimeException(
                        "Unexpected HTTP response code from FlashArray on DELETE [" + url + path + "] - ["
                                + statusCode + "] - " + response.getStatusLine().getReasonPhrase());
            }
        } catch (IOException e) {
            throw new CloudRuntimeException("Error sending request to FlashArray [" + url + "]", e);
        } finally {
            if (response != null) {
                try {
                    response.close();
                } catch (IOException e) {
                    logger.debug("Unexpected failure closing response to FlashArray API", e);
                }
            }
        }
    }

    private CloseableHttpClient getClient() {
        if (_client == null) {
            RequestConfig config = RequestConfig.custom()
                    .setConnectTimeout((int) connTimeout)
                    .setConnectionRequestTimeout((int) connTimeout)
                    .setSocketTimeout((int) connTimeout).build();

            HostnameVerifier verifier = null;
            SSLContext sslContext = null;

            if (this.skipTlsValidation) {
                try {
                    verifier = NoopHostnameVerifier.INSTANCE;
                    sslContext = new SSLContextBuilder().loadTrustMaterial(null, TrustAllStrategy.INSTANCE).build();
                } catch (KeyManagementException e) {
                    throw new CloudRuntimeException(e);
                } catch (NoSuchAlgorithmException e) {
                    throw new CloudRuntimeException(e);
                } catch (KeyStoreException e) {
                    throw new CloudRuntimeException(e);
                }
            }

            _client = HttpClients.custom()
                    .setDefaultRequestConfig(config)
                    .setSSLHostnameVerifier(verifier)
                    .setSSLContext(sslContext)
                    .build();
        }
        return _client;
    }

    /**
     * pure array requires volume sizes in multiples of 512...we'll just round up to
     * next 512 boundary
     *
     * @param sizeInBytes
     * @return
     */
    private Long roundUp512Boundary(Long sizeInBytes) {
        Long remainder = sizeInBytes % 512;
        if (remainder > 0) {
            sizeInBytes = sizeInBytes + (512 - remainder);
        }
        return sizeInBytes;
    }

    @Override
    public Map<String, String> getConnectionIdMap(ProviderAdapterDataObject dataIn) {
        Map<String, String> map = new HashMap<String, String>();

        // flasharray doesn't let you directly map a snapshot to a host, so we'll just return an empty map
        if (dataIn.getType() == ProviderAdapterDataObject.Type.SNAPSHOT) {
            return map;
        }

        try {
            FlashArrayList<FlashArrayConnection> list = GET("/connections?volume_names=" + dataIn.getExternalName(),
                    new TypeReference<FlashArrayList<FlashArrayConnection>>() {
                    });

            if (list != null && list.getItems() != null) {
                for (FlashArrayConnection conn : list.getItems()) {
                    if (conn.getHost() != null) {
                        map.put(conn.getHost().getName(), "" + conn.getLun());
                    }
                }
            }
        } catch (Exception e) {
            // flasharray returns a 400 if the volume doesn't exist, so we'll just return an empty object.
            if (logger.isTraceEnabled()) {
                logger.trace("Error getting connection map for volume [" + dataIn.getExternalName() + "]: " + e.toString(), e);
            }
        }
        return map;
    }

    @Override
    public boolean canDirectAttachSnapshot() {
        return false;
    }
}
