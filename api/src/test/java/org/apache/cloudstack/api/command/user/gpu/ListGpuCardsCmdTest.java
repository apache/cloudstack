package org.apache.cloudstack.api.command.user.gpu;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;


public class ListGpuCardsCmdTest {

    @Test
    public void getId() {
        ListGpuCardsCmd cmd = new ListGpuCardsCmd();
        Assert.assertNull(cmd.getId());
        Long id = 1L;
        ReflectionTestUtils.setField(cmd, "id", id);
        Assert.assertEquals(id, cmd.getId());
    }

    @Test
    public void getVendorName() {
        ListGpuCardsCmd cmd = new ListGpuCardsCmd();
        Assert.assertNull(cmd.getVendorName());
        String vendorName = "vendor name";
        ReflectionTestUtils.setField(cmd, "vendorName", vendorName);
        Assert.assertEquals(vendorName, cmd.getVendorName());
    }

    @Test
    public void getVendorId() {
        ListGpuCardsCmd cmd = new ListGpuCardsCmd();
        Assert.assertNull(cmd.getVendorId());
        String vendorId = "vendor id";
        ReflectionTestUtils.setField(cmd, "vendorId", vendorId);
        Assert.assertEquals(vendorId, cmd.getVendorId());
    }

    @Test
    public void getDeviceId() {
        ListGpuCardsCmd cmd = new ListGpuCardsCmd();
        Assert.assertNull(cmd.getDeviceId());
        String deviceId = "device id";
        ReflectionTestUtils.setField(cmd, "deviceId", deviceId);
        Assert.assertEquals(deviceId, cmd.getDeviceId());
    }

    @Test
    public void getDeviceName() {
        ListGpuCardsCmd cmd = new ListGpuCardsCmd();
        Assert.assertNull(cmd.getDeviceName());
        String deviceName = "device name";
        ReflectionTestUtils.setField(cmd, "deviceName", deviceName);
        Assert.assertEquals(deviceName, cmd.getDeviceName());
    }

    @Test
    public void getActiveOnly() {
        ListGpuCardsCmd cmd = new ListGpuCardsCmd();
        Assert.assertFalse(cmd.getActiveOnly());
        Boolean activeOnly = true;
        ReflectionTestUtils.setField(cmd, "activeOnly", activeOnly);
        Assert.assertEquals(activeOnly, cmd.getActiveOnly());
    }
}
