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
package org.apache.cloudstack.storage.datastore.driver;

import java.util.Set;

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.CommandResult;
import org.apache.cloudstack.engine.subsystem.api.storage.CopyCommandResult;
import org.apache.cloudstack.engine.subsystem.api.storage.CreateCmdResult;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObjectType;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPoint;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.volume.PrimaryDataStoreDriver;
import org.apache.log4j.Logger;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.storage.CreateAnswer;
import com.cloud.agent.api.storage.CreateCommand;
import com.cloud.agent.api.storage.DestroyCommand;
import com.cloud.agent.api.to.StorageFilerTO;
import com.cloud.exception.StorageUnavailableException;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.Storage;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.StorageManager;
import com.cloud.storage.StoragePool;
import com.cloud.storage.VMTemplateHostVO;
import com.cloud.storage.VMTemplateStoragePoolVO;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.template.TemplateManager;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.DiskProfile;
import com.cloud.vm.dao.VMInstanceDao;

public class AncientPrimaryDataStoreDriverImpl implements PrimaryDataStoreDriver {
    private static final Logger s_logger = Logger
            .getLogger(AncientPrimaryDataStoreDriverImpl.class);
    @Inject DiskOfferingDao diskOfferingDao;
    @Inject VMTemplateDao templateDao;
    @Inject VolumeDao volumeDao;
    @Inject TemplateManager templateMgr;
    @Inject HostDao hostDao;
    @Inject StorageManager storageMgr;
    @Inject VMInstanceDao vmDao;
    @Inject PrimaryDataStoreDao primaryStoreDao;
    @Override
    public String grantAccess(DataObject data, EndPoint ep) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean revokeAccess(DataObject data, EndPoint ep) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Set<DataObject> listObjects(DataStore store) {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean createVolume(
            VolumeInfo volume) throws StorageUnavailableException {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Creating volume: " + volume);
        }
        
        DiskOfferingVO offering = diskOfferingDao.findById(volume.getDiskOfferingId());
        DiskProfile diskProfile = new DiskProfile(volume, offering,
                null);

        VMTemplateVO template = null;
        if (volume.getTemplateId() != null) {
            template = templateDao.findById(volume.getTemplateId());
        }

        StoragePool pool = (StoragePool)volume.getDataStore();
        VolumeVO vol = volumeDao.findById(volume.getId());
        if (pool != null) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Trying to create in " + pool);
            }
            vol.setPoolId(pool.getId());
   
            CreateCommand cmd = null;
            VMTemplateStoragePoolVO tmpltStoredOn = null;

            for (int i = 0; i < 2; i++) {
                if (template != null
                        && template.getFormat() != Storage.ImageFormat.ISO) {
                    if (pool.getPoolType() == StoragePoolType.CLVM) {
                        // prepareISOForCreate does what we need, which is to
                        // tell us where the template is
                        VMTemplateHostVO tmpltHostOn = templateMgr
                                .prepareISOForCreate(template, pool);
                        if (tmpltHostOn == null) {
                            s_logger.debug("cannot find template "
                                    + template.getId() + " "
                                    + template.getName());
                            throw new CloudRuntimeException("cannot find template"
                                    + template.getId() 
                                    + template.getName());
                        }
                        HostVO secondaryStorageHost = hostDao
                                .findById(tmpltHostOn.getHostId());
                        String tmpltHostUrl = secondaryStorageHost
                                .getStorageUrl();
                        String fullTmpltUrl = tmpltHostUrl + "/"
                                + tmpltHostOn.getInstallPath();
                        cmd = new CreateCommand(diskProfile, fullTmpltUrl,
                                new StorageFilerTO(pool));
                    } else {
                        tmpltStoredOn = templateMgr.prepareTemplateForCreate(
                                template, pool);
                        if (tmpltStoredOn == null) {
                            s_logger.debug("Cannot use this pool " + pool
                                    + " because we can't propagate template "
                                    + template);
                            throw new CloudRuntimeException("Cannot use this pool " + pool
                                    + " because we can't propagate template "
                                    + template);
                        }
                        cmd = new CreateCommand(diskProfile,
                                tmpltStoredOn.getLocalDownloadPath(),
                                new StorageFilerTO(pool));
                    }
                } else {
                    if (template != null
                            && Storage.ImageFormat.ISO == template.getFormat()) {
                        VMTemplateHostVO tmpltHostOn = templateMgr
                                .prepareISOForCreate(template, pool);
                        if (tmpltHostOn == null) {
                            throw new CloudRuntimeException(
                                    "Did not find ISO in secondry storage in zone "
                                            + pool.getDataCenterId());
                        }
                    }
                    cmd = new CreateCommand(diskProfile, new StorageFilerTO(
                            pool));
                }

                Answer answer = storageMgr.sendToPool(pool, null, cmd);
                if (answer.getResult()) {
                    CreateAnswer createAnswer = (CreateAnswer) answer;
                    vol.setFolder(pool.getPath());
                    vol.setPath(createAnswer.getVolume().getPath());
                    vol.setSize(createAnswer.getVolume().getSize());
                    vol.setPoolType(pool.getPoolType());
                    vol.setPoolId(pool.getId());
                    vol.setPodId(pool.getPodId());
                    this.volumeDao.update(vol.getId(), vol);
                    return true;
                } else {
                    if (tmpltStoredOn != null
                            && (answer instanceof CreateAnswer)
                            && ((CreateAnswer) answer)
                                    .templateReloadRequested()) {
                        if (!templateMgr
                                .resetTemplateDownloadStateOnPool(tmpltStoredOn
                                        .getId())) {
                            break; // break out of template-redeploy retry loop
                        }
                    } else {
                        break;
                    }
                }
            }
        }

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Unable to create volume " + volume.getId());
        }
        return false;
    }

    @Override
    public void createAsync(DataObject data,
            AsyncCompletionCallback<CreateCmdResult> callback) {
        // TODO Auto-generated method stub
        String errMsg = null;
        if (data.getType() == DataObjectType.VOLUME) {
            try {
                createVolume((VolumeInfo)data);
            } catch (StorageUnavailableException e) {
                s_logger.debug("failed to create volume", e);
                errMsg = e.toString();
            } catch (Exception e) {
                s_logger.debug("failed to create volume", e);
                errMsg = e.toString();
            }
        }
        CreateCmdResult result = new CreateCmdResult(null, null);
        if (errMsg != null) {
            result.setResult(errMsg);
        }
        
        callback.complete(result);
        
    }

    @Override
    public void deleteAsync(DataObject data,
            AsyncCompletionCallback<CommandResult> callback) {

        String vmName = null;
        VolumeVO vol = this.volumeDao.findById(data.getId());


        StoragePool pool = (StoragePool)data.getDataStore();

        DestroyCommand cmd = new DestroyCommand(pool, vol, vmName);

        CommandResult result = new CommandResult();
        try {
            Answer answer = this.storageMgr.sendToPool(pool, cmd);
            if (answer != null && !answer.getResult()) {
                result.setResult(answer.getDetails());
                s_logger.info("Will retry delete of " + vol + " from " + pool.getId());
            }
        } catch (StorageUnavailableException e) {
            s_logger.error("Storage is unavailable currently.  Will retry delete of "
                        + vol + " from " + pool.getId(), e);
            result.setResult(e.toString());
        } catch (Exception ex) {
            s_logger.debug("Unable to destoy volume" + vol + " from " + pool.getId(), ex);
            result.setResult(ex.toString());
        }
        callback.complete(result);
    }

    @Override
    public void copyAsync(DataObject srcdata, DataObject destData,
            AsyncCompletionCallback<CopyCommandResult> callback) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean canCopy(DataObject srcData, DataObject destData) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void takeSnapshot(SnapshotInfo snapshot,
            AsyncCompletionCallback<CommandResult> callback) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void revertSnapshot(SnapshotInfo snapshot,
            AsyncCompletionCallback<CommandResult> callback) {
        // TODO Auto-generated method stub
        
    }

}
