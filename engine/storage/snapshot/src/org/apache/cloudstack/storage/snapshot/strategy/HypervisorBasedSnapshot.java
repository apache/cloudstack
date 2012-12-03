package org.apache.cloudstack.storage.snapshot.strategy;

import org.apache.cloudstack.storage.snapshot.SnapshotInfo;
import org.apache.cloudstack.storage.snapshot.SnapshotStrategy;
import org.springframework.stereotype.Component;

@Component
public class HypervisorBasedSnapshot implements SnapshotStrategy {

	@Override
	public boolean takeSnapshot(SnapshotInfo snapshot) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean revertSnapshot(SnapshotInfo snapshot) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean deleteSnapshot(SnapshotInfo snapshot) {
		// TODO Auto-generated method stub
		return false;
	}

}
