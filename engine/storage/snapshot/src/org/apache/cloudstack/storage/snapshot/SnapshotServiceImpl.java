package org.apache.cloudstack.storage.snapshot;

import org.apache.cloudstack.engine.cloud.entity.api.SnapshotEntity;
import org.springframework.stereotype.Component;

@Component
public class SnapshotServiceImpl implements SnapshotService {

	@Override
	public SnapshotEntity getSnapshotEntity(long snapshotId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean takeSnapshot(SnapshotInfo snapshot) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean revertSnapshot(SnapshotInfo snapshot) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean deleteSnapshot(SnapshotInfo snapshot) {
		// TODO Auto-generated method stub
		return false;
	}

}
