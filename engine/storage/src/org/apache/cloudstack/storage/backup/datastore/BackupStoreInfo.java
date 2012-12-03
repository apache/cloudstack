package org.apache.cloudstack.storage.backup.datastore;

import org.apache.cloudstack.storage.backup.SnapshotOnBackupStoreInfo;

public interface BackupStoreInfo {
	public SnapshotOnBackupStoreInfo getSnapshot(long snapshotId);
	public boolean deleteSnapshot(SnapshotOnBackupStoreInfo snapshot);
}
