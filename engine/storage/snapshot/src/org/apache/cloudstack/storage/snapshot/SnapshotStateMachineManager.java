package org.apache.cloudstack.storage.snapshot;

import com.cloud.storage.Snapshot.Event;
import com.cloud.storage.SnapshotVO;
import com.cloud.utils.fsm.NoTransitionException;

public interface SnapshotStateMachineManager {
	public void processEvent(SnapshotVO snapshot, Event event) throws NoTransitionException;
}
