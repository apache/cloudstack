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
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import com.cloud.utils.exception.CloudRuntimeException;

public class SolidFireUtil {
    public static final String PROVIDER_NAME = "SolidFire";

    public static final String MANAGEMENT_VIP = "mVip";
    public static final String STORAGE_VIP = "sVip";

    public static final String MANAGEMENT_PORT = "mPort";
    public static final String STORAGE_PORT = "sPort";

    public static final String CLUSTER_ADMIN_USERNAME = "clusterAdminUsername";
    public static final String CLUSTER_ADMIN_PASSWORD = "clusterAdminPassword";

    public static final String CLUSTER_DEFAULT_MIN_IOPS = "clusterDefaultMinIops";
    public static final String CLUSTER_DEFAULT_MAX_IOPS = "clusterDefaultMaxIops";
    public static final String CLUSTER_DEFAULT_BURST_IOPS_PERCENT_OF_MAX_IOPS = "clusterDefaultBurstIopsPercentOfMaxIops";

    public static final String ACCOUNT_ID = "accountId";

    public static final String CHAP_INITIATOR_USERNAME = "chapInitiatorUsername";
    public static final String CHAP_INITIATOR_SECRET = "chapInitiatorSecret";

    public static final String CHAP_TARGET_USERNAME = "chapTargetUsername";
    public static final String CHAP_TARGET_SECRET = "chapTargetSecret";

    public static long createSolidFireVolume(String strSfMvip, int iSfPort, String strSfAdmin, String strSfPassword,
            String strSfVolumeName, long lSfAccountId, long lTotalSize, boolean bEnable512e, final String strCloudStackVolumeSize,
            long lMinIops, long lMaxIops, long lBurstIops)
    {
        final Gson gson = new GsonBuilder().create();

        VolumeToCreate volumeToCreate =
            new VolumeToCreate(strSfVolumeName, lSfAccountId, lTotalSize, bEnable512e, strCloudStackVolumeSize, lMinIops, lMaxIops, lBurstIops);

        String strVolumeToCreateJson = gson.toJson(volumeToCreate);

        String strVolumeCreateResultJson = executeJsonRpc(strVolumeToCreateJson, strSfMvip, iSfPort, strSfAdmin, strSfPassword);

        VolumeCreateResult volumeCreateResult = gson.fromJson(strVolumeCreateResultJson, VolumeCreateResult.class);

        verifyResult(volumeCreateResult.result, strVolumeCreateResultJson, gson);

        return volumeCreateResult.result.volumeID;
    }

    public static SolidFireVolume getSolidFireVolume(String strSfMvip, int iSfPort, String strSfAdmin, String strSfPassword, long lVolumeId)
    {
        final Gson gson = new GsonBuilder().create();

        VolumeToGet volumeToGet = new VolumeToGet(lVolumeId);

        String strVolumeToGetJson = gson.toJson(volumeToGet);

        String strVolumeGetResultJson = executeJsonRpc(strVolumeToGetJson, strSfMvip, iSfPort, strSfAdmin, strSfPassword);

        VolumeGetResult volumeGetResult = gson.fromJson(strVolumeGetResultJson, VolumeGetResult.class);

        verifyResult(volumeGetResult.result, strVolumeGetResultJson, gson);

        String strVolumeName = getVolumeName(volumeGetResult, lVolumeId);
        String strVolumeIqn = getVolumeIqn(volumeGetResult, lVolumeId);
        long lAccountId = getVolumeAccountId(volumeGetResult, lVolumeId);
        String strVolumeStatus = getVolumeStatus(volumeGetResult, lVolumeId);
        long lTotalSize = getVolumeTotalSize(volumeGetResult, lVolumeId);

        return new SolidFireVolume(lVolumeId, strVolumeName, strVolumeIqn, lAccountId, strVolumeStatus, lTotalSize);
    }

    public static List<SolidFireVolume> getSolidFireVolumesForAccountId(String strSfMvip, int iSfPort, String strSfAdmin, String strSfPassword, long lAccountId) {
        final Gson gson = new GsonBuilder().create();

        VolumesToGetForAccount volumesToGetForAccount = new VolumesToGetForAccount(lAccountId);

        String strVolumesToGetForAccountJson = gson.toJson(volumesToGetForAccount);

        String strVolumesGetForAccountResultJson = executeJsonRpc(strVolumesToGetForAccountJson, strSfMvip, iSfPort, strSfAdmin, strSfPassword);

        VolumeGetResult volumeGetResult = gson.fromJson(strVolumesGetForAccountResultJson, VolumeGetResult.class);

        verifyResult(volumeGetResult.result, strVolumesGetForAccountResultJson, gson);

        List<SolidFireVolume> sfVolumes = new ArrayList<SolidFireVolume>();

        for (VolumeGetResult.Result.Volume volume : volumeGetResult.result.volumes) {
            sfVolumes.add(new SolidFireVolume(volume.volumeID, volume.name, volume.iqn, volume.accountID, volume.status, volume.totalSize));
        }

        return sfVolumes;
    }

    public static List<SolidFireVolume> getDeletedVolumes(String strSfMvip, int iSfPort, String strSfAdmin, String strSfPassword)
    {
        final Gson gson = new GsonBuilder().create();

        ListDeletedVolumes listDeletedVolumes = new ListDeletedVolumes();

        String strListDeletedVolumesJson = gson.toJson(listDeletedVolumes);

        String strListDeletedVolumesResultJson = executeJsonRpc(strListDeletedVolumesJson, strSfMvip, iSfPort,
                strSfAdmin, strSfPassword);

        VolumeGetResult volumeGetResult = gson.fromJson(strListDeletedVolumesResultJson, VolumeGetResult.class);

        verifyResult(volumeGetResult.result, strListDeletedVolumesResultJson, gson);

        List<SolidFireVolume> deletedVolumes = new ArrayList<SolidFireVolume> ();

        for (VolumeGetResult.Result.Volume volume : volumeGetResult.result.volumes) {
            deletedVolumes.add(new SolidFireVolume(volume.volumeID, volume.name, volume.iqn, volume.accountID, volume.status, volume.totalSize));
        }

        return deletedVolumes;
    }

    public static SolidFireVolume deleteSolidFireVolume(String strSfMvip, int iSfPort, String strSfAdmin, String strSfPassword, long lVolumeId)
    {
        SolidFireVolume sfVolume = getSolidFireVolume(strSfMvip, iSfPort, strSfAdmin, strSfPassword, lVolumeId);

        final Gson gson = new GsonBuilder().create();

        VolumeToDelete volumeToDelete = new VolumeToDelete(lVolumeId);

        String strVolumeToDeleteJson = gson.toJson(volumeToDelete);

        executeJsonRpc(strVolumeToDeleteJson, strSfMvip, iSfPort, strSfAdmin, strSfPassword);

        return sfVolume;
    }

   public static void purgeSolidFireVolume(String strSfMvip, int iSfPort, String strSfAdmin, String strSfPassword, long lVolumeId)
    {
        final Gson gson = new GsonBuilder().create();

        VolumeToPurge volumeToPurge = new VolumeToPurge(lVolumeId);

        String strVolumeToPurgeJson = gson.toJson(volumeToPurge);

        executeJsonRpc(strVolumeToPurgeJson, strSfMvip, iSfPort, strSfAdmin, strSfPassword);
    }

    private static final String ACTIVE = "active";

    public static class SolidFireVolume {
        private final long _id;
        private final String _name;
        private final String _iqn;
        private final long _accountId;
        private final String _status;
        private final long _totalSize;

        public SolidFireVolume(long id, String name, String iqn,
                long accountId, String status, long totalSize)
        {
            _id = id;
            _name = name;
            _iqn = "/" + iqn + "/0";
            _accountId = accountId;
            _status = status;
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

    public static long createSolidFireAccount(String strSfMvip, int iSfPort, String strSfAdmin, String strSfPassword, String strAccountName)
    {
        final Gson gson = new GsonBuilder().create();

        AccountToAdd accountToAdd = new AccountToAdd(strAccountName);

        String strAccountAddJson = gson.toJson(accountToAdd);

        String strAccountAddResultJson = executeJsonRpc(strAccountAddJson, strSfMvip, iSfPort, strSfAdmin, strSfPassword);

        AccountAddResult accountAddResult = gson.fromJson(strAccountAddResultJson, AccountAddResult.class);

        verifyResult(accountAddResult.result, strAccountAddResultJson, gson);

        return accountAddResult.result.accountID;
    }

    public static SolidFireAccount getSolidFireAccountById(String strSfMvip, int iSfPort, String strSfAdmin, String strSfPassword, long lSfAccountId)
    {
        final Gson gson = new GsonBuilder().create();

        AccountToGetById accountToGetById = new AccountToGetById(lSfAccountId);

        String strAccountToGetByIdJson = gson.toJson(accountToGetById);

        String strAccountGetByIdResultJson = executeJsonRpc(strAccountToGetByIdJson, strSfMvip, iSfPort, strSfAdmin, strSfPassword);

        AccountGetResult accountGetByIdResult = gson.fromJson(strAccountGetByIdResultJson, AccountGetResult.class);

        verifyResult(accountGetByIdResult.result, strAccountGetByIdResultJson, gson);

        String strSfAccountName = accountGetByIdResult.result.account.username;
        String strSfAccountInitiatorSecret = accountGetByIdResult.result.account.initiatorSecret;
        String strSfAccountTargetSecret = accountGetByIdResult.result.account.targetSecret;

        return new SolidFireAccount(lSfAccountId, strSfAccountName, strSfAccountInitiatorSecret, strSfAccountTargetSecret);
    }

    public static SolidFireAccount getSolidFireAccountByName(String strSfMvip, int iSfPort, String strSfAdmin, String strSfPassword, String strSfAccountName)
    {
        final Gson gson = new GsonBuilder().create();

        AccountToGetByName accountToGetByName = new AccountToGetByName(strSfAccountName);

        String strAccountToGetByNameJson = gson.toJson(accountToGetByName);

        String strAccountGetByNameResultJson = executeJsonRpc(strAccountToGetByNameJson, strSfMvip, iSfPort, strSfAdmin, strSfPassword);

        AccountGetResult accountGetByNameResult = gson.fromJson(strAccountGetByNameResultJson, AccountGetResult.class);

        verifyResult(accountGetByNameResult.result, strAccountGetByNameResultJson, gson);

        long lSfAccountId = accountGetByNameResult.result.account.accountID;
        String strSfAccountInitiatorSecret = accountGetByNameResult.result.account.initiatorSecret;
        String strSfAccountTargetSecret = accountGetByNameResult.result.account.targetSecret;

        return new SolidFireAccount(lSfAccountId, strSfAccountName, strSfAccountInitiatorSecret, strSfAccountTargetSecret);
    }

    public static void deleteSolidFireAccount(String strSfMvip, int iSfPort, String strSfAdmin, String strSfPassword, long lAccountId)
    {
        final Gson gson = new GsonBuilder().create();

        AccountToRemove accountToRemove = new AccountToRemove(lAccountId);

        String strAccountToRemoveJson = gson.toJson(accountToRemove);

        executeJsonRpc(strAccountToRemoveJson, strSfMvip, iSfPort, strSfAdmin, strSfPassword);
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

    public static long createSolidFireVag(String strSfMvip, int iSfPort, String strSfAdmin, String strSfPassword, String strVagName,
            String[] iqns, long[] volumeIds)
    {
        final Gson gson = new GsonBuilder().create();

        VagToCreate vagToCreate = new VagToCreate(strVagName, iqns, volumeIds);

        String strVagCreateJson = gson.toJson(vagToCreate);

        String strVagCreateResultJson = executeJsonRpc(strVagCreateJson, strSfMvip, iSfPort, strSfAdmin, strSfPassword);

        VagCreateResult vagCreateResult = gson.fromJson(strVagCreateResultJson, VagCreateResult.class);

        verifyResult(vagCreateResult.result, strVagCreateResultJson, gson);

        return vagCreateResult.result.volumeAccessGroupID;
    }

    public static void modifySolidFireVag(String strSfMvip, int iSfPort, String strSfAdmin, String strSfPassword, long lVagId,
            String[] iqns, long[] volumeIds)
    {
        final Gson gson = new GsonBuilder().create();

        VagToModify vagToModify = new VagToModify(lVagId, iqns, volumeIds);

        String strVagModifyJson = gson.toJson(vagToModify);

        executeJsonRpc(strVagModifyJson, strSfMvip, iSfPort, strSfAdmin, strSfPassword);
    }

    public static SolidFireVag getSolidFireVag(String strSfMvip, int iSfPort, String strSfAdmin, String strSfPassword, long lVagId)
    {
        final Gson gson = new GsonBuilder().create();

        VagToGet vagToGet = new VagToGet(lVagId);

        String strVagToGetJson = gson.toJson(vagToGet);

        String strVagGetResultJson = executeJsonRpc(strVagToGetJson, strSfMvip, iSfPort, strSfAdmin, strSfPassword);

        VagGetResult vagGetResult = gson.fromJson(strVagGetResultJson, VagGetResult.class);

        verifyResult(vagGetResult.result, strVagGetResultJson, gson);

        String[] vagIqns = getVagIqns(vagGetResult, lVagId);
        long[] vagVolumeIds = getVagVolumeIds(vagGetResult, lVagId);

        return new SolidFireVag(lVagId, vagIqns, vagVolumeIds);
    }

    public static List<SolidFireVag> getAllSolidFireVags(String strSfMvip, int iSfPort, String strSfAdmin, String strSfPassword)
    {
        final Gson gson = new GsonBuilder().create();

        AllVags allVags = new AllVags();

        String strAllVagsJson = gson.toJson(allVags);

        String strAllVagsGetResultJson = executeJsonRpc(strAllVagsJson, strSfMvip, iSfPort, strSfAdmin, strSfPassword);

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

    public static void deleteSolidFireVag(String strSfMvip, int iSfPort, String strSfAdmin, String strSfPassword, long lVagId)
    {
        final Gson gson = new GsonBuilder().create();

        VagToDelete vagToDelete = new VagToDelete(lVagId);

        String strVagToDeleteJson = gson.toJson(vagToDelete);

        executeJsonRpc(strVagToDeleteJson, strSfMvip, iSfPort, strSfAdmin, strSfPassword);
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
    private static final class VolumeToCreate {
        private final String method = "CreateVolume";
        private final VolumeToCreateParams params;

        private VolumeToCreate(final String strVolumeName, final long lAccountId, final long lTotalSize, final boolean bEnable512e, final String strCloudStackVolumeSize,
                final long lMinIOPS, final long lMaxIOPS, final long lBurstIOPS) {
            params = new VolumeToCreateParams(strVolumeName, lAccountId, lTotalSize, bEnable512e, strCloudStackVolumeSize, lMinIOPS, lMaxIOPS, lBurstIOPS);
        }

        private static final class VolumeToCreateParams {
            private final String name;
            private final long accountID;
            private final long totalSize;
            private final boolean enable512e;
            private final VolumeToCreateParamsQoS qos;
            private final VolumeToCreateParamsAttributes attributes;

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
            private long volumeID;

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
            private long volumeID;

            private VolumeToPurgeParams(final long lVolumeId) {
                volumeID = lVolumeId;
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
            private long accountID;

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
            private long volumeAccessGroupID;

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
                private long totalSize;
            }
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
            SSLContext sslContext = SSLContext.getInstance("SSL");
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

    private static String executeJsonRpc(String strJsonToExecute, String strMvip, int iPort, String strAdmin, String strPassword) {
        DefaultHttpClient httpClient = null;
        StringBuilder sb = new StringBuilder();

        try {
            StringEntity input = new StringEntity(strJsonToExecute);

            input.setContentType("application/json");

            httpClient = getHttpClient(iPort);

            URI uri = new URI("https://" + strMvip + ":" + iPort + "/json-rpc/5.0");
            AuthScope authScope = new AuthScope(uri.getHost(), uri.getPort(), AuthScope.ANY_SCHEME);
            UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(strAdmin, strPassword);

            httpClient.getCredentialsProvider().setCredentials(authScope, credentials);

            HttpPost postRequest = new HttpPost(uri);

            postRequest.setEntity(input);

            HttpResponse response = httpClient.execute(postRequest);

            if (!isSuccess(response.getStatusLine().getStatusCode())) {
                throw new CloudRuntimeException("Failed on JSON-RPC API call. HTTP error code = " + response.getStatusLine().getStatusCode());
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

            String strOutput;

            while ((strOutput = br.readLine()) != null) {
                sb.append(strOutput);
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
                }
            }
        }

        return sb.toString();
    }

    private static boolean isSuccess(int iCode) {
        return iCode >= 200 && iCode < 300;
    }

    private static void verifyResult(Object obj, String strJson, Gson gson) throws IllegalStateException {
        if (obj != null) {
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
}
