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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailVO;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailsDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;

import com.cloud.dc.ClusterDetailsDao;
import com.cloud.dc.ClusterDetailsVO;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.user.AccountDetailVO;
import com.cloud.user.AccountDetailsDao;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.exception.CloudRuntimeException;

import com.google.common.primitives.Longs;

import com.solidfire.client.ElementFactory;
import com.solidfire.element.api.Account;
import com.solidfire.element.api.AddAccountRequest;
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
import com.solidfire.element.api.ModifyVolumeAccessGroupRequest;
import com.solidfire.element.api.ModifyVolumeRequest;
import com.solidfire.element.api.QoS;
import com.solidfire.element.api.RollbackToSnapshotRequest;
import com.solidfire.element.api.Snapshot;
import com.solidfire.element.api.SolidFireElement;
import com.solidfire.element.api.Volume;
import com.solidfire.element.api.VolumeAccessGroup;
import com.solidfire.jsvcgen.javautil.Optional;

import static org.apache.commons.lang.ArrayUtils.toPrimitive;

public class SolidFireUtil {
    private static final Logger s_logger = Logger.getLogger(SolidFireUtil.class);

    public static final String PROVIDER_NAME = "SolidFire";
    public static final String SHARED_PROVIDER_NAME = "SolidFireShared";

    public static final int s_lockTimeInSeconds = 300;

    public static final String LOG_PREFIX = "SolidFire: ";

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

    public static final long MIN_VOLUME_SIZE = 1000000000;

    public static final long MIN_IOPS_PER_VOLUME = 100;
    public static final long MAX_MIN_IOPS_PER_VOLUME = 15000;
    public static final long MAX_IOPS_PER_VOLUME = 100000;

    private static final int DEFAULT_MANAGEMENT_PORT = 443;
    private static final int DEFAULT_STORAGE_PORT = 3260;

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
        return "CloudStack_" + csAccountUuid + "_" + csAccountId;
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

    public static void hostAddedToOrRemovedFromCluster(long hostId, long clusterId, boolean added, String storageProvider,
            ClusterDao clusterDao, ClusterDetailsDao clusterDetailsDao, PrimaryDataStoreDao storagePoolDao,
            StoragePoolDetailsDao storagePoolDetailsDao, HostDao hostDao) {
        ClusterVO cluster = clusterDao.findById(clusterId);

        GlobalLock lock = GlobalLock.getInternLock(cluster.getUuid());

        if (!lock.lock(s_lockTimeInSeconds)) {
            String errMsg = "Couldn't lock the DB on the following string: " + cluster.getUuid();

            s_logger.debug(errMsg);

            throw new CloudRuntimeException(errMsg);
        }

        try {
            List<StoragePoolVO> storagePools = storagePoolDao.findPoolsByProvider(storageProvider);

            if (storagePools != null && storagePools.size() > 0) {
                List<SolidFireUtil.SolidFireConnection> sfConnections = new ArrayList<>();

                for (StoragePoolVO storagePool : storagePools) {
                    ClusterDetailsVO clusterDetail = clusterDetailsDao.findDetail(clusterId, SolidFireUtil.getVagKey(storagePool.getId()));

                    String vagId = clusterDetail != null ? clusterDetail.getValue() : null;

                    if (vagId != null) {
                        SolidFireUtil.SolidFireConnection sfConnection = SolidFireUtil.getSolidFireConnection(storagePool.getId(), storagePoolDetailsDao);

                        if (!sfConnections.contains(sfConnection)) {
                            sfConnections.add(sfConnection);

                            SolidFireUtil.SolidFireVag sfVag = SolidFireUtil.getVag(sfConnection, Long.parseLong(vagId));

                            List<HostVO> hostsToAddOrRemove = new ArrayList<>();
                            HostVO hostToAddOrRemove = hostDao.findByIdIncludingRemoved(hostId);

                            hostsToAddOrRemove.add(hostToAddOrRemove);

                            String[] hostIqns = SolidFireUtil.getNewHostIqns(sfVag.getInitiators(), SolidFireUtil.getIqnsFromHosts(hostsToAddOrRemove), added);

                            SolidFireUtil.modifyVag(sfConnection, sfVag.getId(), hostIqns, sfVag.getVolumeIds());
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

    public static long placeVolumeInVolumeAccessGroup(SolidFireConnection sfConnection, long sfVolumeId, long storagePoolId,
                                                      String vagUuid, List<HostVO> hosts, ClusterDetailsDao clusterDetailsDao) {
        if (hosts == null || hosts.isEmpty()) {
            throw new CloudRuntimeException("There must be at least one host in the cluster.");
        }

        long lVagId;

        try {
            lVagId = SolidFireUtil.createVag(sfConnection, "CloudStack-" + vagUuid,
                SolidFireUtil.getIqnsFromHosts(hosts), new long[] { sfVolumeId });
        }
        catch (Exception ex) {
            String iqnInVagAlready1 = "Exceeded maximum number of Volume Access Groups per initiator";
            String iqnInVagAlready2 = "Exceeded maximum number of VolumeAccessGroups per Initiator";

            if (!ex.getMessage().contains(iqnInVagAlready1) && !ex.getMessage().contains(iqnInVagAlready2)) {
                throw new CloudRuntimeException(ex.getMessage());
            }

            // getCompatibleVag throws an exception if an existing VAG can't be located
            SolidFireUtil.SolidFireVag sfVag = getCompatibleVag(sfConnection, hosts);

            lVagId = sfVag.getId();

            long[] volumeIds = getNewVolumeIds(sfVag.getVolumeIds(), sfVolumeId, true);

            SolidFireUtil.modifyVag(sfConnection, lVagId, sfVag.getInitiators(), volumeIds);
        }

        ClusterDetailsVO clusterDetail = new ClusterDetailsVO(hosts.get(0).getClusterId(), getVagKey(storagePoolId), String.valueOf(lVagId));

        clusterDetailsDao.persist(clusterDetail);

        return lVagId;
    }

    public static boolean hostsSupport_iScsi(List<HostVO> hosts) {
        if (hosts == null || hosts.size() == 0) {
            return false;
        }

        for (Host host : hosts) {
            if (host == null || host.getStorageUrl() == null || host.getStorageUrl().trim().length() == 0 || !host.getStorageUrl().startsWith("iqn")) {
                return false;
            }
        }

        return true;
    }

    public static long[] getNewVolumeIds(long[] volumeIds, long volumeIdToAddOrRemove, boolean add) {
        if (add) {
            return getNewVolumeIdsAdd(volumeIds, volumeIdToAddOrRemove);
        }

        return getNewVolumeIdsRemove(volumeIds, volumeIdToAddOrRemove);
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

    public static void modifyVag(SolidFireConnection sfConnection, long vagId, String[] iqns, long[] volumeIds) {
        ModifyVolumeAccessGroupRequest request = ModifyVolumeAccessGroupRequest.builder()
                .volumeAccessGroupID(vagId)
                .optionalInitiators(iqns)
                .optionalVolumes(Longs.asList(volumeIds).toArray(new Long[volumeIds.length]))
                .build();

        getSolidFireElement(sfConnection).modifyVolumeAccessGroup(request);
    }

    public static SolidFireVag getVag(SolidFireConnection sfConnection, long vagId)
    {
        ListVolumeAccessGroupsRequest request = ListVolumeAccessGroupsRequest.builder()
                .optionalStartVolumeAccessGroupID(vagId)
                .optionalLimit(1L)
                .build();

        VolumeAccessGroup vag = getSolidFireElement(sfConnection).listVolumeAccessGroups(request).getVolumeAccessGroups()[0];

        String[] vagIqns = vag.getInitiators();
        long[] vagVolumeIds = toPrimitive(vag.getVolumes());

        return new SolidFireVag(vagId, vagIqns, vagVolumeIds);
    }

    private static List<SolidFireVag> getAllVags(SolidFireConnection sfConnection)
    {
        ListVolumeAccessGroupsRequest request = ListVolumeAccessGroupsRequest.builder().build();

        VolumeAccessGroup[] vags = getSolidFireElement(sfConnection).listVolumeAccessGroups(request).getVolumeAccessGroups();

        List<SolidFireVag> lstSolidFireVags = new ArrayList<>();

        if (vags != null) {
            for (VolumeAccessGroup vag : vags) {
                SolidFireVag sfVag = new SolidFireVag(vag.getVolumeAccessGroupID(), vag.getInitiators(), toPrimitive(vag.getVolumes()));

                lstSolidFireVags.add(sfVag);
            }
        }

        return lstSolidFireVags;
    }

    public static class SolidFireVag {
        private final long _id;
        private final String[] _initiators;
        private final long[] _volumeIds;

        SolidFireVag(long id, String[] initiators, long[] volumeIds) {
            _id = id;
            _initiators = initiators;
            _volumeIds = volumeIds;
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

    private static String[] getNewHostIqns(String[] iqns, String[] iqnsToAddOrRemove, boolean add) {
        if (add) {
            return getNewHostIqnsAdd(iqns, iqnsToAddOrRemove);
        }

        return getNewHostIqnsRemove(iqns, iqnsToAddOrRemove);
    }

    private static String[] getNewHostIqnsAdd(String[] iqns, String[] iqnsToAdd) {
        List<String> lstIqns = iqns != null ? new ArrayList<>(Arrays.asList(iqns)) : new ArrayList<String>();

        if (iqnsToAdd != null) {
            for (String iqnToAdd : iqnsToAdd) {
                if (!lstIqns.contains(iqnToAdd)) {
                    lstIqns.add(iqnToAdd);
                }
            }
        }

        return lstIqns.toArray(new String[0]);
    }

    private static String[] getNewHostIqnsRemove(String[] iqns, String[] iqnsToRemove) {
        List<String> lstIqns = iqns != null ? new ArrayList<>(Arrays.asList(iqns)) : new ArrayList<String>();

        if (iqnsToRemove != null) {
            for (String iqnToRemove : iqnsToRemove) {
                lstIqns.remove(iqnToRemove);
            }
        }

        return lstIqns.toArray(new String[0]);
    }

    private static long[] getNewVolumeIdsAdd(long[] volumeIds, long volumeIdToAdd) {
        List<Long> lstVolumeIds = new ArrayList<>();

        if (volumeIds != null) {
            for (long volumeId : volumeIds) {
                lstVolumeIds.add(volumeId);
            }
        }

        if (lstVolumeIds.contains(volumeIdToAdd)) {
            return volumeIds;
        }

        lstVolumeIds.add(volumeIdToAdd);

        return toPrimitive(lstVolumeIds.toArray(new Long[lstVolumeIds.size()]));
    }

    private static long[] getNewVolumeIdsRemove(long[] volumeIds, long volumeIdToRemove) {
        List<Long> lstVolumeIds = new ArrayList<>();

        if (volumeIds != null) {
            for (long volumeId : volumeIds) {
                lstVolumeIds.add(volumeId);
            }
        }

        lstVolumeIds.remove(volumeIdToRemove);

        return toPrimitive(lstVolumeIds.toArray(new Long[lstVolumeIds.size()]));
    }

    private static String[] getIqnsFromHosts(List<? extends Host> hosts) {
        if (hosts == null || hosts.size() == 0) {
            throw new CloudRuntimeException("There do not appear to be any hosts in this cluster.");
        }

        List<String> lstIqns = new ArrayList<>();

        for (Host host : hosts) {
            lstIqns.add(host.getStorageUrl());
        }

        return lstIqns.toArray(new String[0]);
    }

    // this method takes in a collection of hosts and tries to find an existing VAG that has all of them in it
    // if successful, the VAG is returned; else, a CloudRuntimeException is thrown and this issue should be corrected by an admin
    private static SolidFireUtil.SolidFireVag getCompatibleVag(SolidFireConnection sfConnection, List<HostVO> hosts) {
        List<SolidFireUtil.SolidFireVag> sfVags = SolidFireUtil.getAllVags(sfConnection);

        if (sfVags != null) {
            List<String> hostIqns = new ArrayList<>();

            // where the method we're in is called, hosts should not be null
            for (HostVO host : hosts) {
                // where the method we're in is called, host.getStorageUrl() should not be null (it actually should start with "iqn")
                hostIqns.add(host.getStorageUrl().toLowerCase());
            }

            for (SolidFireUtil.SolidFireVag sfVag : sfVags) {
                List<String> lstInitiators = getStringArrayAsLowerCaseStringList(sfVag.getInitiators());

                // lstInitiators should not be returned from getStringArrayAsLowerCaseStringList as null
                if (lstInitiators.containsAll(hostIqns)) {
                    return sfVag;
                }
            }
        }

        throw new CloudRuntimeException("Unable to locate the appropriate SolidFire Volume Access Group");
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

        Map<String, Object> convertedMap = new HashMap<>();

        convertedMap.putAll(map);

        return convertedMap;
    }
}
