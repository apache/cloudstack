/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.storage.motion;

import java.util.Map;

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.ClusterScope;
import org.apache.cloudstack.engine.subsystem.api.storage.CopyCommandResult;
import org.apache.cloudstack.engine.subsystem.api.storage.DataMotionStrategy;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPoint;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPointSelector;
import org.apache.cloudstack.engine.subsystem.api.storage.HostScope;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine.Event;
import org.apache.cloudstack.engine.subsystem.api.storage.Scope;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.StorageCacheManager;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.ZoneScope;
import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.storage.command.CopyCommand;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.VolumeDataStoreDao;
import org.apache.cloudstack.storage.image.datastore.ImageStoreEntity;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.to.DataObjectType;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.agent.api.to.DataTO;
import com.cloud.agent.api.to.NfsTO;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.configuration.Config;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.host.Host;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.server.ManagementService;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.StorageManager;
import com.cloud.storage.StoragePool;
import com.cloud.storage.VolumeManager;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VMTemplatePoolDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.snapshot.SnapshotManager;
import com.cloud.template.TemplateManager;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.db.DB;
import com.cloud.utils.exception.CloudRuntimeException;

@Component
public class AncientDataMotionStrategy implements DataMotionStrategy {
    private static final Logger s_logger = Logger.getLogger(AncientDataMotionStrategy.class);
    @Inject
    EndPointSelector selector;
    @Inject
    TemplateManager templateMgr;
    @Inject
    VolumeDataStoreDao volumeStoreDao;
    @Inject
    HostDao hostDao;
    @Inject
    ConfigurationDao configDao;
    @Inject
    StorageManager storageMgr;
    @Inject
    VolumeDao volDao;
    @Inject
    VMTemplateDao templateDao;
    @Inject
    SnapshotManager snapshotMgr;
    @Inject
    SnapshotDao snapshotDao;
    @Inject
    SnapshotDataStoreDao _snapshotStoreDao;
    @Inject
    PrimaryDataStoreDao primaryDataStoreDao;
    @Inject
    DataStoreManager dataStoreMgr;
    @Inject
    TemplateDataStoreDao templateStoreDao;
    @Inject
    DiskOfferingDao diskOfferingDao;
    @Inject
    VMTemplatePoolDao templatePoolDao;
    @Inject
    VolumeManager volumeMgr;
    @Inject
    StorageCacheManager cacheMgr;
    @Inject
    ManagementService _mgmtServer;

    @Override
    public boolean canHandle(DataObject srcData, DataObject destData) {
        // TODO Auto-generated method stub
        return true;
    }

    @Override
    public boolean canHandle(Map<VolumeInfo, DataStore> volumeMap, Host srcHost, Host destHost) {
        return false;
    }

    protected boolean needCacheStorage(DataObject srcData, DataObject destData) {
        DataTO srcTO = srcData.getTO();
        DataTO destTO = destData.getTO();
        DataStoreTO srcStoreTO = srcTO.getDataStore();
        DataStoreTO destStoreTO = destTO.getDataStore();
        if (srcStoreTO instanceof NfsTO || srcStoreTO.getRole() == DataStoreRole.ImageCache) {
            return false;
        }

        if (destStoreTO instanceof NfsTO || destStoreTO.getRole() == DataStoreRole.ImageCache) {
            return false;
        }

        if (srcData.getType() == DataObjectType.TEMPLATE) {
            TemplateInfo template = (TemplateInfo)srcData;
            if (template.getHypervisorType() == HypervisorType.Hyperv) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("needCacheStorage false due to src TemplateInfo, which is DataObjectType.TEMPLATE of HypervisorType.Hyperv");
                }
                return false;
            }
        }

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("needCacheStorage true, dest at " +
                    destTO.getPath() + " dest role " + destStoreTO.getRole().toString() +
                    srcTO.getPath() + " src role " + srcStoreTO.getRole().toString() );
        }
        return true;
    }

    private Scope getZoneScope(Scope destScope) {
        ZoneScope zoneScope = null;
        if (destScope instanceof ClusterScope) {
            ClusterScope clusterScope = (ClusterScope) destScope;
            zoneScope = new ZoneScope(clusterScope.getZoneId());
        } else if (destScope instanceof HostScope) {
            HostScope hostScope = (HostScope) destScope;
            zoneScope = new ZoneScope(hostScope.getZoneId());
        } else {
            zoneScope = (ZoneScope) destScope;
        }
        return zoneScope;
    }

    protected Answer copyObject(DataObject srcData, DataObject destData) {
        String value = configDao.getValue(Config.PrimaryStorageDownloadWait.toString());
        int _primaryStorageDownloadWait = NumbersUtil.parseInt(value,
                Integer.parseInt(Config.PrimaryStorageDownloadWait.getDefaultValue()));
        Answer answer = null;
        boolean usingCache = false;
        DataObject cacheData = null;
        DataObject srcForCopy = srcData;
        try {
            if (needCacheStorage(srcData, destData)) {
                Scope destScope = getZoneScope(destData.getDataStore().getScope());
                srcForCopy = cacheData = cacheMgr.createCacheObject(srcData, destScope);
            }

            CopyCommand cmd = new CopyCommand(srcForCopy.getTO(), destData.getTO(), _primaryStorageDownloadWait, _mgmtServer.getExecuteInSequence());
            EndPoint ep = selector.select(srcForCopy, destData);
            answer = ep.sendMessage(cmd);

            if (cacheData != null) {
                if (answer == null || !answer.getResult()) {
                    cacheMgr.deleteCacheObject(srcForCopy);
                } else {
                    cacheMgr.releaseCacheObject(srcForCopy);
                }
            }
            return answer;
        } catch (Exception e) {
            s_logger.debug("copy object failed: " + e.toString());
            if (cacheData != null) {
                cacheMgr.deleteCacheObject(cacheData);
            }
            throw new CloudRuntimeException(e.toString());
        }
    }

    protected DataObject cacheSnapshotChain(SnapshotInfo snapshot) {
        DataObject leafData = null;
        DataStore store = cacheMgr.getCacheStorage(snapshot.getDataStore().getScope());
        while (snapshot != null) {
            DataObject cacheData = cacheMgr.createCacheObject(snapshot, store);
            if (leafData == null) {
                leafData = cacheData;
            }
            snapshot = snapshot.getParent();
        }
        return leafData;
    }

    protected void deleteSnapshotCacheChain(SnapshotInfo snapshot) {
        while (snapshot != null) {
            cacheMgr.deleteCacheObject(snapshot);
            snapshot = snapshot.getParent();
        }
    }

    protected Answer copyVolumeFromSnapshot(DataObject snapObj, DataObject volObj) {
        SnapshotInfo snapshot = (SnapshotInfo) snapObj;
        StoragePool pool = (StoragePool) volObj.getDataStore();

        String basicErrMsg = "Failed to create volume from " + snapshot.getName() + " on pool " + pool;
        DataStore store = snapObj.getDataStore();
        DataStoreTO storTO = store.getTO();
        DataObject srcData = snapObj;
        try {
            if (!(storTO instanceof NfsTO)) {
                srcData = cacheSnapshotChain(snapshot);
            }

            String value = configDao.getValue(Config.CreateVolumeFromSnapshotWait.toString());
            int _createVolumeFromSnapshotWait = NumbersUtil.parseInt(value,
                    Integer.parseInt(Config.CreateVolumeFromSnapshotWait.getDefaultValue()));

            CopyCommand cmd = new CopyCommand(srcData.getTO(), volObj.getTO(), _createVolumeFromSnapshotWait, _mgmtServer.getExecuteInSequence());
            EndPoint ep = selector.select(snapObj, volObj);
            Answer answer = ep.sendMessage(cmd);

            return answer;
        } catch (Exception e) {
            s_logger.error(basicErrMsg, e);
            throw new CloudRuntimeException(basicErrMsg);
        } finally {
            if (!(storTO instanceof NfsTO)) {
                deleteSnapshotCacheChain((SnapshotInfo) srcData);
            }
        }
    }

    protected Answer cloneVolume(DataObject template, DataObject volume) {
        CopyCommand cmd = new CopyCommand(template.getTO(), volume.getTO(), 0, _mgmtServer.getExecuteInSequence());
        try {
            EndPoint ep = selector.select(volume.getDataStore());
            Answer answer = ep.sendMessage(cmd);
            return answer;
        } catch (Exception e) {
            s_logger.debug("Failed to send to storage pool", e);
            throw new CloudRuntimeException("Failed to send to storage pool", e);
        }
    }

    protected Answer copyVolumeBetweenPools(DataObject srcData, DataObject destData) {
        String value = configDao.getValue(Config.CopyVolumeWait.key());
        int _copyvolumewait = NumbersUtil.parseInt(value, Integer.parseInt(Config.CopyVolumeWait.getDefaultValue()));

        Scope destScope = getZoneScope(destData.getDataStore().getScope());
        DataStore cacheStore = cacheMgr.getCacheStorage(destScope);
        if (cacheStore == null) {
            // need to find a nfs image store, assuming that can't copy volume
            // directly to s3
            ImageStoreEntity imageStore = (ImageStoreEntity) this.dataStoreMgr.getImageStore(destScope.getScopeId());
            if (!imageStore.getProtocol().equalsIgnoreCase("nfs")) {
                s_logger.debug("can't find a nfs image store");
                return null;
            }

            DataObject objOnImageStore = imageStore.create(srcData);
            objOnImageStore.processEvent(Event.CreateOnlyRequested);

            Answer answer = this.copyObject(srcData, objOnImageStore);
            if (answer == null || !answer.getResult()) {
                if (answer != null) {
                    s_logger.debug("copy to image store failed: " + answer.getDetails());
                }
                objOnImageStore.processEvent(Event.OperationFailed);
                imageStore.delete(objOnImageStore);
                return answer;
            }

            objOnImageStore.processEvent(Event.OperationSuccessed, answer);

            objOnImageStore.processEvent(Event.CopyingRequested);

            CopyCommand cmd = new CopyCommand(objOnImageStore.getTO(), destData.getTO(), _copyvolumewait, _mgmtServer.getExecuteInSequence());
            EndPoint ep = selector.select(objOnImageStore, destData);
            answer = ep.sendMessage(cmd);

            if (answer == null || !answer.getResult()) {
                if (answer != null) {
                    s_logger.debug("copy to primary store failed: " + answer.getDetails());
                }
                objOnImageStore.processEvent(Event.OperationFailed);
                imageStore.delete(objOnImageStore);
                return answer;
            }

            objOnImageStore.processEvent(Event.OperationSuccessed);
            imageStore.delete(objOnImageStore);
            return answer;
        } else {
            DataObject cacheData = cacheMgr.createCacheObject(srcData, destScope);
            CopyCommand cmd = new CopyCommand(cacheData.getTO(), destData.getTO(), _copyvolumewait, _mgmtServer.getExecuteInSequence());
            EndPoint ep = selector.select(cacheData, destData);
            Answer answer = ep.sendMessage(cmd);
            return answer;
        }

    }

    @Override
    public Void copyAsync(DataObject srcData, DataObject destData, AsyncCompletionCallback<CopyCommandResult> callback) {
        Answer answer = null;
        String errMsg = null;
        try {
            s_logger.debug("copyAsync inspecting src type " + srcData.getType().toString() +
                    " copyAsync inspecting dest type " + destData.getType().toString());

            if (srcData.getType() == DataObjectType.SNAPSHOT && destData.getType() == DataObjectType.VOLUME) {
                answer = copyVolumeFromSnapshot(srcData, destData);
            } else if (srcData.getType() == DataObjectType.SNAPSHOT && destData.getType() == DataObjectType.TEMPLATE) {
                answer = createTemplateFromSnapshot(srcData, destData);
            } else if (srcData.getType() == DataObjectType.TEMPLATE && destData.getType() == DataObjectType.VOLUME) {
                answer = cloneVolume(srcData, destData);
            } else if (destData.getType() == DataObjectType.VOLUME && srcData.getType() == DataObjectType.VOLUME
                    && srcData.getDataStore().getRole() == DataStoreRole.Primary
                    && destData.getDataStore().getRole() == DataStoreRole.Primary) {
                answer = copyVolumeBetweenPools(srcData, destData);
            } else if (srcData.getType() == DataObjectType.SNAPSHOT && destData.getType() == DataObjectType.SNAPSHOT) {
                answer = copySnapshot(srcData, destData);
            } else {
                answer = copyObject(srcData, destData);
            }

            if (answer != null && !answer.getResult()) {
                errMsg = answer.getDetails();
            }
        } catch (Exception e) {
            s_logger.debug("copy failed", e);
            errMsg = e.toString();
        }
        CopyCommandResult result = new CopyCommandResult(null, answer);
        result.setResult(errMsg);
        callback.complete(result);
        return null;
    }

    @DB
    protected Answer createTemplateFromSnapshot(DataObject srcData, DataObject destData) {

        String value = configDao.getValue(Config.CreatePrivateTemplateFromSnapshotWait.toString());
        int _createprivatetemplatefromsnapshotwait = NumbersUtil.parseInt(value,
                Integer.parseInt(Config.CreatePrivateTemplateFromSnapshotWait.getDefaultValue()));

        if (needCacheStorage(srcData, destData)) {
            SnapshotInfo snapshot = (SnapshotInfo) srcData;
            srcData = cacheSnapshotChain(snapshot);
        }

        CopyCommand cmd = new CopyCommand(srcData.getTO(), destData.getTO(), _createprivatetemplatefromsnapshotwait, _mgmtServer.getExecuteInSequence());
        EndPoint ep = selector.select(srcData, destData);
        Answer answer = ep.sendMessage(cmd);
        return answer;
    }

    protected Answer copySnapshot(DataObject srcData, DataObject destData) {
        String value = configDao.getValue(Config.BackupSnapshotWait.toString());
        int _backupsnapshotwait = NumbersUtil.parseInt(value,
                Integer.parseInt(Config.BackupSnapshotWait.getDefaultValue()));

        DataObject cacheData = null;
        Answer answer = null;
        try {
            if (needCacheStorage(srcData, destData)) {
                cacheData = cacheMgr.getCacheObject(srcData, destData.getDataStore().getScope());

                CopyCommand cmd = new CopyCommand(srcData.getTO(), destData.getTO(), _backupsnapshotwait, _mgmtServer.getExecuteInSequence());
                cmd.setCacheTO(cacheData.getTO());
                EndPoint ep = selector.select(srcData, destData);
                answer = ep.sendMessage(cmd);
            } else {
                CopyCommand cmd = new CopyCommand(srcData.getTO(), destData.getTO(), _backupsnapshotwait, _mgmtServer.getExecuteInSequence());
                EndPoint ep = selector.select(srcData, destData);
                answer = ep.sendMessage(cmd);
            }
            // clean up cache entry in case of failure
            if (answer == null || !answer.getResult()) {
                if (cacheData != null) {
                    cacheMgr.deleteCacheObject(cacheData);
                }
            }
            return answer;
        } catch (Exception e) {
            s_logger.debug("copy snasphot failed: " + e.toString());
            if (cacheData != null) {
                cacheMgr.deleteCacheObject(cacheData);
            }
            throw new CloudRuntimeException(e.toString());
        }

    }

    @Override
    public Void copyAsync(Map<VolumeInfo, DataStore> volumeMap, VirtualMachineTO vmTo, Host srcHost, Host destHost,
                          AsyncCompletionCallback<CopyCommandResult> callback) {
        CopyCommandResult result = new CopyCommandResult(null, null);
        result.setResult("Unsupported operation requested for copying data.");
        callback.complete(result);

        return null;
    }
}
