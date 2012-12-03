package org.apache.cloudstack.storage.snapshot;

import org.apache.cloudstack.engine.cloud.entity.api.SnapshotEntity;

public interface SnapshotService {
	public SnapshotEntity getSnapshotEntity(long snapshotId);
	public boolean takeSnapshot(SnapshotInfo snapshot);
	public boolean revertSnapshot(SnapshotInfo snapshot);
	public boolean deleteSnapshot(SnapshotInfo snapshot);
}
