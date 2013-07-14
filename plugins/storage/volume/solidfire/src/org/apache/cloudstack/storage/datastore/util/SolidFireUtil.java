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
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.ArrayList;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.BasicClientConnectionManager;

import com.cloud.utils.exception.CloudRuntimeException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class SolidFireUtil
{
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
            String strSfVolumeName, long lSfAccountId, long lTotalSize, boolean bEnable512e,
            long lMinIops, long lMaxIops, long lBurstIops)
    {
        final Gson gson = new GsonBuilder().create();

        VolumeToCreate volumeToCreate = new VolumeToCreate(strSfVolumeName, lSfAccountId, lTotalSize, bEnable512e,
                lMinIops, lMaxIops, lBurstIops);

        String strVolumeToCreateJson = gson.toJson(volumeToCreate);

        String strVolumeCreateResultJson = executeJsonRpc(strVolumeToCreateJson, strSfMvip, iSfPort, strSfAdmin, strSfPassword);

        VolumeCreateResult volumeCreateResult = gson.fromJson(strVolumeCreateResultJson, VolumeCreateResult.class);

        verifyResult(volumeCreateResult.result, strVolumeCreateResultJson, gson);

        return volumeCreateResult.result.volumeID;
    }

    public static void deleteSolidFireVolume(String strSfMvip, int iSfPort, String strSfAdmin, String strSfPassword, long lVolumeId)
    {
        final Gson gson = new GsonBuilder().create();

        VolumeToDelete volumeToDelete = new VolumeToDelete(lVolumeId);

        String strVolumeToDeleteJson = gson.toJson(volumeToDelete);

        executeJsonRpc(strVolumeToDeleteJson, strSfMvip, iSfPort, strSfAdmin, strSfPassword);
    }

   public static void purgeSolidFireVolume(String strSfMvip, int iSfPort, String strSfAdmin, String strSfPassword, long lVolumeId)
    {
        final Gson gson = new GsonBuilder().create();

        VolumeToPurge volumeToPurge = new VolumeToPurge(lVolumeId);

        String strVolumeToPurgeJson = gson.toJson(volumeToPurge);

        executeJsonRpc(strVolumeToPurgeJson, strSfMvip, iSfPort, strSfAdmin, strSfPassword);
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

        return new SolidFireVolume(lVolumeId, strVolumeName, strVolumeIqn, lAccountId, strVolumeStatus);
    }

    public static List<SolidFireVolume> getSolidFireVolumesForAccountId(String strSfMvip, int iSfPort,
            String strSfAdmin, String strSfPassword, long lAccountId)
    {
        final Gson gson = new GsonBuilder().create();

        VolumesToGetForAccount volumesToGetForAccount = new VolumesToGetForAccount(lAccountId);

        String strVolumesToGetForAccountJson = gson.toJson(volumesToGetForAccount);

        String strVolumesGetForAccountResultJson = executeJsonRpc(strVolumesToGetForAccountJson, strSfMvip, iSfPort,
                strSfAdmin, strSfPassword);

        VolumeGetResult volumeGetResult = gson.fromJson(strVolumesGetForAccountResultJson, VolumeGetResult.class);

        verifyResult(volumeGetResult.result, strVolumesGetForAccountResultJson, gson);

        List<SolidFireVolume> sfVolumes = new ArrayList<SolidFireVolume>();

        for (VolumeGetResult.Result.Volume volume : volumeGetResult.result.volumes) {
            sfVolumes.add(new SolidFireVolume(volume.volumeID, volume.name, volume.iqn, volume.accountID, volume.status));
        }

        return sfVolumes;
	}

	private static final String ACTIVE = "active";

    public static class SolidFireVolume
    {
        private final long _id;
        private final String _name;
        private final String _iqn;
        private final long _accountId;
        private final String _status;

        public SolidFireVolume(long id, String name, String iqn,
                long accountId, String status)
        {
            _id = id;
            _name = name;
            _iqn = "/" + iqn + "/0";
            _accountId = accountId;
            _status = status;
        }

        public long getId()
        {
            return _id;
        }

        public String getName()
        {
            return _name;
        }

        public String getIqn()
        {
            return _iqn;
        }

        public long getAccountId()
        {
            return _accountId;
        }

        public boolean isActive()
        {
            return ACTIVE.equalsIgnoreCase(_status);
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
                isActive() == sfv.isActive()) {
                return true;
            }

            return false;
        }
	}

    public static long createSolidFireAccount(String strSfMvip, int iSfPort, String strSfAdmin, String strSfPassword,
            String strAccountName)
    {
        final Gson gson = new GsonBuilder().create();

        AccountToAdd accountToAdd = new AccountToAdd(strAccountName);

        String strAccountAddJson = gson.toJson(accountToAdd);

        String strAccountAddResultJson = executeJsonRpc(strAccountAddJson, strSfMvip, iSfPort, strSfAdmin, strSfPassword);

        AccountAddResult accountAddResult = gson.fromJson(strAccountAddResultJson, AccountAddResult.class);

        verifyResult(accountAddResult.result, strAccountAddResultJson, gson);

        return accountAddResult.result.accountID;
    }

    public static void deleteSolidFireAccount(String strSfMvip, int iSfPort, String strSfAdmin, String strSfPassword,
            long lAccountId)
    {
        final Gson gson = new GsonBuilder().create();

        AccountToRemove accountToRemove = new AccountToRemove(lAccountId);

        String strAccountToRemoveJson = gson.toJson(accountToRemove);

        executeJsonRpc(strAccountToRemoveJson, strSfMvip, iSfPort, strSfAdmin, strSfPassword);
    }

    public static SolidFireAccount getSolidFireAccountById(String strSfMvip, int iSfPort, String strSfAdmin, String strSfPassword,
            long lSfAccountId)
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

    public static SolidFireAccount getSolidFireAccountByName(String strSfMvip, int iSfPort, String strSfAdmin, String strSfPassword,
            String strSfAccountName)
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

    public static class SolidFireAccount
    {
        private final long _id;
        private final String _name;
        private final String _initiatorSecret;
        private final String _targetSecret;

        public SolidFireAccount(long id, String name, String initiatorSecret, String targetSecret)
        {
            _id = id;
            _name = name;
            _initiatorSecret = initiatorSecret;
            _targetSecret = targetSecret;
        }

        public long getId()
        {
            return _id;
        }

        public String getName()
        {
            return _name;
        }

        public String getInitiatorSecret()
        {
            return _initiatorSecret;
        }

        public String getTargetSecret()
        {
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
            deletedVolumes.add(new SolidFireVolume(volume.volumeID, volume.name, volume.iqn, volume.accountID, volume.status));
        }

        return deletedVolumes;
    }

    public static long createSolidFireVag(String strSfMvip, int iSfPort, String strSfAdmin, String strSfPassword, String strVagName)
    {
        final Gson gson = new GsonBuilder().create();

        VagToCreate vagToCreate = new VagToCreate(strVagName);

        String strVagCreateJson = gson.toJson(vagToCreate);

        String strVagCreateResultJson = executeJsonRpc(strVagCreateJson, strSfMvip, iSfPort, strSfAdmin, strSfPassword);

        VagCreateResult vagCreateResult = gson.fromJson(strVagCreateResultJson, VagCreateResult.class);

        verifyResult(vagCreateResult.result, strVagCreateResultJson, gson);

        return vagCreateResult.result.volumeAccessGroupID;
    }

    public static void deleteSolidFireVag(String strSfMvip, int iSfPort, String strSfAdmin, String strSfPassword, long lVagId)
    {
        final Gson gson = new GsonBuilder().create();

        VagToDelete vagToDelete = new VagToDelete(lVagId);

        String strVagToDeleteJson = gson.toJson(vagToDelete);

        executeJsonRpc(strVagToDeleteJson, strSfMvip, iSfPort, strSfAdmin, strSfPassword);
    }

    @SuppressWarnings("unused")
    private static final class VolumeToCreate
    {
        private final String method = "CreateVolume";
        private final VolumeToCreateParams params;

        private VolumeToCreate(final String strVolumeName, final long lAccountId, final long lTotalSize,
                final boolean bEnable512e, final long lMinIOPS, final long lMaxIOPS, final long lBurstIOPS)
        {
            params = new VolumeToCreateParams(strVolumeName, lAccountId, lTotalSize, bEnable512e,
                    lMinIOPS, lMaxIOPS, lBurstIOPS);
        }

        private static final class VolumeToCreateParams
        {
            private final String name;
            private final long accountID;
            private final long totalSize;
            private final boolean enable512e;
            private final VolumeToCreateParamsQoS qos;

            private VolumeToCreateParams(final String strVolumeName, final long lAccountId, final long lTotalSize,
                    final boolean bEnable512e, final long lMinIOPS, final long lMaxIOPS, final long lBurstIOPS)
            {
                name = strVolumeName;
                accountID = lAccountId;
                totalSize = lTotalSize;
                enable512e = bEnable512e;

                qos = new VolumeToCreateParamsQoS(lMinIOPS, lMaxIOPS, lBurstIOPS);
            }

            private static final class VolumeToCreateParamsQoS
            {
                private final long minIOPS;
                private final long maxIOPS;
                private final long burstIOPS;

                private VolumeToCreateParamsQoS(final long lMinIOPS, final long lMaxIOPS, final long lBurstIOPS)
                {
                    minIOPS = lMinIOPS;
                    maxIOPS = lMaxIOPS;
                    burstIOPS = lBurstIOPS;
                }
            }
        }
    }

    @SuppressWarnings("unused")
    private static final class VolumeToDelete
    {
        private final String method = "DeleteVolume";
        private final VolumeToDeleteParams params;

        private VolumeToDelete(final long lVolumeId)
        {
            params = new VolumeToDeleteParams(lVolumeId);
        }

        private static final class VolumeToDeleteParams
        {
            private long volumeID;

            private VolumeToDeleteParams(final long lVolumeId)
            {
                volumeID = lVolumeId;
            }
        }
    }

    @SuppressWarnings("unused")
    private static final class ListDeletedVolumes
    {
        private final String method = "ListDeletedVolumes";
    }

    @SuppressWarnings("unused")
    private static final class VolumeToPurge
    {
        private final String method = "PurgeDeletedVolume";
        private final VolumeToPurgeParams params;

        private VolumeToPurge(final long lVolumeId)
        {
            params = new VolumeToPurgeParams(lVolumeId);
        }

        private static final class VolumeToPurgeParams
        {
            private long volumeID;

            private VolumeToPurgeParams(final long lVolumeId)
            {
                volumeID = lVolumeId;
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
    private static final class AccountToRemove
    {
        private final String method = "RemoveAccount";
        private final AccountToRemoveParams params;

        private AccountToRemove(final long lAccountId)
        {
            params = new AccountToRemoveParams(lAccountId);
        }

        private static final class AccountToRemoveParams
        {
            private long accountID;

            private AccountToRemoveParams(final long lAccountId)
            {
                accountID = lAccountId;
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
    private static final class VagToCreate
    {
        private final String method = "CreateVolumeAccessGroup";
        private final VagToCreateParams params;

        private VagToCreate(final String strVagName)
        {
            params = new VagToCreateParams(strVagName);
        }

        private static final class VagToCreateParams
        {
            private final String name;

            private VagToCreateParams(final String strVagName)
            {
                name = strVagName;
            }
        }
    }

    @SuppressWarnings("unused")
    private static final class VagToDelete
    {
        private final String method = "DeleteVolumeAccessGroup";
        private final VagToDeleteParams params;

        private VagToDelete(final long lVagId)
        {
            params = new VagToDeleteParams(lVagId);
        }

        private static final class VagToDeleteParams
        {
            private long volumeAccessGroupID;

            private VagToDeleteParams(final long lVagId)
            {
                volumeAccessGroupID = lVagId;
            }
        }
    }

    private static final class VolumeCreateResult
    {
        private Result result;

        private static final class Result
        {
            private long volumeID;
        }
    }

    private static final class VolumeGetResult
    {
        private Result result;

        private static final class Result
        {
            private Volume[] volumes;

            private static final class Volume
            {
                private long volumeID;
                private String name;
                private String iqn;
                private long accountID;
                private String status;
            }
        }
    }

    private static final class AccountAddResult
    {
        private Result result;

        private static final class Result
        {
            private long accountID;
        }
    }

    private static final class AccountGetResult
    {
        private Result result;

        private static final class Result
        {
            private Account account;

            private static final class Account
            {
                private long accountID;
                private String username;
                private String initiatorSecret;
                private String targetSecret;
            }
        }
    }

    private static final class VagCreateResult
    {
        private Result result;

        private static final class Result
        {
            private long volumeAccessGroupID;
        }
    }

    private static final class JsonError
    {
        private Error error;

        private static final class Error
        {
            private String message;
        }
    }

    private static DefaultHttpClient getHttpClient(int iPort) {
        try {
            SSLContext sslContext = SSLContext.getInstance("SSL");
            X509TrustManager tm = new X509TrustManager() {
                public void checkClientTrusted(X509Certificate[] xcs, String string) throws CertificateException {
                }

                public void checkServerTrusted(X509Certificate[] xcs, String string) throws CertificateException {
                }

                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
            };

            sslContext.init(null, new TrustManager[] { tm }, new SecureRandom());

            SSLSocketFactory socketFactory = new SSLSocketFactory(sslContext, SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
            SchemeRegistry registry = new SchemeRegistry();

            registry.register(new Scheme("https", iPort, socketFactory));

            BasicClientConnectionManager mgr = new BasicClientConnectionManager(registry);
            DefaultHttpClient client = new DefaultHttpClient();

            return new DefaultHttpClient(mgr, client.getParams());
        }
        catch (NoSuchAlgorithmException ex) {
            throw new CloudRuntimeException(ex.getMessage());
        }
        catch (KeyManagementException ex) {
            throw new CloudRuntimeException(ex.getMessage());
        }
    }

    private static String executeJsonRpc(String strJsonToExecute, String strMvip, int iPort,
            String strAdmin, String strPassword)
    {
        DefaultHttpClient httpClient = null;
        StringBuilder sb = new StringBuilder();

        try
        {
            StringEntity input = new StringEntity(strJsonToExecute);

            input.setContentType("application/json");

            httpClient = getHttpClient(iPort);

            URI uri = new URI("https://" + strMvip + ":" + iPort + "/json-rpc/1.0");
            AuthScope authScope = new AuthScope(uri.getHost(), uri.getPort(), AuthScope.ANY_SCHEME);
            UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(strAdmin, strPassword);

            httpClient.getCredentialsProvider().setCredentials(authScope, credentials);

            HttpPost postRequest = new HttpPost(uri);

            postRequest.setEntity(input);

            HttpResponse response = httpClient.execute(postRequest);

            if (!isSuccess(response.getStatusLine().getStatusCode()))
            {
                throw new CloudRuntimeException("Failed on JSON-RPC API call. HTTP error code = " + response.getStatusLine().getStatusCode());
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

            String strOutput;

            while ((strOutput = br.readLine()) != null)
            {
                sb.append(strOutput);
            }
        }
        catch (UnsupportedEncodingException ex) {
            throw new CloudRuntimeException(ex.getMessage());
        }
        catch (ClientProtocolException ex) {
            throw new CloudRuntimeException(ex.getMessage());
        }
        catch (IOException ex) {
            throw new CloudRuntimeException(ex.getMessage());
        }
        catch (URISyntaxException ex) {
            throw new CloudRuntimeException(ex.getMessage());
        }
        finally {
            if (httpClient != null) {
                try {
                    httpClient.getConnectionManager().shutdown();
                } catch (Exception t) {}
            }
        }

        return sb.toString();
    }

    private static boolean isSuccess(int iCode) {
        return iCode >= 200 && iCode < 300;
    }

    private static void verifyResult(Object obj, String strJson, Gson gson) throws IllegalStateException
    {
        if (obj != null)
        {
            return;
        }

        JsonError jsonError = gson.fromJson(strJson, JsonError.class);

        if (jsonError != null)
        {
            throw new IllegalStateException(jsonError.error.message);
        }

        throw new IllegalStateException("Problem with the following JSON: " + strJson);
    }

    private static String getVolumeName(VolumeGetResult volumeGetResult, long lVolumeId)
    {
        if (volumeGetResult.result.volumes != null && volumeGetResult.result.volumes.length == 1 &&
            volumeGetResult.result.volumes[0].volumeID == lVolumeId)
        {
            return volumeGetResult.result.volumes[0].name;
        }

        throw new CloudRuntimeException("Could not determine the name of the volume, " +
                "but the volume was created with an ID of " + lVolumeId + ".");
    }

    private static String getVolumeIqn(VolumeGetResult volumeGetResult, long lVolumeId)
    {
        if (volumeGetResult.result.volumes != null && volumeGetResult.result.volumes.length == 1 &&
            volumeGetResult.result.volumes[0].volumeID == lVolumeId)
        {
            return volumeGetResult.result.volumes[0].iqn;
        }

        throw new CloudRuntimeException("Could not determine the IQN of the volume, " +
                "but the volume was created with an ID of " + lVolumeId + ".");
    }

    private static long getVolumeAccountId(VolumeGetResult volumeGetResult, long lVolumeId)
    {
        if (volumeGetResult.result.volumes != null && volumeGetResult.result.volumes.length == 1 &&
            volumeGetResult.result.volumes[0].volumeID == lVolumeId)
        {
            return volumeGetResult.result.volumes[0].accountID;
        }

        throw new CloudRuntimeException("Could not determine the volume's account ID, " +
                "but the volume was created with an ID of " + lVolumeId + ".");
    }

    private static String getVolumeStatus(VolumeGetResult volumeGetResult, long lVolumeId)
    {
        if (volumeGetResult.result.volumes != null && volumeGetResult.result.volumes.length == 1 &&
            volumeGetResult.result.volumes[0].volumeID == lVolumeId)
        {
            return volumeGetResult.result.volumes[0].status;
        }

        throw new CloudRuntimeException("Could not determine the status of the volume, " +
                "but the volume was created with an ID of " + lVolumeId + ".");
    }
}
