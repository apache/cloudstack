package org.apache.cloudstack.api.command.admin.gpu;

import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ListGpuDevicesCmdByAdminTest {

    @Test
    public void getId() {
        ListGpuDevicesCmdByAdmin cmd = new ListGpuDevicesCmdByAdmin();
        assertNull(cmd.getId());
        Long id = 1L;
        ReflectionTestUtils.setField(cmd, "id", id);
        assertEquals(id, cmd.getId());
    }

    @Test
    public void getHostId() {
        ListGpuDevicesCmdByAdmin cmd = new ListGpuDevicesCmdByAdmin();
        assertNull(cmd.getHostId());
        Long hostId = 1L;
        ReflectionTestUtils.setField(cmd, "hostId", hostId);
        assertEquals(hostId, cmd.getHostId());
    }

    @Test
    public void getGpuCardId() {
        ListGpuDevicesCmdByAdmin cmd = new ListGpuDevicesCmdByAdmin();
        assertNull(cmd.getGpuCardId());
        Long gpuCardId = 1L;
        ReflectionTestUtils.setField(cmd, "gpuCardId", gpuCardId);
        assertEquals(gpuCardId, cmd.getGpuCardId());
    }

    @Test
    public void getVgpuProfileId() {
        ListGpuDevicesCmdByAdmin cmd = new ListGpuDevicesCmdByAdmin();
        assertNull(cmd.getVgpuProfileId());
        Long vgpuProfileId = 1L;
        ReflectionTestUtils.setField(cmd, "vgpuProfileId", vgpuProfileId);
        assertEquals(vgpuProfileId, cmd.getVgpuProfileId());
    }
}
