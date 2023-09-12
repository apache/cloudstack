package org.apache.cloudstack.storage.to;

import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

public class SnapshotObjectTOTest {

    @Test
    public void testAccountId() {
        SnapshotObjectTO obj = new SnapshotObjectTO();
        long accountId = 1L;
        ReflectionTestUtils.setField(obj, "accountId", accountId);
        Assert.assertEquals(accountId, obj.getAccountId());
        accountId = 100L;
        obj.setAccountId(accountId);
        Assert.assertEquals(accountId, obj.getAccountId());
        SnapshotInfo snapshot = Mockito.mock(SnapshotInfo.class);
        Mockito.when(snapshot.getAccountId()).thenReturn(accountId);
        Mockito.when(snapshot.getDataStore()).thenReturn(Mockito.mock(DataStore.class));
        SnapshotObjectTO object = new SnapshotObjectTO(snapshot);
        Assert.assertEquals(accountId, object.getAccountId());
    }
}