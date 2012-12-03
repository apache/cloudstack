package org.apache.cloudstack.storage.backup;

import org.apache.cloudstack.storage.backup.datastore.BackupStoreInfo;

public interface SnapshotOnBackupStoreInfo {
	public String getName();
	public BackupStoreInfo getBackupStore();
}
