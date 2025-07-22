package org.apache.cloudstack.api.command.admin.gpu;

import com.cloud.user.Account;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ManageGpuDeviceCmdTest {

    @Test
    public void getIds() {
        ManageGpuDeviceCmd cmd = new ManageGpuDeviceCmd();
        assertNull(cmd.getIds());
        List<Long> ids = List.of(1L, 2L, 3L);
        ReflectionTestUtils.setField(cmd, "ids", ids);
        assertEquals(ids, cmd.getIds());
    }

    @Test
    public void getEntityOwnerId() {
        ManageGpuDeviceCmd cmd = new ManageGpuDeviceCmd();
        assertEquals(Account.ACCOUNT_ID_SYSTEM, cmd.getEntityOwnerId());
    }
}
