package com.cloud.snapshot;

import java.util.List;

import com.cloud.storage.Snapshot;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.dao.SnapshotDaoImpl;
import com.cloud.utils.component.ComponentLocator;

import junit.framework.Assert;
import junit.framework.TestCase;

public class SnapshotDaoTest extends TestCase {
	
    public void testListBy() {
        SnapshotDaoImpl dao = ComponentLocator.inject(SnapshotDaoImpl.class);
        
        List<SnapshotVO> snapshots = dao.listByInstanceId(3, Snapshot.Status.BackedUp);
        for(SnapshotVO snapshot : snapshots) {
            Assert.assertTrue(snapshot.getStatus() == Snapshot.Status.BackedUp);
        }
    }
}
