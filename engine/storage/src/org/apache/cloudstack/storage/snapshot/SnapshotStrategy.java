package org.apache.cloudstack.storage.snapshot;

public interface SnapshotStrategy {
	public boolean takeSnapshot(SnapshotInfo snapshot);
	public boolean revertSnapshot(SnapshotInfo snapshot);
	public boolean deleteSnapshot(SnapshotInfo snapshot); 
}
