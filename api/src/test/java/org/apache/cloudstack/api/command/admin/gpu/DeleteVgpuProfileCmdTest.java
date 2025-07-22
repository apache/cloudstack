package org.apache.cloudstack.api.command.admin.gpu;

import com.cloud.user.Account;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class DeleteVgpuProfileCmdTest {

    @Test
    public void getId() {
        DeleteVgpuProfileCmd cmd = new DeleteVgpuProfileCmd();
        assertNull(cmd.getId());
        Long id = 1L;
        ReflectionTestUtils.setField(cmd, "id", id);
        assertEquals(id, cmd.getId());
    }

    @Test
    public void getEntityOwnerId() {
        DeleteVgpuProfileCmd cmd = new DeleteVgpuProfileCmd();
        assertEquals(Account.ACCOUNT_ID_SYSTEM, cmd.getEntityOwnerId());
    }
}
