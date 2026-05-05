package org.apache.cloudstack.api.command.admin.vm;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class DeployVMCmdByAdminTest {

    @InjectMocks
    private DeployVMCmdByAdmin cmd;

    @Test
    public void testIsBlankInstance_default() {
        assertFalse(cmd.isBlankInstance());
    }

    @Test
    public void testIsBlankInstance_true() {
        ReflectionTestUtils.setField(cmd, "blankInstance", true);
        assertTrue(cmd.isBlankInstance());
    }

    @Test
    public void testIsBlankInstance_false() {
        ReflectionTestUtils.setField(cmd, "blankInstance", false);
        assertFalse(cmd.isBlankInstance());
    }

    @Test
    public void testSetBlankInstance_default() {
        Object obj = ReflectionTestUtils.getField(cmd, "blankInstance");
        assertNull(obj);
    }

    @Test
    public void testSetBlankInstance_true() {
        cmd.setBlankInstance(true);
        Object obj = ReflectionTestUtils.getField(cmd, "blankInstance");
        assertNotNull(obj);
        assertTrue((boolean)obj);
    }

    @Test
    public void testSetBlankInstance_false() {
        cmd.setBlankInstance(false);
        Object obj = ReflectionTestUtils.getField(cmd, "blankInstance");
        assertNotNull(obj);
        assertFalse((boolean)obj);
    }
}
