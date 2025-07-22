package org.apache.cloudstack.api.command.user.gpu;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

public class ListGpuDevicesCmdTest {

    @Test
    public void getVmId() {
        ListGpuDevicesCmd cmd = new ListGpuDevicesCmd();
        Assert.assertNull(cmd.getVmId());
        Long vmId = 1L;
        ReflectionTestUtils.setField(cmd, "vmId", vmId);
        Assert.assertEquals(vmId, cmd.getVmId());
    }
}
