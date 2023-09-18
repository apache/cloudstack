package org.apache.cloudstack.api.command.admin.storage;

import org.apache.cloudstack.api.response.MigrationResponse;
import org.apache.cloudstack.storage.ImageStoreService;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

public class MigrateResourcesToAnotherSecondaryStorageCmdTest {


    @Mock
    ImageStoreService _imageStoreService;

    @InjectMocks
    MigrateResourcesToAnotherSecondaryStorageCmd cmd;
    private AutoCloseable closeable;

    @Before
    public void setUp() throws Exception {
        closeable = MockitoAnnotations.openMocks(this);
    }

    @After
    public void tearDown() throws Exception {
        closeable.close();
    }

    @Test
    public void testGetDestStoreId() {
        Long id = 1234L;
        ReflectionTestUtils.setField(cmd, "destStoreId", id);
        Assert.assertEquals(id, cmd.getDestStoreId());
    }

    @Test
    public void testGetId() {
        Long id = 1234L;
        ReflectionTestUtils.setField(cmd, "id", id);
        Assert.assertEquals(id, cmd.getId());
    }

    @Test
    public void testGetTemplateIdList() {
        List<Long> ids = List.of(1234L, 5678L);
        ReflectionTestUtils.setField(cmd, "templateIdList", ids);
        Assert.assertEquals(ids, cmd.getTemplateIdList());
    }

    @Test
    public void testGetSnapshotIdList() {
        List<Long> ids = List.of(1234L, 5678L);
        ReflectionTestUtils.setField(cmd, "snapshotIdList", ids);
        Assert.assertEquals(ids, cmd.getSnapshotIdList());
    }

    @Test
    public void testExecute() {
        MigrationResponse response = Mockito.mock(MigrationResponse.class);
        Mockito.when(_imageStoreService.migrateResources(Mockito.any())).thenReturn(response);
        cmd.execute();
        Assert.assertEquals(response, cmd.getResponseObject());
    }

}
