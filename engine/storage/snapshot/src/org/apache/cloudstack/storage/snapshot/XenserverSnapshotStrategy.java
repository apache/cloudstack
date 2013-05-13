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
package org.apache.cloudstack.storage.snapshot;

import java.util.List;

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine.State;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotResult;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine.Event;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotService;
import org.apache.cloudstack.storage.command.CreateObjectAnswer;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreVO;
import org.apache.cloudstack.storage.to.SnapshotObjectTO;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.Snapshot;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.snapshot.SnapshotManager;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.fsm.NoTransitionException;

@Component
public class XenserverSnapshotStrategy extends SnapshotStrategyBase {
	private static final Logger s_logger = Logger
			.getLogger(XenserverSnapshotStrategy.class);

	@Inject
	SnapshotManager snapshotMgr;
	@Inject
	SnapshotService snapshotSvr;
	@Inject
	DataStoreManager dataStoreMgr;
	@Inject
	SnapshotDataStoreDao snapshotStoreDao;
	@Inject
	ConfigurationDao configDao;
	@Inject
	SnapshotDao snapshotDao;
	@Inject
	SnapshotDataFactory snapshotDataFactory;

	@Override
	public SnapshotInfo backupSnapshot(SnapshotInfo snapshot) {
		SnapshotInfo parentSnapshot = snapshot.getParent();
		if (parentSnapshot != null && parentSnapshot.getPath().equalsIgnoreCase(snapshot.getPath())) {
			s_logger.debug("backup an empty snapshot");
			//don't need to backup this snapshot
			SnapshotDataStoreVO parentSnapshotOnBackupStore = this.snapshotStoreDao.findBySnapshot(parentSnapshot.getId(), DataStoreRole.Image);
			if (parentSnapshotOnBackupStore != null && 
					parentSnapshotOnBackupStore.getState() == State.Ready) {
				DataStore store = dataStoreMgr.getDataStore(parentSnapshotOnBackupStore.getDataStoreId(), 
						parentSnapshotOnBackupStore.getRole());

				SnapshotInfo snapshotOnImageStore =  (SnapshotInfo)store.create(snapshot);
				snapshotOnImageStore.processEvent(Event.CreateOnlyRequested);

				SnapshotObjectTO snapTO = new SnapshotObjectTO();
				snapTO.setPath(parentSnapshotOnBackupStore.getInstallPath());
				CreateObjectAnswer createSnapshotAnswer = new CreateObjectAnswer(snapTO);

				snapshotOnImageStore.processEvent(Event.OperationSuccessed, createSnapshotAnswer);
				SnapshotObject snapObj = (SnapshotObject)snapshot;
				try {
					snapObj.processEvent(Snapshot.Event.OperationNotPerformed);
				} catch (NoTransitionException e) {
					s_logger.debug("Failed to change state: " + snapshot.getId() + ": " +e.toString());
					throw new CloudRuntimeException(e.toString());
				}
				return this.snapshotDataFactory.getSnapshot(snapObj.getId(), store);
			} else {
				s_logger.debug("parent snapshot hasn't been backed up yet");
			}
		}

		//determine full snapshot backup or not

		boolean fullBackup = false;
		long preSnapshotId = 0;
		if (parentSnapshot != null) {

			preSnapshotId = parentSnapshot.getId();
			int _deltaSnapshotMax = NumbersUtil.parseInt(configDao.getValue("snapshot.delta.max"), SnapshotManager.DELTAMAX);
			int deltaSnap = _deltaSnapshotMax;

			int i;
			SnapshotDataStoreVO parentSnapshotOnBackupStore = null;
			for (i = 1; i < deltaSnap; i++) {
				parentSnapshotOnBackupStore = this.snapshotStoreDao.findBySnapshot(parentSnapshot.getId(), DataStoreRole.Image);

				Long prevBackupId = parentSnapshotOnBackupStore.getParentSnapshotId();

				if (prevBackupId == 0) {
					break;
				}

				parentSnapshotOnBackupStore = this.snapshotStoreDao.findBySnapshot(prevBackupId, DataStoreRole.Image);
			}
			if (i >= deltaSnap) {
				fullBackup = true;
			}
		}
		
		snapshot.addPayload(fullBackup);
		return this.snapshotSvr.backupSnapshot(snapshot);
	}
	
	protected void deleteSnapshotChain(SnapshotInfo snapshot) {
	    while(snapshot != null) {
	        SnapshotInfo child = snapshot.getChild();
	        SnapshotInfo parent = snapshot.getParent();
	        if (child == null) {
	            if (!parent.getPath().equalsIgnoreCase(snapshot.getPath())) {
	                this.snapshotSvr.deleteSnapshot(snapshot);
	                snapshot = parent;
	                continue;
	            }
	            break;
	        } else {
	            break;
	        }
	    }
	}

	@Override
	public boolean deleteSnapshot(Long snapshotId) {
	    SnapshotVO snapshotVO = snapshotDao.findById(snapshotId);
	    if (snapshotVO.getState() == Snapshot.State.Destroyed) {
	        return true;
	    }
	    
		if (!Snapshot.State.BackedUp.equals(snapshotVO.getState())) {
			throw new InvalidParameterValueException("Can't delete snapshotshot " + snapshotId + " due to it is not in BackedUp Status");
		}

		if (s_logger.isDebugEnabled()) {
			s_logger.debug("Calling deleteSnapshot for snapshotId: " + snapshotId);
		}
		
		
		//firt mark the snapshot as destroyed, so that ui can't see it, but we may not destroy the snapshot on the storage, as other snaphosts may depend on it.
		SnapshotInfo snapshotOnPrimary = this.snapshotDataFactory.getSnapshot(snapshotId, DataStoreRole.Primary);
		SnapshotObject obj = (SnapshotObject)snapshotOnPrimary;
		try {
            obj.processEvent(Snapshot.Event.DestroyRequested);
        } catch (NoTransitionException e) {
            s_logger.debug("Failed to destroy snapshot: " + e.toString());
            return false;
        }
		
		try {
		    if (snapshotOnPrimary != null) {
		        deleteSnapshotChain(snapshotOnPrimary);
		    }

		    SnapshotInfo snapshotOnImage = this.snapshotDataFactory.getSnapshot(snapshotId, DataStoreRole.Image);
		    if (snapshotOnImage != null) {
		        deleteSnapshotChain(snapshotOnImage);
		    }
		    
		    obj.processEvent(Snapshot.Event.OperationSucceeded);
		} catch (Exception e) {
		    s_logger.debug("Failed to delete snapshot: " + e.toString());
		    try {
                obj.processEvent(Snapshot.Event.OperationFailed);
            } catch (NoTransitionException e1) {
                s_logger.debug("Failed to change snapshot state: " + e.toString());
            }
		}
		
		return true;
	}
	
	@Override
	public SnapshotInfo takeSnapshot(SnapshotInfo snapshot) {
		snapshot = snapshotSvr.takeSnapshot(snapshot).getSnashot();
		//TODO: add async
		return this.backupSnapshot(snapshot);
	}

    @Override
    public boolean canHandle(Snapshot snapshot) {
       return true;
    }
}
