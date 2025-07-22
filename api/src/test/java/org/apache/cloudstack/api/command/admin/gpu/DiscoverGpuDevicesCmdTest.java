package org.apache.cloudstack.api.command.admin.gpu;

import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class DiscoverGpuDevicesCmdTest {

    @Test
    public void getId() {
        DiscoverGpuDevicesCmd cmd = new DiscoverGpuDevicesCmd();
        assertNull(cmd.getId());
        Long id = 1L;
        ReflectionTestUtils.setField(cmd, "id", id);
        assertEquals(id, cmd.getId());
    }
}
