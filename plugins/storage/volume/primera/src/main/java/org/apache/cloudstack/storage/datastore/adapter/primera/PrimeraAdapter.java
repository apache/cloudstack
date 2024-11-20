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
package org.apache.cloudstack.storage.datastore.adapter.primera;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;

import org.apache.cloudstack.storage.datastore.adapter.ProviderAdapter;
import org.apache.cloudstack.storage.datastore.adapter.ProviderAdapterContext;
import org.apache.cloudstack.storage.datastore.adapter.ProviderAdapterDataObject;
import org.apache.cloudstack.storage.datastore.adapter.ProviderAdapterDiskOffering;
import org.apache.cloudstack.storage.datastore.adapter.ProviderSnapshot;
import org.apache.cloudstack.storage.datastore.adapter.ProviderVolume;
import org.apache.cloudstack.storage.datastore.adapter.ProviderVolume.AddressType;
import org.apache.cloudstack.storage.datastore.adapter.ProviderVolumeNamer;
import org.apache.cloudstack.storage.datastore.adapter.ProviderVolumeStats;
import org.apache.cloudstack.storage.datastore.adapter.ProviderVolumeStorageStats;
import org.apache.cloudstack.storage.datastore.adapter.ProviderAdapterDiskOffering.ProvisioningType;
import org.apache.http.Header;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PrimeraAdapter implements ProviderAdapter {

    protected Logger logger = LogManager.getLogger(getClass());

    public static final String HOSTSET = "hostset";
    public static final String CPG = "cpg";
    public static final String SNAP_CPG = "snapCpg";
    public static final String KEY_TTL = "keyttl";
    public static final String CONNECT_TIMEOUT_MS = "connectTimeoutMs";
    public static final String POST_COPY_WAIT_MS = "postCopyWaitMs";
    public static final String TASK_WAIT_TIMEOUT_MS = "taskWaitTimeoutMs";

    private static final long KEY_TTL_DEFAULT = (1000 * 60 * 14);
    private static final long CONNECT_TIMEOUT_MS_DEFAULT = 60 * 1000;
    private static final long TASK_WAIT_TIMEOUT_MS_DEFAULT = 10 * 60 * 1000;
    public static final long BYTES_IN_MiB = 1048576;

    static final ObjectMapper mapper = new ObjectMapper();
    public String cpg = null;
    public String snapCpg = null;
    public String hostset = null;
    private String username;
    private String password;
    private String key;
    private String url;
    private long keyExpiration = -1;
    private long keyTtl = KEY_TTL_DEFAULT;
    private long connTimeout = CONNECT_TIMEOUT_MS_DEFAULT;
    private long taskWaitTimeoutMs = TASK_WAIT_TIMEOUT_MS_DEFAULT;
    private CloseableHttpClient _client = null;
    private boolean skipTlsValidation;

    private Map<String, String> connectionDetails = null;

    public PrimeraAdapter(String url, Map<String, String> details) {
        this.url = url;
        this.connectionDetails = details;
        login();
    }

    @Override
    public void refresh(Map<String, String> details) {
        this.connectionDetails = details;
        this.refreshSession(true);
    }

    @Override
    public void validate() {
        login();
        // check if hostgroup and pod from details really exist - we will
        // require a distinct configuration object/connection object for each type
        if (this.getCpg(cpg) == null) {
            throw new RuntimeException(
                    "Pod [" + cpg + "] not found in FlashArray at [" + url + "], please validate configuration");
        }
    }

    @Override
    public void disconnect() {
        logger.info("PrimeraAdapter:disconnect(): closing session");
        try {
            _client.close();
        } catch (IOException e) {
            logger.warn("PrimeraAdapter:refreshSession(): Error closing client connection", e);
        } finally {
            _client = null;
            keyExpiration = -1;
        }
        return;
    }

    @Override
    public ProviderVolume create(ProviderAdapterContext context, ProviderAdapterDataObject dataIn,
            ProviderAdapterDiskOffering diskOffering, long sizeInBytes) {
        PrimeraVolumeRequest request = new PrimeraVolumeRequest();
        String externalName = ProviderVolumeNamer.generateObjectName(context, dataIn);
        request.setName(externalName);
        request.setCpg(cpg);
        request.setSnapCPG(snapCpg);
        if (sizeInBytes < BYTES_IN_MiB) {
            request.setSizeMiB(1);
        } else {
            request.setSizeMiB(sizeInBytes/BYTES_IN_MiB);
        }

        // determine volume type based on offering
        // THIN: tpvv=true, reduce=false
        // SPARSE: tpvv=true, reduce=true
        // THICK: tpvv=false, tpZeroFill=true (not supported)
        if (diskOffering != null) {
            if (diskOffering.getType() == ProvisioningType.THIN) {
                request.setTpvv(true);
                request.setReduce(false);
            } else if (diskOffering.getType() == ProvisioningType.SPARSE) {
                request.setTpvv(false);
                request.setReduce(true);
            } else if (diskOffering.getType() == ProvisioningType.FAT) {
                throw new RuntimeException("This storage provider does not support FAT provisioned volumes");
            }

            // sets the amount of space allowed for snapshots as a % of the volumes size
            if (diskOffering.getHypervisorSnapshotReserve() != null) {
                request.setSsSpcAllocLimitPct(diskOffering.getHypervisorSnapshotReserve());
            }
        } else {
            // default to deduplicated volume
            request.setReduce(true);
            request.setTpvv(false);
        }

        request.setComment(ProviderVolumeNamer.generateObjectComment(context, dataIn));
        POST("/volumes", request, null);
        dataIn.setExternalName(externalName);
        ProviderVolume volume = getVolume(context, dataIn);
        return volume;
    }

    @Override
    public String attach(ProviderAdapterContext context, ProviderAdapterDataObject dataIn, String hostname) {
        assert dataIn.getExternalName() != null : "External name not provided internally on volume attach";
        PrimeraHostset.PrimeraHostsetVLUNRequest request = new PrimeraHostset.PrimeraHostsetVLUNRequest();
        PrimeraHost host = getHost(hostname);
        if (host == null) {
            throw new RuntimeException("Unable to find host " + hostname + " on storage provider");
        }
        request.setHostname(host.getName());

        request.setVolumeName(dataIn.getExternalName());
        request.setAutoLun(true);
        // auto-lun returned here: Location: /api/v1/vluns/test_vv02,252,mysystem,2:2:4
        String location = POST("/vluns", request, new TypeReference<String>() {});
        if (location == null) {
            throw new RuntimeException("Attach volume failed with empty location response to vlun add command on storage provider");
        }
        String[] toks = location.split(",");
        if (toks.length <2) {
            throw new RuntimeException("Attach volume failed with invalid location response to vlun add command on storage provider.  Provided location: " + location);
        }
        return toks[1];
    }

    /**
     * This detaches ALL vlun's for the provided volume name IF they are associated to this hostset
     * @param context
     * @param request
     */
    public void detach(ProviderAdapterContext context, ProviderAdapterDataObject request) {
        detach(context, request, null);
    }

    @Override
    public void detach(ProviderAdapterContext context, ProviderAdapterDataObject request, String hostname) {
        // we expect to only be attaching one hostset to the vluns, so on detach we'll
        // remove ALL vluns we find.
        assert request.getExternalName() != null : "External name not provided internally on volume detach";

        PrimeraVlunList list = getVluns(request.getExternalName());
        if (list != null && list.getMembers().size() > 0) {
            list.getMembers().forEach(vlun -> {
                // remove any hostset from old code if configured
                if (hostset != null && vlun.getHostname() != null && vlun.getHostname().equals("set:" + hostset)) {
                    removeVlun(request.getExternalName(), vlun.getLun(), vlun.getHostname());
                }

                if (hostname != null) {
                    if (vlun.getHostname().equals(hostname) || vlun.getHostname().equals(hostname.split("\\.")[0])) {
                        removeVlun(request.getExternalName(), vlun.getLun(), vlun.getHostname());
                    }
                }
            });
        }
    }

    public void removeVlun(String name, Integer lunid, String hostString) {
        // hostString can be a hostname OR "set:<hostsetname>".  It is stored this way
        // in the appliance and returned as the vlun's name/string.
        DELETE("/vluns/" + name + "," + lunid + "," + hostString);
    }

    public PrimeraVlunList getVluns(String name) {
        String query = "%22volumeName%20EQ%20" + name + "%22";
        return GET("/vluns?query=" + query, new TypeReference<PrimeraVlunList>() {});
    }

    @Override
    public void delete(ProviderAdapterContext context, ProviderAdapterDataObject request) {
        assert request.getExternalName() != null : "External name not provided internally on volume delete";

        // first remove vluns (take volumes from vluns) from hostset
        detach(context, request);
        DELETE("/volumes/" + request.getExternalName());
    }

    @Override
    public ProviderVolume copy(ProviderAdapterContext context, ProviderAdapterDataObject sourceVolumeInfo,
            ProviderAdapterDataObject targetVolumeInfo) {
        PrimeraVolumeCopyRequest request = new PrimeraVolumeCopyRequest();
        PrimeraVolumeCopyRequestParameters parms = new PrimeraVolumeCopyRequestParameters();

        assert sourceVolumeInfo.getExternalName() != null: "External provider name not provided on copy request to Primera volume provider";

        // if we have no external name, treat it as a new volume
        if (targetVolumeInfo.getExternalName() == null) {
            targetVolumeInfo.setExternalName(ProviderVolumeNamer.generateObjectName(context, targetVolumeInfo));
        }

        ProviderVolume sourceVolume = this.getVolume(context, sourceVolumeInfo);
        if (sourceVolume == null) {
            throw new RuntimeException("Source volume " + sourceVolumeInfo.getExternalUuid() + " with provider name " + sourceVolumeInfo.getExternalName() + " not found on storage provider");
        }

        ProviderVolume targetVolume = this.getVolume(context, targetVolumeInfo);
        if (targetVolume == null) {
            this.create(context, targetVolumeInfo, null, sourceVolume.getAllocatedSizeInBytes());
        }

        parms.setDestVolume(targetVolumeInfo.getExternalName());
        parms.setOnline(false);
        request.setParameters(parms);

        PrimeraTaskReference taskref = POST("/volumes/" + sourceVolumeInfo.getExternalName(), request, new TypeReference<PrimeraTaskReference>() {});
        if (taskref == null) {
            throw new RuntimeException("Unable to retrieve task used to copy to newly created volume");
        }

        waitForTaskToComplete(taskref.getTaskid(), "copy volume " + sourceVolumeInfo.getExternalName() + " to " +
            targetVolumeInfo.getExternalName(), taskWaitTimeoutMs);

        return this.getVolume(context, targetVolumeInfo);
    }

    private void waitForTaskToComplete(String taskid, String taskDescription, Long timeoutMs) {
        // first wait for task to complete
        long taskWaitTimeout = System.currentTimeMillis() + timeoutMs;
        boolean timedOut = true;
        PrimeraTaskStatus status = null;
        long starttime = System.currentTimeMillis();
        while (System.currentTimeMillis() <= taskWaitTimeout) {
            status = this.getTaskStatus(taskid);
            if (status != null && status.isFinished()) {
                timedOut = false;
                if (!status.isSuccess()) {
                    throw new RuntimeException("Task " + taskDescription + " was cancelled.  TaskID: " + status.getId() + "; Final Status: " + status.getStatusName());
                }
                break;
            } else {
                if (status != null) {
                    logger.info("Task " + taskDescription + " is still running.  TaskID: " + status.getId() + "; Current Status: " + status.getStatusName());
                }
                // ugly...to keep from hot-polling API
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {

                }
            }
        }

        if (timedOut) {
            if (status != null) {
                throw new RuntimeException("Task " + taskDescription + " timed out.  TaskID: " + status.getId() + ", Last Known Status: " + status.getStatusName());
            } else {
                throw new RuntimeException("Task " + taskDescription + " timed out and a current status could not be retrieved from storage endpoint");
            }
        }

        logger.info(taskDescription + " completed in " + ((System.currentTimeMillis() - starttime)/1000) + " seconds");
    }

    private PrimeraTaskStatus getTaskStatus(String taskid) {
        return GET("/tasks/" + taskid + "?view=excludeDetail", new TypeReference<PrimeraTaskStatus>() {
        });
    }

    @Override
    public ProviderSnapshot snapshot(ProviderAdapterContext context, ProviderAdapterDataObject sourceVolume,
            ProviderAdapterDataObject targetSnapshot) {
        assert sourceVolume.getExternalName() != null : "External name not set";
        PrimeraVolumeSnapshotRequest request = new PrimeraVolumeSnapshotRequest();
        PrimeraVolumeSnapshotRequestParameters parms = new PrimeraVolumeSnapshotRequestParameters();
        parms.setName(ProviderVolumeNamer.generateObjectName(context, targetSnapshot));
        request.setParameters(parms);
        POST("/volumes/" + sourceVolume.getExternalName(), request, null);
        targetSnapshot.setExternalName(parms.getName());
        return getSnapshot(context, targetSnapshot);
    }

    @Override
    public ProviderVolume revert(ProviderAdapterContext context, ProviderAdapterDataObject dataIn) {
        assert dataIn.getExternalName() != null: "External name not internally set for provided snapshot when requested storage provider to revert";
        // first get original volume
        PrimeraVolume snapVol = (PrimeraVolume)getVolume(context, dataIn);
        assert snapVol != null: "Storage volume associated with snapshot externally named [" + dataIn.getExternalName() + "] not found";
        assert snapVol.getParentId() != null: "Unable to determine parent volume/snapshot for snapshot named [" + dataIn.getExternalName() + "]";

        PrimeraVolumeRevertSnapshotRequest request = new PrimeraVolumeRevertSnapshotRequest();
        request.setOnline(true);
        request.setPriority(2);
        PrimeraTaskReference taskref = PUT("/volumes/" + dataIn.getExternalName(), request, new TypeReference<PrimeraTaskReference>() {});
        if (taskref == null) {
            throw new RuntimeException("Unable to retrieve task used to revert snapshot to base volume");
        }

        waitForTaskToComplete(taskref.getTaskid(), "revert snapshot " + dataIn.getExternalName(), taskWaitTimeoutMs);

        return getVolumeById(context, snapVol.getParentId());
    }

    /**
     * Resize the volume to the new size.  For HPE Primera, the API takes the additional space to add to the volume
     * so this method will first retrieve the current volume's size and subtract that from the new size provided
     * before calling the API.
     *
     * This method uses option GROW_VOLUME=3 for the API at this URL:
     * https://support.hpe.com/hpesc/public/docDisplay?docId=a00118636en_us&page=v25706371.html
     *
     */
    @Override
    public void resize(ProviderAdapterContext context, ProviderAdapterDataObject request, long totalNewSizeInBytes) {
        assert request.getExternalName() != null: "External name not internally set for provided volume when requesting resize of volume";

        PrimeraVolume existingVolume = (PrimeraVolume) getVolume(context, request);
        assert existingVolume != null: "Storage volume resize request not possible as existing volume not found for external provider name: " + request.getExternalName();
        long existingSizeInBytes = existingVolume.getSizeMiB() * PrimeraAdapter.BYTES_IN_MiB;
        assert existingSizeInBytes < totalNewSizeInBytes: "Existing volume size is larger than requested new size for volume resize request.  The Primera storage system does not support truncating/shrinking volumes.";
        long addOnSizeInBytes = totalNewSizeInBytes - existingSizeInBytes;

        PrimeraVolume volume = new PrimeraVolume();
        volume.setSizeMiB((int) (addOnSizeInBytes / PrimeraAdapter.BYTES_IN_MiB));
        volume.setAction(3);
        PUT("/volumes/" + request.getExternalName(), volume, null);
    }

    @Override
    public ProviderVolume getVolume(ProviderAdapterContext context, ProviderAdapterDataObject request) {
        String externalName;

        // if the external name isn't provided, look for the derived contextual name.  some failure scenarios
        // may result in the volume for this context being created but a subsequent failure causing the external
        // name to not be persisted for later use.  This is true of template-type objects being cached on primary
        // storage
        if (request.getExternalName() == null) {
            externalName = ProviderVolumeNamer.generateObjectName(context, request);
        } else {
            externalName = request.getExternalName();
        }

        return GET("/volumes/" + externalName, new TypeReference<PrimeraVolume>() {
        });
    }

    private ProviderVolume getVolumeById(ProviderAdapterContext context, Integer id) {
        String query = "%22id%20EQ%20" + id + "%22";
        return GET("/volumes?query=" + query, new TypeReference<PrimeraVolume>() {});
    }

    @Override
    public ProviderSnapshot getSnapshot(ProviderAdapterContext context, ProviderAdapterDataObject request) {
        assert request.getExternalName() != null: "External name not provided internally when finding snapshot on storage provider";
        return GET("/volumes/" + request.getExternalName(), new TypeReference<PrimeraVolume>() {
        });
    }

    @Override
    public ProviderVolume getVolumeByAddress(ProviderAdapterContext context, AddressType addressType, String address) {
        assert address != null: "External volume address not provided";
        assert AddressType.FIBERWWN.equals(addressType): "This volume provider currently does not support address type " + addressType.name();
        String query = "%22wwn%20EQ%20" + address + "%22";
        return GET("/volumes?query=" + query, new TypeReference<PrimeraVolume>() {});
    }

    @Override
    public ProviderVolumeStorageStats getManagedStorageStats() {
        PrimeraCpg cpgobj = getCpg(cpg);
        // just in case
        if (cpgobj == null || cpgobj.getTotalSpaceMiB() == 0) {
            return null;
        }

        Long capacityBytes = 0L;
        if (cpgobj.getsDGrowth() != null) {
            capacityBytes = cpgobj.getsDGrowth().getLimitMiB() * PrimeraAdapter.BYTES_IN_MiB;
        }
        Long usedBytes = 0L;
        if (cpgobj.getUsrUsage() != null) {
            usedBytes = (cpgobj.getUsrUsage().getRawUsedMiB()) * PrimeraAdapter.BYTES_IN_MiB;
        }
        ProviderVolumeStorageStats stats = new ProviderVolumeStorageStats();
        stats.setActualUsedInBytes(usedBytes);
        stats.setCapacityInBytes(capacityBytes);
        return stats;
    }

    @Override
    public ProviderVolumeStats getVolumeStats(ProviderAdapterContext context, ProviderAdapterDataObject request) {
        PrimeraVolume vol = (PrimeraVolume)getVolume(context, request);
        if (vol == null || vol.getSizeMiB() == null || vol.getSizeMiB() == 0) {
            return null;
        }

        Long virtualSizeInBytes = vol.getHostWriteMiB()  * PrimeraAdapter.BYTES_IN_MiB;
        Long allocatedSizeInBytes = vol.getSizeMiB() * PrimeraAdapter.BYTES_IN_MiB;
        Long actualUsedInBytes = vol.getTotalUsedMiB() * PrimeraAdapter.BYTES_IN_MiB;
        ProviderVolumeStats stats = new ProviderVolumeStats();
        stats.setActualUsedInBytes(actualUsedInBytes);
        stats.setAllocatedInBytes(allocatedSizeInBytes);
        stats.setVirtualUsedInBytes(virtualSizeInBytes);
        return stats;
    }

    @Override
    public boolean canAccessHost(ProviderAdapterContext context, String hostname) {
        // check that the array has the host configured
        PrimeraHost host = this.getHost(hostname);
        if (host != null) {
            // if hostset is configured we'll additionally check if the host is in it (legacy/original behavior)
            return true;
        }

        return false;
    }

    private PrimeraHost getHost(String name) {
        PrimeraHost host = GET("/hosts/" + name, new TypeReference<PrimeraHost>() {    });
        if (host == null) {
            if (name.indexOf('.') > 0) {
                host = this.getHost(name.substring(0, (name.indexOf('.'))));
            }
        }
        return host;

    }

    private PrimeraCpg getCpg(String name) {
        return GET("/cpgs/" + name, new TypeReference<PrimeraCpg>() {
        });
    }

    private synchronized String refreshSession(boolean force) {
        try {
            if (force || keyExpiration < (System.currentTimeMillis()-15000)) {
                // close client to force connection reset on appliance -- not doing this can result in NotAuthorized error...guessing
                disconnect();
                login();
                logger.debug("PrimeraAdapter:refreshSession(): session created or refreshed with key=" + key + ", expiration=" + keyExpiration);
            } else {
                if (logger.isTraceEnabled()) {
                    logger.trace("PrimeraAdapter:refreshSession(): using existing session key=" + key + ", expiration=" + keyExpiration);
                }
            }
        } catch (Exception e) {
            // retry frequently but not every request to avoid DDOS on storage API
            logger.warn("Failed to refresh Primera API key for " + username + "@" + url + ", will retry in 5 seconds", e);
            keyExpiration = System.currentTimeMillis() + (5*1000);
        }
        return key;
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

        cpg = connectionDetails.get(PrimeraAdapter.CPG);
        if (cpg == null) {
            cpg = queryParms.get(PrimeraAdapter.CPG);
            if (cpg == null) {
                throw new RuntimeException(
                        PrimeraAdapter.CPG + " parameter/option required to configure this storage pool");
            }
        }

        snapCpg = connectionDetails.get(PrimeraAdapter.SNAP_CPG);
        if (snapCpg == null) {
            snapCpg = queryParms.get(PrimeraAdapter.SNAP_CPG);
            if (snapCpg == null) {
                // default to using same CPG as the volume
                snapCpg = cpg;
            }
        }

        // if this is null, we will use direct-to-host vlunids (preferred)
        hostset = connectionDetails.get(PrimeraAdapter.HOSTSET);
        if (hostset == null) {
            hostset = queryParms.get(PrimeraAdapter.HOSTSET);
        }

        String connTimeoutStr = connectionDetails.get(PrimeraAdapter.CONNECT_TIMEOUT_MS);
        if (connTimeoutStr == null) {
            connTimeoutStr = queryParms.get(PrimeraAdapter.CONNECT_TIMEOUT_MS);
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

        String keyTtlString = connectionDetails.get(PrimeraAdapter.KEY_TTL);
        if (keyTtlString == null) {
            keyTtlString = queryParms.get(PrimeraAdapter.KEY_TTL);
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

        String taskWaitTimeoutMsStr = connectionDetails.get(PrimeraAdapter.TASK_WAIT_TIMEOUT_MS);
        if (taskWaitTimeoutMsStr == null) {
            taskWaitTimeoutMsStr = queryParms.get(PrimeraAdapter.TASK_WAIT_TIMEOUT_MS);
            if (taskWaitTimeoutMsStr == null) {
                taskWaitTimeoutMs = PrimeraAdapter.TASK_WAIT_TIMEOUT_MS_DEFAULT;
            } else {
                try {
                    taskWaitTimeoutMs = Long.parseLong(taskWaitTimeoutMsStr);
                } catch (NumberFormatException e) {
                    logger.warn(PrimeraAdapter.TASK_WAIT_TIMEOUT_MS + " property not set to a proper number, using default value");
                }
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
            HttpPost request = new HttpPost(url + "/credentials");
            request.addHeader("Content-Type", "application/json");
            request.addHeader("Accept", "application/json");
            request.setEntity(new StringEntity("{\"user\":\"" + username + "\", \"password\":\"" + password + "\"}"));
            CloseableHttpClient client = getClient();
            response = (CloseableHttpResponse) client.execute(request);

            final int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200 | statusCode == 201) {
                PrimeraKey keyobj = mapper.readValue(response.getEntity().getContent(), PrimeraKey.class);
                key = keyobj.getKey();
                // Set the key expiration to x minutes from now
                this.keyExpiration = System.currentTimeMillis() + keyTtl;
                logger.info("PrimeraAdapter:login(): successful, new session: New key=" + key + ", expiration=" + this.keyExpiration);
            } else if (statusCode == 401 || statusCode == 403) {
                throw new RuntimeException("Authentication or Authorization to Primera [" + url + "] with user [" + username
                        + "] failed, unable to retrieve session token");
            } else {
                throw new RuntimeException("Unexpected HTTP response code from Primera [" + url + "] - [" + statusCode
                        + "] - " + response.getStatusLine().getReasonPhrase());
            }
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Error creating input for login, check username/password encoding");
        } catch (UnsupportedOperationException e) {
            throw new RuntimeException("Error processing login response from Primera [" + url + "]", e);
        } catch (IOException e) {
            throw new RuntimeException("Error sending login request to Primera [" + url + "]", e);
        } finally {
            try {
                if (response != null) {
                    response.close();
                }
            } catch (IOException e) {
                logger.debug("Error closing response from login attempt to Primera", e);
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
                    throw new RuntimeException(e);
                } catch (NoSuchAlgorithmException e) {
                    throw new RuntimeException(e);
                } catch (KeyStoreException e) {
                    throw new RuntimeException(e);
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

    @SuppressWarnings("unchecked")
    private <T> T POST(String path, Object input, final TypeReference<T> type) {
        CloseableHttpResponse response = null;
        try {
            String session_key = this.refreshSession(false);
            HttpPost request = new HttpPost(url + path);
            request.addHeader("Content-Type", "application/json");
            request.addHeader("Accept", "application/json");
            request.addHeader("X-HP3PAR-WSAPI-SessionKey", session_key);
            try {
                String data = mapper.writeValueAsString(input);
                request.setEntity(new StringEntity(data));
                if (logger.isTraceEnabled()) logger.trace("POST data: " + request.getEntity());
            } catch (UnsupportedEncodingException | JsonProcessingException e) {
                throw new RuntimeException(
                        "Error processing request payload to [" + url + "] for path [" + path + "]", e);
            }

            CloseableHttpClient client = getClient();
            try {
                response = (CloseableHttpResponse) client
                        .execute(request);
            } catch (IOException e) {
                throw new RuntimeException("Error sending request to Primera [" + url + path + "]", e);
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
                        } else if (type.getType().getTypeName().equals(PrimeraTaskReference.class.getName())) {
                            T obj = mapper.readValue(response.getEntity().getContent(), type);
                            PrimeraTaskReference taskref = (PrimeraTaskReference) obj;
                            taskref.setLocation(header.getValue());
                            return obj;
                        } else {
                            return mapper.readValue(response.getEntity().getContent(), type);
                        }
                    }
                    return null;
                } catch (UnsupportedOperationException | IOException e) {
                    throw new RuntimeException("Error processing response from Primera [" + url + path + "]", e);
                }
            } else if (statusCode == 400) {
                try {
                    Map<String, Object> payload = mapper.readValue(response.getEntity().getContent(),
                            new TypeReference<Map<String, Object>>() {
                            });
                    throw new RuntimeException("Invalid request error 400: " + payload);
                } catch (UnsupportedOperationException | IOException e) {
                    throw new RuntimeException(
                            "Error processing bad request response from Primera [" + url + path + "]", e);
                }
            } else if (statusCode == 401 || statusCode == 403) {
                throw new RuntimeException("Authentication or Authorization to Primera [" + url + "] with user [" + username
                        + "] failed, unable to retrieve session token");
            } else {
                try {
                    Map<String, Object> payload = mapper.readValue(response.getEntity().getContent(),
                            new TypeReference<Map<String, Object>>() {
                            });
                    throw new RuntimeException("Invalid request error " + statusCode + ": " + payload);
                } catch (UnsupportedOperationException | IOException e) {
                    throw new RuntimeException("Unexpected HTTP response code from Primera on POST [" + url + path + "] - ["
                            + statusCode + "] - " + response.getStatusLine().getReasonPhrase());
                }
            }
        } finally {
            if (response != null) {
                try {
                    response.close();
                } catch (IOException e) {
                    logger.debug("Unexpected failure closing response to Primera API", e);
                }
            }
        }
    }

    private <T> T PUT(String path, Object input, final TypeReference<T> type) {
        CloseableHttpResponse response = null;
        try {
            this.refreshSession(false);
            String session_key = this.refreshSession(false);
            HttpPut request = new HttpPut(url + path);
            request.addHeader("Content-Type", "application/json");
            request.addHeader("Accept", "application/json");
            request.addHeader("X-HP3PAR-WSAPI-SessionKey", session_key);
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
                throw new RuntimeException("Invalid request error 400: " + payload);
            } else if (statusCode == 401 || statusCode == 403) {
                throw new RuntimeException("Authentication or Authorization to Primera [" + url + "] with user [" + username
                        + "] failed, unable to retrieve session token");
            } else {
                Map<String, Object> payload = mapper.readValue(response.getEntity().getContent(),
                        new TypeReference<Map<String, Object>>() {});
                throw new RuntimeException("Invalid request error from Primera on PUT [" + url + path + "]" + statusCode + ": "
                        + response.getStatusLine().getReasonPhrase() + " - " + payload);
            }
        } catch (UnsupportedEncodingException | JsonProcessingException e) {
            throw new RuntimeException(
                    "Error processing request payload to [" + url + "] for path [" + path + "]", e);
        } catch (UnsupportedOperationException e) {
            throw new RuntimeException("Error processing bad request response from Primera [" + url + "]",
                    e);
        } catch (IOException e) {
            throw new RuntimeException("Error sending request to Primera [" + url + "]", e);

        } finally {
            if (response != null) {
                try {
                    response.close();
                } catch (IOException e) {
                    logger.debug("Unexpected failure closing response to Primera API", e);
                }
            }
        }
    }

    private <T> T GET(String path, final TypeReference<T> type) {
        CloseableHttpResponse response = null;
        try {
            this.refreshSession(false);
            String session_key = this.refreshSession(false);
            HttpGet request = new HttpGet(url + path);
            request.addHeader("Content-Type", "application/json");
            request.addHeader("Accept", "application/json");
            request.addHeader("X-HP3PAR-WSAPI-SessionKey", session_key);

            CloseableHttpClient client = getClient();
            response = (CloseableHttpResponse) client.execute(request);
            final int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200) {
                try {
                    return mapper.readValue(response.getEntity().getContent(), type);
                } catch (UnsupportedOperationException | IOException e) {
                    throw new RuntimeException("Error processing response from Primera [" + url + "]", e);
                }
            } else if (statusCode == 401 || statusCode == 403) {
                throw new RuntimeException("Authentication or Authorization to Primera [" + url + "] with user [" + username
                        + "] failed, unable to retrieve session token");
            } else if (statusCode == 404) {
                return null;
            } else {
                throw new RuntimeException("Unexpected HTTP response code from Primera on GET [" + url + path + "] - ["
                        + statusCode + "] - " + response.getStatusLine().getReasonPhrase());
            }
        } catch (IOException e) {
            throw new RuntimeException("Error sending request to Primera [" + url + "]", e);
        } catch (UnsupportedOperationException e) {
            throw new RuntimeException("Error processing response from Primera [" + url + "]", e);
        } finally {
            if (response != null) {
                try {
                    response.close();
                } catch (IOException e) {
                    logger.debug("Unexpected failure closing response to Primera API", e);
                }
            }
        }
    }

    private void DELETE(String path) {
        CloseableHttpResponse response = null;
        try {
            this.refreshSession(false);
            String session_key = this.refreshSession(false);
            HttpDelete request = new HttpDelete(url + path);
            request.addHeader("Content-Type", "application/json");
            request.addHeader("Accept", "application/json");
            request.addHeader("X-HP3PAR-WSAPI-SessionKey", session_key);

            CloseableHttpClient client = getClient();
            response = (CloseableHttpResponse) client.execute(request);
            final int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200 || statusCode == 404 || statusCode == 400) {
                // this means the volume was deleted successfully, or doesn't exist (effective delete), or
                // the volume name is malformed or too long - meaning it never got created to begin with (effective delete)
                return;
            } else if (statusCode == 401 || statusCode == 403) {
                throw new RuntimeException("Authentication or Authorization to Primera [" + url + "] with user [" + username
                        + "] failed, unable to retrieve session token");
            } else if (statusCode == 409) {
                throw new RuntimeException("The volume cannot be deleted at this time due to existing dependencies.  Validate that all snapshots associated with this volume have been deleted and try again." );
            } else {
                throw new RuntimeException("Unexpected HTTP response code from Primera on DELETE [" + url + path + "] - ["
                        + statusCode + "] - " + response.getStatusLine().getReasonPhrase());
            }
        } catch (IOException e) {
            throw new RuntimeException("Error sending request to Primera [" + url + "]", e);
        } finally {
            if (response != null) {
                try {
                    response.close();
                } catch (IOException e) {
                    logger.debug("Unexpected failure closing response to Primera API", e);
                }
            }
        }
    }

    @Override
    public Map<String, String> getConnectionIdMap(ProviderAdapterDataObject dataIn) {
        Map<String,String> connIdMap = new HashMap<String,String>();
        PrimeraVlunList list = this.getVluns(dataIn.getExternalName());

        if (list != null && list.getMembers() != null && list.getMembers().size() > 0) {
            for (PrimeraVlun vlun: list.getMembers()) {
                connIdMap.put(vlun.getHostname(), ""+vlun.getLun());
            }
        }

        return connIdMap;
    }

    @Override
    public boolean canDirectAttachSnapshot() {
        return true;
    }
}
