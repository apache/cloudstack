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
package org.apache.cloudstack.storage.datastore.driver;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.*;
import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.storage.command.CommandResult;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailVO;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailsDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.datastore.util.SolidFireUtil;
import org.apache.commons.lang.StringUtils;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.to.DataObjectType;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.agent.api.to.DataTO;
import com.cloud.dc.ClusterDetailsVO;
import com.cloud.dc.ClusterDetailsDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.StoragePool;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.dao.VolumeDetailsDao;
import com.cloud.user.AccountDetailVO;
import com.cloud.user.AccountDetailsDao;
import com.cloud.user.AccountVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.exception.CloudRuntimeException;

public class SolidfirePrimaryDataStoreDriver implements PrimaryDataStoreDriver {
    @Inject private PrimaryDataStoreDao _storagePoolDao;
    @Inject private StoragePoolDetailsDao _storagePoolDetailsDao;
    @Inject private VolumeDao _volumeDao;
    @Inject private VolumeDetailsDao _volumeDetailsDao;
    @Inject private DataCenterDao _zoneDao;
    @Inject private AccountDao _accountDao;
    @Inject private AccountDetailsDao _accountDetailsDao;
    @Inject private ClusterDetailsDao _clusterDetailsDao;
    @Inject private HostDao _hostDao;

    @Override
    public Map<String, String> getCapabilities() {
        return null;
    }

    @Override
    public DataTO getTO(DataObject data) {
        return null;
    }

    @Override
    public DataStoreTO getStoreTO(DataStore store) {
        return null;
    }

    private static class SolidFireConnection {
        private final String _managementVip;
        private final int _managementPort;
        private final String _clusterAdminUsername;
        private final String _clusterAdminPassword;

        public SolidFireConnection(String managementVip, int managementPort,
                String clusterAdminUsername, String clusterAdminPassword) {
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

    private SolidFireConnection getSolidFireConnection(long storagePoolId) {
        StoragePoolDetailVO storagePoolDetail = _storagePoolDetailsDao.findDetail(storagePoolId, SolidFireUtil.MANAGEMENT_VIP);

        String mVip = storagePoolDetail.getValue();

        storagePoolDetail = _storagePoolDetailsDao.findDetail(storagePoolId, SolidFireUtil.MANAGEMENT_PORT);

        int mPort = Integer.parseInt(storagePoolDetail.getValue());

        storagePoolDetail = _storagePoolDetailsDao.findDetail(storagePoolId, SolidFireUtil.CLUSTER_ADMIN_USERNAME);

        String clusterAdminUsername = storagePoolDetail.getValue();

        storagePoolDetail = _storagePoolDetailsDao.findDetail(storagePoolId, SolidFireUtil.CLUSTER_ADMIN_PASSWORD);

        String clusterAdminPassword = storagePoolDetail.getValue();

        return new SolidFireConnection(mVip, mPort, clusterAdminUsername, clusterAdminPassword);
    }

    private SolidFireUtil.SolidFireAccount createSolidFireAccount(String sfAccountName, SolidFireConnection sfConnection) {
        String mVip = sfConnection.getManagementVip();
        int mPort = sfConnection.getManagementPort();
        String clusterAdminUsername = sfConnection.getClusterAdminUsername();
        String clusterAdminPassword = sfConnection.getClusterAdminPassword();

        long accountNumber = SolidFireUtil.createSolidFireAccount(mVip, mPort, clusterAdminUsername, clusterAdminPassword, sfAccountName);

        return SolidFireUtil.getSolidFireAccountById(mVip, mPort, clusterAdminUsername, clusterAdminPassword, accountNumber);
    }

    private void updateCsDbWithAccountInfo(long csAccountId, SolidFireUtil.SolidFireAccount sfAccount) {
        AccountDetailVO accountDetail = new AccountDetailVO(csAccountId,
                SolidFireUtil.ACCOUNT_ID,
                String.valueOf(sfAccount.getId()));

        _accountDetailsDao.persist(accountDetail);

        accountDetail = new AccountDetailVO(csAccountId,
                SolidFireUtil.CHAP_INITIATOR_USERNAME,
                String.valueOf(sfAccount.getName()));

        _accountDetailsDao.persist(accountDetail);

        accountDetail = new AccountDetailVO(csAccountId,
                SolidFireUtil.CHAP_INITIATOR_SECRET,
                String.valueOf(sfAccount.getInitiatorSecret()));

        _accountDetailsDao.persist(accountDetail);

        accountDetail = new AccountDetailVO(csAccountId,
                SolidFireUtil.CHAP_TARGET_USERNAME,
                sfAccount.getName());

        _accountDetailsDao.persist(accountDetail);

        accountDetail = new AccountDetailVO(csAccountId,
                SolidFireUtil.CHAP_TARGET_SECRET,
                sfAccount.getTargetSecret());

        _accountDetailsDao.persist(accountDetail);
    }

    private class ChapInfoImpl implements ChapInfo {
        private final String _initiatorUsername;
        private final String _initiatorSecret;
        private final String _targetUsername;
        private final String _targetSecret;

        public ChapInfoImpl(String initiatorUsername, String initiatorSecret, String targetUsername, String targetSecret) {
            _initiatorUsername = initiatorUsername;
            _initiatorSecret = initiatorSecret;
            _targetUsername = targetUsername;
            _targetSecret = targetSecret;
        }

        @Override
        public String getInitiatorUsername() {
            return _initiatorUsername;
        }

        @Override
        public String getInitiatorSecret() {
            return _initiatorSecret;
        }

        @Override
        public String getTargetUsername() {
            return _targetUsername;
        }

        @Override
        public String getTargetSecret() {
            return _targetSecret;
        }
    }

    @Override
    public ChapInfo getChapInfo(VolumeInfo volumeInfo) {
        return null;
    }

    /*
    @Override
    public ChapInfo getChapInfo(VolumeInfo volumeInfo) {
        long accountId = volumeInfo.getAccountId();

        AccountDetailVO accountDetail = _accountDetailsDao.findDetail(accountId, SolidFireUtil.CHAP_INITIATOR_USERNAME);

        String chapInitiatorUsername = accountDetail.getValue();

        accountDetail = _accountDetailsDao.findDetail(accountId, SolidFireUtil.CHAP_INITIATOR_SECRET);

        String chapInitiatorSecret = accountDetail.getValue();

        accountDetail = _accountDetailsDao.findDetail(accountId, SolidFireUtil.CHAP_TARGET_USERNAME);

        String chapTargetUsername = accountDetail.getValue();

        accountDetail = _accountDetailsDao.findDetail(accountId, SolidFireUtil.CHAP_TARGET_SECRET);

        String chapTargetSecret = accountDetail.getValue();

        return new ChapInfoImpl(chapInitiatorUsername, chapInitiatorSecret, chapTargetUsername, chapTargetSecret);
    }
    */

    // get the VAG associated with volumeInfo's cluster, if any (ListVolumeAccessGroups)
    // if the VAG exists
    //     update the VAG to contain all IQNs of the hosts (ModifyVolumeAccessGroup)
    //     if the ID of volumeInfo in not in the VAG, add it (ModifyVolumeAccessGroup)
    // if the VAG doesn't exist, create it with the IQNs of the hosts and the ID of volumeInfo (CreateVolumeAccessGroup)
    @Override
    public boolean connectVolumeToHost(VolumeInfo volumeInfo, Host host, DataStore dataStore)
    {
        if (volumeInfo == null || host == null || dataStore == null) {
            return false;
        }

        long sfVolumeId = Long.parseLong(volumeInfo.getFolder());
        long clusterId = host.getClusterId();
        long storagePoolId = dataStore.getId();

        ClusterDetailsVO clusterDetail = _clusterDetailsDao.findDetail(clusterId, getVagKey(storagePoolId));

        String vagId = clusterDetail != null ? clusterDetail.getValue() : null;

        List<HostVO> hosts = _hostDao.findByClusterId(clusterId);

        if (!hostsSupport_iScsi(hosts)) {
            return false;
        }

        SolidFireConnection sfConnection = getSolidFireConnection(storagePoolId);

        if (vagId != null) {
            SolidFireUtil.SolidFireVag sfVag = SolidFireUtil.getSolidFireVag(sfConnection.getManagementVip(), sfConnection.getManagementPort(),
                sfConnection.getClusterAdminUsername(), sfConnection.getClusterAdminPassword(), Long.parseLong(vagId));

            String[] hostIqns = getNewHostIqns(sfVag.getInitiators(), getIqnsFromHosts(hosts));
            long[] volumeIds = getNewVolumeIds(sfVag.getVolumeIds(), sfVolumeId, true);

            SolidFireUtil.modifySolidFireVag(sfConnection.getManagementVip(), sfConnection.getManagementPort(),
                sfConnection.getClusterAdminUsername(), sfConnection.getClusterAdminPassword(), sfVag.getId(),
                hostIqns, volumeIds);
        }
        else {
            long lVagId;

            try {
                lVagId = SolidFireUtil.createSolidFireVag(sfConnection.getManagementVip(), sfConnection.getManagementPort(),
                    sfConnection.getClusterAdminUsername(), sfConnection.getClusterAdminPassword(), "CloudStack-" + UUID.randomUUID().toString(),
                    getIqnsFromHosts(hosts), new long[] { sfVolumeId });
            }
            catch (Exception ex) {
                String iqnInVagAlready = "Exceeded maximum number of Volume Access Groups per initiator";

                if (!ex.getMessage().contains(iqnInVagAlready)) {
                    throw new CloudRuntimeException(ex.getMessage());
                }

                // getCompatibleVag throws an exception if an existing VAG can't be located
                SolidFireUtil.SolidFireVag sfVag = getCompatibleVag(hosts, sfConnection);

                long[] volumeIds = getNewVolumeIds(sfVag.getVolumeIds(), sfVolumeId, true);

                SolidFireUtil.modifySolidFireVag(sfConnection.getManagementVip(), sfConnection.getManagementPort(),
                    sfConnection.getClusterAdminUsername(), sfConnection.getClusterAdminPassword(), sfVag.getId(),
                    sfVag.getInitiators(), volumeIds);

                lVagId = sfVag.getId();
            }

            clusterDetail = new ClusterDetailsVO(clusterId, getVagKey(storagePoolId), String.valueOf(lVagId));

            _clusterDetailsDao.persist(clusterDetail);
        }

        return true;
    }

    // this method takes in a collection of hosts and tries to find an existing VAG that has all three of them in it
    // if successful, the VAG is returned; else, a CloudRuntimeException is thrown and this issue should be corrected by an admin
    private SolidFireUtil.SolidFireVag getCompatibleVag(List<HostVO> hosts, SolidFireConnection sfConnection) {
        List<SolidFireUtil.SolidFireVag> sfVags = SolidFireUtil.getAllSolidFireVags(sfConnection.getManagementVip(), sfConnection.getManagementPort(),
                sfConnection.getClusterAdminUsername(), sfConnection.getClusterAdminPassword());

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

    private List<String> getStringArrayAsLowerCaseStringList(String[] aString) {
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

    // get the VAG associated with volumeInfo's cluster, if any (ListVolumeAccessGroups) // might not exist if using CHAP
    // if the VAG exists
    //     remove the ID of volumeInfo from the VAG (ModifyVolumeAccessGroup)
    @Override
    public void disconnectVolumeFromHost(VolumeInfo volumeInfo, Host host, DataStore dataStore)
    {
        if (volumeInfo == null || host == null || dataStore == null) {
            return;
        }

        long sfVolumeId = Long.parseLong(volumeInfo.getFolder());
        long clusterId = host.getClusterId();
        long storagePoolId = dataStore.getId();

        ClusterDetailsVO clusterDetail = _clusterDetailsDao.findDetail(clusterId, getVagKey(storagePoolId));

        String vagId = clusterDetail != null ? clusterDetail.getValue() : null;

        if (vagId != null) {
            List<HostVO> hosts = _hostDao.findByClusterId(clusterId);

            SolidFireConnection sfConnection = getSolidFireConnection(storagePoolId);

            SolidFireUtil.SolidFireVag sfVag = SolidFireUtil.getSolidFireVag(sfConnection.getManagementVip(), sfConnection.getManagementPort(),
                sfConnection.getClusterAdminUsername(), sfConnection.getClusterAdminPassword(), Long.parseLong(vagId));

            String[] hostIqns = getNewHostIqns(sfVag.getInitiators(), getIqnsFromHosts(hosts));
            long[] volumeIds = getNewVolumeIds(sfVag.getVolumeIds(), sfVolumeId, false);

            SolidFireUtil.modifySolidFireVag(sfConnection.getManagementVip(), sfConnection.getManagementPort(),
                sfConnection.getClusterAdminUsername(), sfConnection.getClusterAdminPassword(), sfVag.getId(),
                hostIqns, volumeIds);
        }
    }

    private boolean hostsSupport_iScsi(List<HostVO> hosts) {
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

    private String[] getNewHostIqns(String[] currentIqns, String[] newIqns) {
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

    private long[] getNewVolumeIds(long[] volumeIds, long volumeIdToAddOrRemove, boolean add) {
        if (add) {
            return getNewVolumeIdsAdd(volumeIds, volumeIdToAddOrRemove);
        }

        return getNewVolumeIdsRemove(volumeIds, volumeIdToAddOrRemove);
    }

    private long[] getNewVolumeIdsAdd(long[] volumeIds, long volumeIdToAdd) {
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

    private long[] getNewVolumeIdsRemove(long[] volumeIds, long volumeIdToRemove) {
        List<Long> lstVolumeIds = new ArrayList<Long>();

        if (volumeIds != null) {
            for (long volumeId : volumeIds) {
                lstVolumeIds.add(volumeId);
            }
        }

        lstVolumeIds.remove(volumeIdToRemove);

        return convertArray(lstVolumeIds);
    }

    private long[] convertArray(List<Long> items) {
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

    private String getVagKey(long storagePoolId) {
        return "sfVolumeAccessGroup_" + storagePoolId;
    }

    private String[] getIqnsFromHosts(List<? extends Host> hosts) {
        if (hosts == null || hosts.size() == 0) {
            throw new CloudRuntimeException("There do not appear to be any hosts in this cluster.");
        }

        List<String> lstIqns = new ArrayList<String>();

        for (Host host : hosts) {
            lstIqns.add(host.getStorageUrl());
        }

        return lstIqns.toArray(new String[0]);
    }

    private long getDefaultMinIops(long storagePoolId) {
        StoragePoolDetailVO storagePoolDetail = _storagePoolDetailsDao.findDetail(storagePoolId, SolidFireUtil.CLUSTER_DEFAULT_MIN_IOPS);

        String clusterDefaultMinIops = storagePoolDetail.getValue();

        return Long.parseLong(clusterDefaultMinIops);
    }

    private long getDefaultMaxIops(long storagePoolId) {
        StoragePoolDetailVO storagePoolDetail = _storagePoolDetailsDao.findDetail(storagePoolId, SolidFireUtil.CLUSTER_DEFAULT_MAX_IOPS);

        String clusterDefaultMaxIops = storagePoolDetail.getValue();

        return Long.parseLong(clusterDefaultMaxIops);
    }

    private long getDefaultBurstIops(long storagePoolId, long maxIops) {
        StoragePoolDetailVO storagePoolDetail = _storagePoolDetailsDao.findDetail(storagePoolId, SolidFireUtil.CLUSTER_DEFAULT_BURST_IOPS_PERCENT_OF_MAX_IOPS);

        String clusterDefaultBurstIopsPercentOfMaxIops = storagePoolDetail.getValue();

        float fClusterDefaultBurstIopsPercentOfMaxIops = Float.parseFloat(clusterDefaultBurstIopsPercentOfMaxIops);

        return (long)(maxIops * fClusterDefaultBurstIopsPercentOfMaxIops);
    }

    private SolidFireUtil.SolidFireVolume createSolidFireVolume(VolumeInfo volumeInfo, SolidFireConnection sfConnection)
    {
        String mVip = sfConnection.getManagementVip();
        int mPort = sfConnection.getManagementPort();
        String clusterAdminUsername = sfConnection.getClusterAdminUsername();
        String clusterAdminPassword = sfConnection.getClusterAdminPassword();

        AccountDetailVO accountDetail = _accountDetailsDao.findDetail(volumeInfo.getAccountId(), SolidFireUtil.ACCOUNT_ID);
        long sfAccountId = Long.parseLong(accountDetail.getValue());

        long storagePoolId = volumeInfo.getDataStore().getId();

        final Iops iops;

        Long minIops = volumeInfo.getMinIops();
        Long maxIops = volumeInfo.getMaxIops();

        if (minIops == null || minIops <= 0 || maxIops == null || maxIops <= 0) {
            long defaultMaxIops = getDefaultMaxIops(storagePoolId);

            iops = new Iops(getDefaultMinIops(storagePoolId), defaultMaxIops, getDefaultBurstIops(storagePoolId, defaultMaxIops));
        }
        else {
            iops = new Iops(volumeInfo.getMinIops(), volumeInfo.getMaxIops(), getDefaultBurstIops(storagePoolId, volumeInfo.getMaxIops()));
        }

        long volumeSize = getVolumeSizeIncludingHypervisorSnapshotReserve(volumeInfo, _storagePoolDao.findById(storagePoolId));

        long sfVolumeId = SolidFireUtil.createSolidFireVolume(mVip, mPort, clusterAdminUsername, clusterAdminPassword,
                getSolidFireVolumeName(volumeInfo.getName()), sfAccountId, volumeSize, true,
                NumberFormat.getInstance().format(volumeInfo.getSize()),
                iops.getMinIops(), iops.getMaxIops(), iops.getBurstIops());

        return SolidFireUtil.getSolidFireVolume(mVip, mPort, clusterAdminUsername, clusterAdminPassword, sfVolumeId);
    }

    @Override
    public long getVolumeSizeIncludingHypervisorSnapshotReserve(Volume volume, StoragePool pool) {
        long volumeSize = volume.getSize();
        Integer hypervisorSnapshotReserve = volume.getHypervisorSnapshotReserve();

        if (hypervisorSnapshotReserve != null) {
            if (hypervisorSnapshotReserve < 25) {
                hypervisorSnapshotReserve = 25;
            }

            volumeSize += volumeSize * (hypervisorSnapshotReserve / 100f);
        }

        return volumeSize;
    }

    private String getSolidFireVolumeName(String strCloudStackVolumeName) {
        final String specialChar = "-";

        StringBuilder strSolidFireVolumeName = new StringBuilder();

        for (int i = 0; i < strCloudStackVolumeName.length(); i++) {
            String strChar = strCloudStackVolumeName.substring(i, i + 1);

            if (StringUtils.isAlphanumeric(strChar)) {
                strSolidFireVolumeName.append(strChar);
            }
            else {
                strSolidFireVolumeName.append(specialChar);
            }
        }

        return strSolidFireVolumeName.toString();
    }

    private static class Iops
    {
        private final long _minIops;
        private final long _maxIops;
        private final long _burstIops;

        public Iops(long minIops, long maxIops, long burstIops) throws IllegalArgumentException
        {
            if (minIops <= 0 || maxIops <= 0) {
                throw new IllegalArgumentException("The 'Min IOPS' and 'Max IOPS' values must be greater than 0.");
            }

            if (minIops > maxIops) {
                throw new IllegalArgumentException("The 'Min IOPS' value cannot exceed the 'Max IOPS' value.");
            }

            if (maxIops > burstIops) {
                throw new IllegalArgumentException("The 'Max IOPS' value cannot exceed the 'Burst IOPS' value.");
            }

            _minIops = minIops;
            _maxIops = maxIops;
            _burstIops = burstIops;
        }

        public long getMinIops()
        {
            return _minIops;
        }

        public long getMaxIops()
        {
            return _maxIops;
        }

        public long getBurstIops()
        {
            return _burstIops;
        }
    }

    private SolidFireUtil.SolidFireVolume deleteSolidFireVolume(VolumeInfo volumeInfo, SolidFireConnection sfConnection)
    {
        Long storagePoolId = volumeInfo.getPoolId();

        if (storagePoolId == null) {
            return null; // this volume was never assigned to a storage pool, so no SAN volume should exist for it
        }

        String mVip = sfConnection.getManagementVip();
        int mPort = sfConnection.getManagementPort();
        String clusterAdminUsername = sfConnection.getClusterAdminUsername();
        String clusterAdminPassword = sfConnection.getClusterAdminPassword();

        long sfVolumeId = Long.parseLong(volumeInfo.getFolder());

        return SolidFireUtil.deleteSolidFireVolume(mVip, mPort, clusterAdminUsername, clusterAdminPassword, sfVolumeId);
    }

    private String getSfAccountName(String csAccountUuid, long csAccountId) {
        return "CloudStack_" + csAccountUuid + "_" + csAccountId;
    }

    private boolean sfAccountExists(String sfAccountName, SolidFireConnection sfConnection) {
        String mVip = sfConnection.getManagementVip();
        int mPort = sfConnection.getManagementPort();
        String clusterAdminUsername = sfConnection.getClusterAdminUsername();
        String clusterAdminPassword = sfConnection.getClusterAdminPassword();

        try {
            SolidFireUtil.getSolidFireAccountByName(mVip, mPort, clusterAdminUsername, clusterAdminPassword, sfAccountName);
        }
        catch (Exception ex) {
            return false;
        }

        return true;
    }

    @Override
    public void createAsync(DataStore dataStore, DataObject dataObject,
            AsyncCompletionCallback<CreateCmdResult> callback) {
        String iqn = null;
        String errMsg = null;

        if (dataObject.getType() == DataObjectType.VOLUME) {
            VolumeInfo volumeInfo = (VolumeInfo)dataObject;
            AccountVO account = _accountDao.findById(volumeInfo.getAccountId());
            String sfAccountName = getSfAccountName(account.getUuid(), account.getAccountId());

            long storagePoolId = dataStore.getId();
            SolidFireConnection sfConnection = getSolidFireConnection(storagePoolId);

            if (!sfAccountExists(sfAccountName, sfConnection)) {
                SolidFireUtil.SolidFireAccount sfAccount = createSolidFireAccount(sfAccountName, sfConnection);

                updateCsDbWithAccountInfo(account.getId(), sfAccount);
            }

            SolidFireUtil.SolidFireVolume sfVolume = createSolidFireVolume(volumeInfo, sfConnection);

            iqn = sfVolume.getIqn();

            VolumeVO volume = this._volumeDao.findById(volumeInfo.getId());

            volume.set_iScsiName(iqn);
            volume.setFolder(String.valueOf(sfVolume.getId()));
            volume.setPoolType(StoragePoolType.IscsiLUN);
            volume.setPoolId(storagePoolId);

            _volumeDao.update(volume.getId(), volume);

            StoragePoolVO storagePool = _storagePoolDao.findById(dataStore.getId());

            long capacityBytes = storagePool.getCapacityBytes();
            long usedBytes = storagePool.getUsedBytes();

            usedBytes += sfVolume.getTotalSize();

            storagePool.setUsedBytes(usedBytes > capacityBytes ? capacityBytes : usedBytes);

            _storagePoolDao.update(storagePoolId, storagePool);
        }
        else {
            errMsg = "Invalid DataObjectType (" + dataObject.getType() + ") passed to createAsync";
        }

        // path = iqn
        // size is pulled from DataObject instance, if errMsg is null
        CreateCmdResult result = new CreateCmdResult(iqn, new Answer(null, errMsg == null, errMsg));

        result.setResult(errMsg);

        callback.complete(result);
    }

    /*
    private void deleteSolidFireAccount(long sfAccountId, SolidFireConnection sfConnection) {
        String mVip = sfConnection.getManagementVip();
        int mPort = sfConnection.getManagementPort();
        String clusterAdminUsername = sfConnection.getClusterAdminUsername();
        String clusterAdminPassword = sfConnection.getClusterAdminPassword();

        List<SolidFireUtil.SolidFireVolume> sfVolumes = SolidFireUtil.getDeletedVolumes(mVip, mPort,
                clusterAdminUsername, clusterAdminPassword);

        // if there are volumes for this account in the trash, delete them (so the account can be deleted)
        if (sfVolumes != null) {
            for (SolidFireUtil.SolidFireVolume sfVolume : sfVolumes) {
                if (sfVolume.getAccountId() == sfAccountId) {
                    SolidFireUtil.purgeSolidFireVolume(mVip, mPort, clusterAdminUsername, clusterAdminPassword, sfVolume.getId());
                }
            }
        }

        SolidFireUtil.deleteSolidFireAccount(mVip, mPort, clusterAdminUsername, clusterAdminPassword, sfAccountId);
    }

    private boolean sfAccountHasVolume(long sfAccountId, SolidFireConnection sfConnection) {
        String mVip = sfConnection.getManagementVip();
        int mPort = sfConnection.getManagementPort();
        String clusterAdminUsername = sfConnection.getClusterAdminUsername();
        String clusterAdminPassword = sfConnection.getClusterAdminPassword();

        List<SolidFireUtil.SolidFireVolume> sfVolumes = SolidFireUtil.getSolidFireVolumesForAccountId(mVip, mPort,
                clusterAdminUsername, clusterAdminPassword, sfAccountId);

        if (sfVolumes != null) {
            for (SolidFireUtil.SolidFireVolume sfVolume : sfVolumes) {
                if (sfVolume.isActive()) {
                    return true;
                }
            }
        }

        return false;
    }
    */

    @Override
    public void deleteAsync(DataStore dataStore, DataObject dataObject, AsyncCompletionCallback<CommandResult> callback) {
        String errMsg = null;

        if (dataObject.getType() == DataObjectType.VOLUME) {
            VolumeInfo volumeInfo = (VolumeInfo)dataObject;
            // AccountVO account = _accountDao.findById(volumeInfo.getAccountId());
            // AccountDetailVO accountDetails = _accountDetailsDao.findDetail(account.getAccountId(), SolidFireUtil.ACCOUNT_ID);
            // long sfAccountId = Long.parseLong(accountDetails.getValue());

            long storagePoolId = dataStore.getId();
            SolidFireConnection sfConnection = getSolidFireConnection(storagePoolId);

            SolidFireUtil.SolidFireVolume sfVolume = deleteSolidFireVolume(volumeInfo, sfConnection);

            //  if (!sfAccountHasVolume(sfAccountId, sfConnection)) {
            //      // delete the account from the SolidFire SAN
            //      deleteSolidFireAccount(sfAccountId, sfConnection);
            //
            //      // delete the info in the account_details table
            //      // that's related to the SolidFire account
            //      _accountDetailsDao.deleteDetails(account.getAccountId());
            //  }

            StoragePoolVO storagePool = _storagePoolDao.findById(storagePoolId);

            long usedBytes = storagePool.getUsedBytes();

            usedBytes -= sfVolume != null ? sfVolume.getTotalSize() : 0;

            storagePool.setUsedBytes(usedBytes < 0 ? 0 : usedBytes);

            _storagePoolDao.update(storagePoolId, storagePool);
        }
        else {
            errMsg = "Invalid DataObjectType (" + dataObject.getType() + ") passed to deleteAsync";
        }

        CommandResult result = new CommandResult();

        result.setResult(errMsg);

        callback.complete(result);
    }

    @Override
    public void copyAsync(DataObject srcdata, DataObject destData, AsyncCompletionCallback<CopyCommandResult> callback) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean canCopy(DataObject srcData, DataObject destData) {
        return false;
    }

    @Override
    public void revertSnapshot(SnapshotInfo snapshot, AsyncCompletionCallback<CommandResult> callback) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void resize(DataObject data, AsyncCompletionCallback<CreateCmdResult> callback) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void takeSnapshot(SnapshotInfo snapshot, AsyncCompletionCallback<CreateCmdResult> callback) {
        throw new UnsupportedOperationException();
    }
}
