package org.apache.cloudstack.api.command.admin.template;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.Assert.assertEquals;

public class ListTemplatesCmdByAdminTest {

    @InjectMocks
    ListTemplatesCmdByAdmin cmd;
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
    public void testGetDataStoreId() {
        Long id = 1234L;
        ReflectionTestUtils.setField(cmd, "dataStoreId", id);
        assertEquals(id, cmd.getDataStoreId());
    }

    @Test
    public void testGetStoreId() {
        Long id = 1234L;
        ReflectionTestUtils.setField(cmd, "zoneId", id);
        assertEquals(id, cmd.getZoneId());
    }

    @Test
    public void testGetShowRemoved() {
        Boolean showRemoved = true;
        ReflectionTestUtils.setField(cmd, "showRemoved", showRemoved);
        assertEquals(showRemoved, cmd.getShowRemoved());
    }

    @Test
    public void testGetShowUnique() {
        Boolean showUnique = true;
        ReflectionTestUtils.setField(cmd, "showUnique", showUnique);
        assertEquals(showUnique, cmd.getShowUnique());
    }

    @Test
    public void testGetShowIcon() {
        Boolean showIcon = true;
        ReflectionTestUtils.setField(cmd, "showIcon", showIcon);
        assertEquals(showIcon, cmd.getShowIcon());
    }

    @Test
    public void testGetHypervisor() {
        String hypervisor = "test";
        ReflectionTestUtils.setField(cmd, "hypervisor", hypervisor);
        assertEquals(hypervisor, cmd.getHypervisor());
    }

    @Test
    public void testGetId() {
        Long id = 1234L;
        ReflectionTestUtils.setField(cmd, "id", id);
        assertEquals(id, cmd.getId());
    }

}
