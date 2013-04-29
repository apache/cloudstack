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
	SnapshotDataFactory snapshotDataFactory;

	@Override
	public SnapshotInfo backupSnapshot(SnapshotInfo snapshot) {
		SnapshotInfo parentSnapshot = snapshot.getParent();
		if (parentSnapshot.getPath().equalsIgnoreCase(snapshot.getPath())) {
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

	@Override
	public boolean deleteSnapshot(SnapshotInfo snapshot) {
		Long snapshotId = snapshot.getId();
		SnapshotObject snapObj = (SnapshotObject)snapshot;

		if (!Snapshot.State.BackedUp.equals(snapshot.getState()) || !Snapshot) {
			throw new InvalidParameterValueException("Can't delete snapshotshot " + snapshotId + " due to it is not in BackedUp Status");
		}

		if (s_logger.isDebugEnabled()) {
			s_logger.debug("Calling deleteSnapshot for snapshotId: " + snapshotId);
		}
		SnapshotVO lastSnapshot = null;
		if (snapshot.getPrevSnapshotId() != null) {
			List<SnapshotVO> snaps = _snapshotDao.listByBackupUuid(snapshot.getVolumeId(), snapshot.getBackupSnapshotId());
			if (snaps != null && snaps.size() > 1) {
				snapshot.setBackupSnapshotId(null);
				SnapshotVO snapshotVO = this._snapshotDao.findById(snapshotId);
				_snapshotDao.update(snapshot.getId(), snapshotVO);
			}
		}

		_snapshotDao.remove(snapshotId);

		long lastId = snapshotId;
		boolean destroy = false;
		while (true) {
			lastSnapshot = _snapshotDao.findNextSnapshot(lastId);
			if (lastSnapshot == null) {
				// if all snapshots after this snapshot in this chain are removed, remove those snapshots.
				destroy = true;
				break;
			}
			if (lastSnapshot.getRemoved() == null) {
				// if there is one child not removed, then can not remove back up snapshot.
				break;
			}
			lastId = lastSnapshot.getId();
		}
		if (destroy) {
			lastSnapshot = _snapshotDao.findByIdIncludingRemoved(lastId);
			while (lastSnapshot.getRemoved() != null) {
				String BackupSnapshotId = lastSnapshot.getBackupSnapshotId();
				if (BackupSnapshotId != null) {
					List<SnapshotVO> snaps = _snapshotDao.listByBackupUuid(lastSnapshot.getVolumeId(), BackupSnapshotId);
					if (snaps != null && snaps.size() > 1) {
						lastSnapshot.setBackupSnapshotId(null);
						_snapshotDao.update(lastSnapshot.getId(), lastSnapshot);
					} else {
						if (destroySnapshotBackUp(lastSnapshot)) {

						} else {
							s_logger.debug("Destroying snapshot backup failed " + lastSnapshot);
							break;
						}
					}
				}
				lastId = lastSnapshot.getPrevSnapshotId();
				if (lastId == 0) {
					break;
				}
				lastSnapshot = _snapshotDao.findByIdIncludingRemoved(lastId);
			}
		}
		return true;
	}

	@Override
	public boolean canHandle(SnapshotInfo snapshot) {
		if (snapshot.getHypervisorType() == HypervisorType.XenServer) {
			return true;
		} else {
			return false;
		}
	}
	
	@Override
	public SnapshotInfo takeSnapshot(SnapshotInfo snapshot) {
		snapshot = snapshotSvr.takeSnapshot(snapshot).getSnashot();
		//TODO: add async
		return this.backupSnapshot(snapshot);
	}
}
