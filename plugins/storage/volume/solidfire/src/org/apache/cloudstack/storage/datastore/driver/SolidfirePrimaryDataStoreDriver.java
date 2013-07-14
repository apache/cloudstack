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

import java.util.List;

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.*;
import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.storage.command.CommandResult;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailsDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailVO;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.datastore.util.SolidFireUtil;
import org.apache.commons.lang.StringUtils;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.to.DataObjectType;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.agent.api.to.DataTO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.dao.VolumeDetailsDao;
import com.cloud.user.AccountVO;
import com.cloud.user.AccountDetailsDao;
import com.cloud.user.AccountDetailVO;
import com.cloud.user.dao.AccountDao;

public class SolidfirePrimaryDataStoreDriver implements PrimaryDataStoreDriver {
    @Inject private PrimaryDataStoreDao _storagePoolDao;
    @Inject private StoragePoolDetailsDao _storagePoolDetailsDao;
    @Inject private VolumeDao _volumeDao;
    @Inject private VolumeDetailsDao _volumeDetailsDao;
    @Inject private DataCenterDao _zoneDao;
    @Inject private AccountDao _accountDao;
    @Inject private AccountDetailsDao _accountDetailsDao;

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

    private SolidFireUtil.SolidFireAccount createSolidFireAccount(String sfAccountName,
            SolidFireConnection sfConnection) {
        String mVip = sfConnection.getManagementVip();
        int mPort = sfConnection.getManagementPort();
        String clusterAdminUsername = sfConnection.getClusterAdminUsername();
        String clusterAdminPassword = sfConnection.getClusterAdminPassword();

        long accountNumber = SolidFireUtil.createSolidFireAccount(mVip, mPort,
            clusterAdminUsername, clusterAdminPassword, sfAccountName);

        return SolidFireUtil.getSolidFireAccountById(mVip, mPort,
            clusterAdminUsername, clusterAdminPassword, accountNumber);
    }

    private void updateCsDbWithAccountInfo(long csAccountId, SolidFireUtil.SolidFireAccount sfAccount) {
        AccountDetailVO accountDetails = new AccountDetailVO(csAccountId,
                SolidFireUtil.ACCOUNT_ID,
                String.valueOf(sfAccount.getId()));

        _accountDetailsDao.persist(accountDetails);

        accountDetails = new AccountDetailVO(csAccountId,
                SolidFireUtil.CHAP_INITIATOR_USERNAME,
                String.valueOf(sfAccount.getName()));

        _accountDetailsDao.persist(accountDetails);

        accountDetails = new AccountDetailVO(csAccountId,
                SolidFireUtil.CHAP_INITIATOR_SECRET,
                String.valueOf(sfAccount.getInitiatorSecret()));

        _accountDetailsDao.persist(accountDetails);

        accountDetails = new AccountDetailVO(csAccountId,
                SolidFireUtil.CHAP_TARGET_USERNAME,
                sfAccount.getName());

        _accountDetailsDao.persist(accountDetails);

        accountDetails = new AccountDetailVO(csAccountId,
                SolidFireUtil.CHAP_TARGET_SECRET,
                sfAccount.getTargetSecret());

        _accountDetailsDao.persist(accountDetails);
    }

    private class ChapInfoImpl implements ChapInfo {
        private final String _initiatorUsername;
        private final String _initiatorSecret;
        private final String _targetUsername;
        private final String _targetSecret;

        public ChapInfoImpl(String initiatorUsername, String initiatorSecret,
                String targetUsername, String targetSecret) {
            _initiatorUsername = initiatorUsername;
            _initiatorSecret = initiatorSecret;
            _targetUsername = targetUsername;
            _targetSecret = targetSecret;
        }

        public String getInitiatorUsername() {
            return _initiatorUsername;
        }

        public String getInitiatorSecret() {
            return _initiatorSecret;
        }

        public String getTargetUsername() {
            return _targetUsername;
        }

        public String getTargetSecret() {
            return _targetSecret;
        }
    }

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

        return new ChapInfoImpl(chapInitiatorUsername, chapInitiatorSecret,
                chapTargetUsername, chapTargetSecret);
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

        if (minIops == null || minIops <= 0 ||
            maxIops == null || maxIops <= 0) {
            long defaultMaxIops = getDefaultMaxIops(storagePoolId);

            iops = new Iops(getDefaultMinIops(storagePoolId), defaultMaxIops, getDefaultBurstIops(storagePoolId, defaultMaxIops));
        }
        else {
            iops = new Iops(volumeInfo.getMinIops(), volumeInfo.getMaxIops(), getDefaultBurstIops(storagePoolId, volumeInfo.getMaxIops()));
        }

        long sfVolumeId = SolidFireUtil.createSolidFireVolume(mVip, mPort, clusterAdminUsername, clusterAdminPassword,
                getSolidFireVolumeName(volumeInfo.getName()), sfAccountId, volumeInfo.getSize(), true,
                iops.getMinIops(), iops.getMaxIops(), iops.getBurstIops());

        return SolidFireUtil.getSolidFireVolume(mVip, mPort, clusterAdminUsername, clusterAdminPassword, sfVolumeId);
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

    private void deleteSolidFireVolume(VolumeInfo volumeInfo, SolidFireConnection sfConnection)
    {
        Long storagePoolId = volumeInfo.getPoolId();

        if (storagePoolId == null) {
            return; // this volume was never assigned to a storage pool, so no SAN volume should exist for it
        }

        String mVip = sfConnection.getManagementVip();
        int mPort = sfConnection.getManagementPort();
        String clusterAdminUsername = sfConnection.getClusterAdminUsername();
        String clusterAdminPassword = sfConnection.getClusterAdminPassword();

        long sfVolumeId = Long.parseLong(volumeInfo.getFolder());

        SolidFireUtil.deleteSolidFireVolume(mVip, mPort, clusterAdminUsername, clusterAdminPassword, sfVolumeId);
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
                SolidFireUtil.SolidFireAccount sfAccount = createSolidFireAccount(sfAccountName,
                        sfConnection);

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

            usedBytes += volumeInfo.getSize();

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

    @Override
    public void deleteAsync(DataStore dataStore, DataObject dataObject,
            AsyncCompletionCallback<CommandResult> callback) {
        String errMsg = null;

        if (dataObject.getType() == DataObjectType.VOLUME) {
            VolumeInfo volumeInfo = (VolumeInfo)dataObject;
            AccountVO account = _accountDao.findById(volumeInfo.getAccountId());
            AccountDetailVO accountDetails = _accountDetailsDao.findDetail(account.getAccountId(), SolidFireUtil.ACCOUNT_ID);
            long sfAccountId = Long.parseLong(accountDetails.getValue());

            long storagePoolId = dataStore.getId();
            SolidFireConnection sfConnection = getSolidFireConnection(storagePoolId);

            deleteSolidFireVolume(volumeInfo, sfConnection);

            _volumeDao.deleteVolumesByInstance(volumeInfo.getId());

//            if (!sfAccountHasVolume(sfAccountId, sfConnection)) {
//                // delete the account from the SolidFire SAN
//                deleteSolidFireAccount(sfAccountId, sfConnection);
//
//                // delete the info in the account_details table
//                // that's related to the SolidFire account
//                _accountDetailsDao.deleteDetails(account.getAccountId());
//            }

            StoragePoolVO storagePool = _storagePoolDao.findById(storagePoolId);

            long usedBytes = storagePool.getUsedBytes();

            usedBytes -= volumeInfo.getSize();

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
