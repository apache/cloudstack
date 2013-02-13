package org.apache.cloudstack.engine.subsystem.api.storage;


public interface SnapshotStrategy {
	public boolean canHandle(SnapshotInfo snapshot);
	public SnapshotInfo takeSnapshot(VolumeInfo volume, Long snapshotId);
	public SnapshotInfo backupSnapshot(SnapshotInfo snapshot);
	public boolean deleteSnapshot(SnapshotInfo snapshot);
	public boolean revertSnapshot(SnapshotInfo snapshot);
}
