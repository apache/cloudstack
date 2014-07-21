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
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.ChapInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.CopyCommandResult;
import org.apache.cloudstack.engine.subsystem.api.storage.CreateCmdResult;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreDriver;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.storage.command.CommandResult;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailVO;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailsDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.datastore.util.SolidFireUtil;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.to.DataObjectType;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.agent.api.to.DataTO;
import com.cloud.capacity.CapacityManager;
import com.cloud.dc.ClusterDetailsVO;
import com.cloud.dc.ClusterDetailsDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.ResizeVolumePayload;
import com.cloud.storage.StoragePool;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.user.AccountDetailVO;
import com.cloud.user.AccountDetailsDao;
import com.cloud.user.AccountVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.exception.CloudRuntimeException;

public class SolidFirePrimaryDataStoreDriver implements PrimaryDataStoreDriver {
    @Inject private AccountDao _accountDao;
    @Inject private AccountDetailsDao _accountDetailsDao;
    @Inject private CapacityManager _capacityMgr;
    @Inject private ClusterDetailsDao _clusterDetailsDao;
    @Inject private DataCenterDao _zoneDao;
    @Inject private HostDao _hostDao;
    @Inject private PrimaryDataStoreDao _storagePoolDao;
    @Inject private StoragePoolDetailsDao _storagePoolDetailsDao;
    @Inject private VolumeDao _volumeDao;

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

    private SolidFireUtil.SolidFireAccount createSolidFireAccount(SolidFireUtil.SolidFireConnection sfConnection, String sfAccountName) {
        long accountNumber = SolidFireUtil.createSolidFireAccount(sfConnection, sfAccountName);

        return SolidFireUtil.getSolidFireAccountById(sfConnection, accountNumber);
    }

    @Override
    public ChapInfo getChapInfo(VolumeInfo volumeInfo) {
        return null;
    }

    // get the VAG associated with volumeInfo's cluster, if any (ListVolumeAccessGroups)
    // if the VAG exists
    //     update the VAG to contain all IQNs of the hosts (ModifyVolumeAccessGroup)
    //     if the ID of volumeInfo in not in the VAG, add it (ModifyVolumeAccessGroup)
    // if the VAG doesn't exist, create it with the IQNs of the hosts and the ID of volumeInfo (CreateVolumeAccessGroup)
    @Override
    public synchronized boolean connectVolumeToHost(VolumeInfo volumeInfo, Host host, DataStore dataStore)
    {
        if (volumeInfo == null || host == null || dataStore == null) {
            return false;
        }

        long sfVolumeId = Long.parseLong(volumeInfo.getFolder());
        long clusterId = host.getClusterId();
        long storagePoolId = dataStore.getId();

        ClusterDetailsVO clusterDetail = _clusterDetailsDao.findDetail(clusterId, SolidFireUtil.getVagKey(storagePoolId));

        String vagId = clusterDetail != null ? clusterDetail.getValue() : null;

        List<HostVO> hosts = _hostDao.findByClusterId(clusterId);

        if (!SolidFireUtil.hostsSupport_iScsi(hosts)) {
            return false;
        }

        SolidFireUtil.SolidFireConnection sfConnection = SolidFireUtil.getSolidFireConnection(storagePoolId, _storagePoolDetailsDao);

        if (vagId != null) {
            SolidFireUtil.SolidFireVag sfVag = SolidFireUtil.getSolidFireVag(sfConnection, Long.parseLong(vagId));

            String[] hostIqns = SolidFireUtil.getNewHostIqns(sfVag.getInitiators(), SolidFireUtil.getIqnsFromHosts(hosts));
            long[] volumeIds = SolidFireUtil.getNewVolumeIds(sfVag.getVolumeIds(), sfVolumeId, true);

            SolidFireUtil.modifySolidFireVag(sfConnection, sfVag.getId(), hostIqns, volumeIds);
        }
        else {
            SolidFireUtil.placeVolumeInVolumeAccessGroup(sfConnection, sfVolumeId, storagePoolId, hosts, _clusterDetailsDao);
        }

        return true;
    }

    // get the VAG associated with volumeInfo's cluster, if any (ListVolumeAccessGroups) // might not exist if using CHAP
    // if the VAG exists
    //     remove the ID of volumeInfo from the VAG (ModifyVolumeAccessGroup)
    @Override
    public synchronized void disconnectVolumeFromHost(VolumeInfo volumeInfo, Host host, DataStore dataStore)
    {
        if (volumeInfo == null || host == null || dataStore == null) {
            return;
        }

        long sfVolumeId = Long.parseLong(volumeInfo.getFolder());
        long clusterId = host.getClusterId();
        long storagePoolId = dataStore.getId();

        ClusterDetailsVO clusterDetail = _clusterDetailsDao.findDetail(clusterId, SolidFireUtil.getVagKey(storagePoolId));

        String vagId = clusterDetail != null ? clusterDetail.getValue() : null;

        if (vagId != null) {
            List<HostVO> hosts = _hostDao.findByClusterId(clusterId);

            SolidFireUtil.SolidFireConnection sfConnection = SolidFireUtil.getSolidFireConnection(storagePoolId, _storagePoolDetailsDao);

            SolidFireUtil.SolidFireVag sfVag = SolidFireUtil.getSolidFireVag(sfConnection, Long.parseLong(vagId));

            String[] hostIqns = SolidFireUtil.getNewHostIqns(sfVag.getInitiators(), SolidFireUtil.getIqnsFromHosts(hosts));
            long[] volumeIds = SolidFireUtil.getNewVolumeIds(sfVag.getVolumeIds(), sfVolumeId, false);

            SolidFireUtil.modifySolidFireVag(sfConnection, sfVag.getId(), hostIqns, volumeIds);
        }
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

    private SolidFireUtil.SolidFireVolume createSolidFireVolume(SolidFireUtil.SolidFireConnection sfConnection, VolumeInfo volumeInfo, long sfAccountId) {
        long storagePoolId = volumeInfo.getDataStore().getId();

        final Iops iops;

        Long minIops = volumeInfo.getMinIops();
        Long maxIops = volumeInfo.getMaxIops();

        if (minIops == null || minIops <= 0 || maxIops == null || maxIops <= 0) {
            long defaultMaxIops = getDefaultMaxIops(storagePoolId);

            iops = new Iops(getDefaultMinIops(storagePoolId), defaultMaxIops, getDefaultBurstIops(storagePoolId, defaultMaxIops));
        } else {
            iops = new Iops(volumeInfo.getMinIops(), volumeInfo.getMaxIops(), getDefaultBurstIops(storagePoolId, volumeInfo.getMaxIops()));
        }

        long volumeSize = getVolumeSizeIncludingHypervisorSnapshotReserve(volumeInfo, _storagePoolDao.findById(storagePoolId));

        long sfVolumeId = SolidFireUtil.createSolidFireVolume(sfConnection, SolidFireUtil.getSolidFireVolumeName(volumeInfo.getName()), sfAccountId, volumeSize, true,
                NumberFormat.getInstance().format(volumeInfo.getSize()), iops.getMinIops(), iops.getMaxIops(), iops.getBurstIops());

        return SolidFireUtil.getSolidFireVolume(sfConnection, sfVolumeId);
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

    private static class Iops {
        private final long _minIops;
        private final long _maxIops;
        private final long _burstIops;

        public Iops(long minIops, long maxIops, long burstIops) throws IllegalArgumentException {
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

        public long getMinIops() {
            return _minIops;
        }

        public long getMaxIops() {
            return _maxIops;
        }

        public long getBurstIops() {
            return _burstIops;
        }
    }

    private SolidFireUtil.SolidFireVolume deleteSolidFireVolume(SolidFireUtil.SolidFireConnection sfConnection, VolumeInfo volumeInfo)
    {
        Long storagePoolId = volumeInfo.getPoolId();

        if (storagePoolId == null) {
            return null; // this volume was never assigned to a storage pool, so no SAN volume should exist for it
        }

        long sfVolumeId = Long.parseLong(volumeInfo.getFolder());

        return SolidFireUtil.deleteSolidFireVolume(sfConnection, sfVolumeId);
    }

    @Override
    public void createAsync(DataStore dataStore, DataObject dataObject, AsyncCompletionCallback<CreateCmdResult> callback) {
        String iqn = null;
        String errMsg = null;

        if (dataObject.getType() == DataObjectType.VOLUME) {
            VolumeInfo volumeInfo = (VolumeInfo)dataObject;
            AccountVO account = _accountDao.findById(volumeInfo.getAccountId());
            String sfAccountName = SolidFireUtil.getSolidFireAccountName(account.getUuid(), account.getAccountId());

            long storagePoolId = dataStore.getId();

            SolidFireUtil.SolidFireConnection sfConnection = SolidFireUtil.getSolidFireConnection(storagePoolId, _storagePoolDetailsDao);

            AccountDetailVO accountDetail = SolidFireUtil.getAccountDetail(volumeInfo.getAccountId(), storagePoolId, _accountDetailsDao);

            if (accountDetail == null || accountDetail.getValue() == null) {
                SolidFireUtil.SolidFireAccount sfAccount = SolidFireUtil.getSolidFireAccount(sfConnection, sfAccountName);

                if (sfAccount == null) {
                    sfAccount = createSolidFireAccount(sfConnection, sfAccountName);
                }

                SolidFireUtil.updateCsDbWithSolidFireAccountInfo(account.getId(), sfAccount, storagePoolId, _accountDetailsDao);

                accountDetail = SolidFireUtil.getAccountDetail(volumeInfo.getAccountId(), storagePoolId, _accountDetailsDao);
            }

            long sfAccountId = Long.parseLong(accountDetail.getValue());

            SolidFireUtil.SolidFireVolume sfVolume = createSolidFireVolume(sfConnection, volumeInfo, sfAccountId);

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
        } else {
            errMsg = "Invalid DataObjectType (" + dataObject.getType() + ") passed to createAsync";
        }

        // path = iqn
        // size is pulled from DataObject instance, if errMsg is null
        CreateCmdResult result = new CreateCmdResult(iqn, new Answer(null, errMsg == null, errMsg));

        result.setResult(errMsg);

        callback.complete(result);
    }

    @Override
    public void deleteAsync(DataStore dataStore, DataObject dataObject, AsyncCompletionCallback<CommandResult> callback) {
        String errMsg = null;

        if (dataObject.getType() == DataObjectType.VOLUME) {
            VolumeInfo volumeInfo = (VolumeInfo)dataObject;

            long storagePoolId = dataStore.getId();
            SolidFireUtil.SolidFireConnection sfConnection = SolidFireUtil.getSolidFireConnection(storagePoolId, _storagePoolDetailsDao);

            SolidFireUtil.SolidFireVolume sfVolume = deleteSolidFireVolume(sfConnection, volumeInfo);

            _volumeDao.deleteVolumesByInstance(volumeInfo.getId());

            StoragePoolVO storagePool = _storagePoolDao.findById(storagePoolId);

            long usedBytes = storagePool.getUsedBytes();

            usedBytes -= sfVolume != null ? sfVolume.getTotalSize() : 0;

            storagePool.setUsedBytes(usedBytes < 0 ? 0 : usedBytes);

            _storagePoolDao.update(storagePoolId, storagePool);
        } else {
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
    public void resize(DataObject dataObject, AsyncCompletionCallback<CreateCmdResult> callback) {
        String iqn = null;
        String errMsg = null;

        if (dataObject.getType() == DataObjectType.VOLUME) {
            VolumeInfo volumeInfo = (VolumeInfo)dataObject;
            iqn = volumeInfo.get_iScsiName();
            long storagePoolId = volumeInfo.getPoolId();
            long sfVolumeId = Long.parseLong(volumeInfo.getFolder());
            ResizeVolumePayload payload = (ResizeVolumePayload)volumeInfo.getpayload();

            SolidFireUtil.SolidFireConnection sfConnection = SolidFireUtil.getSolidFireConnection(storagePoolId, _storagePoolDetailsDao);
            SolidFireUtil.SolidFireVolume sfVolume = SolidFireUtil.getSolidFireVolume(sfConnection, sfVolumeId);

            verifySufficientIopsForStoragePool(storagePoolId, volumeInfo.getId(), payload.newMinIops);

            SolidFireUtil.modifySolidFireVolume(sfConnection, sfVolumeId, sfVolume.getTotalSize(), payload.newMinIops, payload.newMaxIops,
                    getDefaultBurstIops(storagePoolId, payload.newMaxIops));

            VolumeVO volume = _volumeDao.findById(volumeInfo.getId());

            volume.setMinIops(payload.newMinIops);
            volume.setMaxIops(payload.newMaxIops);

            _volumeDao.update(volume.getId(), volume);
        } else {
            errMsg = "Invalid DataObjectType (" + dataObject.getType() + ") passed to resize";
        }

        CreateCmdResult result = new CreateCmdResult(iqn, new Answer(null, errMsg == null, errMsg));

        result.setResult(errMsg);

        callback.complete(result);
    }

    private void verifySufficientIopsForStoragePool(long storagePoolId, long volumeId, long newMinIops) {
        StoragePoolVO storagePool = _storagePoolDao.findById(storagePoolId);
        VolumeVO volume = _volumeDao.findById(volumeId);

        long currentMinIops = volume.getMinIops();
        long diffInMinIops = newMinIops - currentMinIops;

        // if the desire is for more IOPS
        if (diffInMinIops > 0) {
            long usedIops = _capacityMgr.getUsedIops(storagePool);
            long capacityIops = storagePool.getCapacityIops();

            if (usedIops + diffInMinIops > capacityIops) {
                throw new CloudRuntimeException("Insufficient number of IOPS available in this storage pool");
            }
        }
    }

    @Override
    public void takeSnapshot(SnapshotInfo snapshot, AsyncCompletionCallback<CreateCmdResult> callback) {
        throw new UnsupportedOperationException();
    }
}
