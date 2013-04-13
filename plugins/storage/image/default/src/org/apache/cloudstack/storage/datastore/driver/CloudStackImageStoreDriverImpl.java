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

import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.CommandResult;
import org.apache.cloudstack.engine.subsystem.api.storage.CopyCommandResult;
import org.apache.cloudstack.engine.subsystem.api.storage.CreateCmdResult;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObjectType;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataTO;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPoint;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.framework.async.AsyncRpcConext;
import org.apache.cloudstack.storage.image.ImageStoreDriver;
import org.apache.cloudstack.storage.image.store.ImageStoreImpl;
import org.apache.cloudstack.storage.image.store.TemplateObject;
import org.apache.cloudstack.storage.to.ImageStoreTO;
import org.apache.cloudstack.storage.volume.VolumeObject;
import org.apache.log4j.Logger;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.DeleteSnapshotBackupCommand;
import com.cloud.agent.api.storage.DeleteVolumeCommand;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.agent.api.to.S3TO;
import com.cloud.agent.api.to.SwiftTO;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.RegisterVolumePayload;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.VMTemplateStorageResourceAssoc;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.VMTemplateZoneVO;
import com.cloud.storage.VolumeHostVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VMTemplateHostDao;
import com.cloud.storage.dao.VMTemplateZoneDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.dao.VolumeHostDao;
import com.cloud.storage.download.DownloadMonitor;
import com.cloud.storage.s3.S3Manager;
import com.cloud.storage.snapshot.SnapshotManager;
import com.cloud.storage.swift.SwiftManager;
import com.cloud.utils.exception.CloudRuntimeException;

public class CloudStackImageStoreDriverImpl implements ImageStoreDriver {
    private static final Logger s_logger = Logger
            .getLogger(CloudStackImageStoreDriverImpl.class);
    @Inject
    VMTemplateZoneDao templateZoneDao;
    @Inject
    VMTemplateDao templateDao;
    @Inject DownloadMonitor _downloadMonitor;
    @Inject
    VMTemplateHostDao _vmTemplateHostDao;
    @Inject VolumeDao volumeDao;
    @Inject VolumeHostDao volumeHostDao;
    @Inject HostDao hostDao;
    @Inject SnapshotDao snapshotDao;
    @Inject AgentManager agentMgr;
    @Inject SnapshotManager snapshotMgr;
	@Inject
    private SwiftManager _swiftMgr;
    @Inject
    private S3Manager _s3Mgr;
    @Override
    public String grantAccess(DataObject data, EndPoint ep) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public DataTO getTO(DataObject data) {
        return null;
    }


    @Override
    public DataStoreTO getStoreTO(DataStore store) {
        ImageStoreImpl nfsStore = (ImageStoreImpl)store;
        ImageStoreTO nfsTO = new ImageStoreTO();
        nfsTO.setProviderName("CloudStack");
        nfsTO.setRole(DataStoreRole.Image);
        nfsTO.setUri(nfsStore.getUri());
        return nfsTO;
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

    class CreateContext<T> extends AsyncRpcConext<T> {
        final DataObject data;
        public CreateContext(AsyncCompletionCallback<T> callback, DataObject data) {
            super(callback);
            this.data = data;
        }
    }

    @Override
    public void createAsync(DataObject data,
            AsyncCompletionCallback<CreateCmdResult> callback) {
        if (data.getType() == DataObjectType.TEMPLATE) {
            TemplateObject tData = (TemplateObject)data;
            _downloadMonitor.downloadTemplateToStorage(tData.getImage(), tData.getDataStore(), callback);
        } else if (data.getType() == DataObjectType.VOLUME) {
            VolumeObject volInfo = (VolumeObject)data;
            RegisterVolumePayload payload = (RegisterVolumePayload)volInfo.getpayload();
            _downloadMonitor.downloadVolumeToStorage(volInfo.getVolume(), volInfo.getDataStore(), payload.getUrl(),
                    payload.getChecksum(), ImageFormat.valueOf(payload.getFormat().toUpperCase()), callback);
        }

        CreateCmdResult result = new CreateCmdResult(null, null);
        callback.complete(result);
    }

    private void deleteVolume(DataObject data, AsyncCompletionCallback<CommandResult> callback) {
        // TODO Auto-generated method stub
        VolumeVO vol = volumeDao.findById(data.getId());
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Expunging " + vol);
        }

        // Find out if the volume is present on secondary storage
        VolumeHostVO volumeHost = volumeHostDao.findByVolumeId(vol.getId());
        if (volumeHost != null) {
            if (volumeHost.getDownloadState() == VMTemplateStorageResourceAssoc.Status.DOWNLOADED) {
                HostVO ssHost = hostDao.findById(volumeHost.getHostId());
                DeleteVolumeCommand dtCommand = new DeleteVolumeCommand(
                        ssHost.getStorageUrl(), volumeHost.getInstallPath());
                Answer answer = agentMgr.sendToSecStorage(ssHost, dtCommand);
                if (answer == null || !answer.getResult()) {
                    s_logger.debug("Failed to delete "
                            + volumeHost
                            + " due to "
                            + ((answer == null) ? "answer is null" : answer
                                    .getDetails()));
                    return;
                }
            } else if (volumeHost.getDownloadState() == VMTemplateStorageResourceAssoc.Status.DOWNLOAD_IN_PROGRESS) {
                s_logger.debug("Volume: " + vol.getName()
                        + " is currently being uploaded; cant' delete it.");
                throw new CloudRuntimeException(
                        "Please specify a volume that is not currently being uploaded.");
            }
            volumeHostDao.remove(volumeHost.getId());
            volumeDao.remove(vol.getId());
            CommandResult result = new CommandResult();
            callback.complete(result);
            return;
        }
    }

    private void deleteTemplate(DataObject data, AsyncCompletionCallback<CommandResult> callback) {

    }

    private void deleteSnapshot(DataObject data, AsyncCompletionCallback<CommandResult> callback) {
    	Long snapshotId = data.getId();
    	SnapshotVO snapshot = this.snapshotDao.findByIdIncludingRemoved(snapshotId);
    	CommandResult result = new CommandResult();
    	if (snapshot == null) {
    		s_logger.debug("Destroying snapshot " + snapshotId + " backup failed due to unable to find snapshot ");
    		result.setResult("Unable to find snapshot: " + snapshotId);
    		callback.complete(result);
    		return;
    	}

    	try {
    		String secondaryStoragePoolUrl = this.snapshotMgr.getSecondaryStorageURL(snapshot);
    		Long dcId = snapshot.getDataCenterId();
    		Long accountId = snapshot.getAccountId();
    		Long volumeId = snapshot.getVolumeId();

    		String backupOfSnapshot = snapshot.getBackupSnapshotId();
    		if (backupOfSnapshot == null) {
    			callback.complete(result);
    			return;
    		}
    		SwiftTO swift = _swiftMgr.getSwiftTO(snapshot.getSwiftId());
    		S3TO s3 = _s3Mgr.getS3TO();

    		DeleteSnapshotBackupCommand cmd = new DeleteSnapshotBackupCommand(
    				swift, s3, secondaryStoragePoolUrl, dcId, accountId, volumeId,
    				backupOfSnapshot, false);
    		Answer answer = agentMgr.sendToSSVM(dcId, cmd);

    		if ((answer != null) && answer.getResult()) {
    			snapshot.setBackupSnapshotId(null);
    			snapshotDao.update(snapshotId, snapshot);
    		} else if (answer != null) {
    			result.setResult(answer.getDetails());
    		}
    	} catch (Exception e) {
    		s_logger.debug("failed to delete snapshot: " + snapshotId + ": " + e.toString());
    		result.setResult(e.toString());
    	}
    	callback.complete(result);
    }

    @Override
    public void deleteAsync(DataObject data,
            AsyncCompletionCallback<CommandResult> callback) {
        if (data.getType() == DataObjectType.VOLUME) {
            deleteVolume(data, callback);
        } else if (data.getType() == DataObjectType.TEMPLATE) {
            deleteTemplate(data, callback);
        } else if (data.getType() == DataObjectType.SNAPSHOT) {
        	deleteSnapshot(data, callback);
        }
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
	public void resize(DataObject data,
			AsyncCompletionCallback<CreateCmdResult> callback) {
		// TODO Auto-generated method stub

	}

}
