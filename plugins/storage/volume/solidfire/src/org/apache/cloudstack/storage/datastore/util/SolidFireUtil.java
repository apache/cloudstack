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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.BasicClientConnectionManager;
import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailVO;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailsDao;
import org.apache.cloudstack.utils.security.SSLUtils;

import com.cloud.dc.ClusterDetailsDao;
import com.cloud.dc.ClusterDetailsVO;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.user.AccountDetailVO;
import com.cloud.user.AccountDetailsDao;
import com.cloud.utils.exception.CloudRuntimeException;

public class SolidFireUtil {
    private static final Logger s_logger = Logger.getLogger(SolidFireUtil.class);
    public static final String PROVIDER_NAME = "SolidFire";
    public static final String SHARED_PROVIDER_NAME = "SolidFireShared";

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

    public static final String ACCOUNT_ID = "accountId";
    public static final String VOLUME_ID = "volumeId";

    public static final String VOLUME_SIZE = "sfVolumeSize";

    public static final String STORAGE_POOL_ID = "sfStoragePoolId";

    public static final String CHAP_INITIATOR_USERNAME = "chapInitiatorUsername";
    public static final String CHAP_INITIATOR_SECRET = "chapInitiatorSecret";

    public static final String CHAP_TARGET_USERNAME = "chapTargetUsername";
    public static final String CHAP_TARGET_SECRET = "chapTargetSecret";

    public static final String DATACENTER = "datacenter";

    public static final String DATASTORE_NAME = "datastoreName";
    public static final String IQN = "iqn";

    public static final long MAX_IOPS_PER_VOLUME = 100000;

    private static final int DEFAULT_MANAGEMENT_PORT = 443;
    private static final int DEFAULT_STORAGE_PORT = 3260;

    public static class SolidFireConnection {
        private final String _managementVip;
        private final int _managementPort;
        private final String _clusterAdminUsername;
        private final String _clusterAdminPassword;

        public SolidFireConnection(String managementVip, int managementPort, String clusterAdminUsername, String clusterAdminPassword) {
            _managementVip = managementVip;
            _managementPort = managementPort;
            _clusterAdminUsername = clusterAdminUsername;
            _clusterAdminPassword = clusterAdminPassword;
        }

        public String getManagementVip() {
            return _managementVip;
        }

        public int getManagementPort() {
            return _managementPort;
        }

        public String getClusterAdminUsername() {
            return _clusterAdminUsername;
        }

        public String getClusterAdminPassword() {
            return _clusterAdminPassword;
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

    public static String getSolidFireAccountName(String csAccountUuid, long csAccountId) {
        return "CloudStack_" + csAccountUuid + "_" + csAccountId;
    }

    public static void updateCsDbWithSolidFireIopsInfo(long storagePoolId, PrimaryDataStoreDao primaryDataStoreDao, StoragePoolDetailsDao storagePoolDetailsDao,
            long minIops, long maxIops, long burstIops) {
        Map<String, String> existingDetails = storagePoolDetailsDao.listDetailsKeyPairs(storagePoolId);
        Set<String> existingKeys = existingDetails.keySet();

        Map<String, String> existingDetailsToKeep = new HashMap<String, String>();

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

        /*
        accountDetail = new AccountDetailVO(csAccountId,
                SolidFireUtil.CHAP_INITIATOR_USERNAME,
                String.valueOf(sfAccount.getName()));

        accountDetailsDao.persist(accountDetail);

        accountDetail = new AccountDetailVO(csAccountId,
                SolidFireUtil.CHAP_INITIATOR_SECRET,
                String.valueOf(sfAccount.getInitiatorSecret()));

        accountDetailsDao.persist(accountDetail);

        accountDetail = new AccountDetailVO(csAccountId,
                SolidFireUtil.CHAP_TARGET_USERNAME,
                sfAccount.getName());

        accountDetailsDao.persist(accountDetail);

        accountDetail = new AccountDetailVO(csAccountId,
                SolidFireUtil.CHAP_TARGET_SECRET,
                sfAccount.getTargetSecret());

        accountDetailsDao.persist(accountDetail);
        */
    }

    public static SolidFireAccount getSolidFireAccount(SolidFireConnection sfConnection, String sfAccountName) {
        try {
            return getSolidFireAccountByName(sfConnection, sfAccountName);
        } catch (Exception ex) {
            return null;
        }
    }

    public static long placeVolumeInVolumeAccessGroup(SolidFireConnection sfConnection, long sfVolumeId, long storagePoolId,
            String vagUuid, List<HostVO> hosts, ClusterDetailsDao clusterDetailsDao) {
        if (hosts == null || hosts.isEmpty()) {
            throw new CloudRuntimeException("There must be at least one host in the cluster.");
        }

        long lVagId;

        try {
            lVagId = SolidFireUtil.createSolidFireVag(sfConnection, "CloudStack-" + vagUuid,
                SolidFireUtil.getIqnsFromHosts(hosts), new long[] { sfVolumeId });
        }
        catch (Exception ex) {
            String iqnInVagAlready = "Exceeded maximum number of Volume Access Groups per initiator";

            if (!ex.getMessage().contains(iqnInVagAlready)) {
                throw new CloudRuntimeException(ex.getMessage());
            }

            // getCompatibleVag throws an exception if an existing VAG can't be located
            SolidFireUtil.SolidFireVag sfVag = getCompatibleVag(sfConnection, hosts);

            lVagId = sfVag.getId();

            long[] volumeIds = getNewVolumeIds(sfVag.getVolumeIds(), sfVolumeId, true);

            SolidFireUtil.modifySolidFireVag(sfConnection, lVagId,
                sfVag.getInitiators(), volumeIds);
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

    public static String[] getNewHostIqns(String[] currentIqns, String[] newIqns) {
        List<String> lstIqns = new ArrayList<String>();

        if (currentIqns != null) {
            for (String currentIqn : currentIqns) {
                lstIqns.add(currentIqn);
            }
        }

        if (newIqns != null) {
            for (String newIqn : newIqns) {
                if (!lstIqns.contains(newIqn)) {
                    lstIqns.add(newIqn);
                }
            }
        }

        return lstIqns.toArray(new String[0]);
    }

    public static long[] getNewVolumeIds(long[] volumeIds, long volumeIdToAddOrRemove, boolean add) {
        if (add) {
            return getNewVolumeIdsAdd(volumeIds, volumeIdToAddOrRemove);
        }

        return getNewVolumeIdsRemove(volumeIds, volumeIdToAddOrRemove);
    }

    private static long[] getNewVolumeIdsAdd(long[] volumeIds, long volumeIdToAdd) {
        List<Long> lstVolumeIds = new ArrayList<Long>();

        if (volumeIds != null) {
            for (long volumeId : volumeIds) {
                lstVolumeIds.add(volumeId);
            }
        }

        if (lstVolumeIds.contains(volumeIdToAdd)) {
            return volumeIds;
        }

        lstVolumeIds.add(volumeIdToAdd);

        return convertArray(lstVolumeIds);
    }

    private static long[] getNewVolumeIdsRemove(long[] volumeIds, long volumeIdToRemove) {
        List<Long> lstVolumeIds = new ArrayList<Long>();

        if (volumeIds != null) {
            for (long volumeId : volumeIds) {
                lstVolumeIds.add(volumeId);
            }
        }

        lstVolumeIds.remove(volumeIdToRemove);

        return convertArray(lstVolumeIds);
    }

    private static long[] convertArray(List<Long> items) {
        if (items == null) {
            return new long[0];
        }

        long[] outArray = new long[items.size()];

        for (int i = 0; i < items.size(); i++) {
            Long value = items.get(i);

            outArray[i] = value;
        }

        return outArray;
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

    public static String[] getIqnsFromHosts(List<? extends Host> hosts) {
        if (hosts == null || hosts.size() == 0) {
            throw new CloudRuntimeException("There do not appear to be any hosts in this cluster.");
        }

        List<String> lstIqns = new ArrayList<String>();

        for (Host host : hosts) {
            lstIqns.add(host.getStorageUrl());
        }

        return lstIqns.toArray(new String[0]);
    }

    // this method takes in a collection of hosts and tries to find an existing VAG that has all of them in it
    // if successful, the VAG is returned; else, a CloudRuntimeException is thrown and this issue should be corrected by an admin
    private static SolidFireUtil.SolidFireVag getCompatibleVag(SolidFireConnection sfConnection, List<HostVO> hosts) {
        List<SolidFireUtil.SolidFireVag> sfVags = SolidFireUtil.getAllSolidFireVags(sfConnection);

        if (sfVags != null) {
            List<String> hostIqns = new ArrayList<String>();

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
        List<String> lstLowerCaseString = new ArrayList<String>();

        if (aString != null) {
            for (String str : aString) {
                if (str != null) {
                    lstLowerCaseString.add(str.toLowerCase());
                }
            }
        }

        return lstLowerCaseString;
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

    public static long createSolidFireVolume(SolidFireConnection sfConnection, String strSfVolumeName, long lSfAccountId, long lTotalSize,
            boolean bEnable512e, String strCloudStackVolumeSize, long minIops, long maxIops, long burstIops)
    {
        final Gson gson = new GsonBuilder().create();

        Object volumeToCreate = strCloudStackVolumeSize != null && strCloudStackVolumeSize.trim().length() > 0 ?
                new VolumeToCreateWithCloudStackVolumeSize(strSfVolumeName, lSfAccountId, lTotalSize, bEnable512e, strCloudStackVolumeSize, minIops, maxIops, burstIops) :
                new VolumeToCreate(strSfVolumeName, lSfAccountId, lTotalSize, bEnable512e, minIops, maxIops, burstIops);

        String strVolumeToCreateJson = gson.toJson(volumeToCreate);

        String strVolumeCreateResultJson = executeJsonRpc(sfConnection, strVolumeToCreateJson);

        VolumeCreateResult volumeCreateResult = gson.fromJson(strVolumeCreateResultJson, VolumeCreateResult.class);

        verifyResult(volumeCreateResult.result, strVolumeCreateResultJson, gson);

        return volumeCreateResult.result.volumeID;
    }

    public static void modifySolidFireVolume(SolidFireConnection sfConnection, long volumeId, long totalSize, String strCloudStackVolumeSize,
            long minIops, long maxIops, long burstIops)
    {
        final Gson gson = new GsonBuilder().create();

        Object volumeToModify = strCloudStackVolumeSize != null && strCloudStackVolumeSize.trim().length() > 0 ?
                new VolumeToModifyWithCloudStackVolumeSize(volumeId, totalSize, strCloudStackVolumeSize, minIops, maxIops, burstIops) :
                new VolumeToModify(volumeId, totalSize, minIops, maxIops, burstIops);

        String strVolumeToModifyJson = gson.toJson(volumeToModify);

        String strVolumeModifyResultJson = executeJsonRpc(sfConnection, strVolumeToModifyJson);

        JsonError jsonError = gson.fromJson(strVolumeModifyResultJson, JsonError.class);

        if (jsonError.error != null) {
            throw new IllegalStateException(jsonError.error.message);
        }
    }

    public static SolidFireVolume getSolidFireVolume(SolidFireConnection sfConnection, long lVolumeId)
    {
        final Gson gson = new GsonBuilder().create();

        VolumeToGet volumeToGet = new VolumeToGet(lVolumeId);

        String strVolumeToGetJson = gson.toJson(volumeToGet);

        String strVolumeGetResultJson = executeJsonRpc(sfConnection, strVolumeToGetJson);

        VolumeGetResult volumeGetResult = gson.fromJson(strVolumeGetResultJson, VolumeGetResult.class);

        verifyResult(volumeGetResult.result, strVolumeGetResultJson, gson);

        String strVolumeName = getVolumeName(volumeGetResult, lVolumeId);
        String strVolumeIqn = getVolumeIqn(volumeGetResult, lVolumeId);
        long lAccountId = getVolumeAccountId(volumeGetResult, lVolumeId);
        String strVolumeStatus = getVolumeStatus(volumeGetResult, lVolumeId);
        boolean enable512e = getVolumeEnable512e(volumeGetResult, lVolumeId);
        long lMinIops = getVolumeMinIops(volumeGetResult, lVolumeId);
        long lMaxIops = getVolumeMaxIops(volumeGetResult, lVolumeId);
        long lBurstIops = getVolumeBurstIops(volumeGetResult, lVolumeId);
        long lTotalSize = getVolumeTotalSize(volumeGetResult, lVolumeId);

        return new SolidFireVolume(lVolumeId, strVolumeName, strVolumeIqn, lAccountId, strVolumeStatus, enable512e,
                lMinIops, lMaxIops, lBurstIops, lTotalSize);
    }

    public static List<SolidFireVolume> getSolidFireVolumesForAccountId(SolidFireConnection sfConnection, long lAccountId) {
        final Gson gson = new GsonBuilder().create();

        VolumesToGetForAccount volumesToGetForAccount = new VolumesToGetForAccount(lAccountId);

        String strVolumesToGetForAccountJson = gson.toJson(volumesToGetForAccount);

        String strVolumesGetForAccountResultJson = executeJsonRpc(sfConnection, strVolumesToGetForAccountJson);

        VolumeGetResult volumeGetResult = gson.fromJson(strVolumesGetForAccountResultJson, VolumeGetResult.class);

        verifyResult(volumeGetResult.result, strVolumesGetForAccountResultJson, gson);

        List<SolidFireVolume> sfVolumes = new ArrayList<SolidFireVolume>();

        for (VolumeGetResult.Result.Volume volume : volumeGetResult.result.volumes) {
            sfVolumes.add(new SolidFireVolume(volume.volumeID, volume.name, volume.iqn, volume.accountID, volume.status, volume.enable512e,
                    volume.qos.minIOPS, volume.qos.maxIOPS, volume.qos.burstIOPS, volume.totalSize));
        }

        return sfVolumes;
    }

    public static List<SolidFireVolume> getDeletedVolumes(SolidFireConnection sfConnection)
    {
        final Gson gson = new GsonBuilder().create();

        ListDeletedVolumes listDeletedVolumes = new ListDeletedVolumes();

        String strListDeletedVolumesJson = gson.toJson(listDeletedVolumes);

        String strListDeletedVolumesResultJson = executeJsonRpc(sfConnection, strListDeletedVolumesJson);

        VolumeGetResult volumeGetResult = gson.fromJson(strListDeletedVolumesResultJson, VolumeGetResult.class);

        verifyResult(volumeGetResult.result, strListDeletedVolumesResultJson, gson);

        List<SolidFireVolume> deletedVolumes = new ArrayList<SolidFireVolume> ();

        for (VolumeGetResult.Result.Volume volume : volumeGetResult.result.volumes) {
            deletedVolumes.add(new SolidFireVolume(volume.volumeID, volume.name, volume.iqn, volume.accountID, volume.status, volume.enable512e,
                    volume.qos.minIOPS, volume.qos.maxIOPS, volume.qos.burstIOPS, volume.totalSize));
        }

        return deletedVolumes;
    }

    public static void deleteSolidFireVolume(SolidFireConnection sfConnection, long lVolumeId)
    {
        final Gson gson = new GsonBuilder().create();

        VolumeToDelete volumeToDelete = new VolumeToDelete(lVolumeId);

        String strVolumeToDeleteJson = gson.toJson(volumeToDelete);

        executeJsonRpc(sfConnection, strVolumeToDeleteJson);
    }

   public static void purgeSolidFireVolume(SolidFireConnection sfConnection, long lVolumeId)
    {
        final Gson gson = new GsonBuilder().create();

        VolumeToPurge volumeToPurge = new VolumeToPurge(lVolumeId);

        String strVolumeToPurgeJson = gson.toJson(volumeToPurge);

        executeJsonRpc(sfConnection, strVolumeToPurgeJson);
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

        public SolidFireVolume(long id, String name, String iqn,
                long accountId, String status, boolean enable512e,
                long minIops, long maxIops, long burstIops, long totalSize)
        {
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

            if (_id == sfv._id && _name.equals(sfv._name) &&
                _iqn.equals(sfv._iqn) && _accountId == sfv._accountId &&
                isActive() == sfv.isActive() && getTotalSize() == sfv.getTotalSize()) {
                return true;
            }

            return false;
        }
    }

    public static long createSolidFireSnapshot(SolidFireConnection sfConnection, long lVolumeId, String snapshotName) {
        final Gson gson = new GsonBuilder().create();

        SnapshotToCreate snapshotToCreate = new SnapshotToCreate(lVolumeId, snapshotName);

        String strSnapshotToCreateJson = gson.toJson(snapshotToCreate);

        String strSnapshotCreateResultJson = executeJsonRpc(sfConnection, strSnapshotToCreateJson);

        SnapshotCreateResult snapshotCreateResult = gson.fromJson(strSnapshotCreateResultJson, SnapshotCreateResult.class);

        verifyResult(snapshotCreateResult.result, strSnapshotCreateResultJson, gson);

        return snapshotCreateResult.result.snapshotID;
    }

    public static void deleteSolidFireSnapshot(SolidFireConnection sfConnection, long lSnapshotId)
    {
        final Gson gson = new GsonBuilder().create();

        SnapshotToDelete snapshotToDelete = new SnapshotToDelete(lSnapshotId);

        String strSnapshotToDeleteJson = gson.toJson(snapshotToDelete);

        executeJsonRpc(sfConnection, strSnapshotToDeleteJson);
    }

    public static void rollBackVolumeToSnapshot(SolidFireConnection sfConnection, long volumeId, long snapshotId) {
        final Gson gson = new GsonBuilder().create();

        RollbackToInitiate rollbackToInitiate = new RollbackToInitiate(volumeId, snapshotId);

        String strRollbackToInitiateJson = gson.toJson(rollbackToInitiate);

        String strRollbackInitiatedResultJson = executeJsonRpc(sfConnection, strRollbackToInitiateJson);

        RollbackInitiatedResult rollbackInitiatedResult = gson.fromJson(strRollbackInitiatedResultJson, RollbackInitiatedResult.class);

        verifyResult(rollbackInitiatedResult.result, strRollbackInitiatedResultJson, gson);
    }

    public static long createSolidFireClone(SolidFireConnection sfConnection, long lVolumeId, long lSnapshotId, String cloneName) {
        final Gson gson = new GsonBuilder().create();

        CloneToCreate cloneToCreate = new CloneToCreate(lVolumeId, lSnapshotId, cloneName);

        String strCloneToCreateJson = gson.toJson(cloneToCreate);

        String strCloneCreateResultJson = executeJsonRpc(sfConnection, strCloneToCreateJson);

        CloneCreateResult cloneCreateResult = gson.fromJson(strCloneCreateResultJson, CloneCreateResult.class);

        verifyResult(cloneCreateResult.result, strCloneCreateResultJson, gson);

        return cloneCreateResult.result.cloneID;
    }

    public static long createSolidFireAccount(SolidFireConnection sfConnection, String strAccountName)
    {
        final Gson gson = new GsonBuilder().create();

        AccountToAdd accountToAdd = new AccountToAdd(strAccountName);

        String strAccountAddJson = gson.toJson(accountToAdd);

        String strAccountAddResultJson = executeJsonRpc(sfConnection, strAccountAddJson);

        AccountAddResult accountAddResult = gson.fromJson(strAccountAddResultJson, AccountAddResult.class);

        verifyResult(accountAddResult.result, strAccountAddResultJson, gson);

        return accountAddResult.result.accountID;
    }

    public static SolidFireAccount getSolidFireAccountById(SolidFireConnection sfConnection, long lSfAccountId)
    {
        final Gson gson = new GsonBuilder().create();

        AccountToGetById accountToGetById = new AccountToGetById(lSfAccountId);

        String strAccountToGetByIdJson = gson.toJson(accountToGetById);

        String strAccountGetByIdResultJson = executeJsonRpc(sfConnection, strAccountToGetByIdJson);

        AccountGetResult accountGetByIdResult = gson.fromJson(strAccountGetByIdResultJson, AccountGetResult.class);

        verifyResult(accountGetByIdResult.result, strAccountGetByIdResultJson, gson);

        String strSfAccountName = accountGetByIdResult.result.account.username;
        String strSfAccountInitiatorSecret = accountGetByIdResult.result.account.initiatorSecret;
        String strSfAccountTargetSecret = accountGetByIdResult.result.account.targetSecret;

        return new SolidFireAccount(lSfAccountId, strSfAccountName, strSfAccountInitiatorSecret, strSfAccountTargetSecret);
    }

    public static SolidFireAccount getSolidFireAccountByName(SolidFireConnection sfConnection, String strSfAccountName)
    {
        final Gson gson = new GsonBuilder().create();

        AccountToGetByName accountToGetByName = new AccountToGetByName(strSfAccountName);

        String strAccountToGetByNameJson = gson.toJson(accountToGetByName);

        String strAccountGetByNameResultJson = executeJsonRpc(sfConnection, strAccountToGetByNameJson);

        AccountGetResult accountGetByNameResult = gson.fromJson(strAccountGetByNameResultJson, AccountGetResult.class);

        verifyResult(accountGetByNameResult.result, strAccountGetByNameResultJson, gson);

        long lSfAccountId = accountGetByNameResult.result.account.accountID;
        String strSfAccountInitiatorSecret = accountGetByNameResult.result.account.initiatorSecret;
        String strSfAccountTargetSecret = accountGetByNameResult.result.account.targetSecret;

        return new SolidFireAccount(lSfAccountId, strSfAccountName, strSfAccountInitiatorSecret, strSfAccountTargetSecret);
    }

    public static void deleteSolidFireAccount(SolidFireConnection sfConnection, long lAccountId)
    {
        final Gson gson = new GsonBuilder().create();

        AccountToRemove accountToRemove = new AccountToRemove(lAccountId);

        String strAccountToRemoveJson = gson.toJson(accountToRemove);

        executeJsonRpc(sfConnection, strAccountToRemoveJson);
    }

    public static class SolidFireAccount
    {
        private final long _id;
        private final String _name;
        private final String _initiatorSecret;
        private final String _targetSecret;

        public SolidFireAccount(long id, String name, String initiatorSecret, String targetSecret) {
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

        public String getInitiatorSecret() {
            return _initiatorSecret;
        }

        public String getTargetSecret() {
            return _targetSecret;
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

            if (_id == sfa._id && _name.equals(sfa._name) &&
                _initiatorSecret.equals(sfa._initiatorSecret) &&
                _targetSecret.equals(sfa._targetSecret)) {
                return true;
            }

            return false;
        }
    }

    public static long createSolidFireVag(SolidFireConnection sfConnection, String strVagName,
            String[] iqns, long[] volumeIds)
    {
        final Gson gson = new GsonBuilder().create();

        VagToCreate vagToCreate = new VagToCreate(strVagName, iqns, volumeIds);

        String strVagCreateJson = gson.toJson(vagToCreate);

        String strVagCreateResultJson = executeJsonRpc(sfConnection, strVagCreateJson);

        VagCreateResult vagCreateResult = gson.fromJson(strVagCreateResultJson, VagCreateResult.class);

        verifyResult(vagCreateResult.result, strVagCreateResultJson, gson);

        return vagCreateResult.result.volumeAccessGroupID;
    }

    public static void modifySolidFireVag(SolidFireConnection sfConnection, long lVagId, String[] iqns, long[] volumeIds)
    {
        final Gson gson = new GsonBuilder().create();

        VagToModify vagToModify = new VagToModify(lVagId, iqns, volumeIds);

        String strVagModifyJson = gson.toJson(vagToModify);

        executeJsonRpc(sfConnection, strVagModifyJson);
    }

    public static SolidFireVag getSolidFireVag(SolidFireConnection sfConnection, long lVagId)
    {
        final Gson gson = new GsonBuilder().create();

        VagToGet vagToGet = new VagToGet(lVagId);

        String strVagToGetJson = gson.toJson(vagToGet);

        String strVagGetResultJson = executeJsonRpc(sfConnection, strVagToGetJson);

        VagGetResult vagGetResult = gson.fromJson(strVagGetResultJson, VagGetResult.class);

        verifyResult(vagGetResult.result, strVagGetResultJson, gson);

        String[] vagIqns = getVagIqns(vagGetResult, lVagId);
        long[] vagVolumeIds = getVagVolumeIds(vagGetResult, lVagId);

        return new SolidFireVag(lVagId, vagIqns, vagVolumeIds);
    }

    public static List<SolidFireVag> getAllSolidFireVags(SolidFireConnection sfConnection)
    {
        final Gson gson = new GsonBuilder().create();

        AllVags allVags = new AllVags();

        String strAllVagsJson = gson.toJson(allVags);

        String strAllVagsGetResultJson = executeJsonRpc(sfConnection, strAllVagsJson);

        VagGetResult allVagsGetResult = gson.fromJson(strAllVagsGetResultJson, VagGetResult.class);

        verifyResult(allVagsGetResult.result, strAllVagsGetResultJson, gson);

        List<SolidFireVag> lstSolidFireVags = new ArrayList<SolidFireVag>();

        if (allVagsGetResult.result.volumeAccessGroups != null ) {
            for (VagGetResult.Result.Vag vag : allVagsGetResult.result.volumeAccessGroups) {
                SolidFireVag sfVag = new SolidFireVag(vag.volumeAccessGroupID, vag.initiators, vag.volumes);

                lstSolidFireVags.add(sfVag);
            }
        }

        return lstSolidFireVags;
    }

    public static void deleteSolidFireVag(SolidFireConnection sfConnection, long lVagId)
    {
        final Gson gson = new GsonBuilder().create();

        VagToDelete vagToDelete = new VagToDelete(lVagId);

        String strVagToDeleteJson = gson.toJson(vagToDelete);

        executeJsonRpc(sfConnection, strVagToDeleteJson);
    }

    public static class SolidFireVag
    {
        private final long _id;
        private final String[] _initiators;
        private final long[] _volumeIds;

        public SolidFireVag(long id, String[] initiators, long[] volumeIds)
        {
            _id = id;
            _initiators = initiators;
            _volumeIds = volumeIds;
        }

        public long getId()
        {
            return _id;
        }

        public String[] getInitiators()
        {
            return _initiators;
        }

        public long[] getVolumeIds()
        {
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

            SolidFireVag sfvag = (SolidFireVag)obj;

            if (_id == sfvag._id) {
                return true;
            }

            return false;
        }
    }

    @SuppressWarnings("unused")
    private static final class VolumeToCreateWithCloudStackVolumeSize {
        private final String method = "CreateVolume";
        private final VolumeToCreateParams params;

        private VolumeToCreateWithCloudStackVolumeSize(final String strVolumeName, final long lAccountId, final long lTotalSize,
                final boolean bEnable512e, final String strCloudStackVolumeSize, final long lMinIOPS, final long lMaxIOPS, final long lBurstIOPS) {
            params = new VolumeToCreateParams(strVolumeName, lAccountId, lTotalSize, bEnable512e, strCloudStackVolumeSize, lMinIOPS, lMaxIOPS, lBurstIOPS);
        }

        private static final class VolumeToCreateParams {
            private final String name;
            private final long accountID;
            private final long totalSize;
            private final boolean enable512e;
            private final VolumeToCreateParamsAttributes attributes;
            private final VolumeToCreateParamsQoS qos;

            private VolumeToCreateParams(final String strVolumeName, final long lAccountId, final long lTotalSize, final boolean bEnable512e,
                    final String strCloudStackVolumeSize, final long lMinIOPS, final long lMaxIOPS, final long lBurstIOPS) {
                name = strVolumeName;
                accountID = lAccountId;
                totalSize = lTotalSize;
                enable512e = bEnable512e;

                attributes = new VolumeToCreateParamsAttributes(strCloudStackVolumeSize);
                qos = new VolumeToCreateParamsQoS(lMinIOPS, lMaxIOPS, lBurstIOPS);
            }

            private static final class VolumeToCreateParamsAttributes {
                private final String CloudStackVolumeSize;

                private VolumeToCreateParamsAttributes(final String strCloudStackVolumeSize) {
                    CloudStackVolumeSize = strCloudStackVolumeSize;
                }
            }

            private static final class VolumeToCreateParamsQoS {
                private final long minIOPS;
                private final long maxIOPS;
                private final long burstIOPS;

                private VolumeToCreateParamsQoS(final long lMinIOPS, final long lMaxIOPS, final long lBurstIOPS) {
                    minIOPS = lMinIOPS;
                    maxIOPS = lMaxIOPS;
                    burstIOPS = lBurstIOPS;
                }
            }
        }
    }

    @SuppressWarnings("unused")
    private static final class VolumeToCreate {
        private final String method = "CreateVolume";
        private final VolumeToCreateParams params;

        private VolumeToCreate(final String strVolumeName, final long lAccountId, final long lTotalSize, final boolean bEnable512e,
                final long lMinIOPS, final long lMaxIOPS, final long lBurstIOPS) {
            params = new VolumeToCreateParams(strVolumeName, lAccountId, lTotalSize, bEnable512e, lMinIOPS, lMaxIOPS, lBurstIOPS);
        }

        private static final class VolumeToCreateParams {
            private final String name;
            private final long accountID;
            private final long totalSize;
            private final boolean enable512e;
            private final VolumeToCreateParamsQoS qos;

            private VolumeToCreateParams(final String strVolumeName, final long lAccountId, final long lTotalSize, final boolean bEnable512e,
                    final long lMinIOPS, final long lMaxIOPS, final long lBurstIOPS) {
                name = strVolumeName;
                accountID = lAccountId;
                totalSize = lTotalSize;
                enable512e = bEnable512e;

                qos = new VolumeToCreateParamsQoS(lMinIOPS, lMaxIOPS, lBurstIOPS);
            }

            private static final class VolumeToCreateParamsQoS {
                private final long minIOPS;
                private final long maxIOPS;
                private final long burstIOPS;

                private VolumeToCreateParamsQoS(final long lMinIOPS, final long lMaxIOPS, final long lBurstIOPS) {
                    minIOPS = lMinIOPS;
                    maxIOPS = lMaxIOPS;
                    burstIOPS = lBurstIOPS;
                }
            }
        }
    }

    @SuppressWarnings("unused")
    private static final class VolumeToModifyWithCloudStackVolumeSize
    {
        private final String method = "ModifyVolume";
        private final VolumeToModifyParams params;

        private VolumeToModifyWithCloudStackVolumeSize(final long lVolumeId, final long lTotalSize, final String strCloudStackVolumeSize,
                final long lMinIOPS, final long lMaxIOPS, final long lBurstIOPS)
        {
            params = new VolumeToModifyParams(lVolumeId, lTotalSize, strCloudStackVolumeSize, lMinIOPS, lMaxIOPS, lBurstIOPS);
        }

        private static final class VolumeToModifyParams
        {
            private final long volumeID;
            private final long totalSize;
            private final VolumeToModifyParamsAttributes attributes;
            private final VolumeToModifyParamsQoS qos;

            private VolumeToModifyParams(final long lVolumeId, final long lTotalSize, String strCloudStackVolumeSize, final long lMinIOPS, final long lMaxIOPS, final long lBurstIOPS)
            {
                volumeID = lVolumeId;

                totalSize = lTotalSize;

                attributes = new VolumeToModifyParamsAttributes(strCloudStackVolumeSize);
                qos = new VolumeToModifyParamsQoS(lMinIOPS, lMaxIOPS, lBurstIOPS);
            }
        }

        private static final class VolumeToModifyParamsAttributes {
            private final String CloudStackVolumeSize;

            private VolumeToModifyParamsAttributes(final String strCloudStackVolumeSize) {
                CloudStackVolumeSize = strCloudStackVolumeSize;
            }
        }

        private static final class VolumeToModifyParamsQoS {
            private final long minIOPS;
            private final long maxIOPS;
            private final long burstIOPS;

            private VolumeToModifyParamsQoS(final long lMinIOPS, final long lMaxIOPS, final long lBurstIOPS) {
                minIOPS = lMinIOPS;
                maxIOPS = lMaxIOPS;
                burstIOPS = lBurstIOPS;
            }
        }
    }

    @SuppressWarnings("unused")
    private static final class VolumeToModify
    {
        private final String method = "ModifyVolume";
        private final VolumeToModifyParams params;

        private VolumeToModify(final long lVolumeId, final long lTotalSize, final long lMinIOPS, final long lMaxIOPS, final long lBurstIOPS)
        {
            params = new VolumeToModifyParams(lVolumeId, lTotalSize, lMinIOPS, lMaxIOPS, lBurstIOPS);
        }

        private static final class VolumeToModifyParams
        {
            private final long volumeID;
            private final long totalSize;
            private final VolumeToModifyParamsQoS qos;

            private VolumeToModifyParams(final long lVolumeId, final long lTotalSize, final long lMinIOPS, final long lMaxIOPS, final long lBurstIOPS)
            {
                volumeID = lVolumeId;

                totalSize = lTotalSize;

                qos = new VolumeToModifyParamsQoS(lMinIOPS, lMaxIOPS, lBurstIOPS);
            }
        }

        private static final class VolumeToModifyParamsQoS {
            private final long minIOPS;
            private final long maxIOPS;
            private final long burstIOPS;

            private VolumeToModifyParamsQoS(final long lMinIOPS, final long lMaxIOPS, final long lBurstIOPS) {
                minIOPS = lMinIOPS;
                maxIOPS = lMaxIOPS;
                burstIOPS = lBurstIOPS;
            }
        }
    }

    @SuppressWarnings("unused")
    private static final class VolumeToGet
    {
        private final String method = "ListActiveVolumes";
        private final VolumeToGetParams params;

        private VolumeToGet(final long lVolumeId)
        {
            params = new VolumeToGetParams(lVolumeId);
        }

        private static final class VolumeToGetParams
        {
            private final long startVolumeID;
            private final long limit = 1;

            private VolumeToGetParams(final long lVolumeId)
            {
                startVolumeID = lVolumeId;
            }
        }
    }

    @SuppressWarnings("unused")
    private static final class VolumesToGetForAccount
    {
        private final String method = "ListVolumesForAccount";
        private final VolumesToGetForAccountParams params;

        private VolumesToGetForAccount(final long lAccountId)
        {
            params = new VolumesToGetForAccountParams(lAccountId);
        }

        private static final class VolumesToGetForAccountParams
        {
            private final long accountID;

            private VolumesToGetForAccountParams(final long lAccountId)
            {
                accountID = lAccountId;
            }
        }
    }

    @SuppressWarnings("unused")
    private static final class ListDeletedVolumes
    {
        private final String method = "ListDeletedVolumes";
    }

    @SuppressWarnings("unused")
    private static final class VolumeToDelete
    {
        private final String method = "DeleteVolume";
        private final VolumeToDeleteParams params;

        private VolumeToDelete(final long lVolumeId) {
            params = new VolumeToDeleteParams(lVolumeId);
        }

        private static final class VolumeToDeleteParams {
            private final long volumeID;

            private VolumeToDeleteParams(final long lVolumeId) {
                volumeID = lVolumeId;
            }
        }
    }

    @SuppressWarnings("unused")
    private static final class VolumeToPurge
    {
        private final String method = "PurgeDeletedVolume";
        private final VolumeToPurgeParams params;

        private VolumeToPurge(final long lVolumeId) {
            params = new VolumeToPurgeParams(lVolumeId);
        }

        private static final class VolumeToPurgeParams {
            private final long volumeID;

            private VolumeToPurgeParams(final long lVolumeId) {
                volumeID = lVolumeId;
            }
        }
    }

    @SuppressWarnings("unused")
    private static final class SnapshotToCreate {
        private final String method = "CreateSnapshot";
        private final SnapshotToCreateParams params;

        private SnapshotToCreate(final long lVolumeId, final String snapshotName) {
            params = new SnapshotToCreateParams(lVolumeId, snapshotName);
        }

        private static final class SnapshotToCreateParams {
            private final long volumeID;
            private final String name;

            private SnapshotToCreateParams(final long lVolumeId, final String snapshotName) {
                volumeID = lVolumeId;
                name = snapshotName;
            }
        }
    }

    @SuppressWarnings("unused")
    private static final class SnapshotToDelete
    {
        private final String method = "DeleteSnapshot";
        private final SnapshotToDeleteParams params;

        private SnapshotToDelete(final long lSnapshotId) {
            params = new SnapshotToDeleteParams(lSnapshotId);
        }

        private static final class SnapshotToDeleteParams {
            private final long snapshotID;

            private SnapshotToDeleteParams(final long lSnapshotId) {
                snapshotID = lSnapshotId;
            }
        }
    }

    @SuppressWarnings("unused")
    private static final class RollbackToInitiate {
        private final String method = "RollbackToSnapshot";
        private final RollbackToInitiateParams params;

        private RollbackToInitiate(final long lVolumeId, final long lSnapshotId) {
            params = new RollbackToInitiateParams(lVolumeId, lSnapshotId);
        }

        private static final class RollbackToInitiateParams {
            private final long volumeID;
            private final long snapshotID;

            private RollbackToInitiateParams(final long lVolumeId, final long lSnapshotId) {
                volumeID = lVolumeId;
                snapshotID = lSnapshotId;
            }
        }
    }

    @SuppressWarnings("unused")
    private static final class CloneToCreate {
        private final String method = "CloneVolume";
        private final CloneToCreateParams params;

        private CloneToCreate(final long lVolumeId, final long lSnapshotId, final String cloneName) {
            params = new CloneToCreateParams(lVolumeId, lSnapshotId, cloneName);
        }

        private static final class CloneToCreateParams {
            private final long volumeID;
            private final long snapshotID;
            private final String name;

            private CloneToCreateParams(final long lVolumeId, final long lSnapshotId, final String cloneName) {
                volumeID = lVolumeId;
                snapshotID = lSnapshotId;
                name = cloneName;
            }
        }
    }

    @SuppressWarnings("unused")
    private static final class AccountToAdd
    {
        private final String method = "AddAccount";
        private final AccountToAddParams params;

        private AccountToAdd(final String strAccountName)
        {
            params = new AccountToAddParams(strAccountName);
        }

        private static final class AccountToAddParams
        {
            private final String username;

            private AccountToAddParams(final String strAccountName)
            {
                username = strAccountName;
            }
        }
    }

    @SuppressWarnings("unused")
    private static final class AccountToGetById
    {
        private final String method = "GetAccountByID";
        private final AccountToGetByIdParams params;

        private AccountToGetById(final long lAccountId)
        {
            params = new AccountToGetByIdParams(lAccountId);
        }

        private static final class AccountToGetByIdParams
        {
            private final long accountID;

            private AccountToGetByIdParams(final long lAccountId)
            {
                accountID = lAccountId;
            }
        }
    }

    @SuppressWarnings("unused")
    private static final class AccountToGetByName
    {
        private final String method = "GetAccountByName";
        private final AccountToGetByNameParams params;

        private AccountToGetByName(final String strUsername)
        {
            params = new AccountToGetByNameParams(strUsername);
        }

        private static final class AccountToGetByNameParams
        {
            private final String username;

            private AccountToGetByNameParams(final String strUsername)
            {
                username = strUsername;
            }
        }
    }

    @SuppressWarnings("unused")
    private static final class AccountToRemove {
        private final String method = "RemoveAccount";
        private final AccountToRemoveParams params;

        private AccountToRemove(final long lAccountId) {
            params = new AccountToRemoveParams(lAccountId);
        }

        private static final class AccountToRemoveParams {
            private final long accountID;

            private AccountToRemoveParams(final long lAccountId) {
                accountID = lAccountId;
            }
        }
    }

    @SuppressWarnings("unused")
    private static final class VagToCreate
    {
        private final String method = "CreateVolumeAccessGroup";
        private final VagToCreateParams params;

        private VagToCreate(final String strVagName, final String[] iqns, final long[] volumeIds)
        {
            params = new VagToCreateParams(strVagName, iqns, volumeIds);
        }

        private static final class VagToCreateParams
        {
            private final String name;
            private final String[] initiators;
            private final long[] volumes;

            private VagToCreateParams(final String strVagName, final String[] iqns, final long[] volumeIds)
            {
                name = strVagName;
                initiators = iqns;
                volumes = volumeIds;
            }
        }
    }

    @SuppressWarnings("unused")
    private static final class VagToModify
    {
        private final String method = "ModifyVolumeAccessGroup";
        private final VagToModifyParams params;

        private VagToModify(final long lVagName, final String[] iqns, final long[] volumeIds)
        {
            params = new VagToModifyParams(lVagName, iqns, volumeIds);
        }

        private static final class VagToModifyParams
        {
            private final long volumeAccessGroupID;
            private final String[] initiators;
            private final long[] volumes;

            private VagToModifyParams(final long lVagName, final String[] iqns, final long[] volumeIds)
            {
                volumeAccessGroupID = lVagName;
                initiators = iqns;
                volumes = volumeIds;
            }
        }
    }

    @SuppressWarnings("unused")
    private static final class VagToGet
    {
        private final String method = "ListVolumeAccessGroups";
        private final VagToGetParams params;

        private VagToGet(final long lVagId)
        {
            params = new VagToGetParams(lVagId);
        }

        private static final class VagToGetParams
        {
            private final long startVolumeAccessGroupID;
            private final long limit = 1;

            private VagToGetParams(final long lVagId)
            {
                startVolumeAccessGroupID = lVagId;
            }
        }
    }

    @SuppressWarnings("unused")
    private static final class AllVags
    {
        private final String method = "ListVolumeAccessGroups";
        private final VagToGetParams params;

        private AllVags()
        {
            params = new VagToGetParams();
        }

        private static final class VagToGetParams
        {}
    }

    @SuppressWarnings("unused")
    private static final class VagToDelete
    {
        private final String method = "DeleteVolumeAccessGroup";
        private final VagToDeleteParams params;

        private VagToDelete(final long lVagId) {
            params = new VagToDeleteParams(lVagId);
        }

        private static final class VagToDeleteParams {
            private final long volumeAccessGroupID;

            private VagToDeleteParams(final long lVagId) {
                volumeAccessGroupID = lVagId;
            }
        }
    }

    private static final class VolumeCreateResult {
        private Result result;

        private static final class Result {
            private long volumeID;
        }
    }

    private static final class VolumeGetResult {
        private Result result;

        private static final class Result {
            private Volume[] volumes;

            private static final class Volume {
                private long volumeID;
                private String name;
                private String iqn;
                private long accountID;
                private String status;
                private boolean enable512e;
                private Qos qos;
                private long totalSize;

                private static final class Qos {
                    private long minIOPS;
                    private long maxIOPS;
                    private long burstIOPS;
                }
            }
        }
    }

    private static final class SnapshotCreateResult {
        private Result result;

        private static final class Result {
            private long snapshotID;
        }
    }

    @SuppressWarnings("unused")
    private static final class RollbackInitiatedResult {
        private Result result;

        private static final class Result {
            private long snapshotID;
        }
    }

    private static final class CloneCreateResult {
        private Result result;

        private static final class Result {
            private long cloneID;
        }
    }

    private static final class AccountAddResult {
        private Result result;

        private static final class Result {
            private long accountID;
        }
    }

    private static final class AccountGetResult {
        private Result result;

        private static final class Result {
            private Account account;

            private static final class Account {
                private long accountID;
                private String username;
                private String initiatorSecret;
                private String targetSecret;
            }
        }
    }

    private static final class VagCreateResult {
        private Result result;

        private static final class Result {
            private long volumeAccessGroupID;
        }
    }

    private static final class VagGetResult
    {
        private Result result;

        private static final class Result
        {
            private Vag[] volumeAccessGroups;

            private static final class Vag
            {
                private long volumeAccessGroupID;
                private String[] initiators;
                private long[] volumes;
            }
        }
    }

    private static final class JsonError
    {
        private Error error;

        private static final class Error {
            private String message;
        }
    }

    private static DefaultHttpClient getHttpClient(int iPort) {
        try {
            SSLContext sslContext = SSLUtils.getSSLContext();
            X509TrustManager tm = new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] xcs, String string) throws CertificateException {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] xcs, String string) throws CertificateException {
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
            };

            sslContext.init(null, new TrustManager[] {tm}, new SecureRandom());

            SSLSocketFactory socketFactory = new SSLSocketFactory(sslContext, SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
            SchemeRegistry registry = new SchemeRegistry();

            registry.register(new Scheme("https", iPort, socketFactory));

            BasicClientConnectionManager mgr = new BasicClientConnectionManager(registry);
            DefaultHttpClient client = new DefaultHttpClient();

            return new DefaultHttpClient(mgr, client.getParams());
        } catch (NoSuchAlgorithmException ex) {
            throw new CloudRuntimeException(ex.getMessage());
        } catch (KeyManagementException ex) {
            throw new CloudRuntimeException(ex.getMessage());
        }
    }

    private static String executeJsonRpc(SolidFireConnection sfConnection, String strJsonToExecute) {
        DefaultHttpClient httpClient = null;
        StringBuilder sb = new StringBuilder();

        try {
            StringEntity input = new StringEntity(strJsonToExecute);

            input.setContentType("application/json");

            httpClient = getHttpClient(sfConnection.getManagementPort());

            URI uri = new URI("https://" + sfConnection.getManagementVip() + ":" + sfConnection.getManagementPort() + "/json-rpc/6.0");
            AuthScope authScope = new AuthScope(uri.getHost(), uri.getPort(), AuthScope.ANY_SCHEME);
            UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(sfConnection.getClusterAdminUsername(), sfConnection.getClusterAdminPassword());

            httpClient.getCredentialsProvider().setCredentials(authScope, credentials);

            HttpPost postRequest = new HttpPost(uri);

            postRequest.setEntity(input);

            HttpResponse response = httpClient.execute(postRequest);

            if (!isSuccess(response.getStatusLine().getStatusCode())) {
                throw new CloudRuntimeException("Failed on JSON-RPC API call. HTTP error code = " + response.getStatusLine().getStatusCode());
            }

            try(BufferedReader br = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));) {
                String strOutput;
                while ((strOutput = br.readLine()) != null) {
                    sb.append(strOutput);
                }
            }catch (IOException ex) {
                throw new CloudRuntimeException(ex.getMessage());
            }
        } catch (UnsupportedEncodingException ex) {
            throw new CloudRuntimeException(ex.getMessage());
        } catch (ClientProtocolException ex) {
            throw new CloudRuntimeException(ex.getMessage());
        } catch (IOException ex) {
            throw new CloudRuntimeException(ex.getMessage());
        } catch (URISyntaxException ex) {
            throw new CloudRuntimeException(ex.getMessage());
        } finally {
            if (httpClient != null) {
                try {
                    httpClient.getConnectionManager().shutdown();
                } catch (Exception t) {
                    s_logger.info("[ignored]"
                            + "error shutting down http client: " + t.getLocalizedMessage());
                }
            }
        }

        return sb.toString();
    }

    private static boolean isSuccess(int iCode) {
        return iCode >= 200 && iCode < 300;
    }

    private static void verifyResult(Object result, String strJson, Gson gson) throws IllegalStateException {
        if (result != null) {
            return;
        }

        JsonError jsonError = gson.fromJson(strJson, JsonError.class);

        if (jsonError != null) {
            throw new IllegalStateException(jsonError.error.message);
        }

        throw new IllegalStateException("Problem with the following JSON: " + strJson);
    }

    private static String getVolumeName(VolumeGetResult volumeGetResult, long lVolumeId) {
        if (volumeGetResult.result.volumes != null && volumeGetResult.result.volumes.length == 1 && volumeGetResult.result.volumes[0].volumeID == lVolumeId) {
            return volumeGetResult.result.volumes[0].name;
        }

        throw new CloudRuntimeException("Could not determine the name of the volume for volume ID of " + lVolumeId + ".");
    }

    private static String getVolumeIqn(VolumeGetResult volumeGetResult, long lVolumeId) {
        if (volumeGetResult.result.volumes != null && volumeGetResult.result.volumes.length == 1 && volumeGetResult.result.volumes[0].volumeID == lVolumeId) {
            return volumeGetResult.result.volumes[0].iqn;
        }

        throw new CloudRuntimeException("Could not determine the IQN of the volume for volume ID of " + lVolumeId + ".");
    }

    private static long getVolumeAccountId(VolumeGetResult volumeGetResult, long lVolumeId) {
        if (volumeGetResult.result.volumes != null && volumeGetResult.result.volumes.length == 1 && volumeGetResult.result.volumes[0].volumeID == lVolumeId) {
            return volumeGetResult.result.volumes[0].accountID;
        }

        throw new CloudRuntimeException("Could not determine the account ID of the volume for volume ID of " + lVolumeId + ".");
    }

    private static String getVolumeStatus(VolumeGetResult volumeGetResult, long lVolumeId) {
        if (volumeGetResult.result.volumes != null && volumeGetResult.result.volumes.length == 1 && volumeGetResult.result.volumes[0].volumeID == lVolumeId) {
            return volumeGetResult.result.volumes[0].status;
        }

        throw new CloudRuntimeException("Could not determine the status of the volume for volume ID of " + lVolumeId + ".");
    }

    private static boolean getVolumeEnable512e(VolumeGetResult volumeGetResult, long lVolumeId)
    {
        if (volumeGetResult.result.volumes != null && volumeGetResult.result.volumes.length == 1 &&
            volumeGetResult.result.volumes[0].volumeID == lVolumeId)
        {
            return volumeGetResult.result.volumes[0].enable512e;
        }

        throw new CloudRuntimeException("Could not determine the enable 512 emulation of the volume for volume ID of " + lVolumeId + ".");
    }

    private static long getVolumeMinIops(VolumeGetResult volumeGetResult, long lVolumeId)
    {
        if (volumeGetResult.result.volumes != null && volumeGetResult.result.volumes.length == 1 &&
            volumeGetResult.result.volumes[0].volumeID == lVolumeId && volumeGetResult.result.volumes[0].qos != null)
        {
            return volumeGetResult.result.volumes[0].qos.minIOPS;
        }

        throw new CloudRuntimeException("Could not determine the min IOPS of the volume for volume ID of " + lVolumeId + ".");
    }

    private static long getVolumeMaxIops(VolumeGetResult volumeGetResult, long lVolumeId)
    {
        if (volumeGetResult.result.volumes != null && volumeGetResult.result.volumes.length == 1 &&
            volumeGetResult.result.volumes[0].volumeID == lVolumeId && volumeGetResult.result.volumes[0].qos != null)
        {
            return volumeGetResult.result.volumes[0].qos.maxIOPS;
        }

        throw new CloudRuntimeException("Could not determine the max IOPS of the volume for volume ID of " + lVolumeId + ".");
    }

    private static long getVolumeBurstIops(VolumeGetResult volumeGetResult, long lVolumeId)
    {
        if (volumeGetResult.result.volumes != null && volumeGetResult.result.volumes.length == 1 &&
            volumeGetResult.result.volumes[0].volumeID == lVolumeId && volumeGetResult.result.volumes[0].qos != null)
        {
            return volumeGetResult.result.volumes[0].qos.burstIOPS;
        }

        throw new CloudRuntimeException("Could not determine the burst IOPS of the volume for volume ID of " + lVolumeId + ".");
    }

    private static long getVolumeTotalSize(VolumeGetResult volumeGetResult, long lVolumeId)
    {
        if (volumeGetResult.result.volumes != null && volumeGetResult.result.volumes.length == 1 &&
            volumeGetResult.result.volumes[0].volumeID == lVolumeId)
        {
            return volumeGetResult.result.volumes[0].totalSize;
        }

        throw new CloudRuntimeException("Could not determine the total size of the volume for volume ID of " + lVolumeId + ".");
    }

    private static String[] getVagIqns(VagGetResult vagGetResult, long lVagId)
    {
        if (vagGetResult.result.volumeAccessGroups != null && vagGetResult.result.volumeAccessGroups.length == 1 &&
            vagGetResult.result.volumeAccessGroups[0].volumeAccessGroupID == lVagId)
        {
            return vagGetResult.result.volumeAccessGroups[0].initiators;
        }

        throw new CloudRuntimeException("Could not determine the IQNs of the volume access group for volume access group ID of " + lVagId + ".");
    }

    private static long[] getVagVolumeIds(VagGetResult vagGetResult, long lVagId)
    {
        if (vagGetResult.result.volumeAccessGroups != null && vagGetResult.result.volumeAccessGroups.length == 1 &&
            vagGetResult.result.volumeAccessGroups[0].volumeAccessGroupID == lVagId)
        {
            return vagGetResult.result.volumeAccessGroups[0].volumes;
        }

        throw new CloudRuntimeException("Could not determine the volume IDs of the volume access group for volume access group ID of " + lVagId + ".");
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
                String valueToReturn = token.substring(index + delimiter2.length());

                return valueToReturn;
            }
        }

        if (throwExceptionIfNotFound) {
            throw new RuntimeException("Key not found in URL");
        }

        return null;
    }
}
