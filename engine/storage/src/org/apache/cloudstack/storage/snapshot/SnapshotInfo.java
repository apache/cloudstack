package org.apache.cloudstack.storage.snapshot;

import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;

public interface SnapshotInfo {
	public String getName();
	public SnapshotInfo getParent();
	public SnapshotInfo getChild();
	public VolumeInfo getBaseVolume();
}
