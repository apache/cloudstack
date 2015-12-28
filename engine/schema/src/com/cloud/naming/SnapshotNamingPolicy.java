package com.cloud.naming;

import com.cloud.storage.Snapshot;
import com.cloud.storage.SnapshotVO;

public interface SnapshotNamingPolicy extends ResourceNamingPolicy<Snapshot, SnapshotVO>{

    public String getSnapshotName(Long volumeId);
}
