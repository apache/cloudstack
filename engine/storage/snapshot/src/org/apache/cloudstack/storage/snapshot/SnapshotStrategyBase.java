package org.apache.cloudstack.storage.snapshot;

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotService;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotStrategy;

public abstract class SnapshotStrategyBase implements SnapshotStrategy {
	@Inject
	SnapshotService snapshotSvr;

	@Override
	public SnapshotInfo takeSnapshot(SnapshotInfo snapshot) {
		return snapshotSvr.takeSnapshot(snapshot).getSnashot();
	}

	@Override
	public SnapshotInfo backupSnapshot(SnapshotInfo snapshot) {
		return snapshotSvr.backupSnapshot(snapshot);
	}
}
