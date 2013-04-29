package org.apache.cloudstack.engine.subsystem.api.storage;

import com.cloud.storage.Snapshot;


public interface SnapshotStrategy {
	public SnapshotInfo takeSnapshot(SnapshotInfo snapshot);
	public SnapshotInfo backupSnapshot(SnapshotInfo snapshot);
	public boolean deleteSnapshot(Long snapshotId);
	public boolean canHandle(Snapshot snapshot);
}
