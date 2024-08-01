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
package org.apache.cloudstack.storage.datastore.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.UUID;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailVO;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailsDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;

import com.cloud.dc.ClusterVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.user.AccountDetailVO;
import com.cloud.user.AccountDetailsDao;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.exception.CloudRuntimeException;

import com.google.common.base.Preconditions;
import com.google.common.primitives.Longs;

import com.solidfire.client.ElementFactory;
import com.solidfire.element.api.Account;
import com.solidfire.element.api.AddAccountRequest;
import com.solidfire.element.api.AddInitiatorsToVolumeAccessGroupRequest;
import com.solidfire.element.api.AddVolumesToVolumeAccessGroupRequest;
import com.solidfire.element.api.CloneVolumeRequest;
import com.solidfire.element.api.CloneVolumeResult;
import com.solidfire.element.api.CreateSnapshotRequest;
import com.solidfire.element.api.CreateVolumeAccessGroupRequest;
import com.solidfire.element.api.CreateVolumeRequest;
import com.solidfire.element.api.DeleteSnapshotRequest;
import com.solidfire.element.api.DeleteVolumeRequest;
import com.solidfire.element.api.GetAccountByIDRequest;
import com.solidfire.element.api.GetAccountByNameRequest;
import com.solidfire.element.api.GetAsyncResultRequest;
import com.solidfire.element.api.ListSnapshotsRequest;
import com.solidfire.element.api.ListVolumeAccessGroupsRequest;
import com.solidfire.element.api.ListVolumesRequest;
import com.solidfire.element.api.ModifyVolumeRequest;
import com.solidfire.element.api.QoS;
import com.solidfire.element.api.RemoveInitiatorsFromVolumeAccessGroupRequest;
import com.solidfire.element.api.RemoveVolumesFromVolumeAccessGroupRequest;
import com.solidfire.element.api.RollbackToSnapshotRequest;
import com.solidfire.element.api.Snapshot;
import com.solidfire.element.api.SolidFireElement;
import com.solidfire.element.api.Volume;
import com.solidfire.element.api.VolumeAccessGroup;
import com.solidfire.jsvcgen.javautil.Optional;

import static org.apache.commons.lang.ArrayUtils.toPrimitive;

public class SolidFireUtil {
    protected static Logger LOGGER = LogManager.getLogger(SolidFireUtil.class);

    public static final String PROVIDER_NAME = "SolidFire";
    public static final String SHARED_PROVIDER_NAME = "SolidFireShared";

    private static final Random RANDOM = new Random(System.nanoTime());
    public static final int LOCK_TIME_IN_SECONDS = 300;

    public static final String LOGGER_PREFIX = "SolidFire: ";

    public static final String MANAGEMENT_VIP = "mVip";
    public static final String STORAGE_VIP = "sVip";

    public static final String MANAGEMENT_PORT = "mPort";
    public static final String STORAGE_PORT = "sPort";

    public static final String CLUSTER_ADMIN_USERNAME = "clusterAdminUsername";
    public static final String CLUSTER_ADMIN_PASSWORD = "clusterAdminPassword";

    // these three variables should only be used for the SolidFire plug-in with the name SolidFireUtil.PROVIDER_NAME
    public static final String CLUSTER_DEFAULT_MIN_IOPS = "clusterDefaultMinIops";
    public static final String CLUSTER_DEFAULT_MAX_IOPS = "clusterDefaultMaxIops";
    public static final String CLUSTER_DEFAULT_BURST_IOPS_PERCENT_OF_MAX_IOPS = "clusterDefaultBurstIopsPercentOfMaxIops";

    // these three variables should only be used for the SolidFire plug-in with the name SolidFireUtil.SHARED_PROVIDER_NAME
    public static final String MIN_IOPS = "minIops";
    public static final String MAX_IOPS = "maxIops";
    public static final String BURST_IOPS = "burstIops";

    private static final String ACCOUNT_ID = "accountId";

    public static final String VOLUME_ID = "volumeId";
    public static final String TEMP_VOLUME_ID = "tempVolumeId";
    public static final String SNAPSHOT_ID = "snapshotId";

    public static final String CloudStackVolumeId = "CloudStackVolumeId";
    public static final String CloudStackVolumeSize = "CloudStackVolumeSize";
    public static final String CloudStackSnapshotId = "CloudStackSnapshotId";
    public static final String CloudStackSnapshotSize = "CloudStackSnapshotSize";
    public static final String CloudStackTemplateId = "CloudStackTemplateId";
    public static final String CloudStackTemplateSize = "CloudStackTemplateSize";

    public static final String ORIG_CS_VOLUME_ID = "originalCloudStackVolumeId";

    public static final String VOLUME_SIZE = "sfVolumeSize";

    public static final String STORAGE_POOL_ID = "sfStoragePoolId";

    public static final String DATACENTER = "datacenter";

    public static final String DATASTORE_NAME = "datastoreName";
    public static final String IQN = "iqn";

    private static final String SF_CS_ACCOUNT_PREFIX = "CloudStack_";

    public static final long MIN_VOLUME_SIZE = 1000000000;

    public static final long MIN_IOPS_PER_VOLUME = 100;
    public static final long MAX_MIN_IOPS_PER_VOLUME = 15000;
    public static final long MAX_IOPS_PER_VOLUME = 100000;

    private static final int DEFAULT_MANAGEMENT_PORT = 443;
    private static final int DEFAULT_STORAGE_PORT = 3260;

    private static final int MAX_NUM_VAGS_PER_VOLUME = 4;
    private static final int MAX_NUM_INITIATORS_PER_VAG = 64;

    public static class SolidFireConnection {
        private final String _managementVip;
        private final int _managementPort;
        private final String _clusterAdminUsername;
        private final String _clusterAdminPassword;

        public SolidFireConnection(String managementVip, int managementPort, String clusterAdminUsername, String clusterAdminPassword) {
            if (managementVip == null) {
                throw new CloudRuntimeException("The management VIP cannot be 'null'.");
            }

            if (managementPort <= 0) {
                throw new CloudRuntimeException("The management port must be a positive integer.");
            }

            if (clusterAdminUsername == null) {
                throw new CloudRuntimeException("The cluster admin username cannot be 'null'.");
            }

            if (clusterAdminPassword == null) {
                throw new CloudRuntimeException("The cluster admin password cannot be 'null'.");
            }

            _managementVip = managementVip;
            _managementPort = managementPort;
            _clusterAdminUsername = clusterAdminUsername;
            _clusterAdminPassword = clusterAdminPassword;
        }

        String getManagementVip() {
            return _managementVip;
        }

        int getManagementPort() {
            return _managementPort;
        }

        String getClusterAdminUsername() {
            return _clusterAdminUsername;
        }

        private String getClusterAdminPassword() {
            return _clusterAdminPassword;
        }

        @Override
        public int hashCode() {
            return _managementVip.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof SolidFireConnection)) {
                return false;
            }

            SolidFireConnection sfConnection = (SolidFireConnection)obj;

            return _managementVip.equals(sfConnection.getManagementVip());
        }
    }

    public static SolidFireConnection getSolidFireConnection(long storagePoolId, StoragePoolDetailsDao storagePoolDetailsDao) {
        StoragePoolDetailVO storagePoolDetail = storagePoolDetailsDao.findDetail(storagePoolId, SolidFireUtil.MANAGEMENT_VIP);

        String mVip = storagePoolDetail.getValue();

        storagePoolDetail = storagePoolDetailsDao.findDetail(storagePoolId, SolidFireUtil.MANAGEMENT_PORT);

        int mPort = Integer.parseInt(storagePoolDetail.getValue());

        storagePoolDetail = storagePoolDetailsDao.findDetail(storagePoolId, SolidFireUtil.CLUSTER_ADMIN_USERNAME);

        String clusterAdminUsername = storagePoolDetail.getValue();

        storagePoolDetail = storagePoolDetailsDao.findDetail(storagePoolId, SolidFireUtil.CLUSTER_ADMIN_PASSWORD);

        String clusterAdminPassword = storagePoolDetail.getValue();

        return new SolidFireConnection(mVip, mPort, clusterAdminUsername, clusterAdminPassword);
    }

    private static SolidFireElement getSolidFireElement(SolidFireConnection sfConnection) {
        return ElementFactory.create(sfConnection.getManagementVip(), sfConnection.getClusterAdminUsername(), sfConnection.getClusterAdminPassword());
    }

    // used to parse the "url" parameter when creating primary storage that's based on the SolidFire plug-in with the
    // name SolidFireUtil.PROVIDER_NAME (as opposed to the SolidFire plug-in with the name SolidFireUtil.SHARED_PROVIDER_NAME)
    // return a String instance that contains at most the MVIP and SVIP info
    public static String getModifiedUrl(String originalUrl) {
        StringBuilder sb = new StringBuilder();

        String delimiter = ";";

        StringTokenizer st = new StringTokenizer(originalUrl, delimiter);

        while (st.hasMoreElements()) {
            String token = st.nextElement().toString().toUpperCase();

            if (token.startsWith(SolidFireUtil.MANAGEMENT_VIP.toUpperCase()) || token.startsWith(SolidFireUtil.STORAGE_VIP.toUpperCase())) {
                sb.append(token).append(delimiter);
            }
        }

        String modifiedUrl = sb.toString();
        int lastIndexOf = modifiedUrl.lastIndexOf(delimiter);

        if (lastIndexOf == (modifiedUrl.length() - delimiter.length())) {
            return modifiedUrl.substring(0, lastIndexOf);
        }

        return modifiedUrl;
    }

    public static String getManagementVip(String url) {
        return getVip(SolidFireUtil.MANAGEMENT_VIP, url);
    }

    public static String getStorageVip(String url) {
        return getVip(SolidFireUtil.STORAGE_VIP, url);
    }

    public static int getManagementPort(String url) {
        return getPort(SolidFireUtil.MANAGEMENT_VIP, url, DEFAULT_MANAGEMENT_PORT);
    }

    public static int getStoragePort(String url) {
        return getPort(SolidFireUtil.STORAGE_VIP, url, DEFAULT_STORAGE_PORT);
    }

    public static String getValue(String keyToMatch, String url) {
        return getValue(keyToMatch, url, true);
    }

    public static String getValue(String keyToMatch, String url, boolean throwExceptionIfNotFound) {
        String delimiter1 = ";";
        String delimiter2 = "=";

        StringTokenizer st = new StringTokenizer(url, delimiter1);

        while (st.hasMoreElements()) {
            String token = st.nextElement().toString();

            int index = token.indexOf(delimiter2);

            if (index == -1) {
                throw new RuntimeException("Invalid URL format");
            }

            String key = token.substring(0, index);

            if (key.equalsIgnoreCase(keyToMatch)) {
                return token.substring(index + delimiter2.length());
            }
        }

        if (throwExceptionIfNotFound) {
            throw new RuntimeException("Key not found in URL");
        }

        return null;
    }

    public static String getSolidFireAccountName(String csAccountUuid, long csAccountId) {
        return SF_CS_ACCOUNT_PREFIX + csAccountUuid + "_" + csAccountId;
    }

    public static void updateCsDbWithSolidFireIopsInfo(long storagePoolId, PrimaryDataStoreDao primaryDataStoreDao,
                                                       StoragePoolDetailsDao storagePoolDetailsDao, long minIops, long maxIops, long burstIops) {
        Map<String, String> existingDetails = storagePoolDetailsDao.listDetailsKeyPairs(storagePoolId);
        Set<String> existingKeys = existingDetails.keySet();

        Map<String, String> existingDetailsToKeep = new HashMap<>();

        for (String existingKey : existingKeys) {
            String existingValue = existingDetails.get(existingKey);

            if (!SolidFireUtil.MIN_IOPS.equalsIgnoreCase(existingValue) &&
                    !SolidFireUtil.MAX_IOPS.equalsIgnoreCase(existingValue) &&
                    !SolidFireUtil.BURST_IOPS.equalsIgnoreCase(existingValue)) {
                existingDetailsToKeep.put(existingKey, existingValue);
            }
        }

        existingDetailsToKeep.put(SolidFireUtil.MIN_IOPS, String.valueOf(minIops));
        existingDetailsToKeep.put(SolidFireUtil.MAX_IOPS, String.valueOf(maxIops));
        existingDetailsToKeep.put(SolidFireUtil.BURST_IOPS, String.valueOf(burstIops));

        primaryDataStoreDao.updateDetails(storagePoolId, existingDetailsToKeep);
    }

    public static void updateCsDbWithSolidFireAccountInfo(long csAccountId, SolidFireUtil.SolidFireAccount sfAccount,
                                                          long storagePoolId, AccountDetailsDao accountDetailsDao) {
        AccountDetailVO accountDetail = new AccountDetailVO(csAccountId,
                SolidFireUtil.getAccountKey(storagePoolId),
                String.valueOf(sfAccount.getId()));

        accountDetailsDao.persist(accountDetail);
    }

    public static SolidFireAccount getAccount(SolidFireConnection sfConnection, String sfAccountName) {
        try {
            return getAccountByName(sfConnection, sfAccountName);
        } catch (Exception ex) {
            return null;
        }
    }

    private static boolean isCloudStackOnlyVag(SolidFireConnection sfConnection, SolidFireVag sfVag) {
        long[] volumeIds = sfVag.getVolumeIds();

        if (ArrayUtils.isEmpty(volumeIds)) {
            // We count this situation as being "CloudStack only" because the reason we call this method is to determine
            // if we can remove a host from a VAG (we only want to allow the host to be removed from the VAG if there are
            // no non-CloudStack volumes in it).
            return true;
        }

        List<Long> knownSfAccountsForCs = new ArrayList<>();

        for (long volumeId : volumeIds) {
            SolidFireVolume sfVolume = getVolume(sfConnection, volumeId);
            long sfAccountId = sfVolume.getAccountId();

            if (!knownSfAccountsForCs.contains(sfAccountId)) {
                SolidFireAccount sfAccount = getAccountById(sfConnection, sfAccountId);

                if (sfAccount.getName().startsWith(SF_CS_ACCOUNT_PREFIX)) {
                    knownSfAccountsForCs.add(sfAccountId);
                }
                else {
                    return false;
                }
            }
        }

        return true;
    }

    private static boolean isStorageApplicableToZoneOrCluster(StoragePoolVO storagePoolVO, long clusterId, ClusterDao clusterDao) {
        if (storagePoolVO.getClusterId() != null) {
            if (storagePoolVO.getClusterId() == clusterId) {
                return true;
            }
        }
        else {
            List<ClusterVO> clustersInZone = clusterDao.listClustersByDcId(storagePoolVO.getDataCenterId());

            if (clustersInZone != null) {
                for (ClusterVO clusterInZone : clustersInZone) {
                    if (clusterInZone.getId() == clusterId) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public static void hostRemovedFromCluster(long hostId, long clusterId, String storageProvider, ClusterDao clusterDao, HostDao hostDao,
                                              PrimaryDataStoreDao storagePoolDao, StoragePoolDetailsDao storagePoolDetailsDao) {
        HostVO hostVO = hostDao.findByIdIncludingRemoved(hostId);

        Preconditions.checkArgument(hostVO != null, "Could not locate host for ID: " + hostId);

        ClusterVO cluster = clusterDao.findById(clusterId);

        GlobalLock lock = GlobalLock.getInternLock(cluster.getUuid());

        if (!lock.lock(LOCK_TIME_IN_SECONDS)) {
            String errMsg = "Couldn't lock the DB on the following string: " + cluster.getUuid();

            LOGGER.warn(errMsg);

            throw new CloudRuntimeException(errMsg);
        }

        try {
            List<StoragePoolVO> storagePools = storagePoolDao.findPoolsByProvider(storageProvider);

            if (storagePools != null && storagePools.size() > 0) {
                List<SolidFireUtil.SolidFireConnection> sfConnections = new ArrayList<>();

                for (StoragePoolVO storagePool : storagePools) {
                    if (!isStorageApplicableToZoneOrCluster(storagePool, clusterId, clusterDao)) {
                        continue;
                    }

                    SolidFireUtil.SolidFireConnection sfConnection = SolidFireUtil.getSolidFireConnection(storagePool.getId(), storagePoolDetailsDao);

                    if (!sfConnections.contains(sfConnection)) {
                        sfConnections.add(sfConnection);

                        List<SolidFireUtil.SolidFireVag> sfVags = SolidFireUtil.getAllVags(sfConnection);
                        SolidFireVag sfVag = getVolumeAccessGroup(hostVO.getStorageUrl(), sfVags);

                        if (sfVag != null && isCloudStackOnlyVag(sfConnection, sfVag)) {
                            removeInitiatorsFromSolidFireVag(sfConnection, sfVag.getId(), new String[] { hostVO.getStorageUrl() });
                        }
                    }
                }
            }
        }
        finally {
            lock.unlock();
            lock.releaseRef();
        }
    }

    public static void hostAddedToCluster(long hostId, long clusterId, String storageProvider, ClusterDao clusterDao, HostDao hostDao,
                                          PrimaryDataStoreDao storagePoolDao, StoragePoolDetailsDao storagePoolDetailsDao) {
        HostVO hostVO = hostDao.findById(hostId);

        Preconditions.checkArgument(hostVO != null, "Could not locate host for ID: " + hostId);

        ClusterVO cluster = clusterDao.findById(clusterId);

        GlobalLock lock = GlobalLock.getInternLock(cluster.getUuid());

        if (!lock.lock(LOCK_TIME_IN_SECONDS)) {
            String errMsg = "Couldn't lock the DB on the following string: " + cluster.getUuid();

            LOGGER.warn(errMsg);

            throw new CloudRuntimeException(errMsg);
        }

        try {
            List<StoragePoolVO> storagePools = storagePoolDao.findPoolsByProvider(storageProvider);

            if (storagePools != null && storagePools.size() > 0) {
                List<SolidFireUtil.SolidFireConnection> sfConnections = new ArrayList<>();

                for (StoragePoolVO storagePool : storagePools) {
                    if (!isStorageApplicableToZoneOrCluster(storagePool, clusterId, clusterDao)) {
                        continue;
                    }

                    SolidFireUtil.SolidFireConnection sfConnection = SolidFireUtil.getSolidFireConnection(storagePool.getId(), storagePoolDetailsDao);

                    if (!sfConnections.contains(sfConnection)) {
                        sfConnections.add(sfConnection);

                        List<SolidFireUtil.SolidFireVag> sfVags = SolidFireUtil.getAllVags(sfConnection);
                        SolidFireVag sfVag = getVolumeAccessGroup(hostVO.getStorageUrl(), sfVags);

                        if (sfVag != null) {
                            placeVolumeIdsInVag(sfConnection, sfVags, sfVag, hostVO, hostDao);
                        } else {
                            handleVagForHost(sfConnection, sfVags, hostVO, hostDao, clusterDao);
                        }
                    }
                }
            }
        }
        finally {
            lock.unlock();
            lock.releaseRef();
        }
    }

    // Put the host in an existing VAG or create a new one (only create a new one if all existing VAGs are full (i.e. 64 hosts max per VAG) and if
    // creating a new VAG won't exceed 4 VAGs for the computer cluster).
    // If none of the hosts in the cluster are in a VAG, then leave this host out of a VAG.
    // Place applicable volume IDs in VAG, if need be (account of volume starts with SF_CS_ACCOUNT_PREFIX).
    private static void handleVagForHost(SolidFireUtil.SolidFireConnection sfConnection, List<SolidFireUtil.SolidFireVag> sfVags, Host host, HostDao hostDao, ClusterDao clusterDao) {
        List<HostVO> hostVOs = hostDao.findByClusterId(host.getClusterId());

        if (hostVOs != null) {
            int numVags = 0;

            addInitiatorsToExistingVag(clusterDao, host, sfVags, sfConnection);

            Collections.shuffle(hostVOs, RANDOM);

            for (HostVO hostVO : hostVOs) {
                if (hostVO.getId() != host.getId()) {
                    SolidFireVag sfVag = getVolumeAccessGroup(hostVO.getStorageUrl(), sfVags);

                    if (sfVag != null) {
                        numVags++;

                        // A volume should be visible to all hosts that are in the same compute cluster. That being the case, you
                        // can use MAX_NUM_VAGS_PER_VOLUME here. This is to limit the number of VAGs being used in a compute cluster
                        // to MAX_NUM_VAGS_PER_VOLUME.
                        if (numVags > MAX_NUM_VAGS_PER_VOLUME) {
                            throw new CloudRuntimeException("Can support at most four volume access groups per compute cluster (>)");
                        }

                        if (sfVag.getInitiators().length < MAX_NUM_INITIATORS_PER_VAG) {
                            if (!hostSupports_iScsi(host)) {
                                String errMsg = "Host with ID " + host.getId() + " does not support iSCSI.";

                                LOGGER.warn(errMsg);

                                throw new CloudRuntimeException(errMsg);
                            }

                            if(!isInitiatorInSfVag(host.getStorageUrl(),sfVag)) {
                                addInitiatorsToSolidFireVag(sfConnection, sfVag.getId(), new String[]{host.getStorageUrl()});
                            }
                            return;
                        }
                    }
                }
            }

            if (numVags == MAX_NUM_VAGS_PER_VOLUME) {
                throw new CloudRuntimeException("Can support at most four volume access groups per compute cluster (==)");
            }

            if (numVags > 0) {
                if (!hostSupports_iScsi(host)) {
                    String errMsg = "Host with ID " + host.getId() + " does not support iSCSI.";

                    LOGGER.warn(errMsg);

                    throw new CloudRuntimeException(errMsg);
                }

                SolidFireUtil.createVag(sfConnection, "CloudStack-" + UUID.randomUUID().toString(),
                        new String[]{host.getStorageUrl()}, getVolumeIds(sfConnection, sfVags, host, hostDao));
            }
        }
    }

    private static void addInitiatorsToExistingVag(ClusterDao clusterDao, Host host, List<SolidFireUtil.SolidFireVag> sfVags, SolidFireUtil.SolidFireConnection sfConnection){
        String clusterUuId = clusterDao.findById(host.getClusterId()).getUuid();
        SolidFireVag sfVagMatchingClusterId = sfVags.stream().filter(vag -> vag.getName().equals("CloudStack-"+clusterUuId)).findFirst().orElse(null);
        if (sfVagMatchingClusterId != null && sfVagMatchingClusterId.getInitiators().length < MAX_NUM_INITIATORS_PER_VAG) {
            addInitiatorsToSolidFireVag(sfConnection, sfVagMatchingClusterId.getId(), new String[]{host.getStorageUrl()});
        }
    }

    /**
     * Make use of the volume access group (VAG) of a random host in the cluster. With this VAG, collect all of its volume IDs that are for
     * volumes that are in SolidFire accounts that are for CloudStack.
     */
    private static long[] getVolumeIds(SolidFireUtil.SolidFireConnection sfConnection, List<SolidFireUtil.SolidFireVag> sfVags,
                                       Host host, HostDao hostDao) {
        List<Long> volumeIdsToReturn = new ArrayList<>();

        SolidFireVag sfVagForRandomHostInCluster = getVagForRandomHostInCluster(sfVags, host, hostDao);

        if (sfVagForRandomHostInCluster != null) {
            long[] volumeIds = sfVagForRandomHostInCluster.getVolumeIds();

            if (volumeIds != null) {
                List<Long> knownSfAccountsForCs = new ArrayList<>();
                List<Long> knownSfAccountsNotForCs = new ArrayList<>();

                for (long volumeId : volumeIds) {
                    SolidFireVolume sfVolume = getVolume(sfConnection, volumeId);
                    long sfAccountId = sfVolume.getAccountId();

                    if (knownSfAccountsForCs.contains(sfAccountId)) {
                        volumeIdsToReturn.add(volumeId);
                    }
                    else if (!knownSfAccountsNotForCs.contains(sfAccountId)) {
                        SolidFireAccount sfAccount = getAccountById(sfConnection, sfAccountId);

                        if (sfAccount.getName().startsWith(SF_CS_ACCOUNT_PREFIX)) {
                            knownSfAccountsForCs.add(sfAccountId);

                            volumeIdsToReturn.add(volumeId);
                        }
                        else {
                            knownSfAccountsNotForCs.add(sfAccountId);
                        }
                    }
                }
            }
        }

        return volumeIdsToReturn.stream().mapToLong(l -> l).toArray();
    }

    private static void placeVolumeIdsInVag(SolidFireUtil.SolidFireConnection sfConnection, List<SolidFireUtil.SolidFireVag> sfVags,
                                            SolidFireVag sfVag, Host host, HostDao hostDao) {
        SolidFireVag sfVagForRandomHostInCluster = getVagForRandomHostInCluster(sfVags, host, hostDao);

        if (sfVagForRandomHostInCluster != null) {
            long[] volumeIds = sfVagForRandomHostInCluster.getVolumeIds();

            if (volumeIds != null) {
                List<Long> knownSfAccountsForCs = new ArrayList<>();
                List<Long> knownSfAccountsNotForCs = new ArrayList<>();

                List<Long> newVolumeIds = new ArrayList<>();

                for (long volumeId : volumeIds) {
                    SolidFireVolume sfVolume = getVolume(sfConnection, volumeId);
                    long sfAccountId = sfVolume.getAccountId();

                    if (knownSfAccountsForCs.contains(sfAccountId)) {
                        addVolumeIdToSolidFireVag(volumeId, sfVag, newVolumeIds);
                    }
                    else if (!knownSfAccountsNotForCs.contains(sfAccountId)) {
                        SolidFireAccount sfAccount = getAccountById(sfConnection, sfAccountId);

                        if (sfAccount.getName().startsWith(SF_CS_ACCOUNT_PREFIX)) {
                            knownSfAccountsForCs.add(sfAccountId);

                            addVolumeIdToSolidFireVag(volumeId, sfVag, newVolumeIds);
                        }
                        else {
                            knownSfAccountsNotForCs.add(sfAccountId);
                        }
                    }
                }

                if (newVolumeIds.size() > 0) {
                    addVolumeIdsToSolidFireVag(sfConnection, sfVag.getId(), newVolumeIds.toArray(new Long[0]));
                }
            }
        }
    }

    private static void addVolumeIdToSolidFireVag(long volumeId, SolidFireVag sfVag, List<Long> newVolumeIds) {
        List<Long> existingVolumeIds = Longs.asList(sfVag.getVolumeIds());

        if (!existingVolumeIds.contains(volumeId) && !newVolumeIds.contains(volumeId)) {
            newVolumeIds.add(volumeId);
        }
    }

    private static SolidFireVag getVagForRandomHostInCluster(List<SolidFireUtil.SolidFireVag> sfVags, Host host, HostDao hostDao) {
        List<HostVO> hostVOs = hostDao.findByClusterId(host.getClusterId());

        if (hostVOs != null) {
            Collections.shuffle(hostVOs, RANDOM);

            for (HostVO hostVO : hostVOs) {
                if (hostVO.getId() != host.getId() && hostSupports_iScsi(hostVO)) {
                    SolidFireVag sfVag = getVolumeAccessGroup(hostVO.getStorageUrl(), sfVags);

                    if (sfVag != null) {
                        return sfVag;
                    }
                }
            }
        }

        return null;
    }

    public static void placeVolumeInVolumeAccessGroups(SolidFireConnection sfConnection, long sfVolumeId, List<HostVO> hosts, String clusterUuId) {
        if (!SolidFireUtil.hostsSupport_iScsi(hosts)) {
            String errMsg = "Not all hosts in the compute cluster support iSCSI.";

            LOGGER.warn(errMsg);

            throw new CloudRuntimeException(errMsg);
        }
        List<SolidFireUtil.SolidFireVag> sfVags = SolidFireUtil.getAllVags(sfConnection);
        Map<SolidFireUtil.SolidFireVag, List<String>> sfVagToIqnsMap = buildVagToIQNMap(hosts, sfVags);
        if (sfVagToIqnsMap.size() > MAX_NUM_VAGS_PER_VOLUME) {
            throw new CloudRuntimeException("A SolidFire volume can be in at most four volume access groups simultaneously.");
        }
        if (sfVagToIqnsMap.containsKey(null)) {
            sfVagToIqnsMap = updateNullKeyInSfVagToIqnsMap(sfVagToIqnsMap, sfVags, sfConnection, clusterUuId, sfVolumeId);
        }
        addVolumestoVagIfNotPresent(sfVagToIqnsMap.keySet(), sfVolumeId, sfConnection);
    }

    private static Map<SolidFireUtil.SolidFireVag, List<String>> updateNullKeyInSfVagToIqnsMap(Map<SolidFireUtil.SolidFireVag,List<String>> sfVagToIqnsMap, List <SolidFireUtil.SolidFireVag> sfVags, SolidFireConnection sfConnection, String clusterUuId, long sfVolumeId){
        SolidFireUtil.SolidFireVag sfVagMatchingClusterId = createClusterVagIfDoesntExist(sfVags, sfConnection, clusterUuId, sfVagToIqnsMap, sfVolumeId);
        sfVagToIqnsMap.put(sfVagMatchingClusterId, sfVagToIqnsMap.get(null));
        sfVagToIqnsMap.remove(null);
        return sfVagToIqnsMap;
    }

    private static SolidFireVag createClusterVagIfDoesntExist(List<SolidFireUtil.SolidFireVag> sfVags, SolidFireConnection sfConnection, String clusterUuId, Map<SolidFireUtil.SolidFireVag, List<String>> sfVagToIqnsMap, long sfVolumeId) {
        SolidFireVag sfVagMatchingClusterId = sfVags.stream().filter(vag -> vag.getName().equals("CloudStack-" + clusterUuId)).findFirst().orElse(null);
        if (sfVagMatchingClusterId == null) {
            LOGGER.info("Creating volume access group CloudStack-" + clusterUuId);
            SolidFireUtil.createVag(sfConnection, "CloudStack-" + clusterUuId, sfVagToIqnsMap.get(null).toArray(new String[0]), new long[]{sfVolumeId});
            sfVags = SolidFireUtil.getAllVags(sfConnection);
            return sfVags.stream().filter(vag -> vag.getName().equals("CloudStack-" + clusterUuId)).findFirst().orElse(null);
        }else{
            return sfVagMatchingClusterId;
        }
    }

    private static void addVolumestoVagIfNotPresent(Set<SolidFireUtil.SolidFireVag> sfVagSet, long sfVolumeId, SolidFireConnection sfConnection){
        for (SolidFireUtil.SolidFireVag sfVag : sfVagSet) {
            if (sfVag != null) {
                if (!SolidFireUtil.isVolumeIdInSfVag(sfVolumeId, sfVag)) {
                    SolidFireUtil.addVolumeIdsToSolidFireVag(sfConnection, sfVag.getId(), new Long[] { sfVolumeId });
                }
            }
        }
    }


    private static Map<SolidFireVag,List<String>> buildVagToIQNMap(List<HostVO> hosts, List<SolidFireVag> sfVags) {

        Map<SolidFireUtil.SolidFireVag, List<String>> sfVagToIqnsMap = new HashMap<>();
        for (HostVO hostVO : hosts) {
            String iqn = hostVO.getStorageUrl();

            SolidFireUtil.SolidFireVag sfVag = getVolumeAccessGroup(iqn, sfVags);

            List<String> iqnsInVag = sfVagToIqnsMap.computeIfAbsent(sfVag, k -> new ArrayList<>());

            iqnsInVag.add(iqn);
        }
        return sfVagToIqnsMap;

    }

    public static SolidFireUtil.SolidFireVag getVolumeAccessGroup(String hostIqn, List<SolidFireUtil.SolidFireVag> sfVags) {
        if (hostIqn == null) {
            return null;
        }

        hostIqn = hostIqn.toLowerCase();

        if (sfVags != null) {
            for (SolidFireUtil.SolidFireVag sfVag : sfVags) {
                List<String> lstInitiators = getStringArrayAsLowerCaseStringList(sfVag.getInitiators());

                // lstInitiators should not be returned from getStringArrayAsLowerCaseStringList as null
                if (lstInitiators.contains(hostIqn)) {
                    return sfVag;
                }
            }
        }

        return null;
    }

    public static boolean sfVagContains(SolidFireUtil.SolidFireVag sfVag, long sfVolumeId, long clusterId, HostDao hostDao) {
        if (isVolumeIdInSfVag(sfVolumeId, sfVag)) {
            String[] iqns = sfVag.getInitiators();
            List<HostVO> hosts = hostDao.findByClusterId(clusterId);

            for (String iqn : iqns) {
                for (HostVO host : hosts) {
                    String hostIqn = host.getStorageUrl();

                    if (iqn.equalsIgnoreCase(hostIqn)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private static boolean isVolumeIdInSfVag(long sfVolumeIdToCheck, SolidFireUtil.SolidFireVag sfVag) {
        long[] sfVolumeIds = sfVag.getVolumeIds();

        for (long sfVolumeId : sfVolumeIds) {
            if (sfVolumeId == sfVolumeIdToCheck) {
                return true;
            }
        }

        return false;
    }

    private static boolean isInitiatorInSfVag(String initiatorName, SolidFireUtil.SolidFireVag sfVag) {
        String[] initiatorsList = sfVag.getInitiators();

        for (String initiator : initiatorsList) {
            if (initiatorName.equals(initiator)) {
                return true;
            }
        }

        return false;
    }

    private static boolean hostSupports_iScsi(Host host) {
        return host != null && host.getStorageUrl() != null && host.getStorageUrl().trim().length() > 0 && host.getStorageUrl().startsWith("iqn");
    }

    private static boolean hostsSupport_iScsi(List<HostVO> hosts) {
        if (hosts == null || hosts.size() == 0) {
            return false;
        }

        for (Host host : hosts) {
            if (!hostSupports_iScsi(host)) {
                return false;
            }
        }

        return true;
    }

    public static String getVagKey(long storagePoolId) {
        return "sfVolumeAccessGroup_" + storagePoolId;
    }

    public static String getAccountKey(long storagePoolId) {
        return SolidFireUtil.ACCOUNT_ID + "_" + storagePoolId;
    }

    public static AccountDetailVO getAccountDetail(long csAccountId, long storagePoolId, AccountDetailsDao accountDetailsDao) {
        AccountDetailVO accountDetail = accountDetailsDao.findDetail(csAccountId, SolidFireUtil.getAccountKey(storagePoolId));

        if (accountDetail == null || accountDetail.getValue() == null) {
            accountDetail = accountDetailsDao.findDetail(csAccountId, SolidFireUtil.ACCOUNT_ID);
        }

        return accountDetail;
    }

    public static String getSolidFireVolumeName(String strCloudStackVolumeName) {
        final String specialChar = "-";

        StringBuilder strSolidFireVolumeName = new StringBuilder();

        for (int i = 0; i < strCloudStackVolumeName.length(); i++) {
            String strChar = strCloudStackVolumeName.substring(i, i + 1);

            if (StringUtils.isAlphanumeric(strChar)) {
                strSolidFireVolumeName.append(strChar);
            } else {
                strSolidFireVolumeName.append(specialChar);
            }
        }

        return strSolidFireVolumeName.toString();
    }

    public static long createVolume(SolidFireConnection sfConnection, String volumeName, long accountId, long totalSize,
                                    boolean enable512e, Map<String, String> mapAttributes, long minIops, long maxIops, long burstIops) {
        CreateVolumeRequest request = CreateVolumeRequest.builder()
                .name(volumeName)
                .accountID(accountId)
                .totalSize(totalSize)
                .enable512e(enable512e)
                .optionalAttributes(convertMap(mapAttributes))
                .optionalQos(new QoS(Optional.of(minIops), Optional.of(maxIops), Optional.of(burstIops), Optional.EMPTY_LONG))
                .build();

        return getSolidFireElement(sfConnection).createVolume(request).getVolumeID();
    }

    public static void modifyVolume(SolidFireConnection sfConnection, long volumeId, Long totalSize, Map<String, String> mapAttributes,
                                    long minIops, long maxIops, long burstIops) {
        ModifyVolumeRequest request = ModifyVolumeRequest.builder()
                .volumeID(volumeId)
                .optionalTotalSize(totalSize)
                .optionalAttributes(convertMap(mapAttributes))
                .optionalQos(new QoS(Optional.of(minIops), Optional.of(maxIops), Optional.of(burstIops), Optional.EMPTY_LONG))
                .build();

        getSolidFireElement(sfConnection).modifyVolume(request);
    }

    public static void modifyVolumeQoS(SolidFireConnection sfConnection, long volumeId, long minIops, long maxIops, long burstIops) {
        ModifyVolumeRequest request = ModifyVolumeRequest.builder()
                .volumeID(volumeId)
                .optionalQos(new QoS(Optional.of(minIops), Optional.of(maxIops), Optional.of(burstIops), Optional.EMPTY_LONG))
                .build();

        getSolidFireElement(sfConnection).modifyVolume(request);
    }

    public static SolidFireVolume getVolume(SolidFireConnection sfConnection, long volumeId) {
        ListVolumesRequest request = ListVolumesRequest.builder()
                .optionalStartVolumeID(volumeId)
                .optionalLimit(1L)
                .build();

        Volume volume = getSolidFireElement(sfConnection).listVolumes(request).getVolumes()[0];

        return new SolidFireVolume(volume.getVolumeID(), volume.getName(), volume.getIqn(), volume.getAccountID(), volume.getStatus(),
                volume.getEnable512e(), volume.getQos().getMinIOPS(), volume.getQos().getMaxIOPS(), volume.getQos().getBurstIOPS(),
                volume.getTotalSize(), volume.getScsiNAADeviceID());
    }

    public static void deleteVolume(SolidFireConnection sfConnection, long volumeId) {
        DeleteVolumeRequest request = DeleteVolumeRequest.builder()
                .volumeID(volumeId)
                .build();

        getSolidFireElement(sfConnection).deleteVolume(request);
    }

    private static final String ACTIVE = "active";

    public static class SolidFireVolume {
        private final long _id;
        private final String _name;
        private final String _iqn;
        private final long _accountId;
        private final String _status;
        private final boolean _enable512e;
        private final long _minIops;
        private final long _maxIops;
        private final long _burstIops;
        private final long _totalSize;
        private final String _scsiNaaDeviceId;

        SolidFireVolume(long id, String name, String iqn, long accountId, String status, boolean enable512e,
                        long minIops, long maxIops, long burstIops, long totalSize, String scsiNaaDeviceId) {
            _id = id;
            _name = name;
            _iqn = "/" + iqn + "/0";
            _accountId = accountId;
            _status = status;
            _enable512e = enable512e;
            _minIops = minIops;
            _maxIops = maxIops;
            _burstIops = burstIops;
            _totalSize = totalSize;
            _scsiNaaDeviceId = scsiNaaDeviceId;
        }

        public long getId() {
            return _id;
        }

        public String getName() {
            return _name;
        }

        public String getIqn() {
            return _iqn;
        }

        public long getAccountId() {
            return _accountId;
        }

        public boolean isActive() {
            return ACTIVE.equalsIgnoreCase(_status);
        }

        public boolean isEnable512e() {
            return _enable512e;
        }

        public long getMinIops() {
            return _minIops;
        }

        public long getMaxIops() {
            return _maxIops;
        }

        public long getBurstIops() {
            return _burstIops;
        }

        public long getTotalSize() {
            return _totalSize;
        }

        public String getScsiNaaDeviceId() {
            return _scsiNaaDeviceId;
        }

        @Override
        public int hashCode() {
            return _iqn.hashCode();
        }

        @Override
        public String toString() {
            return _name;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }

            if (!obj.getClass().equals(SolidFireVolume.class)) {
                return false;
            }

            SolidFireVolume sfv = (SolidFireVolume)obj;

            return _id == sfv._id && _name.equals(sfv._name) && _iqn.equals(sfv._iqn) && _accountId == sfv._accountId &&
                    isActive() == sfv.isActive() && getTotalSize() == sfv.getTotalSize();
        }
    }

    public static long createSnapshot(SolidFireConnection sfConnection, long volumeId, String snapshotName, Map<String, String> mapAttributes) {
        CreateSnapshotRequest request = CreateSnapshotRequest.builder()
                .volumeID(volumeId)
                .optionalName(snapshotName)
                .optionalAttributes(convertMap(mapAttributes))
                .build();

        return getSolidFireElement(sfConnection).createSnapshot(request).getSnapshotID();
    }

    public static SolidFireSnapshot getSnapshot(SolidFireConnection sfConnection, long volumeId, long snapshotId) {
        ListSnapshotsRequest request = ListSnapshotsRequest.builder()
                .optionalVolumeID(volumeId)
                .build();

        Snapshot[] snapshots = getSolidFireElement(sfConnection).listSnapshots(request).getSnapshots();

        String snapshotName = null;
        long totalSize = 0;

        if (snapshots != null) {
            for (Snapshot snapshot : snapshots) {
                if (snapshot.getSnapshotID() == snapshotId) {
                    snapshotName = snapshot.getName();
                    totalSize = snapshot.getTotalSize();

                    break;
                }
            }
        }

        if (snapshotName == null) {
            throw new CloudRuntimeException("Could not find SolidFire snapshot ID: " + snapshotId + " for the following SolidFire volume ID: " + volumeId);
        }

        return new SolidFireSnapshot(snapshotId, snapshotName, totalSize);
    }

    public static void deleteSnapshot(SolidFireConnection sfConnection, long snapshotId) {
        DeleteSnapshotRequest request = DeleteSnapshotRequest.builder()
                .snapshotID(snapshotId)
                .build();

        getSolidFireElement(sfConnection).deleteSnapshot(request);
    }

    public static void rollBackVolumeToSnapshot(SolidFireConnection sfConnection, long volumeId, long snapshotId) {
        RollbackToSnapshotRequest request = RollbackToSnapshotRequest.builder()
                .volumeID(volumeId)
                .snapshotID(snapshotId)
                .build();

        getSolidFireElement(sfConnection).rollbackToSnapshot(request);
    }

    public static class SolidFireSnapshot {
        private final long _id;
        private final String _name;
        private final long _totalSize;

        SolidFireSnapshot(long id, String name, long totalSize) {
            _id = id;
            _name = name;
            _totalSize = totalSize;
        }

        public long getId() {
            return _id;
        }

        public String getName() {
            return _name;
        }

        public long getTotalSize() {
            return _totalSize;
        }
    }

    public static long createClone(SolidFireConnection sfConnection, long volumeId, long snapshotId, long accountId,
                                   String cloneName, Map<String, String> mapAttributes) {
        CloneVolumeRequest request = CloneVolumeRequest.builder()
                .volumeID(volumeId)
                .optionalSnapshotID(snapshotId < 1 ? null : snapshotId)
                .optionalNewAccountID(accountId)
                .name(cloneName)
                .optionalAttributes(convertMap(mapAttributes))
                .build();

        CloneVolumeResult result = getSolidFireElement(sfConnection).cloneVolume(request);

        // Clone is an async operation. Poll until we get data.

        GetAsyncResultRequest asyncResultRequest = GetAsyncResultRequest.builder()
                .asyncHandle(result.getAsyncHandle())
                .build();

        do {
            String status = getSolidFireElement(sfConnection).getAsyncResult(asyncResultRequest).getStatus();

            if (status.equals("complete")) {
                break;
            }

            try {
                Thread.sleep(500); // sleep for 1/2 of a second
            }
            catch (Exception ex) {
                // ignore
            }
        }
        while (true);

        return result.getVolumeID();
    }

    public static long createAccount(SolidFireConnection sfConnection, String accountName) {
        AddAccountRequest request = AddAccountRequest.builder()
                .username(accountName)
                .build();

        return getSolidFireElement(sfConnection).addAccount(request).getAccountID();
    }

    public static SolidFireAccount getAccountById(SolidFireConnection sfConnection, long accountId) {
        GetAccountByIDRequest request = GetAccountByIDRequest.builder()
                .accountID(accountId)
                .build();

        Account sfAccount = getSolidFireElement(sfConnection).getAccountByID(request).getAccount();

        String sfAccountName = sfAccount.getUsername();
        String sfAccountInitiatorSecret = sfAccount.getInitiatorSecret().isPresent() ? sfAccount.getInitiatorSecret().get().toString() : "";
        String sfAccountTargetSecret = sfAccount.getTargetSecret().isPresent() ? sfAccount.getTargetSecret().get().toString() : "";

        return new SolidFireAccount(accountId, sfAccountName, sfAccountInitiatorSecret, sfAccountTargetSecret);
    }

    private static SolidFireAccount getAccountByName(SolidFireConnection sfConnection, String accountName) {
        GetAccountByNameRequest request = GetAccountByNameRequest.builder()
                .username(accountName)
                .build();

        Account sfAccount = getSolidFireElement(sfConnection).getAccountByName(request).getAccount();

        long sfAccountId = sfAccount.getAccountID();
        String sfAccountInitiatorSecret = sfAccount.getInitiatorSecret().isPresent() ? sfAccount.getInitiatorSecret().get().toString() : "";
        String sfAccountTargetSecret = sfAccount.getTargetSecret().isPresent() ? sfAccount.getTargetSecret().get().toString() : "";

        return new SolidFireAccount(sfAccountId, accountName, sfAccountInitiatorSecret, sfAccountTargetSecret);
    }

    public static class SolidFireAccount {
        private final long _id;
        private final String _name;
        private final String _initiatorSecret;
        private final String _targetSecret;

        SolidFireAccount(long id, String name, String initiatorSecret, String targetSecret) {
            _id = id;
            _name = name;
            _initiatorSecret = initiatorSecret;
            _targetSecret = targetSecret;
        }

        public long getId() {
            return _id;
        }

        public String getName() {
            return _name;
        }

        @Override
        public int hashCode() {
            return (_id + _name).hashCode();
        }

        @Override
        public String toString() {
            return _name;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }

            if (!obj.getClass().equals(SolidFireAccount.class)) {
                return false;
            }

            SolidFireAccount sfa = (SolidFireAccount)obj;

            return _id == sfa._id && _name.equals(sfa._name) &&
                    _initiatorSecret.equals(sfa._initiatorSecret) &&
                    _targetSecret.equals(sfa._targetSecret);
        }
    }

    private static long createVag(SolidFireConnection sfConnection, String vagName, String[] iqns, long[] volumeIds) {
        CreateVolumeAccessGroupRequest request = CreateVolumeAccessGroupRequest.builder()
                .name(vagName)
                .optionalInitiators(iqns)
                .optionalVolumes(Longs.asList(volumeIds).toArray(new Long[volumeIds.length]))
                .build();

        return getSolidFireElement(sfConnection).createVolumeAccessGroup(request).getVolumeAccessGroupID();
    }

    private static void addInitiatorsToSolidFireVag(SolidFireConnection sfConnection, long vagId, String[] initiators) {
        AddInitiatorsToVolumeAccessGroupRequest request = AddInitiatorsToVolumeAccessGroupRequest.builder()
                .volumeAccessGroupID(vagId)
                .initiators(initiators)
                .build();

        getSolidFireElement(sfConnection).addInitiatorsToVolumeAccessGroup(request);
    }

    private static void removeInitiatorsFromSolidFireVag(SolidFireConnection sfConnection, long vagId, String[] initiators) {
        RemoveInitiatorsFromVolumeAccessGroupRequest request = RemoveInitiatorsFromVolumeAccessGroupRequest.builder()
                .volumeAccessGroupID(vagId)
                .initiators(initiators)
                .build();

        getSolidFireElement(sfConnection).removeInitiatorsFromVolumeAccessGroup(request);
    }

    private static void addVolumeIdsToSolidFireVag(SolidFireConnection sfConnection, long vagId, Long[] volumeIds) {
        AddVolumesToVolumeAccessGroupRequest request = AddVolumesToVolumeAccessGroupRequest.builder()
                .volumeAccessGroupID(vagId)
                .volumes(volumeIds)
                .build();

        getSolidFireElement(sfConnection).addVolumesToVolumeAccessGroup(request);
    }

    public static void removeVolumeIdsFromSolidFireVag(SolidFireConnection sfConnection, long vagId, Long[] volumeIds) {
        RemoveVolumesFromVolumeAccessGroupRequest request = RemoveVolumesFromVolumeAccessGroupRequest.builder()
                .volumeAccessGroupID(vagId)
                .volumes(volumeIds)
                .build();

        getSolidFireElement(sfConnection).removeVolumesFromVolumeAccessGroup(request);
    }

    public static List<SolidFireVag> getAllVags(SolidFireConnection sfConnection)
    {
        ListVolumeAccessGroupsRequest request = ListVolumeAccessGroupsRequest.builder().build();

        VolumeAccessGroup[] vags = getSolidFireElement(sfConnection).listVolumeAccessGroups(request).getVolumeAccessGroups();

        List<SolidFireVag> lstSolidFireVags = new ArrayList<>();

        if (vags != null) {
            for (VolumeAccessGroup vag : vags) {
                SolidFireVag sfVag = new SolidFireVag(vag.getVolumeAccessGroupID(), vag.getInitiators(), toPrimitive(vag.getVolumes()), vag.getName());

                lstSolidFireVags.add(sfVag);
            }
        }

        return lstSolidFireVags;
    }

    public static class SolidFireVag {
        private final long _id;
        private final String[] _initiators;
        private final long[] _volumeIds;
        private final String _vagName;

        SolidFireVag(long id, String[] initiators, long[] volumeIds, String name) {
            _id = id;
            _initiators = initiators;
            _volumeIds = volumeIds;
            _vagName = name;
        }

        public long getId() {
            return _id;
        }

        public String[] getInitiators() {
            return _initiators;
        }

        public long[] getVolumeIds() {
            return _volumeIds;
        }

        public String getName() { return _vagName; }

        @Override
        public int hashCode() {
            return String.valueOf(_id).hashCode();
        }

        @Override
        public String toString() {
            return String.valueOf(_id);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }

            if (!obj.getClass().equals(SolidFireVag.class)) {
                return false;
            }

            SolidFireVag sfVag = (SolidFireVag)obj;

            return _id == sfVag._id;
        }
    }

    private static String getVip(String keyToMatch, String url) {
        String delimiter = ":";

        String storageVip = getValue(keyToMatch, url);

        int index = storageVip.indexOf(delimiter);

        if (index != -1) {
            return storageVip.substring(0, index);
        }

        return storageVip;
    }

    private static int getPort(String keyToMatch, String url, int defaultPortNumber) {
        String delimiter = ":";

        String storageVip = getValue(keyToMatch, url);

        int index = storageVip.indexOf(delimiter);

        int portNumber = defaultPortNumber;

        if (index != -1) {
            String port = storageVip.substring(index + delimiter.length());

            try {
                portNumber = Integer.parseInt(port);
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("Invalid URL format (port is not an integer)");
            }
        }

        return portNumber;
    }

    private static List<String> getStringArrayAsLowerCaseStringList(String[] aString) {
        List<String> lstLowerCaseString = new ArrayList<>();

        if (aString != null) {
            for (String str : aString) {
                if (str != null) {
                    lstLowerCaseString.add(str.toLowerCase());
                }
            }
        }

        return lstLowerCaseString;
    }

    private static Map<String, Object> convertMap(Map<String, String> map) {
        if (map == null) {
            return null;
        }

        return new HashMap<>(map);
    }
}
