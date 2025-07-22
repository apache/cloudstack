package org.apache.cloudstack.api.command.admin.gpu;

import com.cloud.user.Account;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class CreateGpuCardCmdTest {

    @Test
    public void getDeviceId() {
        CreateGpuCardCmd cmd = new CreateGpuCardCmd();
        assertNull(cmd.getDeviceId());
        String deviceId = "0000:00:1f.6";
        ReflectionTestUtils.setField(cmd, "deviceId", deviceId);
        assertEquals(deviceId, cmd.getDeviceId());
    }

    @Test
    public void getDeviceName() {
        CreateGpuCardCmd cmd = new CreateGpuCardCmd();
        assertNull(cmd.getDeviceName());
        String deviceName = "NVIDIA GeForce GTX 1080";
        ReflectionTestUtils.setField(cmd, "deviceName", deviceName);
        assertEquals(deviceName, cmd.getDeviceName());
    }

    @Test
    public void getName() {
        CreateGpuCardCmd cmd = new CreateGpuCardCmd();
        assertNull(cmd.getName());
        String name = "Test GPU Card";
        ReflectionTestUtils.setField(cmd, "name", name);
        assertEquals(name, cmd.getName());
    }

    @Test
    public void getVendorName() {
        CreateGpuCardCmd cmd = new CreateGpuCardCmd();
        assertNull(cmd.getVendorName());
        String vendorName = "NVIDIA";
        ReflectionTestUtils.setField(cmd, "vendorName", vendorName);
        assertEquals(vendorName, cmd.getVendorName());
    }

    @Test
    public void getVendorId() {
        CreateGpuCardCmd cmd = new CreateGpuCardCmd();
        assertNull(cmd.getVendorId());
        String vendorId = "10de"; // NVIDIA vendor ID
        ReflectionTestUtils.setField(cmd, "vendorId", vendorId);
        assertEquals(vendorId, cmd.getVendorId());
    }

    @Test
    public void getVideoRam() {
        CreateGpuCardCmd cmd = new CreateGpuCardCmd();
        assertNull(cmd.getVideoRam());
        Long videoRam = 8192L; // 8 GB
        ReflectionTestUtils.setField(cmd, "videoRam", videoRam);
        assertEquals(videoRam, cmd.getVideoRam());
    }

    @Test
    public void getEntityOwnerId() {
        CreateGpuCardCmd cmd = new CreateGpuCardCmd();
        assertEquals(Account.ACCOUNT_ID_SYSTEM, cmd.getEntityOwnerId());
    }
}
